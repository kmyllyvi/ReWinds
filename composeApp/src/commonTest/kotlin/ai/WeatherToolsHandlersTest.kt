package ai

import core.DataAvailabilityStatus
import core.Day
import core.GeoSearchResult
import core.Station
import core.StationsResult
import core.WeatherRepository
import core.WeatherResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the WeatherTools handlers that were previously untested:
 * get_wind_summary, list_saved_places, get_monthly_stats, get_best_days, and the
 * unknown-tool / dispatch paths. (get_weather_metrics is covered separately by
 * WeatherToolsMetricsTest.)
 *
 * Uses a configurable in-memory fake repository rather than mocks.
 */
class WeatherToolsHandlersTest {

    /**
     * Configurable fake. Returns [daysToReturn] from getDaysRange regardless of the
     * requested range (the handlers don't re-slice, they trust the repo). Set
     * [daysToReturn] to null to simulate "no data".
     */
    private class FakeRepo(
        var savedPlaces: List<String> = listOf("Tarifa", "Maui"),
        var daysToReturn: List<Day>? = null,
        var availability: DataAvailabilityStatus = DataAvailabilityStatus.Available
    ) : WeatherRepository {
        override suspend fun getSavedPlaceNames(): List<String> = savedPlaces
        override suspend fun getPlaceDayCounts(): Map<String, Long> = emptyMap()
        override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse? = null
        override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse =
            TestWeatherRepositoryFactory.createWeatherResponse(place = place)
                .copy(days = daysToReturn)
        override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse =
            getDaysRange(place, "2026-01-01", "2026-01-07")
        override suspend fun deletePlace(placeName: String) {}
        override suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse =
            getDaysRange(place, "$year-01-01", null)
        override suspend fun searchForLocations(query: String): List<GeoSearchResult> = emptyList()
        override suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse =
            getDaysRange(place.name, "2026-01-01", null)
        override suspend fun checkDataAvailability(place: String, fromDate: String, toDate: String): DataAvailabilityStatus =
            availability
        override suspend fun fetchAndPersistStations(place: String): StationsResult = StationsResult.Empty
        override suspend fun getPersistedStations(place: String): List<Station> = emptyList()
    }

    // windspeed 10 km/h -> 5.4 knots; gust 12 km/h -> 6.5 knots
    private fun standardDays() = TestWeatherRepositoryFactory.generateTestDays(
        startDate = "2026-06-01",
        endDate = "2026-06-03",
        baseWindSpeed = 10.0,
        baseWindGust = 12.0
    )

    // ===================== get_wind_summary =====================

    @Test
    fun windSummary_returnsKnotsAndCount() = runTest {
        val repo = FakeRepo(daysToReturn = standardDays())
        val args = buildJsonObject {
            put("location_name", "Tarifa")
            put("start_date", "2026-06-01")
            put("end_date", "2026-06-03")
        }
        val result = WeatherTools.handleToolCall("get_wind_summary", args, repo)
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals("Tarifa", json["place"]?.jsonPrimitive?.content)
        assertEquals(3, json["days_count"]?.jsonPrimitive?.content?.toInt())
        // 10 km/h / 1.852 = 5.4 knots
        assertContains(result, "5.4")
        // gust 12 km/h / 1.852 = 6.5 knots
        assertContains(result, "6.5")
    }

