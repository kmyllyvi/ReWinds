# Developer Agent Memory

## CI / GitHub Actions Patterns

### Android job (ubuntu-latest)
- Use `gradle/actions/setup-gradle@v4` (not deprecated `gradle/gradle-build-action@v3`)
- Pass `-PincludeAllTargets=false` at Gradle command line to skip iOS targets at configuration time
  - The `buildAndroidOnly` task sets this too late (execution time via `doFirst`), so don't use it
- BuildConfig fields must always be emitted unconditionally or they cause `Unresolved reference` errors:
  ```kotlin
  val apiKey = rootProject.findProperty("ANTHROPIC_API_KEY")?.toString()
      ?: System.getenv("ANTHROPIC_API_KEY") ?: ""
  buildConfigField("String", "ANTHROPIC_API_KEY", "\"$apiKey\"")
  ```
- Tasks: `:composeApp:assembleDebug` then `:composeApp:testDebugUnitTest`

### iOS job (macos-latest arm64)
- Must run `./gradlew :composeApp:generateDummyFramework` BEFORE `pod install`
  - composeApp.podspec validates the framework directory at pod install time
- `xcpretty` is NOT pre-installed on macos-latest arm64 — install via `gem install xcpretty --no-document`
- The Xcode scheme must be in `xcshareddata/xcschemes/` (not `xcuserdata/`) to be found on CI
  - Created: `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`
  - `.gitignore` already has `!*.xcodeproj/xcshareddata/` so it gets committed
- The iOS Xcode project has NO XCTest targets — all Kotlin unit tests run via Android/JVM Gradle job
  - Use xcodebuild `build` action (not `test`) to verify iOS app compilation
- `Config.xcconfig` must use `#include?` (not `#include`) for optional local xcconfig files
  - `iosApp/Configuration/Config.xcconfig` includes `Config.local.xcconfig` which is gitignored
- iOS builds take ~19 minutes on CI (Kotlin/Native compilation is slow)
- JDK 17 is required in the iOS job too (Gradle compiles Kotlin framework)

### Simulator selection
- Use `xcrun simctl list devices available --json` + Python to get latest iPhone simulator UDID
- Export UDID via `GITHUB_ENV`, reference as `$SIMULATOR_UDID` in shell scripts

## Key File Locations
- CI workflow: `.github/workflows/ci.yml`
- Shared Xcode scheme: `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`
- Xcode config: `iosApp/Configuration/Config.xcconfig` (use `#include?` for optional includes)
- BuildConfig API key: `composeApp/build.gradle.kts` buildTypes section

## MV* Modal pattern

- Modal visibility: `showXxx: StateFlow<Boolean>` on ViewModel (not `remember { mutableStateOf }` in View)
- Open/close methods: `openXxx()` / `closeXxx()` on ViewModel
- Modal composable receives all data as params + callbacks — no ViewModel access inside it
- This pattern was enforced/renamed in KIM-259 (`isMapModalVisible` → `showStationMap`, etc.)

## Station architecture (post KIM-258)

- Stations stored in `WeatherStation` table (multi-row per place), not on `WeatherResponse`
- `WeatherRepository.fetchAndPersistStations(place)` → `StationsResult` (Success/Empty/Error)
- `WeatherRepository.getPersistedStations(place)` → reads without network
- Auto-backfill in `PlaceSummaryViewModel.loadWeatherData()` when no stations persisted
- `refreshStations()` on ViewModel for manual re-fetch (sets `isRefreshingStations` flag)
- `StationDisplayData` in `place/` package for UI — separate from `core.Station`
- `WeatherSummaryUiState.Success.stations: List<StationDisplayData>` — View reads from state only
- Migration: 4.sqm (v4→v5) recreates WeatherResponse table without old station columns
- Modal visibility state (`showStationMap: StateFlow<Boolean>`) lives on ViewModel, not in View
- `openStationMap()` / `closeStationMap()` on ViewModel (renamed in KIM-259 from showMapModal/dismissMapModal)

## Visual Crossing API JSON shape
- `stations` is a JSON **object keyed by station ID**, NOT a JSON array
- `WeatherResponse.stations: Map<String, Station>?` — already the correct Kotlin type
- Deserializes correctly with `Json { ignoreUnknownKeys = true }`
- Fakes bypass JSON deserialization — always add a JSON-layer test for new API response shapes:
  `Json { ignoreUnknownKeys = true }.decodeFromString<WeatherResponse>(rawJson)`
- See `StationsJsonDeserializationTest.kt` for the Ermatingen regression test pattern

