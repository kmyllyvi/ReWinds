package core

/**
 * Manages the Anthropic API key in memory.
 * On iOS, this is initialized from Keychain at app startup.
 * On Android, this reads from BuildConfig.
 * At runtime, users can update it via the Settings screen.
 */
object ApiKeyManager {
    private var _apiKey: String = ""

    fun setApiKey(key: String) {
        _apiKey = key
        Log.d("ApiKeyManager: API key updated")
    }

    fun getApiKey(): String = _apiKey

    fun hasValidKey(): Boolean {
        return _apiKey.isNotBlank() &&
               !_apiKey.contains("placeholder") &&
               _apiKey.startsWith("sk-ant-")
    }
}
