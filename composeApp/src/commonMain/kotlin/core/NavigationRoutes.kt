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

/**
 * Chat tab destination. [placeId] carries the "Ask AI about this place" context so the
 * Chat tab can resolve to (or create) a session tagged with that place. [initialMessage]
 * is the deferred deep-link seed (KIM-267) — left as-is.
 */
@Serializable
data class ChatRoute(
    val initialMessage: String? = null,
    val placeId: String? = null
) : NavRoute

@Serializable
data object SettingsRoute : NavRoute
