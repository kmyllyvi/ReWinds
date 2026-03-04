package ai

import core.WeatherRepository
import core.Log
import core.Day
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.round

// Multiplatform-compatible number formatter (String.format not available on iOS)
private fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

/**
 * Sealed class representing a tool available to Claude.
 * Each tool has a name, description, and input schema (as JSON).
 */
sealed class Tool {
    abstract val name: String
    abstract val description: String
    abstract val inputSchema: JsonObject

    /**
     * get_wind_summary: Fetches wind data for a location over a date range.
     * Returns average wind speed, gusts, and sustained wind calculations per day.
     */
    object GetWindSummary : Tool() {
        override val name = "get_wind_summary"
        override val description = "Get wind data for a saved location over a date range. Returns average wind speed, gusts, and sustained wind calculations per day."

        override val inputSchema: JsonObject = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("location_name") {
                    put("type", "string")
                    put("description", "Name of the saved place")
                }
                putJsonObject("start_date") {
                    put("type", "string")
                    put("description", "ISO date YYYY-MM-DD")
                }
                putJsonObject("end_date") {
                    put("type", "string")
                    put("description", "ISO date YYYY-MM-DD")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("location_name"))
                add(JsonPrimitive("start_date"))
                add(JsonPrimitive("end_date"))
            }
        }
    }

    /**
     * list_saved_places: Returns all saved location names in the user's library.
     */
    object ListSavedPlaces : Tool() {
        override val name = "list_saved_places"
        override val description = "List all saved place names that have weather data available."

        override val inputSchema: JsonObject = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                // No properties - this tool takes no input
            }
            putJsonArray("required") {
                // No required fields
            }
        }
    }

    /**
     * get_monthly_stats: Returns aggregated wind statistics for a given month.
     */
    object GetMonthlyStats : Tool() {
        override val name = "get_monthly_stats"
        override val description = "Get aggregated wind statistics for a saved location for a specific month. Returns min, max, average wind speeds and gust data."

        override val inputSchema: JsonObject = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("location_name") {
                    put("type", "string")
                    put("description", "Name of the saved place")
                }
                putJsonObject("year") {
                    put("type", "integer")
                    put("description", "Year (YYYY)")
                }
                putJsonObject("month") {
                    put("type", "integer")
                    put("description", "Month (1-12)")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("location_name"))
                add(JsonPrimitive("year"))
                add(JsonPrimitive("month"))
            }
        }
    }

    /**
     * get_best_days: Filters days matching specified wind criteria.
     */
    object GetBestDays : Tool() {
        override val name = "get_best_days"
        override val description = "Find days in a date range matching specific wind and weather criteria. Useful for finding ideal conditions for kitesurfing/windsurfing."

        override val inputSchema: JsonObject = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("location_name") {
                    put("type", "string")
                    put("description", "Name of the saved place")
                }
                putJsonObject("start_date") {
                    put("type", "string")
                    put("description", "ISO date YYYY-MM-DD")
                }
                putJsonObject("end_date") {
                    put("type", "string")
                    put("description", "ISO date YYYY-MM-DD")
                }
                putJsonObject("min_wind_speed") {
                    put("type", "number")
                    put("description", "Minimum wind speed in knots (optional)")
                }
                putJsonObject("max_wind_speed") {
                    put("type", "number")
                    put("description", "Maximum wind speed in knots (optional)")
                }
                putJsonObject("max_gust") {
                    put("type", "number")
                    put("description", "Maximum gust speed in knots (optional)")
                }
                putJsonObject("no_rain") {
                    put("type", "boolean")
                    put("description", "Exclude rainy days (optional)")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("location_name"))
                add(JsonPrimitive("start_date"))
                add(JsonPrimitive("end_date"))
            }
        }
    }

    /**
     * get_weather_metrics: Fetch ANY weather metric(s) for a location over a date range.
     * Flexible tool that accepts metric names and returns the requested data.
     */
    object GetWeatherMetrics : Tool() {
        override val name = "get_weather_metrics"
        override val description =
            """Retrieve any weather metric(s) for a location over a date range.
            |
            |Available metrics (use friendly names):
            |• Temperature: temperature, temp_max, temp_min, feels_like, dew_point
            |• Humidity & Rain: humidity, rainfall, rain_probability, snow
            |• Wind: wind_speed, wind_gust, wind_direction
            |• Atmosphere: visibility, cloud_cover, pressure
            |• Solar: uv_index, solar_energy, solar_radiation
            |• Conditions: conditions, description
            |• Other: sunrise, sunset, moon_phase
            |
            |Examples: ["temperature", "visibility"], ["wind_speed", "humidity", "rainfall"]
            """

        override val inputSchema: JsonObject = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("location_name") {
                    put("type", "string")
                    put("description", "Name of the saved place")
                }
                putJsonObject("start_date") {
                    put("type", "string")
                    put("description", "ISO date YYYY-MM-DD")
                }
                putJsonObject("end_date") {
                    put("type", "string")
                    put("description", "ISO date YYYY-MM-DD")
                }
                putJsonObject("metrics") {
                    put("type", "array")
                    putJsonObject("items") {
                        put("type", "string")
                    }
                    put("description",
                        "List of metrics to retrieve. Use friendly names like 'temperature', " +
                        "'visibility', 'humidity', 'rainfall', 'wind_speed', etc.")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("location_name"))
                add(JsonPrimitive("start_date"))
                add(JsonPrimitive("end_date"))
                add(JsonPrimitive("metrics"))
            }
        }
    }
}

