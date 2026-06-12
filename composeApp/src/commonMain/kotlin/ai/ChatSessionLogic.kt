package ai

/**
 * A lightweight, UI-facing view of a saved chat session. Backs the (future) chat
 * list/switcher (KIM-129b); intentionally holds no messages.
 */
data class ChatSessionSummary(
    val id: Long,
    val title: String,
    val lastMessageTimestamp: Long,
    val messageCount: Long,
    val placeId: String?
)

/**
 * Pure session helpers shared by [ChatRepository] and [ChatViewModel]. Kept free of
 * database and coroutine dependencies so the auto-title, cap-eviction, and
 * active-session rules can be unit-tested directly (MV* — logic out of Composables
 * and out of I/O code).
 */
object ChatSessionLogic {

    /** Default title applied to a session before its first user message arrives. */
    const val DEFAULT_TITLE: String = "Chat"

    /** Maximum number of saved sessions; the oldest is evicted past this. */
    const val SESSION_CAP: Int = 50

    /** Upper bound for a message-derived title before ellipsis. */
    private const val TITLE_MAX_CHARS: Int = 40

    /**
     * Derives a session title from the first user message.
     *
     * When a place is tagged via the context chip, the title leads with that place
     * name. Otherwise it is the first [TITLE_MAX_CHARS] characters of the (collapsed,
     * trimmed) message text, with an ellipsis if truncated. Falls back to
     * [DEFAULT_TITLE] when nothing usable remains.
     */
    fun deriveTitle(firstUserMessage: String, placeName: String?): String {
        val tag = placeName?.trim()?.takeIf { it.isNotEmpty() }
        val snippet = truncate(firstUserMessage)

        return when {
            tag != null && snippet != null -> "$tag: $snippet"
            tag != null -> tag
            snippet != null -> snippet
            else -> DEFAULT_TITLE
        }
    }

    /**
     * Whether creating one more session would exceed [SESSION_CAP]. When true the
     * caller must evict the oldest session before inserting the new one.
     */
    fun shouldEvictBeforeCreate(currentCount: Long): Boolean = currentCount >= SESSION_CAP

    /**
     * Resolves which session to load: the explicitly requested [requestedId] when it
     * still exists, otherwise the most recent available session, otherwise null
     * (caller creates a fresh session). [availableIdsNewestFirst] is ordered the same
     * way as [ChatSessionSummary] lists — newest activity first.
     */
    fun resolveActiveSessionId(
        requestedId: Long?,
        availableIdsNewestFirst: List<Long>
    ): Long? {
        if (requestedId != null && requestedId in availableIdsNewestFirst) return requestedId
        return availableIdsNewestFirst.firstOrNull()
    }

    private fun truncate(raw: String): String? {
        val collapsed = raw.trim().replace(Regex("\\s+"), " ")
        if (collapsed.isEmpty()) return null
        return if (collapsed.length <= TITLE_MAX_CHARS) {
            collapsed
        } else {
            collapsed.take(TITLE_MAX_CHARS).trimEnd() + "…"
        }
    }
}
