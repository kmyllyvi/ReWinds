package core

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute

@Serializable
data object HomeRoute : NavRoute

@Serializable
data class PlaceSummaryRoute(val placeName: String) : NavRoute

@Serializable
data class MonthlyStatisticsRoute(
    val placeName: String,
    val year: Int,
    val month: Int
) : NavRoute

@Serializable
data object ChatRoute : NavRoute

@Serializable
data object SettingsRoute : NavRoute
