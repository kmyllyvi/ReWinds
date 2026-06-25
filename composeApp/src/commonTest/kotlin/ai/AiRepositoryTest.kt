package ai

import core.WeatherRepository
import core.WeatherResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Simple data structure tests for AI Repository components.
 * Integration tests with async would need a full test framework setup.
 */
class AiRepositoryTest {

    @Test
    fun testConversationMessageCreation() {
        val content = listOf<AnthropicContent>(
            AnthropicContent.Text(text = "What is the wind speed?")
        )
        val message = ConversationMessage(
            role = "user",
            content = content
        )

        assertEquals("user", message.role)
        assertEquals(1, message.content.size)
    }

    @Test
    fun testAiMessageResultStructure() {
        val result = AiMessageResult(
            responseText = "The wind is 15 knots.",
            toolCallsMade = listOf("get_wind_summary"),
            totalTurns = 2
        )

        assertEquals("The wind is 15 knots.", result.responseText)
        assertEquals(1, result.toolCallsMade.size)
        assertEquals(2, result.totalTurns)
    }

    @Test
    fun testStopReasonEnum() {
        val endTurn = StopReason.fromString("end_turn")
        val toolUse = StopReason.fromString("tool_use")
        val maxTokens = StopReason.fromString("max_tokens")

        assertEquals(StopReason.END_TURN, endTurn)
        assertEquals(StopReason.TOOL_USE, toolUse)
        assertEquals(StopReason.MAX_TOKENS, maxTokens)
    }

    @Test
    fun testContentBlockDeserialization() {
        val textBlock = ContentBlockDeserializer.deserializeFromJson(
            kotlinx.serialization.json.buildJsonObject {
                put("type", kotlinx.serialization.json.JsonPrimitive("text"))
                put("text", kotlinx.serialization.json.JsonPrimitive("Hello"))
            }
        )

        assertNotNull(textBlock)
        assertIs<ContentBlock.Text>(textBlock)
        assertEquals("Hello", textBlock.text)
    }

    @Test
    fun testMultipleToolResults() {
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
    }

    @Test
    fun testWeatherToolsIntegration() {
        // Test that WeatherTools can be instantiated and schemas accessed
        val tools = WeatherTools
        val schemas = tools.allToolSchemas()

        assertEquals(5, schemas.size)
        assertEquals("get_weather_metrics", schemas[0].name)
        assertEquals("get_wind_summary", schemas[1].name)
        assertEquals("list_saved_places", schemas[2].name)
        assertEquals("get_monthly_stats", schemas[3].name)
        assertEquals("get_best_days", schemas[4].name)
    }

    @Test
    fun testTestWeatherRepositoryFactory() {
        val testDays = TestWeatherRepositoryFactory.generateTestDays("2025-01-01", "2025-01-05")
        assertEquals(5, testDays.size)

        val firstDay = testDays.first()
        assertEquals("2025-01-01", firstDay.datetime)
        assertEquals(10.0, firstDay.windspeed)
        assertEquals(12.0, firstDay.windgust)
    }

    @Test
    fun testTestWeatherResponseCreation() {
        val testDays = TestWeatherRepositoryFactory.generateTestDays("2025-01-01", "2025-01-03")
        val response = TestWeatherRepositoryFactory.createWeatherResponse(
            place = "Tarifa",
            days = testDays
        )

        assertNotNull(response)
        assertEquals("Tarifa", response.resolvedAddress)
        assertEquals(3, response.days?.size ?: 0)
    }
}
