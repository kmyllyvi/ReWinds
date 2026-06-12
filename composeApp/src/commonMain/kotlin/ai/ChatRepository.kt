package ai

import com.km.rewinds.db.AppDatabase
import core.Log
import kotlin.time.Clock

interface ChatRepository {
    suspend fun getOrCreateSession(): Long
    suspend fun saveMessage(sessionId: Long, message: ChatMessage)
    suspend fun loadUiMessages(sessionId: Long): List<ChatMessage>
    suspend fun loadConversationHistory(sessionId: Long): List<ConversationMessage>
    suspend fun clearSession(sessionId: Long)
    suspend fun getCurrentSessionId(): Long?

    /** All saved sessions, newest activity first. */
    suspend fun listSessions(): List<ChatSessionSummary>

    /**
     * Creates a new session and returns its id, evicting the oldest session first
     * when the [ChatSessionLogic.SESSION_CAP] would be exceeded. [placeId] is an
     * optional place tag used for auto-titling.
     */
    suspend fun createSession(placeId: String? = null): Long

    /**
     * Loads the messages for [sessionId], making it the active session. Returns null
     * when the session does not exist.
     */
    suspend fun switchToSession(sessionId: Long): List<ChatMessage>?

    /** Renames a session. */
    suspend fun renameSession(sessionId: Long, title: String)
}

class ChatRepositoryImpl(private val db: AppDatabase) : ChatRepository {

    private val queries get() = db.appDatabaseQueries

    private fun now() = Clock.System.now().toEpochMilliseconds()

    override suspend fun getOrCreateSession(): Long {
        val existing = queries.getCurrentSession().executeAsOneOrNull()
        if (existing != null) return existing.id
        return createSession()
    }

    override suspend fun createSession(placeId: String?): Long {
        if (ChatSessionLogic.shouldEvictBeforeCreate(queries.countSessions().executeAsOne())) {
            queries.deleteOldestSession()
            Log.d("ChatRepository: session cap reached, evicted oldest session")
        }
        queries.createSession(
            lastMessageTimestamp = now(),
            messageCount = 0,
            title = ChatSessionLogic.DEFAULT_TITLE,
            placeId = placeId
        )
        return queries.getLatestSessionId().executeAsOne()
    }

    override suspend fun saveMessage(sessionId: Long, message: ChatMessage) {
        val isFirstUserMessage = message.role == MessageRole.USER &&
            queries.getMessagesBySessionId(sessionId).executeAsList().none { it.messageRole == "USER" }

        val timestamp = now()
        queries.insertChatMessage(
            conversationMessageJson = message.content,
            messageRole = message.role.name,
            messageContent = message.content,
            timestamp = timestamp,
            chatSessionId = sessionId
        )
        val count = queries.getMessagesBySessionId(sessionId).executeAsList().size.toLong()
        queries.updateSessionMetadata(timestamp, count, sessionId)

        if (isFirstUserMessage) {
            autoTitleSession(sessionId, message.content)
        }

        Log.d("ChatRepository: saved message (role=${message.role.name}, session=$sessionId)")
    }

    /**
     * Generates the session title from its first user message, tagging the place when
     * one is associated with the session. Only the still-default title is overwritten,
     * so an explicit rename is never clobbered.
     */
    private fun autoTitleSession(sessionId: Long, firstUserMessage: String) {
        val session = queries.getSessionById(sessionId).executeAsOneOrNull() ?: return
        if (session.title != ChatSessionLogic.DEFAULT_TITLE) return

        val title = ChatSessionLogic.deriveTitle(firstUserMessage, session.placeId)
        queries.updateSessionTitle(title, sessionId)
        Log.d("ChatRepository: auto-titled session $sessionId -> \"$title\"")
    }

    override suspend fun listSessions(): List<ChatSessionSummary> {
        return queries.listSessions().executeAsList().map { row ->
            ChatSessionSummary(
                id = row.id,
                title = row.title,
                lastMessageTimestamp = row.lastMessageTimestamp,
                messageCount = row.messageCount,
                placeId = row.placeId
            )
        }
    }

    override suspend fun switchToSession(sessionId: Long): List<ChatMessage>? {
        if (queries.getSessionById(sessionId).executeAsOneOrNull() == null) return null
        return loadUiMessages(sessionId)
    }

    override suspend fun renameSession(sessionId: Long, title: String) {
        queries.updateSessionTitle(title, sessionId)
    }

    override suspend fun loadUiMessages(sessionId: Long): List<ChatMessage> {
        return queries.getMessagesBySessionId(sessionId).executeAsList().map { row ->
            ChatMessage(
                id = row.id.toString(),
                role = if (row.messageRole == "USER") MessageRole.USER else MessageRole.ASSISTANT,
                content = row.messageContent,
                timestamp = row.timestamp
            )
        }
    }

    override suspend fun loadConversationHistory(sessionId: Long): List<ConversationMessage> {
        return queries.getMessagesBySessionId(sessionId).executeAsList().map { row ->
            ConversationMessage(
                role = if (row.messageRole == "USER") "user" else "assistant",
                content = listOf(AnthropicContent.Text(text = row.messageContent))
            )
        }
    }

    override suspend fun clearSession(sessionId: Long) {
        queries.deleteAllMessagesBySessionId(sessionId)
        Log.d("ChatRepository: cleared messages for session $sessionId")
    }

    override suspend fun getCurrentSessionId(): Long? {
        return queries.getCurrentSession().executeAsOneOrNull()?.id
    }
}
