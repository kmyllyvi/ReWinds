package core

/**
 * iOS-specific wrapper to expose Visual Crossing Keychain operations to Swift.
 * These functions are called from iOSApp.swift.
 */

fun setWeatherApiKeyFromKeychain(key: String) {
    WeatherApiKeyManager.setApiKey(key)
}

fun registerWeatherKeychainCallbacks(
    onSave: (String) -> Unit,
    onDelete: () -> Unit
) {
    WeatherKeychainBridge.saveKeyCallback = onSave
    WeatherKeychainBridge.deleteKeyCallback = onDelete
}
