package place

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the pure y-axis tick computation behind the hourly wind chart's scale (KIM-303).
 *
 * The chart only renders what [yAxisTicks] returns, so the rounding and ordering rules are pinned
 * here rather than inside a Composable.
 */
class YAxisTicksTest {

    @Test
    fun roundsCeilingUpToNiceStep_andOrdersHighToLow() {
        // 34 / 4 = 8.5 → snapped up to a step of 10 → ceiling 40.
        assertEquals(listOf(40.0, 30.0, 20.0, 10.0, 0.0), yAxisTicks(34.0))
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
        // 3 / 4 = 0.75 → step snapped to 1 → ceiling 4.
        assertEquals(listOf(4.0, 3.0, 2.0, 1.0, 0.0), yAxisTicks(3.0))
    }

    @Test
    fun exactMultipleKeepsFourIntervals() {
        // 40 / 4 = 10 exactly → step 10, ceiling 40, no extra intervals grown.
        assertEquals(listOf(40.0, 30.0, 20.0, 10.0, 0.0), yAxisTicks(40.0))
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
