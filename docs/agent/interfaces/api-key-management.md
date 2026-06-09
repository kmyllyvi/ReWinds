# API Key Management

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. -->

**Last updated:** 2026-06-09 (bootstrap)
**Status:** Active

---

## Overview

Two separate API keys are managed by the app: the Anthropic key (for AI chat) and the Visual Crossing key (for weather data). Each has an in-memory manager singleton, a platform-specific storage mechanism, and expect/actual bridge functions.

---

## ApiKeyManager (Anthropic key, `core/ApiKeyManager.kt`)

```kotlin
object ApiKeyManager {
    fun setApiKey(key: String)
    fun getApiKey(): String
    fun hasValidKey(): Boolean     // non-blank, starts with "sk-ant-", no "placeholder"
}
```

## WeatherApiKeyManager (Visual Crossing key, `core/WeatherApiKeyManager.kt`)

```kotlin
object WeatherApiKeyManager {
    fun setApiKey(key: String)
    fun getApiKey(): String
    fun hasValidKey(): Boolean     // non-blank, no "placeholder"
}
```

---

## Platform expect/actual contract (`core/Platform.kt`)

```kotlin
expect fun getAnthropicApiKey(): String
expect fun saveApiKeyPlatform(key: String)
expect fun deleteApiKeyPlatform()

expect fun getVisualCrossingApiKey(): String
expect fun saveWeatherApiKeyPlatform(key: String)
expect fun deleteWeatherApiKeyPlatform()

// Convenience check (common, not expect)
fun isAnthropicApiKeyConfigured(): Boolean
```

---

## Platform implementations

### iOS
- `getAnthropicApiKey()` → `ApiKeyManager.getApiKey()`
- `saveApiKeyPlatform(key)` → `KeychainBridge.saveKey(key)` → registered Swift closure → `KeychainHelper.shared.save(key)`
- `deleteApiKeyPlatform()` → `KeychainBridge.deleteKey()` → registered Swift closure → `KeychainHelper.shared.delete()`
- Same pattern for Visual Crossing via `WeatherKeychainBridge` / `KeychainHelper.saveWeatherKey`.

**Startup load order (iOSApp.swift):**
1. Check `Info.plist["ANTHROPIC_API_KEY"]` (from `Config.local.xcconfig`, gitignored).
2. Else load from Keychain.
3. Register save/delete callbacks.
4. Load Visual Crossing key from Keychain only (no build-config path for VC key on iOS).
5. Register VC save/delete callbacks.

### Android
- `getAnthropicApiKey()` → reads from `BuildConfig.ANTHROPIC_API_KEY` (injected from `gradle.properties`, gitignored).
- `getVisualCrossingApiKey()` → reads from `BuildConfig.VISUAL_CROSSING_API_KEY`.
- Save/delete are no-ops on Android (keys are not runtime-editable via Settings on Android).

---

## Keychain storage details (iOS)

| Key | Service | Account | Accessibility |
|---|---|---|---|
| Anthropic | `com.km.rewinds` | `anthropic_api_key` | `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` |
| Visual Crossing | `com.km.rewinds` | `visual_crossing_api_key` | `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` |

`WhenUnlockedThisDeviceOnly` means keys do not migrate via iCloud backup.

---

## Validation rules

| Key | Valid when |
|---|---|
| Anthropic | Non-blank, starts with `sk-ant-`, does not contain `placeholder` |
| Visual Crossing | Non-blank, does not contain `placeholder` |

`isAnthropicApiKeyConfigured()` is called by `ChatViewModel` before sending a message. `WeatherApiKeyManager.hasValidKey()` is called by `WeatherRepository.doRequest()` before any Visual Crossing network call.
