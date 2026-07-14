package ai

import core.AppConstants
import core.Log
import core.WeatherRepository
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Result of a message exchange with the AI.
 * Contains the final text response and information about which tools were called.
 */
data class AiMessageResult(
    val responseText: String,
    val toolCallsMade: List<String> = emptyList(),
    val totalTurns: Int = 1
)

/**
 * Data class to represent a message in the conversation history.
 */
data class ConversationMessage(
    val role: String, // "user" or "assistant"
    val content: List<AnthropicContent>
)

/**
 * The conversation seam [ChatViewModel] depends on: send a turn and manage history.
 *
 * Extracted so the chat screen can be driven by a deterministic fake in tests (KIM-293,
 * journey J6) without a real Anthropic network call. Production binds [AiRepository].
 */
interface AiConversationRepository {
    /** Sends a user turn and returns the final assistant response (may run the agentic loop). */
    suspend fun sendMessage(userMessage: String): AiMessageResult

    /** Clears the in-memory conversation history for a fresh session. */
    fun clearHistory()

    /** Seeds the conversation history from persisted messages (e.g. on session switch/restart). */
    fun loadHistory(messages: List<ConversationMessage>)

    /**
     * Sets the system prompt used for subsequent turns. The ViewModel re-assembles this from
     * the live downloaded-months state and calls it before each send, so the prompt always
     * reflects the data currently in the DB (KIM-321). Default is a no-op for test fakes.
     */
    fun setSystemPrompt(prompt: String) {}
}

/**
 * AiRepository orchestrates the agentic loop.
 * Maintains conversation history, sends requests to Claude, and handles tool calls.
 */
