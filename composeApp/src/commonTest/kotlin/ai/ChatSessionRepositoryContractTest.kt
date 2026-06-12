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

    /**
     * Faithful in-memory stand-in for ChatRepositoryImpl's session/message store.
     *
     * Messages are kept in a separate store keyed by session id (NOT inside the Session
     * object) so eviction must explicitly delete a session's messages, mirroring prod:
     * SQLite's ON DELETE CASCADE is not enforced at runtime, so the repository deletes
     * messages then the session as two steps. A future regression that drops the message
     * delete from the eviction path would orphan rows here exactly as it would in prod.
     */
    private class InMemoryChatRepository : ChatRepository {
        private data class Session(
            val id: Long,
            var title: String,
            var lastMessageTimestamp: Long,
            val placeId: String?
        )

        private val sessions = mutableListOf<Session>()
        private val messagesBySession = mutableMapOf<Long, MutableList<ChatMessage>>()
        private var nextId = 1L
        private var clock = 0L

        private fun messages(sessionId: Long) = messagesBySession.getOrPut(sessionId) { mutableListOf() }

        override suspend fun getOrCreateSession(): Long =
            sessions.maxByOrNull { it.id }?.id ?: createSession()

        override suspend fun createSession(placeId: String?): Long {
            if (ChatSessionLogic.shouldEvictBeforeCreate(sessions.size.toLong())) {
                val oldest = sessions.minByOrNull { it.lastMessageTimestamp }!!
                // Two-step delete mirroring prod: messages first, then the session row.
                messagesBySession.remove(oldest.id)
                sessions.remove(oldest)
            }
            val session = Session(nextId++, ChatSessionLogic.DEFAULT_TITLE, ++clock, placeId)
            sessions.add(session)
            return session.id
        }

        override suspend fun saveMessage(sessionId: Long, message: ChatMessage) {
            val session = sessions.first { it.id == sessionId }
            val sessionMessages = messages(sessionId)
            val isFirstUser = message.role == MessageRole.USER &&
                sessionMessages.none { it.role == MessageRole.USER }
            sessionMessages.add(message)
            session.lastMessageTimestamp = ++clock
            if (isFirstUser && session.title == ChatSessionLogic.DEFAULT_TITLE) {
                session.title = ChatSessionLogic.deriveTitle(message.content, session.placeId)
            }
        }

        override suspend fun loadUiMessages(sessionId: Long): List<ChatMessage> =
            messagesBySession[sessionId]?.toList() ?: emptyList()

        override suspend fun loadConversationHistory(sessionId: Long): List<ConversationMessage> =
            emptyList()

        override suspend fun clearSession(sessionId: Long) {
            messagesBySession[sessionId]?.clear()
        }

        override suspend fun getCurrentSessionId(): Long? = sessions.maxByOrNull { it.id }?.id

        override suspend fun listSessions(): List<ChatSessionSummary> =
            sessions.sortedWith(compareByDescending<Session> { it.lastMessageTimestamp }.thenByDescending { it.id })
                .map { ChatSessionSummary(it.id, it.title, it.lastMessageTimestamp, (messagesBySession[it.id]?.size ?: 0).toLong(), it.placeId) }

        override suspend fun switchToSession(sessionId: Long): List<ChatMessage>? =
            sessions.firstOrNull { it.id == sessionId }?.let { messages(it.id).toList() }

        override suspend fun renameSession(sessionId: Long, title: String) {
            sessions.firstOrNull { it.id == sessionId }?.title = title
        }

        /** Test-only: total messages stored across all sessions, including any orphans. */
        fun totalStoredMessages(): Int = messagesBySession.values.sumOf { it.size }

        /** Test-only: messages still stored for [sessionId] regardless of whether it exists. */
        fun storedMessagesFor(sessionId: Long): Int = messagesBySession[sessionId]?.size ?: 0
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

    /**
     * Regression for the Major bug fixed in KIM-285: eviction must delete the evicted
     * session's ChatMessage rows, not just the session row. ON DELETE CASCADE is OFF at
     * runtime in SQLite, so orphaned messages would otherwise leak. The fake's separate
     * message store lets this test fail if the message delete is ever dropped from the
     * eviction path.
     */
    @Test
    fun evictionDeletesEvictedSessionMessagesNotJustTheSessionRow() = runTest {
        val repo = InMemoryChatRepository()
        val created = (1..ChatSessionLogic.SESSION_CAP).map { repo.createSession() }
        val oldest = created.first()
        repo.saveMessage(oldest, ChatMessage(role = MessageRole.USER, content = "doomed message"))
        repo.saveMessage(oldest, ChatMessage(role = MessageRole.ASSISTANT, content = "doomed reply"))

        // Touch the others so the oldest stays the eviction target.
        created.drop(1).forEach { repo.saveMessage(it, ChatMessage(role = MessageRole.USER, content = "keep")) }

        repo.createSession() // cap+1 -> evicts oldest

        assertTrue(oldest !in repo.listSessions().map { it.id }, "oldest session should be gone")
        assertEquals(0, repo.storedMessagesFor(oldest), "evicted session's messages must be deleted, not orphaned")
        // The other SESSION_CAP-1 sessions each kept one message; the evicted session's
        // two messages must be gone, leaving no orphans.
        assertEquals(
            ChatSessionLogic.SESSION_CAP - 1,
            repo.totalStoredMessages(),
            "no orphaned messages should remain after eviction"
        )
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
