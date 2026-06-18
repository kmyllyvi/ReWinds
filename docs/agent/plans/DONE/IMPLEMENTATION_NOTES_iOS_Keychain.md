# Implementation Notes: iOS Keychain-Based API Key Storage

**Date**: March 4, 2026
**Commit**: `8e7ea87 - Feature: Implement iOS Keychain-based API key storage with Settings screen`

## Overview

Implemented secure API key storage for iOS using the native Keychain. This allows users to enter their Anthropic API key once via a Settings screen, and have it securely persist across app restarts without needing to modify source code.

## Problem Statement

Previously:
- **iOS**: Returned hardcoded placeholder API key, making chat feature unusable without modifying source
- **Android**: Used BuildConfig (gradle.properties at build time)
- **Users**: Had to rebuild app for each key change, security risk of storing keys in source

## Solution: Multi-Layer Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ iOS App Startup                                             │
│ 1. iOSApp.swift initializes                                │
│ 2. Reads Keychain via KeychainHelper.swift                 │
│ 3. Sets key in ApiKeyManager                               │
│ 4. Registers save/delete callbacks                          │
│ 5. Initializes Koin                                        │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ Runtime: User navigates to Settings                         │
│ 1. Settings gear icon in home header                       │
│ 2. Navigate to SettingsView                                │
│ 3. User enters API key (masked input)                      │
│ 4. Save button triggers:                                   │
│    a. ApiKeyManager.setApiKey(key)                         │
│    b. KeychainBridge.saveKey(key) → Swift callback         │
│    c. KeychainHelper.save(key) to Keychain                 │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ App Restart/Chat Usage                                     │
│ 1. Key automatically loaded from Keychain at startup       │
│ 2. Set in ApiKeyManager before Koin init                  │
│ 3. Platform.apple.kt returns key from ApiKeyManager        │
│ 4. AnthropicClient uses key for API calls                 │
└─────────────────────────────────────────────────────────────┘
```

## Implementation Details

### 1. ApiKeyManager.kt (NEW - commonMain)

In-memory key storage accessible by all platform implementations:

```kotlin
object ApiKeyManager {
    private var _apiKey: String = ""

    fun setApiKey(key: String)
    fun getApiKey(): String
    fun hasValidKey(): Boolean
}
```

**Used by**:
- Platform implementations
- Settings screen to save key
- Chat VM to check if configured

### 2. KeychainHelper.swift (NEW - iosApp)

Native Swift wrapper around Security.framework:

```swift
class KeychainHelper {
    func save(_ key: String) -> Bool        // SecItemAdd/Update
    func load() -> String?                  // SecItemCopyMatching
    func delete() -> Bool                   // SecItemDelete
}
```

**Features**:
- No external dependencies (Security framework included by default)
- kSecAttrAccessibleWhenUnlockedThisDeviceOnly for security
- Graceful handling of missing keys

### 3. KeychainBridge.kt (NEW - iosMain)

Callback bridge for Kotlin↔Swift communication:

```kotlin
object KeychainBridge {
    var saveKeyCallback: ((String) -> Unit)? = null
    var deleteKeyCallback: (() -> Unit)? = null

    fun saveKey(key: String)
    fun deleteKey()
}
```

**Lifecycle**:
1. iOSApp.swift registers callbacks at startup
2. SettingsView calls KeychainBridge functions
3. Callbacks invoke Swift Keychain operations

### 4. Platform Functions (expect/actual pattern)

**Platform.kt (commonMain)**:
```kotlin
expect fun saveApiKeyPlatform(key: String)
expect fun deleteApiKeyPlatform()
```

**Platform.apple.kt**:
```kotlin
actual fun saveApiKeyPlatform(key: String) {
    ApiKeyManager.setApiKey(key)
    KeychainBridge.saveKey(key)
}

actual fun deleteApiKeyPlatform() {
    ApiKeyManager.setApiKey("")
    KeychainBridge.deleteKey()
}
```

**Platform.android.kt**:
```kotlin
actual fun saveApiKeyPlatform(key: String) {
    // No-op: Android uses BuildConfig at build time
}

