package core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the Visual Crossing API key in memory.
 * On iOS, this is initialized from Keychain at app startup.
 * On Android, this reads from BuildConfig.
 * At runtime, users can update it via the Settings screen.
 */
object WeatherApiKeyManager {
    private var _apiKey: String = ""

    private val _hasValidKey = MutableStateFlow(false)
    /**
     * Reactive view of [hasValidKey]. Emits on every [setApiKey] so observers (e.g. the
     * KIM-309 onboarding gate) react to a saved key in the same session without polling.
     */
    val hasValidKeyFlow: StateFlow<Boolean> = _hasValidKey.asStateFlow()

    fun setApiKey(key: String) {
        _apiKey = key
        _hasValidKey.value = computeHasValidKey()
        Log.d("WeatherApiKeyManager: Weather API key updated")
    }

    fun getApiKey(): String = _apiKey

    fun hasValidKey(): Boolean = computeHasValidKey()

    private fun computeHasValidKey(): Boolean {
        return _apiKey.isNotBlank() &&
               !_apiKey.contains("placeholder")
    }
}