## htmlToBase64 bug (fixed in KIM-259)
- Original code computed `hasSecond`/`hasThird` from `i` AFTER advancing the index
- Produced wrong padding: `bytes.size % 3 == 0` lost last byte; `% 3 == 1` added phantom byte
- iOS WKWebView is strict — rejects malformed base64 data-URIs → blank map, no station markers
- Fix: capture `hasB2 = i < bytes.size` BEFORE the `if (hasB2) bytes[i++]` call
- See `StationMapBase64Test.kt` for RFC-4648 regression coverage

## Test isolation gotcha

- `StandardTestDispatcher` + `viewModelScope.launch` coroutines: uncaught exceptions in `.map {}` operators leak to next test as `UncaughtExceptionsBeforeTest`
- Fix: wrap `repository.call()` in try-catch inside `.map {}` so exceptions never escape
- See `HomeViewModel.kt` search flow for the pattern

## Build command notes

- `./gradlew buildAndroidOnly` may fail with config-cache error (pre-existing, unrelated to code)
- It also drags in `linkReleaseFrameworkIosSimulatorArm64`, which OOMs locally (exit 137) — NOT a code failure
- For reliable Android-only verification skip buildAndroidOnly and run the leaf tasks directly:
  - `./gradlew :composeApp:compileDebugKotlinAndroid --no-configuration-cache` (compile check)
  - `./gradlew :composeApp:testDebugUnitTest --no-configuration-cache` for unit tests
- Background gradle output piped through `tail` only flushes on completion; poll the
  JUnit XML at `composeApp/build/test-results/testDebugUnitTest/TEST-*.xml` for pass/fail counts

## Core interface test fakes (update ALL when adding interface methods)

When you add a method to `core.WeatherRepository` or `core.Database`, every fake must implement it
or the commonTest source set won't compile. Known fakes as of KIM-278:
- `WeatherRepository`: `MockWeatherRepository` (HomeViewTest), `MockWeatherRepositoryForSearch`
  (HomeViewModelSearchTest), `FakeWeatherRepository` (PlaceSummaryViewModelStationTest),
  anonymous object (WeatherToolsMetricsTest), `FakeWeatherRepository` (ChatPersistenceTest).
- `Database`: `MockDatabase` (HomeViewTest), `MockDatabaseForSearch` (HomeViewModelSearchTest).
  Production impl is `SqlDelightDatabase` in `core/Database.kt`.

## Chat multi-session data layer (KIM-285)

- `ChatSession` columns: `title TEXT NOT NULL DEFAULT 'Chat'` + nullable `placeId TEXT`.
  placeId = `WeatherResponse.resolvedAddress` (TEXT, NOT integer) — chats are not 1:1 with places.
- Migration `5.sqm` (v5→v6) uses plain `ALTER TABLE ... ADD COLUMN` (additive, no recreation needed).
- `ai/ChatSessionLogic.kt` — DB-free object holding the testable rules: `deriveTitle(msg, placeName)`
  (place tag + first ~40 chars, ellipsis, default fallback), `shouldEvictBeforeCreate(count)` (cap=50),
  `resolveActiveSessionId(requestedId, idsNewestFirst)`. Unit tested in `ChatSessionLogicTest`.
- `ChatRepository` interface gained: `listSessions()`, `createSession(placeId)`, `switchToSession(id)`,
  `renameSession(id, title)`. Auto-title fires on the first USER message and only overwrites the
  still-default title (never clobbers an explicit rename).
- `ChatViewModel`: `loadActiveSession(requestedId)` replaces the old latest-only rule; `switchToSession(id)`
  added. Single-chat launch preserved (auto-creates a session when none exist).
- KNOWN FAKES of `ChatRepository` to keep in sync: `FakeChatRepository` (ChatPersistenceTest),
  `InMemoryChatRepository` (ChatSessionRepositoryContractTest).

## Chat session switcher UI (KIM-286)

- `ChatViewModel` now takes `ioDispatcher: CoroutineDispatcher = Dispatchers.IO` and uses it in
  every `viewModelScope.launch(ioDispatcher)`. This is the only way to make its coroutine-driven
  actions testable: `Dispatchers.setMain` does NOT redirect a hardcoded `Dispatchers.IO`. Inject a
  `StandardTestDispatcher` and drive with `runTest(dispatcher){ ... advanceUntilIdle() }`. Teardown
  must cancel each VM's `viewModelScope` before `resetMain()`. See `ChatSessionSwitcherViewModelTest`
  (its own `MultiSessionFakeChatRepository` tracking per-session messages, newest-first listSessions).
- Switcher state lives in `ChatUiState`: `isSessionSwitcherOpen`, `sessions: List<ChatSessionSummary>`,
  `activeSessionId`. VM actions: `openSessionSwitcher()` (refreshes list), `closeSessionSwitcher()`,
  `startNewChat()`, `switchToSession(id)` (closes sheet, no-op if missing). Never re-sort in the View.
