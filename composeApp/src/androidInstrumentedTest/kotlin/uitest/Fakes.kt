package uitest

import ai.AiConversationRepository
import ai.AiMessageResult
import ai.ChatMessage
import ai.ChatRepository
import ai.ChatSessionLogic
import ai.ChatSessionSummary
import ai.ConversationMessage
import ai.MessageRole
import core.DataAvailabilityStatus
import core.Database
import core.GeoSearchResult
import core.Station
import core.StationsResult
import core.WeatherRepository
import core.WeatherResponse

/**
 * Shared in-memory test doubles for the Compose semantic UI tests (KIM-293).
 *
 * Every fake here is deterministic and free of network/database access, so the journey tests
 * pass identically in isolation or as part of the full suite (order-independent). The fakes
 * mirror the in-memory patterns already used in `commonTest` (see `ChatTabGeneralSessionViewModelTest`,
 * `ChatViewModelKoinGraphTest`), kept in `androidInstrumentedTest` because the journeys drive
 * real Compose screens through the instrumented runner.
 */

/** Empty [WeatherResponse] used wherever a fake must satisfy a return type but the value is unused. */
private fun emptyWeather(place: String) = WeatherResponse(
    resolvedAddress = place,
    address = place,
    latitude = 0.0,
    longitude = 0.0,
    timezone = "UTC",
    tzoffset = 0.0,
    days = emptyList()
)

/**
 * [WeatherRepository] fake with a scriptable search result set and an observable list of saved
 * places. [searchResults] seeds what [searchForLocations] returns; selecting a result appends its
 * name to [savedPlaceNames], which the Home screen renders as a new `PlaceRow` (journey J2).
 */
class FakeWeatherRepository(
    private val searchResults: List<GeoSearchResult> = emptyList(),
    initialPlaces: List<String> = emptyList()
) : WeatherRepository {

    val savedPlaceNames: MutableList<String> = initialPlaces.toMutableList()

    override suspend fun getSavedPlaceNames(): List<String> = savedPlaceNames.toList()

    override suspend fun getPlaceDayCounts(): Map<String, Long> =
        savedPlaceNames.associateWith { 1L }

    override suspend fun searchForLocations(query: String): List<GeoSearchResult> =
        if (query.isBlank()) emptyList() else searchResults

    override suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse {
        if (place.name !in savedPlaceNames) savedPlaceNames.add(place.name)
        return emptyWeather(place.name)
    }

    override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse? = null
    override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse =
        emptyWeather(place)
    override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse =
        emptyWeather(place)
    override suspend fun deletePlace(placeName: String) { savedPlaceNames.remove(placeName) }
    override suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse =
        emptyWeather(place)
    override suspend fun checkDataAvailability(place: String, fromDate: String, toDate: String): DataAvailabilityStatus =
        DataAvailabilityStatus.Available
    override suspend fun fetchAndPersistStations(place: String): StationsResult = StationsResult.Empty
    override suspend fun getPersistedStations(place: String): List<Station> = emptyList()
}

/** No-op [Database] fake — `HomeViewModel` only references it from a disabled startup block. */
class FakeDatabase : Database {
    override suspend fun getAllSavedPlaces(): List<String> = emptyList()
    override suspend fun getPlaceDayCounts(): Map<String, Long> = emptyMap()
    override suspend fun getSavedPlaceFull(place: String): WeatherResponse? = null
    override suspend fun saveWeatherResponse(weatherResponse: WeatherResponse) {}
    override suspend fun getWeatherDataFor(placeName: String, date: String): WeatherResponse? = null
    override suspend fun deletePlace(placeName: String) {}
    override suspend fun cleanupForecastDays() {}
    override suspend fun upsertStations(place: String, stations: List<Station>) {}
    override suspend fun getStationsForPlace(place: String): List<Station> = emptyList()
}

/**
 * [AiConversationRepository] fake returning a fixed canned reply, so the chat send/receive
 * journey (J6) is deterministic and never touches the Anthropic API.
 */
class FakeAiConversationRepository(
    private val cannedReply: String = "It will be windy in Helsinki."
) : AiConversationRepository {
    override suspend fun sendMessage(userMessage: String): AiMessageResult =
        AiMessageResult(responseText = cannedReply)
    override fun clearHistory() {}
    override fun loadHistory(messages: List<ConversationMessage>) {}
}

/**
 * In-memory [ChatRepository] holding sessions and their messages. Seeded sessions let the
 * session-switcher journey (J8) assert that switching loads the selected session's messages.
 */
class FakeChatRepository : ChatRepository {
    private data class Session(
        val id: Long,
        var title: String,
        var ts: Long,
        val messages: MutableList<ChatMessage>,
        val placeId: String?
    )

    private val sessions = mutableMapOf<Long, Session>()
    private var nextId = 1L
    private var currentId: Long? = null

    /** Seeds a session with a known id, title and message set for switcher tests. */
    fun seed(id: Long, title: String, messages: List<ChatMessage>, ts: Long = id, placeId: String? = null) {
        sessions[id] = Session(id, title, ts, messages.toMutableList(), placeId)
        if (id >= nextId) nextId = id + 1
        currentId = id
    }

    override suspend fun getOrCreateSession(): Long = currentId ?: createSession()

    override suspend fun saveMessage(sessionId: Long, message: ChatMessage) {
        sessions[sessionId]?.messages?.add(message)
    }

    override suspend fun loadUiMessages(sessionId: Long): List<ChatMessage> =
        sessions[sessionId]?.messages?.toList() ?: emptyList()

    override suspend fun loadConversationHistory(sessionId: Long): List<ConversationMessage> =
        sessions[sessionId]?.messages.orEmpty().map { msg ->
            ConversationMessage(
                role = if (msg.role == MessageRole.USER) "user" else "assistant",
                content = emptyList()
            )
        }

    override suspend fun clearSession(sessionId: Long) { sessions[sessionId]?.messages?.clear() }

    override suspend fun getCurrentSessionId(): Long? = currentId

    override suspend fun listSessions(): List<ChatSessionSummary> =
        sessions.values.sortedByDescending { it.ts }.map {
            ChatSessionSummary(it.id, it.title, it.ts, it.messages.size.toLong(), it.placeId)
        }

    override suspend fun createSession(placeId: String?): Long {
        val id = nextId++
        sessions[id] = Session(id, ChatSessionLogic.DEFAULT_TITLE, id, mutableListOf(), placeId)
        currentId = id
        return id
    }

    override suspend fun switchToSession(sessionId: Long): List<ChatMessage>? {
        val session = sessions[sessionId] ?: return null
        currentId = sessionId
        return session.messages.toList()
    }

    override suspend fun renameSession(sessionId: Long, title: String) {
        sessions[sessionId]?.title = title
    }
}
