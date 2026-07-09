# KIM-285: Data layer: multiple chat sessions (list, create, switch, auto-title)

**Status:** Done · **Priority:** High · **Labels:** spec-ready
**Created:** 2026-06-12T06:27:06.184Z · **Completed:** 2026-06-12T11:45:39.024Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-285/data-layer-multiple-chat-sessions-list-create-switch-auto-title
**Related:** related: KIM-119 — Feature: Multiple chats

## Description

## Spec

Extend the chat data layer to support multiple independent saved chat sessions, replacing the current "always use the latest session" behaviour. `ChatSession`/`ChatMessage` tables already exist (added in `1.sqm`) and already key everything by `chatSessionId`, so this is additive: a new migration adds session metadata columns (title, optional place tag), repository methods for listing/creating/renaming/switching sessions, a 50-session cap, and auto-title generation from the first user message (optionally incorporating a place name if one is mentioned or tagged). This ticket is data/ViewModel only — no new screens. UI for the chat list/switcher is KIM-129b (separate ticket, pending UX direction).

Per [KIM-119](https://linear.app/kimmo-m/issue/KIM-119/feature-multiple-chats) discussion: chats are NOT 1:1 with places. A chat session may optionally carry a `placeId` tag (nullable) used for auto-titling, but is not owned by or restricted to that place.

## Acceptance criteria

- [ ] A new SQLDelight migration (`5.sqm`) adds `title TEXT` and `placeId TEXT` (nullable, no FK constraint required) columns to `ChatSession`, with existing rows getting a non-null default title (e.g. "Chat")
- [ ] `AppDatabase.sq` includes a query to list all chat sessions ordered by `lastMessageTimestamp` descending
- [ ] `AppDatabase.sq` includes a query to create a new `ChatSession` row and return its generated id
- [ ] `AppDatabase.sq` includes a query to update a `ChatSession`'s `title`
- [ ] `AppDatabase.sq` includes a query to delete the oldest `ChatSession` (lowest `lastMessageTimestamp`) when the cap is exceeded
- [ ] `ChatRepository` (or equivalent) exposes a method to list all chat sessions as a `Flow`/list of session summaries (id, title, lastMessageTimestamp, messageCount, placeId)
- [ ] `ChatRepository` exposes a method to create a new chat session and returns its id
- [ ] `ChatRepository` exposes a method to switch the active session by id, used by `ChatViewModel` to load that session's messages via the existing `getMessagesBySessionId` query
- [ ] When a new chat session's first user message is saved, the repository auto-generates a title: if a place is tagged/selected via the existing context chip, the title incorporates that place's name; otherwise the title is derived from the first ~40 characters of the message text
- [ ] When creating a new chat session would exceed 50 total sessions, the repository deletes the oldest session (and its messages cascade via existing `ON DELETE CASCADE`) before inserting the new one
- [ ] `ChatViewModel` no longer hardcodes "always load the latest session" — it loads whichever session id is marked/passed as active, defaulting to the most recent if none is set
- [ ] Existing single-chat behaviour (app launch with no prior sessions) still works: a session is created automatically and used as before

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit covering: migration applies cleanly, session list ordering, auto-title generation (with and without place tag), 50-session cap eviction, ChatViewModel session-switching logic
- [ ] No MV* violations (see ARCHITECTURE-RULES.md) — session list/switch state and logic live in ChatRepository/ChatViewModel, not in Composables
- [ ] No new lint violations

## Notes

* Confirmed by Kimmo ([KIM-119](https://linear.app/kimmo-m/issue/KIM-119/feature-multiple-chats) comment, 2026-06-12): chats are not location-locked; independent sessions with optional place tag for auto-titling.
* This ticket is data/ViewModel-only by design — keeps it to one session's worth of work. KIM-129b (chat list/switcher UI) and [KIM-130](https://linear.app/kimmo-m/issue/KIM-130/delete-chat) (delete chat, builds on this session model) follow once 129a lands.
* Migration check (per Armin): `ChatSession`/`ChatMessage` already have a proper `.sqm` migration (`1.sqm`, schema v1→v2) — no missing-migration issue. This ticket adds `5.sqm` for the new columns.
* Reviewers needed: code-reviewer (always), qa-test-agent (logic-heavy: migration + auto-title + cap eviction + session-switch logic).

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-12T07:58:40.388Z

## Technical implementation notes (Architect pass, 2026-06-12)

Schema, repository, ViewModel and nav reviewed against current code. One correction needed to the spec; everything else checks out.

### ⚠️ Correction: `placeId` should be `placeId TEXT`, not `INTEGER`

There is **no integer-keyed Places table** in the schema. Places are identified by `WeatherResponse.resolvedAddress` (`TEXT NOT NULL PRIMARY KEY`), and `WeatherRepository.getSavedPlaceNames()` (used by `ChatViewModel.loadContextChips()`) returns `List<String>` of place names/addresses — not ids.

**Action**: `5.sqm` should add `placeId TEXT` (nullable), conceptually storing the place name / `resolvedAddress` string from the selected `ContextChip`. No FK constraint needed (per spec), but the column type must be TEXT to match `WeatherResponse.resolvedAddress`. Keep the column name `placeId` for ticket-naming consistency if preferred, but it MUST be `TEXT`, not `INTEGER`.

### Schema/migration — confirmed sound otherwise

- Latest migration on disk is `4.sqm` (v4→5), and `AppDatabase.sq` already reflects schema v5 (`WeatherStation`, `ChatSession`/`ChatMessage` from `1.sqm`, `AppSettings`, `Day`/`Hour` station rework from `4.sqm`). **`5.sqm` (v5→6) is the correct next migration number** — no collision.
- `ChatSession` (from `1.sqm`) currently has: `id, lastMessageTimestamp, messageCount`. Adding `title TEXT NOT NULL DEFAULT 'Chat'` and `placeId TEXT` (nullable) via two `ALTER TABLE ChatSession ADD COLUMN ...` statements is straightforward — no need for the create-temp-table-and-copy approach `4.sqm` used (that was only needed because of a column *removal*).
- `ChatMessage` FK to `ChatSession(id) ON DELETE CASCADE` already exists, so 50-session cap eviction (delete oldest `ChatSession`) cascade-deletes its messages automatically as the spec assumes.

### Repository (`ChatRepository.kt` / `ChatRepositoryImpl`)

Current interface is single-session shaped:
```kotlin
interface ChatRepository {
    suspend fun getOrCreateSession(): Long
    suspend fun saveMessage(sessionId: Long, message: ChatMessage)
    suspend fun loadUiMessages(sessionId: Long): List<ChatMessage>
    suspend fun loadConversationHistory(sessionId: Long): List<ConversationMessage>
    suspend fun clearSession(sessionId: Long)
    suspend fun getCurrentSessionId(): Long?
}
```
`getOrCreateSession()` currently does "find session with MAX(id), else create" via the `getCurrentSession` query (`SELECT * FROM ChatSession WHERE id = (SELECT MAX(id) FROM ChatSession) LIMIT 1`). Keep this for the **first-launch fallback** (acceptance criterion: existing single-chat behaviour still works) but it should no longer be the only path.

Suggested additions:
```kotlin
data class ChatSessionSummary(
    val id: Long,
    val title: String,
    val lastMessageTimestamp: Long,
    val messageCount: Long,
    val placeId: String? // TEXT, see correction above
)

suspend fun listSessions(): List<ChatSessionSummary>     // ordered by lastMessageTimestamp DESC
suspend fun createSession(placeId: String? = null): Long  // enforces 50-cap eviction before insert
suspend fun setSessionTitle(sessionId: Long, title: String)
// getOrCreateSession() retained for first-launch fallback
```

Auto-title generation: implement **in the repository**, triggered from `saveMessage()` when it's the session's first user message and the title is still the default ("Chat") — check `getMessagesBySessionId(sessionId).isEmpty()` before insert. Title = first ~40 chars of message text, optionally combined with the place name from `ChatSession.placeId` if non-null. This is pure string logic — **no AI call needed**, consistent with the spec's "derived from the first ~40 characters" wording. Worth a quick confirm with PO that no AI-generated title is expected for v1, but the simple heuristic satisfies the acceptance criteria as written.

### New queries needed in `AppDatabase.sq`

```sql
listChatSessions:
SELECT * FROM ChatSession ORDER BY lastMessageTimestamp DESC;

createSessionWithTitle:
INSERT INTO ChatSession(lastMessageTimestamp, messageCount, title, placeId) VALUES (?, ?, ?, ?);

updateSessionTitle:
UPDATE ChatSession SET title = ? WHERE id = ?;

getOldestSessionId:
SELECT id FROM ChatSession ORDER BY lastMessageTimestamp ASC LIMIT 1;

deleteSessionById:
DELETE FROM ChatSession WHERE id = ?;

countSessions:
SELECT COUNT(*) FROM ChatSession;
```
(`deleteSessionById` relies on the existing `ON DELETE CASCADE` on `ChatMessage.chatSessionId` to clean up messages.)

### ViewModel (`ChatViewModel.kt`)

- `currentSessionId: Long?` is currently a private field set once in `init{}` via `getOrCreateSession()`. For session-switching, add `fun switchSession(sessionId: Long)`: clears `aiRepository` history, calls `chatRepository.loadUiMessages(sessionId)` + `loadConversationHistory(sessionId)`, updates `currentSessionId` and `_uiState.messages`. Consider extracting a shared `private suspend fun loadSession(sessionId: Long)` used by both `init{}` and `switchSession()` to avoid duplicating the load logic.
- `clearChat()` currently calls `chatRepository.clearSession(it)`, which **deletes messages but keeps the session row** (`deleteAllMessagesBySessionId`). For "create new chat" (KIM-285 scope) add a distinct `startNewSession(placeId: String? = null)`: calls `chatRepository.createSession(placeId)`, switches to it, resets `_uiState` to the fresh-chat default. Don't conflate with `clearChat()` — KIM-130 (Delete chat) will need yet a third operation (delete session entirely + its messages, then switch to another session or create one if none remain).
- `ContextChip`/`loadContextChips()` already exist and expose place names via `weatherRepository.getSavedPlaceNames()` — `ChatSession.placeId` (TEXT) can store the selected chip's `placeName` directly with no translation layer, confirming the TEXT recommendation above.
- No DI changes expected — `ChatRepositoryImpl(db: AppDatabase)` constructor is unchanged; only the interface grows. Title generation is pure string logic, no new dependencies.

### Navigation — no changes needed for KIM-285

`ChatRoute(initialMessage: String? = null)` (`NavigationRoutes.kt`) and `NavigatorImpl.navigateToChat()` are unaffected by this ticket. Flagging for KIM-129b only: `ChatRoute` will likely need a `sessionId: Long? = null` param so navigation can open a specific session from the list/switcher — keep in mind when 129b is specced so the route shape doesn't need a breaking change later.

### Remaining open questions / risks for the developer

1. **`placeId` type** (above) — must be `TEXT`, not `INTEGER`. This is the one spec change needed before/during implementation; everything else is additive.
2. **Default title for existing rows**: spec says "non-null default title (e.g. 'Chat')" — `DEFAULT 'Chat'` on the `ALTER TABLE ADD COLUMN` is sufficient per spec; flagging only so it's a conscious choice vs. wanting per-row generated titles for pre-existing sessions.
3. **Auto-title timing**: fires on first **user** message only (session is created empty, so hook into `saveMessage()` when role == USER and it's the first message in that session).
4. **`getCurrentSessionId()`** — grep call sites; with multi-session, "current" becomes ambiguous (latest in DB vs. active in ChatViewModel UI state). Recommend restricting this method to the first-launch fallback path only, not as a general "active session" accessor once switching exists.
5. Confirm `loadConversationHistory()` / `aiRepository.loadHistory()` is reset on `switchSession()` so conversation context doesn't bleed across sessions when calling Claude.

No remaining schema-collision or migration-numbering concerns — `5.sqm` is correctly the next file. The TEXT-vs-INTEGER `placeId` fix is the only blocking correction; everything else is additive and low-risk for the developer picking this up.

