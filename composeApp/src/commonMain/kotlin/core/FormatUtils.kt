package core.utils

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Formats a Double to one decimal place in a KMP-compatible way (no String.format).
 * Example: 12.34 → "12.3", -5.67 → "-5.7", -0.5 → "-0.5", 3.0 → "3.0"
 */
fun formatDecimal(value: Double): String {
    val shifted = (value * 10).roundToLong()
    val intPart = shifted / 10
    val decPart = abs(shifted % 10)
    // intPart is 0 for values in (-1, 0) — the minus sign must be added explicitly
    val sign = if (value < 0.0 && intPart == 0L) "-" else ""
    return "$sign$intPart.$decPart"
}

private val WEEKDAY_ABBREVIATIONS =
    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/**
 * Formats a "YYYY-MM-DD" date string into a short "Mon 13." label
 * (three-letter weekday abbreviation, day-of-month, trailing period).
 *
 * KMP-safe: parses via string splitting and derives the weekday with a
 * Sakamoto-style integer calculation — no JVM date classes.
 *
 * Falls back to the raw [date] string when it is unparseable, or "--" when null.
 */
fun shortDayLabel(date: String?): String {
    if (date == null) return "--"
    val parts = date.split("-")
    if (parts.size != 3) return date
    val year = parts[0].toIntOrNull()
    val month = parts[1].toIntOrNull()
    val day = parts[2].toIntOrNull()
    if (year == null || month == null || day == null) return date
    if (month !in 1..12 || day !in 1..31) return date

    val weekday = WEEKDAY_ABBREVIATIONS[mondayBasedWeekday(year, month, day)]
    return "$weekday $day."
}

/**
 * Returns the day of week as 0=Monday .. 6=Sunday using Sakamoto's algorithm.
 */
private fun mondayBasedWeekday(year: Int, month: Int, day: Int): Int {
    val monthOffsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val y = if (month < 3) year - 1 else year
    // Sakamoto yields 0=Sunday .. 6=Saturday; shift to 0=Monday .. 6=Sunday.
    val sundayBased =
        (y + y / 4 - y / 100 + y / 400 + monthOffsets[month - 1] + day) % 7
    return (sundayBased + 6) % 7
}

/**
 * Formats a temperature range as "min–max °C", rendering each missing value as "--".
 * Example: 12.0, 19.0 → "12–19 °C"; null, 19.0 → "--–19 °C".
 */
fun formatTemperatureRange(minTemp: Double?, maxTemp: Double?): String {
    val min = minTemp?.let { formatWhole(it) } ?: "--"
    val max = maxTemp?.let { formatWhole(it) } ?: "--"
    return "$min–$max °C"
}

/**
 * Formats a wind speed as a whole number followed by "km/h".
 * Example: 32.4 → "32 km/h"; null → "-- km/h".
 */
fun formatWindSpeed(speed: Double?): String {
    val value = speed?.let { formatWhole(it) } ?: "--"
    return "$value km/h"
}

private fun formatWhole(value: Double): String = value.roundToLong().toString()

/**
 * Formats a "YYYY-MM" string into a full month-name label, e.g. "2025-10" → "October 2025".
 *
 * KMP-safe: parses by splitting and maps the month via [monthName] — no String.format,
 * no java.time. Used for all user-facing copy in the chat (KIM-321); internal grouping
 * stays "YYYY-MM". Falls back to the raw [yyyyMM] string when it is unparseable.
 */
fun formatMonthName(yyyyMM: String): String {
    val parts = yyyyMM.split("-")
    if (parts.size != 2) return yyyyMM
    val year = parts[0].toIntOrNull()
    val month = parts[1].toIntOrNull()
    if (year == null || month == null || month !in 1..12) return yyyyMM
    return "${monthName(month)} $year"
}

// Helper for month name (consider a KMM-friendly date library for more robust formatting)
fun monthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Unknown"
    }
}
