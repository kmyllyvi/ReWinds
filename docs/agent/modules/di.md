# Dependency Injection Module

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. -->

**Last updated:** 2026-06-09 (bootstrap)
**Status:** Active

---

## Purpose

Configures the Koin DI container. All Koin bindings live in a single `appModule` function in `DI.kt`. Platform entry points (iOS Swift, Android `MainApplication`) call `initKoin()` once at startup.

---

## Responsibilities

- Providing the platform `DatabaseDriverFactory` as a Koin singleton.
- Creating and binding the SQLDelight `AppDatabase` and the `Database` interface implementation.
- Binding `NetworkService` as `Networking` singleton (with `enableNetworkLogs` flag).
- Binding `WeatherRepositoryImpl` as `WeatherRepository` singleton.
- Binding platform-specific `DatabaseExportImport` singleton.
- Creating `AnthropicClient` (reads the Anthropic API key at startup via `getAnthropicApiKey()`).
- Binding `WeatherTools` singleton.
- Binding `AiRepository` singleton (depends on `AnthropicClient`, `WeatherTools`, `WeatherRepository`).
- Binding `ChatRepositoryImpl` as `ChatRepository` singleton.
- Binding `AppSettingsRepository` singleton.
- Registering all ViewModels via `viewModelOf`: `AppViewModel`, `HomeViewModel`, `PlaceSummaryViewModel`, `MonthlyStatisticsViewModel`, `ChatViewModel`, `SettingsViewModel`, `TabNavigationViewModel`.
- Guard against double-initialisation via `KoinApplicationAlreadyStartedException` catch.

---

## Dependencies

All modules listed in `appModule` — see architecture.md for the full component map.

---

## Key interfaces

### appModule function signature
```kotlin
fun appModule(databaseDriverFactory: DatabaseDriverFactory, enableNetworkLogs: Boolean): Module
```

### initKoin entry point
```kotlin
fun initKoin(databaseDriverFactory: DatabaseDriverFactory)
```
Called from iOS `iOSApp.init()` as `DIKt.doInitKoin(databaseDriverFactory:)` and from Android `MainApplication.onCreate()`.

---

## Known constraints

- `Navigator` is explicitly **not** created through DI. It is instantiated in the `Navigation()` composable using `remember`, because each tab needs its own instance wrapping its own back stack.
- `AnthropicClient` reads the API key at Koin startup time via `getAnthropicApiKey()`. On iOS, this means the Keychain load in `iOSApp.init()` must complete before `initKoin` is called (the current startup sequence in `iOSApp.swift` satisfies this).
- `enableNetworkLogs = true` is hard-coded in `initKoin`. To disable logs, callers must use `appModule` directly.

---

## Decisions log

- See `/docs/agent/decisions/` for records created after the bootstrap phase.
