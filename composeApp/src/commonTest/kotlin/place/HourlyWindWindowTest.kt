package place

import core.Hour
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the pure 09:00–21:00 local-time hourly filter (KIM-303).
 *
 * Epochs are chosen so that, after applying tzoffset, the local hour is obvious.
 * 1700000000 is a round Unix timestamp on a UTC hour boundary; offsetting by full
 * hours keeps the arithmetic readable.
 */
class HourlyWindWindowTest {

    // The filter only cares about (epoch + offset) / 3600 % 24, so the shared fixture's
    // midnight-UTC base keeps the arithmetic readable.
    private fun hourAt(utcHour: Int, speed: Double? = 10.0, gust: Double? = 15.0, dir: Double? = 200.0): Hour =
        testHourAtUtc(utcHour = utcHour, speed = speed, gust = gust, dir = dir)

    @Test
    fun filtersToNineToTwentyOneInclusive_atUtc() {
        val hours = (0..23).map { hourAt(it) }

        val result = hourlyWindWindow(hours, tzoffset = 0.0)

        // 09..21 inclusive = 13 hours.
        assertEquals(13, result.size)
        assertEquals("09", result.first().label)
        assertEquals("21", result.last().label)
    }

    @Test
    fun appliesPositiveTzoffset_shiftingLocalHour() {
        // A reading at 06:00 UTC becomes 09:00 local with a +3h offset → included.
        val hours = listOf(hourAt(utcHour = 6))

        val result = hourlyWindWindow(hours, tzoffset = 3.0)

        assertEquals(1, result.size)
        assertEquals("09", result.first().label)
    }

    @Test
    fun appliesNegativeTzoffset_excludingOutOfWindowHour() {
        // 09:00 UTC with a -2h offset becomes 07:00 local → excluded.
        val hours = listOf(hourAt(utcHour = 9))

        val result = hourlyWindWindow(hours, tzoffset = -2.0)

        assertTrue(result.isEmpty())
    }

    @Test
    fun nullTzoffset_treatedAsUtc() {
        val hours = listOf(hourAt(utcHour = 8), hourAt(utcHour = 9), hourAt(utcHour = 22))

        val result = hourlyWindWindow(hours, tzoffset = null)

        // Only 09:00 is inside the window when treated as UTC.
        assertEquals(1, result.size)
        assertEquals("09", result.first().label)
    }

    @Test
    fun resultIsOrderedByLocalHour() {
        val hours = listOf(hourAt(15), hourAt(9), hourAt(21), hourAt(12))

        val result = hourlyWindWindow(hours, tzoffset = 0.0)

        assertEquals(listOf("09", "12", "15", "21"), result.map { it.label })
    }

    @Test
    fun exposesLocalHour_forFixedSlotPlacement() {
        // The chart maps each point to a fixed x-axis slot by (hour - 9), so the numeric local
        // hour must be carried alongside the label.
        val hours = listOf(hourAt(9), hourAt(13), hourAt(21))

        val result = hourlyWindWindow(hours, tzoffset = 0.0)

        assertEquals(listOf(9, 13, 21), result.map { it.hour })
    }

    @Test
    fun localHourReflectsTzoffset() {
        // 06:00 UTC at +3h → local hour 9, label "09".
        val result = hourlyWindWindow(listOf(hourAt(utcHour = 6)), tzoffset = 3.0)

        assertEquals(1, result.size)
        assertEquals(9, result.first().hour)
        assertEquals("09", result.first().label)
    }

    @Test
    fun mapsSpeedGustAndDirectionThrough() {
        val hours = listOf(hourAt(10, speed = 18.5, gust = 26.2, dir = 270.0))

        val result = hourlyWindWindow(hours, tzoffset = 0.0)

        assertEquals(1, result.size)
        assertEquals(18.5, result[0].windspeed)
        assertEquals(26.2, result[0].windgust)
        assertEquals(270.0, result[0].winddir)
    }

    @Test
    fun dropsRowsWithoutEpoch_noFabrication() {
        val hours = listOf(
            hourAt(10),
            testHour(epoch = null, speed = 5.0, gust = 9.0, dir = 100.0)
        )

        val result = hourlyWindWindow(hours, tzoffset = 0.0)

        assertEquals(1, result.size)
    }

    @Test
    fun emptyOrNullInput_returnsEmpty() {
        assertTrue(hourlyWindWindow(null, 0.0).isEmpty())
        assertTrue(hourlyWindWindow(emptyList(), 0.0).isEmpty())
    }

    // --- hourlyWindSlots: fixed 13-slot 09:00–21:00 framing ---

    private fun point(hour: Int, speed: Double? = 10.0) =
        HourlyWindPoint(hour = hour, label = hour.toString().padStart(2, '0'), windspeed = speed, windgust = null, winddir = null)

    @Test
    fun slots_emptyInput_returnsThirteenNulls() {
        val slots = hourlyWindSlots(emptyList())

        assertEquals(13, slots.size)
        assertTrue(slots.all { it == null })
    }

    @Test
    fun slots_placesPointsByLocalHourOffset() {
        val slots = hourlyWindSlots(listOf(point(9), point(15), point(21)))

        assertEquals(13, slots.size)
        assertEquals(9, slots[0]?.hour)   // 09:00 → index 0
        assertEquals(15, slots[6]?.hour)  // 15:00 → index 6
        assertEquals(21, slots[12]?.hour) // 21:00 → index 12
        assertEquals(null, slots[1])      // 10:00 has no point
    }

    @Test
    fun slots_dropsHoursOutsideWindow() {
        // 08:00 and 22:00 fall outside 09:00–21:00 and must not appear in any slot.
        val slots = hourlyWindSlots(listOf(point(8), point(22)))

        assertEquals(13, slots.size)
        assertTrue(slots.all { it == null })
    }

    @Test
    fun slots_duplicateHour_lastWriteWins() {
        // Two points share hour 9; the framing overwrites by slot index, so the later one survives.
        val slots = hourlyWindSlots(listOf(point(9, speed = 5.0), point(9, speed = 99.0)))

        assertEquals(99.0, slots[0]?.windspeed)
    }

}
