package ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for chat persistence logic introduced in KIM-119.
 * Covers ChatMessage data structures, FakeChatRepository contract,
 * and AiRepository.loadHistory().
 */
class ChatPersistenceTest {

    // region ChatMessage

    @Test
    fun testChatMessageDefaultTimestamp() {
        val message = ChatMessage(role = MessageRole.USER, content = "Hello")
        assertEquals(0L, message.timestamp)
    }

    @Test
    fun testChatMessageRoles() {
        val user = ChatMessage(role = MessageRole.USER, content = "Hi")
        val assistant = ChatMessage(role = MessageRole.ASSISTANT, content = "Hello!")
        assertEquals(MessageRole.USER, user.role)
        assertEquals(MessageRole.ASSISTANT, assistant.role)
    }

    @Test
    fun testChatMessageIdUniqueness() {
        val a = ChatMessage(role = MessageRole.USER, content = "msg1")
        val b = ChatMessage(role = MessageRole.USER, content = "msg2")
        assertTrue(a.id != b.id, "Each ChatMessage should have a unique id")
    }

    @Test
    fun testChatMessageEquality() {
        val id = "fixed-id"
        val a = ChatMessage(id = id, role = MessageRole.ASSISTANT, content = "same", timestamp = 100L)
        val b = ChatMessage(id = id, role = MessageRole.ASSISTANT, content = "same", timestamp = 100L)
        assertEquals(a, b)
    }

    // endregion

    // region FakeChatRepository

    private fun makeFakeRepo() = FakeChatRepository()

    @Test
    fun testFakeRepoCreatesSession() {
        val repo = makeFakeRepo()
        val sessionId = runBlockingTest { repo.getOrCreateSession() }
        assertEquals(1L, sessionId)
    }

    @Test
    fun testFakeRepoReturnsSameSession() {
        val repo = makeFakeRepo()
        val first = runBlockingTest { repo.getOrCreateSession() }
        val second = runBlockingTest { repo.getOrCreateSession() }
        assertEquals(first, second)
    }

    @Test
    fun testFakeRepoSaveAndLoadMessages() {
        val repo = makeFakeRepo()
        val sessionId = runBlockingTest { repo.getOrCreateSession() }

        val msg1 = ChatMessage(role = MessageRole.USER, content = "What's the wind like?")
        val msg2 = ChatMessage(role = MessageRole.ASSISTANT, content = "It's blowing 20 knots.")

        runBlockingTest {
            repo.saveMessage(sessionId, msg1)
            repo.saveMessage(sessionId, msg2)
        }

        val loaded = runBlockingTest { repo.loadUiMessages(sessionId) }
        assertEquals(2, loaded.size)
        assertEquals("What's the wind like?", loaded[0].content)
        assertEquals(MessageRole.USER, loaded[0].role)
        assertEquals("It's blowing 20 knots.", loaded[1].content)
        assertEquals(MessageRole.ASSISTANT, loaded[1].role)
    }

    @Test
    fun testFakeRepoClearSession() {
        val repo = makeFakeRepo()
        val sessionId = runBlockingTest { repo.getOrCreateSession() }

        runBlockingTest {
            repo.saveMessage(sessionId, ChatMessage(role = MessageRole.USER, content = "Hello"))
            repo.saveMessage(sessionId, ChatMessage(role = MessageRole.ASSISTANT, content = "Hi"))
        }

        runBlockingTest { repo.clearSession(sessionId) }

        val loaded = runBlockingTest { repo.loadUiMessages(sessionId) }
        assertTrue(loaded.isEmpty(), "Messages should be empty after clearSession")
    }

    @Test
    fun testFakeRepoLoadConversationHistory() {
        val repo = makeFakeRepo()
        val sessionId = runBlockingTest { repo.getOrCreateSession() }

        runBlockingTest {
            repo.saveMessage(sessionId, ChatMessage(role = MessageRole.USER, content = "User msg"))
            repo.saveMessage(sessionId, ChatMessage(role = MessageRole.ASSISTANT, content = "Assistant reply"))
        }

        val history = runBlockingTest { repo.loadConversationHistory(sessionId) }
        assertEquals(2, history.size)
        assertEquals("user", history[0].role)
        assertEquals("assistant", history[1].role)
        assertEquals(1, history[0].content.size)
        assertTrue(history[0].content[0] is AnthropicContent.Text)
        assertEquals("User msg", (history[0].content[0] as AnthropicContent.Text).text)
    }

