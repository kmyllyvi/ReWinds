package core

/**
 * iOS-specific wrapper to expose Keychain operations to Swift.
 * These functions are called from iOSApp.swift.
 */

fun setApiKeyFromKeychain(key: String) {
    ApiKeyManager.setApiKey(key)
}

fun registerKeychainCallbacks(
    onSave: (String) -> Unit,
    onDelete: () -> Unit
) {
    KeychainBridge.saveKeyCallback = onSave
    KeychainBridge.deleteKeyCallback = onDelete
}
