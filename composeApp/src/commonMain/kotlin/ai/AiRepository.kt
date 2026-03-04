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
 * AiRepository orchestrates the agentic loop.
 * Maintains conversation history, sends requests to Claude, and handles tool calls.
 */
class AiRepository(
    private val anthropicClient: AnthropicClient,
    private val weatherTools: WeatherTools,
    private val weatherRepository: WeatherRepository
) {
    // Maintain conversation history for the session
    private val conversationHistory = mutableListOf<ConversationMessage>()

    private val systemPrompt = AppConstants.ANTHROPIC_SYSTEM_PROMPT

    /**
     * Sends a user message and orchestrates the agentic loop until a final response is obtained.
     * @param userMessage The user's input message.
     * @return An AiMessageResult containing the final response and tool call information.
     */
    suspend fun sendMessage(userMessage: String): AiMessageResult {
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
    fun clearHistory() {
        conversationHistory.clear()
        Log.d("AiRepository: conversation history cleared")
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
