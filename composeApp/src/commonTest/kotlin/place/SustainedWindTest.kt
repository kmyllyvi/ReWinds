package place

import core.DaysOfInterestFilter
import core.Hour
import core.matches
import core.utils.WindSpeedUnit
import core.utils.formatWindSpeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression tests for KIM-419: the month list's "best 2h avg" disagreed with the day detail
 * chart, which showed no "Meets threshold" window for the same date.
 *
 * Two rules keep the two views in agreement, and both are asserted here:
 *  1. the aggregation reads the same 09:00–21:00 local-time frame the chart draws, with gaps
 *     breaking a window rather than being squeezed out;
 *  2. the threshold decision compares the window *floor* against the minimum, which is the rule
 *     [criteriaShading] uses for [ShadingTier.SUSTAINED] — not the window average.
 */
class SustainedWindTest {

    private companion object {
        const val TOLERANCE = 1e-9
        const val KONSTANZ_TZ_OFFSET = 2.0 // CEST
        const val MIN_SPEED_KMH = 20.0
        const val WINDOW_HOURS = 2
    }

    // -----------------------------------------------------------------------------
    // The reported case: Konstanz, Mon 27 Jul
    // -----------------------------------------------------------------------------

    /**
     * Speeds in km/h by local hour. The 09:00–21:00 hours the chart draws stay light — one hour
     * touches 21.3 km/h (~11.5 kn) mid-afternoon, matching the screenshot — while a windy stretch
     * runs overnight, outside the chart entirely. Gusts diverge sharply from wind speed so a test
     * failure distinguishes "read the wrong hours" from "read the wrong series".
     */
    private val konstanzJul27SpeedsByLocalHour: Map<Int, Double> = mapOf(
        0 to 18.0, 1 to 20.0, 2 to 21.0, 3 to 22.0, 4 to 22.4, 5 to 20.0,
        6 to 17.0, 7 to 14.0, 8 to 12.5,
        9 to 12.0, 10 to 13.5, 11 to 14.0, 12 to 15.0, 13 to 16.2, 14 to 17.0,
        15 to 21.3, 16 to 16.5, 17 to 15.0, 18 to 14.0, 19 to 12.5, 20 to 11.0, 21 to 10.0,
        22 to 15.0, 23 to 17.0
    )

    private fun konstanzJul27Hours(): List<Hour> =
        konstanzJul27SpeedsByLocalHour.entries.sortedBy { it.key }.map { (localHour, speed) ->
            testHourAtLocal(
                localHour = localHour,
                tzoffset = KONSTANZ_TZ_OFFSET,
                speed = speed,
                gust = speed * 1.75, // gusts run well above sustained wind, peaking near 29.6 km/h
                dir = 240.0
            )
        }

    private fun konstanzJul27Slots(): List<HourlyWindPoint?> =
        hourlyWindSlots(hourlyWindWindow(konstanzJul27Hours(), KONSTANZ_TZ_OFFSET))

    @Test
    fun konstanzJul27_bestAverage_coversOnlyTheHoursTheChartDraws() {
        val hours = konstanzJul27Hours()

        // What the pre-fix aggregation produced: every stored hour, nulls squeezed out. The windy
        // 03:00–04:00 block gives 22.2 km/h, which renders as the "12 knots" from the ticket.
        val allHoursBestAverage = hours.mapNotNull { it.windspeed }
            .windowed(size = WINDOW_HOURS, step = 1) { it.average() }
            .max()
        assertEquals(22.2, allHoursBestAverage, TOLERANCE)
        assertEquals("12 knots", formatWindSpeed(allHoursBestAverage, WindSpeedUnit.KNOTS))

        // What the chart's 09:00–21:00 window actually supports: the 14:00/15:00 pair.
        val sustained = sustainedWind(konstanzJul27Slots(), WINDOW_HOURS)
        assertEquals(19.15, sustained.bestAverageKmh!!, TOLERANCE)
        assertEquals("10 knots", formatWindSpeed(sustained.bestAverageKmh, WindSpeedUnit.KNOTS))
    }

    @Test
    fun konstanzJul27_dayIsNotFlagged_andChartShadesNothing() {
        val slots = konstanzJul27Slots()
        val summary = summaryFrom(slots)

        // The chart shows no "Meets threshold" window …
        val tiers = criteriaShading(slots, MIN_SPEED_KMH, WINDOW_HOURS)
        assertFalse(tiers.any { it == ShadingTier.SUSTAINED })

        // … so the month row must not be highlighted either.
        assertFalse(DaysOfInterestFilter.DEFAULT.matches(summary))
    }

    @Test
    fun konstanzJul27_gustsNeverReachTheSustainedFigures() {
        val sustained = sustainedWind(konstanzJul27Slots(), WINDOW_HOURS)
        val peakGust = konstanzJul27Hours().mapNotNull { it.windgust }.max()

        // Guards the "gusts leaked into the average" hypothesis: the peak gust clears the
        // threshold, the sustained figures do not.
        assertTrue(peakGust > MIN_SPEED_KMH)
        assertTrue(sustained.bestAverageKmh!! < MIN_SPEED_KMH)
        assertFalse(sustained.meetsThreshold(MIN_SPEED_KMH))
    }

    // -----------------------------------------------------------------------------
    // Floor vs average — the second half of the disagreement
    // -----------------------------------------------------------------------------