class AiRepository(
    private val anthropicClient: AnthropicClient,
    private val weatherTools: WeatherTools,
    private val weatherRepository: WeatherRepository
) : AiConversationRepository {
    // Maintain conversation history for the session
    private val conversationHistory = mutableListOf<ConversationMessage>()

    // Re-assembled by the ViewModel before each send from the live downloaded-months state
    // (KIM-321). Falls back to a places-unaware base prompt until the first update.
    private var systemPrompt = AppConstants.buildAnthropicSystemPrompt(emptyMap()) { it }

    override fun setSystemPrompt(prompt: String) {
        systemPrompt = prompt
    }

    /**
     * Sends a user message and orchestrates the agentic loop until a final response is obtained.
     * @param userMessage The user's input message.
     * @return An AiMessageResult containing the final response and tool call information.
     */
    override suspend fun sendMessage(userMessage: String): AiMessageResult {
        Log.d("AiRepository: starting message exchange with user message: '$userMessage'")

        // Add user message to history
        conversationHistory.add(
            ConversationMessage(
                role = "user",
                content = listOf(
                    AnthropicContent.Text(text = userMessage)
                )
            )
        )

        var turnCount = 0
        val toolsCalled = mutableListOf<String>()

        while (true) {
            turnCount++
            Log.d("AiRepository: turn $turnCount")

            if (turnCount > AppConstants.ANTHROPIC_MAX_TURNS) {
                Log.d("AiRepository: max turns (${AppConstants.ANTHROPIC_MAX_TURNS}) reached, stopping loop")
                return AiMessageResult(
                    responseText = "Error: Conversation took too many turns. Please try again.",
                    toolCallsMade = toolsCalled,
                    totalTurns = turnCount
                )
            }

            // Build the request with current conversation history
            val request = buildAnthropicRequest()

            // Send to API
            val response = try {
                anthropicClient.sendMessage(request)
            } catch (e: Exception) {
                Log.e("AiRepository: error sending message to Anthropic", e)
                return AiMessageResult(
                    responseText = "Error: Failed to communicate with AI assistant: ${e.message}",
                    toolCallsMade = toolsCalled,
                    totalTurns = turnCount
                )
            }

            Log.d("AiRepository: received response with stop_reason: ${response.stopReason}")

            // Process the response content
            val assistantContent = mutableListOf<AnthropicContent>()
            var hasToolUse = false

            for (contentBlock in response.getContentBlocks()) {
                when (contentBlock) {
                    is ContentBlock.Text -> {
                        Log.d("AiRepository: received text response: ${contentBlock.text.take(100)}...")
                        assistantContent.add(
                            AnthropicContent.Text(text = contentBlock.text)
                        )
                    }
                    is ContentBlock.ToolUse -> {
                        Log.d("AiRepository: received tool use request: ${contentBlock.name}")
                        assistantContent.add(
                            AnthropicContent.ToolUse(
                                id = contentBlock.id,
                                name = contentBlock.name,
                                input = contentBlock.input
                            )
                        )
                        hasToolUse = true
                    }
                    is ContentBlock.ToolResult -> {
                        // This shouldn't happen in the response, but handle it gracefully
                        Log.d("AiRepository: unexpected ToolResult in response content")
                    }
                }
            }

            // Add assistant message to history
            conversationHistory.add(
                ConversationMessage(
                    role = "assistant",
                    content = assistantContent
                )
            )

            // Check if we should continue the loop
            val stopReason = StopReason.fromString(response.stopReason)

            when (stopReason) {
                StopReason.END_TURN -> {
                    // Extract the final text response
                    val finalText = assistantContent
                        .filterIsInstance<AnthropicContent.Text>()
                        .joinToString("\n") { it.text }

                    Log.d("AiRepository: conversation ended, returning final response")
                    return AiMessageResult(
                        responseText = finalText.ifBlank { "No response generated." },
                        toolCallsMade = toolsCalled,
                        totalTurns = turnCount
                    )
                }
                StopReason.TOOL_USE -> {
                    if (!hasToolUse) {
                        Log.d("AiRepository: stop_reason is TOOL_USE but no tool calls found")
                        return AiMessageResult(
                            responseText = "Error: Unexpected tool use state.",
                            toolCallsMade = toolsCalled,
                            totalTurns = turnCount
                        )
                    }

                    // Execute all tool uses and collect results
                    val toolResults = mutableListOf<AnthropicContent.ToolResult>()

                    for (content in assistantContent) {
                        if (content is AnthropicContent.ToolUse) {
                            Log.d("AiRepository: executing tool: ${content.name}")
                            toolsCalled.add(content.name)

                            try {
                                val result = weatherTools.handleToolCall(
                                    content.name,
                                    content.input,
                                    weatherRepository
                                )

                                toolResults.add(
                                    AnthropicContent.ToolResult(
                                        toolUseId = content.id,
                                        content = result,
                                        isError = false
                                    )
                                )
                                Log.d("AiRepository: tool '${content.name}' executed successfully")
                            } catch (e: Exception) {
                                Log.e("AiRepository: tool execution failed for '${content.name}'", e)
                                toolResults.add(
                                    AnthropicContent.ToolResult(
                                        toolUseId = content.id,
                                        content = """{"error": "${e.message}", "tool": "${content.name}"}""",
                                        isError = true
                                    )
                                )
                            }
                        }
                    }

                    // Add tool results to history
                    if (toolResults.isNotEmpty()) {
                        conversationHistory.add(
                            ConversationMessage(
                                role = "user",
                                content = toolResults
                            )
                        )
                        Log.d("AiRepository: added ${toolResults.size} tool results to history")
                    }

                    // Loop continues to next iteration
                }
                StopReason.MAX_TOKENS -> {
                    Log.d("AiRepository: max tokens reached")

                    // A max_tokens cutoff can land mid tool_use: the assistant message we appended
                    // above may carry a tool_use block that will never be executed. Anthropic rejects
                    // any history where a tool_use has no paired tool_result on the following turn
                    // (HTTP 400), which corrupts every subsequent send. Synthesize an error result for
                    // each dangling call — the same history shaping the TOOL_USE branch does, minus the
                    // execution (Claude never finished specifying the call).
                    val danglingToolResults = assistantContent
                        .filterIsInstance<AnthropicContent.ToolUse>()
                        .map { toolUse ->
                            AnthropicContent.ToolResult(
                                toolUseId = toolUse.id,
                                content = "Response was cut off before this tool call could complete.",
                                isError = true
                            )
                        }
                    if (danglingToolResults.isNotEmpty()) {
                        conversationHistory.add(
                            ConversationMessage(
                                role = "user",
                                content = danglingToolResults
                            )
                        )
                        Log.d("AiRepository: synthesized ${danglingToolResults.size} tool results for max_tokens cutoff")
                    }

                    val textContent = assistantContent
                        .filterIsInstance<AnthropicContent.Text>()
                        .joinToString("\n") { it.text }
                    return AiMessageResult(
                        responseText = (textContent.ifBlank { "Response generation was cut off. " }) +
                                "Please ask your question again to continue.",
                        toolCallsMade = toolsCalled,
                        totalTurns = turnCount
                    )
                }
            }
        }
    }

    /**
     * Clears the conversation history for a fresh start.
     */
    override fun clearHistory() {
        conversationHistory.clear()
        Log.d("AiRepository: conversation history cleared")
    }

    /**
     * Seeds the conversation history from persisted messages (e.g. on app restart).
     */
    override fun loadHistory(messages: List<ConversationMessage>) {
        conversationHistory.clear()
        conversationHistory.addAll(messages)
        Log.d("AiRepository: loaded ${messages.size} messages from persistence")
    }

    /**
     * Returns a copy of the current conversation history.
     */
    fun getHistory(): List<ConversationMessage> = conversationHistory.toList()

    /**
     * Builds an AnthropicRequest from the current conversation history.
     */
    private fun buildAnthropicRequest(): AnthropicRequest {
        val messages = conversationHistory.map { msg ->
            // Build content as a JSON array
            val contentArray = kotlinx.serialization.json.buildJsonArray {
                msg.content.forEach { content ->
                    when (content) {
                        is AnthropicContent.Text -> add(AnthropicContentSerializer.serializeToJson(content))
                        is AnthropicContent.ToolUse -> add(AnthropicContentSerializer.serializeToJson(content))
                        is AnthropicContent.ToolResult -> add(AnthropicContentSerializer.serializeToJson(content))
                    }
                }
            }

            AnthropicMessage(
                role = msg.role,
                content = contentArray
            )
        }

        val tools = weatherTools.allToolSchemas().map { tool ->
            AnthropicTool(
                name = tool.name,
                description = tool.description,
                inputSchema = tool.inputSchema
            )
        }

        return AnthropicRequest(
            model = AppConstants.ANTHROPIC_MODEL,
            maxTokens = 1024,
            system = systemPrompt,
            tools = tools,
            messages = messages
        )
    }
}
