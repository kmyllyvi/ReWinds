package place

import androidx.lifecycle.ViewModel // KMM ViewModel
import androidx.lifecycle.viewModelScope
import core.AppSettingsStore
import core.Day
import core.DaysOfInterestFilter
import core.Hour
import core.KiteSpotterConfig
import core.Log // Assuming you have a Log wrapper or use Napier
import core.MonthlyStatisticsRoute
import core.WeatherRepository
import core.WeatherResponse
import core.filterSummary
import core.matches
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import settings.SettingsViewModel

// CalculatedStats data class remains the same
data class CalculatedStats(
    val numberOfDaysWithData: Int = 0,
    val averageMinTemp: Double? = null,
    val averageMaxTemp: Double? = null,
    val overallAverageTemp: Double? = null,
    val absoluteMinTemp: Double? = null,
    val coldestDate: String? = null,
    val absoluteMaxTemp: Double? = null,
    val hottestDate: String? = null,
    val daysOfInterestCount: Int = 0,
    val filterSummary: String = "",
    val totalRainfall: Double? = null,
    val totalSolarEnergy: Double? = null,
    val averageSustainedWindSpeed: Double? = null
)

class MonthlyStatisticsViewModel(
    route: MonthlyStatisticsRoute,
    private val weatherRepository: WeatherRepository,
    private val settingsRepo: AppSettingsStore
) : ViewModel() { // Extend androidx.lifecycle.ViewModel

    val placeName: String = route.placeName

    private var currentYear: Int = route.year
    private var currentMonth: Int = route.month

    private val _statistics = MutableStateFlow<CalculatedStats?>(null)
    val statistics: StateFlow<CalculatedStats?> = _statistics.asStateFlow()

    private val _dailySummaries = MutableStateFlow<List<DayWeatherSummary>>(emptyList())
    val dailySummaries: StateFlow<List<DayWeatherSummary>> = _dailySummaries.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _year = MutableStateFlow(route.year)
    val year: StateFlow<Int> = _year.asStateFlow()

    private val _month = MutableStateFlow(route.month)
    val month: StateFlow<Int> = _month.asStateFlow()

    /** Index into [dailySummaries] of the day with the peak sustained wind speed, or -1 when none. */
    private val _peakWindDayIndex = MutableStateFlow(-1)
    val peakWindDayIndex: StateFlow<Int> = _peakWindDayIndex.asStateFlow()

    /** The day whose detail sheet is open. Non-null drives sheet visibility; null means closed. */
    private val _selectedDay = MutableStateFlow<DayWeatherSummary?>(null)
    val selectedDay: StateFlow<DayWeatherSummary?> = _selectedDay.asStateFlow()

    /** Hourly wind points (09:00–21:00 local time) for [selectedDay]; empty until loaded or when none. */
    private val _selectedDayHours = MutableStateFlow<List<HourlyWindPoint>>(emptyList())
    val selectedDayHours: StateFlow<List<HourlyWindPoint>> = _selectedDayHours.asStateFlow()

    /** True while the selected day's hourly rows are being resolved, so the sheet can show a spinner. */
    private val _isLoadingHours = MutableStateFlow(false)
    val isLoadingHours: StateFlow<Boolean> = _isLoadingHours.asStateFlow()

    /** Full place data from the last load, retained so day selection can resolve hours + tzoffset on demand. */
    private var loadedData: WeatherResponse? = null

    private var filter: DaysOfInterestFilter = loadFilter()

    /**
     * The preferred-day filter currently driving day-of-interest matching, exposed so the day
     * detail sheet can shade hours against the same wind criteria (KIM-305). Initialised from the
     * stored filter at construction and refreshed on every [loadStatistics] so it tracks settings
     * changes.
     */
    private val _activeFilter = MutableStateFlow<DaysOfInterestFilter?>(filter)
    val activeFilter: StateFlow<DaysOfInterestFilter?> = _activeFilter.asStateFlow()

    /**
     * Per-slot criteria shading for the open day's hourly chart, derived from [selectedDayHours]
     * and [activeFilter] so the sheet is a pure render target (KIM-305 / MV*). Emits an empty list
     * (no shading) when there is no filter or no `minWindSpeedKmh`; otherwise one [ShadingTier] per
     * fixed 09:00–21:00 slot. A null `sustainedWindHours` falls back to the same window default the
     * sustained-wind calculation uses, so the shading agrees with the bar-chart peak logic.
     */
    val selectedDayShading: StateFlow<List<ShadingTier>> =
        combine(_selectedDayHours, _activeFilter) { hours, filter ->
            val minSpeed = filter?.minWindSpeedKmh ?: return@combine emptyList()
            val sustainedSlots = filter.sustainedWindHours ?: KiteSpotterConfig.SUSTAINED_WIND_WINDOW_HOURS
            criteriaShading(hourlyWindSlots(hours), minSpeed, sustainedSlots)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun loadFilter(): DaysOfInterestFilter {
        val jsonStr = settingsRepo.getString(SettingsViewModel.FILTER_KEY) ?: return DaysOfInterestFilter.DEFAULT
        return try {
            Json.decodeFromString<DaysOfInterestFilter>(jsonStr)
        } catch (_: Exception) {
            DaysOfInterestFilter.DEFAULT
        }
    }

    init {
        // Log or print the retrieved arguments to verify
        Log.d("MonthlyStatisticsVM", "placeName: $placeName, year: $currentYear, month: $currentMonth")
        loadStatistics()
    }

    fun reloadStatistics(year: Int? = null, month: Int? = null) {
        if (year != null) currentYear = year
        if (month != null) currentMonth = month
        loadStatistics()
    }

    /** Steps the visible month one back, wrapping into the previous year at January. */
    fun navigateToPreviousMonth() {
        if (currentMonth == 1) {
            currentMonth = 12
            currentYear -= 1
        } else {
            currentMonth -= 1
        }
        loadStatistics()
    }

    /** Steps the visible month one forward, wrapping into the next year at December. */
    fun navigateToNextMonth() {
        if (currentMonth == 12) {
            currentMonth = 1
            currentYear += 1
        } else {
            currentMonth += 1
        }
        loadStatistics()
    }

    private fun loadStatistics() {
        filter = loadFilter()
        _activeFilter.value = filter
        _year.value = currentYear
        _month.value = currentMonth
        // Clear previous month's results so the View renders its loading state while the new
        // month resolves, and so the day rows emit before the stats card (progressive render).
        _statistics.value = null
        _dailySummaries.value = emptyList()
        // A reload (month change / download) invalidates any open day sheet.
        dismissDaySheet()
        viewModelScope.launch {

            // Now use the 'this.placeName', 'this.currentYear', 'this.currentMonth' properties
            val allDaysForPlace = weatherRepository.getSavedDataFor(placeName)
            loadedData = allDaysForPlace
            val relevantDaysSummary = filterAndMapDaysForMonth(allDaysForPlace, currentYear, currentMonth)
            _dailySummaries.value = relevantDaysSummary
            _peakWindDayIndex.value = peakSustainedWindIndex(relevantDaysSummary)

            if (relevantDaysSummary.isNotEmpty()) {
                _statistics.value = calculateStatsInternal(relevantDaysSummary)
            } else {
                _statistics.value = CalculatedStats() // Or some error/empty state
            }
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
            maxWindSpeed = this.windgust, // map from windgust
            sustainedWindSpeed = calculateMaxSustainedWindSpeed(this.hours),
            solarenergy = this.solarenergy,
            isFoggy = foggyHours > 0,
            foggyHours = foggyHours,
            precipitation = this.precip,
            windDirection = this.winddir,
            sunrise = this.sunrise,
            sunset = this.sunset
        )
    }

    // New helper to calculate the highest rolling average wind speed using the filter's window
    private fun calculateMaxSustainedWindSpeed(hours: List<Hour>?): Double? {
        val windowSize = filter.sustainedWindHours ?: KiteSpotterConfig.SUSTAINED_WIND_WINDOW_HOURS
        if (hours == null || hours.size < windowSize) {
            return null
        }

        return hours
            .mapNotNull { it.windspeed }
            .windowed(size = windowSize, step = 1) { window ->
                window.average()
            }
            .maxOrNull()
    }


    // Helper to parse date parts (year, month) from a date string "YYYY-MM-DD"
    // This can be moved to a shared utility file if needed elsewhere
    private fun parseDateParts(dateString: String?): Pair<Int?, Int?> {
        if (dateString == null) return Pair(null, null)
        val parts = dateString.split('-')
        val year = parts.getOrNull(0)?.toIntOrNull()
        val month = parts.getOrNull(1)?.toIntOrNull()
        return Pair(year, month)
    }

    private fun filterAndMapDaysForMonth(
        weatherData: WeatherResponse?,
        targetYear: Int,
        targetMonth: Int
    ): List<DayWeatherSummary> {
        if (weatherData?.days == null) {
            Log.d("filterAndMap", "Weather data or days list is null.")
            return emptyList()
        }

        return weatherData.days
            .mapNotNull { day ->
                val (dayYear, dayMonth) = parseDateParts(day.datetime)
                if (dayYear == targetYear && dayMonth == targetMonth) {
                    day // Keep the day if it matches the target year and month
                } else {
                    null // Discard otherwise
                }
            }
            .map { day ->
                val summary = day.toDayWeatherSummary()
                summary.copy(isMatch = filter.matches(summary))
            }
    }


    /**
     * Index of the day holding the peak [DayWeatherSummary.sustainedWindSpeed] value.
     *
     * Returns -1 when no day carries a sustained-wind reading. On ties the earliest
     * day wins, so a single bar is highlighted in the chart. Kept here (not in the
     * composable) so peak selection is unit-testable per MV* rules.
     */
    internal fun peakSustainedWindIndex(daysData: List<DayWeatherSummary>): Int {
        var peakIndex = -1
        var peakValue = Double.NEGATIVE_INFINITY
        daysData.forEachIndexed { index, day ->
            val wind = day.sustainedWindSpeed ?: return@forEachIndexed
            if (wind > peakValue) {
                peakValue = wind
                peakIndex = index
            }
        }
        return peakIndex
    }

    /**
     * Opens the detail sheet for [summary] and resolves its 09:00–21:00 local-time hourly wind
     * points on demand. Hours are read from the already-loaded [WeatherResponse] (the full-load
     * path populates [Day.hours]); the location's [WeatherResponse.tzoffset] drives the local-time
     * window. Emits 0–12 points — empty means no hourly data for the day, which the sheet renders
     * as an empty state rather than a chart.
     */
    fun selectDay(summary: DayWeatherSummary) {
        _selectedDay.value = summary
        _selectedDayHours.value = emptyList()
        _isLoadingHours.value = true
        viewModelScope.launch {
            val day = loadedData?.days?.firstOrNull { it.datetime == summary.date }
            _selectedDayHours.value = hourlyWindWindow(day?.hours, loadedData?.tzoffset)
            _isLoadingHours.value = false
        }
    }

    /** Closes the detail sheet and clears its hourly data. */
    fun dismissDaySheet() {
        _selectedDay.value = null
        _selectedDayHours.value = emptyList()
        _isLoadingHours.value = false
    }

    private fun calculateStatsInternal(daysData: List<DayWeatherSummary>): CalculatedStats {
        if (daysData.isEmpty()) return CalculatedStats()

        val validDays = daysData.filter { it.date != null }
        if (validDays.isEmpty()) return CalculatedStats(numberOfDaysWithData = daysData.size)

        val kiteableDaysCount = validDays.count { day ->
            filter.matches(day)
        }

        val minTemps = validDays.mapNotNull { it.minTemp }
        val maxTemps = validDays.mapNotNull { it.maxTemp }
        val avgTemps = validDays.mapNotNull { it.avgTemp }
        val totalSolarEnergy = validDays.mapNotNull { it.solarenergy }.sum()
        val totalRainfall = validDays.mapNotNull { it.precipitation }.sum()
        val sustainedWinds = validDays.mapNotNull { it.sustainedWindSpeed }


        var absMinTemp: Double? = null
        var coldestDate: String? = null
        validDays.forEach { day ->
            day.minTemp?.let { temp ->
                if (absMinTemp == null || temp < absMinTemp!!) {
                    absMinTemp = temp
                    coldestDate = day.date
                }
            }
        }

        var absMaxTemp: Double? = null
        var hottestDate: String? = null
        validDays.forEach { day ->
            day.maxTemp?.let { temp ->
                if (absMaxTemp == null || temp > absMaxTemp!!) {
                    absMaxTemp = temp
                    hottestDate = day.date
                }
            }
        }

        return CalculatedStats(
            numberOfDaysWithData = validDays.size,
            averageMinTemp = if (minTemps.isNotEmpty()) minTemps.average() else null,
            averageMaxTemp = if (maxTemps.isNotEmpty()) maxTemps.average() else null,
            overallAverageTemp = if (avgTemps.isNotEmpty()) avgTemps.average() else null,
            absoluteMinTemp = absMinTemp,
            coldestDate = coldestDate,
            absoluteMaxTemp = absMaxTemp,
            hottestDate = hottestDate,
            daysOfInterestCount = kiteableDaysCount,
            filterSummary = filter.filterSummary(),
            totalRainfall = if (totalRainfall > 0) totalRainfall else null,
            totalSolarEnergy = if(totalSolarEnergy > 0) totalSolarEnergy else null,
            averageSustainedWindSpeed = if (sustainedWinds.isNotEmpty()) sustainedWinds.average() else null
        )
    }

    fun downloadFullMonth() {
        viewModelScope.launch {
            try {
                _isDownloading.value = true
                weatherRepository.downloadFullMonth(placeName, currentYear, currentMonth)
                Log.d("Successfully downloaded full month. Reloading statistics...")
            } catch (e: Exception) {
                Log.e("Error downloading full month data", e)
            } finally {
                _isDownloading.value = false
                loadStatistics() // Always reload — partial downloads still write to DB
            }
        }
    }

    fun getMissingDaysCount(dailySummaries: List<DayWeatherSummary>): Int {
        val totalDaysInMonth = getDaysInMonth(currentMonth, currentYear)
        return maxOf(0, totalDaysInMonth - dailySummaries.size)
    }

    private fun getDaysInMonth(month: Int, year: Int): Int {
        return when (month) {
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
}
