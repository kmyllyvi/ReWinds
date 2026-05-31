package home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.Database
import core.DatabaseExportImport
import core.GeoSearchResult
import core.Log
import core.NetworkException
import core.WeatherApiKeyManager
import core.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data class representing the display data for a place.
 *
 * @property name The name of the place.
 * @property dayCount The number of days for which weather data is available.
 */
data class PlaceDisplayData(
    val name: String,
    val dayCount: Int
)

/**
 * Data class representing the UI state of the home screen.
 *
 * @property placeDisplayData The list of places to display.
 * @property searchResults The list of search results.
 * @property isSearching Whether a search is in progress.
 * @property error A string containing an error message, if any.
 * @property showDeleteConfirmation Whether to show the delete confirmation dialog.
 * @property placeToDelete The name of the place to be deleted.
 * @property showDebugMenu Whether to show the debug menu.
 * @property debugMessage The current debug message.
 */
/**
 * Distinguishes Visual Crossing key problems from generic network errors so the UI
 * can offer a targeted "Go to Settings" CTA rather than a raw error message.
 */
enum class VcKeyErrorType { MISSING, INVALID }

data class HomeUiState(
    val placeDisplayData: List<PlaceDisplayData> = emptyList(),
    val searchResults: List<GeoSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val vcKeyError: VcKeyErrorType? = null,
    val showDeleteConfirmation: Boolean = false,
    val placeToDelete: String? = null,
    val showDebugMenu: Boolean = false,
    val debugMessage: String = "",
    val importFilePath: String = ""
)

/**
 * Sealed class representing navigation events.
 */
sealed class NavigationEvent {
    /**
     * Event to navigate to the place summary screen.
     *
     * @property placeName The name of the place.
     */
    data class ToPlaceSummary(val placeName: String) : NavigationEvent()
}

/**
 * ViewModel for the home screen.
 *
 * @param weatherRepository The repository for accessing weather data.
 * @param databaseExportImport The database export/import manager.
 */