    @Test
    fun windowAveragingOverThreshold_withOneHourBelow_isNotFlagged() {
        // 25 and 16 km/h average to 20.5, over a 20 km/h minimum, but the chart shades neither
        // hour because 16 fails the bar.
        val slots = slotsOf(25.0, 16.0)

        val sustained = sustainedWind(slots, WINDOW_HOURS)

        assertEquals(20.5, sustained.bestAverageKmh!!, TOLERANCE)
        assertTrue(sustained.bestAverageKmh > MIN_SPEED_KMH)
        assertEquals(16.0, sustained.bestFloorKmh!!, TOLERANCE)
        assertFalse(sustained.meetsThreshold(MIN_SPEED_KMH))
    }

    @Test
    fun everyHourAtOrAboveThreshold_isFlagged() {
        val sustained = sustainedWind(slotsOf(20.0, 24.0), WINDOW_HOURS)

        assertEquals(20.0, sustained.bestFloorKmh!!, TOLERANCE)
        assertTrue(sustained.meetsThreshold(MIN_SPEED_KMH))
    }

    @Test
    fun threshold_agreesWithChartShading_acrossFixtures() {
        // The property the ticket asks for: the month row's flag fires exactly when the chart
        // shades a sustained window, for the same hourly data.
        val fixtures = listOf(
            konstanzJul27Slots(),
            slotsOf(25.0, 16.0),                      // averages over, floor under
            slotsOf(20.0, 24.0),                      // clean sustained block
            slotsOf(19.9, 40.0),                      // one hour just under the bar
            slotsOf(30.0, null, 30.0),                // gap between two strong hours
            slotsOf(5.0, 6.0, 7.0),                   // calm
            slotsOf(22.0, 23.0, 24.0, 5.0, 26.0),     // sustained block then a lull
            emptyList()
        )

        fixtures.forEach { slots ->
            val flagged = sustainedWind(slots, WINDOW_HOURS).meetsThreshold(MIN_SPEED_KMH)
            val shaded = criteriaShading(slots, MIN_SPEED_KMH, WINDOW_HOURS)
                .any { it == ShadingTier.SUSTAINED }
            assertEquals(shaded, flagged, "disagreement for ${slots.map { it?.windspeed }}")
        }
    }

    // -----------------------------------------------------------------------------
    // Missing data
    // -----------------------------------------------------------------------------

    @Test
    fun gapBreaksTheWindow_ratherThanCollapsingIt() {
        // Two strong hours either side of a missing reading are not two sustained hours.
        val sustained = sustainedWind(slotsOf(30.0, null, 30.0), WINDOW_HOURS)

        assertNull(sustained.bestAverageKmh)
        assertNull(sustained.bestFloorKmh)
        assertFalse(sustained.meetsThreshold(MIN_SPEED_KMH))
    }

    @Test
    fun windowsWithDataAreStillUsed_whenOtherWindowsHaveGaps() {
        val sustained = sustainedWind(slotsOf(null, 24.0, 26.0, null), WINDOW_HOURS)

        assertEquals(25.0, sustained.bestAverageKmh!!, TOLERANCE)
        assertEquals(24.0, sustained.bestFloorKmh!!, TOLERANCE)
    }

    @Test
    fun fewerSlotsThanTheWindow_yieldsNoFigures() {
        assertEquals(SustainedWind.NONE, sustainedWind(slotsOf(30.0), WINDOW_HOURS))
        assertEquals(SustainedWind.NONE, sustainedWind(emptyList(), WINDOW_HOURS))
    }

    @Test
    fun nonPositiveWindow_treatedAsSingleSlot() {
        // Mirrors criteriaShading, which clamps its window to at least one slot.
        val sustained = sustainedWind(slotsOf(12.0, 30.0), windowSlots = 0)

        assertEquals(30.0, sustained.bestAverageKmh!!, TOLERANCE)
        assertEquals(30.0, sustained.bestFloorKmh!!, TOLERANCE)
    }

    @Test
    fun meetsThreshold_isFalseWithoutAFloor() {
        assertFalse(SustainedWind.NONE.meetsThreshold(0.0))
    }

    // -----------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------

    private fun slotsOf(vararg speeds: Double?): List<HourlyWindPoint?> =
        speeds.mapIndexed { index, speed ->
            speed?.let {
                HourlyWindPoint(
                    hour = WINDOW_START_HOUR + index,
                    label = (WINDOW_START_HOUR + index).toString().padStart(2, '0'),
                    windspeed = it,
                    windgust = null,
                    winddir = null
                )
            }
        }

    /** Mirrors how both ViewModels populate the wind fields of a day row. */
    private fun summaryFrom(slots: List<HourlyWindPoint?>): DayWeatherSummary {
        val sustained = sustainedWind(slots, WINDOW_HOURS)
        return DayWeatherSummary(
            date = "2026-07-27",
            description = null,
            maxTemp = null,
            minTemp = null,
            avgTemp = 20.0, // clears the DEFAULT filter's temperature criterion
            avgWindSpeed = null,
            maxWindSpeed = null,
            sustainedWindSpeed = sustained.bestAverageKmh,
            solarenergy = null,
            isFoggy = false,
            foggyHours = 0,
            sustainedWindFloor = sustained.bestFloorKmh
        )
    }
}
