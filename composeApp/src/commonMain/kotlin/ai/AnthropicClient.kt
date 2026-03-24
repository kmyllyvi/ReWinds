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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

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
    /**
     * Sends a raw JSON request to the Anthropic API and returns the response as a JsonObject.
     * Used for simple single-turn requests (e.g. structured data extraction) that don't
     * require the full AnthropicRequest/AnthropicResponse model.
     * @param request A JsonObject representing the full request body.
     * @return The raw JSON response as a JsonObject.
     * @throws AnthropicException if the API call fails.
     */
    suspend fun sendRawMessage(request: JsonObject): JsonObject = try {
        Log.d("AnthropicClient: sending raw message")

        val response = client.post("https://api.anthropic.com/v1/messages") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(JsonObject.serializer(), request))
        }

        Log.d("AnthropicClient: received raw response with status ${response.status}")

        if (response.status.value !in 200..299) {
            val errorBody = response.bodyAsText()
            Log.e("AnthropicClient: API error (${response.status}): $errorBody")
            throw AnthropicException("Anthropic API error ${response.status}: $errorBody")
        }

        val bodyText = response.bodyAsText()
        Json.decodeFromString(JsonObject.serializer(), bodyText)
    } catch (e: ClientRequestException) {
        val errorBody = e.response.bodyAsText()
        Log.e("AnthropicClient: API Client Error (${e.response.status}): $errorBody", e)
        throw AnthropicException("Anthropic API error: ${e.response.status} - $errorBody", e)
    } catch (e: AnthropicException) {
        throw e
    } catch (e: Exception) {
        Log.e("AnthropicClient: Generic error during raw message send: ${e.message}", e)
        throw AnthropicException("Failed to communicate with Anthropic API: ${e.message}", e)
    }

    suspend fun sendMessage(request: AnthropicRequest): AnthropicResponse = try {
        Log.d("AnthropicClient: sending message with ${request.messages.size} messages, maxTokens=${request.maxTokens}")

        val response = client.post("https://api.anthropic.com/v1/messages") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(request)  // Let Ktor handle serialization with ContentNegotiation
        }

        Log.d("AnthropicClient: received response with status ${response.status}")

        // Check for error status codes
        if (response.status.value !in 200..299) {
            val errorBody = response.bodyAsText()
            Log.e("AnthropicClient: API error (${response.status}): $errorBody")
            throw AnthropicException("Anthropic API error ${response.status}: $errorBody")
        }

        response.body()
    } catch (e: ClientRequestException) {
        val errorBody = e.response.bodyAsText()
        Log.e("AnthropicClient: API Client Error (${e.response.status}): $errorBody", e)
        throw AnthropicException("Anthropic API error: ${e.response.status} - $errorBody", e)
    } catch (e: Exception) {
        Log.e("AnthropicClient: Generic error during message send: ${e.message}", e)
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
