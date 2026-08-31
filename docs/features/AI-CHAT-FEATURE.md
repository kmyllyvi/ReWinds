# AI Chat Feature - Technical Documentation

**Status**: ✅ Implemented and Working (Phase A-C Complete)
**Last Updated**: March 2, 2026
**Platforms**: Android ✅, iOS ✅

---

## Overview

The ReWinds app now includes an **AI-powered weather chat assistant** that helps users analyze their saved weather locations using natural language. The feature uses Claude (Anthropic API) with real-time tool calling to provide intelligent wind sports insights.

### Architecture Summary

```
ChatView (UI)
    ↓
ChatViewModel (State Management)
    ↓
AiRepository (Agentic Loop Orchestration)
    ↓
AnthropicClient (HTTP to Anthropic API)
    ↓
WeatherTools (Tool Schemas + Handlers)
    ↓
WeatherRepository (Data Access)
```

---

## Implementation Phases

### Phase A: MCP Tool Layer ✅ Complete
**Goal**: Build and test tool infrastructure independently

**Tools Implemented** (4 tools):
1. **`get_wind_summary`** - Wind data for a location over a date range
   - Returns: daily wind speeds, gusts, sustained wind calculations, temperature, precipitation
   - Parameters: location_name, start_date (ISO 8601), end_date (ISO 8601)

2. **`list_saved_places`** - All saved location names
   - Returns: list of place names in user's library
   - Parameters: none

3. **`get_monthly_stats`** - Aggregated statistics for a month
   - Returns: min/max/avg wind speeds, gust data, rainy days
   - Parameters: location_name, year (YYYY), month (1-12)

4. **`get_best_days`** - Filter days by wind and weather criteria
   - Returns: days matching specified conditions
   - Parameters: location_name, start_date, end_date, min_wind_speed (optional), max_wind_speed (optional), max_gust (optional), no_rain (optional)

**Files**:
- `composeApp/src/commonMain/kotlin/ai/WeatherTools.kt` (465 lines)
- Test files: `WeatherToolsTest.kt`, `WeatherToolsIntegrationTest.kt`

**Test Coverage**:
- Unit tests for each tool with valid/invalid/edge case inputs
- Integration tests validating JSON results
- All tests passing ✅

---

### Phase B: Agentic Loop (AI Integration) ✅ Complete
**Goal**: Wire Anthropic API and orchestrate the tool-call loop

#### AnthropicModels.kt (Data Layer)
Defines all Anthropic API request/response shapes:
- `StopReason` enum: `end_turn`, `tool_use`, `max_tokens`
- `ContentBlock` sealed class: `Text`, `ToolUse`, `ToolResult`
- `AnthropicMessage`, `AnthropicRequest`, `AnthropicResponse`
- `AnthropicTool` - Tool schemas sent to Claude
- `UsageData` - Token tracking

**Key Feature**: Custom deserializer for polymorphic `ContentBlock` types to handle Claude's varied response formats.

**Tests**: 17 unit tests covering serialization, deserialization, and data structure validation ✅

#### AnthropicClient.kt (HTTP Client)
Ktor-based HTTP client for Anthropic API:
- **Base URL**: `https://api.anthropic.com/v1/`
- **Authentication**: `x-api-key` header with API key
- **Headers**:
  - `anthropic-version: 2023-06-01`
  - `Content-Type: application/json`
- **Error Handling**: Catches `ClientRequestException` and generic exceptions, wraps in `AnthropicException`
- **JSON Serialization**: Uses `kotlinx.serialization.json` with `ignoreUnknownKeys = true`

**Implementation**:
```kotlin
class AnthropicClient(
    private val apiKey: String,
    private val enableLogs: Boolean = false
) {
    private val client: HttpClient = httpClient(enableLogs)

    suspend fun sendMessage(request: AnthropicRequest): AnthropicResponse
}
```

#### AiRepository.kt (Agentic Loop)
Core orchestration logic for multi-turn conversations with tool calling:

**Key Features**:
- **In-memory conversation history** - Maintains full message history for context
- **Tool-call loop** - Automatically handles up to 20 turns of tool invocations
- **Tool dispatch** - Routes tool calls to `WeatherTools.handleToolCall()`
- **Conversation reconstruction** - Rebuilds history after tool calls for next API request
- **Token management** - Detects max_tokens overflow and provides user feedback
- **System prompt** - Focused on wind sports analysis

