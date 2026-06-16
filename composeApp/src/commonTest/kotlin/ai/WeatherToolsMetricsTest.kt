package ai

import core.Day
import core.GeoSearchResult
import core.WeatherRepository
import core.WeatherResponse
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * Unit tests for WeatherTools.get_weather_metrics handler.
 * Tests the flexible metric-based weather query functionality.
 */
class WeatherToolsMetricsTest {

    private val mockRepository = object : WeatherRepository {
        override suspend fun getSavedPlaceNames(): List<String> =
            listOf("Oahu", "Maui", "Kauai")

        override suspend fun getPlaceDayCounts(): Map<String, Long> = emptyMap()

        override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse? =
            if (resolvedPlace == "unknown") null else getDaysRange(resolvedPlace, "2026-02-26", "2026-02-27")

        override suspend fun getDaysRange(
            place: String,
            fromDate: String,
            toDate: String?
        ): WeatherResponse {
            // Mock weather data
            val days = listOf(
                Day(
                    datetime = "2026-02-26",
                    datetimeEpoch = 1708963200,
                    tempmax = 25.1,
                    tempmin = 21.7,
                    temp = 23.4,
                    feelslikemax = 25.2,
                    feelslikemin = 21.5,
                    feelslike = 24.2,
                    dew = 18.5,
                    humidity = 65.0,
                    precip = 0.0,
                    precipprob = 10.0,
                    precipcover = 0.0,
                    preciptype = null,
                    snow = null,
                    snowdepth = null,
                    windgust = 8.1,   // km/h
                    windspeed = 5.2,  // km/h
                    winddir = 180.0,
                    pressure = 1013.0,
                    cloudcover = 45.0,
                    visibility = 14.5,
                    solarradiation = 185.5,
                    solarenergy = 16.2,
                    uvindex = 6.5,
                    sunrise = "06:45",
                    sunriseEpoch = null,
                    sunset = "18:30",
                    sunsetEpoch = null,
                    moonphase = 0.25,
                    conditions = "Partly Cloudy",
                    description = "Partly cloudy throughout the day",
                    icon = "partly-cloudy-day",
                    stations = null,
                    source = "obs"
                ),
                Day(
                    datetime = "2026-02-27",
                    datetimeEpoch = 1709049600,
                    tempmax = 26.0,
                    tempmin = 22.3,
                    temp = 24.1,
                    feelslikemax = 26.1,
                    feelslikemin = 22.2,
                    feelslike = 25.0,
                    dew = 19.2,
                    humidity = 68.0,
                    precip = 2.3,  // Light rain
                    precipprob = 60.0,
                    precipcover = null,
                    preciptype = listOf("rain"),
                    snow = null,
                    snowdepth = null,
                    windgust = 9.5,
                    windspeed = 6.1,
                    winddir = 175.0,
                    pressure = 1012.0,
                    cloudcover = 65.0,
                    visibility = 12.0,
                    solarradiation = 150.0,
                    solarenergy = 13.5,
                    uvindex = 5.2,
                    sunrise = "06:44",
                    sunriseEpoch = null,
                    sunset = "18:31",
                    sunsetEpoch = null,
                    moonphase = 0.30,
                    conditions = "Rainy",
                    description = "Rainy periods",
                    icon = "rain",
                    stations = null,
                    source = "obs"
                )
            )

            return WeatherResponse(
                resolvedAddress = place,
                address = place,
                latitude = 21.3099,
                longitude = -157.8581,
                timezone = "Pacific/Honolulu",
                tzoffset = -10.0,
                days = if (place == "unknown") null else days,
                stations = null
            )
        }

        override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse =
            getDaysRange(place, "2026-02-20", "2026-02-27")

        override suspend fun deletePlace(placeName: String) {}

        override suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse =
            getDaysRange(place, "$year-${month.toString().padStart(2, '0')}-01", "$year-${month.toString().padStart(2, '0')}-28")

        override suspend fun searchForLocations(query: String): List<GeoSearchResult> =
            emptyList()

        override suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse =
            getDaysRange(place.name, "2026-02-26", "2026-02-27")

        override suspend fun checkDataAvailability(
            place: String,
            fromDate: String,
            toDate: String
        ): core.DataAvailabilityStatus {
            // Mock: always return Available for testing
            return core.DataAvailabilityStatus.Available
        }
        override suspend fun fetchAndPersistStations(place: String): core.StationsResult = core.StationsResult.Empty
        override suspend fun getPersistedStations(place: String): List<core.Station> = emptyList()
    }

