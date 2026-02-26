package ai

import core.Day
import core.WeatherRepository
import core.WeatherResponse
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Factory for creating test weather repositories and test data.
 * Eliminates duplication between MockWeatherRepository and IntegrationMockRepository.
 */
object TestWeatherRepositoryFactory {

    /**
     * Generates a list of Day objects for a date range with consistent test data.
     * Used by both mock repositories to create identical test weather data.
     */
    fun generateTestDays(
        startDate: String,
        endDate: String,
        baseWindSpeed: Double = 10.0,
        baseWindGust: Double = 12.0
    ): List<Day> {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)

        val days = mutableListOf<Day>()
        var current = start
        while (current <= end) {
            days.add(
                Day(
                    datetime = current.toString(),
                    datetimeEpoch = null,
                    tempmax = 22.0,
                    tempmin = 15.0,
                    temp = 18.5,
                    feelslikemax = null,
                    feelslikemin = null,
                    feelslike = null,
                    dew = null,
                    humidity = 65.0,
                    precip = null,
                    precipprob = null,
                    precipcover = null,
                    preciptype = null,
                    snow = null,
                    snowdepth = null,
                    windgust = baseWindGust,
                    windspeed = baseWindSpeed,
                    winddir = 230.0,
                    pressure = null,
                    cloudcover = null,
                    visibility = null,
                    solarradiation = null,
                    solarenergy = null,
                    uvindex = null,
                    sunrise = null,
                    sunriseEpoch = null,
                    sunset = null,
                    sunsetEpoch = null,
                    moonphase = null,
                    conditions = "Clear",
                    description = null,
                    icon = null,
                    stations = null,
                    source = "test",
                    hours = null,
                    normal = null
                )
            )
            current = current.plus(1, DateTimeUnit.DAY)
        }

        return days
    }

    /**
     * Creates a basic WeatherResponse for a place.
     */
    fun createWeatherResponse(
        place: String,
        latitude: Double = 36.19,
        longitude: Double = -5.59,
        days: List<Day> = emptyList()
    ): WeatherResponse {
        return WeatherResponse(
            resolvedAddress = place,
            address = place,
            latitude = latitude,
            longitude = longitude,
            timezone = "Africa/Casablanca",
            tzoffset = 0.0,
            days = days
        )
    }
}
