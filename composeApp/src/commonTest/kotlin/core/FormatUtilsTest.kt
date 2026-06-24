package core

import core.utils.formatDecimal
import core.utils.formatGust
import core.utils.formatMonthName
import core.utils.formatTemperatureRange
import core.utils.monthName
import core.utils.shortDayLabel
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatUtilsTest {

    @Test
    fun formatMonthName_allTwelveMonths() {
        assertEquals("January 2025", formatMonthName("2025-01"))
        assertEquals("February 2025", formatMonthName("2025-02"))
        assertEquals("March 2025", formatMonthName("2025-03"))
        assertEquals("April 2025", formatMonthName("2025-04"))
        assertEquals("May 2025", formatMonthName("2025-05"))
        assertEquals("June 2025", formatMonthName("2025-06"))
        assertEquals("July 2025", formatMonthName("2025-07"))
        assertEquals("August 2025", formatMonthName("2025-08"))
        assertEquals("September 2025", formatMonthName("2025-09"))
        assertEquals("October 2025", formatMonthName("2025-10"))
        assertEquals("November 2025", formatMonthName("2025-11"))
        assertEquals("December 2025", formatMonthName("2025-12"))
    }

    @Test
    fun formatMonthName_invalidInputFallsBackToRaw() {
        // Malformed shapes and out-of-range months return the raw string unchanged.
        assertEquals("garbage", formatMonthName("garbage"))
        assertEquals("2025", formatMonthName("2025"))
        assertEquals("2025-13", formatMonthName("2025-13"))
        assertEquals("2025-00", formatMonthName("2025-00"))
        assertEquals("2025-AB", formatMonthName("2025-AB"))
        assertEquals("", formatMonthName(""))
    }

    @Test
    fun formatDecimal_roundsToOneDecimalPlace() {
        assertEquals("12.3", formatDecimal(12.34))
        assertEquals("12.4", formatDecimal(12.39))
    }

    @Test
    fun formatDecimal_wholeNumberGetsZeroDecimal() {
        assertEquals("3.0", formatDecimal(3.0))
        assertEquals("0.0", formatDecimal(0.0))
    }

    @Test
    fun formatDecimal_negativeRoundsCorrectly() {
        assertEquals("-5.7", formatDecimal(-5.67))
        assertEquals("-5.6", formatDecimal(-5.64))
    }

    @Test
    fun formatDecimal_negativeWhole() {
        assertEquals("-7.0", formatDecimal(-7.0))
    }

    @Test
    fun formatDecimal_smallPositiveFraction() {
        assertEquals("0.5", formatDecimal(0.5))
    }

    @Test
    fun formatDecimal_negativeBetweenMinusOneAndZeroRetainsSign() {
        assertEquals("-0.5", formatDecimal(-0.5))
        assertEquals("-0.3", formatDecimal(-0.34))
    }

    @Test
    fun monthName_allTwelveMonths() {
        val expected = listOf(
            1 to "January", 2 to "February", 3 to "March", 4 to "April",
            5 to "May", 6 to "June", 7 to "July", 8 to "August",
            9 to "September", 10 to "October", 11 to "November", 12 to "December"
        )
        for ((num, name) in expected) {
            assertEquals(name, monthName(num))
        }
    }

    @Test
    fun monthName_outOfRangeReturnsUnknown() {
        assertEquals("Unknown", monthName(0))
        assertEquals("Unknown", monthName(13))
        assertEquals("Unknown", monthName(-1))
    }

    @Test
    fun shortDayLabel_formatsKnownWeekdays() {
        // 2026-06-16 is a Tuesday, 2026-06-15 a Monday, 2026-06-21 a Sunday.
        assertEquals("Tue 16.", shortDayLabel("2026-06-16"))
        assertEquals("Mon 15.", shortDayLabel("2026-06-15"))
        assertEquals("Sun 21.", shortDayLabel("2026-06-21"))
    }

    @Test
    fun shortDayLabel_handlesLeapDayAndYearBoundaries() {
        // 2024-02-29 is a Thursday; 2000-01-01 is a Saturday.
        assertEquals("Thu 29.", shortDayLabel("2024-02-29"))
        assertEquals("Sat 1.", shortDayLabel("2000-01-01"))
    }

    @Test
    fun shortDayLabel_dropsLeadingZeroOnDayNumber() {
        assertEquals("Mon 1.", shortDayLabel("2026-06-01"))
    }

    @Test
    fun shortDayLabel_fallsBackToRawStringWhenUnparseable() {
        assertEquals("not-a-date", shortDayLabel("not-a-date"))
        assertEquals("2026/06/16", shortDayLabel("2026/06/16"))
        assertEquals("2026-13-40", shortDayLabel("2026-13-40"))
    }

    @Test
    fun shortDayLabel_nullReturnsPlaceholder() {
        assertEquals("--", shortDayLabel(null))
    }

    @Test
    fun formatTemperatureRange_bothValues() {
        assertEquals("12–19 °C", formatTemperatureRange(12.0, 19.0))
        assertEquals("12–20 °C", formatTemperatureRange(11.6, 19.5))
    }

    @Test
    fun formatTemperatureRange_missingValuesShowPlaceholder() {
        assertEquals("--–19 °C", formatTemperatureRange(null, 19.0))
        assertEquals("12–-- °C", formatTemperatureRange(12.0, null))
        assertEquals("--–-- °C", formatTemperatureRange(null, null))
    }

    @Test
    fun formatGust_roundsToWholeKmh() {
        assertEquals("32 km/h", formatGust(32.4))
        assertEquals("33 km/h", formatGust(32.6))
    }

    @Test
    fun formatGust_nullShowsPlaceholder() {
        assertEquals("-- km/h", formatGust(null))
    }
}
