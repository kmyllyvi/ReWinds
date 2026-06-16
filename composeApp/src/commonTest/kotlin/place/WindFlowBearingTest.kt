package place

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [windFlowBearing] (KIM-303 wind direction fix).
 *
 * Stored `winddir` is meteorological — the direction the wind blows *from*. The chart arrow must
 * point where the wind blows *toward*, which is the source bearing rotated by 180°. These cases
 * pin the cardinal flips a wind app reader expects: a West wind (270° from) flows East (90°).
 */
class WindFlowBearingTest {

    @Test
    fun westWindFlowsEast() {
        assertEquals(90f, windFlowBearing(270.0))
    }

    @Test
    fun northWindFlowsSouth() {
        assertEquals(180f, windFlowBearing(0.0))
    }

    @Test
    fun southWindFlowsNorth() {
        // 180 + 180 = 360, normalised back to 0 (North).
        assertEquals(0f, windFlowBearing(180.0))
    }

    @Test
    fun southwestWindFlowsNortheast() {
        // 225° (SW, from) → 45° (NE, toward).
        assertEquals(45f, windFlowBearing(225.0))
    }

    @Test
    fun resultStaysInZeroTo360() {
        // 200° (typical Konstanz SW reading) → 20°.
        assertEquals(20f, windFlowBearing(200.0))
    }

    @Test
    fun handlesValuesAtOrBeyond360() {
        assertEquals(180f, windFlowBearing(360.0))
        assertEquals(90f, windFlowBearing(630.0))
    }

    @Test
    fun handlesNegativeBearing() {
        // Defensive: a -90° reading normalises to 270° (West, from) → 90° (East, toward).
        assertEquals(90f, windFlowBearing(-90.0))
    }
}