    // ========== Valid Request Tests ==========

    @Test
    fun testGetWeatherMetricsValidRequest() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-27")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
                add(JsonPrimitive("visibility"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertTrue(result.isNotEmpty())
        assertContains(result, "Oahu")
        assertContains(result, "temperature")
        assertContains(result, "visibility")
    }

    @Test
    fun testGetWeatherMetricsSingleMetric() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-26")
            putJsonArray("metrics") {
                add(JsonPrimitive("visibility"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertContains(result, "visibility")
        assertContains(result, "14.5")  // Should have the visibility value
    }

    @Test
    fun testGetWeatherMetricsMultipleMetrics() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-26")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
                add(JsonPrimitive("humidity"))
                add(JsonPrimitive("wind_speed"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertContains(result, "temperature")
        assertContains(result, "humidity")
        assertContains(result, "wind_speed")
        assertContains(result, "23.4")  // temperature value
        assertContains(result, "65")    // humidity value
    }

    @Test
    fun testGetWeatherMetricsWindConversion() = runTest {
        // Verify that wind speed is converted from km/h to knots
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-26")
            putJsonArray("metrics") {
                add(JsonPrimitive("wind_speed"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        // 5.2 km/h / 1.852 = 2.8 knots
        assertContains(result, "2.8")
    }

    @Test
    fun testGetWeatherMetricsDateRange() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-27")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        // Should have data for both dates
        assertContains(result, "2026-02-26")
        assertContains(result, "2026-02-27")
        assertContains(result, "23.4")  // Feb 26 temp
        assertContains(result, "24.1")  // Feb 27 temp
    }

    @Test
    fun testGetWeatherMetricsFriendlyMetricNames() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-26")
            putJsonArray("metrics") {
                add(JsonPrimitive("rainfall"))  // Friendly name for precip
                add(JsonPrimitive("feels_like"))  // Friendly name for feelslike
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertContains(result, "rainfall")
        assertContains(result, "feels_like")
    }

    @Test
    fun testGetWeatherMetricsNullValues() = runTest {
        // Some fields may be null (like snow in tropical location)
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-26")
            putJsonArray("metrics") {
                add(JsonPrimitive("snow"))  // Should be null in mock data
                add(JsonPrimitive("temperature"))  // Non-null
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        // Should still include temperature
        assertContains(result, "temperature")
        assertContains(result, "23.4")
    }

    // ========== Error Handling Tests ==========

    @Test
    fun testGetWeatherMetricsMissingLocationName() = runTest {
        val args = buildJsonObject {
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-27")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertContains(result, "error")
        assertContains(result.lowercase(), "location_name")
    }

    @Test
    fun testGetWeatherMetricsMissingStartDate() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("end_date", "2026-02-27")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertContains(result, "error")
        assertContains(result.lowercase(), "start_date")
    }

    @Test
    fun testGetWeatherMetricsMissingEndDate() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertContains(result, "error")
        assertContains(result.lowercase(), "end_date")
    }

    @Test
    fun testGetWeatherMetricsMissingMetrics() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-27")
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertContains(result, "error")
        assertContains(result.lowercase(), "metrics")
    }

    @Test
    fun testGetWeatherMetricsEmptyMetricsArray() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-27")
            putJsonArray("metrics") {}  // Empty array
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertContains(result, "error")
        assertContains(result.lowercase(), "empty")
    }

    @Test
    fun testGetWeatherMetricsInvalidDateFormat() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "02-26-2026")  // Wrong format
            put("end_date", "2026-02-27")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertContains(result, "error")
        assertContains(result.lowercase(), "date")
    }

    @Test
    fun testGetWeatherMetricsInvalidEndDateFormat() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "Feb 27, 2026")  // Wrong format
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertContains(result, "error")
        assertContains(result.lowercase(), "date")
    }

