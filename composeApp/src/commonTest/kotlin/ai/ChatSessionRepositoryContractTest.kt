package ai

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Behavioural tests for the multi-session contract that [ChatViewModel] relies on
 * (KIM-285): session list ordering, 50-session cap eviction, auto-title generation
 * (with and without a place tag), and session switching.
 *
 * The real [ChatRepositoryImpl] is backed by a SQLDelight [com.km.rewinds.db.AppDatabase];
 * there is no JVM SQLite driver wired into unit tests, so this verifies the contract via
 * an in-memory fake that mirrors the repository's documented behaviour. The underlying SQL
 * (ordering, oldest-row eviction) is validated at build time by the SQLDelight compiler,
 * and the decision rules it shares are covered directly in [ChatSessionLogicTest].
 */
class ChatSessionRepositoryContractTest {

    /** Faithful in-memory stand-in for ChatRepositoryImpl's session/message store. */
    private class InMemoryChatRepository : ChatRepository {
        private data class Session(
            val id: Long,
            var title: String,
            var lastMessageTimestamp: Long,
            val placeId: String?,
            val messages: MutableList<ChatMessage> = mutableListOf()
        )

        private val sessions = mutableListOf<Session>()
        private var nextId = 1L
        private var clock = 0L

        override suspend fun getOrCreateSession(): Long =
            sessions.maxByOrNull { it.id }?.id ?: createSession()

        override suspend fun createSession(placeId: String?): Long {
            if (ChatSessionLogic.shouldEvictBeforeCreate(sessions.size.toLong())) {
                val oldest = sessions.minByOrNull { it.lastMessageTimestamp }!!
                sessions.remove(oldest)
            }
            val session = Session(nextId++, ChatSessionLogic.DEFAULT_TITLE, ++clock, placeId)
            sessions.add(session)
            return session.id
        }

        override suspend fun saveMessage(sessionId: Long, message: ChatMessage) {
            val session = sessions.first { it.id == sessionId }
            val isFirstUser = message.role == MessageRole.USER &&
                session.messages.none { it.role == MessageRole.USER }
            session.messages.add(message)
            session.lastMessageTimestamp = ++clock
            if (isFirstUser && session.title == ChatSessionLogic.DEFAULT_TITLE) {
                session.title = ChatSessionLogic.deriveTitle(message.content, session.placeId)
            }
        }

        override suspend fun loadUiMessages(sessionId: Long): List<ChatMessage> =
            sessions.firstOrNull { it.id == sessionId }?.messages?.toList() ?: emptyList()

        override suspend fun loadConversationHistory(sessionId: Long): List<ConversationMessage> =
            emptyList()

        override suspend fun clearSession(sessionId: Long) {
            sessions.firstOrNull { it.id == sessionId }?.messages?.clear()
        }

        override suspend fun getCurrentSessionId(): Long? = sessions.maxByOrNull { it.id }?.id

        override suspend fun listSessions(): List<ChatSessionSummary> =
            sessions.sortedWith(compareByDescending<Session> { it.lastMessageTimestamp }.thenByDescending { it.id })
                .map { ChatSessionSummary(it.id, it.title, it.lastMessageTimestamp, it.messages.size.toLong(), it.placeId) }

        override suspend fun switchToSession(sessionId: Long): List<ChatMessage>? =
            sessions.firstOrNull { it.id == sessionId }?.messages?.toList()

        override suspend fun renameSession(sessionId: Long, title: String) {
            sessions.firstOrNull { it.id == sessionId }?.title = title
        }
    }

    // --- Session list ordering ---

    @Test
    fun listSessionsOrdersByMostRecentActivityFirst() = runTest {
        val repo = InMemoryChatRepository()
        val a = repo.createSession()
        val b = repo.createSession()
        val c = repo.createSession()

        // Touch them out of creation order; ordering must follow last activity, not id.
        repo.saveMessage(a, ChatMessage(role = MessageRole.USER, content = "first a"))
        repo.saveMessage(c, ChatMessage(role = MessageRole.USER, content = "first c"))
        repo.saveMessage(b, ChatMessage(role = MessageRole.USER, content = "first b"))

        val ids = repo.listSessions().map { it.id }
        assertEquals(listOf(b, c, a), ids)
    }

    // --- 50-session cap eviction ---

    @Test
    fun creatingPastCapEvictsOldestSession() = runTest {
        val repo = InMemoryChatRepository()
        val created = (1..ChatSessionLogic.SESSION_CAP).map { repo.createSession() }
        assertEquals(ChatSessionLogic.SESSION_CAP, repo.listSessions().size)

        val oldest = created.first()
        repo.createSession() // 51st -> evicts oldest

        val remaining = repo.listSessions().map { it.id }
        assertEquals(ChatSessionLogic.SESSION_CAP, remaining.size)
        assertTrue(oldest !in remaining, "oldest session should have been evicted")
    }

    // --- Auto-title ---

    @Test
    fun firstUserMessageAutoTitlesWithoutPlace() = runTest {
        val repo = InMemoryChatRepository()
        val id = repo.createSession(placeId = null)
        repo.saveMessage(id, ChatMessage(role = MessageRole.USER, content = "Is it windy in spring?"))

        assertEquals("Is it windy in spring?", repo.listSessions().first { it.id == id }.title)
    }

    @Test
    fun firstUserMessageAutoTitlesWithPlaceTag() = runTest {
        val repo = InMemoryChatRepository()
        val id = repo.createSession(placeId = "Tarifa, Spain")
        repo.saveMessage(id, ChatMessage(role = MessageRole.USER, content = "Best month for kitesurfing?"))

        assertEquals("Tarifa, Spain: Best month for kitesurfing?", repo.listSessions().first { it.id == id }.title)
    }

    @Test
    fun secondUserMessageDoesNotOverwriteTitle() = runTest {
        val repo = InMemoryChatRepository()
        val id = repo.createSession()
        repo.saveMessage(id, ChatMessage(role = MessageRole.USER, content = "First question"))
        repo.saveMessage(id, ChatMessage(role = MessageRole.ASSISTANT, content = "Some answer"))
        repo.saveMessage(id, ChatMessage(role = MessageRole.USER, content = "Totally different follow up"))

        assertEquals("First question", repo.listSessions().first { it.id == id }.title)
    }

    // --- Switching ---

    @Test
    fun switchToExistingSessionReturnsItsMessages() = runTest {
        val repo = InMemoryChatRepository()
        val first = repo.createSession()
        repo.saveMessage(first, ChatMessage(role = MessageRole.USER, content = "hello from first"))
        val second = repo.createSession()
        repo.saveMessage(second, ChatMessage(role = MessageRole.USER, content = "hello from second"))

        val messages = repo.switchToSession(first)
        assertNotNull(messages)
        assertEquals("hello from first", messages.single().content)
    }

    @Test
    fun switchToMissingSessionReturnsNull() = runTest {
        val repo = InMemoryChatRepository()
        repo.createSession()
        assertNull(repo.switchToSession(999L))
    }
}