**Loop Flow**:
```
1. User sends message
2. AiRepository adds message to history
3. Sends request to Anthropic API (with tool schemas)
4. If stop_reason == "tool_use":
   - Extract tool calls from response
   - Execute each via WeatherTools
   - Add tool results to history
   - Send again (step 3)
5. If stop_reason == "end_turn":
   - Extract final text response
   - Return to ViewModel
```

**Result Type**:
```kotlin
data class AiMessageResult(
    val responseText: String,
    val toolCallsMade: Int = 0,
    val totalTurns: Int = 1
)
```

**Tests**: 8 integration tests covering tool dispatch, multi-turn loops, error handling ✅

#### ChatViewModel.kt (State Management)
Standard MVVM ViewModel following ReWinds patterns:

**State**:
```kotlin
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = ""
)
```

**Public Methods**:
- `sendMessage(userInput: String)` - Send user message, trigger AI response
- `onInputTextChange(text: String)` - Update input field text
- `onErrorDismissed()` - Clear error state
- `clearChat()` - Reset conversation and history

**Coroutine Handling**: Uses `viewModelScope.launch(Dispatchers.IO)` for AI operations, updates UI state atomically.

---

### Phase C: Chat UI ✅ Complete
**Goal**: Surface the chat in Compose UI

#### ChatView.kt (Compose UI)
Clean, simple chat interface with 5 components:

1. **ChatView** - Main container
   - Header with back button and "Chat" title
   - Auto-scrolling message list (LazyColumn)
   - Input area at bottom
   - Error notification with dismissible button

2. **ChatHeader** - Navigation header

3. **ChatMessageBubble** - Individual message display
   - User messages: right-aligned, primary color background
   - Assistant messages: left-aligned, surface variant background
   - Proper padding and spacing

4. **ErrorMessageBox** - Error notification
   - Error container color
   - Dismissible with button
   - Animated visibility

5. **ChatInputArea** - User input
   - OutlinedTextField (max 3 lines)
   - Send button with validation
   - Disabled while loading

**Theme Integration**:
- Full Material3 support
- Light and dark mode support
- Uses existing ReWinds theme colors
- Respects Material3 typography

**Navigation Integration**:
- Added `ChatRoute` to `NavigationRoutes.kt`
- Navigation methods in `Navigator.kt`
- Chat button in `HomeView.kt` header
- Back stack support for returning to home

**Tests**: Compiles and renders correctly ✅

---

## API Key Configuration

### For Development/Testing

The app provides **placeholder keys** to allow development without breaking on startup:
- Both Android and iOS use `sk-placeholder-dev-key-not-configured`
- App initializes and renders Chat UI
- Chat will fail at runtime with proper error message

### For Production Use

Set a real Anthropic API key before running:

**Android**:
```bash
export ANTHROPIC_API_KEY=sk-ant-<your-key>
./gradlew installDebug  # or run on emulator
```

**iOS**:
```bash
export ANTHROPIC_API_KEY=sk-ant-<your-key>
xcodebuild -workspace iosApp/iosApp.xcworkspace -scheme ReWinds -configuration Debug
```

Or update `Platform.apple.kt` to load from Info.plist or BuildConfig for production.

### Future Improvement
For production, implement secure key storage:
- Android: Use EncryptedSharedPreferences or BuildConfig
- iOS: Use Keychain via secure storage library
- Both: Load from secure configuration during app init

---

## Koin Dependency Injection

All AI components are registered in `DI.kt`:

```kotlin
fun appModule(...) = module {
    // AI Components
    single { AnthropicClient(apiKey = getAnthropicApiKey(), enableLogs = enableNetworkLogs) }
    single { WeatherTools }
    single { AiRepository(get(), get(), get()) }  // Client, Tools, Repository

    // ViewModels
    viewModelOf(::ChatViewModel)  // Auto-wires AiRepository
}
```

**Why This Pattern**:
- Single instances (AnthropicClient, WeatherTools) are shared across the app
- ChatViewModel is created per screen instance
- `get()` automatically resolves dependencies
- Easy to mock for testing

---

## Error Handling

### User-Facing Errors
The ChatView displays errors gracefully:
- Network failures → "Error: Connection failed..."
- API errors → "Error: Anthropic API error..."
- Tool failures → "Error: Tool execution failed..."
- Invalid API key → "Error: 401 Unauthorized..."

Users can dismiss errors and retry.

### Developer-Facing Errors
All components log errors:
- `Log.d()` for debug info
- `Log.e()` for exceptions
- Full exception stack traces in development

See app logs for detailed debugging information.

---

## Token Efficiency