    @Test
    fun testFakeRepoGetCurrentSessionId() {
        val repo = makeFakeRepo()
        assertNull(runBlockingTest { repo.getCurrentSessionId() })
        runBlockingTest { repo.getOrCreateSession() }
        assertEquals(1L, runBlockingTest { repo.getCurrentSessionId() })
    }

    // endregion

    // region AiRepository.loadHistory

    @Test
    fun testLoadHistoryReplacesExistingHistory() {
        val client = AnthropicClient(apiKey = "test-key", enableLogs = false)
        val repo = AiRepository(client, WeatherTools, FakeWeatherRepository())

        val initial = listOf(
            ConversationMessage("user", listOf(AnthropicContent.Text(text = "old message")))
        )
        repo.loadHistory(initial)
        assertEquals(1, repo.getHistory().size)

        val replacement = listOf(
            ConversationMessage("user", listOf(AnthropicContent.Text(text = "new msg 1"))),
            ConversationMessage("assistant", listOf(AnthropicContent.Text(text = "new reply")))
        )
        repo.loadHistory(replacement)

        val history = repo.getHistory()
        assertEquals(2, history.size)
        assertEquals("user", history[0].role)
        assertEquals("assistant", history[1].role)
        assertEquals("new msg 1", (history[0].content[0] as AnthropicContent.Text).text)
    }

    @Test
    fun testLoadHistoryWithEmptyListClearsHistory() {
        val client = AnthropicClient(apiKey = "test-key", enableLogs = false)
        val repo = AiRepository(client, WeatherTools, FakeWeatherRepository())

        repo.loadHistory(listOf(
            ConversationMessage("user", listOf(AnthropicContent.Text(text = "something")))
        ))
        assertEquals(1, repo.getHistory().size)

        repo.loadHistory(emptyList())
        assertTrue(repo.getHistory().isEmpty())
    }

    @Test
    fun testLoadHistoryPreservesMessageContent() {
        val client = AnthropicClient(apiKey = "test-key", enableLogs = false)
        val repo = AiRepository(client, WeatherTools, FakeWeatherRepository())

        val messages = listOf(
            ConversationMessage("user", listOf(AnthropicContent.Text(text = "What was the best day in Tarifa?"))),
            ConversationMessage("assistant", listOf(AnthropicContent.Text(text = "July 14th had 25 knots."))),
            ConversationMessage("user", listOf(AnthropicContent.Text(text = "Thanks!")))
        )
        repo.loadHistory(messages)

        val loaded = repo.getHistory()
        assertEquals(3, loaded.size)
        assertEquals("What was the best day in Tarifa?", (loaded[0].content[0] as AnthropicContent.Text).text)
        assertEquals("July 14th had 25 knots.", (loaded[1].content[0] as AnthropicContent.Text).text)
        assertEquals("Thanks!", (loaded[2].content[0] as AnthropicContent.Text).text)
    }

    // endregion
}

/**
 * In-memory ChatRepository implementation for tests.
 */
class FakeChatRepository : ChatRepository {
    private var sessionId: Long? = null
    private val messages = mutableListOf<ChatMessage>()

    override suspend fun getOrCreateSession(): Long {
        if (sessionId == null) sessionId = 1L
        return sessionId!!
    }

    override suspend fun saveMessage(sessionId: Long, message: ChatMessage) {
        messages.add(message)
    }

    override suspend fun loadUiMessages(sessionId: Long): List<ChatMessage> = messages.toList()

    override suspend fun loadConversationHistory(sessionId: Long): List<ConversationMessage> {
        return messages.map { msg ->
            ConversationMessage(
                role = if (msg.role == MessageRole.USER) "user" else "assistant",
                content = listOf(AnthropicContent.Text(text = msg.content))
            )
        }
    }

    override suspend fun clearSession(sessionId: Long) {
        messages.clear()
    }

    override suspend fun getCurrentSessionId(): Long? = sessionId
}

/**
 * Minimal WeatherRepository fake for AiRepository tests.
 */
private class FakeWeatherRepository : core.WeatherRepository {
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

/**
 * Minimal helper to run suspend functions synchronously in common tests.
 * Uses kotlinx.coroutines.test or a simple runBlocking equivalent.
 */
private fun <T> runBlockingTest(block: suspend () -> T): T {
    var result: T? = null
    var error: Throwable? = null
    kotlinx.coroutines.runBlocking {
        try {
            result = block()
        } catch (e: Throwable) {
            error = e
        }
    }
    if (error != null) throw error!!
    @Suppress("UNCHECKED_CAST")
    return result as T
}
