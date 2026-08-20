package place

import core.Hour

/**
 * Shared [Hour] builders for the hourly-wind tests.
 *
 * [Hour] has 24 fields, almost all irrelevant to wind logic, so every test that needs one would
 * otherwise carry its own 25-line constructor call.
 */

/** Midnight UTC of an arbitrary fixed day, so `TEST_DAY_BASE_UTC + h * 3600` reads as "h:00 UTC". */
internal const val TEST_DAY_BASE_UTC: Long = 1700000000L - (1700000000L % 86400L)

internal fun testHour(
    epoch: Long?,
    speed: Double? = null,
    gust: Double? = null,
    dir: Double? = null
): Hour = Hour(
    datetime = "",
    datetimeEpoch = epoch,
    temp = null,
    feelslike = null,
    humidity = null,
    dew = null,
    precip = null,
    precipprob = null,
    snow = null,
    snowdepth = null,
    preciptype = null,
    windgust = gust,
    windspeed = speed,
    winddir = dir,
    pressure = null,
    visibility = null,
    cloudcover = null,
    solarradiation = null,
    solarenergy = null,
    uvindex = null,
    conditions = null,
    icon = null,
    source = null,
    stations = null
)

/** An hour whose UTC clock hour is [utcHour] on the fixed test day. */
internal fun testHourAtUtc(
    utcHour: Int,
    speed: Double? = 10.0,
    gust: Double? = 15.0,
    dir: Double? = 200.0
): Hour = testHour(
    epoch = TEST_DAY_BASE_UTC + utcHour * 3600L,
    speed = speed,
    gust = gust,
    dir = dir
)

/**
 * An hour that reads as [localHour] once [tzoffset] is applied, so a fixture can be written in the
 * location's local time — the frame both the day chart and the month aggregation work in.
 */
internal fun testHourAtLocal(
    localHour: Int,
    tzoffset: Double,
    speed: Double? = null,
    gust: Double? = null,
    dir: Double? = null
): Hour = testHour(
    epoch = TEST_DAY_BASE_UTC + ((localHour - tzoffset) * 3600).toLong(),
    speed = speed,
    gust = gust,
    dir = dir
)
