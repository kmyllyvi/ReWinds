package core

import com.km.rewinds.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


// API documentation: https://open-meteo.com/en/docs/geocoding-api
@Serializable
data class GeoSearchResponse(
    val results: List<GeoSearchResult>? = null
)

@Serializable
data class GeoSearchResult(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @SerialName("admin1") val region: String? = null // "admin1" is the region/state
)

interface WeatherRepository {
    suspend fun getSavedPlaceNames(): List<String>
    suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse?
    suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse
    suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse
    suspend fun deletePlace(placeName: String)
    suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse

    // new search
    suspend fun searchForLocations(query: String): List<GeoSearchResult>
    suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse
}

class WeatherRepositoryImpl(
    private val networkService: Networking,
    private val database: Database,
    private val enableNetworkLogs: Boolean = false
) :  WeatherRepository {
    private val visualcrossingUrl = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/"
    private val apiKey = "***REMOVED***"
    private val apiQuery = "?unitGroup=metric&key=$apiKey&contentType=json&include=hours"

    init {
        Log.d("init WeatherRepository")
    }

    override suspend fun getSavedPlaceNames(): List<String> {
        return withContext(Dispatchers.IO) {
            database.getAllSavedPlaces()
        }
    }

    override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse? {
        return withContext(Dispatchers.IO) {
            database.getSavedPlaceFull(resolvedPlace)
        }
    }

    private fun generateDateList(startDateStr: String, endDateStr: String): List<String> {
        val dates = mutableListOf<String>()
        try {
            var currentDate = LocalDate.parse(startDateStr)
            val endDate = LocalDate.parse(endDateStr)

            if (currentDate > endDate) {
                Log.d("generateDateList: Start date is after end date ($startDateStr > $endDateStr)")
                return emptyList()
            }

            while (currentDate <= endDate) {
                dates.add(currentDate.toString())
                if (currentDate == endDate) break // Avoid issues if plus somehow skips endDate
                currentDate = currentDate.plus(1, DateTimeUnit.DAY)
            }
        } catch (e: Exception) {
            Log.e("Error generating date list from $startDateStr to $endDateStr", e)
            return emptyList() // Return empty on parsing error
        }
        return dates
    }

    // Data class to represent a date range gap that needs to be fetched
    private data class DateGap(val startDate: String, val endDate: String)

    // Calculate consecutive date ranges from a list of missing dates
    private fun calculateDateGaps(missingDates: List<String>): List<DateGap> {
        if (missingDates.isEmpty()) return emptyList()

        val sortedDates = missingDates.map { LocalDate.parse(it) }.sorted()
        val gaps = mutableListOf<DateGap>()

        var gapStart = sortedDates[0]
        var gapEnd = sortedDates[0]

        for (i in 1 until sortedDates.size) {
            val currentDate = sortedDates[i]
            val expectedNextDate = gapEnd.plus(1, DateTimeUnit.DAY)

            if (currentDate == expectedNextDate) {
                // Continue the current gap
                gapEnd = currentDate
            } else {
                // End the current gap and start a new one
                gaps.add(DateGap(gapStart.toString(), gapEnd.toString()))
                gapStart = currentDate
                gapEnd = currentDate
            }
        }

        // Add the final gap
        gaps.add(DateGap(gapStart.toString(), gapEnd.toString()))
        return gaps
    }

    private fun resolveLocationString(place: String, existingData: WeatherResponse?): String {
        if (existingData == null) return place
        val lat = existingData.latitude
        val lon = existingData.longitude
        if (lat == 0.0 && lon == 0.0) return place
        return "$lat%2C$lon"
    }

    private fun truncateToYesterday(toDate: String): String {
        try {
            val requestedDate = LocalDate.parse(toDate)
            val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
            val yesterday = today.minus(1, DateTimeUnit.DAY)
            return if (requestedDate > yesterday) {
                Log.d("WeatherRepository - Truncating toDate from $toDate to $yesterday (only historical data allowed)")
                yesterday.toString()
            } else {
                toDate
            }
        } catch (e: Exception) {
            Log.e("Error parsing toDate: $toDate", e)
            return toDate
        }
    }

    override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse {
        Log.d("WeatherRepository, getDaysRange for: $place, from: $fromDate, to: $toDate")

        if (toDate == null) { // Fetching a single day
            // Prevent fetching future/forecast data - only allow up to yesterday
            val truncatedFromDate = truncateToYesterday(fromDate)
            val existingPlaceData = database.getSavedPlaceFull(place) // Get WeatherResponse for the place
            val dayFromDb = existingPlaceData?.days?.find { it.datetime == truncatedFromDate }

            if (dayFromDb != null) {
                Log.d("WeatherRepository - found existing single day data in DB: $place date: $truncatedFromDate")
                return existingPlaceData.copy(days = listOf(dayFromDb)) // Return WeatherResponse with only that day
            } else {
                Log.d("Fetching single day from network: $place, $truncatedFromDate")
                val locationString = resolveLocationString(place, existingPlaceData)
                val networkResponse = fetchWeatherFromNetwork(locationString, truncatedFromDate, null)
                val correctedResponse = networkResponse.copy(resolvedAddress = place, address = place)
                database.saveWeatherResponse(correctedResponse)
                return correctedResponse
            }
        } else { // Fetching a date range
            val truncatedToDate = truncateToYesterday(toDate)
            val targetDates = generateDateList(fromDate, truncatedToDate)

            if (targetDates.isEmpty()) {
                Log.d("WeatherRepository - targetDates list is empty for range $fromDate to $truncatedToDate. Returning empty response.")
                val existingPlaceInfo = database.getSavedPlaceFull(place) // To get address, lat, lon for empty response shell
                return WeatherResponse( // Return a valid WeatherResponse shell with no days
                    queryCost = 0,
                    latitude = existingPlaceInfo?.latitude ?: 0.0,
                    longitude = existingPlaceInfo?.longitude ?: 0.0,
                    resolvedAddress = existingPlaceInfo?.resolvedAddress ?: place,
                    address = existingPlaceInfo?.address ?: place,
                    timezone = existingPlaceInfo?.timezone ?: "",
                    tzoffset = existingPlaceInfo?.tzoffset ?: 0.0,
                    days = emptyList(),
                    // currentConditions = existingPlaceInfo?.currentConditions
                )
            }

            val existingPlaceData = database.getSavedPlaceFull(place)

            // Step 1: Identify which dates are already in the database
            val foundDatesInDb: Set<String> = if (existingPlaceData != null) {
                existingPlaceData.days?.filter { day ->
                    try {
                        val dayDate = LocalDate.parse(day.datetime)
                        val from = LocalDate.parse(fromDate)
                        val to = LocalDate.parse(truncatedToDate)
                        dayDate in from..to // Check if dayDate is in the range
                    } catch (e: Exception) {
                        false // If date parsing fails for a stored day, exclude it
                    }
                }?.map { it.datetime }?.toSet() ?: emptySet()
            } else {
                emptySet()
            }

            // Step 2: Calculate missing dates
            val missingDates = targetDates.filter { !foundDatesInDb.contains(it) }

            if (missingDates.isEmpty()) {
                // All data is already in the database
                Log.d("WeatherRepository - Full range $fromDate to $truncatedToDate found in DB for $place.")
                val daysInDbWithinDateRange = existingPlaceData?.days?.filter { day ->
                    targetDates.contains(day.datetime)
                }
                return existingPlaceData!!.copy(days = daysInDbWithinDateRange)
            }

            // Step 3: Calculate consecutive date gaps
            val dateGaps = calculateDateGaps(missingDates)
            val locationString = resolveLocationString(place, existingPlaceData)
            Log.d("WeatherRepository - Found ${missingDates.size} missing days in ${dateGaps.size} gap(s) for $place. Fetching gaps...")

            // Step 4: Fetch each gap from the network
            for (gap in dateGaps) {
                try {
                    Log.d("WeatherRepository - Fetching gap: ${gap.startDate} to ${gap.endDate}")
                    val gapResponse = fetchWeatherFromNetwork(locationString, gap.startDate, gap.endDate)
                    val correctedGapResponse = gapResponse.copy(resolvedAddress = place, address = place)
                    database.saveWeatherResponse(correctedGapResponse)
                } catch (e: Exception) {
                    Log.e("Failed to fetch gap ${gap.startDate} to ${gap.endDate} for $place", e)
                    // Continue with other gaps even if one fails
                }
            }

            // Step 5: Re-query the database to get the complete range
            val updatedPlaceData = database.getSavedPlaceFull(place)
            val finalDaysInRange = updatedPlaceData?.days?.filter { day ->
                targetDates.contains(day.datetime)
            }?.sortedBy { it.datetime } // Sort by date

            return updatedPlaceData?.copy(days = finalDaysInRange) ?: WeatherResponse(
                queryCost = 0,
                latitude = existingPlaceData?.latitude ?: 0.0,
                longitude = existingPlaceData?.longitude ?: 0.0,
                resolvedAddress = existingPlaceData?.resolvedAddress ?: place,
                address = existingPlaceData?.address ?: place,
                timezone = existingPlaceData?.timezone ?: "",
                tzoffset = existingPlaceData?.tzoffset ?: 0.0,
                days = emptyList()
            )
        }
    }

    override suspend fun deletePlace(placeName: String) {
        withContext(Dispatchers.IO) {
            database.deletePlace(placeName)
        }
    }

    override suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse {
        Log.d("Downloading full month data for $year-$month for place: $place")

        val firstDayOfMonth = LocalDate(year, month, 1)
        val lastDayOfMonth = firstDayOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

        val startDateString = firstDayOfMonth.toString()
        val endDateString = lastDayOfMonth.toString()

        Log.d("Calculated date range for download: $startDateString to $endDateString")

        return getDaysRange(place, startDateString, endDateString)
    }

    // Fetch number of previous days
    override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse {
        // always get from API - days saved if new
        val response = fetchWeatherFromNetwork(place, previousDaysCount)
        // save to DB
        database.saveWeatherResponse(response)
        return response
    }

    override suspend fun searchForLocations(query: String): List<GeoSearchResult> {
        if (query.isBlank()) return emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$query&count=10&language=en&format=json"
        return try {
            networkService.fetchGeoSearchData(url).results ?: emptyList()
        } catch (e: Exception) {
            Log.e("Geo-search failed for query: $query", e)
            emptyList()
        }
    }

    override suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse {
        // Fetch weather for the location using its lat/lon from geo-search
        val locationString = "${place.latitude}%2C${place.longitude}"
        val weatherResponse = fetchWeatherFromNetwork(locationString, 0) // Fetch some data to validate the new place

        // Overwrite the address from the API response with the correct name from the search result
        val correctedResponse = weatherResponse.copy(
            address = place.name,
            resolvedAddress = place.name
        )
        // Save the corrected weather data to the database
        database.saveWeatherResponse(correctedResponse)
        return correctedResponse
    }

    private suspend fun fetchWeatherFromNetwork(place: String, fromDate: String, toDate: String?): WeatherResponse {
        var requestUrl = "$visualcrossingUrl$place/$fromDate"
        if(toDate != null) {
            requestUrl += "/$toDate"
        }
        requestUrl += apiQuery
        return doRequest(requestUrl)
    }

    private suspend fun fetchWeatherFromNetwork(place: String, previousDaysCount: Int): WeatherResponse {
        val dynamicRangeString = "last${previousDaysCount}days"
        val requestUrl = "$visualcrossingUrl$place/$dynamicRangeString$apiQuery"
        return doRequest(requestUrl)
    }

    private suspend fun doRequest(requestUrl: String): WeatherResponse {
        try {
            val response = networkService.fetchWeatherData(requestUrl)
            Log.d("WeatherRepository - new weather data SUCCESS")
            return response
        } catch (e: Exception) {
            Log.e("Request failed for URL: $requestUrl")
            when (e) {
                is NetworkException -> {
                    Log.e("NetworkException details: message='${e.message}', cause='${e.cause}'")
                }
                else -> {
                    Log.e("Generic exception during request: type='${e::class.simpleName}', message='${e.message}', cause='${e.cause}'")
                }
            }
            // Re-throw the exception after logging it
            throw e
        }
    }
}