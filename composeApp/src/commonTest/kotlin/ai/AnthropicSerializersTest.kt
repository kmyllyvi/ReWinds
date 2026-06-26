package ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Round-trip (de)serialization tests for the Anthropic wire models (KIM-325 AC). The existing
 * AnthropicModelsTest covers data-class construction and the manual deserializers; this focuses
 * on serialize → parse → equal round trips through kotlinx.serialization, including the
 * @SerialName mappings (snake_case wire names) and optional/nullable fields (e.g. Usage,
 * tool_result.is_error) being preserved or correctly defaulted.
 */
class AnthropicSerializersTest {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    // ── AnthropicTool ────────────────────────────────────────────────────────

    @Test
    fun anthropicTool_roundTrips_withSerialNameInputSchema() {
        val tool = AnthropicTool(
            name = "get_wind_summary",
            description = "Get wind data",
            inputSchema = buildJsonObject { put("type", "object") }
        )

        val encoded = json.encodeToString(AnthropicTool.serializer(), tool)
        // Wire name is snake_case per @SerialName.
        assertTrue(encoded.contains("\"input_schema\""), "must serialize as input_schema, was: $encoded")

        val decoded = json.decodeFromString(AnthropicTool.serializer(), encoded)
        assertEquals(tool, decoded)
    }

    // ── AnthropicRequest ─────────────────────────────────────────────────────

    @Test
    fun anthropicRequest_roundTrips_withSnakeCaseFields() {
        val request = AnthropicRequest(
            model = "claude-haiku-4-5",
            maxTokens = 512,
            system = "be concise",
            tools = listOf(
                AnthropicTool("t1", "d1", buildJsonObject { put("type", "object") })
            ),
            messages = listOf(
                AnthropicMessage(
                    role = "user",
                    content = buildJsonObject {
                        put("text", "hello")
                    }["text"]!!
                )
            )
        )

        val encoded = json.encodeToString(AnthropicRequest.serializer(), request)
        assertTrue(encoded.contains("\"max_tokens\""), "maxTokens must map to max_tokens")

        val decoded = json.decodeFromString(AnthropicRequest.serializer(), encoded)
        assertEquals(request.model, decoded.model)
        assertEquals(512, decoded.maxTokens)
        assertEquals("be concise", decoded.system)
        assertEquals(1, decoded.tools.size)
        assertEquals(1, decoded.messages.size)
        assertEquals("user", decoded.messages.single().role)
    }

    @Test
    fun anthropicRequest_appliesModelAndMaxTokenDefaults_whenAbsentFromJson() {
        // Optional fields with defaults must be filled from the JSON's omitted values.
        val partial = """{"system":"hi","tools":[],"messages":[]}"""
        val decoded = json.decodeFromString(AnthropicRequest.serializer(), partial)
        assertEquals(core.AppConstants.ANTHROPIC_MODEL, decoded.model)
        assertEquals(core.AppConstants.ANTHROPIC_MAX_TOKENS, decoded.maxTokens)
    }

    // ── AnthropicResponse + Usage (nullable) ─────────────────────────────────

    @Test
    fun anthropicResponse_roundTrips_withUsage() {
        val wire = """
            {
              "content": [ { "type": "text", "text": "windy" } ],
              "stop_reason": "end_turn",
              "usage": { "input_tokens": 10, "output_tokens": 5 }
            }
        """.trimIndent()

        val decoded = json.decodeFromString(AnthropicResponse.serializer(), wire)
        assertEquals("end_turn", decoded.stopReason)
        assertEquals(10, decoded.usage?.inputTokens)
        assertEquals(5, decoded.usage?.outputTokens)

        // Re-encode and re-decode: stop_reason + usage survive the round trip.
        val reencoded = json.encodeToString(AnthropicResponse.serializer(), decoded)
        assertTrue(reencoded.contains("\"stop_reason\""))
        val redecoded = json.decodeFromString(AnthropicResponse.serializer(), reencoded)
        assertEquals(decoded.stopReason, redecoded.stopReason)
        assertEquals(decoded.usage, redecoded.usage)
    }

    @Test
    fun anthropicResponse_parses_withNullUsage() {
        // usage is nullable and may be absent entirely.
        val wire = """{"content":[],"stop_reason":"tool_use"}"""
        val decoded = json.decodeFromString(AnthropicResponse.serializer(), wire)
        assertEquals("tool_use", decoded.stopReason)
        assertNull(decoded.usage)
        assertTrue(decoded.content.isEmpty())
    }

