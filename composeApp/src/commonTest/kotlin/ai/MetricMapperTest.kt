package ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for MetricMapper - metric name translation and unit conversion.
 */
class MetricMapperTest {

    // ========== mapMetricName Tests ==========

    @Test
    fun testMapTemperatureMetrics() {
        assertEquals("temp", MetricMapper.mapMetricName("temperature"))
        assertEquals("temp", MetricMapper.mapMetricName("temp"))
        assertEquals("temp", MetricMapper.mapMetricName("TEMPERATURE"))
        assertEquals("tempmax", MetricMapper.mapMetricName("temp_max"))
        assertEquals("tempmax", MetricMapper.mapMetricName("temperature_max"))
        assertEquals("tempmin", MetricMapper.mapMetricName("temp_min"))
        assertEquals("feelslike", MetricMapper.mapMetricName("feels_like"))
        assertEquals("feelslikemax", MetricMapper.mapMetricName("feels_like_max"))
        assertEquals("dew", MetricMapper.mapMetricName("dew_point"))
    }

    @Test
    fun testMapHumidityMetrics() {
        assertEquals("humidity", MetricMapper.mapMetricName("humidity"))
        assertEquals("humidity", MetricMapper.mapMetricName("relative_humidity"))
        assertEquals("humidity", MetricMapper.mapMetricName("rh"))
    }

    @Test
    fun testMapPrecipitationMetrics() {
        assertEquals("precip", MetricMapper.mapMetricName("precipitation"))
        assertEquals("precip", MetricMapper.mapMetricName("rainfall"))
        assertEquals("precip", MetricMapper.mapMetricName("rain"))
        assertEquals("precip", MetricMapper.mapMetricName("precip"))
        assertEquals("precipprob", MetricMapper.mapMetricName("rain_probability"))
        assertEquals("precipprob", MetricMapper.mapMetricName("precip_prob"))
        assertEquals("precipcover", MetricMapper.mapMetricName("rain_coverage"))
        assertEquals("snow", MetricMapper.mapMetricName("snow"))
        assertEquals("snow", MetricMapper.mapMetricName("snowfall"))
        assertEquals("snowdepth", MetricMapper.mapMetricName("snow_depth"))
    }

    @Test
    fun testMapWindMetrics() {
        assertEquals("windspeed", MetricMapper.mapMetricName("wind"))
        assertEquals("windspeed", MetricMapper.mapMetricName("wind_speed"))
        assertEquals("windspeed", MetricMapper.mapMetricName("windspeed"))
        assertEquals("windgust", MetricMapper.mapMetricName("wind_gust"))
        assertEquals("windgust", MetricMapper.mapMetricName("gust"))
        assertEquals("winddir", MetricMapper.mapMetricName("wind_direction"))
        assertEquals("winddir", MetricMapper.mapMetricName("winddir"))
    }

    @Test
    fun testMapAtmosphereMetrics() {
        assertEquals("visibility", MetricMapper.mapMetricName("visibility"))
        assertEquals("visibility", MetricMapper.mapMetricName("visible_distance"))
        assertEquals("cloudcover", MetricMapper.mapMetricName("cloud_cover"))
        assertEquals("cloudcover", MetricMapper.mapMetricName("clouds"))
        assertEquals("pressure", MetricMapper.mapMetricName("pressure"))
        assertEquals("pressure", MetricMapper.mapMetricName("atmospheric_pressure"))
    }

    @Test
    fun testMapSolarMetrics() {
        assertEquals("uvindex", MetricMapper.mapMetricName("uv_index"))
        assertEquals("uvindex", MetricMapper.mapMetricName("uv"))
        assertEquals("solarradiation", MetricMapper.mapMetricName("solar_radiation"))
        assertEquals("solarradiation", MetricMapper.mapMetricName("solar"))
        assertEquals("solarenergy", MetricMapper.mapMetricName("solar_energy"))
    }

