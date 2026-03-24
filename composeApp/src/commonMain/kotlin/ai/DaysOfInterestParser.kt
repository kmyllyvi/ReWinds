package ai

import core.DaysOfInterestFilter
import core.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Parses a natural language "days of interest" criteria string into a
 * structured [DaysOfInterestFilter] using the Anthropic API.
 *
 * This is a one-time call triggered when the user saves new filter criteria.
 * Claude returns a JSON object only — no tools are involved.
 */
object DaysOfInterestParser {

    private val json = Json { ignoreUnknownKeys = true }

    private val systemPrompt = """
        You are a structured data extractor. The user will describe criteria for a "day of interest"
        in natural language. Extract the criteria into JSON with these optional fields:

        {
          "minTempC": number | null,
          "maxTempC": number | null,
          "minWindSpeedKmh": number | null,
          "maxWindSpeedKmh": number | null,
          "windDirections": string[] | null,
          "sustainedWindHours": number | null,
          "daylightOnly": boolean | null,
          "noRain": boolean | null,
          "maxCloudCoverPct": number | null
        }

        windDirections values must be compass points: N, NE, E, SE, S, SW, W, NW.
        Convert all wind speeds to km/h. Convert knots: 1 kn = 1.852 km/h.
        Reply with ONLY the JSON object, no explanation, no markdown code blocks.
    """.trimIndent()

    /**
     * Calls the Anthropic API to parse [criteria] into a [DaysOfInterestFilter].
     * @throws Exception if the API call fails or the response cannot be parsed.
     */
    suspend fun parse(criteria: String, client: AnthropicClient): DaysOfInterestFilter {
        Log.d("DaysOfInterestParser: parsing criteria: '$criteria'")

        val userMessageContent = buildJsonArray {
            add(buildJsonObject {
                put("type", "text")
                put("text", criteria)
            })
        }

        val request = buildJsonObject {
            put("model", "claude-haiku-4-5")
            put("max_tokens", 512)
            put("system", systemPrompt)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", userMessageContent)
                })
            })
        }

        val response = client.sendRawMessage(request)
        val jsonText = extractTextFromResponse(response)

        Log.d("DaysOfInterestParser: received JSON: $jsonText")

        val extracted = json.decodeFromString<ExtractedFilter>(jsonText)
        return DaysOfInterestFilter(
            naturalLanguageCriteria = criteria,
            minTempC = extracted.minTempC,
            maxTempC = extracted.maxTempC,
            minWindSpeedKmh = extracted.minWindSpeedKmh,
            maxWindSpeedKmh = extracted.maxWindSpeedKmh,
            windDirections = extracted.windDirections,
            sustainedWindHours = extracted.sustainedWindHours,
            daylightOnly = extracted.daylightOnly,
            noRain = extracted.noRain,
            maxCloudCoverPct = extracted.maxCloudCoverPct
        )
    }

    private fun extractTextFromResponse(response: JsonObject): String {
        val content = response["content"] as? JsonArray
            ?: throw AnthropicException("No content array in response")
        val firstBlock = content.firstOrNull() as? JsonObject
            ?: throw AnthropicException("Content array is empty")
        val text = (firstBlock["text"] as? JsonPrimitive)?.content
            ?: throw AnthropicException("No text field in first content block")
        return text.trim()
    }

    @kotlinx.serialization.Serializable
    private data class ExtractedFilter(
        val minTempC: Double? = null,
        val maxTempC: Double? = null,
        val minWindSpeedKmh: Double? = null,
        val maxWindSpeedKmh: Double? = null,
        val windDirections: List<String>? = null,
        val sustainedWindHours: Int? = null,
        val daylightOnly: Boolean? = null,
        val noRain: Boolean? = null,
        val maxCloudCoverPct: Double? = null
    )
}
