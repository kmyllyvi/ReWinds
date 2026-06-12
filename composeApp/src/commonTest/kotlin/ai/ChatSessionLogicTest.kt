package ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for KIM-285 multi-session logic: auto-title derivation (with/without a
 * place tag), the 50-session cap decision, and active-session resolution. These cover
 * the pure rules that [ChatRepository]/[ChatViewModel] delegate to.
 */
class ChatSessionLogicTest {

    // --- Auto-title: without place tag ---

    @Test
    fun titleFromShortMessageUsesFullText() {
        assertEquals("How windy is it?", ChatSessionLogic.deriveTitle("How windy is it?", placeName = null))
    }

    @Test
    fun titleFromLongMessageTruncatesWithEllipsis() {
        val msg = "What was the strongest wind gust recorded over the last twelve months in total"
        val title = ChatSessionLogic.deriveTitle(msg, placeName = null)
        assertTrue(title.endsWith("…"), "expected ellipsis, got: $title")
        // 40 chars of content + ellipsis.
        assertEquals(41, title.length)
        assertTrue(msg.startsWith(title.dropLast(1)))
    }

    @Test
    fun titleCollapsesWhitespace() {
        assertEquals("hello world", ChatSessionLogic.deriveTitle("  hello   \n  world  ", placeName = null))
    }

    @Test
    fun titleFallsBackToDefaultForBlankMessage() {
        assertEquals(ChatSessionLogic.DEFAULT_TITLE, ChatSessionLogic.deriveTitle("   ", placeName = null))
    }

    // --- Auto-title: with place tag ---

    @Test
    fun titleIncorporatesPlaceWhenTagged() {
        assertEquals(
            "Tarifa: How windy is it?",
            ChatSessionLogic.deriveTitle("How windy is it?", placeName = "Tarifa")
        )
    }

    @Test
    fun titleIsPlaceOnlyWhenMessageBlank() {
        assertEquals("Tarifa", ChatSessionLogic.deriveTitle("   ", placeName = "Tarifa"))
    }

    @Test
    fun blankPlaceTagIsIgnored() {
        assertEquals("Hi there", ChatSessionLogic.deriveTitle("Hi there", placeName = "  "))
    }

    // --- 50-session cap ---

    @Test
    fun doesNotEvictBelowCap() {
        assertFalse(ChatSessionLogic.shouldEvictBeforeCreate(0))
        assertFalse(ChatSessionLogic.shouldEvictBeforeCreate((ChatSessionLogic.SESSION_CAP - 1).toLong()))
    }

    @Test
    fun evictsAtAndAboveCap() {
        assertTrue(ChatSessionLogic.shouldEvictBeforeCreate(ChatSessionLogic.SESSION_CAP.toLong()))
        assertTrue(ChatSessionLogic.shouldEvictBeforeCreate((ChatSessionLogic.SESSION_CAP + 5).toLong()))
    }

    // --- Active-session resolution ---

    @Test
    fun resolvesRequestedSessionWhenPresent() {
        assertEquals(7L, ChatSessionLogic.resolveActiveSessionId(7L, listOf(9L, 7L, 3L)))
    }

    @Test
    fun fallsBackToMostRecentWhenRequestedMissing() {
        // List is newest-first, so head is the most recent.
        assertEquals(9L, ChatSessionLogic.resolveActiveSessionId(42L, listOf(9L, 7L, 3L)))
    }

    @Test
    fun fallsBackToMostRecentWhenNoneRequested() {
        assertEquals(9L, ChatSessionLogic.resolveActiveSessionId(null, listOf(9L, 7L, 3L)))
    }

    @Test
    fun resolvesToNullWhenNoSessionsExist() {
        assertNull(ChatSessionLogic.resolveActiveSessionId(null, emptyList()))
        assertNull(ChatSessionLogic.resolveActiveSessionId(5L, emptyList()))
    }
}