/**
 * WeatherTools manages the tool infrastructure.
 * Provides all tool schemas and dispatches tool calls to handlers.
 */
object WeatherTools {
    /**
     * Returns all available tool schemas.
     * These schemas are passed to Claude API in requests.
     */
    fun allToolSchemas(): List<Tool> = listOf(
        Tool.GetWeatherMetrics,  // New: flexible metric-based queries
        Tool.GetWindSummary,
        Tool.ListSavedPlaces,
        Tool.GetMonthlyStats,
        Tool.GetBestDays
    )

    /**
     * Dispatches a tool call to the appropriate handler.
     * @param toolName The name of the tool being called (kebab-case)
     * @param args The arguments as a JsonObject
     * @param repo The WeatherRepository instance to query
     * @return A JSON string with the tool result or error
     */
    suspend fun handleToolCall(
        toolName: String,
        args: JsonObject,
        repo: WeatherRepository
    ): String = try {
        Log.d("WeatherTools: handling tool call '$toolName' with args: $args")

        when (toolName) {
            "get_weather_metrics" -> handleGetWeatherMetrics(args, repo)
            "get_wind_summary" -> handleGetWindSummary(args, repo)
            "list_saved_places" -> handleListSavedPlaces(repo)
            "get_monthly_stats" -> handleGetMonthlyStats(args, repo)
            "get_best_days" -> handleGetBestDays(args, repo)
            else -> {
                Log.e("WeatherTools: unknown tool '$toolName'")
                buildErrorJson("Unknown tool: $toolName", toolName)
            }
        }
    } catch (e: Exception) {
        Log.e("WeatherTools: exception in handleToolCall for '$toolName'", e)
        buildErrorJson("Internal error: ${e.message}", toolName)
    }

    /**
     * Get wind summary for a location over a date range.
     * Returns lean JSON with place info and daily wind summaries.
     */
    private suspend fun handleGetWindSummary(
        args: JsonObject,
        repo: WeatherRepository
    ): String = try {
        val locationName = args["location_name"]?.jsonPrimitive?.content
            ?: return buildErrorJson("Missing required field: location_name", "get_wind_summary")
        val startDate = args["start_date"]?.jsonPrimitive?.content
            ?: return buildErrorJson("Missing required field: start_date", "get_wind_summary")
        val endDate = args["end_date"]?.jsonPrimitive?.content
            ?: return buildErrorJson("Missing required field: end_date", "get_wind_summary")

        // Validate date format
        try {
            LocalDate.parse(startDate)
            LocalDate.parse(endDate)
        } catch (e: Exception) {
            return buildErrorJson("Invalid date format. Expected ISO 8601 (YYYY-MM-DD)", "get_wind_summary")
        }

        val weatherResponse = repo.getDaysRange(locationName, startDate, endDate)

        if (weatherResponse.days.isNullOrEmpty()) {
            return buildJsonObject {

                put("date_range", "$startDate to $endDate")
                putJsonArray("wind_summary") {}
                put("note", "No data available for this date range")
            }.toString()
        }

        val windSummary = weatherResponse.days.map { day ->
            buildJsonObject {
                put("date", day.datetime)
                // Convert m/s to knots (1 m/s ≈ 1.944 knots)
                if (day.windspeed != null) {
                    put("avg_wind_knots", (day.windspeed * 1.944).roundTo(1))
                }
                if (day.windgust != null) {
                    put("max_gust_knots", (day.windgust * 1.944).roundTo(1))
                }
                // Simple heuristic: sustained wind is between 15-25 knots
                val avgWindKnots = day.windspeed?.let { it * 1.944 } ?: 0.0
                put("sustained_15_25", avgWindKnots in 15.0..25.0)
                // Include temperature and precipitation for context
                if (day.temp != null) put("temp_c", day.temp)
                if (day.precip != null && day.precip > 0) put("precip_mm", day.precip)
            }
        }

        buildJsonObject {
            put("place", weatherResponse.resolvedAddress)
            put("date_range", "$startDate to $endDate")
            put("days_count", windSummary.size)
            putJsonArray("wind_summary") {
                windSummary.forEach { daySummary ->
                    add(daySummary)
                }
            }
        }.toString()
    } catch (e: Exception) {
        Log.e("handleGetWindSummary failed", e)
        buildErrorJson("Failed to fetch wind summary: ${e.message}", "get_wind_summary")
    }

