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
