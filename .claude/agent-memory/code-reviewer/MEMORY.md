# Code Reviewer Agent Memory

## Project Overview
ReWinds is a Kotlin Multiplatform app (iOS + Android) using Jetbrains Compose, Koin for DI,
and SQLDelight for the database. iOS builds via Cocoapods + ComposeUIViewController.

See `patterns.md` for detailed notes on architecture and platform patterns.

## Critical Rules to Enforce
1. MV* Pattern: NO logic/state in Composables. All state/logic in ViewModels via StateFlow.
   - App.kt CURRENTLY VIOLATES this rule (var showContent by remember) - known technical debt.
2. Koin: initialized exactly once per platform before Compose renders.
3. Platform init order: Swift App.init() -> Koin -> Napier -> MainViewController -> App().

## Key File Locations
- iOS entry: `iosApp/iosApp/iOSApp.swift` (App.init initializes Koin + Napier)
- iOS bridge: `iosApp/iosApp/ContentView.swift` (calls MainViewControllerKt.MainViewController())
- iOS Kotlin entry: `composeApp/src/iosMain/kotlin/MainViewController.kt`
- Android entry: `composeApp/src/androidMain/kotlin/com/km/rewinds/MainApplication.kt`
- DI module: `composeApp/src/commonMain/kotlin/DI.kt` (fun initKoin, fun appModule)
- Platform detection: `composeApp/src/iosMain/kotlin/core/Platform.apple.kt` (isAndroid/isIOS)
- Napier init: `composeApp/src/iosMain/kotlin/core/IosUtils.kt` (fun initLogger)
- Arch rules doc: `docs/ARCHITECTURE-RULES.md`

## Swift/Kotlin Naming Convention
- Kotlin top-level functions in file `Foo.kt` (no package, or root package) -> `FooKt.functionName()` in Swift
- Kotlin functions starting with `init` get `do` prefix in Swift to avoid Swift keyword conflict
  - `fun initKoin(...)` in `DI.kt` -> `DIKt.doInitKoin(databaseDriverFactory:)` in Swift
  - `fun initLogger()` in `IosUtils.kt` (package `core`) -> `IosUtilsKt.doInitLogger()` in Swift
- `DatabaseDriverFactory` is an expect/actual class; iOS creates it in Swift then passes to initKoin

## Known Architecture Issues (Existing, Not Introduced by Review)
- `App.kt` AppContent() uses `var showContent by remember { mutableStateOf(true) }` -- MV* violation
  This is existing technical debt; flag but do not block commits on it.
