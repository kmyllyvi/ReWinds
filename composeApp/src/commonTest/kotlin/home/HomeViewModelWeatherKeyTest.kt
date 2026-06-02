package home

import core.WeatherApiKeyManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for KIM-245: VC key nudge banner state in HomeUiState / HomeViewModel.
 *
 * WeatherApiKeyManager is a global object so we reset it between tests.
 */
class HomeViewModelWeatherKeyTest {

    @BeforeTest
    fun setUp() {
        WeatherApiKeyManager.setApiKey("")
    }

    @AfterTest
    fun tearDown() {
        WeatherApiKeyManager.setApiKey("")
    }

    @Test
    fun initialStateReportsKeyNotConfiguredWhenManagerHasNoKey() {
        val state = HomeUiState(isWeatherKeyConfigured = WeatherApiKeyManager.hasValidKey())
        assertFalse(state.isWeatherKeyConfigured)
    }

    @Test
    fun stateReportsKeyConfiguredAfterValidKeyIsSet() {
        WeatherApiKeyManager.setApiKey("valid-key-abc123")
        val state = HomeUiState(isWeatherKeyConfigured = WeatherApiKeyManager.hasValidKey())
        assertTrue(state.isWeatherKeyConfigured)
    }

    @Test
    fun placeholderKeyIsNotConsideredValid() {
        WeatherApiKeyManager.setApiKey("your-placeholder-key")
        val state = HomeUiState(isWeatherKeyConfigured = WeatherApiKeyManager.hasValidKey())
        assertFalse(state.isWeatherKeyConfigured)
    }

    @Test
    fun keyTransitionFalseToTrue() {
        // Starts without a key
        var state = HomeUiState(isWeatherKeyConfigured = WeatherApiKeyManager.hasValidKey())
        assertFalse(state.isWeatherKeyConfigured, "Banner should be shown when no key is present")

        // User saves a key in Settings; ViewModel calls refreshWeatherKeyState()
        WeatherApiKeyManager.setApiKey("valid-key-abc123")
        state = state.copy(isWeatherKeyConfigured = WeatherApiKeyManager.hasValidKey())
        assertTrue(state.isWeatherKeyConfigured, "Banner should disappear after a valid key is saved")
    }
}
