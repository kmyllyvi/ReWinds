package core

import kotlinx.serialization.Serializable
import place.DayWeatherSummary

@Serializable
data class DaysOfInterestFilter(
    val naturalLanguageCriteria: String,
    val minTempC: Double? = null,
    val maxTempC: Double? = null,
    val minWindSpeedKmh: Double? = null,
    val maxWindSpeedKmh: Double? = null,
    val windDirections: List<String>? = null,
    val sustainedWindHours: Int? = null,
    val daylightOnly: Boolean? = null,
    val noRain: Boolean? = null,
    val maxCloudCoverPct: Double? = null
) {
    companion object {
        val DEFAULT = DaysOfInterestFilter(
            naturalLanguageCriteria = "Sustained wind \u2265 20 km/h for 2+ hours, temperature \u2265 10\u00B0C",
            minWindSpeedKmh = 20.0,
            sustainedWindHours = 2,
            minTempC = 10.0
        )
    }
}

fun degreesToCompass(degrees: Double): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((degrees + 22.5) / 45.0).toInt() % 8
    return directions[index]
}

fun DaysOfInterestFilter.filterSummary(): String {
    val parts = mutableListOf<String>()
    if (minWindSpeedKmh != null || maxWindSpeedKmh != null) {
        val windRange = when {
            minWindSpeedKmh != null && maxWindSpeedKmh != null -> "${minWindSpeedKmh.toInt()}–${maxWindSpeedKmh.toInt()} km/h"
            minWindSpeedKmh != null -> "≥ ${minWindSpeedKmh.toInt()} km/h"
            else -> "≤ ${maxWindSpeedKmh!!.toInt()} km/h"
        }
        val hours = if (sustainedWindHours != null) " for ${sustainedWindHours}h+" else ""
        val dirs = if (!windDirections.isNullOrEmpty()) " from ${windDirections.joinToString("/")}" else ""
        parts.add("Wind: $windRange$dirs$hours")
    }
    if (minTempC != null || maxTempC != null) {
        val tempRange = when {
            minTempC != null && maxTempC != null -> "${minTempC.toInt()}–${maxTempC.toInt()}°C"
            minTempC != null -> "≥ ${minTempC.toInt()}°C"
            else -> "≤ ${maxTempC!!.toInt()}°C"
        }
        parts.add("Temp: $tempRange")
    }
    if (maxCloudCoverPct != null) parts.add("Cloud: ≤ ${maxCloudCoverPct.toInt()}%")
    if (noRain == true) parts.add("No rain")
    if (daylightOnly == true) parts.add("Daylight hours only")
    return if (parts.isEmpty()) naturalLanguageCriteria else parts.joinToString(" · ")
}

fun DaysOfInterestFilter.matches(day: DayWeatherSummary): Boolean {
    if (minTempC != null && (day.avgTemp ?: Double.MIN_VALUE) < minTempC) return false
    if (maxTempC != null && (day.avgTemp ?: Double.MAX_VALUE) > maxTempC) return false
    if (noRain == true && (day.precipitation ?: 0.0) > 0.0) return false
    if (windDirections != null && day.windDirection != null) {
        val compassPoint = degreesToCompass(day.windDirection)
        if (compassPoint !in windDirections) return false
    }
    // The minimum is checked against the window *floor*, not its average, so a day only counts as
    // windy enough when every hour of some window clears the bar — the same rule the day chart
    // shades with (KIM-419). The maximum stays on the average: a single gusty hour inside an
    // otherwise rideable window shouldn't disqualify the day.
    if (minWindSpeedKmh != null && (day.sustainedWindFloor ?: 0.0) < minWindSpeedKmh) return false
    if (maxWindSpeedKmh != null && (day.sustainedWindSpeed ?: 0.0) > maxWindSpeedKmh) return false
    return true
}
