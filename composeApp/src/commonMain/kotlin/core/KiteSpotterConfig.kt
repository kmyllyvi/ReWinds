package core

/**
 * Centralized configuration for "Day of Interest" (kiting) criteria.
 * This makes it easy to adjust the thresholds or prepare for user-defined settings.
 */
object KiteSpotterConfig {
    const val MIN_SUSTAINED_WIND_SPEED_KMH = 20.0
    const val MIN_TEMP_CELSIUS = 10.0
    const val SUSTAINED_WIND_WINDOW_HOURS = 2
}
