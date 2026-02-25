package ai

import core.Day
import core.WeatherRepository
import core.WeatherResponse
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Integration tests for WeatherTools.
 * Tests the full tool call flow with realistic scenarios.
 */
class WeatherToolsIntegrationTest {

    private val integrationRepository = IntegrationMockRepository()

    @Test
    fun toolCall_getWindSummary_fullFlow_returnsCompleteJson() {
        runBlocking {
            integrationRepository.addTestPlace("Tarifa", 36.19, -5.59)

            val args = buildJsonObject {
                put("location_name", JsonPrimitive("Tarifa"))
                put("start_date", JsonPrimitive("2025-11-01"))
                put("end_date", JsonPrimitive("2025-11-10"))
            }
            val result = WeatherTools.handleToolCall("get_wind_summary", args, integrationRepository)

            assertTrue(isValidJson(result), "Result should be valid JSON")
            assertTrue(result.contains("place"), "Should have place field")
            assertTrue(result.contains("date_range"), "Should have date_range field")
            assertTrue(result.contains("wind_summary"), "Should have wind_summary array")
        }
    }

    @Test
    fun toolCall_listSavedPlaces_fullFlow_returnsAllPlaces() {
        runBlocking {
            integrationRepository.addTestPlace("Tarifa", 36.19, -5.59)
            integrationRepository.addTestPlace("Cabarete", 19.75, -70.47)

            val args = buildJsonObject {}
            val result = WeatherTools.handleToolCall("list_saved_places", args, integrationRepository)

            assertTrue(isValidJson(result), "Result should be valid JSON")
            assertTrue(result.contains("Tarifa"), "Should contain Tarifa")
            assertTrue(result.contains("Cabarete"), "Should contain Cabarete")
            assertTrue(result.contains("count"), "Should show count")
        }
    }

    @Test
    fun toolCall_getMonthlyStats_fullFlow_returnsAggregates() {
        runBlocking {
            integrationRepository.addTestPlace("Tarifa", 36.19, -5.59)

            val args = buildJsonObject {
                put("location_name", JsonPrimitive("Tarifa"))
                put("year", JsonPrimitive(2025))
                put("month", JsonPrimitive(11))
            }
            val result = WeatherTools.handleToolCall("get_monthly_stats", args, integrationRepository)

            assertTrue(isValidJson(result), "Result should be valid JSON")
            assertTrue(result.contains("place"), "Should have place field")
            assertTrue(result.contains("month"), "Should have month field")
            assertTrue(result.contains("avg_wind_knots"), "Should have avg_wind_knots")
        }
    }

    @Test
    fun toolCall_getBestDays_fullFlow_returnsFilteredDays() {
        runBlocking {
            integrationRepository.addTestPlace("Tarifa", 36.19, -5.59)

            val args = buildJsonObject {
                put("location_name", JsonPrimitive("Tarifa"))
                put("start_date", JsonPrimitive("2025-11-01"))
                put("end_date", JsonPrimitive("2025-11-30"))
            }
            val result = WeatherTools.handleToolCall("get_best_days", args, integrationRepository)

            assertTrue(isValidJson(result), "Result should be valid JSON")
            assertTrue(result.contains("place"), "Should have place field")
            assertTrue(result.contains("matching_days"), "Should have matching_days count")
        }
    }

    @Test
    fun dataConsistency_sameQueryTwice_returnsSameData() {
        runBlocking {
            integrationRepository.addTestPlace("Tarifa", 36.19, -5.59)

            val args = buildJsonObject {
                put("location_name", JsonPrimitive("Tarifa"))
                put("start_date", JsonPrimitive("2025-11-01"))
                put("end_date", JsonPrimitive("2025-11-10"))
            }

            val result1 = WeatherTools.handleToolCall("get_wind_summary", args, integrationRepository)
            val result2 = WeatherTools.handleToolCall("get_wind_summary", args, integrationRepository)

            assertTrue(result1 == result2, "Same query should return same data")
        }
    }

