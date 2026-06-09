package core

import place.DayWeatherSummary
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [DaysOfInterestFilter.matches], [DaysOfInterestFilter.filterSummary],
 * [degreesToCompass], and JSON round-tripping — all previously untested logic in
 * core/DaysOfInterestFilter.kt.
 */
class DaysOfInterestFilterTest {

    private fun day(
        avgTemp: Double? = 15.0,
        precipitation: Double? = 0.0,
        windDirection: Double? = null,
        sustainedWindSpeed: Double? = 25.0
    ) = DayWeatherSummary(
        date = "2026-06-01",
        description = null,
        maxTemp = null,
        minTemp = null,
        avgTemp = avgTemp,
        avgWindSpeed = null,
        maxWindSpeed = null,
        sustainedWindSpeed = sustainedWindSpeed,
        solarenergy = null,
        isFoggy = false,
        foggyHours = 0,
        precipitation = precipitation,
        windDirection = windDirection
    )

    // ---------- degreesToCompass ----------

    @Test
    fun degreesToCompass_cardinalPoints() {
        assertEquals("N", degreesToCompass(0.0))
        assertEquals("NE", degreesToCompass(45.0))
        assertEquals("E", degreesToCompass(90.0))
        assertEquals("SE", degreesToCompass(135.0))
        assertEquals("S", degreesToCompass(180.0))
        assertEquals("SW", degreesToCompass(225.0))
        assertEquals("W", degreesToCompass(270.0))
        assertEquals("NW", degreesToCompass(315.0))
    }

    @Test
    fun degreesToCompass_wrapsAt360() {
        // 360 + 22.5 -> index ((382.5)/45)=8 % 8 = 0 -> N
        assertEquals("N", degreesToCompass(360.0))
    }

    @Test
    fun degreesToCompass_roundsToNearestSector() {
        // 22.4 still rounds into N sector (just below the 22.5 boundary)
        assertEquals("N", degreesToCompass(22.4))
        // 23.0 crosses into NE
        assertEquals("NE", degreesToCompass(23.0))
    }

    // ---------- matches: temperature ----------

    @Test
    fun matches_rejectsBelowMinTemp() {
        val filter = DaysOfInterestFilter(naturalLanguageCriteria = "", minTempC = 10.0)
        assertFalse(filter.matches(day(avgTemp = 5.0)))
        assertTrue(filter.matches(day(avgTemp = 10.0)))
        assertTrue(filter.matches(day(avgTemp = 12.0)))
    }

    @Test
    fun matches_rejectsAboveMaxTemp() {
        val filter = DaysOfInterestFilter(naturalLanguageCriteria = "", maxTempC = 30.0)
        assertFalse(filter.matches(day(avgTemp = 31.0)))
        assertTrue(filter.matches(day(avgTemp = 30.0)))
    }

    @Test
    fun matches_nullTempTreatedAsExtremes() {
        // With a minTemp constraint, a null avgTemp uses Double.MIN_VALUE (~0), which is < 10 -> reject.
        // Note: Double.MIN_VALUE is the smallest *positive* value, so it is still below 10.0.
        val minFilter = DaysOfInterestFilter(naturalLanguageCriteria = "", minTempC = 10.0)
        assertFalse(minFilter.matches(day(avgTemp = null)))

        // With a maxTemp constraint, a null avgTemp uses Double.MAX_VALUE -> exceeds max -> reject.
        val maxFilter = DaysOfInterestFilter(naturalLanguageCriteria = "", maxTempC = 30.0)
        assertFalse(maxFilter.matches(day(avgTemp = null)))
    }

    // ---------- matches: rain ----------

    @Test
    fun matches_noRainRejectsAnyPrecip() {
        val filter = DaysOfInterestFilter(naturalLanguageCriteria = "", noRain = true)
        assertFalse(filter.matches(day(precipitation = 0.5)))
        assertTrue(filter.matches(day(precipitation = 0.0)))
        // null precipitation is treated as 0.0 -> allowed
        assertTrue(filter.matches(day(precipitation = null)))
    }

    // ---------- matches: wind direction ----------

    @Test
    fun matches_windDirectionFilter() {
        val filter = DaysOfInterestFilter(
            naturalLanguageCriteria = "",
            windDirections = listOf("SW", "W")
        )
        // 230 degrees -> SW (allowed)
        assertTrue(filter.matches(day(windDirection = 230.0)))
        // 90 degrees -> E (not allowed)
        assertFalse(filter.matches(day(windDirection = 90.0)))
    }

    @Test
    fun matches_windDirectionIgnoredWhenDayDirectionNull() {
        val filter = DaysOfInterestFilter(
            naturalLanguageCriteria = "",
            windDirections = listOf("N")
        )
        // No day direction -> direction filter is skipped, day passes.
        assertTrue(filter.matches(day(windDirection = null)))
    }

