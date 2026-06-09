# Anthropic API Interface

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. -->

**Last updated:** 2026-06-09 (bootstrap)
**Status:** Active

---

## Overview

The contract between the app and the Anthropic `/v1/messages` endpoint. All types are defined in `ai/AnthropicModels.kt`; the client is `ai/AnthropicClient.kt`.

---

## Request models (`AnthropicRequest`)

```kotlin
@Serializable
data class AnthropicRequest(
    val model: String,          // AppConstants.ANTHROPIC_MODEL = "claude-haiku-4-5"
    val maxTokens: Int,         // 1024 (hard-coded at call site in AiRepository)
    val system: String,         // AppConstants.ANTHROPIC_SYSTEM_PROMPT
    val tools: List<AnthropicTool>,
    val messages: List<AnthropicMessage>
)

data class AnthropicMessage(val role: String, val content: JsonArray)
data class AnthropicTool(val name: String, val description: String, val inputSchema: JsonObject)
```

## Response models (`AnthropicResponse`)

```kotlin
// AnthropicResponse.getContentBlocks(): List<ContentBlock>
sealed class ContentBlock {
    data class Text(val text: String)
    data class ToolUse(val id: String, val name: String, val input: JsonObject)
    data class ToolResult(...)     // not expected in API response; handled defensively
}
// stopReason: "end_turn" | "tool_use" | "max_tokens"
```

## Content building (`AnthropicContent`)

Used to construct `ConversationMessage.content` in the conversation history:

```kotlin
sealed class AnthropicContent {
    data class Text(val text: String)
    data class ToolUse(val id: String, val name: String, val input: JsonObject)
    data class ToolResult(val toolUseId: String, val content: String, val isError: Boolean)
}
```
Serialized to JSON via `AnthropicContentSerializer.serializeToJson(content)`.

## Error type

```kotlin
class AnthropicException(
    message: String,
    cause: Throwable? = null,
    val httpStatus: Int? = null    // populated for HTTP-level errors (e.g. 401, 403)
) : Exception
```

---

## API constants (`AppConstants`)

| Constant | Value |
|---|---|
| `ANTHROPIC_MODEL` | `claude-haiku-4-5` |
| `ANTHROPIC_API_VERSION` | `2023-06-01` |
| `ANTHROPIC_API_BASE_URL` | `https://api.anthropic.com/v1/` |
| `ANTHROPIC_MAX_TOKENS` | `1024` |
| `ANTHROPIC_MAX_TURNS` | `20` |

**Note:** `ANTHROPIC_MAX_TOKENS` is defined in `AppConstants` but the `AiRepository.buildAnthropicRequest()` hard-codes `maxTokens = 1024` directly. These are currently in sync but the constant is not used at the call site.

---

## System prompt

The system prompt identifies the assistant as a wind sports analyst focused on kite/windsurfing, instructs it to use tools for data access, and directs it to be concise. Defined in `AppConstants.ANTHROPIC_SYSTEM_PROMPT`.

---

## Tool schemas

Five tools are registered per request (order matters — `get_weather_metrics` is listed first):

| Tool | Required args | Optional args |
|---|---|---|
| `get_weather_metrics` | `location_name`, `start_date`, `end_date`, `metrics[]` | — |
| `get_wind_summary` | `location_name`, `start_date`, `end_date` | — |
| `list_saved_places` | — | — |
| `get_monthly_stats` | `location_name`, `year`, `month` | — |
| `get_best_days` | `location_name`, `start_date`, `end_date` | `min_wind_speed`, `max_wind_speed`, `max_gust`, `no_rain` |

Wind speed parameters in `get_best_days` are in knots. The tool handler converts them to m/s for comparison against the database.