    @Test
    fun schemaValidation_allSchemasAreValidJsonObjects() {
        val schemas = WeatherTools.allToolSchemas()

        for (schema in schemas) {
            val schemaStr = schema.inputSchema.toString()
            assertTrue(isValidJson(schemaStr), "Schema for ${schema.name} should be valid JSON")
            assertTrue(schemaStr.contains("type"), "Schema should have type field")
        }
    }

    @Test
    fun responseShape_windSummaryHasCorrectFields() {
        runBlocking {
            integrationRepository.addTestPlace("Tarifa", 36.19, -5.59)

            val args = buildJsonObject {
                put("location_name", JsonPrimitive("Tarifa"))
                put("start_date", JsonPrimitive("2025-11-01"))
                put("end_date", JsonPrimitive("2025-11-05"))
            }
            val result = WeatherTools.handleToolCall("get_wind_summary", args, integrationRepository)

            assertTrue(result.contains("place"), "Should have place")
            assertTrue(result.contains("date_range"), "Should have date_range")
            assertTrue(result.contains("days_count"), "Should have days_count")
            assertTrue(result.contains("wind_summary"), "Should have wind_summary")
        }
    }

    private fun isValidJson(json: String): Boolean {
        return json.startsWith("{") && json.endsWith("}")
    }
}

/**
 * Integration mock repository
 */
private class IntegrationMockRepository : WeatherRepository {
    private val places = mutableMapOf<String, TestPlaceData>()

    data class TestPlaceData(
        val name: String,
        val latitude: Double,
        val longitude: Double
    )

    fun addTestPlace(name: String, latitude: Double, longitude: Double) {
        places[name] = TestPlaceData(name, latitude, longitude)
    }

    override suspend fun getSavedPlaceNames(): List<String> = places.keys.toList()

    override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse? {
        val placeData = places[resolvedPlace] ?: return null
        return createWeatherResponseFor(placeData)
    }

    override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse {
        val placeData = places[place] ?: return WeatherResponse(
            resolvedAddress = place,
            address = place,
            latitude = 0.0,
            longitude = 0.0,
            timezone = "UTC",
            tzoffset = 0.0,
            days = emptyList()
        )

        val start = LocalDate.parse(fromDate)
        val end = if (toDate != null) {
            LocalDate.parse(toDate)
        } else {
            start
        }

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
                    windgust = 12.0,
                    windspeed = 10.0,
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

        return WeatherResponse(
            resolvedAddress = placeData.name,
            address = placeData.name,
            latitude = placeData.latitude,
            longitude = placeData.longitude,
            timezone = "Africa/Casablanca",
            tzoffset = 0.0,
            days = days
        )
    }

    override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse {
        val placeData = places[place] ?: return WeatherResponse(
            resolvedAddress = place,
            address = place,
            latitude = 0.0,
            longitude = 0.0,
            timezone = "UTC",
            tzoffset = 0.0,
            days = emptyList()
        )
        return getDaysRange(place, "2025-11-01", "2025-11-10")
    }

    override suspend fun deletePlace(placeName: String) {
        places.remove(placeName)
    }

    override suspend fun searchForLocations(query: String): List<core.GeoSearchResult> {
        return emptyList()
    }

    override suspend fun addPlaceFromSearch(place: core.GeoSearchResult): WeatherResponse {
        places[place.name] = TestPlaceData(place.name, place.latitude, place.longitude)
        return createWeatherResponseFor(places[place.name]!!)
    }

    private fun createWeatherResponseFor(placeData: TestPlaceData): WeatherResponse {
        return WeatherResponse(
            resolvedAddress = placeData.name,
            address = placeData.name,
            latitude = placeData.latitude,
            longitude = placeData.longitude,
            timezone = "Africa/Casablanca",
            tzoffset = 0.0,
            days = emptyList()
        )
    }
}
