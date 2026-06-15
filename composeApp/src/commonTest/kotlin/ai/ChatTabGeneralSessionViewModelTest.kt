package ai

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * ViewModel-level tests for the bottom-nav Chat tab default: entering the Chat tab
 * (`requestedId == null`) must always land on the most-recent GENERAL session
 * (`placeId == null`), never on a place-tagged chat — even when a place chat is the
 * most-recently-active session overall. When no untagged session exists one is created.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatTabGeneralSessionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val createdViewModels = mutableListOf<ChatViewModel>()

    private fun viewModel(repo: ChatRepository): ChatViewModel {
        val weather = GeneralSessionFakeWeatherRepository()
        val client = AnthropicClient(apiKey = "test-key", enableLogs = false)
        val ai = AiRepository(client, WeatherTools, weather)
        return ChatViewModel(ai, weather, repo, ioDispatcher = dispatcher)
            .also { createdViewModels.add(it) }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        createdViewModels.forEach { it.viewModelScope.cancel() }
        createdViewModels.clear()
        dispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
    }

    @Test
    fun chatTabSkipsPlaceTaggedSessionEvenWhenMostRecent() = runTest(dispatcher) {
        // Place chat (id 2) is the most-recently-active session; the general chat (id 1)
        // is older. The bottom-nav Chat tab must resolve to the general one.
        val repo = GeneralSessionFakeChatRepository().apply {
            seed(id = 1L, ts = 100L, placeId = null)
            seed(id = 2L, ts = 999L, placeId = "Tarifa")
        }
        val vm = viewModel(repo)
        advanceUntilIdle()

        assertEquals(1L, vm.uiState.value.activeSessionId)
    }

    @Test
    fun chatTabCreatesGeneralSessionWhenOnlyPlaceChatsExist() = runTest(dispatcher) {
        val repo = GeneralSessionFakeChatRepository().apply {
            seed(id = 1L, ts = 100L, placeId = "Helsinki")
            seed(id = 2L, ts = 200L, placeId = "Oulu")
        }
        val vm = viewModel(repo)
        advanceUntilIdle()

        val activeId = vm.uiState.value.activeSessionId
        assertNotNull(activeId)
        // The resolved session must be a freshly created untagged one, not either place chat.
        assertEquals(null, repo.placeIdOf(activeId))
    }

    @Test
    fun chatTabPicksMostRecentGeneralAmongSeveral() = runTest(dispatcher) {
        val repo = GeneralSessionFakeChatRepository().apply {
            seed(id = 1L, ts = 100L, placeId = null)
            seed(id = 2L, ts = 300L, placeId = null) // most recent general
            seed(id = 3L, ts = 400L, placeId = "Tarifa")
            seed(id = 4L, ts = 200L, placeId = null)
        }
        val vm = viewModel(repo)
        advanceUntilIdle()

        assertEquals(2L, vm.uiState.value.activeSessionId)
    }
}

/**
 * In-memory ChatRepository that honours placeId on both seed and create, so the general-
 * vs-place-tagged resolution can be exercised without the database.
 */
private class GeneralSessionFakeChatRepository : ChatRepository {
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

    fun seed(id: Long, ts: Long, placeId: String?) {
        sessions[id] = Session(id, ChatSessionLogic.DEFAULT_TITLE, ts, mutableListOf(), placeId)
        if (id >= nextId) nextId = id + 1
        currentId = id
    }

    fun placeIdOf(id: Long?): String? = id?.let { sessions[it]?.placeId }

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
                content = listOf(AnthropicContent.Text(text = msg.content))
            )
        }

    override suspend fun clearSession(sessionId: Long) {
        sessions[sessionId]?.messages?.clear()
    }

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

/** Minimal WeatherRepository fake — getSavedPlaceNames is the only call made by VM init. */
private class GeneralSessionFakeWeatherRepository : core.WeatherRepository {
    override suspend fun getSavedPlaceNames(): List<String> = emptyList()
    override suspend fun getPlaceDayCounts(): Map<String, Long> = emptyMap()
    override suspend fun getSavedDataFor(resolvedPlace: String): core.WeatherResponse? = null
    override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): core.WeatherResponse =
        TestWeatherRepositoryFactory.createWeatherResponse(place)
    override suspend fun getPreviousDays(place: String, previousDaysCount: Int): core.WeatherResponse =
        TestWeatherRepositoryFactory.createWeatherResponse(place)
    override suspend fun deletePlace(placeName: String) {}
    override suspend fun downloadFullMonth(place: String, year: Int, month: Int): core.WeatherResponse =
        TestWeatherRepositoryFactory.createWeatherResponse(place)
    override suspend fun searchForLocations(query: String): List<core.GeoSearchResult> = emptyList()
    override suspend fun addPlaceFromSearch(place: core.GeoSearchResult): core.WeatherResponse =
        TestWeatherRepositoryFactory.createWeatherResponse(place.name)
    override suspend fun checkDataAvailability(place: String, fromDate: String, toDate: String): core.DataAvailabilityStatus =
        core.DataAvailabilityStatus.Available
    override suspend fun fetchAndPersistStations(place: String): core.StationsResult = core.StationsResult.Empty
    override suspend fun getPersistedStations(place: String): List<core.Station> = emptyList()
}
