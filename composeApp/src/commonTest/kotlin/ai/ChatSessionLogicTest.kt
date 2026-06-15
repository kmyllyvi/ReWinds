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

    // --- relativeTimeLabel (KIM-286) ---

    private val now = 1_000_000_000_000L

    @Test
    fun relativeTimeJustNowUnderOneMinute() {
        assertEquals("Just now", ChatSessionLogic.relativeTimeLabel(now - 30_000L, now))
        assertEquals("Just now", ChatSessionLogic.relativeTimeLabel(now, now))
    }

    @Test
    fun relativeTimeMinutes() {
        assertEquals("5m ago", ChatSessionLogic.relativeTimeLabel(now - 5 * 60_000L, now))
        assertEquals("59m ago", ChatSessionLogic.relativeTimeLabel(now - 59 * 60_000L, now))
    }

    @Test
    fun relativeTimeHours() {
        assertEquals("1h ago", ChatSessionLogic.relativeTimeLabel(now - 60 * 60_000L, now))
        assertEquals("23h ago", ChatSessionLogic.relativeTimeLabel(now - 23 * 3_600_000L, now))
    }

    @Test
    fun relativeTimeDays() {
        assertEquals("1d ago", ChatSessionLogic.relativeTimeLabel(now - 24 * 3_600_000L, now))
        assertEquals("6d ago", ChatSessionLogic.relativeTimeLabel(now - 6 * 86_400_000L, now))
    }

    @Test
    fun relativeTimeWeeks() {
        assertEquals("1w ago", ChatSessionLogic.relativeTimeLabel(now - 7 * 86_400_000L, now))
        assertEquals("3w ago", ChatSessionLogic.relativeTimeLabel(now - 21 * 86_400_000L, now))
    }

    @Test
    fun relativeTimeFutureTimestampClampsToJustNow() {
        // Clock skew shouldn't produce negative deltas.
        assertEquals("Just now", ChatSessionLogic.relativeTimeLabel(now + 60_000L, now))
    }

    // --- Place → Chat session resolution (direct fix per Kimmo) ---

    private fun summary(id: Long, ts: Long, placeId: String?) =
        ChatSessionSummary(id = id, title = "t$id", lastMessageTimestamp = ts, messageCount = 0, placeId = placeId)

    @Test
    fun resolveSessionForPlaceReturnsMatchingSessionId() {
        val sessions = listOf(
            summary(3L, ts = 300L, placeId = "Oulu"),
            summary(2L, ts = 200L, placeId = "Helsinki"),
            summary(1L, ts = 100L, placeId = null)
        )
        assertEquals(2L, ChatSessionLogic.resolveSessionForPlace("Helsinki", sessions))
    }

    @Test
    fun resolveSessionForPlaceReturnsNullWhenNoSessionTaggedWithPlace() {
        val sessions = listOf(
            summary(1L, ts = 100L, placeId = "Oulu"),
            summary(2L, ts = 200L, placeId = null)
        )
        assertNull(ChatSessionLogic.resolveSessionForPlace("Helsinki", sessions))
    }

    @Test
    fun resolveSessionForPlacePicksMostRecentWhenMultipleTagged() {
        // listSessions is newest-first; resolution must take the first (most recent) match.
        val sessions = listOf(
            summary(9L, ts = 900L, placeId = "Helsinki"),
            summary(4L, ts = 400L, placeId = "Helsinki")
        )
        assertEquals(9L, ChatSessionLogic.resolveSessionForPlace("Helsinki", sessions))
    }

    @Test
    fun resolveSessionForPlaceReturnsNullForEmptyList() {
        assertNull(ChatSessionLogic.resolveSessionForPlace("Helsinki", emptyList()))
    }

    // --- KIM-286 fix: context chips only for places that have chats ---

    @Test
    fun placesWithSessionsExcludesPlacesWithNoTaggedSession() {
        val saved = listOf("Helsinki", "Oulu", "Tampere")
        val tagged = listOf("Helsinki", null, "Helsinki") // only Helsinki has chats
        assertEquals(listOf("Helsinki"), ChatSessionLogic.placesWithSessions(saved, tagged))
    }

    @Test
    fun placesWithSessionsPreservesSavedOrderNotSessionOrder() {
        val saved = listOf("Helsinki", "Oulu", "Tampere")
        val tagged = listOf("Tampere", "Helsinki") // session order differs from chip order
        assertEquals(listOf("Helsinki", "Tampere"), ChatSessionLogic.placesWithSessions(saved, tagged))
    }

    @Test
    fun placesWithSessionsIsEmptyWhenNoSessionsAreTagged() {
        val saved = listOf("Helsinki", "Oulu")
        assertTrue(ChatSessionLogic.placesWithSessions(saved, listOf(null, null)).isEmpty())
    }

    @Test
    fun placesWithSessionsIgnoresTagsForUnsavedPlaces() {
        // A tag pointing at a place no longer in the saved list shouldn't resurrect a chip.
        val saved = listOf("Helsinki")
        val tagged = listOf("Helsinki", "DeletedPlace")
        assertEquals(listOf("Helsinki"), ChatSessionLogic.placesWithSessions(saved, tagged))
    }
}