- `ai/ChatSessionSwitcher.kt` is the `ModalBottomSheet` composable (pure render + callbacks).
  Header entry point: `AppHeader(rightContent = { IconButton(...) })` with `Icons.AutoMirrored.Filled.List`.
- Relative timestamps: pure `ChatSessionLogic.relativeTimeLabel(ts, now)` (Just now / Nm / Nh / Nd / Nw,
  clamps negative deltas). Caller passes a single `Clock.System...toEpochMilliseconds()` per render.
- Reminder: every new AppStrings field must be added to BOTH English and German with a real German
  translation. `LocalizationTest` is hand-written per-field (not reflection), so it won't auto-fail on
  a missing/untranslated new field — translate anyway.

## WeatherRepositoryImpl in-memory cache (KIM-278)

- Session-scoped `savedDataCache: HashMap<String, WeatherResponse>` in `WeatherRepositoryImpl`.
- `getSavedDataFor` is read-through (cache hit returns immediately; miss loads + stores).
- ALL DB writes go through private `persistAndInvalidate(response)` (saves + evicts entry).
  `deletePlace` also evicts. If you add a new write path, route it through `persistAndInvalidate`,
  never call `database.saveWeatherResponse` directly, or the cache goes stale.
- Home day counts use the lightweight `getPlaceDayCounts()` (single GROUP BY via
  `getAllPlaceDayCounts` in AppDatabase.sq) instead of loading full per-place data.

## Progressive render + skeletons (KIM-278)

- For staged UI, the VM clears state at the start of a load so the View can show a loading
  state, then emits in stages. Example: `MonthlyStatisticsViewModel.loadStatistics()` sets
  `_statistics = null` and `_dailySummaries = []` first, then emits days, then stats.
- Skeleton placeholders are plain `Box` filled with
  `MaterialTheme.rewinds.textTertiary.copy(alpha = 0.18f)` — no shimmer library.
  See `PlaceRowSkeleton` (HomeView) and `DaySummaryRowSkeleton` (MonthlyStatisticsView).
- SQLDelight `SELECT col, COUNT(*) AS dayCount` → generated row has `.col` and `.dayCount`
  (the AS alias). Map with `.executeAsList().associate { it.col to it.dayCount }`.

## Theme / Design system (Midnight Blue — KIM-265+)

- `ReWindsColors` object in `ui/theme/ReWindsTheme.kt` — all palette tokens
- `MaterialTheme.rewinds.xxx` to access extended tokens (accentBlue, attention, error, surface, border, textPrimary, textSecondary, textTertiary, etc.)
- Never use hex literals in view code — always reference token
- Isobar background: `IsobarBackground()` composable, renders as the lowest Box layer

## Home / Places screen patterns (KIM-268)

- `PlaceDisplayData(name, subtitle, status: PlaceStatus)` — PlaceStatus.NORMAL/WARNING/ERROR
- Status dot colour computed in VM (when), rendered in `StatusDot` composable — never in view logic
- `AlertBanner(message, isError: Boolean)` — isError → red (`error` token), else amber (`attention` token)
- `HomeUiState.alertBanners: List<AlertBanner>` — empty by default
- `AppHeader` accepts optional `titleSizeSp: Int?` to override typography sp (used for 26 sp on Home)

## Localization Pattern (KIM-57)

### CompositionLocal Architecture
- `AppStrings.kt` - data class with all string fields; plurals are lambdas `(Int) -> String`
- `LanguageManager.kt` - object with `StateFlow<Language>` + `setLanguage()` + persistence
- `LocalAppStrings.kt` - `compositionLocalOf { AppStrings.English }`
- `App.kt` - wraps AppContent in `CompositionLocalProvider(LocalAppStrings provides strings)`
- Views: `val strings = LocalAppStrings.current` then `strings.someKey`
- No `stringResource()`, `pluralStringResource()`, or `Res` imports in views after migration

### Android Context for SharedPreferences
- `provideAndroidContextForLanguage(context: Context)` in `Platform.android.kt`
- Called from `MainApplication.onCreate()` (same pattern as `initializeDatabaseExportImport`)
- SharedPrefs key: `"rewinds_prefs"` / `"language_code"`

### iOS NSUserDefaults
- `NSUserDefaults.standardUserDefaults.setObject(code, forKey: "rewinds_language_code")`
- In `iosMain/kotlin/core/Platform.apple.kt`

### Tests
- `commonTest/kotlin/core/LocalizationTest.kt` - 38 tests, 3 test classes
  - `AppStringsCompletenessTest` - German strings not blank
  - `AppStringsGermanNotEnglishTest` - German differs from English
  - `LanguageManagerTest` - StateFlow updates correctly