### System Prompt
- ~150 tokens - Focused on wind sports use case

### Tool Schemas
- ~400 tokens total for all 4 tools
- Each tool: ~80-100 tokens

### Per Message Round
- First message: ~800 tokens (schemas + system + message)
- Subsequent: ~200-400 tokens (message pair + optional tool results)

### Conversation History
- In-memory storage means tokens accumulate with conversation length
- After ~15-20 turns, recommend warning user or implementing session reset
- MVP doesn't persist history - each app launch starts fresh

---

## Testing

### Test Files
- **Anthropic Models Tests**: `ai/AnthropicModelsTest.kt` (17 tests)
- **AI Repository Tests**: `ai/AiRepositoryTest.kt` (8 tests)
- **Weather Tools Tests**: Already in Phase A

### Running Tests
```bash
./gradlew testDebugUnitTest          # Unit tests
./gradlew connectedAndroidTest       # Device tests (if available)
./gradlew test                       # All tests
```

### Current Status
- ✅ 76/76 tests passing
- ✅ No compilation errors
- ✅ Both Android and iOS verified

---

## Known Limitations & Future Work

### Current Limitations
1. **No API key in production** - Requires manual setup via environment variables
2. **No streaming** - Full response shown when complete
3. **No persistence** - Chat history lost on app restart
4. **In-memory only** - No database storage of conversations
5. **Limited tools** - 4 tools; can expand with more weather analysis tools

### Planned Enhancements
1. **Streaming responses** - Show Claude's response as it's generated
2. **Chat persistence** - Store conversation history in SQLDelight
3. **Session management** - Auto-reset after N turns or time period
4. **More tools** - Add analysis tools (e.g., `forecast_next_week`, `compare_locations`, `wind_consistency`)
5. **MCP Server extraction** - Move tools to dedicated MCP server
6. **Secure API key storage** - Keychain on iOS, secure storage on Android
7. **User preferences** - System prompt customization, tool selection

---

## Architecture Decisions

### Why In-Memory History?
- Simple MVP implementation
- Sufficient for single-session use
- Easy to add persistence later
- Avoids database complexity for chat feature

### Why Stateless Loop?
- Single `sendMessage()` call completes full conversation
- No async callback handling needed
- Easier to reason about state transitions
- Proper error handling at each turn

### Why Separate AnthropicClient?
- Decoupled from `NetworkService` (which is Visual Crossing-specific)
- Independent configuration and error handling
- Potential to extract to MCP server later
- Clear responsibility boundary

### Why Tool Schemas in Code?
- Ensures type safety with Kotlin
- Easy to validate inputs
- Schema changes caught at compile time
- No JSON string parsing

---

## References

### Documentation
- `/docs/plans/ai-agent-plan.md` - Original implementation plan
- `/docs/plans/ai-mcp-plan-highlevel.md` - 3-phase vision
- `CLAUDE.md` - Project conventions and build commands

### Source Files
- `composeApp/src/commonMain/kotlin/ai/` - All AI implementation
- `composeApp/src/commonTest/kotlin/ai/` - All AI tests
- `composeApp/src/commonMain/kotlin/core/Platform*.kt` - API key retrieval
- `composeApp/src/commonMain/kotlin/DI.kt` - Dependency injection

### External APIs
- [Anthropic API Docs](https://docs.anthropic.com/)
- [Anthropic Message API Reference](https://docs.anthropic.com/en/api/messages)
- [Tool Use Guide](https://docs.anthropic.com/en/docs/build-a-basic-ai-cli#tool-use)

---

## Maintenance Notes

### Adding a New Tool
1. Add sealed class to `Tool` in `WeatherTools.kt`
2. Implement handler method (e.g., `handleNewTool()`)
3. Add case in `handleToolCall()` dispatch
4. Write unit tests in `WeatherToolsTest.kt`
5. Update system prompt in `AiRepository.kt` if needed
6. Test with `testDebugUnitTest`

### Updating the System Prompt
Edit in `AiRepository.kt` line ~38. System prompt guides Claude's behavior and tool usage.

### Debugging Tool Calls
- Enable logs in `DI.kt`: `enableNetworkLogs = true`
- Check logcat for `WeatherTools:` and `AiRepository:` tags
- All tool calls and results are logged

### API Changes
If Anthropic API changes:
1. Update `AnthropicModels.kt` data classes
2. Update `AnthropicClient.kt` request/response handling
3. Update `AiRepository.kt` loop logic if needed
4. Run tests to catch breaking changes