    /**
     * List all saved place names.
     */
    private suspend fun handleListSavedPlaces(repo: WeatherRepository): String = try {
        val places = repo.getSavedPlaceNames()

        buildJsonObject {
            put("count", places.size)
            putJsonArray("places") {
                places.forEach { add(JsonPrimitive(it)) }
            }
        }.toString()
    } catch (e: Exception) {
        Log.e("handleListSavedPlaces failed", e)
        buildErrorJson("Failed to list saved places: ${e.message}", "list_saved_places")
    }

    /**
     * Get any weather metric(s) for a location over a date range.
     * Flexible tool that fetches requested metrics and returns formatted data.
     */
    private suspend fun handleGetWeatherMetrics(
        args: JsonObject,
        repo: WeatherRepository
    ): String = try {
        val locationName = args["location_name"]?.jsonPrimitive?.content
            ?: return buildErrorJson("Missing required field: location_name", "get_weather_metrics")
        val startDate = args["start_date"]?.jsonPrimitive?.content
            ?: return buildErrorJson("Missing required field: start_date", "get_weather_metrics")
        val endDate = args["end_date"]?.jsonPrimitive?.content
            ?: return buildErrorJson("Missing required field: end_date", "get_weather_metrics")

        val metricsElement = args["metrics"]
            ?: return buildErrorJson("Missing required field: metrics (array)", "get_weather_metrics")

        val metricsList = try {
            metricsElement.jsonArray.map { it.jsonPrimitive.content }
        } catch (e: Exception) {
            return buildErrorJson("Invalid metrics field: must be an array of strings", "get_weather_metrics")
        }

        if (metricsList.isEmpty()) {
            return buildErrorJson("metrics array cannot be empty", "get_weather_metrics")
        }

        // Validate date format
        try {
            LocalDate.parse(startDate)
            LocalDate.parse(endDate)
        } catch (e: Exception) {
            return buildErrorJson("Invalid date format. Expected ISO 8601 (YYYY-MM-DD)", "get_weather_metrics")
        }

        val weatherResponse = repo.getDaysRange(locationName, startDate, endDate)

        if (weatherResponse.days.isNullOrEmpty()) {
            return buildJsonObject {
                put("place", weatherResponse.resolvedAddress)
                put("date_range", "$startDate to $endDate")
                put("metrics_requested", metricsList.size)
                put("note", "No data available for this date range")
                putJsonArray("data") {}
            }.toString()
        }

        // Transform days to include only requested metrics
        val results = weatherResponse.days!!.map { day ->
            buildJsonObject {
                put("date", day.datetime)

                // Add each requested metric
                for (friendlyMetricName in metricsList) {
                    val fieldName = MetricMapper.mapMetricName(friendlyMetricName)
                    val value: Any? = when (fieldName) {
                        "datetime" -> day.datetime
                        "datetimeEpoch" -> day.datetimeEpoch
                        "tempmax" -> day.tempmax
                        "tempmin" -> day.tempmin
                        "temp" -> day.temp
                        "feelslikemax" -> day.feelslikemax
                        "feelslikemin" -> day.feelslikemin
                        "feelslike" -> day.feelslike
                        "dew" -> day.dew
                        "humidity" -> day.humidity
                        "precip" -> day.precip
                        "precipprob" -> day.precipprob
                        "precipcover" -> day.precipcover
                        "preciptype" -> day.preciptype
                        "snow" -> day.snow
                        "snowdepth" -> day.snowdepth
                        "windgust" -> day.windgust
                        "windspeed" -> day.windspeed
                        "winddir" -> day.winddir
                        "pressure" -> day.pressure
                        "cloudcover" -> day.cloudcover
                        "visibility" -> day.visibility
                        "solarradiation" -> day.solarradiation
                        "solarenergy" -> day.solarenergy
                        "uvindex" -> day.uvindex
                        "sunrise" -> day.sunrise
                        "sunriseEpoch" -> day.sunriseEpoch
                        "sunset" -> day.sunset
                        "sunsetEpoch" -> day.sunsetEpoch
                        "moonphase" -> day.moonphase
                        "conditions" -> day.conditions
                        "description" -> day.description
                        "icon" -> day.icon
                        "source" -> day.source
                        else -> null
                    }

                    if (value != null) {
                        val formattedValue = MetricMapper.formatValue(fieldName, value)
                        val units = MetricMapper.getUnits(fieldName)
                        val displayKey = if (units.isNotEmpty()) {
                            "${friendlyMetricName} (${units})"
                        } else {
                            friendlyMetricName
                        }
                        put(displayKey, JsonPrimitive(formattedValue.toString()))
                    }
                }
            }
        }

        buildJsonObject {
            put("place", weatherResponse.resolvedAddress)
            put("date_range", "$startDate to $endDate")
            put("days_with_data", results.size)
            putJsonArray("metrics_requested") {
                metricsList.forEach { add(JsonPrimitive(it)) }
            }
            putJsonArray("data") {
                results.forEach { add(it) }
            }
        }.toString()
    } catch (e: Exception) {
        Log.e("handleGetWeatherMetrics failed", e)
        buildErrorJson("Failed to fetch weather metrics: ${e.message}", "get_weather_metrics")
    }

