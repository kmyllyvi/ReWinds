package place

data class DayWeatherSummary(
    val date: String?,
    val description: String?,
    val maxTemp: Double?,
    val minTemp: Double?,
    val avgTemp: Double?,
    val avgWindSpeed: Double?,
    val maxWindSpeed: Double?, // gust
    val sustainedWindSpeed: Double? // 3h avg
)
