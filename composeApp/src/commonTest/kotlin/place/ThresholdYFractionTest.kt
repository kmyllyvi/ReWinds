package place

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [thresholdYFraction] — the pure normalisation that places the hourly wind chart's
 * threshold dashed lines (KIM-306). Fractions are multiplied by the pixel plot height by the chart,
 * so 1.0 is the bottom edge and 0.0 the top edge.
 */
class ThresholdYFractionTest {

    @Test
    fun zeroMapsToBottom() {
        assertEquals(1f, thresholdYFraction(value = 0.0, yMax = 40.0))
    }

    @Test
    fun yMaxMapsToTop() {
        assertEquals(0f, thresholdYFraction(value = 40.0, yMax = 40.0))
    }

    @Test
    fun halfMapsToMiddle() {
        assertEquals(0.5f, thresholdYFraction(value = 20.0, yMax = 40.0))
    }

    @Test
    fun aboveCeilingClampsToTop() {
        assertEquals(0f, thresholdYFraction(value = 60.0, yMax = 40.0))
    }

    @Test
    fun negativeClampsToBottom() {
        assertEquals(1f, thresholdYFraction(value = -10.0, yMax = 40.0))
    }

    @Test
    fun nonPositiveCeilingFallsToBottom() {
        assertEquals(1f, thresholdYFraction(value = 10.0, yMax = 0.0))
    }
}
