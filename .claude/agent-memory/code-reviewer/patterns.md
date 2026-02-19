# ReWinds Code Patterns Reference

## iOS Initialization Lifecycle (Correct Pattern)
The correct iOS initialization order is:

1. `iOSApp.swift` `init()` - first thing Swift calls before any scene
   - Creates `DatabaseDriverFactory()` (Swift-side, calls into Kotlin)
   - Calls `DIKt.doInitKoin(databaseDriverFactory: driverFactory)` -- one-time Koin start
   - Calls `IosUtilsKt.doInitLogger()` -- Napier logger setup
2. `ContentView.swift` `body` renders `ComposeView`
3. `ComposeView.makeUIViewController` calls `MainViewControllerKt.MainViewController()`
4. `MainViewController.kt` just returns `ComposeUIViewController { App() }` -- no side effects

Anti-pattern (now fixed in commit 4ee4071): Putting Koin/Napier init inside the
ComposeUIViewController lambda. That lambda can be called on recomposition, breaking the
single-init guarantee.

## DI Module Structure
`DI.kt` (commonMain, no package) exports:
- `fun initKoin(databaseDriverFactory: DatabaseDriverFactory)` -- calls startKoin
- `fun appModule(databaseDriverFactory, enableNetworkLogs)` -- Koin module definition

Android calls initKoin from `MainApplication.onCreate()`.
iOS calls initKoin from `iOSApp.init()` via Swift `DIKt.doInitKoin(...)`.

## Platform Detection Pattern
`expect/actual` in `core` package:
- `expect fun isAndroid(): Boolean` / `expect fun isIOS(): Boolean`
- iOS actuals in `Platform.apple.kt`: returns false/true respectively
- Android actuals in `Platform.android.kt`: returns true/false respectively
Used to gate iOS-incompatible features (e.g., DB export/import) with `if (isAndroid())`.

## Test Infrastructure
- iOS tests: `composeApp/src/iosTest/kotlin/`
- Compile check: `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` (~44 min first run)
- Common tests: `composeApp/src/commonTest/kotlin/`
- `AppStartupTest.kt` tests navigation state, NOT Koin initialization

## Known iOS Build Constraints
- Device ARM64 builds OOM (Kotlin/Native devirtualization). Simulator builds OK.
- iOS features that add significant Kotlin code should be gated via `if (isAndroid())`.
- `gradle.properties` has 6GB heap; was tested at 10GB for iOS -- still OOM on device.