    // ---------- matches: wind speed ----------

    @Test
    fun matches_windSpeedRange() {
        val filter = DaysOfInterestFilter(
            naturalLanguageCriteria = "",
            minWindSpeedKmh = 20.0,
            maxWindSpeedKmh = 40.0
        )
        assertFalse(filter.matches(day(sustainedWindSpeed = 15.0)))
        assertTrue(filter.matches(day(sustainedWindSpeed = 25.0)))
        assertFalse(filter.matches(day(sustainedWindSpeed = 45.0)))
    }

    @Test
    fun matches_nullWindSpeedTreatedAsZero() {
        val filter = DaysOfInterestFilter(naturalLanguageCriteria = "", minWindSpeedKmh = 20.0)
        // null sustained wind -> 0.0 -> below min -> reject
        assertFalse(filter.matches(day(sustainedWindSpeed = null)))
    }

    @Test
    fun matches_defaultFilterAcceptsGoodDay() {
        val good = day(avgTemp = 18.0, precipitation = 0.0, sustainedWindSpeed = 25.0)
        assertTrue(DaysOfInterestFilter.DEFAULT.matches(good))
        // Too cold for DEFAULT (minTempC = 10)
        assertFalse(DaysOfInterestFilter.DEFAULT.matches(day(avgTemp = 5.0)))
        // Too little wind for DEFAULT (minWindSpeedKmh = 20)
        assertFalse(DaysOfInterestFilter.DEFAULT.matches(day(sustainedWindSpeed = 10.0)))
    }

    // ---------- filterSummary ----------

    @Test
    fun filterSummary_windRangeWithDirectionAndHours() {
        val filter = DaysOfInterestFilter(
            naturalLanguageCriteria = "raw",
            minWindSpeedKmh = 20.0,
            maxWindSpeedKmh = 35.0,
            windDirections = listOf("SW", "W"),
            sustainedWindHours = 3
        )
        val summary = filter.filterSummary()
        assertTrue(summary.contains("Wind: 20–35 km/h"), summary)
        assertTrue(summary.contains("from SW/W"), summary)
        assertTrue(summary.contains("for 3h+"), summary)
    }

    @Test
    fun filterSummary_minOnlyWind() {
        val filter = DaysOfInterestFilter(naturalLanguageCriteria = "raw", minWindSpeedKmh = 20.0)
        assertTrue(filter.filterSummary().contains("≥ 20 km/h"))
    }

    @Test
    fun filterSummary_maxOnlyWind() {
        val filter = DaysOfInterestFilter(naturalLanguageCriteria = "raw", maxWindSpeedKmh = 40.0)
        assertTrue(filter.filterSummary().contains("≤ 40 km/h"))
    }

    @Test
    fun filterSummary_tempCloudRainDaylight() {
        val filter = DaysOfInterestFilter(
            naturalLanguageCriteria = "raw",
            minTempC = 10.0,
            maxTempC = 25.0,
            maxCloudCoverPct = 30.0,
            noRain = true,
            daylightOnly = true
        )
        val summary = filter.filterSummary()
        assertTrue(summary.contains("Temp: 10–25°C"), summary)
        assertTrue(summary.contains("Cloud: ≤ 30%"), summary)
        assertTrue(summary.contains("No rain"), summary)
        assertTrue(summary.contains("Daylight hours only"), summary)
    }

    @Test
    fun filterSummary_fallsBackToNaturalLanguageWhenNoStructuredFields() {
        val filter = DaysOfInterestFilter(naturalLanguageCriteria = "just a nice day")
        assertEquals("just a nice day", filter.filterSummary())
    }

    @Test
    fun filterSummary_partsJoinedWithMiddot() {
        val filter = DaysOfInterestFilter(
            naturalLanguageCriteria = "raw",
            minWindSpeedKmh = 20.0,
            minTempC = 10.0
        )
        assertTrue(filter.filterSummary().contains(" · "))
    }

    // ---------- serialization round-trip ----------

    @Test
    fun jsonRoundTrip_preservesAllFields() {
        val filter = DaysOfInterestFilter(
            naturalLanguageCriteria = "wind 20kmh SW",
            minTempC = 10.0,
            maxTempC = 28.0,
            minWindSpeedKmh = 20.0,
            maxWindSpeedKmh = 40.0,
            windDirections = listOf("SW", "W"),
            sustainedWindHours = 2,
            daylightOnly = true,
            noRain = true,
            maxCloudCoverPct = 50.0
        )
        val json = Json.encodeToString(DaysOfInterestFilter.serializer(), filter)
        val restored = Json.decodeFromString(DaysOfInterestFilter.serializer(), json)
        assertEquals(filter, restored)
    }
}
