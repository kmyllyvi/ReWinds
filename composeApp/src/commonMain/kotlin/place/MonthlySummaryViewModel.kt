package place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MonthlySummaryViewModel(
    val placeName: String,
    val year: Int,
    val month: Int
) : ViewModel(), KoinComponent {

    private val weatherRepository: WeatherRepository by inject()

    private val _uiState = MutableStateFlow<WeatherSummaryUiState>(WeatherSummaryUiState.Loading)
    val uiState: StateFlow<WeatherSummaryUiState> = _uiState.asStateFlow()

    init {
        loadMonthlyData()
    }

    private fun loadMonthlyData() {
        viewModelScope.launch {
            _uiState.value = WeatherSummaryUiState.Loading
            try {
                val weatherData = weatherRepository.getSavedDataFor(placeName)
                if (weatherData != null) {
                    val monthlyData = weatherData.days?.filter {
                        val dateParts = it.datetime.split("-")
                        dateParts.size == 3 && dateParts[0].toInt() == year && dateParts[1].toInt() == month
                    } ?: emptyList()
                    _uiState.value = WeatherSummaryUiState.Success(
                        placeName = placeName,
                        currentPlaceDescription = weatherData.resolvedAddress,
                        storedDays = monthlyData,
                        availableYears = emptyList() // Not needed for this view
                    )
                } else {
                    _uiState.value = WeatherSummaryUiState.Error("No data found for $placeName")
                }
            } catch (e: Exception) {
                _uiState.value = WeatherSummaryUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}
