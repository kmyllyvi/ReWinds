# ReWinds — AI Agent Implementation Plan

## Architecture Overview

```
ChatViewModel
    ↓
AiRepository          ← orchestrates the tool-call loop
    ↓ calls
WeatherTools          ← tool schemas (JSON) + tool handlers
    ↓ delegates to
WeatherRepository     ← existing, untouched
    ↓
SQLDelight / Visual Crossing API
```

The Anthropic API is called via a dedicated `AnthropicClient` — separate from `NetworkService`,
which is coupled to Visual Crossing response shapes. Same underlying Ktor `httpClient()` factory,
different instance with Anthropic-specific config (base URL, auth header).

---

## File Structure

```
commonMain/kotlin/com.km.rewinds/
    ai/
        AnthropicClient.kt       ← Ktor HTTP calls to Anthropic API
        AnthropicModels.kt       ← Request/response data classes
        WeatherTools.kt          ← Tool schemas + tool call handlers
        AiRepository.kt          ← Agentic loop (send → tool call → send → response)
    chat/
        ChatView.kt              ← Compose UI
        ChatViewModel.kt         ← State + calls AiRepository
```

---

## Implementation Phases

### Phase A: MCP Layer (Tool Infrastructure) — No AI yet
Build and thoroughly test the tool layer independently from Anthropic API.
Once this is solid, Phase B adds the AI loop on top.

### Phase B: Agentic Loop (AI Integration)
Wire in AnthropicClient, AiRepository, and the conversational loop.

### Phase C: UI
Surface the chat in the UI.

---

## Step-by-Step Implementation

### Phase A: Tool Infrastructure

#### Step 1 — Tool Schemas & Handlers (`WeatherTools.kt`)

Build the core tool definitions first (no Anthropic API calls yet):

```kotlin
sealed class Tool {
    abstract val name: String
    abstract val description: String
    abstract val inputSchema: JsonObject

    data class GetWindSummary(/* ... */) : Tool()
    data class ListSavedPlaces(/* ... */) : Tool()
    // expand as needed
}

object WeatherTools {
    fun allToolSchemas(): List<Tool> = listOf(
        GetWindSummary(...),
        ListSavedPlaces(...)
    )

    suspend fun handleToolCall(toolName: String, args: JsonObject, repo: WeatherRepository): ToolResult
}
```

**Goal:** Define all schemas and implement handlers to validate they work with `WeatherRepository`.

#### Step 2 — Unit Tests for Tools (`WeatherToolsTest.kt`)

Test each tool independently:
- Valid inputs → correct WeatherRepository calls → lean JSON results
- Invalid inputs → error results with descriptive messages
- Edge cases (empty place list, date parsing failures, missing data)

Mock `WeatherRepository` or use real test data.

#### Step 3 — Integration Tests for Tool Layer

Test the full tool flow:
1. Pass tool schemas to hypothetical Claude (just log them for now)
2. Simulate tool calls with realistic args
3. Verify handlers return correct JSON shapes
4. Verify error handling

**No Anthropic API calls yet.** Just validate the tool infrastructure is solid.

---

### Phase B: Agentic Loop (AI Integration)

#### Step 4 — Data Models (`AnthropicModels.kt`)

Define the Anthropic API shapes using `@Serializable` data classes:

- `AnthropicRequest` — model, system prompt, messages, tools, max_tokens
- `AnthropicMessage` — role (`user` | `assistant`) + content
- `ContentBlock` — sealed class: `Text`, `ToolUse`, `ToolResult`
- `AnthropicTool` — name, description, input_schema (JsonObject)
- `AnthropicResponse` — content (list of ContentBlock), stop_reason
- `StopReason` — enum: `end_turn`, `tool_use`

> Key: `stop_reason = "tool_use"` is how Claude signals it wants to call a tool.
> The loop continues until `stop_reason = "end_turn"`.

---

### Step 2 — Anthropic HTTP Client (`AnthropicClient.kt`)

A dedicated Ktor client (not reusing `NetworkService`) configured with:

```kotlin
private val client = httpClient(enableLogs).config {
    defaultRequest {
        url("https://api.anthropic.com/v1/")
        header("x-api-key", apiKey)
        header("anthropic-version", "2023-06-01")
        contentType(ContentType.Application.Json)
    }
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
}
```

