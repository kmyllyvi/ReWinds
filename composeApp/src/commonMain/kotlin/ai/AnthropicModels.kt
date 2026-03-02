package ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Represents different types of stop reasons from the Anthropic API.
 */
@Serializable
enum class StopReason {
    @SerialName("end_turn")
    END_TURN,

    @SerialName("tool_use")
    TOOL_USE,

    @SerialName("max_tokens")
    MAX_TOKENS;

    companion object {
        fun fromString(value: String): StopReason = when (value) {
            "end_turn" -> END_TURN
            "tool_use" -> TOOL_USE
            "max_tokens" -> MAX_TOKENS
            else -> END_TURN
        }
    }
}

/**
 * Sealed class representing different types of content blocks in messages.
 */
@Serializable
sealed class ContentBlock {
    /**
     * Text content from Claude.
     */
    @Serializable
    data class Text(
        @SerialName("type")
        val type: String = "text",
        @SerialName("text")
        val text: String
    ) : ContentBlock()

    /**
     * A tool use request from Claude.
     */
    @Serializable
    data class ToolUse(
        @SerialName("type")
        val type: String = "tool_use",
        @SerialName("id")
        val id: String,
        @SerialName("name")
        val name: String,
        @SerialName("input")
        val input: JsonObject
    ) : ContentBlock()

    /**
     * Result of a tool execution (returned by the client).
     */
    @Serializable
    data class ToolResult(
        @SerialName("type")
        val type: String = "tool_result",
        @SerialName("tool_use_id")
        val toolUseId: String,
        @SerialName("content")
        val content: String,
        @SerialName("is_error")
        val isError: Boolean = false
    ) : ContentBlock()
}

/**
 * A message in the conversation (can be from user or assistant).
 */
@Serializable
data class AnthropicMessage(
    @SerialName("role")
    val role: String, // "user" or "assistant"
    @SerialName("content")
    val content: List<AnthropicContent>
)

/**
 * Content within an AnthropicMessage.
 * Can be text or a tool use/result reference.
 */
@Serializable
sealed class AnthropicContent {
    @Serializable
    data class Text(
        @SerialName("type")
        val type: String = "text",
        @SerialName("text")
        val text: String
    ) : AnthropicContent()

    @Serializable
    data class ToolUse(
        @SerialName("type")
        val type: String = "tool_use",
        @SerialName("id")
        val id: String,
        @SerialName("name")
        val name: String,
        @SerialName("input")
        val input: JsonObject
    ) : AnthropicContent()

    @Serializable
    data class ToolResult(
        @SerialName("type")
        val type: String = "tool_result",
        @SerialName("tool_use_id")
        val toolUseId: String,
        @SerialName("content")
        val content: String,
        @SerialName("is_error")
        val isError: Boolean = false
    ) : AnthropicContent()
}

/**
 * Tool definition as expected by the Anthropic API.
 */
@Serializable
data class AnthropicTool(
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String,
    @SerialName("input_schema")
    val inputSchema: JsonObject
)

/**
 * Request to the Anthropic API.
 */
@Serializable
data class AnthropicRequest(
    @SerialName("model")
    val model: String = "claude-3-5-sonnet-20241022",
    @SerialName("max_tokens")
    val maxTokens: Int = 1024,
    @SerialName("system")
    val system: String,
    @SerialName("tools")
    val tools: List<AnthropicTool>,
    @SerialName("messages")
    val messages: List<AnthropicMessage>
)

/**
 * Response from the Anthropic API.
 */
@Serializable
data class AnthropicResponse(
    @SerialName("content")
    val content: List<ContentBlock>,
    @SerialName("stop_reason")
    val stopReason: String,
    @SerialName("usage")
    val usage: Usage? = null
)

/**
 * Token usage information from the API response.
 */
@Serializable
data class Usage(
    @SerialName("input_tokens")
    val inputTokens: Int,
    @SerialName("output_tokens")
    val outputTokens: Int
)

/**
 * Custom deserializer helper for ContentBlock to handle the polymorphic deserialization.
 */
object ContentBlockDeserializer {
    fun deserializeFromJson(jsonObject: JsonObject): ContentBlock? {
        val type = jsonObject["type"]?.let {
            if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
        } ?: return null

        return when (type) {
            "text" -> {
                val text = jsonObject["text"]?.let {
                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
                } ?: return null
                ContentBlock.Text(text = text)
            }
            "tool_use" -> {
                val id = jsonObject["id"]?.let {
                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
                } ?: return null
                val name = jsonObject["name"]?.let {
                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
                } ?: return null
                val input = jsonObject["input"] as? JsonObject ?: return null
                ContentBlock.ToolUse(id = id, name = name, input = input)
            }
            "tool_result" -> {
                val toolUseId = jsonObject["tool_use_id"]?.let {
                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
                } ?: return null
                val content = jsonObject["content"]?.let {
                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
                } ?: return null
                val isError = jsonObject["is_error"]?.let {
                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toBoolean() else false
                } ?: false
                ContentBlock.ToolResult(toolUseId = toolUseId, content = content, isError = isError)
            }
            else -> null
        }
    }
}
