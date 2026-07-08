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
import core.platformName
import core.sendEmail
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
 * Outcome of an Anthropic key save/delete, so the Settings dialog can render inline
 * validation and success feedback without owning any logic (KIM-252). [messageKey] is a
 * stable, locale-independent token the View maps to a localized string.
 */
sealed class AnthropicKeySaveState {
    /** No save attempted yet, or the field is being edited again. */
    object Idle : AnthropicKeySaveState()

    /** Save was rejected because the field was blank. */
    object EmptyError : AnthropicKeySaveState()

    /** A non-blank key was persisted successfully. */
    object Saved : AnthropicKeySaveState()
}

/**
 * Persistence seam for the Anthropic API key. Production binds the platform Keychain
 * (iOS) / SharedPreferences (Android) store; tests substitute an in-memory fake so the
 * save/validate flow is exercised without touching the device (KIM-252).
 */
interface AnthropicKeyStore {
    fun save(key: String)
    fun delete()
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
    private val apiKeyChecker: ApiKeyChecker = PlatformApiKeyChecker,
    /**
     * Anthropic key persistence seam. Defaults to the platform Keychain/SharedPreferences
     * store; injectable so the save/validate flow is testable without a device (KIM-252).
     */
    private val anthropicKeyStore: AnthropicKeyStore = core.PlatformAnthropicKeyStore,
    /**
     * Email launcher seam. Defaults to the platform [sendEmail]; injectable so feedback
     * assembly is unit-testable without opening a real mail client (KIM-334).
     */
    private val emailSender: (recipient: String, subject: String, body: String) -> Unit =
        ::sendEmail,
    /** Platform label for the feedback body. Injectable for deterministic tests. */
    private val platformLabel: () -> String = ::platformName
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

    // Result of the last Anthropic key save/delete: drives inline validation + success text
    // in the dialog. Owned here so the composable stays pure render (KIM-252).
    private val _anthropicKeySaveState = MutableStateFlow<AnthropicKeySaveState>(AnthropicKeySaveState.Idle)
    val anthropicKeySaveState: StateFlow<AnthropicKeySaveState> = _anthropicKeySaveState.asStateFlow()

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

    /**
     * Validates and persists the Anthropic API key (KIM-252). A blank field is rejected with
     * [AnthropicKeySaveState.EmptyError] and nothing is written; a non-blank key is stored via
     * the key seam and reported as [AnthropicKeySaveState.Saved]. Returns true on a successful
     * save so the View can close the dialog only when the key was actually persisted.
     */
    fun saveAnthropicKey(key: String): Boolean {
        if (key.isBlank()) {
            _anthropicKeySaveState.value = AnthropicKeySaveState.EmptyError
            return false
        }
        anthropicKeyStore.save(key.trim())
        _anthropicKeySaveState.value = AnthropicKeySaveState.Saved
        refreshKeyStatus()
        return true
    }

    /** Removes the stored Anthropic key and clears any prior save feedback. */
    fun deleteAnthropicKey() {
        anthropicKeyStore.delete()
        _anthropicKeySaveState.value = AnthropicKeySaveState.Idle
        refreshKeyStatus()
    }

    /** Clears save feedback, e.g. when the field is edited again or the dialog reopens. */
    fun resetAnthropicKeySaveState() {
        _anthropicKeySaveState.value = AnthropicKeySaveState.Idle
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

    /**
     * Opens the device email client pre-addressed to the feedback inbox (KIM-334). Localized
     * subject/body intro are supplied by the View (from AppStrings); the ViewModel appends the
     * app version + platform for beta triage. Body assembly is delegated to [buildFeedbackBody]
     * so it can be unit-tested.
     */
    fun onSendFeedbackClicked(subject: String, bodyIntro: String, appVersion: String) {
        val body = buildFeedbackBody(bodyIntro, appVersion, platformLabel())
        emailSender(FEEDBACK_EMAIL, subject, body)
    }

    fun toggleWifiOnly() {
        _wifiOnlyEnabled.value = !_wifiOnlyEnabled.value
    }

    companion object {
        const val FILTER_KEY = "days_of_interest_filter"
        const val WIND_SPEED_UNIT_KEY = "wind_speed_unit"
        const val FEEDBACK_EMAIL = "apps@goaheadand.dev"

        /**
         * Assembles the feedback email body: the localized intro followed by app version and
         * platform diagnostics on their own lines. Pure so it's directly unit-testable.
         *
         * Line breaks use CRLF: once percent-encoded into the `mailto:` body (iOS), RFC 6068
         * calls for `%0D%0A`, and email composers render it consistently.
         */
        fun buildFeedbackBody(bodyIntro: String, appVersion: String, platform: String): String =
            buildString {
                append(bodyIntro)
                append(CRLF)
                append(CRLF)
                append("---")
                append(CRLF)
                append("App version: ")
                append(appVersion)
                append(CRLF)
                append("Platform: ")
                append(platform)
            }

        private const val CRLF = "\r\n"
    }
}