    /**
     * Get monthly statistics for a location.
     * Returns aggregated wind data for the entire month.
     */
    private suspend fun handleGetMonthlyStats(
        args: JsonObject,
        repo: WeatherRepository
    ): String = try {
        val locationName = args["location_name"]?.jsonPrimitive?.content
            ?: return buildErrorJson("Missing required field: location_name", "get_monthly_stats")
        val year = args["year"]?.jsonPrimitive?.content?.toIntOrNull()
            ?: return buildErrorJson("Missing or invalid field: year (must be integer)", "get_monthly_stats")
        val month = args["month"]?.jsonPrimitive?.content?.toIntOrNull()
            ?: return buildErrorJson("Missing or invalid field: month (must be integer 1-12)", "get_monthly_stats")

        if (month < 1 || month > 12) {
            return buildErrorJson("Invalid month: must be between 1 and 12", "get_monthly_stats")
        }

        // Build date range for the month
        val monthStr = month.toString().padStart(2, '0')
        val startDate = "$year-$monthStr-01"
        val endDate = "$year-$monthStr-${getDaysInMonth(year, month)}"

        val weatherResponse = repo.getDaysRange(locationName, startDate, endDate)

        if (weatherResponse.days.isNullOrEmpty()) {
            return buildJsonObject {
                put("place", weatherResponse.resolvedAddress)
                put("month", "$year-$monthStr")
                put("note", "No data available for this month")
            }.toString()
        }

        val days = weatherResponse.days!!
        val windSpeeds = days.mapNotNull { it.windspeed?.times(1.944) }
        val gusts = days.mapNotNull { it.windgust?.times(1.944) }
        val precipDays = days.count { it.precip != null && it.precip > 0 }

        val stats = buildJsonObject {
            put("place", weatherResponse.resolvedAddress)
            put("month", "$year-$monthStr")
            put("days_with_data", days.size)
            if (windSpeeds.isNotEmpty()) {
                put("avg_wind_knots", windSpeeds.average().roundTo(1))
                put("min_wind_knots", (windSpeeds.minOrNull() ?: 0.0).roundTo(1))
                put("max_wind_knots", (windSpeeds.maxOrNull() ?: 0.0).roundTo(1))
            }
            if (gusts.isNotEmpty()) {
                put("avg_gust_knots", gusts.average().roundTo(1))
                put("max_gust_knots", (gusts.maxOrNull() ?: 0.0).roundTo(1))
            }
            put("rainy_days", precipDays)
        }

        stats.toString()
    } catch (e: Exception) {
        Log.e("handleGetMonthlyStats failed", e)
        buildErrorJson("Failed to fetch monthly stats: ${e.message}", "get_monthly_stats")
    }

