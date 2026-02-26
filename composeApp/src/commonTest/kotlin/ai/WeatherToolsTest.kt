package ai

import core.Day
import core.WeatherRepository
import core.WeatherResponse
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
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

            val json = Json.parseToJsonElement(result).jsonObject
            assertTrue(json.containsKey("place"), "Result should contain place key")
            assertTrue(json.containsKey("wind_summary"), "Result should contain wind_summary key")
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

            val json = Json.parseToJsonElement(result).jsonObject
            assertTrue(json.containsKey("error"), "Result should contain error key")
            assertTrue(json.containsKey("tool"), "Result should contain tool key")
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

            val json = Json.parseToJsonElement(result).jsonObject
            assertTrue(json.containsKey("error"), "Result should contain error key")
            assertTrue(json.containsKey("tool"), "Result should contain tool key")
        }
    }

    @Test
    fun listSavedPlaces_returnsAllPlaces() {
        runBlocking {
            mockRepository.addPlace("Tarifa")
            mockRepository.addPlace("Cabarete")

            val args = buildJsonObject {}

            val result = WeatherTools.handleToolCall("list_saved_places", args, mockRepository)

            val json = Json.parseToJsonElement(result).jsonObject
            assertTrue(json.containsKey("places"), "Result should contain places key")
            assertTrue(json.containsKey("count"), "Result should contain count key")
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

            val json = Json.parseToJsonElement(result).jsonObject
            assertTrue(json.containsKey("month"), "Result should contain month key")
            assertTrue(json.containsKey("avg_wind_knots"), "Result should contain avg_wind_knots key")
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

            val json = Json.parseToJsonElement(result).jsonObject
            assertTrue(json.containsKey("error"), "Result should contain error key")
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

            val json = Json.parseToJsonElement(result).jsonObject
            assertTrue(json.containsKey("days"), "Result should contain days key")
            assertTrue(json.containsKey("matching_days"), "Result should contain matching_days key")
        }
    }

    @Test
    fun unknownTool_returnsErrorJson() {
        runBlocking {
            val args = buildJsonObject {}

            val result = WeatherTools.handleToolCall("unknown_tool", args, mockRepository)

            val json = Json.parseToJsonElement(result).jsonObject
            assertTrue(json.containsKey("error"), "Result should contain error key")
            assertTrue(
                json["error"]?.toString()?.contains("Unknown tool") == true,
                "Error message should indicate unknown tool"
            )
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
        val end = if (toDate != null) {
            toDate
        } else {
            fromDate
        }

        val days = TestWeatherRepositoryFactory.generateTestDays(fromDate, end)

        return TestWeatherRepositoryFactory.createWeatherResponse(place, days = days)
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
        return TestWeatherRepositoryFactory.createWeatherResponse(place)
    }
}
