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
}

class ChatRepositoryImpl(private val db: AppDatabase) : ChatRepository {

    private val queries get() = db.appDatabaseQueries

    override suspend fun getOrCreateSession(): Long {
        val existing = queries.getCurrentSession().executeAsOneOrNull()
        if (existing != null) return existing.id
        val now = Clock.System.now().toEpochMilliseconds()
        queries.createSession(now, 0)
        return queries.getLatestSessionId().executeAsOne()
    }

    override suspend fun saveMessage(sessionId: Long, message: ChatMessage) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertChatMessage(
            conversationMessageJson = message.content,
            messageRole = message.role.name,
            messageContent = message.content,
            timestamp = now,
            chatSessionId = sessionId
        )
        val count = queries.getMessagesBySessionId(sessionId).executeAsList().size.toLong()
        queries.updateSessionMetadata(now, count, sessionId)
        Log.d("ChatRepository: saved message (role=${message.role.name}, session=$sessionId)")
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
