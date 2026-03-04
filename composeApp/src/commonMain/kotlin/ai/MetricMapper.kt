package ai

import core.Log

/**
 * Maps user-friendly metric names to database field names.
 * Handles both common variations and abbreviations.
 */
object MetricMapper {
    /**
     * Maps a metric name to its corresponding database field.
     * Tries friendly names first, falls back to raw field name.
     */
    fun mapMetricName(metricName: String): String = when (metricName.lowercase().trim()) {
        // Temperature metrics
        "temperature", "temp", "air_temperature" -> "temp"
        "temperature_max", "temp_max", "tempmax", "max_temperature" -> "tempmax"
        "temperature_min", "temp_min", "tempmin", "min_temperature" -> "tempmin"
        "feels_like", "feelslike", "feels_like_temperature" -> "feelslike"
        "feels_like_max", "feelslikemax" -> "feelslikemax"
        "feels_like_min", "feelslikemin" -> "feelslikemin"
        "dew_point", "dew", "dewpoint" -> "dew"

        // Humidity & moisture
        "humidity", "relative_humidity", "rh" -> "humidity"
        "precipitation", "rainfall", "rain", "precip", "precip_amount" -> "precip"
        "rain_probability", "precip_prob", "precipprob", "probability_rain" -> "precipprob"
        "rain_coverage", "precip_cover", "precipcover", "precipitation_coverage" -> "precipcover"
        "precipitation_type", "precip_type", "rain_type" -> "preciptype"
        "snow", "snowfall" -> "snow"
        "snow_depth", "snowdepth" -> "snowdepth"

        // Wind metrics
        "wind", "wind_speed", "windspeed", "wind_velocity" -> "windspeed"
        "wind_gust", "windgust", "gust", "gust_speed", "wind_gusts" -> "windgust"
        "wind_direction", "winddir", "wind_dir", "direction" -> "winddir"

        // Visibility & atmosphere
        "visibility", "visible_distance", "visibility_distance" -> "visibility"
        "cloud_cover", "cloudcover", "clouds", "cloud_percentage" -> "cloudcover"
        "pressure", "barometric_pressure", "atmospheric_pressure" -> "pressure"

        // Solar & radiation
        "uv_index", "uvindex", "uv", "ultraviolet_index" -> "uvindex"
        "solar_radiation", "solarradiation", "solar" -> "solarradiation"
        "solar_energy", "solarenergy", "solar_power" -> "solarenergy"

        // Sun & moon
        "sunrise" -> "sunrise"
        "sunset" -> "sunset"
        "moon_phase", "moonphase" -> "moonphase"

        // Conditions
        "conditions", "weather_conditions", "weather" -> "conditions"
        "description", "weather_description" -> "description"
        "icon", "weather_icon" -> "icon"

        // If not in map, use as-is (for fields we might have missed)
        else -> metricName
    }

    /**
     * Returns the unit for a given database field name.
     */
    fun getUnits(fieldName: String): String = when (fieldName) {
        // Temperature: Celsius
        "temp", "tempmax", "tempmin", "feelslike", "feelslikemax", "feelslikemin", "dew" -> "°C"

        // Percentages
        "humidity", "precipprob", "cloudcover" -> "%"

        // Precipitation
        "precip", "snow", "snowdepth" -> "mm"

        // Wind: knots (will be converted from m/s)
        "windspeed", "windgust" -> "knots"

        // Pressure: hPa
        "pressure" -> "hPa"

        // Distance: km
        "visibility" -> "km"

        // Energy
        "solarenergy" -> "MJ/m²"
        "solarradiation" -> "W/m²"

        // No unit (categorical or timestamps)
        "conditions", "description", "icon", "sunrise", "sunset", "preciptype" -> ""
        "moonphase", "winddir" -> ""

        else -> ""
    }

    /**
     * Formats a value for display, applying conversions as needed.
     * @param fieldName The database field name
     * @param value The raw value from the database
     * @return Formatted value (may be converted from database units)
     */
    fun formatValue(fieldName: String, value: Any?): Any? {
        if (value == null) return null

        return when (fieldName) {
            // Convert wind from m/s to knots (1.944x)
            "windspeed", "windgust" -> {
                val ms = (value as? Number)?.toDouble() ?: return value
                (ms * 1.944).roundTo(1)
            }
            // Keep numbers at 1 decimal for readability
            "temp", "tempmax", "tempmin", "feelslike", "feelslikemax", "feelslikemin", "dew",
            "humidity", "precipprob", "cloudcover", "precip", "snow", "snowdepth",
            "pressure", "visibility", "solarenergy", "solarradiation", "uvindex" -> {
                val num = (value as? Number)?.toDouble() ?: return value
                num.roundTo(1)
            }
            else -> value
        }
    }

    /**
     * Friendly display name for a metric.
     */
    fun getDisplayName(metricName: String): String {
        return metricName.replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}

/**
 * Extension to round Double to N decimal places.
 */
private fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}