    @Test
    fun anthropicResponse_getContentBlocks_deserializesMixedContent() {
        val wire = """
            {
              "content": [
                { "type": "text", "text": "let me check" },
                { "type": "tool_use", "id": "tu_1", "name": "get_wind_summary", "input": { "place": "Tarifa" } }
              ],
              "stop_reason": "tool_use"
            }
        """.trimIndent()

        val blocks = json.decodeFromString(AnthropicResponse.serializer(), wire).getContentBlocks()
        assertEquals(2, blocks.size)
        assertIs<ContentBlock.Text>(blocks[0])
        val toolUse = assertIs<ContentBlock.ToolUse>(blocks[1])
        assertEquals("tu_1", toolUse.id)
        assertEquals("get_wind_summary", toolUse.name)
        assertEquals("Tarifa", toolUse.input["place"]?.jsonPrimitive?.content)
    }

    // ── Usage ────────────────────────────────────────────────────────────────

    @Test
    fun usage_roundTrips_withSerialNames() {
        val usage = Usage(inputTokens = 100, outputTokens = 50)
        val encoded = json.encodeToString(Usage.serializer(), usage)
        assertTrue(encoded.contains("\"input_tokens\""))
        assertTrue(encoded.contains("\"output_tokens\""))
        assertEquals(usage, json.decodeFromString(Usage.serializer(), encoded))
    }

    // ── AnthropicContentSerializer ↔ ContentBlockDeserializer round trips ─────

    @Test
    fun anthropicContentText_serializesAndDeserializesBack() {
        val original = AnthropicContent.Text(text = "hello")
        val jsonObj: JsonObject = AnthropicContentSerializer.serializeToJson(original)
        assertEquals("text", jsonObj["type"]?.jsonPrimitive?.content)

        val block = ContentBlockDeserializer.deserializeFromJson(jsonObj)
        assertIs<ContentBlock.Text>(block)
        assertEquals("hello", block.text)
    }

    @Test
    fun anthropicContentToolUse_serializesPreservingInputObject() {
        val original = AnthropicContent.ToolUse(
            id = "tu_9",
            name = "get_best_days",
            input = buildJsonObject { put("place", "Tarifa") }
        )
        val jsonObj = AnthropicContentSerializer.serializeToJson(original)
        assertEquals("tool_use", jsonObj["type"]?.jsonPrimitive?.content)
        assertEquals("Tarifa", jsonObj["input"]?.jsonObject?.get("place")?.jsonPrimitive?.content)

        val block = ContentBlockDeserializer.deserializeFromJson(jsonObj)
        val toolUse = assertIs<ContentBlock.ToolUse>(block)
        assertEquals("tu_9", toolUse.id)
        assertEquals("get_best_days", toolUse.name)
    }

    @Test
    fun anthropicContentToolResult_roundTripsIsErrorFlag() {
        val original = AnthropicContent.ToolResult(
            toolUseId = "tu_3",
            content = "boom",
            isError = true
        )
        val jsonObj = AnthropicContentSerializer.serializeToJson(original)
        assertEquals("tool_result", jsonObj["type"]?.jsonPrimitive?.content)
        assertEquals("tu_3", jsonObj["tool_use_id"]?.jsonPrimitive?.content)
        assertEquals(true, jsonObj["is_error"]?.jsonPrimitive?.content?.toBoolean())

        val block = ContentBlockDeserializer.deserializeFromJson(jsonObj)
        val result = assertIs<ContentBlock.ToolResult>(block)
        assertEquals("tu_3", result.toolUseId)
        assertEquals("boom", result.content)
        assertTrue(result.isError)
    }

    @Test
    fun contentBlockDeserializer_toolResult_defaultsIsErrorFalseWhenAbsent() {
        val obj = buildJsonObject {
            put("type", JsonPrimitive("tool_result"))
            put("tool_use_id", JsonPrimitive("tu_4"))
            put("content", JsonPrimitive("ok"))
        }
        val block = ContentBlockDeserializer.deserializeFromJson(obj)
        val result = assertIs<ContentBlock.ToolResult>(block)
        assertEquals(false, result.isError, "is_error must default to false when omitted")
    }

    @Test
    fun anthropicMessage_roundTrips_withJsonArrayContent() {
        val contentArray = buildJsonObject {
            put("content", json.parseToJsonElement("""[{"type":"text","text":"hi"}]"""))
        }["content"]!!
        val message = AnthropicMessage(role = "assistant", content = contentArray)

        val encoded = json.encodeToString(AnthropicMessage.serializer(), message)
        val decoded = json.decodeFromString(AnthropicMessage.serializer(), encoded)
        assertEquals("assistant", decoded.role)
        assertEquals(1, decoded.content.jsonArray.size)
    }
}
