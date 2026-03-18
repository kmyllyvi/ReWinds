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
