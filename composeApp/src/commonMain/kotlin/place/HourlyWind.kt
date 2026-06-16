package place

import core.Hour
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

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

/**
 * Computes evenly-spaced "nice" y-axis tick values from 0 up to a rounded ceiling at or above
 * [yMax], returned high-to-low so they map top-to-bottom onto the chart.
 *
 * The step is snapped to a 1/2/5 × 10ⁿ value so labels read as round numbers (e.g. yMax 34 →
 * step 10 → [40, 30, 20, 10, 0]). Pure and top-level so the y-scale is unit-testable outside the
 * Composable (MV*): the chart only renders what this returns. The largest tick is also the value
 * the plot area normalises against, so the line never clips above the top gridline.
 *
 * [tickIntervals] is the number of gaps between ticks (so the list has tickIntervals + 1 entries).
 * A non-positive or zero [yMax] yields a flat [0.0] axis.
 */
fun yAxisTicks(yMax: Double, tickIntervals: Int = 4): List<Double> {
    if (yMax <= 0.0 || tickIntervals <= 0) return listOf(0.0)

    val rawStep = yMax / tickIntervals
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val normalized = rawStep / magnitude
    val niceUnit = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    val step = niceUnit * magnitude
    // Grow the tick count if the rounded step can't reach yMax in the requested number of gaps.
    val intervals = maxOf(tickIntervals, ceil(yMax / step).toInt())

    return (intervals downTo 0).map { it * step }
}

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
