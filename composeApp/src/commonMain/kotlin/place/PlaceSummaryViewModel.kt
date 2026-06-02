package place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.Day
import core.Hour // Import Hour
import core.KiteSpotterConfig
import core.Log
import core.PlaceSummaryRoute
import core.Station
import core.StationsResult
import core.WeatherRepository
import core.WeatherResponse
import io.github.aakira.napier.Napier
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

sealed interface WeatherSummaryUiState {
    object Loading : WeatherSummaryUiState
    data class Success(
        val placeName: String,
        val storedDays: List<DayWeatherSummary>,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val currentPlaceDescription: String? = null,
        val isDownloadingMonth: Boolean = false,
        // Stations from the WeatherStation table. Empty list = no station data available.
        val stations: List<StationDisplayData> = emptyList(),
        // Non-null when the last station fetch/refresh failed (existing stations still shown).
        val stationsError: String? = null
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

    private val _isRefreshingStations = MutableStateFlow(false)
    val isRefreshingStations: StateFlow<Boolean> = _isRefreshingStations.asStateFlow()

    private val _isMapModalVisible = MutableStateFlow(false)
    val isMapModalVisible: StateFlow<Boolean> = _isMapModalVisible.asStateFlow()

    fun showMapModal() { _isMapModalVisible.value = true }
    fun dismissMapModal() { _isMapModalVisible.value = false }

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

    fun refreshData() {
        loadWeatherData()
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

                    // Check for persisted stations. If none exist, auto-backfill once.
                    val persistedStations = weatherRepository.getPersistedStations(placeName)
                    val (displayStations, stationsError) = if (persistedStations.isEmpty()) {
                        Log.d("No stations persisted for $placeName — triggering auto-backfill")
                        backfillStations()
                    } else {
                        Log.d("${persistedStations.size} station(s) already persisted for $placeName")
                        Pair(persistedStations.toDisplayData(), null)
                    }

                    val newState = WeatherSummaryUiState.Success(
                        placeName = placeName,
                        storedDays = newStoredDays,
                        latitude = loadedData.latitude,
                        longitude = loadedData.longitude,
                        stations = displayStations,
                        stationsError = stationsError
                    )
                    _uiState.value = newState
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

    /**
     * Re-fetch station data from the API and replace whatever is stored.
     * Called both for auto-backfill (once when table is empty) and user-triggered refresh.
     *
     * Returns the display list and an optional error message to surface in the UI.
     * On network error the previously-persisted rows are untouched, so we fall back to reading
     * them from the repository again.
     */
    private suspend fun backfillStations(): Pair<List<StationDisplayData>, String?> {
        return when (val result = weatherRepository.fetchAndPersistStations(placeName)) {
            is StationsResult.Success -> {
                Log.d("Station backfill success: ${result.stations.size} station(s) for $placeName")
                Pair(result.stations.toDisplayData(), null)
            }
            is StationsResult.Empty -> {
                Log.d("Station backfill: API returned no stations for $placeName")
                Pair(emptyList(), null)
            }
            is StationsResult.Error -> {
                Log.e("Station backfill failed for $placeName: ${result.message}")
                // Existing rows were not touched — read back whatever is still in the DB.
                val fallback = weatherRepository.getPersistedStations(placeName).toDisplayData()
                Pair(fallback, result.message)
            }
        }
    }

    /**
     * Manually re-fetch and replace station data. Sets isRefreshingStations while running.
     * On error the previously-persisted rows remain, and an error message is shown in state.
     */
    fun refreshStations() {
        viewModelScope.launch {
            _isRefreshingStations.value = true
            val (displayStations, error) = backfillStations()
            val current = _uiState.value
            if (current is WeatherSummaryUiState.Success) {
                _uiState.value = current.copy(stations = displayStations, stationsError = error)
            }
            _isRefreshingStations.value = false
        }
    }

    private fun List<Station>.toDisplayData(): List<StationDisplayData> =
        mapNotNull { station ->
            val lat = station.latitude ?: return@mapNotNull null
            val lon = station.longitude ?: return@mapNotNull null
            StationDisplayData(
                name = station.name,
                latitude = lat,
                longitude = lon,
                distance = station.distance,
                quality = station.quality,
                useCount = station.useCount
            )
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
                weatherRepository.downloadFullMonth(placeName, year, month)
                Log.d("Successfully downloaded full month data for $year-$month. Reloading weather data...")
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

        // helper function to check month completion status (downloaded or not)
        fun calculateMonthCompletionStatusMap(
            selectedYear: Int?,
            storedDays: List<DayWeatherSummary> // Make sure DayWeatherSummary is the correct type
        ): Map<Int, MonthCompletionInfo> {
            if (selectedYear == null || selectedYear == Int.MIN_VALUE) {
                return emptyMap()
            }
            return (1..12).associateWith { monthIndex ->
                val firstDayOfMonth = LocalDate(selectedYear, monthIndex, 1)
                val totalDaysInMonth = firstDayOfMonth.plus(1, DateTimeUnit.MONTH)
                    .minus(1, DateTimeUnit.DAY).dayOfMonth

                val presentDaysCount = storedDays.count { daySummary ->
                    val (dYear, dMonth) = try {
                        val dateStr = daySummary.date
                        if (dateStr != null) {
                            val parts = dateStr.split('-')
                            if (parts.size >= 2) {
                                val year = parts[0].toIntOrNull()
                                val month = parts[1].toIntOrNull()
                                if (year != null && month != null) {
                                    Pair(year, month)
                                } else {
                                    Pair(-1, -1)
                                }
                            } else {
                                Pair(-1, -1)
                            }
                        } else {
                            Pair(-1, -1)
                        }
                    } catch (e: Exception) {
                        Napier.w(
                            "Error parsing date: ${daySummary.date}",
                            e,
                            tag = "PlaceSummaryView"
                        )
                        Pair(-1, -1)
                    }
                    dYear == selectedYear && dMonth == monthIndex
                }
                MonthCompletionInfo(
                    presentDaysCount = presentDaysCount,
                    totalDaysInMonth = totalDaysInMonth,
                    isFullyLoaded = presentDaysCount >= totalDaysInMonth
                )
            }
        }

        // Helper function to calculate missing days count from MonthCompletionInfo map
        fun calculateMissingDaysMap(
            detailedMap: Map<Int, MonthCompletionInfo>
        ): Map<Int, Int> {
            return detailedMap.mapValues { (_, info) ->
                maxOf(0, info.totalDaysInMonth - info.presentDaysCount)
            }
        }
}
