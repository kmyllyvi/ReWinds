package core
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val resolvedAddress: String,
    val address: String? = null,
    val queryCost: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val tzoffset: Double? = null,
    val days: List<Day>? = null,
    // stations is here a map of ID to Station info, but in Day/Hour objects it's just a list of IDs (String)
    val stations: Map<String, Station>? = null
)

@Serializable
data class Day(
    val datetime: String,
    val datetimeEpoch: Long?,
    val tempmax: Double?,
    val tempmin: Double?,
    val temp: Double?,
    val feelslikemax: Double?,
    val feelslikemin: Double?,
    val feelslike: Double?,
    val dew: Double?,
    val humidity: Double?,
    val precip: Double?,
    val precipprob: Double?,
    val precipcover: Double?,
    val preciptype: List<String>?,
    val snow: Double?,
    val snowdepth: Double?,
    val windgust: Double?,
    val windspeed: Double?,
    val winddir: Double?,
    val pressure: Double?,
    val cloudcover: Double?,
    val visibility: Double?,
    val solarradiation: Double?,
    val solarenergy: Double?,
    val uvindex: Double?,
    val sunrise: String?,
    val sunriseEpoch: Long?,
    val sunset: String?,
    val sunsetEpoch: Long?,
    val moonphase: Double?,
    val conditions: String?,
    val description: String?,
    val icon: String?,
    val stations: List<String>?,
    val source: String?,
    val hours: List<Hour>? = null,
    val normal: NormalStats? = null // for stats
)

@Serializable
data class Hour(
    val datetime: String,
    val datetimeEpoch: Long?,
    val temp: Double?,
    val feelslike: Double?,
    val humidity: Double?,
    val dew: Double?,
    val precip: Double?,
    val precipprob: Double?,
    val snow: Double?,
    val snowdepth: Double?,
    val preciptype: List<String>?,
    val windgust: Double?,
    val windspeed: Double?,
    val winddir: Double?,
    val pressure: Double?,
    val visibility: Double?,
    val cloudcover: Double?,
    val solarradiation: Double?,
    val solarenergy: Double?,
    val uvindex: Double?,
    val conditions: String?,
    val icon: String?,
    val source: String?,
    val stations: List<String>?
)

@Serializable
data class Station(
    val id: String?,
    val name: String?,
    val distance: Double?,
    val latitude: Double?,
    val longitude: Double?,
    val useCount: Int?,
    val quality: Int?,
    val contribution: Double?
)

@Serializable
// when quering "include=stats"
data class NormalStats(
    val tempmax: List<Double?>? = null,
    val tempmin: List<Double?>? = null,
    val feelslike: List<Double?>? = null,
    val precip: List<Double?>? = null,
    val humidity: List<Double?>? = null,
    val snowdepth: List<Double?>? = null,
    val windspeed: List<Double?>? = null,
    val windgust: List<Double?>? = null, // Example showed [null, null, null]
    val winddir: List<Double?>? = null,
    val cloudcover: List<Double?>? = null
    // Add any other fields you observe in the "normal" object if they exist
)
