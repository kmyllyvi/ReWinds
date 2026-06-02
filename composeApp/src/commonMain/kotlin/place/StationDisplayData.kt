package place

/**
 * Lightweight UI model for a weather station.
 *
 * Intentionally separate from core.Station (the API/network model) to keep the layer boundary clean.
 * The View reads this from WeatherSummaryUiState.Success — it never queries the repository directly.
 */
data class StationDisplayData(
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val distance: Double? = null,
    val quality: Int? = null,
    val useCount: Int? = null
)