Single method:
```kotlin
suspend fun sendMessage(request: AnthropicRequest): AnthropicResponse
```

The API key should come from a `BuildConfig` / `expect`/`actual` platform config —
never hardcoded or committed.

---

### Step 3 — Tool Definitions + Handlers (`WeatherTools.kt`)

#### First tool: `get_wind_summary`

```json
{
  "name": "get_wind_summary",
  "description": "Get wind data for a saved location over a date range. Returns average wind speed, gusts, and sustained wind calculations per day.",
  "input_schema": {
    "type": "object",
    "properties": {
      "location_name": { "type": "string", "description": "Name of the saved place" },
      "start_date":    { "type": "string", "description": "ISO date YYYY-MM-DD" },
      "end_date":      { "type": "string", "description": "ISO date YYYY-MM-DD" }
    },
    "required": ["location_name", "start_date", "end_date"]
  }
}
```

#### Planned tool set (expand in later iterations):

| Tool | Description |
|---|---|
| `get_wind_summary` | Wind speed, gusts, sustained wind for a date range |
| `get_monthly_stats` | Aggregated stats for a given month |
| `get_best_days` | Filter days by wind range, no rain, etc. |
| `get_weather_day` | Full weather detail for a single day |
| `list_saved_places` | Return the user's saved locations |

> Design rule: tool results must be **lean**. Return summaries and filtered slices,
> not raw DB rows. This keeps token usage low and Claude's reasoning focused.

`WeatherTools.kt` exposes:
- `allToolSchemas: List<AnthropicTool>` — passed to every API request
- `handleToolCall(name, args, repo): String` — dispatches to `WeatherRepository`, returns a JSON string result

---

### Step 4 — Agentic Loop (`AiRepository.kt`)

This is the core of the agent. The loop:

```
1. Build request with user message + tool schemas
2. Send to Anthropic API
3. If stop_reason == "tool_use":
     a. Extract tool call(s) from response
     b. Execute each via WeatherTools.handleToolCall()
     c. Append assistant message + tool results to conversation history
     d. Send again → go to step 3
4. If stop_reason == "end_turn":
     a. Extract text content
     b. Return final response to ViewModel
```

Key detail: **conversation history is maintained in memory** for the duration of a chat session.
Each request sends the full message history so Claude has context. This is intentional — no 
persistence needed for MVP.

System prompt (starting point, iterate as you go):
```
You are a wind sports assistant for the ReWinds app. You help kitesurfers and windsurfers 
analyse historical weather data for their saved locations. Be concise and focus on 
wind-relevant insights. When asked about conditions, always consider wind speed, 
gusts, and sustained wind together.
```

---

### Step 5 — ViewModel (`ChatViewModel.kt`)

Standard pattern:

```kotlin
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChatMessage(
    val role: Role, // User | Assistant
    val content: String
)
```

- Exposes `StateFlow<ChatUiState>`
- `sendMessage(text: String)` calls `AiRepository`, appends messages, handles loading/error state
- ViewModel holds the conversation history and passes it to the repository each turn

---

### Step 6 — Chat UI (`ChatView.kt`)

Minimal first pass:
- `LazyColumn` of message bubbles (user right, assistant left)
- `TextField` + send button at the bottom
- Loading indicator while awaiting response
- No streaming for MVP — show response when complete

---

### Step 7 — Wiring into Koin (`DI.kt`)

```kotlin
single { AnthropicClient(apiKey = ..., enableLogs = ...) }
single { WeatherTools() }
single { AiRepository(get(), get(), get()) } // client, tools, weatherRepository
viewModel { ChatViewModel(get()) }
```

---

## Iteration Order

**Phase A (Tool Infrastructure):**
1. `WeatherTools.kt` — define schemas and handlers for `get_wind_summary` + `list_saved_places`
2. Write unit tests for each tool (valid args, invalid args, edge cases)
3. Write integration tests (simulate tool calls, verify JSON results)
4. Once all tests pass, mark Phase A complete

**Phase B (Agentic Loop) — only after Phase A is solid:**
5. `AnthropicModels.kt` — data classes for Anthropic API shapes
6. `AnthropicClient.kt` — test with a hardcoded "hello" message, confirm API works
7. `AiRepository.kt` — full loop, test with "what was the wind like in [place] last week?"

