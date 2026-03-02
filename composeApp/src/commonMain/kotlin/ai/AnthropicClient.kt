package ai

import core.Log
import core.httpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Ktor HTTP client for communicating with the Anthropic API.
 * Handles authentication and request/response serialization.
 */
class AnthropicClient(
    private val apiKey: String,
    private val enableLogs: Boolean = false
) {
    private val client: HttpClient = httpClient(enableLogs)

    /**
     * Sends a message request to the Anthropic API and returns the response.
     * @param request The AnthropicRequest containing the message and tools.
     * @return The AnthropicResponse from the API.
     * @throws Exception if the API call fails.
     */
    suspend fun sendMessage(request: AnthropicRequest): AnthropicResponse = try {
        Log.d("AnthropicClient: sending message with ${request.messages.size} messages and ${request.tools.size} tools")

        val response = client.post("https://api.anthropic.com/v1/messages") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        Log.d("AnthropicClient: received response with status ${response.status}")
        response.body()
    } catch (e: ClientRequestException) {
        val errorBody = e.response.bodyAsText()
        Log.e("AnthropicClient: API Client Error: $errorBody", e)
        throw AnthropicException("Anthropic API error: ${e.response.status}", e)
    } catch (e: Exception) {
        Log.e("AnthropicClient: Generic error during message send", e)
        throw AnthropicException("Failed to communicate with Anthropic API: ${e.message}", e)
    }
}

/**
 * Exception thrown when Anthropic API communication fails.
 */
class AnthropicException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