    @Test
    fun testMapConditionMetrics() {
        assertEquals("conditions", MetricMapper.mapMetricName("conditions"))
        assertEquals("conditions", MetricMapper.mapMetricName("weather_conditions"))
        assertEquals("description", MetricMapper.mapMetricName("description"))
        assertEquals("description", MetricMapper.mapMetricName("weather_description"))
        assertEquals("icon", MetricMapper.mapMetricName("icon"))
        assertEquals("icon", MetricMapper.mapMetricName("weather_icon"))
    }

    @Test
    fun testMapSunMoonMetrics() {
        assertEquals("sunrise", MetricMapper.mapMetricName("sunrise"))
        assertEquals("sunset", MetricMapper.mapMetricName("sunset"))
        assertEquals("moonphase", MetricMapper.mapMetricName("moon_phase"))
        assertEquals("moonphase", MetricMapper.mapMetricName("moonphase"))
    }

    @Test
    fun testMapMetricFallbackToRawName() {
        // Unknown metric should fall back to raw name
        assertEquals("unknownmetric", MetricMapper.mapMetricName("unknownmetric"))
        assertEquals("custom_field", MetricMapper.mapMetricName("custom_field"))
    }

    @Test
    fun testMapMetricCaseInsensitive() {
        assertEquals("temp", MetricMapper.mapMetricName("TEMPERATURE"))
        assertEquals("temp", MetricMapper.mapMetricName("Temperature"))
        assertEquals("windspeed", MetricMapper.mapMetricName("WIND_SPEED"))
        assertEquals("humidity", MetricMapper.mapMetricName("HUMIDITY"))
    }

    @Test
    fun testMapMetricTrimWhitespace() {
        assertEquals("temp", MetricMapper.mapMetricName("  temperature  "))
        assertEquals("windspeed", MetricMapper.mapMetricName(" wind_speed "))
    }

    // ========== getUnits Tests ==========

    @Test
    fun testGetUnitsTemperature() {
        assertEquals("°C", MetricMapper.getUnits("temp"))
        assertEquals("°C", MetricMapper.getUnits("tempmax"))
        assertEquals("°C", MetricMapper.getUnits("tempmin"))
        assertEquals("°C", MetricMapper.getUnits("feelslike"))
        assertEquals("°C", MetricMapper.getUnits("dew"))
    }

    @Test
    fun testGetUnitsPercentage() {
        assertEquals("%", MetricMapper.getUnits("humidity"))
        assertEquals("%", MetricMapper.getUnits("precipprob"))
        assertEquals("%", MetricMapper.getUnits("cloudcover"))
    }

    @Test
    fun testGetUnitsDistance() {
        assertEquals("mm", MetricMapper.getUnits("precip"))
        assertEquals("mm", MetricMapper.getUnits("snow"))
        assertEquals("mm", MetricMapper.getUnits("snowdepth"))
        assertEquals("km", MetricMapper.getUnits("visibility"))
    }

    @Test
    fun testGetUnitsWind() {
        assertEquals("knots", MetricMapper.getUnits("windspeed"))
        assertEquals("knots", MetricMapper.getUnits("windgust"))
    }

    @Test
    fun testGetUnitsPressure() {
        assertEquals("hPa", MetricMapper.getUnits("pressure"))
    }

    @Test
    fun testGetUnitsSolar() {
        assertEquals("MJ/m²", MetricMapper.getUnits("solarenergy"))
        assertEquals("W/m²", MetricMapper.getUnits("solarradiation"))
    }

    @Test
    fun testGetUnitsNoUnit() {
        assertEquals("", MetricMapper.getUnits("conditions"))
        assertEquals("", MetricMapper.getUnits("description"))
        assertEquals("", MetricMapper.getUnits("icon"))
        assertEquals("", MetricMapper.getUnits("sunrise"))
        assertEquals("", MetricMapper.getUnits("sunset"))
    }

    @Test
    fun testGetUnitsUnknownField() {
        assertEquals("", MetricMapper.getUnits("unknownfield"))
    }

    // ========== formatValue Tests ==========

    @Test
    fun testFormatValueWindConversion() {
        // Wind: km/h to knots (divide by 1.852)
        val result = MetricMapper.formatValue("windspeed", 5.0)
        assertEquals(2.7, result)  // 5.0 / 1.852 = 2.7 rounded to 1 decimal
    }

