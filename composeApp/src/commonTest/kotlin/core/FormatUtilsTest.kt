package core

import core.utils.formatDecimal
import core.utils.monthName
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for core/FormatUtils.kt — the KMP-safe number/month formatting helpers
 * (no String.format, must behave identically on iOS and Android).
 */
class FormatUtilsTest {

    @Test
    fun formatDecimal_positiveTruncatesToOneDecimal() {
        assertEquals("12.3", formatDecimal(12.34))
        assertEquals("12.3", formatDecimal(12.39)) // truncation, not rounding
    }

    @Test
    fun formatDecimal_wholeNumberGetsZeroDecimal() {
        assertEquals("3.0", formatDecimal(3.0))
        assertEquals("0.0", formatDecimal(0.0))
    }

    @Test
    fun formatDecimal_negativeTruncatesTowardZero() {
        // (-5.67 * 10).toLong() == -56  ->  intPart = -5, decPart = abs(-6) = 6
        // This documents the ACTUAL behavior (truncation), which differs from the
        // KDoc example that claims "-5.7". The implementation truncates, not rounds.
        assertEquals("-5.6", formatDecimal(-5.67))
    }

    @Test
    fun formatDecimal_negativeWhole() {
        assertEquals("-7.0", formatDecimal(-7.0))
    }

    @Test
    fun formatDecimal_smallFraction() {
        assertEquals("0.5", formatDecimal(0.5))
    }

    @Test
    fun formatDecimal_negativeBetweenMinusOneAndZeroLosesSign_knownBug() {
        // KNOWN BUG (pinned, not endorsed): for values in (-1.0, 0.0) the integer part is 0,
        // and "$intPart" renders as "0" with no minus sign, so the result is positive-looking.
        // (-0.5 * 10).toLong() == -5  ->  intPart = -5/10 = 0,  decPart = abs(-5 % 10) = 5  ->  "0.5"
        // A correct implementation would yield "-0.5". This test documents current behavior so a
        // future fix will surface here intentionally.
        assertEquals("0.5", formatDecimal(-0.5))
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
}
