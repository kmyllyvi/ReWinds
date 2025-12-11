package core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
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

    // new search
    suspend fun searchForLocations(query: String): List<GeoSearchResult>
    suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse
}


// To fetch weather data either from network or cache
// API usage https://www.visualcrossing.com/usage/
// => 1000 credits free/day
class WeatherRepositoryImpl(private val networkService: Networking,
                            private val database: Database) : WeatherRepository {
    val visualcrossingUrl = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/"
    val apiKey = "***REMOVED***"
    val apiQuery = "?unitGroup=metric&key=$apiKey&contentType=json&include=hours"

    init {
        Log.d("init WeatherRepositoryImpl")
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

    // https://www.visualcrossing.com/resources/documentation/weather-api/timeline-weather-api/
    // date format "yyyy-mm-dd"
    // example https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/konstanz/2023-06-20?unitGroup=metric&key=***REMOVED***&contentType=json

// ... other imports

// Inside WeatherRepositoryImpl class

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

    override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse {
        Log.d("WeatherRepository, getDaysRange for: $place, from: $fromDate, to: $toDate")

        if (toDate == null) { // Fetching a single day
            val existingPlaceData = database.getSavedPlaceFull(place) // Get WeatherResponse for the place
            val dayFromDb = existingPlaceData?.days?.find { it.datetime == fromDate }

            if (dayFromDb != null) {
                Log.d("WeatherRepository - found existing single day data in DB: $place date: $fromDate")
                return existingPlaceData.copy(days = listOf(dayFromDb)) // Return WeatherResponse with only that day
            } else {
                Log.d("Fetching single day from network: $place, $fromDate")
                val networkResponse = fetchWeatherFromNetwork(place, fromDate, null)
                database.saveWeatherResponse(networkResponse) // Save and merge
                return networkResponse
            }
        } else { // Fetching a date range
            val targetDates = generateDateList(fromDate, toDate)

            if (targetDates.isEmpty()) {
                Log.d("WeatherRepository - targetDates list is empty for range $fromDate to $toDate. Returning empty response.")
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

            if (existingPlaceData != null) {
                // Filter days from DB that are within the requested fromDate and toDate
                val daysInDbWithinDateRange = existingPlaceData.days?.filter { day ->
                    try {
                        val dayDate = LocalDate.parse(day.datetime)
                        val from = LocalDate.parse(fromDate)
                        val to = LocalDate.parse(toDate)
                        dayDate in from..to // Check if dayDate is in the range
                    } catch (e: Exception) {
                        false // If date parsing fails for a stored day, exclude it
                    }
                }

                val foundDatesInDb: Set<String> = (daysInDbWithinDateRange?.map { it.datetime })?.toSet() ?: emptySet()
                val allTargetDatesFound = targetDates.all { foundDatesInDb.contains(it) }
                if (allTargetDatesFound) {
                    Log.d("WeatherRepository - Full range $fromDate to $toDate found in DB for $place.")
                    // Return a WeatherResponse using the main place data but only with days from the specified range
                    return existingPlaceData.copy(days = daysInDbWithinDateRange)
                } else {
                    val missingCount = targetDates.size - foundDatesInDb.size
                    Log.d("WeatherRepository - Range $fromDate to $toDate incomplete in DB for $place ($missingCount days missing). Fetching from network.")
                    // Fall through to fetch from network
                }
            } else {
                Log.d("WeatherRepository - No existing data structure found in DB for place $place. Fetching range from network.")
                // Fall through to fetch from network
            }

            // Fetch from network if existingPlaceData is null or the range is incomplete
            val networkResponse = fetchWeatherFromNetwork(place, fromDate, toDate)
            database.saveWeatherResponse(networkResponse) // This must merge data intelligently
            return networkResponse
        }
    }

    override suspend fun deletePlace(placeName: String) {
        withContext(Dispatchers.IO) {
            database.deletePlace(placeName)
        }
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


    // .../services/timeline/[location]/[date1]/[date2]?key=YOUR_API_KEY
    private suspend fun fetchWeatherFromNetwork(place: String, fromDate: String, toDate: String?): WeatherResponse {
        var requestUrl = "$visualcrossingUrl$place/$fromDate"
        if(toDate != null) {
            requestUrl += "/$toDate"
        }
        requestUrl += apiQuery
        return doRequest(requestUrl)
    }

    // TODO add dynamic day search
// => https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/London,UK/last30days?key=YOUR_API_KEY
    // https://www.visualcrossing.com/resources/documentation/weather-api/using-the-time-period-parameter-to-specify-dynamic-dates-for-weather-api-requests/
    private suspend fun fetchWeatherFromNetwork(place: String, previousDaysCount: Int): WeatherResponse {
        val dynamicRangeString = "last${previousDaysCount}days"
        val requestUrl = "$visualcrossingUrl$place/$dynamicRangeString$apiQuery"
        return doRequest(requestUrl)
    }

    private suspend fun doRequest(requestUrl: String): WeatherResponse {
        try {
            val response = networkService.fetchWeatherData(requestUrl)
            Log.d("WeatherRepository - new weather data: $response")
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