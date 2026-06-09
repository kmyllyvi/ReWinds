# iOS Platform Module

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. -->

**Last updated:** 2026-06-09 (bootstrap)
**Status:** Active

---

## Purpose

Provides all iOS-specific implementations: Keychain-based API key storage for both the Anthropic and Visual Crossing keys, the SQLite database driver, the Swift app entry point, and the Kotlin↔Swift callback bridge pattern.

---

## Responsibilities

- Loading API keys from the iOS Keychain at app startup (priority: build config > Keychain).
- Registering Swift callback closures with Kotlin bridge objects so Kotlin-side save/delete actions persist to Keychain.
- Exposing wrapper functions in `iosMain` Kotlin so Swift can call into the Kotlin layer without direct object access.
- Initialising Koin DI with the iOS `DatabaseDriverFactory`.
- Initialising the Napier logger for iOS.
- Providing the Compose root view controller (`ContentView.swift` wrapping the KMP `MainViewController`).
- Forcing dark mode via `.preferredColorScheme(.dark)`.
- Platform-specific `actual` implementations for: `getAnthropicApiKey()`, `saveApiKeyPlatform()`, `deleteApiKeyPlatform()`, `getVisualCrossingApiKey()`, `saveWeatherApiKeyPlatform()`, `deleteWeatherApiKeyPlatform()`, `httpClient()`, `isIOS()`, `isAndroid()`, language persistence, and `DatabaseExportImport`.

---

## Dependencies

### Kotlin side (iosMain)
- `core.ApiKeyManager` — in-memory Anthropic key store.
- `core.WeatherApiKeyManager` — in-memory Visual Crossing key store.
- `core.KeychainBridge` — singleton holding Swift-registered save/delete callbacks.
- `core.WeatherKeychainBridge` — equivalent for Visual Crossing key.

### Swift side
- `KeychainHelper.swift` — wraps Apple Security framework (`SecItemAdd`, `SecItemCopyMatching`, `SecItemDelete`).
- `ComposeApp` framework — Kotlin/Native compiled framework imported into Swift.
- `iOSApp.swift` — `@main` entry point; wires all of the above.

---

## Key interfaces

### IosKeychain.kt (iosMain wrappers exposed to Swift)
```kotlin
fun setApiKeyFromKeychain(key: String)           // push key into ApiKeyManager
fun registerKeychainCallbacks(
    onSave: (String) -> Unit,
    onDelete: () -> Unit
)
```
Swift calls these as `IosKeychainKt.setApiKeyFromKeychain(key:)` and `IosKeychainKt.registerKeychainCallbacks(onSave:onDelete:)`.

### IosWeatherKeychain.kt (iosMain wrappers)
```kotlin
fun setWeatherApiKeyFromKeychain(key: String)
fun registerWeatherKeychainCallbacks(
    onSave: (String) -> Unit,
    onDelete: () -> Unit
)
```

### KeychainBridge (object, iosMain)
```kotlin
var saveKeyCallback: ((String) -> Unit)?
var deleteKeyCallback: (() -> Unit)?
fun saveKey(key: String)
fun deleteKey()
```
Called from `SettingsViewModel`/platform actual when the user saves or removes the Anthropic key.

### WeatherKeychainBridge (object, iosMain)
Same shape as `KeychainBridge` for the Visual Crossing key.

### KeychainHelper.swift (service: `com.km.rewinds`)
- `save(_ key: String) -> Bool` / `load() -> String?` / `delete() -> Bool` — account: `anthropic_api_key`
- `saveWeatherKey(_:) -> Bool` / `loadWeatherKey() -> String?` / `deleteWeatherKey() -> Bool` — account: `visual_crossing_api_key`
- Accessibility: `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`

### iOSApp startup sequence
1. Check `Bundle.main.infoDictionary["ANTHROPIC_API_KEY"]` (build config). If valid → `setApiKeyFromKeychain`.
2. Else load from Keychain → `setApiKeyFromKeychain`.
3. Register Keychain callbacks for future save/delete.
4. Load Visual Crossing key from Keychain → `setWeatherApiKeyFromKeychain`.
5. Register Visual Crossing callbacks.
6. `DatabaseDriverFactory()` → `DIKt.doInitKoin(databaseDriverFactory:)`.
7. `IosUtilsKt.doInitLogger()`.

---

## Known constraints

- The Anthropic API key build-config path reads from `Info.plist` key `ANTHROPIC_API_KEY`, typically populated from `Config.local.xcconfig` (gitignored). This file must be created manually per developer.
- There is no equivalent build-config path for the Visual Crossing key on iOS; it must be entered in Settings and stored in Keychain.
- `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` means keys do not migrate to a new device via iCloud Keychain backup. Users must re-enter keys after device migration.
- The Kotlin/Native framework must be built and embedded before the Swift code can compile. Use Xcode workspace (`iosApp.xcworkspace`), not `xcodeproj`.
- Do not add manual `-framework ComposeApp` linker flags in Xcode build settings. The `composeApp.podspec` via `vendored_frameworks` handles all linkage. Manual flags cause duplicate symbol errors at link time.

---

## Decisions log

- See `/docs/agent/decisions/` for records created after the bootstrap phase.
