package settings

import ai.AnthropicClient
import ai.DaysOfInterestParser
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.AppSettingsRepository
import core.DaysOfInterestFilter
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

class SettingsViewModel(
    private val settingsRepo: AppSettingsRepository,
    private val anthropicClient: AnthropicClient
) : ViewModel() {

    private val _doiState = MutableStateFlow<DaysOfInterestUiState>(DaysOfInterestUiState.Idle)
    val doiState: StateFlow<DaysOfInterestUiState> = _doiState.asStateFlow()

    private val _currentFilter = MutableStateFlow<DaysOfInterestFilter?>(null)
    val currentFilter: StateFlow<DaysOfInterestFilter?> = _currentFilter.asStateFlow()

    init {
        loadSavedFilter()
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

    companion object {
        const val FILTER_KEY = "days_of_interest_filter"
    }
}
