package ai

import core.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests for [AnthropicClient] — request construction (auth headers, body) and response parsing
 * for both the typed [sendMessage] path and the raw [sendRawMessage] path, plus the non-200 and
 * connection-failure error mappings to [AnthropicException]. Previously ~7% covered (KIM-325).
 *
 * Uses Ktor's [MockEngine] via the [AnthropicClient] internal HttpClient constructor seam (added
 * in KIM-325, mirroring NetworkService), so no real network I/O happens. Lives in androidUnitTest
 * (JVM) because ktor-client-mock is wired there, alongside [core.NetworkServiceTest].
 */
class AnthropicClientTest {

    private val jsonConfig = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Builds a client whose engine records the outgoing request and returns [status]/[body]. */
    private fun clientCapturing(
        status: HttpStatusCode,
        body: String,
        captured: MutableList<HttpRequestData> = mutableListOf(),
        apiKey: String = "test-key"
    ): Pair<AnthropicClient, MutableList<HttpRequestData>> {
        val engine = MockEngine { request ->
            captured.add(request)
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(jsonConfig) }
        }
        return AnthropicClient(apiKey, httpClient) to captured
    }

    private fun clientThrowing(error: Throwable, apiKey: String = "test-key"): AnthropicClient {
        val httpClient = HttpClient(MockEngine { throw error }) {
            install(ContentNegotiation) { json(jsonConfig) }
        }
        return AnthropicClient(apiKey, httpClient)
    }

    private fun sampleRequest() = AnthropicRequest(
        model = AppConstants.ANTHROPIC_MODEL,
        maxTokens = 256,
        system = "You are a helpful weather assistant",
        tools = emptyList(),
        messages = listOf(
            AnthropicMessage(role = "user", content = buildJsonObject { put("ignored", "x") }["ignored"]!!)
        )
    )

    private val successBody = """
        {
          "content": [ { "type": "text", "text": "It is windy." } ],
          "stop_reason": "end_turn",
          "usage": { "input_tokens": 12, "output_tokens": 7 }
        }
    """.trimIndent()

    // ── sendMessage: success ─────────────────────────────────────────────────

    @Test
    fun sendMessage_success_parsesResponseAndSendsAuthHeaders() = runTest {
        val (client, captured) = clientCapturing(HttpStatusCode.OK, successBody)

        val response = client.sendMessage(sampleRequest())

        assertEquals("end_turn", response.stopReason)
        assertEquals(1, response.content.size)
        assertEquals(12, response.usage?.inputTokens)
        assertEquals(7, response.usage?.outputTokens)

        // Request construction: endpoint, auth + version headers.
        val req = captured.single()
        assertEquals("https://api.anthropic.com/v1/messages", req.url.toString())
        assertEquals("test-key", req.headers["x-api-key"])
        assertEquals("2023-06-01", req.headers["anthropic-version"])
    }

    @Test
    fun sendMessage_success_contentBlocksDeserialize() = runTest {
        val (client, _) = clientCapturing(HttpStatusCode.OK, successBody)
        val blocks = client.sendMessage(sampleRequest()).getContentBlocks()
        assertEquals(1, blocks.size)
        assertTrue(blocks.single() is ContentBlock.Text)
        assertEquals("It is windy.", (blocks.single() as ContentBlock.Text).text)
    }

    // ── sendMessage: error paths ─────────────────────────────────────────────

    @Test
    fun sendMessage_non200_throwsAnthropicExceptionWithStatus() = runTest {
        val (client, _) = clientCapturing(
            HttpStatusCode.TooManyRequests, """{"error":{"message":"rate limited"}}"""
        )
        try {
            client.sendMessage(sampleRequest())
            fail("Expected AnthropicException")
        } catch (e: AnthropicException) {
            assertEquals(429, e.httpStatus)
            assertTrue(e.message?.contains("rate limited") == true, "body should be in message: ${e.message}")
        }
    }

    @Test
    fun sendMessage_connectionFailure_wrapsInAnthropicException() = runTest {
        val client = clientThrowing(IOException("connection refused"))
        try {
            client.sendMessage(sampleRequest())
            fail("Expected AnthropicException")
        } catch (e: AnthropicException) {
            assertTrue(
                e.message?.contains("Failed to communicate") == true,
                "generic failures use the wrapped message, was: ${e.message}"
            )
        }
    }

    // ── sendRawMessage: success + error ──────────────────────────────────────

    @Test
    fun sendRawMessage_success_returnsParsedJsonObject() = runTest {
        val (client, captured) = clientCapturing(HttpStatusCode.OK, """{"ok":true,"value":42}""")
        val request: JsonObject = buildJsonObject {
            put("model", AppConstants.ANTHROPIC_MODEL)
            put("max_tokens", 10)
        }

        val result = client.sendRawMessage(request)

        assertEquals("42", result["value"]?.jsonPrimitive?.content)
        assertEquals("https://api.anthropic.com/v1/messages", captured.single().url.toString())
    }

    @Test
    fun sendRawMessage_non200_throwsAnthropicException() = runTest {
        val (client, _) = clientCapturing(HttpStatusCode.Unauthorized, """{"error":"bad key"}""")
        try {
            client.sendRawMessage(buildJsonObject { put("x", 1) })
            fail("Expected AnthropicException")
        } catch (e: AnthropicException) {
            assertTrue(e.message?.contains("bad key") == true, "error body should surface: ${e.message}")
        }
    }
}
