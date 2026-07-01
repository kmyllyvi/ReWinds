package place

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the collapsed day-row wind semantics (KIM-329): the row must surface the day's
 * average top wind (sustained), never the momentary gust.
 */
class DayWeatherSummaryTest {

    private fun summary(sustained: Double?, gust: Double?): DayWeatherSummary =
        DayWeatherSummary(
            date = "2025-01-01",
            description = null,
            maxTemp = null,
            minTemp = null,
            avgTemp = null,
            avgWindSpeed = null,
            maxWindSpeed = gust, // gust
            sustainedWindSpeed = sustained,
            solarenergy = null,
            isFoggy = false,
            foggyHours = 0
        )

    @Test
    fun collapsedRowWindSpeed_usesSustainedNotGust() {
        val day = summary(sustained = 22.0, gust = 48.0)
        assertEquals(22.0, day.collapsedRowWindSpeed)
    }

    @Test
    fun collapsedRowWindSpeed_isNullWhenNoSustainedReading() {
        // Even with a gust present, no sustained reading means no value on the row.
        val day = summary(sustained = null, gust = 48.0)
        assertNull(day.collapsedRowWindSpeed)
    }
}
