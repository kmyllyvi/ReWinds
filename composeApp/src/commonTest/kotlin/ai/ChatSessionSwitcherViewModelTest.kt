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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * KIM-286 — ViewModel-level tests for the chat session switcher: list-state population,
 * open/close, switch, and new-chat actions. Drives the VM through a StandardTestDispatcher
 * (injected as the VM's ioDispatcher) so the coroutine-launched repository work is
 * deterministic. No Compose UI is exercised.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatSessionSwitcherViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val createdViewModels = mutableListOf<ChatViewModel>()

    private fun viewModel(repo: ChatRepository): ChatViewModel {
        val client = AnthropicClient(apiKey = "test-key", enableLogs = false)
        val weather = SwitcherFakeWeatherRepository()
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
    fun switcherStartsClosedWithNoSessions() {
        val repo = MultiSessionFakeChatRepository()
        val vm = viewModel(repo)

        // Before any coroutine runs.
        assertFalse(vm.uiState.value.isSessionSwitcherOpen)
        assertTrue(vm.uiState.value.sessions.isEmpty())
    }

    @Test
    fun openSwitcherLoadsSessionsNewestFirstAndKeepsOrder() = runTest(dispatcher) {
        val repo = MultiSessionFakeChatRepository().apply {
            seed(id = 1L, title = "Oldest", ts = 100L)
            seed(id = 2L, title = "Newest", ts = 300L)
            seed(id = 3L, title = "Middle", ts = 200L)
        }
        val vm = viewModel(repo)
        advanceUntilIdle() // init's loadActiveSession

        vm.openSessionSwitcher()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isSessionSwitcherOpen)
        // Repository returns newest-first; VM must not re-sort.
        assertEquals(listOf("Newest", "Middle", "Oldest"), state.sessions.map { it.title })
    }

    @Test
    fun openSwitcherWithNoSessionsShowsEmptyState() = runTest(dispatcher) {
        val repo = MultiSessionFakeChatRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.openSessionSwitcher()
        advanceUntilIdle()

        // init auto-creates one empty session, so simulate the genuinely-empty case by
        // wiping after init to assert the empty branch directly.
        repo.clearAllSessions()
        vm.openSessionSwitcher()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isSessionSwitcherOpen)
        assertTrue(state.sessions.isEmpty())
    }

    @Test
    fun closeSwitcherClearsOpenFlag() = runTest(dispatcher) {
        val repo = MultiSessionFakeChatRepository().apply { seed(1L, "A", 100L) }
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.openSessionSwitcher()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isSessionSwitcherOpen)

        vm.closeSessionSwitcher()
        assertFalse(vm.uiState.value.isSessionSwitcherOpen)
    }

    @Test
    fun switchToExistingSessionUpdatesActiveIdAndClosesSwitcher() = runTest(dispatcher) {
        val repo = MultiSessionFakeChatRepository().apply {
            seed(id = 1L, title = "First", ts = 100L, messages = listOf(
                ChatMessage(role = MessageRole.USER, content = "hi from 1")
            ))
            seed(id = 2L, title = "Second", ts = 200L, messages = listOf(
                ChatMessage(role = MessageRole.USER, content = "hi from 2")
            ))
        }
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.openSessionSwitcher()
        advanceUntilIdle()

        vm.switchToSession(1L)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1L, state.activeSessionId)
        assertFalse(state.isSessionSwitcherOpen)
        assertEquals(listOf("hi from 1"), state.messages.map { it.content })
    }

    @Test
    fun switchToMissingSessionIsNoOpAndLeavesSwitcherOpen() = runTest(dispatcher) {
        val repo = MultiSessionFakeChatRepository().apply { seed(1L, "Only", 100L) }
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.openSessionSwitcher()
        advanceUntilIdle()

        vm.switchToSession(999L) // does not exist
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isSessionSwitcherOpen)
        assertEquals(1L, vm.uiState.value.activeSessionId)
    }

    @Test
    fun startNewChatCreatesSessionMakesItActiveAndClosesSwitcher() = runTest(dispatcher) {
        val repo = MultiSessionFakeChatRepository().apply { seed(1L, "Existing", 100L) }
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.openSessionSwitcher()
        advanceUntilIdle()
        val before = vm.uiState.value.activeSessionId

        vm.startNewChat()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSessionSwitcherOpen)
        assertTrue(state.activeSessionId != before, "new chat should have a fresh session id")
        // Fresh chat starts with just the assistant greeting.
        assertEquals(1, state.messages.size)
        assertEquals(MessageRole.ASSISTANT, state.messages.single().role)
    }
}

/**
 * In-memory ChatRepository tracking multiple sessions with per-session messages, so the
 * switcher's list/switch/create behaviour can be exercised without the database.
 * listSessions() returns newest-activity-first to mirror the SQL ordering contract.
 */
private class MultiSessionFakeChatRepository : ChatRepository {
    private data class Session(
        val id: Long,
        var title: String,
        var ts: Long,
        val messages: MutableList<ChatMessage>
    )

    private val sessions = mutableMapOf<Long, Session>()
    private var nextId = 1L
    private var currentId: Long? = null

    fun seed(id: Long, title: String, ts: Long, messages: List<ChatMessage> = emptyList()) {
        sessions[id] = Session(id, title, ts, messages.toMutableList())
        if (id >= nextId) nextId = id + 1
        currentId = id
    }

    fun clearAllSessions() {
        sessions.clear()
        currentId = null
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
                content = listOf(AnthropicContent.Text(text = msg.content))
            )
        }

    override suspend fun clearSession(sessionId: Long) {
        sessions[sessionId]?.messages?.clear()
    }

    override suspend fun getCurrentSessionId(): Long? = currentId

    override suspend fun listSessions(): List<ChatSessionSummary> =
        sessions.values.sortedByDescending { it.ts }.map {
            ChatSessionSummary(it.id, it.title, it.ts, it.messages.size.toLong(), null)
        }

    override suspend fun createSession(placeId: String?): Long {
        val id = nextId++
        sessions[id] = Session(id, ChatSessionLogic.DEFAULT_TITLE, id, mutableListOf())
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
private class SwitcherFakeWeatherRepository : core.WeatherRepository {
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
