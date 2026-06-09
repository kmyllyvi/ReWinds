package core

import core.utils.formatDecimal
import core.utils.monthName
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatUtilsTest {

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
}
