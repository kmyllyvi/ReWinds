package place

data class DayWeatherSummary(
    val date: String?,
    val description: String?,
    val maxTemp: Double?,
    val minTemp: Double?,
    val avgTemp: Double?,
    val avgWindSpeed: Double?,
    val maxWindSpeed: Double?, // gust
    val sustainedWindSpeed: Double?, // 3h avg
    val solarenergy: Double?,
    val isFoggy: Boolean,
    val foggyHours: Int,
    val precipitation: Double? = null,
    val windDirection: Double? = null,  // degrees
    val isMatch: Boolean = false
)