**Phase C (UI) — final:**
8. `ChatViewModel.kt` + `ChatView.kt` — wire up UI
9. Expand tool set iteratively (add `get_monthly_stats`, `get_best_days`, etc.)

---

## WeatherRepository API (Reference for Tool Handlers)

The `WeatherRepository` interface provides:

```kotlin
// Core queries
suspend fun getSavedPlaceNames(): List<String>
suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse?
suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse
suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse

// Maintenance
suspend fun deletePlace(placeName: String)
```

**Key implementation detail:** `getDaysRange` intelligently fills gaps in the database (only fetches missing dates from Visual Crossing API, avoiding redundant calls). Always use this for date-range queries; it handles caching internally.

**Return type:** `WeatherResponse` contains:
- `address`, `resolvedAddress`, `latitude`, `longitude`, `timezone`, `tzoffset`
- `days: List<Day>` where each `Day` has: `datetime` (ISO date string), wind speed, gusts, temperature, precipitation, etc.

**Tool result serialization:** Convert `WeatherResponse` to a lean JSON summary (not raw DB rows) to keep tokens low. Example:
```json
{
  "place": "Tarifa",
  "date_range": "2025-11-01 to 2025-11-30",
  "wind_summary": [
    {"date": "2025-11-01", "avg_wind_knots": 12, "max_gust_knots": 24, "sustained_15_25": true},
    {"date": "2025-11-02", "avg_wind_knots": 18, "max_gust_knots": 28, "sustained_15_25": true}
  ]
}
```

---

## Open Questions

### Before Phase B (AI Integration)
- Navigation: is Chat a top-level screen or accessible from within a Place screen?

### API Key Placement (Phase B Decision)
**Not blocking Phase A.** Decide before implementing `AnthropicClient`.

Options:
1. **expect/actual pattern** (recommended) — platform-specific: `BuildConfig` on Android, env var on iOS
2. Gradle properties + env var fallback — build-time config
3. Runtime config file — least secure, but flexible

See discussion above under "API Key Placement" for detailed pros/cons.

---

---

## Notes for Coder Agent

### Error Handling in Tool Execution
When a tool call fails (malformed args, missing place, date parsing error):
1. Catch the exception in `handleToolCall()`
2. Return a structured error JSON: `{ "error": "reason", "tool": "tool_name" }`
3. Append this as a `ToolResult` content block with `is_error: true` (Anthropic API field)
4. Claude will read the error and retry or reformulate

### Token Efficiency
- **System prompt:** ~150 tokens (keep it focused)
- **Tool schemas:** ~400 tokens (5 tools × 80 tokens each)
- **Per message round:** ~200 tokens base + data-dependent
- **Recommendation:** Each tool result should be <500 tokens. If results exceed this, paginate or filter.

### Conversation History Growth
In-memory history means tokens accumulate:
- First turn: ~800 tokens (schemas + system + first message)
- Each subsequent turn: +200–400 tokens (new message pair + tool results)
- **Session limit:** After ~15–20 turns, consider warning the user or implementing session reset

### Debugging the Loop
When testing `AiRepository`, use logging at key points:
1. Request sent (log tool count, message count)
2. Response received (log `stop_reason`, content block types)
3. Tool execution (log tool name, args, result length)
4. Loop continuation or termination

### Serialization Gotchas
- Anthropic expects `input_schema` as a JSON object, not a string. Use `JsonObject` from `kotlinx.serialization.json`.
- `ContentBlock` union types (Text, ToolUse, ToolResult) must deserialize correctly. Test with real API responses early.
- Dates must be ISO 8601 strings (YYYY-MM-DD). Validate and normalize in tool args before passing to `WeatherRepository`.

### Testing Strategy (for QA Agent)
1. Unit test each tool schema against sample inputs (JSON validation)
2. Integration test `AiRepository` with mocked Anthropic API and real `WeatherRepository`
3. End-to-end test Chat UI with a simple prompt (e.g., "Summarize last week's wind in Tarifa")
4. Monitor token usage and latency in real sessions

---

## Future / Phase 3 Note

When extracting to an MCP server, `WeatherTools.kt` is the extraction point.
The tool schemas and handlers move into the server — `WeatherRepository` stays in the app
and is exposed via a local interface the server calls into. The Kotlin MCP SDK wraps the transport.