    @Test
    fun windSummary_sustainedFlagTrueInRange() = runTest {
        // 30 km/h -> 16.2 knots, within the 15..25 sustained band
        val repo = FakeRepo(daysToReturn = TestWeatherRepositoryFactory.generateTestDays("2026-06-01", "2026-06-01", baseWindSpeed = 30.0))
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("start_date", "2026-06-01"); put("end_date", "2026-06-01")
        }
        val result = WeatherTools.handleToolCall("get_wind_summary", args, repo)
        val first = Json.parseToJsonElement(result).jsonObject["wind_summary"]!!.jsonArray.first().jsonObject
        assertEquals(true, first["sustained_15_25"]?.jsonPrimitive?.content?.toBoolean())
    }

    @Test
    fun windSummary_emptyDaysGivesNote() = runTest {
        val repo = FakeRepo(daysToReturn = emptyList())
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("start_date", "2026-06-01"); put("end_date", "2026-06-03")
        }
        val result = WeatherTools.handleToolCall("get_wind_summary", args, repo)
        assertContains(result, "No data available")
    }

    @Test
    fun windSummary_missingLocationNameErrors() = runTest {
        val repo = FakeRepo(daysToReturn = standardDays())
        val args = buildJsonObject { put("start_date", "2026-06-01"); put("end_date", "2026-06-03") }
        val result = WeatherTools.handleToolCall("get_wind_summary", args, repo)
        assertContains(result, "error")
        assertContains(result.lowercase(), "location_name")
    }

    @Test
    fun windSummary_invalidDateErrors() = runTest {
        val repo = FakeRepo(daysToReturn = standardDays())
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("start_date", "06/01/2026"); put("end_date", "2026-06-03")
        }
        val result = WeatherTools.handleToolCall("get_wind_summary", args, repo)
        assertContains(result, "error")
        assertContains(result.lowercase(), "date")
    }

    // ===================== list_saved_places =====================

    @Test
    fun listSavedPlaces_returnsAllNames() = runTest {
        val repo = FakeRepo(savedPlaces = listOf("Tarifa", "Maui", "Kauai"))
        val result = WeatherTools.handleToolCall("list_saved_places", buildJsonObject {}, repo)
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals(3, json["count"]?.jsonPrimitive?.content?.toInt())
        val names = json["places"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("Tarifa", "Maui", "Kauai"), names)
    }

    @Test
    fun listSavedPlaces_emptyReturnsZero() = runTest {
        val repo = FakeRepo(savedPlaces = emptyList())
        val result = WeatherTools.handleToolCall("list_saved_places", buildJsonObject {}, repo)
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals(0, json["count"]?.jsonPrimitive?.content?.toInt())
    }

    // ===================== get_monthly_stats =====================

    @Test
    fun monthlyStats_aggregatesWindAndRain() = runTest {
        // Three days at 10 km/h windspeed (=5.4 kn) and 12 km/h gust (=6.5 kn), no precip.
        val repo = FakeRepo(daysToReturn = standardDays())
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("year", "2026"); put("month", "6")
        }
        val result = WeatherTools.handleToolCall("get_monthly_stats", args, repo)
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals("2026-06", json["month"]?.jsonPrimitive?.content)
        assertEquals(3, json["days_with_data"]?.jsonPrimitive?.content?.toInt())
        // avg/min/max wind all 5.4 since uniform
        assertEquals(5.4, json["avg_wind_knots"]?.jsonPrimitive?.content?.toDouble())
        assertEquals(5.4, json["min_wind_knots"]?.jsonPrimitive?.content?.toDouble())
        assertEquals(0, json["rainy_days"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun monthlyStats_invalidMonthErrors() = runTest {
        val repo = FakeRepo(daysToReturn = standardDays())
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("year", "2026"); put("month", "13")
        }
        val result = WeatherTools.handleToolCall("get_monthly_stats", args, repo)
        assertContains(result, "error")
        assertContains(result.lowercase(), "month")
    }

    @Test
    fun monthlyStats_nonIntegerYearErrors() = runTest {
        val repo = FakeRepo(daysToReturn = standardDays())
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("year", "twenty"); put("month", "6")
        }
        val result = WeatherTools.handleToolCall("get_monthly_stats", args, repo)
        assertContains(result, "error")
        assertContains(result.lowercase(), "year")
    }

    @Test
    fun monthlyStats_noDataGivesNote() = runTest {
        val repo = FakeRepo(daysToReturn = emptyList())
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("year", "2026"); put("month", "2")
        }
        val result = WeatherTools.handleToolCall("get_monthly_stats", args, repo)
        assertContains(result, "No data available")
    }

    // ===================== get_best_days =====================

    @Test
    fun bestDays_filtersByMinWind() = runTest {
        // standardDays are 5.4 knots. min_wind_speed=25 -> none match.
        val repo = FakeRepo(daysToReturn = standardDays())
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("start_date", "2026-06-01"); put("end_date", "2026-06-03")
            put("min_wind_speed", "25")
        }
        val result = WeatherTools.handleToolCall("get_best_days", args, repo)
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals(0, json["matching_days"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun bestDays_allMatchWhenCriteriaLoose() = runTest {
        val repo = FakeRepo(daysToReturn = standardDays())
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("start_date", "2026-06-01"); put("end_date", "2026-06-03")
            put("min_wind_speed", "4")
        }
        val result = WeatherTools.handleToolCall("get_best_days", args, repo)
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals(3, json["matching_days"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun bestDays_noRainExcludesRainyDays() = runTest {
        val days = standardDays().mapIndexed { i, d ->
            if (i == 0) d.copy(precip = 5.0) else d
        }
        val repo = FakeRepo(daysToReturn = days)
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("start_date", "2026-06-01"); put("end_date", "2026-06-03")
            put("no_rain", "true")
        }
        val result = WeatherTools.handleToolCall("get_best_days", args, repo)
        val json = Json.parseToJsonElement(result).jsonObject
        // One of three days is rainy -> 2 remain
        assertEquals(2, json["matching_days"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun bestDays_maxGustFilter() = runTest {
        // gust 12 km/h = 6.5 kn. max_gust=4 -> none pass.
        val repo = FakeRepo(daysToReturn = standardDays())
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("start_date", "2026-06-01"); put("end_date", "2026-06-03")
            put("max_gust", "4")
        }
        val result = WeatherTools.handleToolCall("get_best_days", args, repo)
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals(0, json["matching_days"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun bestDays_invalidDateErrors() = runTest {
        val repo = FakeRepo(daysToReturn = standardDays())
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("start_date", "nope"); put("end_date", "2026-06-03")
        }
        val result = WeatherTools.handleToolCall("get_best_days", args, repo)
        assertContains(result, "error")
        assertContains(result.lowercase(), "date")
    }

    // ===================== dispatch =====================

    @Test
    fun unknownToolReturnsError() = runTest {
        val repo = FakeRepo()
        val result = WeatherTools.handleToolCall("frobnicate", buildJsonObject {}, repo)
        assertContains(result, "error")
        assertContains(result, "Unknown tool")
    }

    @Test
    fun allToolSchemasContainExpectedTools() {
        val names = WeatherTools.allToolSchemas().map { it.name }.toSet()
        assertTrue(names.containsAll(setOf(
            "get_weather_metrics", "get_wind_summary", "list_saved_places",
            "get_monthly_stats", "get_best_days"
        )))
        assertFalse(names.contains("frobnicate"))
    }
}