    /**
     * Filter days by wind and weather criteria.
     * Returns only days matching the specified conditions.
     */
    private suspend fun handleGetBestDays(
        args: JsonObject,
        repo: WeatherRepository
    ): String = try {
        val locationName = args["location_name"]?.jsonPrimitive?.content
            ?: return buildErrorJson("Missing required field: location_name", "get_best_days")
        val startDate = args["start_date"]?.jsonPrimitive?.content
            ?: return buildErrorJson("Missing required field: start_date", "get_best_days")
        val endDate = args["end_date"]?.jsonPrimitive?.content
            ?: return buildErrorJson("Missing required field: end_date", "get_best_days")

        // Parse optional filter parameters
        val minWindSpeed = args["min_wind_speed"]?.jsonPrimitive?.content?.toDoubleOrNull()
        val maxWindSpeed = args["max_wind_speed"]?.jsonPrimitive?.content?.toDoubleOrNull()
        val maxGust = args["max_gust"]?.jsonPrimitive?.content?.toDoubleOrNull()
        val noRain = args["no_rain"]?.jsonPrimitive?.content?.toBoolean() ?: false

        // Validate date format
        try {
            LocalDate.parse(startDate)
            LocalDate.parse(endDate)
        } catch (e: Exception) {
            return buildErrorJson("Invalid date format. Expected ISO 8601 (YYYY-MM-DD)", "get_best_days")
        }

        val weatherResponse = repo.getDaysRange(locationName, startDate, endDate)

        if (weatherResponse.days.isNullOrEmpty()) {
            return buildJsonObject {
                put("place", weatherResponse.resolvedAddress)
                put("date_range", "$startDate to $endDate")
                put("matching_days", 0)
                putJsonArray("days") {}
            }.toString()
        }

        // Filter days by criteria
        val matchingDays = weatherResponse.days!!.filter { day ->
            val windKnots = day.windspeed?.let { it * 1.944 } ?: 0.0
            val gustKnots = day.windgust?.let { it * 1.944 } ?: 0.0
            val hasPrecip = day.precip != null && day.precip > 0

            val windOk = (minWindSpeed == null || windKnots >= minWindSpeed) &&
                         (maxWindSpeed == null || windKnots <= maxWindSpeed)
            val gustOk = maxGust == null || gustKnots <= maxGust
            val rainOk = !noRain || !hasPrecip

            windOk && gustOk && rainOk
        }

        val matchingDaysSummary = matchingDays.map { day ->
            buildJsonObject {
                put("date", day.datetime)
                if (day.windspeed != null) {
                    put("avg_wind_knots", (day.windspeed * 1.944).roundTo(1))
                }
                if (day.windgust != null) {
                    put("max_gust_knots", (day.windgust * 1.944).roundTo(1))
                }
                if (day.conditions != null) put("conditions", day.conditions)
                if (day.precip != null && day.precip > 0) put("precip_mm", day.precip)
            }
        }

        buildJsonObject {
            put("place", weatherResponse.resolvedAddress)
            put("date_range", "$startDate to $endDate")
            put("matching_days", matchingDays.size)
            putJsonArray("days") {
                matchingDaysSummary.forEach { daySummary ->
                    add(daySummary)
                }
            }
        }.toString()
    } catch (e: Exception) {
        Log.e("handleGetBestDays failed", e)
        buildErrorJson("Failed to filter best days: ${e.message}", "get_best_days")
    }

    /**
     * Builds a structured error JSON response.
     */
    private fun buildErrorJson(message: String, toolName: String): String {
        return buildJsonObject {
            put("error", message)
            put("tool", toolName)
        }.toString()
    }

    /**
     * Helper to get the number of days in a month.
     */
    private fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 28
        }
    }

    /**
     * Helper to determine if a year is a leap year.
     */
    private fun isLeapYear(year: Int): Boolean {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
}
