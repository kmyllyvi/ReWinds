package place

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [criteriaShading] (KIM-305).
 *
 * Only [HourlyWindPoint.windspeed] feeds the tier logic, so the points carry just a speed; the
 * label/hour/direction fields are filler. A null speed represents a slot with no data, which must
 * break a sustained run.
 */
class CriteriaShadingTest {

    private fun point(speed: Double?): HourlyWindPoint =
        HourlyWindPoint(hour = 9, label = "09", windspeed = speed, windgust = null, winddir = null)

    private fun speeds(vararg values: Double?): List<HourlyWindPoint?> = values.map { point(it) }

    @Test
    fun allBelowThreshold_areNone() {
        val points = speeds(5.0, 10.0, 8.0, 12.0)

        val tiers = criteriaShading(points, minSpeedKmh = 20.0, sustainedSlots = 2)

        assertEquals(List(4) { ShadingTier.NONE }, tiers)
    }

    @Test
    fun allAboveThresholdButFewerThanSustained_areThreshold() {
        // Two qualifying slots but a window of three never fully fills.
        val points = speeds(25.0, 30.0)

        val tiers = criteriaShading(points, minSpeedKmh = 20.0, sustainedSlots = 3)

        assertEquals(listOf(ShadingTier.THRESHOLD, ShadingTier.THRESHOLD), tiers)
    }

    @Test
    fun exactSustainedWindow_thoseSlotsSustained_othersNone() {
        // Slots 1..2 form the only qualifying run of length 2; the edges fail the threshold.
        val points = speeds(10.0, 25.0, 30.0, 5.0)

        val tiers = criteriaShading(points, minSpeedKmh = 20.0, sustainedSlots = 2)

        assertEquals(
            listOf(ShadingTier.NONE, ShadingTier.SUSTAINED, ShadingTier.SUSTAINED, ShadingTier.NONE),
            tiers
        )
    }

    @Test
    fun mixedSustainedBlockAndIsolatedThresholdHours() {
        // Slots 0..1 sustain; slot 3 qualifies but stands alone; null at slot 5 breaks any run.
        val points = speeds(25.0, 30.0, 10.0, 22.0, 5.0, null)

        val tiers = criteriaShading(points, minSpeedKmh = 20.0, sustainedSlots = 2)

        assertEquals(
            listOf(
                ShadingTier.SUSTAINED,
                ShadingTier.SUSTAINED,
                ShadingTier.NONE,
                ShadingTier.THRESHOLD,
                ShadingTier.NONE,
                ShadingTier.NONE
            ),
            tiers
        )
    }

    @Test
    fun nullSpeedBreaksAnOtherwiseContiguousRun() {
        // Slots 0..1 form a qualifying run of 2 (SUSTAINED); the null at index 2 breaks the run, so
        // slot 3 stands alone and only reaches THRESHOLD despite meeting the speed threshold.
        val points = speeds(25.0, 30.0, null, 28.0)

        val tiers = criteriaShading(points, minSpeedKmh = 20.0, sustainedSlots = 2)

        assertEquals(
            listOf(
                ShadingTier.SUSTAINED,
                ShadingTier.SUSTAINED,
                ShadingTier.NONE,
                ShadingTier.THRESHOLD
            ),
            tiers
        )
    }

    @Test
    fun nullBetweenTwoSingleQualifiersLeavesBothThreshold() {
        // Neither side reaches a run of 2: index 0 is isolated by the null, index 2 by the edge.
        val points = speeds(25.0, null, 28.0)

        val tiers = criteriaShading(points, minSpeedKmh = 20.0, sustainedSlots = 2)

        assertEquals(
            listOf(ShadingTier.THRESHOLD, ShadingTier.NONE, ShadingTier.THRESHOLD),
            tiers
        )
    }

    @Test
    fun sustainedSlotsOne_everyQualifyingSlotIsSustained() {
        val points = speeds(25.0, 10.0, 30.0)

        val tiers = criteriaShading(points, minSpeedKmh = 20.0, sustainedSlots = 1)

        assertEquals(
            listOf(ShadingTier.SUSTAINED, ShadingTier.NONE, ShadingTier.SUSTAINED),
            tiers
        )
    }

    @Test
    fun overlappingWindows_allCoveredSlotsSustained() {
        // A run of four qualifying slots with a window of 2: every slot is covered by some window.
        val points = speeds(25.0, 26.0, 27.0, 28.0)

        val tiers = criteriaShading(points, minSpeedKmh = 20.0, sustainedSlots = 2)

        assertEquals(List(4) { ShadingTier.SUSTAINED }, tiers)
    }
}
