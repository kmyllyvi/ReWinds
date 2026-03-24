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

fun DaysOfInterestFilter.matches(day: DayWeatherSummary): Boolean {
    if (minTempC != null && (day.avgTemp ?: Double.MIN_VALUE) < minTempC) return false
    if (maxTempC != null && (day.avgTemp ?: Double.MAX_VALUE) > maxTempC) return false
    if (noRain == true && (day.precipitation ?: 0.0) > 0.0) return false
    if (windDirections != null && day.windDirection != null) {
        val compassPoint = degreesToCompass(day.windDirection)
        if (compassPoint !in windDirections) return false
    }
    if (minWindSpeedKmh != null && (day.sustainedWindSpeed ?: 0.0) < minWindSpeedKmh) return false
    if (maxWindSpeedKmh != null && (day.sustainedWindSpeed ?: 0.0) > maxWindSpeedKmh) return false
    return true
}
