package place

/**
 * The two sustained-wind figures a day summary needs, both derived from one pass over the same
 * 09:00–21:00 slot frame the day detail chart draws ([hourlyWindSlots]).
 *
 * Deriving them together is the point (KIM-419): the month list previously computed its "best 2h
 * avg" from all 24 stored hours with nulls squeezed out, so a windy night could flag a day whose
 * chart — which only ever shows 09:00–21:00 and only shades contiguous hours — showed nothing.
 *
 * [bestAverageKmh] is the headline "best Nh avg" figure shown on the collapsed day row.
 * [bestFloorKmh] is the highest *minimum* over the same windows, and is what decides whether the
 * day meets the wind threshold: a window's floor clears `minSpeedKmh` exactly when every hour in
 * that window does, which is the rule [criteriaShading] uses for [ShadingTier.SUSTAINED]. Comparing
 * the *average* against the threshold was the second half of the disagreement — a 25/16 km/h pair
 * averages over a 20 km/h threshold while the chart shades neither hour.
 *
 * Both are null when no window has complete data.
 */
data class SustainedWind(
    val bestAverageKmh: Double?,
    val bestFloorKmh: Double?
) {
    /**
     * Whether the day has a sustained window at or above [minSpeedKmh] — true exactly when
     * [criteriaShading] would mark at least one slot [ShadingTier.SUSTAINED] for the same input.
     */
    fun meetsThreshold(minSpeedKmh: Double): Boolean =
        bestFloorKmh != null && bestFloorKmh >= minSpeedKmh

    companion object {
        val NONE = SustainedWind(bestAverageKmh = null, bestFloorKmh = null)
    }
}

/**
 * Scans [slots] for every contiguous run of [windowSlots] hours that has a wind speed in each
 * slot, and returns the best average and best floor across those windows.
 *
 * [slots] must be the fixed 09:00–21:00 frame from [hourlyWindSlots] so the result describes the
 * same hours the day detail chart plots. A null slot (or a slot with no wind speed) is missing
 * data, not calm: it breaks the run rather than contributing a zero, matching [criteriaShading]
 * and the KIM-303 no-fabricated-data rule.
 *
 * Pure and top-level so both the month list and the place summary share one definition of
 * "sustained wind" instead of keeping a private copy each (that drift is what KIM-419 reported).
 */
fun sustainedWind(slots: List<HourlyWindPoint?>, windowSlots: Int): SustainedWind {
    val window = maxOf(1, windowSlots)
    if (slots.size < window) return SustainedWind.NONE

    val completeWindows = (0..slots.size - window).mapNotNull { start ->
        val speeds = (start until start + window).map { slots[it]?.windspeed }
        if (speeds.any { it == null }) null else speeds.filterNotNull()
    }

    return SustainedWind(
        bestAverageKmh = completeWindows.maxOfOrNull { it.average() },
        bestFloorKmh = completeWindows.maxOfOrNull { it.min() }
    )
}
