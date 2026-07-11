package place

import core.utils.WindSpeedUnit
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [toDisplayUnit] and its interplay with [yAxisTicks] — the pure pipeline the hourly
 * wind chart uses to render stored km/h speeds in the user's chosen unit (KIM-370).
 *
 * Regression guard for the bug where the chart printed a "knots" axis label but plotted raw km/h
 * magnitudes: conversion must happen before the scale math.
 */
class HourlyWindDisplayUnitTest {

    private fun point(hour: Int, speed: Double?, gust: Double?) =
        HourlyWindPoint(
            hour = hour,
            label = hour.toString().padStart(2, '0'),
            windspeed = speed,
            windgust = gust,
            winddir = 270.0
        )

    @Test
    fun kmhUnitIsIdentity() {
        val points = listOf(point(9, 20.0, 42.0), point(10, 15.0, 30.0))
        assertEquals(points, points.toDisplayUnit(WindSpeedUnit.KMH))
    }

    @Test
    fun knotsConvertsBothSeries() {
        val converted = listOf(point(9, 20.0, 42.0)).toDisplayUnit(WindSpeedUnit.KNOTS)
        // 42 km/h ÷ 1.852 ≈ 22.7 knots; 20 km/h ≈ 10.8 knots.
        assertEquals(23L, converted.single().windgust!!.roundToLong())
        assertEquals(11L, converted.single().windspeed!!.roundToLong())
    }

    @Test
    fun nullMagnitudesStayNull() {
        val converted = listOf(point(9, null, null)).toDisplayUnit(WindSpeedUnit.KNOTS)
        assertNull(converted.single().windspeed)
        assertNull(converted.single().windgust)
    }

    @Test
    fun directionHourAndLabelUntouched() {
        val original = point(14, 20.0, 42.0)
        val converted = listOf(original).toDisplayUnit(WindSpeedUnit.MPH).single()
        assertEquals(original.hour, converted.hour)
        assertEquals(original.label, converted.label)
        assertEquals(original.winddir, converted.winddir)
    }

    /**
     * The AC scenario: a knots-preference user viewing a day whose peak gust is ~42 km/h sees the
     * gust rendered near 23 and the axis capped in the 0..30 band — never the raw km/h 0..80.
     */
    @Test
    fun knotsUserSeesConvertedGustAndTightAxis() {
        val raw = listOf(point(9, 30.0, 42.0), point(10, 25.0, 38.0))
        val display = raw.toDisplayUnit(WindSpeedUnit.KNOTS)

        val maxGust = display.mapNotNull { it.windgust }.max()
        assertEquals(23L, maxGust.roundToLong(), "42 km/h gust should read ~23 knots")

        val ticks = yAxisTicks(maxGust)
        assertEquals(listOf(25.0, 20.0, 15.0, 10.0, 5.0, 0.0), ticks)
    }
}
