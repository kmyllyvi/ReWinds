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
    val sunrise: String? = null,
    val sunset: String? = null,
    val isMatch: Boolean = false
) {
    /**
     * The wind value shown on the collapsed day summary row: the day's average top wind
     * (peak sustained/rolling-average), not the momentary gust (KIM-329). Kept here so the
     * collapsed-row semantics are unit-testable rather than decided inside the composable.
     */
    val collapsedRowWindSpeed: Double?
        get() = sustainedWindSpeed
}