    @Test
    fun testFormatValueWindGustConversion() {
        val result = MetricMapper.formatValue("windgust", 10.0)
        assertEquals(5.4, result)  // 10.0 / 1.852 = 5.4 rounded to 1 decimal
    }

    @Test
    fun testFormatValueTemperatureRounding() {
        val result = MetricMapper.formatValue("temp", 23.456)
        assertEquals(23.5, result)  // Rounded to 1 decimal
    }

    @Test
    fun testFormatValueHumidityRounding() {
        val result = MetricMapper.formatValue("humidity", 67.8901)
        assertEquals(67.9, result)
    }

    @Test
    fun testFormatValuePrecipitationRounding() {
        val result = MetricMapper.formatValue("precip", 12.345)
        assertEquals(12.3, result)
    }

    @Test
    fun testFormatValueNoConversion() {
        // Conditions - categorical, no conversion
        val result = MetricMapper.formatValue("conditions", "Sunny")
        assertEquals("Sunny", result)
    }

    @Test
    fun testFormatValueNullInput() {
        val result = MetricMapper.formatValue("temp", null)
        assertEquals(null, result)
    }

    @Test
    fun testFormatValueIntegerInput() {
        // Should handle integers
        val result = MetricMapper.formatValue("humidity", 75)
        assertEquals(75.0, result)
    }

    @Test
    fun testFormatValueZero() {
        val result = MetricMapper.formatValue("temp", 0.0)
        assertEquals(0.0, result)
    }

    @Test
    fun testFormatValueNegative() {
        val result = MetricMapper.formatValue("temp", -5.456)
        assertEquals(-5.5, result)
    }

    // ========== getDisplayName Tests ==========

    @Test
    fun testGetDisplayNameSingleWord() {
        assertEquals("Temperature", MetricMapper.getDisplayName("temperature"))
        assertEquals("Humidity", MetricMapper.getDisplayName("humidity"))
        assertEquals("Visibility", MetricMapper.getDisplayName("visibility"))
    }

    @Test
    fun testGetDisplayNameMultipleWords() {
        assertEquals("Wind Speed", MetricMapper.getDisplayName("wind_speed"))
        assertEquals("Feels Like", MetricMapper.getDisplayName("feels_like"))
        assertEquals("Cloud Cover", MetricMapper.getDisplayName("cloud_cover"))
        assertEquals("Solar Energy", MetricMapper.getDisplayName("solar_energy"))
    }

    @Test
    fun testGetDisplayNameCapitalization() {
        // First letter of each word should be capitalized
        val result = MetricMapper.getDisplayName("wind_gust_direction")
        assertEquals("Wind Gust Direction", result)
    }

    @Test
    fun testGetDisplayNameEmptyString() {
        assertEquals("", MetricMapper.getDisplayName(""))
    }

    // ========== Integration Tests ==========

    @Test
    fun testMetricMappingAndUnitsCombination() {
        // Test a realistic scenario: map metric name, get units
        val metricName = "wind_speed"
        val fieldName = MetricMapper.mapMetricName(metricName)
        val units = MetricMapper.getUnits(fieldName)

        assertEquals("windspeed", fieldName)
        assertEquals("knots", units)
    }

    @Test
    fun testValueFormattingWithUnits() {
        // Map -> get units -> format value
        val fieldName = MetricMapper.mapMetricName("rainfall")
        val units = MetricMapper.getUnits(fieldName)
        val formatted = MetricMapper.formatValue(fieldName, 5.123)

        assertEquals("precip", fieldName)
        assertEquals("mm", units)
        assertEquals(5.1, formatted)
    }

    @Test
    fun testMultipleMetricScenario() {
        // Scenario: User asks for multiple metrics
        val metrics = listOf("temperature", "wind_speed", "visibility")

        metrics.forEach { metric ->
            val fieldName = MetricMapper.mapMetricName(metric)
            val units = MetricMapper.getUnits(fieldName)

            assertTrue(fieldName.isNotEmpty(), "Field name should not be empty for $metric")
            assertTrue(units.isNotEmpty() || metric == "conditions", "Should have units or be categorical")
        }
    }
}
