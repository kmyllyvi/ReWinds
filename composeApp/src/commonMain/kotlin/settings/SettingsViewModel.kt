package settings

import ai.AnthropicClient
import ai.DaysOfInterestParser
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.ApiKeyChecker
import core.AppSettingsStore
import core.DaysOfInterestFilter
import core.Language
import core.LanguageManager
import core.PlatformApiKeyChecker
import core.WeatherApiKeyManager
import core.utils.WindSpeedUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class DaysOfInterestUiState {
    object Idle : DaysOfInterestUiState()
    object Parsing : DaysOfInterestUiState()
    data class Success(val filter: DaysOfInterestFilter) : DaysOfInterestUiState()
    data class Error(val message: String) : DaysOfInterestUiState()
}

/**
 * Measurement system for temperature. Retained for a future temperature-conversion ticket;
 * the Settings row is hidden until then, since temperature is always shown in °C (KIM-330).
 */
enum class UnitSystem(val displayName: String) {
    METRIC("Metric"),
    IMPERIAL("Imperial")
}

class SettingsViewModel(
    private val settingsRepo: AppSettingsStore,
    private val anthropicClient: AnthropicClient,
    /** Anthropic key gate. Injectable so the Configured chip is testable without the platform store (J10). */
    private val apiKeyChecker: ApiKeyChecker = PlatformApiKeyChecker
) : ViewModel() {

    private val _doiState = MutableStateFlow<DaysOfInterestUiState>(DaysOfInterestUiState.Idle)
    val doiState: StateFlow<DaysOfInterestUiState> = _doiState.asStateFlow()

    private val _currentFilter = MutableStateFlow<DaysOfInterestFilter?>(null)
    val currentFilter: StateFlow<DaysOfInterestFilter?> = _currentFilter.asStateFlow()

    // ── General group state ──────────────────────────────────────────────────
    // Language mirrors the global LanguageManager so the row reflects the live choice.
    private val _languageState = MutableStateFlow(LanguageManager.currentLanguage.value)
    val languageState: StateFlow<Language> = _languageState.asStateFlow()

    private val _windSpeedUnitState = MutableStateFlow(WindSpeedUnit.KMH)
    val windSpeedUnitState: StateFlow<WindSpeedUnit> = _windSpeedUnitState.asStateFlow()

    // ── API Keys group state ─────────────────────────────────────────────────
    // Configured flags are exposed as state so the chip is driven by the ViewModel,
    // not computed inline in the composable (per ARCHITECTURE-RULES + AC).
    private val _anthropicKeyConfigured = MutableStateFlow(false)
    val anthropicKeyConfigured: StateFlow<Boolean> = _anthropicKeyConfigured.asStateFlow()

    private val _visualCrossingKeyConfigured = MutableStateFlow(false)
    val visualCrossingKeyConfigured: StateFlow<Boolean> = _visualCrossingKeyConfigured.asStateFlow()

    // ── Data group state ─────────────────────────────────────────────────────
    private val _autoRefreshEnabled = MutableStateFlow(true)
    val autoRefreshEnabled: StateFlow<Boolean> = _autoRefreshEnabled.asStateFlow()

    private val _wifiOnlyEnabled = MutableStateFlow(false)
    val wifiOnlyEnabled: StateFlow<Boolean> = _wifiOnlyEnabled.asStateFlow()

    init {
        loadSavedFilter()
        loadWindSpeedUnit()
        refreshKeyStatus()
    }

    /** Restores the persisted wind-speed unit; falls back to km/h when absent or unrecognised. */
    private fun loadWindSpeedUnit() {
        val saved = settingsRepo.getString(WIND_SPEED_UNIT_KEY) ?: return
        _windSpeedUnitState.value =
            WindSpeedUnit.entries.firstOrNull { it.name == saved } ?: WindSpeedUnit.KMH
    }

    private fun loadSavedFilter() {
        val jsonStr = settingsRepo.getString(FILTER_KEY) ?: return
        try {
            _currentFilter.value = Json.decodeFromString<DaysOfInterestFilter>(jsonStr)
        } catch (_: Exception) {}
    }

    fun saveFilter(criteria: String) {
        if (criteria.isBlank()) return
        viewModelScope.launch {
            _doiState.value = DaysOfInterestUiState.Parsing
            try {
                val filter = DaysOfInterestParser.parse(criteria, anthropicClient)
                val jsonStr = Json.encodeToString(filter)
                settingsRepo.setString(FILTER_KEY, jsonStr)
                _currentFilter.value = filter
                _doiState.value = DaysOfInterestUiState.Success(filter)
            } catch (e: Exception) {
                _doiState.value = DaysOfInterestUiState.Error(e.message ?: "Failed to parse criteria")
            }
        }
    }

    fun resetDoiState() {
        _doiState.value = DaysOfInterestUiState.Idle
    }

    /** Re-reads platform key stores into the configured flags. Call after save/delete. */
    fun refreshKeyStatus() {
        _anthropicKeyConfigured.value = apiKeyChecker.isAnthropicKeyConfigured()
        _visualCrossingKeyConfigured.value = WeatherApiKeyManager.hasValidKey()
    }

    fun setLanguage(language: Language) {
        LanguageManager.setLanguage(language)
        _languageState.value = language
    }

    fun setWindSpeedUnit(unit: WindSpeedUnit) {
        _windSpeedUnitState.value = unit
        settingsRepo.setString(WIND_SPEED_UNIT_KEY, unit.name)
    }

    fun setAutoRefreshEnabled(enabled: Boolean) {
        _autoRefreshEnabled.value = enabled
    }

    fun toggleAutoRefresh() {
        _autoRefreshEnabled.value = !_autoRefreshEnabled.value
    }

    fun setWifiOnlyEnabled(enabled: Boolean) {
        _wifiOnlyEnabled.value = enabled
    }

    fun toggleWifiOnly() {
        _wifiOnlyEnabled.value = !_wifiOnlyEnabled.value
    }

    companion object {
        const val FILTER_KEY = "days_of_interest_filter"
        const val WIND_SPEED_UNIT_KEY = "wind_speed_unit"
    }
}
