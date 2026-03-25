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

## SQLDelight Schema Migrations
- Migration files: `composeApp/src/commonMain/sqldelight/com/km/rewinds/db/N.sqm`
- Schema: `AppDatabase.sq` (CREATE TABLE + queries)
- **When adding a table**: add to `AppDatabase.sq` AND create matching `N.sqm` migration file
- Current schema version: 3 (KIM-140 added AppSettings in `2.sqm`)

## KMP Compatibility: String Formatting

`"%.1f".format(value)` is JVM-only — does NOT compile on Kotlin/Native (iOS).
Use `core.utils.formatDecimal(value: Double): String` from `FormatUtils.kt` instead.
- Implementation uses integer math: `(value * 10).toLong()` — safe on all platforms
- Other existing pattern in codebase: `(value * 10).roundToInt() / 10.0` (returns Double, not String)
- Any `String.format(...)` calls in `commonMain` will fail iOS builds — always replace with KMP-safe alternatives

## Build Commands
- Android compile only (fastest): `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon`
- Common metadata check: `./gradlew :composeApp:compileCommonMainKotlinMetadata --no-daemon`
- Full Android build (slow, runs tests): `./gradlew buildAndroidOnly --no-daemon`
- Pre-existing test failures: `compileTestKotlinIos*` fails with `@ExperimentalNativeApi` — not my code
- `buildAndroidOnly` has config cache issues — use `:composeApp:assembleDebug --no-configuration-cache` instead
- Add `--rerun-tasks` when build cache is stale and you need to verify compilation

## DayWeatherSummary Pattern
- Data model lives at: `composeApp/src/commonMain/kotlin/place/DayWeatherSummary.kt`
- Mapping from `Day` (DB model) happens in `MonthlyStatisticsViewModel.toDayWeatherSummary()`
- New nullable fields with defaults don't break existing positional-argument tests
- `degreesToCompass(degrees: Double)` in `core.DaysOfInterestFilter.kt` converts wind degrees to compass point string

## Anthropic API Patterns
- `AnthropicClient.sendMessage()` — typed request (with tools, full model)
- `AnthropicClient.sendRawMessage(JsonObject)` — raw JSON in/out (for simple one-shot calls)
- DaysOfInterestParser: uses `sendRawMessage`, no tools, system prompt instructs JSON-only output

## Key File Locations
- CI workflow: `.github/workflows/ci.yml`
- Shared Xcode scheme: `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`
- Xcode config: `iosApp/Configuration/Config.xcconfig` (use `#include?` for optional includes)
- BuildConfig API key: `composeApp/build.gradle.kts` buildTypes section

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
