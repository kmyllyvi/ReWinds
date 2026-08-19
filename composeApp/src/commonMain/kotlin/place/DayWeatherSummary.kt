package place

data class DayWeatherSummary(
    val date: String?,
    val description: String?,
    val maxTemp: Double?,
    val minTemp: Double?,
    val avgTemp: Double?,
    val avgWindSpeed: Double?,
    val maxWindSpeed: Double?, // gust
    val sustainedWindSpeed: Double?, // best rolling average over the chart's 09:00–21:00 window
    val solarenergy: Double?,
    val isFoggy: Boolean,
    val foggyHours: Int,
    val precipitation: Double? = null,
    val windDirection: Double? = null,  // degrees
    val sunrise: String? = null,
    val sunset: String? = null,
    val isMatch: Boolean = false,
    /**
     * The highest *minimum* wind speed across the same windows [sustainedWindSpeed] averages, i.e.
     * [SustainedWind.bestFloorKmh]. This — not the average — decides whether the day meets the wind
     * threshold, so the month list's highlight agrees with the day chart's "Meets threshold"
     * shading (KIM-419). Null when no window has complete hourly data.
     */
    val sustainedWindFloor: Double? = null
) {
    /**
     * The wind value shown on the collapsed day summary row: the day's average top wind
     * (peak sustained/rolling-average), not the momentary gust (KIM-329). Kept here so the
     * collapsed-row semantics are unit-testable rather than decided inside the composable.
     */
    val collapsedRowWindSpeed: Double?
        get() = sustainedWindSpeed
}