    @Test
    fun testGetWeatherMetricsLocationNotFound() = runTest {
        val args = buildJsonObject {
            put("location_name", "unknown")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-27")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        // Should return empty data but not an error (repo returns null days)
        assertContains(result, "No data available")
    }

    // ========== Response Format Tests ==========

    @Test
    fun testGetWeatherMetricsResponseStructure() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-26")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)

        // Parse JSON to verify structure
        val json = Json.parseToJsonElement(result).jsonObject
        assertTrue(json.containsKey("place"))
        assertTrue(json.containsKey("date_range"))
        assertTrue(json.containsKey("days_with_data"))
        assertTrue(json.containsKey("metrics_requested"))
        assertTrue(json.containsKey("data"))

        assertEquals("Oahu", json["place"]?.jsonPrimitive?.content)
        // Mock returns 2 days of data regardless of date range requested
        assertEquals(2, json["days_with_data"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun testGetWeatherMetricsDataArrayStructure() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-26")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
                add(JsonPrimitive("humidity"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        val json = Json.parseToJsonElement(result).jsonObject
        val dataArray = json["data"]?.jsonArray

        assertTrue(dataArray != null)
        assertTrue(dataArray.size > 0)

        val firstDay = dataArray.first().jsonObject
        assertTrue(firstDay.containsKey("date"))
        assertTrue(firstDay.containsKey("temperature (°C)"))
        assertTrue(firstDay.containsKey("humidity (%)"))
    }

    @Test
    fun testGetWeatherMetricsUnitsInResponse() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-26")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
                add(JsonPrimitive("visibility"))
                add(JsonPrimitive("wind_speed"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)

        // Response should include units in the key names
        assertContains(result, "temperature (°C)")
        assertContains(result, "visibility (km)")
        assertContains(result, "wind_speed (knots)")
    }

    // ========== Edge Case Tests ==========

    @Test
    fun testGetWeatherMetricsVeryLongDateRange() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-01-01")
            put("end_date", "2026-12-31")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
            }
        }

        // Should handle long date range (returns only the mock data)
        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertTrue(result.isNotEmpty())
        assertFalse(result.contains("error"))
    }

    @Test
    fun testGetWeatherMetricsSameDateRange() = runTest {
        // Same start and end date (single day)
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-26")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertContains(result, "2026-02-26")
    }

    @Test
    fun testGetWeatherMetricsAllAvailableMetrics() = runTest {
        val args = buildJsonObject {
            put("location_name", "Oahu")
            put("start_date", "2026-02-26")
            put("end_date", "2026-02-26")
            putJsonArray("metrics") {
                add(JsonPrimitive("temperature"))
                add(JsonPrimitive("humidity"))
                add(JsonPrimitive("rainfall"))
                add(JsonPrimitive("wind_speed"))
                add(JsonPrimitive("wind_gust"))
                add(JsonPrimitive("visibility"))
                add(JsonPrimitive("cloud_cover"))
                add(JsonPrimitive("pressure"))
                add(JsonPrimitive("uv_index"))
                add(JsonPrimitive("conditions"))
            }
        }

        val result = WeatherTools.handleToolCall("get_weather_metrics", args, mockRepository)
        assertTrue(result.isNotEmpty())
        assertFalse(result.contains("error"))
    }

    @Test
    fun testGetWeatherMetricsBackwardsCompatibility() = runTest {
        // Old tools should still work
        val tools = WeatherTools.allToolSchemas()
        val toolNames = tools.map { it.name }

        assertTrue(toolNames.contains("get_weather_metrics"), "New tool should exist")
        assertTrue(toolNames.contains("get_wind_summary"), "Old wind tool should still exist")
        assertTrue(toolNames.contains("get_best_days"), "Best days tool should still exist")
        assertTrue(toolNames.contains("list_saved_places"), "List places tool should still exist")

        // New tool should be first
        assertEquals("get_weather_metrics", tools.first().name)
    }
}
