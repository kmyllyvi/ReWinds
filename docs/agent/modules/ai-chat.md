# AI Chat Module

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. -->

**Last updated:** 2026-06-17 (PR #36 — KIM-297)
**Status:** Active

---

## Purpose

Provides the conversational AI feature backed by the Anthropic Claude API. The module orchestrates a multi-turn agentic loop in which Claude can invoke weather-data tools to answer user questions about their saved locations. Chat history is persisted per session in SQLite.

---

## Responsibilities

- Sending and receiving messages to/from the Anthropic `/v1/messages` endpoint.
- Running the agentic tool loop (up to `ANTHROPIC_MAX_TURNS = 20` turns per user message).
- Dispatching tool calls to `WeatherTools` and returning results back to Claude.
- Gating data fetches behind a three-layer permission flow: check availability → return `permission_required` response → user confirms → fetch → retry.
- Persisting conversation messages per session via `ChatRepository` / SQLDelight.
- Exposing UI state (`ChatUiState`) to `ChatView` including loading, error, pending-fetch, and context-chip states.
- Translating user-friendly metric names to database field names (`MetricMapper`).
- Parsing natural-language days-of-interest criteria into structured `DaysOfInterestFilter` objects (`DaysOfInterestParser`).

---

## Dependencies

### Internal
- `core.WeatherRepository` — queried by tool handlers to read weather data.
- `core.ApiKeyManager` — runtime in-memory store for the Anthropic API key.
- `core.AppConstants` — model name, max tokens, max turns, system prompt, API base URL.
- `core.AppDatabase` (via `ChatRepository`) — persists sessions and messages.

### External
- **Anthropic API** `https://api.anthropic.com/v1/messages` — Claude `claude-haiku-4-5`.
- **Ktor HTTP client** — platform-specific `httpClient(enableLogs)` expect/actual.

---

## Key interfaces

### AnthropicClient
```kotlin
suspend fun sendMessage(request: AnthropicRequest): AnthropicResponse
suspend fun sendRawMessage(request: JsonObject): JsonObject   // single-turn structured extraction
```
Throws `AnthropicException` with optional `httpStatus` (401/403 distinguishable from generic errors).

### AiRepository
```kotlin
suspend fun sendMessage(userMessage: String): AiMessageResult
fun clearHistory()
fun loadHistory(messages: List<ConversationMessage>)
fun getHistory(): List<ConversationMessage>
```
`AiMessageResult` carries `responseText`, `toolCallsMade: List<String>`, `totalTurns: Int`.

### WeatherTools (object)
```kotlin
fun allToolSchemas(): List<Tool>
suspend fun handleToolCall(toolName: String, args: JsonObject, repo: WeatherRepository): String
```
Available tools: `get_weather_metrics`, `get_wind_summary`, `list_saved_places`, `get_monthly_stats`, `get_best_days`.

### MetricMapper (object)
```kotlin
fun mapMetricName(metricName: String): String   // friendly name → DB field name
fun getUnits(fieldName: String): String
fun formatValue(fieldName: String, value: Any?): Any?
fun getDisplayName(metricName: String): String  // title-cases a metric name for display
```
Supports ~50 metric alias variants across all `Day` fields. Wind values (`windspeed`, `windgust`) are stored in the database in km/h and are converted to knots by dividing by 1.852 before being returned to Claude. All numeric fields are rounded to 1 decimal place. Categorical and timestamp fields (`conditions`, `description`, `icon`, `sunrise`, `sunset`, `moonphase`, `winddir`, `preciptype`) are returned as-is with no unit suffix.

### ChatRepository
```kotlin
suspend fun getOrCreateSession(): Long
suspend fun saveMessage(sessionId: Long, message: ChatMessage)
suspend fun loadUiMessages(sessionId: Long): List<ChatMessage>
suspend fun loadConversationHistory(sessionId: Long): List<ConversationMessage>
suspend fun clearSession(sessionId: Long)
```

### ChatViewModel (public state)
```kotlin
val uiState: StateFlow<ChatUiState>
fun sendMessage(userInput: String)
fun onInputTextChange(text: String)
fun clearChat()
fun selectContextChip(placeName: String?)
fun onErrorDismissed()
fun onApiKeyDialogDismissed()
fun onApiKeyInvalidErrorDismissed()
fun openPlaceChat(placeId: String)
fun ensureGeneralChat()
```

`ensureGeneralChat()` — resets the active session to the most-recent untagged (general) session, or creates one, when `currentPlaceTag != null`. No-op if the current session is already general. Called by `ChatView` on every plain (no `placeId`) entry to the Chat tab so that a prior "Ask AI about this place" visit does not persist as the active session across tab switches (KIM-297).

---

## Chat tab session lifecycle (KIM-297)

`ChatViewModel` is a singleton scoped to the app's `ViewModelStore`; `currentSessionId` and `currentPlaceTag` persist across tab switches.

Session selection on `ChatView` entry is controlled by a `LaunchedEffect(placeId)`:
- `placeId != null` — calls `vm.openPlaceChat(placeId)` then `onPlaceIdConsumed()`.
- `placeId == null` and `hadPlaceId == false` — calls `vm.ensureGeneralChat()`.
- `placeId == null` and `hadPlaceId == true` — no-op (this is the consume transition, not a cold entry).

The `hadPlaceId` flag (`remember { mutableStateOf(false) }`, per composable instance) prevents `ensureGeneralChat()` from racing against the immediate placeId-consume re-trigger that follows `openPlaceChat()`.

## Known constraints

- Model is hard-coded to `claude-haiku-4-5` via `AppConstants.ANTHROPIC_MODEL`. Changing model requires updating the constant only.
- Max tokens per response: 1024 (`AnthropicRequest.maxTokens`). Not driven by `AppConstants.ANTHROPIC_MAX_TOKENS` at the call site — the constant exists but is not wired to the request builder as of bootstrap.
- Streaming responses are not implemented (`AppConstants.FEATURE_STREAMING_CHAT = false`).
- `ChatRepository.loadConversationHistory` reconstructs history as single `Text` content blocks; multi-part tool-use turns from previous sessions are not preserved with full fidelity.
- The permission-flow pattern in `ChatViewModel.sendMessage` uses regex to extract location/date from the AI response text, which is fragile if the model's phrasing changes.
- Context chips are UI-only state; the selected chip does not currently constrain which tool data Claude receives — the context chip drives no filtering in `AiRepository` or `WeatherTools`.

---

## Decisions log

- See `/docs/agent/decisions/` for records created after the bootstrap phase.