@OptIn(FlowPreview::class)
class HomeViewModel(
    private val weatherRepository: WeatherRepository,
    private val databaseExportImport: DatabaseExportImport,
    private val database: Database
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    /**
     * The UI state of the home screen.
     */
    val uiState = _uiState.asStateFlow()

    private val _searchText = MutableStateFlow("")
    /**
     * The current search text.
     */
    val searchText = _searchText.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    /**
     * A flow of navigation events.
     */
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadSavedPlaces()

        viewModelScope.launch(Dispatchers.IO) {
            // Cleanup any forecast data on app startup (keep only historical data up to yesterday)
//            try {
//                database.cleanupForecastDays()
//            } catch (e: Exception) {
//                Log.e("Failed to cleanup forecast days", e)
//            }
        }

        viewModelScope.launch {
            searchText
                .debounce(500L)
                .onEach { _uiState.update { it.copy(isSearching = true) } }
                .map {
                    if (it.isNotBlank()) {
                        weatherRepository.searchForLocations(it)
                    } else {
                        emptyList()
                    }
                }
                .onEach { _uiState.update { it.copy(isSearching = false) } }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    _uiState.value.searchResults
                ).collect { results ->
                    _uiState.update { it.copy(searchResults = results) }
                }
        }
    }

    private fun loadSavedPlaces() {
        viewModelScope.launch {
            val placeNames = weatherRepository.getSavedPlaceNames()
            val displayData = placeNames.map { name ->
                val data = weatherRepository.getSavedDataFor(name)
                PlaceDisplayData(name, data?.days?.size ?: 0)
            }
            _uiState.update { it.copy(placeDisplayData = displayData) }
        }
    }

    /**
     * Called when the search text changes.
     *
     * @param text The new search text.
     */
    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    /**
     * Called when a search result is selected.
     *
     * @param place The selected search result.
     */
    fun onSearchResultSelected(place: GeoSearchResult) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchResults = emptyList()) }
            _searchText.value = ""
            try {
                withContext(Dispatchers.IO) {
                    weatherRepository.addPlaceFromSearch(place)
                }
                loadSavedPlaces()
            } catch (e: IllegalStateException) {
                // WeatherRepository throws this when the VC key is absent
                _uiState.update { it.copy(vcKeyError = VcKeyErrorType.MISSING) }
            } catch (e: NetworkException) {
                if (e.httpStatus == 401 || e.httpStatus == 403) {
                    _uiState.update { it.copy(vcKeyError = VcKeyErrorType.INVALID) }
                } else {
                    _uiState.update { it.copy(error = e.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(isSearching = false) }
        }
    }

    /**
     * Called when a saved place is selected.
     *
     * @param placeName The name of the selected place.
     */
    fun onSavedPlaceSelected(placeName: String) {
        viewModelScope.launch {
            _navigationEvent.send(NavigationEvent.ToPlaceSummary(placeName))
        }
    }

    /**
     * Called when the error dialog is dismissed.
     */
    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Called when the VC key error banner is dismissed.
     */
    fun onVcKeyErrorDismissed() {
        _uiState.update { it.copy(vcKeyError = null) }
    }

    /**
     * Called when a delete request is made.
     *
     * @param placeName The name of the place to delete.
     */
    fun onDeleteRequest(placeName: String) {
        _uiState.update { it.copy(showDeleteConfirmation = true, placeToDelete = placeName) }
    }

    /**
     * Called when the delete confirmation is cancelled.
     */
    fun onDeleteCancelled() {
        _uiState.update { it.copy(showDeleteConfirmation = false, placeToDelete = null) }
    }

    /**
     *
     * Called when the delete confirmation is confirmed.
     */
    fun onDeleteConfirmed() {
        _uiState.value.placeToDelete?.let { placeToDelete ->
            viewModelScope.launch {
                weatherRepository.deletePlace(placeToDelete)
                loadSavedPlaces()
                _uiState.update { it.copy(showDeleteConfirmation = false, placeToDelete = null) }
            }
        }
    }

    /**
     * Toggle the debug menu visibility.
     */
    fun toggleDebugMenu() {
        _uiState.update { it.copy(showDebugMenu = !it.showDebugMenu) }
    }

    /**
     * Export the database to an accessible location.
     */
    fun exportDatabase() {
        viewModelScope.launch {
            val result = databaseExportImport.exportDatabase()
            result.onSuccess { message ->
                _uiState.update { it.copy(debugMessage = "✅ $message") }
            }
            result.onFailure { exception ->
                _uiState.update { it.copy(debugMessage = "❌ Export failed: ${exception.message}") }
            }
        }
    }

    /**
     * List available backup files.
     */
    fun listBackups() {
        viewModelScope.launch {
            val result = databaseExportImport.listBackups()
            result.onSuccess { backups ->
                val message = if (backups.isEmpty()) {
                    "No backups found"
                } else {
                    "📋 Found ${backups.size} backups:\n${backups.joinToString("\n")}"
                }
                _uiState.update { it.copy(debugMessage = message) }
            }
            result.onFailure { exception ->
                _uiState.update { it.copy(debugMessage = "❌ List failed: ${exception.message}") }
            }
        }
    }

    /**
     * Update the import file path input.
     */
    fun onImportFilePathChange(path: String) {
        _uiState.update { it.copy(importFilePath = path) }
    }

    /**
     * Import database from the specified file path.
     */
    fun importDatabase() {
        val filePath = _uiState.value.importFilePath
        if (filePath.isBlank()) {
            _uiState.update { it.copy(debugMessage = "❌ Please enter a file path") }
            return
        }

        viewModelScope.launch {
            val result = databaseExportImport.importDatabase(filePath)
            result.onSuccess { message ->
                _uiState.update { it.copy(debugMessage = "📥 $message", importFilePath = "") }
            }
            result.onFailure { exception ->
                _uiState.update { it.copy(debugMessage = "❌ Import failed: ${exception.message}") }
            }
        }
    }
}
