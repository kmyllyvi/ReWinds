package ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.addJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for Anthropic API data models.
 * Focus on structure and basic serialization/deserialization.
 */
class AnthropicModelsTest {

    @Test
    fun testStopReasonDeserialization() {
        assertEquals(StopReason.END_TURN, StopReason.fromString("end_turn"))
        assertEquals(StopReason.TOOL_USE, StopReason.fromString("tool_use"))
        assertEquals(StopReason.MAX_TOKENS, StopReason.fromString("max_tokens"))
        assertEquals(StopReason.END_TURN, StopReason.fromString("unknown"))
    }

    @Test
    fun testContentBlockTextDeserialization() {
        val json = buildJsonObject {
            put("type", JsonPrimitive("text"))
            put("text", JsonPrimitive("Hello, world!"))
        }

        val block = ContentBlockDeserializer.deserializeFromJson(json)
        assertNotNull(block)
        assert(block is ContentBlock.Text)
        assertEquals("Hello, world!", (block as ContentBlock.Text).text)
    }

    @Test
    fun testContentBlockToolUseDeserialization() {
        val inputSchema = buildJsonObject {
            put("type", JsonPrimitive("object"))
            putJsonObject("properties") {
                putJsonObject("param1") {
                    put("type", JsonPrimitive("string"))
                }
            }
        }

        val json = buildJsonObject {
            put("type", JsonPrimitive("tool_use"))
            put("id", JsonPrimitive("tool-123"))
            put("name", JsonPrimitive("get_wind_summary"))
            put("input", inputSchema)
        }

        val block = ContentBlockDeserializer.deserializeFromJson(json)
        assertNotNull(block)
        assert(block is ContentBlock.ToolUse)
        val toolUse = block as ContentBlock.ToolUse
        assertEquals("tool-123", toolUse.id)
        assertEquals("get_wind_summary", toolUse.name)
        assertNotNull(toolUse.input)
    }

    @Test
    fun testContentBlockToolResultDeserialization() {
        val json = buildJsonObject {
            put("type", JsonPrimitive("tool_result"))
            put("tool_use_id", JsonPrimitive("tool-123"))
            put("content", JsonPrimitive("Result data here"))
            put("is_error", JsonPrimitive(false))
        }

        val block = ContentBlockDeserializer.deserializeFromJson(json)
        assertNotNull(block)
        assert(block is ContentBlock.ToolResult)
        val result = block as ContentBlock.ToolResult
        assertEquals("tool-123", result.toolUseId)
        assertEquals("Result data here", result.content)
        assertEquals(false, result.isError)
    }

    @Test
    fun testUsageDataStructure() {
        val usage = Usage(
            inputTokens = 100,
            outputTokens = 50
        )

        assertEquals(100, usage.inputTokens)
        assertEquals(50, usage.outputTokens)
    }

    @Test
    fun testAnthropicToolStructure() {
        val tool = AnthropicTool(
            name = "get_wind_summary",
            description = "Get wind data",
            inputSchema = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("location") {
                        put("type", JsonPrimitive("string"))
                    }
                }
            }
        )

        assertEquals("get_wind_summary", tool.name)
        assertEquals("Get wind data", tool.description)
        assertNotNull(tool.inputSchema)
    }

    @Test
    fun testAnthropicRequestStructure() {
        val request = AnthropicRequest(
            model = "claude-3-5-sonnet-20241022",
            maxTokens = 1024,
            system = "You are helpful",
            tools = emptyList(),
            messages = emptyList()
        )

        assertEquals("claude-3-5-sonnet-20241022", request.model)
        assertEquals(1024, request.maxTokens)
        assertEquals("You are helpful", request.system)
        assertEquals(0, request.tools.size)
        assertEquals(0, request.messages.size)
    }

    @Test
    fun testStopReasonEnum() {
        val endTurn = StopReason.END_TURN
        val toolUse = StopReason.TOOL_USE
        val maxTokens = StopReason.MAX_TOKENS

        assertNotNull(endTurn)
        assertNotNull(toolUse)
        assertNotNull(maxTokens)
    }

    @Test
    fun testContentBlockTextStructure() {
        val content = ContentBlock.Text(text = "Test message")
        assertEquals("text", content.type)
        assertEquals("Test message", content.text)
    }

    @Test
    fun testContentBlockToolUseStructure() {
        val inputSchema = buildJsonObject {
            put("type", JsonPrimitive("object"))
        }

        val content = ContentBlock.ToolUse(
            id = "tool-1",
            name = "get_wind_summary",
            input = inputSchema
        )

        assertEquals("tool_use", content.type)
        assertEquals("tool-1", content.id)
        assertEquals("get_wind_summary", content.name)
        assertNotNull(content.input)
    }

    @Test
    fun testContentBlockToolResultStructure() {
        val content = ContentBlock.ToolResult(
            toolUseId = "tool-1",
            content = "Result data",
            isError = false
        )

        assertEquals("tool_result", content.type)
        assertEquals("tool-1", content.toolUseId)
        assertEquals("Result data", content.content)
        assertEquals(false, content.isError)
    }

    @Test
    fun testContentBlockToolResultWithErrorStructure() {
        val content = ContentBlock.ToolResult(
            toolUseId = "tool-1",
            content = "Error message",
            isError = true
        )

        assertEquals("tool_result", content.type)
        assertEquals("tool-1", content.toolUseId)
        assertEquals("Error message", content.content)
        assertEquals(true, content.isError)
    }

    @Test
    fun testAnthropicMessageStructure() {
        val contentJson = buildJsonObject {
            putJsonArray("content") {
                addJsonObject {
                    put("type", "text")
                    put("text", "Hello")
                }
            }
        }

        val message = AnthropicMessage(
            role = "user",
            content = contentJson["content"]!!
        )

        assertEquals("user", message.role)
        assertEquals(1, message.content.jsonArray.size)
    }

    @Test
    fun testAnthropicContentTextStructure() {
        val content = AnthropicContent.Text(text = "Test message")
        assertEquals("text", content.type)
        assertEquals("Test message", content.text)
    }

    @Test
    fun testAnthropicContentToolUseStructure() {
        val inputSchema = buildJsonObject {
            put("type", JsonPrimitive("object"))
        }

        val content = AnthropicContent.ToolUse(
            id = "tool-1",
            name = "get_wind_summary",
            input = inputSchema
        )

        assertEquals("tool_use", content.type)
        assertEquals("tool-1", content.id)
        assertEquals("get_wind_summary", content.name)
        assertNotNull(content.input)
    }

    @Test
    fun testAnthropicContentToolResultStructure() {
        val content = AnthropicContent.ToolResult(
            toolUseId = "tool-1",
            content = "Result data",
            isError = false
        )

        assertEquals("tool_result", content.type)
        assertEquals("tool-1", content.toolUseId)
        assertEquals("Result data", content.content)
        assertEquals(false, content.isError)
    }

    @Test
    fun testMultipleToolResultsStructure() {
        val results = listOf(
            AnthropicContent.ToolResult(
                toolUseId = "call_1",
                content = """{"places": ["Tarifa"]}""",
                isError = false
            ),
            AnthropicContent.ToolResult(
                toolUseId = "call_2",
                content = """{"stats": {"wind": 15}}""",
                isError = false
            )
        )

        assertEquals(2, results.size)
        assertEquals("call_1", results[0].toolUseId)
        assertEquals("call_2", results[1].toolUseId)
        assertEquals(false, results[0].isError)
        assertEquals(false, results[1].isError)
    }
}
