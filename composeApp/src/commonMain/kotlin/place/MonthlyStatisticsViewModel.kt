package place

import androidx.lifecycle.ViewModel // KMM ViewModel
import androidx.lifecycle.viewModelScope
import core.Day
import core.Hour
import core.KiteSpotterConfig
import core.Log // Assuming you have a Log wrapper or use Napier
import core.MonthlyStatisticsRoute
import core.WeatherRepository
import core.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate

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
    val kiteableDaysCount: Int = 0, // New field for kiteable days
    val totalSolarEnergy: Double? = null
)

class MonthlyStatisticsViewModel(
    route: MonthlyStatisticsRoute,
    private val weatherRepository: WeatherRepository
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

    private fun loadStatistics() {
        viewModelScope.launch {

            // Now use the 'this.placeName', 'this.currentYear', 'this.currentMonth' properties
            val allDaysForPlace = weatherRepository.getSavedDataFor(placeName)
            val relevantDaysSummary = filterAndMapDaysForMonth(allDaysForPlace, currentYear, currentMonth)
            _dailySummaries.value = relevantDaysSummary

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
            foggyHours = foggyHours
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
            .map { it.toDayWeatherSummary() } // Map the filtered Days to DayWeatherSummary
    }


    private fun calculateStatsInternal(daysData: List<DayWeatherSummary>): CalculatedStats {
        if (daysData.isEmpty()) return CalculatedStats()

        val validDays = daysData.filter { it.date != null }
        if (validDays.isEmpty()) return CalculatedStats(numberOfDaysWithData = daysData.size)

        val kiteableDaysCount = validDays.count { day ->
            // Use predefined filter values for decision on "kiteable"
            (day.sustainedWindSpeed ?: 0.0) >= KiteSpotterConfig.MIN_SUSTAINED_WIND_SPEED_KMH &&
                    (day.avgTemp ?: 0.0) >= KiteSpotterConfig.MIN_TEMP_CELSIUS
        }

        val minTemps = validDays.mapNotNull { it.minTemp }
        val maxTemps = validDays.mapNotNull { it.maxTemp }
        val avgTemps = validDays.mapNotNull { it.avgTemp }
        val totalSolarEnergy = validDays.mapNotNull { it.solarenergy }.sum()


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
            kiteableDaysCount = kiteableDaysCount, // Set the new count
            totalSolarEnergy = if(totalSolarEnergy > 0) totalSolarEnergy else null
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
