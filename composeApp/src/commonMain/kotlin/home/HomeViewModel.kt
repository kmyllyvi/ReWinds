package home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.GeoSearchResult
import core.NetworkException
import core.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.FlowPreview
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

data class PlaceDisplayData(
    val name: String,
    val dayCount: Int
)

data class HomeUiState(
    val placeDisplayData: List<PlaceDisplayData> = emptyList(),
    val searchResults: List<GeoSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val placeToDelete: String? = null
)

sealed class NavigationEvent {
    data class ToPlaceSummary(val placeName: String) : NavigationEvent()
}

@OptIn(FlowPreview::class)
class HomeViewModel(private val weatherRepository: WeatherRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
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

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

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

    fun onSavedPlaceSelected(placeName: String) {
        viewModelScope.launch {
            _navigationEvent.send(NavigationEvent.ToPlaceSummary(placeName))
        }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    fun onDeleteRequest(placeName: String) {
        _uiState.update { it.copy(showDeleteConfirmation = true, placeToDelete = placeName) }
    }

    fun onDeleteCancelled() {
        _uiState.update { it.copy(showDeleteConfirmation = false, placeToDelete = null) }
    }

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
