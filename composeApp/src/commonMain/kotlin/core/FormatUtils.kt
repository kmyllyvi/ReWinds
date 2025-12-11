package core.utils

// Helper for month name (consider a KMM-friendly date library for more robust formatting)
fun monthName(month: Int): String {
    // Basic Jvm specific, replace if KMM formatting needed
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
