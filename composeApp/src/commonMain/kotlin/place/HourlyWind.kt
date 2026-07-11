package place

import core.Hour
import core.utils.WindSpeedUnit
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
 * Converts each point's stored km/h [HourlyWindPoint.windspeed] and [HourlyWindPoint.windgust] into
 * the user's [unit] for display, leaving the hour, label, and direction untouched. Null magnitudes
 * stay null (missing data, not zero). [WindSpeedUnit.KMH] is the identity.
 *
 * Weather is stored and thresholded in km/h; this presentation-only transform is the single place the
 * hourly chart's plotted magnitudes cross into the display unit. Pure and top-level so the conversion
 * is unit-testable outside the Composable (MV*): the chart only renders what this returns (KIM-370).
 */
fun List<HourlyWindPoint>.toDisplayUnit(unit: WindSpeedUnit): List<HourlyWindPoint> =
    map { point ->
        point.copy(
            windspeed = point.windspeed?.let(unit::fromKmh),
            windgust = point.windgust?.let(unit::fromKmh)
        )
    }

/**
 * Computes evenly-spaced "nice" y-axis tick values from 0 up to a rounded ceiling at or above
 * [yMax], returned high-to-low so they map top-to-bottom onto the chart.
 *
 * The step is snapped to a 1/2/5 × 10ⁿ value so labels read as round numbers (e.g. yMax 42 →
 * step 10 → [50, 40, 30, 20, 10, 0]). Pure and top-level so the y-scale is unit-testable outside the
 * Composable (MV*): the chart only renders what this returns. The largest tick is also the value
 * the plot area normalises against, so the line never clips above the top gridline.
 *
 * [tickIntervals] is the number of gaps between ticks (so the list has tickIntervals + 1 entries).
 * The default of 5 keeps typical wind ranges finely divided — e.g. a peak gust near 42 yields a
 * step of 10 (ticks 0..50) rather than the coarser step of 20 (ticks 0..80) that 4 intervals gives.
 * A non-positive or zero [yMax] yields a flat [0.0] axis.
 */
fun yAxisTicks(yMax: Double, tickIntervals: Int = 5): List<Double> {
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

/**
 * Converts a meteorological wind bearing (the direction the wind blows *from*) into the flow
 * bearing (the direction it blows *toward*), normalised to [0, 360).
 *
 * Stored `winddir` follows the meteorological convention — a West wind reads 270°. Wind apps
 * conventionally render the direction arrow pointing where the wind goes, so the chart rotates by
 * this flow bearing instead of the raw value. Pure and top-level so the conversion is unit-testable
 * outside the Composable.
 */
fun windFlowBearing(meteorologicalDegrees: Double): Float =
    (((meteorologicalDegrees % 360.0) + 360.0 + 180.0) % 360.0).toFloat()

/** Inclusive local-hour window shown in the day detail chart. */
internal const val WINDOW_START_HOUR = 9
internal const val WINDOW_END_HOUR = 21

/** Fixed number of x-axis slots: one per hour in the inclusive 09:00–21:00 window. */
internal const val HOURLY_WIND_SLOT_COUNT = WINDOW_END_HOUR - WINDOW_START_HOUR + 1

/**
 * Places [points] into the fixed 13-slot 09:00–21:00 frame used by the chart: index 0 == 09:00 …
 * index 12 == 21:00. A point lands in the slot matching its local hour; unfilled slots stay null so
 * the x-axis width never depends on how many hours exist, and a slot list is index-aligned with the
 * hour labels, arrows, and criteria shading.
 *
 * Pure and top-level so the framing is shared verbatim between the chart and the shading input
 * (single source of truth for slot indices) without a Compose dependency.
 */
fun hourlyWindSlots(points: List<HourlyWindPoint>): List<HourlyWindPoint?> {
    val slots = arrayOfNulls<HourlyWindPoint>(HOURLY_WIND_SLOT_COUNT)
    points.forEach { point ->
        val slot = point.hour - WINDOW_START_HOUR
        if (slot in slots.indices) slots[slot] = point
    }
    return slots.toList()
}

/**
 * Normalises a chart [value] to its vertical fraction of the plot height, matching the series and
 * gridline mapping `1 - value / yMax`: 0 maps to 1.0 (bottom edge) and [yMax] maps to 0.0 (top
 * edge). Values above [yMax] clamp to 0.0 so a threshold above the ceiling pins to the top rather
 * than drawing off-canvas; negatives clamp to 1.0. A non-positive [yMax] yields 1.0 (bottom).
 *
 * The chart multiplies this fraction by the pixel plot height to place the threshold line. Pure and
 * top-level so the placement is unit-testable outside the Composable (MV*).
 */
fun thresholdYFraction(value: Double, yMax: Double): Float {
    if (yMax <= 0.0) return 1f
    return (1f - (value / yMax).toFloat()).coerceIn(0f, 1f)
}

/**
 * Per-slot annotation tier for the hourly wind chart's criteria shading.
 *
 * [NONE] — slot has no data or fails the speed threshold.
 * [THRESHOLD] — slot meets the speed threshold but is not part of a sustained block.
 * [SUSTAINED] — slot belongs to a contiguous run that all meets the threshold (the "go out" signal).
 */
enum class ShadingTier { NONE, THRESHOLD, SUSTAINED }

/**
 * Classifies each slot in [points] against the preferred-day wind criteria, returning one
 * [ShadingTier] per input slot (parallel to [points] by index).
 *
 * A slot qualifies for the speed threshold when its [HourlyWindPoint.windspeed] is non-null and
 * `>= minSpeedKmh`. A slot is [ShadingTier.SUSTAINED] when it falls within at least one contiguous
 * run of [sustainedSlots] consecutive qualifying slots; null speeds break a run. Qualifying slots
 * not covered by any such run are [ShadingTier.THRESHOLD]; everything else is [ShadingTier.NONE].
 *
 * With [sustainedSlots] `<= 1` every qualifying slot is [ShadingTier.SUSTAINED], since a run of one
 * always satisfies the window.
 *
 * Pure and top-level so the tier logic is unit-testable outside any Composable (MV*): the chart
 * only renders what this returns.
 */
fun criteriaShading(
    points: List<HourlyWindPoint?>,
    minSpeedKmh: Double,
    sustainedSlots: Int
): List<ShadingTier> {
    val qualifies = points.map { point ->
        val speed = point?.windspeed
        speed != null && speed >= minSpeedKmh
    }

    val tiers = MutableList(points.size) { if (qualifies[it]) ShadingTier.THRESHOLD else ShadingTier.NONE }

    val window = maxOf(1, sustainedSlots)
    if (window <= points.size) {
        for (start in 0..points.size - window) {
            if ((start until start + window).all { qualifies[it] }) {
                for (i in start until start + window) tiers[i] = ShadingTier.SUSTAINED
            }
        }
    }

    return tiers
}

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