actual fun deleteApiKeyPlatform() {
    // No-op
}
```

### 5. SettingsView.kt (NEW - commonMain)

Compose UI screen with:
- **Status indicator**: Shows if key is configured or not
- **Input field**: Masked text input (PasswordVisualTransformation)
- **Save button**: Calls saveApiKeyPlatform()
- **Delete button**: Calls deleteApiKeyPlatform() with confirmation dialog
- **Help link**: "Get your API key from https://console.anthropic.com/account/keys"
- **Success message**: Shows after successful save

### 6. Navigation Integration

- Add SettingsRoute to NavigationRoutes.kt
- Add navigateToSettings() to Navigator interface
- Implement in NavigatorImpl
- Add SettingsView case to Router.kt when statement
- Add Settings gear icon button to HomeView header

### 7. iOSApp.swift Integration

At app startup:

```swift
init() {
    // Load API key from Keychain BEFORE Koin init
    if let savedKey = KeychainHelper.shared.load() {
        ApiKeyManagerKt.ApiKeyManager.setApiKey(savedKey)
    }

    // Register callbacks for future save/delete operations
    KeychainBridgeKt.KeychainBridge.saveKeyCallback = { key in
        _ = KeychainHelper.shared.save(key)
    }
    KeychainBridgeKt.KeychainBridge.deleteKeyCallback = {
        _ = KeychainHelper.shared.delete()
    }

    // Now initialize Koin (which uses getAnthropicApiKey from Platform)
    let driverFactory = DatabaseDriverFactory()
    DIKt.doInitKoin(databaseDriverFactory: driverFactory)
    IosUtilsKt.doInitLogger()
}
```

## User Experience

### First-Time Setup
1. User launches app
2. Taps Settings gear icon (top-right of home screen)
3. Navigates to Settings screen
4. See: "⚠ API key not configured"
5. Enter API key in text field (masked)
6. Tap "Save Key"
7. See: "✓ API key saved successfully"
8. Back to home, click Chat
9. Chat now works!

### Subsequent Uses
1. API key automatically loaded from Keychain at app launch
2. No need to re-enter
3. Settings screen shows: "✓ API key is configured"

### Deleting/Changing Key
1. Go to Settings
2. Tap "Delete" button
3. Confirm deletion
4. Enter new key and save
5. App uses new key immediately

## Test Fixes

Updated mock WeatherRepository implementations to include the `checkDataAvailability` method:
- `WeatherToolsMetricsTest.kt`: Added mock implementation returning `DataAvailabilityStatus.Available`
- `HomeViewTest.kt`: Added mock implementation
- `AnthropicModelsTest.kt`: Fixed JSON serialization issue in message structure test

## Security Considerations

1. **Keychain Security**:
   - Stored with `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`
   - Only accessible when device is unlocked
   - Encrypted by iOS security subsystem

2. **No Logging**:
   - API key never logged or printed
   - Only status messages logged (saved, deleted, configured)

3. **No Source Code**:
   - Key not in gradle.properties or source
   - Not transmitted over unencrypted connections

## Android Notes

Android implementation is unchanged:
- Continues to use `BuildConfig.ANTHROPIC_API_KEY` from gradle.properties
- The `saveApiKeyPlatform()` and `deleteApiKeyPlatform()` are no-ops
- Could be enhanced later to use SharedPreferences if runtime key changes needed

## Future Enhancements

1. **Biometric unlock**: Use Face ID/Touch ID before retrieving key
2. **Key validation**: Verify format before saving
3. **Android SharedPreferences**: Support runtime key entry on Android
4. **Settings persistence**: Save other settings to Keychain
5. **Key expiration**: Optional warning if key hasn't been validated in N days

## Files Modified

| File | Change |
|------|--------|
| `composeApp/src/commonMain/kotlin/core/ApiKeyManager.kt` | NEW |
| `composeApp/src/commonMain/kotlin/core/Platform.kt` | Added expect functions |
| `composeApp/src/commonMain/kotlin/core/Navigator.kt` | Added navigateToSettings |
| `composeApp/src/commonMain/kotlin/core/NavigationRoutes.kt` | Added SettingsRoute |
| `composeApp/src/commonMain/kotlin/core/NavigatorImpl.kt` | Added navigateToSettings impl |
| `composeApp/src/commonMain/kotlin/core/Router.kt` | Added SettingsView case |
| `composeApp/src/commonMain/kotlin/settings/SettingsView.kt` | NEW |
| `composeApp/src/iosMain/kotlin/core/Platform.apple.kt` | Use ApiKeyManager, implement save/delete |
| `composeApp/src/iosMain/kotlin/core/KeychainBridge.kt` | NEW |
| `composeApp/src/androidMain/kotlin/core/Platform.android.kt` | Added no-op save/delete |
| `composeApp/src/commonMain/kotlin/home/HomeView.kt` | Add Settings button |
| `iosApp/iosApp/KeychainHelper.swift` | NEW |
| `iosApp/iosApp/iOSApp.swift` | Load Keychain at startup, register callbacks |
| `composeApp/src/commonTest/kotlin/ai/WeatherToolsMetricsTest.kt` | Add checkDataAvailability |
| `composeApp/src/commonTest/kotlin/home/HomeViewTest.kt` | Add checkDataAvailability |
| `composeApp/src/commonTest/kotlin/ai/AnthropicModelsTest.kt` | Fix JSON serialization |

## Testing

### Manual Testing (iOS Simulator)
1. Build: `./gradlew :composeApp:linkPodReleaseFrameworkIosSimulatorArm64`
2. Run in Xcode: Open `iosApp.xcworkspace`, select simulator, Run
3. Tap Settings gear icon
4. Enter: `sk-ant-[valid-test-key]`
5. Tap Save
6. Kill and restart app
7. Settings should show "✓ API key is configured"
8. Chat feature should work

### Automated Tests
1. Run: `./gradlew :composeApp:testDebugUnitTest`
2. All mock repositories now have checkDataAvailability implemented
3. AnthropicModelsTest serialization fixed

## Notes

- This implementation is purely iOS-specific Keychain integration
- Works alongside the permission flow for AI data fetching (separate feature)
- Chat feature now usable immediately on iOS without source code modifications
- Perfect for app store distribution without exposing API keys
