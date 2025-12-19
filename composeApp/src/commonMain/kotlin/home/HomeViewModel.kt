package home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.GeoSearchResult
import core.NetworkException
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
 */
data class HomeUiState(
    val placeDisplayData: List<PlaceDisplayData> = emptyList(),
    val searchResults: List<GeoSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val placeToDelete: String? = null
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
 */
@OptIn(FlowPreview::class)
class HomeViewModel(private val weatherRepository: WeatherRepository) : ViewModel() {

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
            } catch (e: NetworkException) {
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
     * Called when the delete confirmation is confirmed.
     */
    fun onDeleteConfirmed() {
        _uiState.value.placeToDelete?.let {
            viewModelScope.launch {
                weatherRepository.deletePlace(it)
                loadSavedPlaces()
                _uiState.update { it.copy(showDeleteConfirmation = false, placeToDelete = null) }
            }
        }
    }
}
