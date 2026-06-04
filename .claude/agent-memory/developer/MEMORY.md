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
- Use `./gradlew :composeApp:compileDebugKotlinAndroid` for a clean compile check
- `./gradlew :composeApp:testDebugUnitTest` for unit tests

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
