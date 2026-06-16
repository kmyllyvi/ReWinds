package place

import core.Hour

/**
 * A single hour's wind reading prepared for the hourly chart.
 *
 * Decoupled from the DB [Hour] row so the chart Composable depends only on what it draws:
 * a local-time label, the two speed series, and the bearing used to rotate the direction arrow.
 */
data class HourlyWindPoint(
    val hour: Int,          // local hour 9..21, used to place the point in its fixed x-axis slot
    val label: String,      // local-time "HH:00", e.g. "09"
    val windspeed: Double?,
    val windgust: Double?,
    val winddir: Double?
)

/** Inclusive local-hour window shown in the day detail chart. */
internal const val WINDOW_START_HOUR = 9
internal const val WINDOW_END_HOUR = 21

/** Fixed number of x-axis slots: one per hour in the inclusive 09:00–21:00 window. */
internal const val HOURLY_WIND_SLOT_COUNT = WINDOW_END_HOUR - WINDOW_START_HOUR + 1

/**
 * Filters [hours] down to the 09:00–21:00 window in the location's local time and maps the
 * survivors to [HourlyWindPoint]s ordered by local hour.
 *
 * Local hour is derived from the UTC [Hour.datetimeEpoch] plus [tzoffset] (hours east of UTC),
 * which needs no timezone database. A null [tzoffset] is treated as UTC. Rows without an epoch
 * are dropped — there is no fabricated data and no interpolation (KIM-303 partial-data rule).
 *
 * Kept as a pure top-level function so the window logic is unit-testable outside any Composable
 * and outside the ViewModel's DB dependencies (MV*).
 */
fun hourlyWindWindow(hours: List<Hour>?, tzoffset: Double?): List<HourlyWindPoint> {
    if (hours.isNullOrEmpty()) return emptyList()
    val offsetSeconds = ((tzoffset ?: 0.0) * 3600).toLong()

    return hours
        .mapNotNull { hour ->
            val epoch = hour.datetimeEpoch ?: return@mapNotNull null
            val localHour = (((epoch + offsetSeconds) / 3600) % 24).toInt()
            if (localHour < WINDOW_START_HOUR || localHour > WINDOW_END_HOUR) return@mapNotNull null
            localHour to hour
        }
        .sortedBy { it.first }
        .map { (localHour, hour) ->
            HourlyWindPoint(
                hour = localHour,
                label = localHour.toString().padStart(2, '0'),
                windspeed = hour.windspeed,
                windgust = hour.windgust,
                winddir = hour.winddir
            )
        }
}
