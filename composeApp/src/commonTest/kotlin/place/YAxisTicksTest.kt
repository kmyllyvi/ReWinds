package place

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the pure y-axis tick computation behind the hourly wind chart's scale (KIM-303,
 * KIM-370).
 *
 * The chart only renders what [yAxisTicks] returns, so the rounding and ordering rules are pinned
 * here rather than inside a Composable. The default interval count is 5 (KIM-370) so typical wind
 * ranges divide finely (step 10 instead of 20).
 */
class YAxisTicksTest {

    @Test
    fun roundsCeilingUpToNiceStep_andOrdersHighToLow() {
        // 34 / 5 = 6.8 → snapped up to a step of 10 → ceiling 50, six ticks high-to-low.
        assertEquals(listOf(50.0, 40.0, 30.0, 20.0, 10.0, 0.0), yAxisTicks(34.0))
    }

    @Test
    fun topTickIsAtOrAboveData() {
        listOf(1.0, 7.3, 12.0, 34.0, 88.5, 150.0).forEach { max ->
            val ticks = yAxisTicks(max)
            assertTrue(ticks.first() >= max, "top tick ${ticks.first()} should cover $max")
            assertEquals(0.0, ticks.last(), "axis should reach zero for $max")
        }
    }

    @Test
    fun ticksAreEvenlySpacedAndDescending() {
        val ticks = yAxisTicks(34.0)
        val step = ticks[0] - ticks[1]
        for (i in 1 until ticks.size) {
            assertEquals(step, ticks[i - 1] - ticks[i], 1e-9, "uneven gap at index $i")
            assertTrue(ticks[i] < ticks[i - 1], "ticks must descend")
        }
    }

    @Test
    fun smallRangeUsesSmallNiceStep() {
        // 3 / 5 = 0.6 → step snapped to 1 → ceiling 5.
        assertEquals(listOf(5.0, 4.0, 3.0, 2.0, 1.0, 0.0), yAxisTicks(3.0))
    }

    @Test
    fun exactMultipleKeepsFiveIntervals() {
        // 50 / 5 = 10 exactly → step 10, ceiling 50, no extra intervals grown.
        assertEquals(listOf(50.0, 40.0, 30.0, 20.0, 10.0, 0.0), yAxisTicks(50.0))
    }

    /**
     * KIM-370: a knots-preference user with a ~42 km/h peak gust (≈ 22.7 knots) should get a tight
     * 0..25 axis in step 5 — not the km/h 0..80 range — so the plotted data fills the chart.
     */
    @Test
    fun knotsRangePeakGustStaysUnderThirty() {
        val ticks = yAxisTicks(22.7)
        assertEquals(listOf(25.0, 20.0, 15.0, 10.0, 5.0, 0.0), ticks)
        assertTrue(ticks.first() <= 30.0, "knots ceiling ${ticks.first()} should stay in the 0..30 band")
    }

    /**
     * KIM-370: the same day plotted in km/h (peak gust ~42) yields the finer step-10 ceiling of 50,
     * i.e. ticks 0/10/20/30/40/50 rather than the coarse 0/20/40/60/80 that four intervals produced.
     */
    @Test
    fun kmhPeakGustUsesStepOfTen() {
        assertEquals(listOf(50.0, 40.0, 30.0, 20.0, 10.0, 0.0), yAxisTicks(42.0))
    }

    @Test
    fun highWindStaysReadable() {
        // 80 / 5 = 16 → step 20 → ceiling 100; still six round labels, none cramped.
        val ticks = yAxisTicks(80.0)
        assertEquals(listOf(100.0, 80.0, 60.0, 40.0, 20.0, 0.0), ticks)
        assertTrue(ticks.size <= 7, "too many labels would crowd the axis at the high end")
    }

    @Test
    fun zeroOrNegativeMaxYieldsFlatAxis() {
        assertEquals(listOf(0.0), yAxisTicks(0.0))
        assertEquals(listOf(0.0), yAxisTicks(-5.0))
    }

    @Test
    fun nonPositiveIntervalsYieldsFlatAxis() {
        assertEquals(listOf(0.0), yAxisTicks(34.0, tickIntervals = 0))
    }
}
