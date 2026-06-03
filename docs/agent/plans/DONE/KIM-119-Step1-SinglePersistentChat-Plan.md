# KIM-119 Step 1: Single Persistent Chat

## Goal
Persist chat history across app restarts. Previously, the conversation was lost every time the app was killed.

## Approach
Single persistent session — one ongoing chat that survives restarts. No multi-chat UI yet.

---

## What Was Implemented

### Database Schema (`AppDatabase.sq`)
Two new tables:

**`ChatSession`** — tracks a chat session
- `id` (PK), `lastMessageTimestamp`, `messageCount`

**`ChatMessage`** — stores individual messages
- `id` (PK), `conversationMessageJson`, `messageRole` (USER/ASSISTANT), `messageContent`, `timestamp`, `chatSessionId` (FK)

Queries added: `getCurrentSession`, `createSession`, `updateSessionMetadata`, `insertChatMessage`, `getMessagesBySessionId`, `deleteAllMessagesBySessionId`, `getLatestSessionId`

### ChatRepository (`ai/ChatRepository.kt`) — new file
Interface + `ChatRepositoryImpl` backed by SQLDelight.

- `getOrCreateSession()` — returns existing session ID or creates a new one
- `saveMessage(sessionId, message)` — persists a UI `ChatMessage` to DB
- `loadUiMessages(sessionId)` — loads messages as `List<ChatMessage>` for display
- `loadConversationHistory(sessionId)` — loads messages as `List<ConversationMessage>` for AI context restoration
- `clearSession(sessionId)` — deletes all messages for a session

### AiRepository (`ai/AiRepository.kt`)
Added `loadHistory(messages)` — seeds the in-memory conversation history from a list of persisted messages, enabling Claude to have context of prior turns on restart.

### ChatViewModel (`ai/ChatViewModel.kt`)
- Injected `ChatRepository`
- `init` block: on startup, loads existing messages from DB → restores UI state and AI conversation context
- `sendMessage()`: persists user message before sending, persists assistant response after receiving
- `confirmPendingDataFetch()`: persists user confirmation message and assistant response
- `clearChat()`: calls `chatRepository.clearSession()` to wipe DB messages alongside clearing in-memory state

### DI (`DI.kt`)
Registered `ChatRepositoryImpl` as a Koin singleton: `single<ChatRepository> { ChatRepositoryImpl(get()) }`

---

## Behaviour After This Change

- First launch: new session created, greeting shown, no history
- After sending messages: each user + assistant message saved to DB
- App killed and reopened: previous messages reload automatically, AI has context of prior conversation
- "Clear chat": wipes DB messages and resets to greeting

## Commit
`c699915` — Feature: Implement persistent chat history (KIM-119)
