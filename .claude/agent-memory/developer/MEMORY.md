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
- BETTER (KIM-321): when adding a NEW method to these interfaces, give it a DEFAULT body in the
  interface (`= emptySet()`, `= flowOf(emptySet())`, no-op `{}`). Then only the production impl
  (and the fakes that actually exercise it) need overrides — you avoid touching ~10 unrelated
  fakes. Same trick works for `ai.AiConversationRepository` (e.g. `fun setSystemPrompt(p) {}`).

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
- CRASH FIX (post-KIM-286): adding `ioDispatcher` broke the `viewModelOf(::ChatViewModel)` binding.
  `viewModelOf`/`singleOf`/`factoryOf` use constructor reflection and try to resolve EVERY param —
  Kotlin default values are IGNORED. Koin had no `CoroutineDispatcher` definition →
  `NoDefinitionFoundException` → chat screen crash (iOS surfaced it as coroutine
  `propagateExceptionFinalResort`). Fix in `DI.kt`: bind with explicit lambda
  `viewModel { ChatViewModel(get(), get(), get()) }` so the defaulted param falls back. Any VM/class
  with a defaulted ctor param must use an explicit lambda binding, not the reflective `*Of` helpers.
  Regression test: `ChatViewModelKoinGraphTest` resolves the VM from a graph with NO dispatcher.
- CONTEXT CHIPS (bug fix): `loadContextChips()` must only show a chip for a place that has a
  tagged chat session. It intersects `getSavedPlaceNames()` with the non-null `placeId`s from
  `listSessions()` via pure `ChatSessionLogic.placesWithSessions(savedNames, taggedPlaceIds)`.
  "All places" (placeName=null) is always first. NOTE: production never sets `placeId` yet
  (createSession() is called without it everywhere) — until the KIM-129c follow-up wires
  place-tagging on the active session, the per-place chips will legitimately be empty.

## Tab navigation & cross-tab routing (Router.kt + TabRoutingNavigator)

- `core/Navigation()` in `Router.kt` holds THREE independent back stacks (places/chat/settings),
  each wrapped in its own `NavigatorImpl`. Active tab lives in `TabNavigationViewModel.activeTab`.
