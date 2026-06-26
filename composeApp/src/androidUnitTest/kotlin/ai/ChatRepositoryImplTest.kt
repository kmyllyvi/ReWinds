package ai

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.km.rewinds.db.AppDatabase
import com.km.rewinds.db.Day as DayAdapterClass
import com.km.rewinds.db.Hour as HourAdapterClass
import core.listOfStringAdapter
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Direct tests for [ChatRepositoryImpl] — the SQLDelight-backed chat session/message store.
 * Previously 0% covered (KIM-325). The in-memory contract was already covered indirectly via a
 * hand-written fake (ChatSessionRepositoryContractTest); this exercises the REAL SQL through an
 * in-memory SQLite database, so the queries (ordering, eviction, auto-title update) are validated
 * against an actual engine, not a re-implementation.
 *
 * Lives in androidUnitTest (not commonTest) because the JDBC/in-memory driver is JVM-only — the
 * same placement used by [core.SqlDelightDatabaseTest].
 */
class ChatRepositoryImplTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repo: ChatRepositoryImpl

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val db = AppDatabase(
            driver = driver,
            DayAdapter = DayAdapterClass.Adapter(preciptypeAdapter = listOfStringAdapter),
            HourAdapter = HourAdapterClass.Adapter(preciptypeAdapter = listOfStringAdapter)
        )
        repo = ChatRepositoryImpl(db)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun user(content: String) = ChatMessage(role = MessageRole.USER, content = content)
    private fun assistant(content: String) = ChatMessage(role = MessageRole.ASSISTANT, content = content)

    // ── getOrCreateSession / getCurrentSessionId ─────────────────────────────

    @Test
    fun getOrCreateSession_createsThenReusesSameSession() = runTest {
        val first = repo.getOrCreateSession()
        val second = repo.getOrCreateSession()
        assertEquals(first, second, "second call must reuse the current session")
    }

    @Test
    fun getCurrentSessionId_nullBeforeAnySession_thenReturnsId() = runTest {
        assertNull(repo.getCurrentSessionId())
        val id = repo.getOrCreateSession()
        assertEquals(id, repo.getCurrentSessionId())
    }

    // ── saveMessage / loadUiMessages / loadConversationHistory ───────────────

    @Test
    fun saveAndLoadUiMessages_roundTripsRoleAndContent() = runTest {
        val session = repo.getOrCreateSession()
        repo.saveMessage(session, user("What's the wind like?"))
        repo.saveMessage(session, assistant("20 knots."))

        val loaded = repo.loadUiMessages(session)
        assertEquals(2, loaded.size)
        assertEquals(MessageRole.USER, loaded[0].role)
        assertEquals("What's the wind like?", loaded[0].content)
        assertEquals(MessageRole.ASSISTANT, loaded[1].role)
        assertEquals("20 knots.", loaded[1].content)
    }

    @Test
    fun loadConversationHistory_mapsRolesToApiStrings() = runTest {
        val session = repo.getOrCreateSession()
        repo.saveMessage(session, user("hi"))
        repo.saveMessage(session, assistant("hello"))

        val history = repo.loadConversationHistory(session)
        assertEquals(2, history.size)
        assertEquals("user", history[0].role)
        assertEquals("assistant", history[1].role)
        assertTrue(history[0].content.single() is AnthropicContent.Text)
        assertEquals("hi", (history[0].content.single() as AnthropicContent.Text).text)
    }

    @Test
    fun loadUiMessages_unknownSession_returnsEmpty() = runTest {
        assertTrue(repo.loadUiMessages(999L).isEmpty())
    }

    // ── auto-title behaviour ─────────────────────────────────────────────────

    @Test
    fun firstUserMessage_autoTitlesSession() = runTest {
        val session = repo.createSession(placeId = null)
        repo.saveMessage(session, user("Is it windy in spring?"))

        val summary = repo.listSessions().single { it.id == session }
        assertEquals("Is it windy in spring?", summary.title)
    }

    @Test
    fun firstUserMessage_withPlace_tagsTitle() = runTest {
        val session = repo.createSession(placeId = "Tarifa, Spain")
        repo.saveMessage(session, user("Best month for kitesurfing?"))

        val summary = repo.listSessions().single { it.id == session }
        assertEquals("Tarifa, Spain: Best month for kitesurfing?", summary.title)
    }

    @Test
    fun secondUserMessage_doesNotOverwriteTitle() = runTest {
        val session = repo.createSession()
        repo.saveMessage(session, user("First question"))
        repo.saveMessage(session, assistant("an answer"))
        repo.saveMessage(session, user("totally different follow up"))

        assertEquals("First question", repo.listSessions().single { it.id == session }.title)
    }

    @Test
    fun explicitRename_isNotClobberedByLaterFirstUserMessageLogic() = runTest {
        val session = repo.createSession()
        repo.renameSession(session, "My custom title")
        // A user message after an explicit rename must keep the custom title (title != DEFAULT_TITLE).
        repo.saveMessage(session, user("a question"))

        assertEquals("My custom title", repo.listSessions().single { it.id == session }.title)
    }

    // ── listSessions ordering + metadata ─────────────────────────────────────

    @Test
    fun listSessions_ordersByMostRecentActivityAndCountsMessages() = runTest {
        val a = repo.createSession()
        val b = repo.createSession()
        // Touch a last so it should sort ahead of b despite a lower id.
        repo.saveMessage(b, user("b first"))
        repo.saveMessage(a, user("a first"))
        repo.saveMessage(a, assistant("a reply"))

        val sessions = repo.listSessions()
        assertEquals(listOf(a, b), sessions.map { it.id })
        assertEquals(2L, sessions.single { it.id == a }.messageCount)
        assertEquals(1L, sessions.single { it.id == b }.messageCount)
    }

    // ── switchToSession ──────────────────────────────────────────────────────

    @Test
    fun switchToSession_existing_returnsMessages() = runTest {
        val first = repo.createSession()
        repo.saveMessage(first, user("hello from first"))
        repo.createSession() // a newer session

        val messages = repo.switchToSession(first)
        assertNotNull(messages)
        assertEquals("hello from first", messages.single().content)
    }

    @Test
    fun switchToSession_missing_returnsNull() = runTest {
        repo.createSession()
        assertNull(repo.switchToSession(999L))
    }

    // ── clearSession ─────────────────────────────────────────────────────────

    @Test
    fun clearSession_removesMessagesButKeepsSession() = runTest {
        val session = repo.getOrCreateSession()
        repo.saveMessage(session, user("hi"))
        repo.saveMessage(session, assistant("hello"))

        repo.clearSession(session)

        assertTrue(repo.loadUiMessages(session).isEmpty())
        // Session row itself remains and is still resolvable.
        assertEquals(session, repo.getCurrentSessionId())
    }

    // ── eviction at the session cap ──────────────────────────────────────────

    @Test
    fun creatingPastCap_evictsOldestSessionAndItsMessages() = runTest {
        val created = (1..ChatSessionLogic.SESSION_CAP).map { repo.createSession() }
        val oldest = created.first()
        repo.saveMessage(oldest, user("doomed"))
        // Touch the rest so the oldest stays the eviction target.
        created.drop(1).forEach { repo.saveMessage(it, user("keep")) }
        assertEquals(ChatSessionLogic.SESSION_CAP, repo.listSessions().size)

        repo.createSession() // cap + 1 -> evicts the oldest

        val remaining = repo.listSessions()
        assertEquals(ChatSessionLogic.SESSION_CAP, remaining.size)
        assertTrue(oldest !in remaining.map { it.id }, "oldest session should be evicted")
        // Its messages must be gone too (ON DELETE CASCADE is off at runtime; repo deletes explicitly).
        assertTrue(repo.loadUiMessages(oldest).isEmpty(), "evicted session's messages must not be orphaned")
    }
}
