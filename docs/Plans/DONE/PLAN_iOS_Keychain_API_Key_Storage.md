# Plan: iOS Keychain-Based API Key Storage

## Context
The iOS AI Chat feature requires an Anthropic API key. Currently `Platform.apple.kt` returns a hardcoded placeholder, making the chat unusable on iOS without modifying source code before each build. Android uses `BuildConfig` (gradle.properties at build time), but iOS/Kotlin Native has no equivalent. The solution is to store the key securely in the iOS Keychain and allow users to enter it once via an in-app settings screen.

## Architecture

```
Settings screen (Compose)
    ↓ user enters key
ApiKeyManager (Kotlin - commonMain)
    ↓ calls platform-specific save
Platform.apple.kt (actual)
    ↓ calls Swift via Kotlin/Native callback
KeychainHelper.swift
    ↓ SecItem* APIs
iOS Keychain (persists across app launches)

On startup:
iOSApp.swift → reads Keychain → passes key to Kotlin → initKoin reads it
```

## Implementation Steps

### 1. Create `KeychainHelper.swift` (new file in iosApp/iosApp/)
Simple wrapper around Security.framework (no pods needed, available by default on iOS 15.3+).

```swift
import Foundation
import Security

class KeychainHelper {
    static let shared = KeychainHelper()
    private let service = "com.km.rewinds"
    private let account = "anthropic_api_key"

    func save(_ key: String) -> Bool { ... }   // SecItemAdd / SecItemUpdate
    func load() -> String? { ... }             // SecItemCopyMatching
    func delete() -> Bool { ... }             // SecItemDelete
}
```

### 2. Add `ApiKeyManager` to commonMain (new file)
Shared expect/actual interface for reading/writing the API key at runtime.

**`core/ApiKeyManager.kt`** (commonMain):
```kotlin
object ApiKeyManager {
    private var _apiKey: String = ""

    fun setApiKey(key: String) { _apiKey = key }
    fun getApiKey(): String = _apiKey
    fun hasValidKey(): Boolean = _apiKey.isNotBlank() && !_apiKey.contains("placeholder") && _apiKey.startsWith("sk-ant-")
}
```

### 3. Update `Platform.apple.kt`
Read from `ApiKeyManager` instead of returning placeholder:
```kotlin
actual fun getAnthropicApiKey(): String = ApiKeyManager.getApiKey()
```

### 4. Update `iOSApp.swift`
Load from Keychain before `initKoin`:
```swift
init() {
    // Load API key from Keychain into Kotlin memory before Koin starts
    if let savedKey = KeychainHelper.shared.load() {
        ApiKeyManagerKt.ApiKeyManager.setApiKey(savedKey)
    }
    let driverFactory = DatabaseDriverFactory()
    DIKt.doInitKoin(databaseDriverFactory: driverFactory)
    IosUtilsKt.doInitLogger()
}
```

### 5. Add `SettingsView.kt` (new file in commonMain/kotlin/settings/)
A Compose screen with:
- TextField for entering the API key (masked like a password field)
- Save button → calls `saveApiKeyPlatform(key)` platform function
- Delete button → clears Keychain and resets key
- Status indicator (key configured / not configured)
- Navigation back to home

### 6. Add `saveApiKeyPlatform` expect/actual
**`core/Platform.kt`** (add):
```kotlin
expect fun saveApiKeyPlatform(key: String)
expect fun deleteApiKeyPlatform()
```

**`Platform.apple.kt`** (actual):
```kotlin
// Delegate to Swift Keychain via a registered callback
actual fun saveApiKeyPlatform(key: String) {
    ApiKeyManager.setApiKey(key)
    KeychainBridge.saveKey(key)   // calls Swift
}
actual fun deleteApiKeyPlatform() {
    ApiKeyManager.setApiKey("")
    KeychainBridge.deleteKey()
}
```

**`Platform.android.kt`** (actual - no-op since Android uses BuildConfig):
```kotlin
actual fun saveApiKeyPlatform(key: String) { /* no-op for Android */ }
actual fun deleteApiKeyPlatform() { /* no-op for Android */ }
```

### 7. Create `KeychainBridge.kt` (iosMain - Kotlin/Native → Swift callback)
A Kotlin object that Swift registers a callback on at startup:
```kotlin
object KeychainBridge {
    var saveKeyCallback: ((String) -> Unit)? = null
    var deleteKeyCallback: (() -> Unit)? = null

    fun saveKey(key: String) { saveKeyCallback?.invoke(key) }
    fun deleteKey() { deleteKeyCallback?.invoke() }
}
```

In `iOSApp.swift`, register callbacks:
```swift
KeychainBridgeKt.KeychainBridge.saveKeyCallback = { key in
    _ = KeychainHelper.shared.save(key)
}
KeychainBridgeKt.KeychainBridge.deleteKeyCallback = {
    _ = KeychainHelper.shared.delete()
}
```

### 8. Add Settings navigation
- Add `SettingsRoute` to `NavigationRoutes.kt`
- Add navigate method to `Navigator.kt`
- Add Settings button to `HomeView.kt` header (gear icon)
- Update `ChatViewModel` to re-check key validity after settings change

## Critical Files

| File | Change |
|------|--------|
| `iosApp/iosApp/KeychainHelper.swift` | NEW - Keychain read/write |
| `iosApp/iosApp/iOSApp.swift` | Read Keychain before initKoin, register callbacks |
| `composeApp/src/iosMain/kotlin/core/Platform.apple.kt` | Read from ApiKeyManager |
| `composeApp/src/commonMain/kotlin/core/ApiKeyManager.kt` | NEW - in-memory key store |
| `composeApp/src/iosMain/kotlin/core/KeychainBridge.kt` | NEW - callback bridge |
| `composeApp/src/commonMain/kotlin/core/Platform.kt` | Add expect saveApiKeyPlatform/delete |
| `composeApp/src/androidMain/kotlin/core/Platform.android.kt` | Add no-op actuals |
| `composeApp/src/commonMain/kotlin/settings/SettingsView.kt` | NEW - Settings UI |
| `composeApp/src/commonMain/kotlin/core/NavigationRoutes.kt` | Add SettingsRoute |
| `composeApp/src/commonMain/kotlin/core/Navigator.kt` | Add navigateToSettings |
| `composeApp/src/commonMain/kotlin/home/HomeView.kt` | Add gear icon to header |

## Verification
1. **Build iOS**: `./gradlew :composeApp:linkPodReleaseFrameworkIosSimulatorArm64`
2. **Run on simulator**: Open Xcode, run app
3. **Test flow**:
   - Open app → chat shows "API key missing" modal
   - Tap Settings → enter API key → save
   - Go to chat → send message → should work
   - Kill and relaunch app → key should persist (loaded from Keychain)
   - Go to settings → delete key → chat should show modal again
4. **Android**: Verify no regression (no-op save/delete, still reads BuildConfig)
