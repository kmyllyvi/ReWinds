package place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.Day
import core.Hour // Import Hour
import core.KiteSpotterConfig
import core.Log
import core.PlaceSummaryRoute
import core.WeatherRepository
import core.WeatherResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.minus

sealed interface WeatherSummaryUiState {
    object Loading : WeatherSummaryUiState
    data class Success(
        val placeName: String,
        val storedDays: List<DayWeatherSummary>,
        val currentPlaceDescription: String? = null,
        val isDownloadingMonth: Boolean = false
    ) : WeatherSummaryUiState
    data class Error(val message: String) : WeatherSummaryUiState
}

/**
 * Sealed class representing navigation events.
 */
sealed class NavigationEvent {
    /**
     * Event to navigate to the monthly statistics screen.
     *
     * @property placeName The name of the place.
     * @property year The year for the statistics.
     * @property month The month for the statistics.
     */
    data class ToMonthlySummary(val placeName: String, val year: Int, val month: Int) : NavigationEvent()
}

class PlaceSummaryViewModel(
    route: PlaceSummaryRoute,
    private val weatherRepository: WeatherRepository
) : ViewModel()  {
    private var weatherData: WeatherResponse? = null
    val placeName: String = route.placeName

    private val _uiState = MutableStateFlow<WeatherSummaryUiState>(WeatherSummaryUiState.Loading)
    val uiState: StateFlow<WeatherSummaryUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    /**
     * A flow of navigation events.
     */
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _monthlyAverageTemps = MutableStateFlow<Map<Int, Double?>>(emptyMap())
    val monthlyAverageTemps: StateFlow<Map<Int, Double?>> = _monthlyAverageTemps.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(Int.MIN_VALUE)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    fun setSelectedYear(year: Int?) {
        _selectedYear.value = year
    }

    init {
        println("PlaceSummaryViewModel: $placeName")
        loadWeatherData()
    }

    fun onBackClicked() {
        viewModelScope.launch {
            println("PlaceSummaryViewModel BACK")
        }
    }

    private fun loadWeatherData() {
        viewModelScope.launch {
            try {
                Log.d("Fetching weather data for $placeName...")
                val loadedData = weatherRepository.getSavedDataFor(placeName)

                if (loadedData != null) {
                    weatherData = loadedData
                    Log.d("Loaded ${weatherData?.days?.count()} days for $placeName")

                    val newStoredDays = loadedData.days?.toDayWeatherSummaryList() ?: emptyList()

                    val newState = WeatherSummaryUiState.Success(
                        placeName = placeName,
                        storedDays = newStoredDays
                    )
                    _uiState.value = newState
                    // Update monthly average temperatures
                    _monthlyAverageTemps.value = calculateMonthlyAverageTemps(newStoredDays)

                } else {
                    Log.d("No weather data found for $placeName")
                    _uiState.value = WeatherSummaryUiState.Error(
                        message = "No weather data found for $placeName.",
                    )
                }
            } catch (e: Exception) {
                Log.e("Error fetching weather data for $placeName", e)
                _uiState.value = WeatherSummaryUiState.Error(
                    message = "Error loading data: ${e.message ?: "Unknown error"}",
                )
            }
        }
    }

    // New helper to calculate the highest 3-hour rolling average wind speed
    private fun calculateMaxSustainedWindSpeed(hours: List<Hour>?): Double? {
        if (hours == null || hours.size < KiteSpotterConfig.SUSTAINED_WIND_WINDOW_HOURS) {
            return null
        }

        return hours
            .mapNotNull { it.windspeed }
            .windowed(size = KiteSpotterConfig.SUSTAINED_WIND_WINDOW_HOURS, step = 1) { window ->
                window.average()
            }
            .maxOrNull()
    }

    private fun List<Day>.toDayWeatherSummaryList(): List<DayWeatherSummary> {
        return this.map { day ->
            day.toDayWeatherSummary()
        }
    }

    private fun Day.toDayWeatherSummary(): DayWeatherSummary {
        val foggyHours = this.hours?.count { (it.visibility ?: 24.0) < 1.0 } ?: 0
        return DayWeatherSummary(
            date = this.datetime,
            description = this.description ?: this.conditions,
            maxTemp = this.tempmax,
            minTemp = this.tempmin,
            avgTemp = this.temp,
            avgWindSpeed = this.windspeed,
            maxWindSpeed = this.windgust,
            sustainedWindSpeed = calculateMaxSustainedWindSpeed(this.hours),
            solarenergy = this.solarenergy,
            isFoggy = foggyHours > 0,
            foggyHours = foggyHours
        )
    }

    // Helper to parse date parts (year, month) from a date string "YYYY-MM-DD"
    private fun parseDateParts(dateString: String?): Pair<Int?, Int?> {
        if (dateString == null) return Pair(null, null)
        val parts = dateString.split('-')
        val year = parts.getOrNull(0)?.toIntOrNull()
        val month = parts.getOrNull(1)?.toIntOrNull()
        return Pair(year, month)
    }

    // Calculate average temperature for each month from stored days for a specific year
    private fun calculateMonthlyAverageTemps(
        storedDays: List<DayWeatherSummary>,
        year: Int? = null
    ): Map<Int, Double?> {
        return (1..12).associateWith { monthIndex ->
            val daysInMonth = storedDays.filter { daySummary ->
                val (dayYear, dayMonth) = parseDateParts(daySummary.date)
                dayMonth == monthIndex && (year == null || dayYear == year)
            }

            if (daysInMonth.isEmpty()) {
                null
            } else {
                daysInMonth.mapNotNull { it.avgTemp }.average()
            }
        }
    }

    fun onDownloadFullMonth(year: Int, month: Int) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is WeatherSummaryUiState.Success) {
                _uiState.value = currentState.copy(isDownloadingMonth = true)
            }

            try {
                Log.d("Attempting to download full month data for $year-$month for place: $placeName")

                val firstDayOfMonth = LocalDate(year, month, 1)
                val lastDayOfMonth = firstDayOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

                val startDateString = firstDayOfMonth.toString()
                val endDateString = lastDayOfMonth.toString()

                Log.d("Calculated date range for download: $startDateString to $endDateString")

                weatherRepository.getDaysRange(placeName, startDateString, endDateString)

                Log.d("Successfully called getDaysRange for $year-$month. Reloading weather data...")
                loadWeatherData()

            } catch (e: Exception) {
                Log.e("Error downloading full month data for $year-$month", e)
                _uiState.value = WeatherSummaryUiState.Error(
                    message = "Error downloading data for $year-$month: ${e.message ?: "Unknown error"}",
                )
            }
        }
    }

    fun updateMonthTemperaturesForYear(year: Int?) {
        val currentState = _uiState.value
        if (currentState is WeatherSummaryUiState.Success) {
            _monthlyAverageTemps.value = calculateMonthlyAverageTemps(currentState.storedDays, year)
        }
    }

    fun onShowMonth(year: Int?, month: Int?) {
        Log.d("onShowMonth: $month/$year")
        if (year != null && month != null) {
            viewModelScope.launch {
                _navigationEvent.send(NavigationEvent.ToMonthlySummary(placeName, year, month))
            }
        }
    }
}
