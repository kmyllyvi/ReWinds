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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for WeatherTools.
 * Tests each tool with valid inputs, invalid inputs, and edge cases.
 */
class WeatherToolsTest {

    private val mockRepository = MockWeatherRepository()

    @Test
    fun getWindSummary_validInputs_returnsJsonWithWindData() {
        runBlocking {
            val args = buildJsonObject {
                put("location_name", JsonPrimitive("Tarifa"))
                put("start_date", JsonPrimitive("2025-11-01"))
                put("end_date", JsonPrimitive("2025-11-02"))
            }

            val result = WeatherTools.handleToolCall("get_wind_summary", args, mockRepository)

            assertTrue(result.contains("place"), "Result should contain place field")
            assertTrue(result.contains("Tarifa"), "Result should contain place name")
            assertTrue(result.contains("wind_summary"), "Result should contain wind_summary array")
        }
    }

    @Test
    fun getWindSummary_missingLocationName_returnsErrorJson() {
        runBlocking {
            val args = buildJsonObject {
                put("start_date", JsonPrimitive("2025-11-01"))
                put("end_date", JsonPrimitive("2025-11-02"))
            }

            val result = WeatherTools.handleToolCall("get_wind_summary", args, mockRepository)

            assertTrue(result.contains("error"), "Result should contain error field")
            assertTrue(result.contains("location_name"), "Error should mention missing field")
        }
    }

    @Test
    fun getWindSummary_invalidDateFormat_returnsErrorJson() {
        runBlocking {
            val args = buildJsonObject {
                put("location_name", JsonPrimitive("Tarifa"))
                put("start_date", JsonPrimitive("11-01-2025"))
                put("end_date", JsonPrimitive("2025-11-02"))
            }

            val result = WeatherTools.handleToolCall("get_wind_summary", args, mockRepository)

            assertTrue(result.contains("error"), "Result should contain error field")
            assertTrue(result.contains("date format"), "Error should mention date format")
        }
    }

    @Test
    fun listSavedPlaces_returnsAllPlaces() {
        runBlocking {
            mockRepository.addPlace("Tarifa")
            mockRepository.addPlace("Cabarete")

            val args = buildJsonObject {}

            val result = WeatherTools.handleToolCall("list_saved_places", args, mockRepository)

            assertTrue(result.contains("places"), "Result should contain places array")
            assertTrue(result.contains("Tarifa"), "Result should contain Tarifa")
            assertTrue(result.contains("Cabarete"), "Result should contain Cabarete")
            assertTrue(result.contains("count"), "Result should show count")
        }
    }

    @Test
    fun getMonthlyStats_validInputs_returnsAggregatedStats() {
        runBlocking {
            val args = buildJsonObject {
                put("location_name", JsonPrimitive("Tarifa"))
                put("year", JsonPrimitive(2025))
                put("month", JsonPrimitive(11))
            }

            val result = WeatherTools.handleToolCall("get_monthly_stats", args, mockRepository)

            assertTrue(result.contains("month"), "Result should contain month field")
            assertTrue(result.contains("2025-11"), "Result should contain year-month")
            assertTrue(result.contains("avg_wind_knots"), "Result should include average wind")
        }
    }

    @Test
    fun getMonthlyStats_invalidMonth_returnsError() {
        runBlocking {
            val args = buildJsonObject {
                put("location_name", JsonPrimitive("Tarifa"))
                put("year", JsonPrimitive(2025))
                put("month", JsonPrimitive(13))
            }

            val result = WeatherTools.handleToolCall("get_monthly_stats", args, mockRepository)

            assertTrue(result.contains("error"), "Result should contain error field")
        }
    }

    @Test
    fun getBestDays_validInputs_returnsMatchingDays() {
        runBlocking {
            val args = buildJsonObject {
                put("location_name", JsonPrimitive("Tarifa"))
                put("start_date", JsonPrimitive("2025-11-01"))
                put("end_date", JsonPrimitive("2025-11-05"))
            }

            val result = WeatherTools.handleToolCall("get_best_days", args, mockRepository)

            assertTrue(result.contains("days"), "Result should contain days array")
            assertTrue(result.contains("matching_days"), "Result should contain matching_days count")
        }
    }

    @Test
    fun unknownTool_returnsErrorJson() {
        runBlocking {
            val args = buildJsonObject {}

            val result = WeatherTools.handleToolCall("unknown_tool", args, mockRepository)

            assertTrue(result.contains("error"), "Result should contain error field")
            assertTrue(result.contains("Unknown tool"), "Error should indicate unknown tool")
        }
    }

    @Test
    fun allToolSchemas_includesGetWindSummary() {
        val schemas = WeatherTools.allToolSchemas()
        val windTool = schemas.find { it.name == "get_wind_summary" }

        assertEquals("get_wind_summary", windTool?.name)
        assertTrue(windTool?.description?.contains("wind") ?: false)
    }

    @Test
    fun allToolSchemas_includesListSavedPlaces() {
        val schemas = WeatherTools.allToolSchemas()
        val listTool = schemas.find { it.name == "list_saved_places" }

        assertEquals("list_saved_places", listTool?.name)
    }

    @Test
    fun getWindSummarySchema_hasRequiredFields() {
        val schema = Tool.GetWindSummary.inputSchema
        val schemaStr = schema.toString()

        assertTrue(schemaStr.contains("location_name"), "Schema should have location_name property")
        assertTrue(schemaStr.contains("start_date"), "Schema should have start_date property")
        assertTrue(schemaStr.contains("end_date"), "Schema should have end_date property")
    }
}

/**
 * Simple mock repository for testing
 */
private class MockWeatherRepository : WeatherRepository {
    private val places = mutableListOf<String>()

    fun addPlace(name: String) {
        if (!places.contains(name)) {
            places.add(name)
        }
    }

    override suspend fun getSavedPlaceNames(): List<String> = places

    override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse? {
        return if (places.contains(resolvedPlace)) {
            createTestWeatherResponse(resolvedPlace)
        } else {
            null
        }
    }

    override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse {
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
            resolvedAddress = place,
            address = place,
            latitude = 36.19,
            longitude = -5.59,
            timezone = "Africa/Casablanca",
            tzoffset = 0.0,
            days = days
        )
    }

    override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse {
        return getDaysRange(place, "2025-11-01", "2025-11-05")
    }

    override suspend fun deletePlace(placeName: String) {
        places.remove(placeName)
    }

    override suspend fun searchForLocations(query: String): List<core.GeoSearchResult> {
        return emptyList()
    }

    override suspend fun addPlaceFromSearch(place: core.GeoSearchResult): WeatherResponse {
        return createTestWeatherResponse(place.name)
    }

    private fun createTestWeatherResponse(place: String): WeatherResponse {
        return WeatherResponse(
            resolvedAddress = place,
            address = place,
            latitude = 36.19,
            longitude = -5.59,
            timezone = "Africa/Casablanca",
            tzoffset = 0.0,
            days = emptyList()
        )
    }
}
