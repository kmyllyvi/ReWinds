# Development Session - March 6, 2026

## Summary
Fixed iOS Keychain API key storage implementation. The infrastructure was in place but not wired up properly. Now fully functional with build-time configuration support.

## Issues Fixed

### 1. Duplicate Symbol Linker Error (528 symbols)
**Problem**: Xcode was linking `ComposeApp.framework` twice:
- Directly via project's Frameworks build phase
- Via CocoaPods linker flags (`-framework "composeApp"`)

**Solution**:
- Removed manual `ComposeApp.framework` reference from `iosApp.xcodeproj/project.pbxproj`
- Let CocoaPods handle all framework linkage
- Verified with `Pods-iosApp.debug.xcconfig` which defines `-framework "composeApp"`

**Files Modified**:
- `iosApp/iosApp.xcodeproj/project.pbxproj` - Removed duplicate framework references

### 2. iOS Keychain Save Not Working
**Problem**: `saveApiKeyPlatform()` was just a TODO stub on iOS. Debug logs showed:
```
Platform: saveApiKeyPlatform not yet implemented on iOS
Platform: Using placeholder API key for development on iOS
```

**Root Cause**: Implementation incomplete - all infrastructure existed but not connected:
- ✅ `KeychainHelper.swift` - Fully implemented Keychain read/write
- ✅ `ApiKeyManager.kt` - In-memory API key storage
- ✅ `KeychainBridge.kt` - Kotlin↔Swift callback bridge
- ❌ `iOSApp.swift` - Didn't load from Keychain or register callbacks
- ❌ `Platform.apple.kt` - `saveApiKeyPlatform()` was stub

**Solution**:
1. Created `IosKeychain.kt` wrapper functions in `iosMain`:
   - `setApiKeyFromKeychain(key)` - Updates ApiKeyManager
   - `registerKeychainCallbacks(onSave, onDelete)` - Registers Swift callbacks

2. Updated `iOSApp.swift` to:
   - Load API key from Keychain before `initKoin()`
   - Register callbacks via `IosKeychainKt` wrapper functions

3. Updated `Platform.apple.kt`:
   - `getAnthropicApiKey()` - Reads from `ApiKeyManager` instead of placeholder
   - `saveApiKeyPlatform()` - Calls `ApiKeyManager.setApiKey()` + `KeychainBridge.saveKey()`
   - `deleteApiKeyPlatform()` - Calls `ApiKeyManager.setApiKey("")` + `KeychainBridge.deleteKey()`

**Why the wrapper was needed**: Direct object access from Swift (`ApiKeyManagerKt.ApiKeyManager`) wasn't working for `commonMain` code. Top-level functions in `iosMain` are properly exposed to Swift as `IosKeychainKt.functionName()`.

**Files Modified**:
- `iosApp/iosApp/iOSApp.swift` - Load Keychain + register callbacks
- `composeApp/src/iosMain/kotlin/core/IosKeychain.kt` - NEW wrapper functions
- `composeApp/src/iosMain/kotlin/core/Platform.apple.kt` - Implement save/delete/read

## Build-Time Configuration (API Key in Xcode Build)

Added optional build-time API key support, parallel to Android's `gradle.properties`:

### Setup
1. Created `iosApp/Config.local.xcconfig` (gitignored):
   ```xcconfig
   ANTHROPIC_API_KEY = sk-ant-your-key-here
   ```

2. Updated `iosApp/Configuration/Config.xcconfig`:
   ```xcconfig
   #include "Config.local.xcconfig"?
   ```

3. Added to `iosApp/iosApp/Info.plist`:
   ```xml
   <key>ANTHROPIC_API_KEY</key>
   <string>$(ANTHROPIC_API_KEY)</string>
   ```

4. Updated `iOSApp.swift` startup priority:
   - **Priority 1**: Read from Info.plist (build config)
   - **Priority 2**: Load from Keychain (user entered in Settings)

### Key Priority Logic
```swift
// Try build config first
if buildConfigKey exists && valid {
    use buildConfigKey
}
// Fall back to Keychain
else if keychainKey exists {
    use keychainKey
}
// Otherwise placeholder
else {
    use placeholder key
}
```

**Files Created**:
- `iosApp/Config.local.xcconfig` - Local development config (gitignored)

**Files Modified**:
- `iosApp/Configuration/Config.xcconfig` - Include local config
- `iosApp/iosApp/Info.plist` - Add API key bundle entry
- `.gitignore` - Protect Config.local.xcconfig
- `iosApp/iosApp/iOSApp.swift` - Priority-based key loading

## Testing

### Keychain Storage
1. Build and run app with Xcode
2. Open Settings → Enter API key → Save
3. Kill and restart app
4. Verify: Key loads from Keychain automatically
5. Chat feature works without re-entering key

### Build-Time Config
1. Add key to `iosApp/Config.local.xcconfig`
2. Clean build folder (**Cmd+Shift+K**)
3. Rebuild (**Cmd+B**)
4. Run app
5. Verify: Key is available at startup (no Settings entry needed)

## Key Learnings

1. **Kotlin/Native exposure to Swift**: Objects in `commonMain` aren't reliably exposed. Use wrapper functions in `iosMain` instead.

2. **CocoaPods integration**: Manual framework linkage conflicts with Pod configuration. Let the tool handle it.

3. **iOS build path**: Must use Xcode (`open iosApp.xcworkspace`), not Gradle. Gradle iOS tasks are unreliable without full Xcode integration.

4. **Build config hierarchy**: Similar to Android but via Info.plist + xcconfig files instead of BuildConfig.

## Status
✅ iOS Keychain integration fully working
✅ Build-time configuration optional support added
✅ No duplicate symbol errors
✅ Secure API key storage on iOS
✅ Ready for Settings UI to become visible

## Next Steps
- Users can now configure API keys either:
  1. Via Settings screen (persists in Keychain)
  2. Via build config (embedded at compile time)
- Both methods work seamlessly with fallback priority logic
