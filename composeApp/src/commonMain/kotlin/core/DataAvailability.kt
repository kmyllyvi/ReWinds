package core

import kotlinx.serialization.Serializable

/**
 * Represents the availability of weather data for a given location and date range.
 */
enum class DataAvailabilityStatus {
    /**
     * All requested data is available in the database.
     */
    Available,

    /**
     * Some data exists but not the complete date range.
     * May need to fetch additional data from API.
     */
    Partial,

    /**
     * Data is missing or not available for this location/date range.
     * Must fetch from API.
     */
    Missing
}

/**
 * Details about data availability for a location and date range.
 */
@Serializable
data class DataAvailabilityInfo(
    val status: String, // "available", "partial", or "missing"
    val placeName: String,
    val startDate: String,
    val endDate: String,
    val presentDaysCount: Int,
    val requestedDaysCount: Int,
    val missingDates: List<String> = emptyList()
)
