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
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * KIM-321: all five weather tools must gate any paid Visual Crossing fetch behind a
 * permission_required response when data is Partial or Missing, naming downloaded and
 * missing months separately in full month-name format. No tool may auto-fetch.
 */
class WeatherToolsPermissionGateTest {

    /**
     * Fake whose availability and downloaded-month set are configurable. getDaysRange
     * records whether it was called so the tests can prove no fetch happened on a gate.
     */
    private class GateFakeRepo(
        var availability: DataAvailabilityStatus = DataAvailabilityStatus.Partial,
        var downloadedMonths: Set<String> = emptySet()
    ) : WeatherRepository {
        var getDaysRangeCalled = false

        override suspend fun getSavedPlaceNames(): List<String> = listOf("Tarifa")
        override suspend fun getPlaceDayCounts(): Map<String, Long> = emptyMap()
        override suspend fun getDownloadedMonths(place: String): Set<String> = downloadedMonths
        override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse? = null
        override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse {
            getDaysRangeCalled = true
            return TestWeatherRepositoryFactory.createWeatherResponse(place)
                .copy(days = TestWeatherRepositoryFactory.generateTestDays(fromDate, toDate ?: fromDate))
        }
        override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse =
            getDaysRange(place, "2025-10-01", null)
        override suspend fun deletePlace(placeName: String) {}
        override suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse =
            getDaysRange(place, "$year-01-01", null)
        override suspend fun searchForLocations(query: String): List<GeoSearchResult> = emptyList()
        override suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse =
            getDaysRange(place.name, "2025-10-01", null)
        override suspend fun checkDataAvailability(place: String, fromDate: String, toDate: String): DataAvailabilityStatus =
            availability
        override suspend fun fetchAndPersistStations(place: String): StationsResult = StationsResult.Empty
        override suspend fun getPersistedStations(place: String): List<Station> = emptyList()
    }

    private fun windArgs() = buildJsonObject {
        put("location_name", "Tarifa")
        put("start_date", "2025-10-01")
        put("end_date", "2025-11-30")
    }

    // ---- monthsInRange ----

    @Test
    fun monthsInRange_spansInclusiveMonths() {
        assertEquals(listOf("2025-10", "2025-11", "2025-12"), WeatherTools.monthsInRange("2025-10-05", "2025-12-20"))
        assertEquals(listOf("2025-10"), WeatherTools.monthsInRange("2025-10-01", "2025-10-31"))
    }

    @Test
    fun monthsInRange_invertedOrInvalidReturnsEmpty() {
        assertTrue(WeatherTools.monthsInRange("2025-12-01", "2025-10-01").isEmpty())
        assertTrue(WeatherTools.monthsInRange("nope", "2025-10-01").isEmpty())
    }

    // ---- get_wind_summary ----

    @Test
    fun windSummary_partialGatesAndNamesMonths() = runTest {
        val repo = GateFakeRepo(
            availability = DataAvailabilityStatus.Partial,
            downloadedMonths = setOf("2025-10")
        )
        val result = WeatherTools.handleToolCall("get_wind_summary", windArgs(), repo)
        val json = Json.parseToJsonElement(result).jsonObject

        assertEquals("permission_required", json["status"]?.jsonPrimitive?.content)
        assertFalse(repo.getDaysRangeCalled, "Partial data must not trigger a fetch")
        val downloaded = json["downloaded_months"]!!.jsonArray.map { it.jsonPrimitive.content }
        val missing = json["missing_months"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("October 2025"), downloaded)
        assertEquals(listOf("November 2025"), missing)
    }

    @Test
    fun windSummary_missingGatesWithNoDownloadedMonths() = runTest {
        val repo = GateFakeRepo(availability = DataAvailabilityStatus.Missing, downloadedMonths = emptySet())
        val result = WeatherTools.handleToolCall("get_wind_summary", windArgs(), repo)
        val json = Json.parseToJsonElement(result).jsonObject

        assertEquals("permission_required", json["status"]?.jsonPrimitive?.content)
        assertFalse(repo.getDaysRangeCalled)
        assertTrue(json["downloaded_months"]!!.jsonArray.isEmpty())
        assertEquals(listOf("October 2025", "November 2025"), json["missing_months"]!!.jsonArray.map { it.jsonPrimitive.content })
    }

    // ---- get_monthly_stats ----

    @Test
    fun monthlyStats_partialGates() = runTest {
        val repo = GateFakeRepo(availability = DataAvailabilityStatus.Partial, downloadedMonths = emptySet())
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("year", "2025"); put("month", "10")
        }
        val result = WeatherTools.handleToolCall("get_monthly_stats", args, repo)
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals("permission_required", json["status"]?.jsonPrimitive?.content)
        assertFalse(repo.getDaysRangeCalled)
        assertEquals(listOf("October 2025"), json["missing_months"]!!.jsonArray.map { it.jsonPrimitive.content })
    }

    // ---- get_best_days ----

    @Test
    fun bestDays_missingGates() = runTest {
        val repo = GateFakeRepo(availability = DataAvailabilityStatus.Missing, downloadedMonths = emptySet())
        val args = buildJsonObject {
            put("location_name", "Tarifa"); put("start_date", "2025-10-01"); put("end_date", "2025-10-31")
        }
        val result = WeatherTools.handleToolCall("get_best_days", args, repo)
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals("permission_required", json["status"]?.jsonPrimitive?.content)
        assertFalse(repo.getDaysRangeCalled)
    }

    // ---- get_weather_metrics ----

    @Test
    fun weatherMetrics_partialNamesDownloadedAndMissingSeparately() = runTest {
        val repo = GateFakeRepo(
            availability = DataAvailabilityStatus.Partial,
            downloadedMonths = setOf("2025-10")
        )
        val args = buildJsonObject {
            put("location_name", "Tarifa")
            put("start_date", "2025-10-01")
            put("end_date", "2025-11-30")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
            }
        }
        val result = WeatherTools.handleToolCall("get_weather_metrics", args, repo)
        val json = Json.parseToJsonElement(result).jsonObject

        assertEquals("permission_required", json["status"]?.jsonPrimitive?.content)
        assertFalse(repo.getDaysRangeCalled)
        assertEquals(listOf("October 2025"), json["downloaded_months"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("November 2025"), json["missing_months"]!!.jsonArray.map { it.jsonPrimitive.content })
        // The message must not describe the already-downloaded month as needing a fetch.
        assertContains(result, "November 2025")
    }

    @Test
    fun availableDataStillFetchesNormally() = runTest {
        // Sanity: with Available status the wind tool proceeds (no gate) and returns data.
        val repo = GateFakeRepo(availability = DataAvailabilityStatus.Available)
        val result = WeatherTools.handleToolCall("get_wind_summary", windArgs(), repo)
        assertTrue(repo.getDaysRangeCalled)
        assertFalse(result.contains("permission_required"))
    }
}
