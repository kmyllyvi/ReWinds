package core

/**
 * Bridge between Kotlin and Swift for Visual Crossing Keychain operations.
 * Swift code registers callbacks here at app startup.
 * Kotlin code calls these functions when the user saves/deletes Weather API key.
 */
object WeatherKeychainBridge {
    var saveKeyCallback: ((String) -> Unit)? = null
    var deleteKeyCallback: (() -> Unit)? = null

    fun saveKey(key: String) {
        saveKeyCallback?.invoke(key)
    }

    fun deleteKey() {
        deleteKeyCallback?.invoke()
    }
}