- `NavigatorImpl.navigateToChat/navigateToSettings` PUSH onto the *current* stack. That is wrong
  for cross-tab intents (e.g. PlaceSummary's "Chat about X"): pushing a `ChatRoute` onto the Places
  stack renders nothing and falls through to Home. Cross-tab navigation must go through
  `core/TabRoutingNavigator` (a `Navigator by base` decorator) which redirects chat/settings to
  `selectTab(...)` and handles root-level back via an `onRootBack` callback.
- Both the Places-tab delegate AND the PlaceSummary push destination use the routing delegate —
  if you add a new screen that can launch chat, pass it a `TabRoutingNavigator`, never raw `NavigatorImpl`.
- GOTCHA: the push-destination branch in `Navigation()` must be guarded with `activeTab == AppTab.PLACES`.
  A PlaceSummaryRoute stays on the Places stack after navigateToChat switches tabs; without the guard
  the place screen overrides the Chat tab. Routing logic is unit-tested in `TabRoutingNavigatorTest`.
- Chat-tab root back is NOT a no-op anymore: `onRootBack = { selectTab(PLACES); true }`.

## Keyboard/IME inset handling (KIM-298)
- iOS CMP gotcha: `consumeWindowInsets(innerPadding)` does NOT reliably subtract a consumed
  inset (e.g. Scaffold bottomBar / 72.dp tab bar) from `WindowInsets.ime` downstream. A child
  `imePadding()` then double-counts the tab-bar height → empty gap (~tab-bar height) between the
  chat input and the keyboard. Was tried in PR #35 and DID NOT fix iOS.
- Working fix: handle the bottom inset in ONE place. In `core/Router.kt` Scaffold content lambda,
  pad the content Box by `bottom = maxOf(innerPadding.calculateBottomPadding(), imeBottom)` where
  `imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()`. Removed `imePadding()`
  from `ai/ChatView.kt`'s Column so the inset isn't applied twice. Keyboard closed → tab-bar inset;
  open → content rises to sit on the keyboard.
- Scaffold uses `contentWindowInsets = WindowInsets(0)`, so `innerPadding` is ONLY the bottomBar
  height (no IME/system bars) — that's why IME must be read separately and combined manually.
- API note: `PaddingValues.calculate{Bottom,Top}Padding()` are MEMBER fns (no import);
  `calculate{Start,End}Padding(layoutDirection)` are EXTENSIONS needing import + LocalLayoutDirection.
- Not unit-testable (no IME in JVM unit tests) — layout-only change, allowed testing exception.

## Reproducing iOS runtime crashes locally (no Gradle iOS build)
- Build: `xcodebuild -workspace iosApp/iosApp.xcworkspace -scheme iosApp -configuration Debug
  -sdk iphonesimulator -destination "id=<UDID>" -derivedDataPath build`.
- Boot/install/run: `xcrun simctl boot <UDID>`; `xcrun simctl install <UDID> <.app>`;
  `xcrun simctl launch --console-pty <UDID> com.km.rewinds` — the console shows the full Kotlin
  `Caused by:` chain; scroll past generic coroutine final-resort frames to the real cause.
- Force-open a deep screen without UI taps (computer-use/osascript need Accessibility perms often
  unavailable): temporarily change default tab in `core/TabNavigationViewModel.kt` (`AppTab.PLACES`
  -> target), rebuild, launch, then REVERT.
- Simulate upgrade-from-old-DB: hand-build app.db with `sqlite3`, `PRAGMA user_version=N`, drop into
  `$(xcrun simctl get_app_container <UDID> com.km.rewinds data)/Library/Application Support/databases/app.db`.
  NativeSqliteDriver auto-runs `Schema.migrate` on first DB access. Verified migration 5->6 applies
  cleanly on iOS.

## buildAndroidOnly known lint failure (unrelated to feature work)
- `./gradlew buildAndroidOnly` currently FAILS at `lintDebug` with `ProtectedPermissions` on
  `READ_DEVICE_CONFIG` in `androidMain/AndroidManifest.xml` (pre-existing, on clean develop).
  Kotlin compilation + `:composeApp:testDebugUnitTest` both PASS. Verify via the test task, not the
  full lint-gated build.

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

## DB-backed tests need androidUnitTest, not commonTest (KIM-321)
- `commonTest` compiles for JVM + Native, so it CANNOT use `JdbcSqliteDriver` (JVM-only). A test
  that needs a REAL SQLDelight DB (e.g. proving a reactive Flow re-emits after an insert) must live
  in the JVM-only `androidUnitTest` source set — it runs under `:composeApp:testDebugUnitTest`.
- Wire-up in `composeApp/build.gradle.kts`: `val androidUnitTest by getting { dependencies {
  implementation(libs.sqldelight.sqlite.driver); implementation(libs.sqldelight.coroutines.extensions) } }`.
  Catalog entry: `sqldelight-sqlite-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqlDelight" }`.
- In-memory DB: `JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)` then `AppDatabase.Schema.create(driver)`,
  then `SqlDelightDatabase(AppDatabase(driver, DayAdapter=..., HourAdapter=...))`. Adapters use
  `listOfStringAdapter` (in `core`). Insert via `db.saveWeatherResponse(WeatherResponse(...))`.

## Reactive downloaded-months + chat data-awareness (KIM-321)
- `coroutines-extensions` is a commonMain dep. Reactive query pattern:
  `dbQuery.someSelect(arg).asFlow().mapToList(Dispatchers.IO).map { ... }` (imports
  `app.cash.sqldelight.coroutines.asFlow` / `.mapToList`). Re-emits on any write to the queried table.
- Distinct months in DB without schema change: `SELECT DISTINCT SUBSTR(datetime,1,7) AS month FROM Day WHERE ...`.
- KMP-safe month label: `core.utils.formatMonthName("YYYY-MM") -> "October 2025"` in FormatUtils.kt
  (reuses `monthName(Int)`; NO String.format / java.time). Internal grouping stays YYYY-MM.
- System prompt is no longer a static constant: `AppConstants.buildAnthropicSystemPrompt(map, formatter)`.
  `ChatViewModel.refreshSystemPrompt()` pushes it via `AiConversationRepository.setSystemPrompt(...)`
  before every send AND before the post-confirmation retry. VM caches per-place months in a
  `StateFlow<Map<String,Set<String>>>` collected from `observeDownloadedMonths(place)` at init.
- ALL paid Visual Crossing fetches in `WeatherTools` are gated: call `checkDataAvailability` BEFORE
  `getDaysRange`; on Partial/Missing return the shared `buildPermissionRequired(...)` JSON
  (`downloaded_months` + `missing_months`, full month names). No auto-fetch path remains.
  `list_saved_places` takes no date range → no gate.

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

## Chat place pill testTag (post KIM-287 / KIM-290)
- KIM-287 (PR #27) replaced the interactive `ContextChipRow`/`ContextChip` composables in
  `ai/ChatView.kt` with a single static `PlaceTagPill(placeName: String)` (display-only; switching
  chats is the session switcher's job, KIM-286).
- testTag for it: `TestTags.CHAT_PLACE_TAG_PILL` ("chat_place_tag_pill"), on PlaceTagPill's root Row.
  The old `CHAT_CONTEXT_CHIP_ROW` / `CHAT_CONTEXT_CHIP` constants were removed — do not reintroduce.
- `ChatLayoutTest.kt` lives in androidInstrumentedTest (NOT commonTest), uses text matching, runs via
  connectedAndroidTest — it is NOT executed by `:composeApp:testDebugUnitTest`.

## Maestro E2E suite (KIM-294)
- Lives in `.maestro/`: 8 flows (J1-J8 from KIM-291) + `config.yaml` (scopes runs to `flows/*.yaml`).
  Pinned Maestro **1.39.0**; run manually / on release-to-master only via
  `.github/workflows/e2e-smoke.yml` (push:[master] + workflow_dispatch, NOT per-PR; no nightly
  cron — saves CI minutes on free plan). Per-PR UI gating is KIM-293 (Compose semantic tests).
- Cross-platform via `appId: ${APP_ID}`. Android `com.km.rewinds`; iOS bundle id is
  `${BUNDLE_ID}${TEAM_ID}` (base `com.km.rewinds` + Apple TEAM_ID suffix). Resolve installed iOS id:
  `xcrun simctl listapps booted | grep -i rewinds`. Selectors are TestTags constants used as `id:`.
- TestTags GAPS (no constant exists → flows use commented text/index fallback, documented in
  `.maestro/README.md`): month-cell FULL/NO_DATA state (PLACE_MONTH_CELL is state-agnostic),
  DownloadMissingDaysDialog (PlaceSummaryView.kt:181), chat API-key dialog (ChatView.kt:203).
  Candidate KIM-290 follow-up to add `PLACE_MONTH_CELL_FULL/_NO_DATA`, `DOWNLOAD_DIALOG_*`,
  `CHAT_API_KEY_DIALOG_*`.
- App identifiers: Android applicationId/namespace `com.km.rewinds` (composeApp/build.gradle.kts);
  iOS base bundle in `iosApp/Configuration/Config.xcconfig` (`BUNDLE_ID=com.km.rewinds`), TEAM_ID in
  `iosApp/Config.local.xcconfig`.
- gh tip: PR/commit bodies with apostrophes break bash heredocs — write to a temp file and use
  `gh pr create --body-file`.

## Monthly-summary bug fixes (KIM-327/328/329)
- "Day card" terminology: collapsed/closed = `DaySummaryRow`; open = `DayDetailSheet`
  (hourly chart). Collapsed row shows `DayWeatherSummary.collapsedRowWindSpeed`
  (= `sustainedWindSpeed`, the average top / peak-sustained wind), NOT gust
  (`maxWindSpeed` ← `windgust`). Field choice lives in the data-class accessor so it's
  unit-testable, not in the composable (KIM-329).
- `formatGust` was renamed to `formatWindSpeed` (core/utils/FormatUtils.kt; generic
  whole-number km/h). CAVEAT: `place/components/WeatherCards.kt` has its OWN file-private
  `formatWindSpeed` with different rounding — future cleanup to collapse onto the shared one.
- `DailyWindBarChart` highlights via `highlightedIndices: Set<Int>` derived in the VM from
  `DayWeatherSummary.isMatch` (set by `filter.matches`), NOT a single `peakIndex`. Chart does
  no selection logic. VM helper `qualifyingDayIndices(days): Set<Int>` (KIM-328).
- Skeleton/loading placeholders MUST use a DISTINCT testTag from the loaded element.
  `StatCardGridSkeleton` uses `MONTH_STAT_CARD_GRID_SKELETON`; the loaded `StatCardGrid`
  keeps `MONTH_STAT_CARD_GRID`. Reusing it lets a Maestro flow's "loaded" wait be satisfied
  by the skeleton and mask the loaded-state assertion (Marcy caught this on KIM-327).
- Marcy sub-agents run git ops that can leave the PARENT session on `develop` — always
  re-checkout your feature branch before applying review fixes.

## Per-cell "which item is busy" state (KIM-332)
- To show a loading indicator on ONE cell/row of a list (not a screen-wide flag), model the
  identity in the VM state, not a boolean: `PlaceSummaryViewModel` replaced
  `isDownloadingMonth: Boolean` with `downloadingMonth: Int?` on `WeatherSummaryUiState.Success`.
  The grid derives per-cell `isDownloading = cell.month == downloadingMonth` (pure mapping in the
  composable, not logic). A boolean can't tell WHICH cell to mark — always use the id/index.
- The indicator clears "for free" because `loadWeatherData()` emits a FRESH `Success(...)` (defaults
  `downloadingMonth = null`) after the download, rather than `.copy()`. Verify this when relying on
  reload-to-clear; a `.copy()`-based reload would keep the stale value.
- Guard the in-flight cell against re-taps: `.clickable(enabled = !isDownloading, onClick = ...)`.
- `MonthCell` download indicator: small `CircularProgressIndicator(size=14.dp, strokeWidth=2.dp,
  color=accentBlue)` + shared `strings.downloading` ("Downloading..."/"Herunterladen..." already
  in both locales). testTag `PLACE_MONTH_CELL_DOWNLOADING`.

## Compose semantic UI tests (KIM-293, Layer 1) — canonical pattern
- Doc: `composeApp/src/androidInstrumentedTest/README.md`. Tests in `uitest/` package; shared fakes
  in `uitest/Fakes.kt` (FakeWeatherRepository, FakeAiConversationRepository, FakeChatRepository,
  FakeDatabase). Run: `./gradlew :composeApp:connectedDebugAndroidTest` (needs emulator).
  Compile-only check (no device): `./gradlew :composeApp:compileDebugAndroidTestKotlin`.
- androidInstrumentedTest does NOT see commonTest. Shared doubles visible to both go in
  `src/commonTestFixtures/kotlin` (added to commonTest `kotlin.srcDir` AND android `androidTest`
  srcDirs in build.gradle.kts). FakeNavigator was moved there from commonTest.
- Every screen composable takes vm + navigator as params → build the REAL ViewModel from fakes and
  pass it directly (no Koin singleton → order-independent). koinViewModel() only used if no vm arg.
- Testability seams added: `ai.AiConversationRepository` interface (AiRepository implements it;
  ChatViewModel depends on the interface so chat is fakeable w/o Anthropic network). DI binds
  `single<AiConversationRepository> { AiRepository(...) }`. `core.ApiKeyChecker` fun interface +
  `PlatformApiKeyChecker` default, injected into ChatViewModel & SettingsViewModel so the Anthropic
  key present/absent branch is deterministic (no SharedPreferences/BuildConfig dependence).
- Koin: SettingsViewModel switched viewModelOf→`viewModel { SettingsViewModel(get(), get()) }` so the
  defaulted apiKeyChecker isn't resolved by reflection (same reason ChatViewModel uses a lambda).
- Compose merged-tree gotcha: a clickable Row merges descendant Text into its own node. Assert a
  row's chip via `hasTestTag(...).and(hasText(chip, substring=true))` on the merged tree —
  hasAnyDescendant(hasText) finds nothing. `assertExists()`/`onNode`/`onAllNodes` are MEMBERS
  (no import); assertIsDisplayed/assertCountEquals/performClick are extension imports.
- New shared tags: `TestTags.APP_HEADER_TITLE` (AppHeader title), `TestTags.CHAT_MESSAGE_BUBBLE`
  (ChatMessageBubble) — added for the ChatLayoutTest tag migration.
