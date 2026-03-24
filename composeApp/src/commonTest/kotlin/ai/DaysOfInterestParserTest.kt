package ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for DaysOfInterestParser JSON extraction logic.
 *
 * Tests the stripCodeFences() helper that sanitizes Claude's response
 * before JSON parsing — Claude sometimes wraps output in markdown code
 * blocks despite being instructed not to.
 */
class DaysOfInterestParserTest {

    @Test
    fun testPlainJsonPassesThrough() {
        val input = """{"minTempC": 2.0, "minWindSpeedKmh": 22.2}"""
        assertEquals(input, DaysOfInterestParser.stripCodeFences(input))
    }

    @Test
    fun testJsonFenceStripped() {
        val input = "```json\n{\"minTempC\": 2.0}\n```"
        assertEquals("{\"minTempC\": 2.0}", DaysOfInterestParser.stripCodeFences(input))
    }

    @Test
    fun testGenericFenceStripped() {
        val input = "```\n{\"noRain\": true}\n```"
        assertEquals("{\"noRain\": true}", DaysOfInterestParser.stripCodeFences(input))
    }

    @Test
    fun testFenceWithLeadingTrailingWhitespace() {
        val input = "  ```json\n{\"sustainedWindHours\": 3}\n```  "
        assertEquals("{\"sustainedWindHours\": 3}", DaysOfInterestParser.stripCodeFences(input))
    }

    @Test
    fun testParseJsonFromFencedResponse() {
        // Simulates the actual failure: Claude returns fenced JSON
        val fencedResponse = """
            ```json
            {
              "minTempC": 2.0,
              "minWindSpeedKmh": 22.224,
              "windDirections": ["S", "SW", "W"],
              "sustainedWindHours": 3
            }
            ```
        """.trimIndent()

        val cleaned = DaysOfInterestParser.stripCodeFences(fencedResponse)
        val filter = DaysOfInterestParser.parseJson("my criteria", cleaned)

        assertEquals(2.0, filter.minTempC)
        assertEquals(22.224, filter.minWindSpeedKmh)
        assertEquals(listOf("S", "SW", "W"), filter.windDirections)
        assertEquals(3, filter.sustainedWindHours)
        assertEquals("my criteria", filter.naturalLanguageCriteria)
    }

    @Test
    fun testParseJsonAllNullFields() {
        val json = """{}"""
        val filter = DaysOfInterestParser.parseJson("windy days", json)
        assertNotNull(filter)
        assertEquals("windy days", filter.naturalLanguageCriteria)
        assertEquals(null, filter.minTempC)
        assertEquals(null, filter.minWindSpeedKmh)
    }
}
