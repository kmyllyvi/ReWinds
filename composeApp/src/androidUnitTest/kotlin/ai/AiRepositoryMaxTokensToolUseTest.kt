package ai

import core.DataAvailabilityStatus
import core.GeoSearchResult
import core.Station
import core.StationsResult
import core.WeatherRepository
import core.WeatherResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression test for the chat 400 bug: a `max_tokens` cutoff that lands mid `tool_use` used to
 * leave an assistant `tool_use` in [AiRepository]'s history with no paired `tool_result`, so the
 * next send was rejected by Anthropic with "tool_use ids were found without tool_result blocks".
 *
 * Drives the real [AiRepository] loop through a [MockEngine]-backed [AnthropicClient] (the same seam
 * [AnthropicClientTest] uses; ktor-client-mock is only wired into androidUnitTest, so this lives here
 * rather than commonTest). Asserts the invariant on the repository history that
 * `buildAnthropicRequest()` sends verbatim — no dangling `tool_use` survives into a subsequent turn.
 */
class AiRepositoryMaxTokensToolUseTest {

    private val jsonConfig = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Minimal repository stub — the max_tokens path never executes a tool, so nothing here is hit. */
    private class StubRepo : WeatherRepository {
        override suspend fun getSavedPlaceNames(): List<String> = emptyList()
        override suspend fun getPlaceDayCounts(): Map<String, Long> = emptyMap()
        override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse? = null
        override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse =
            TestWeatherRepositoryFactory.createWeatherResponse(place = place)
        override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse =
            getDaysRange(place, "2026-01-01", null)
        override suspend fun deletePlace(placeName: String) {}
        override suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse =
            getDaysRange(place, "$year-01-01", null)
        override suspend fun searchForLocations(query: String): List<GeoSearchResult> = emptyList()
        override suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse =
            getDaysRange(place.name, "2026-01-01", null)
        override suspend fun checkDataAvailability(place: String, fromDate: String, toDate: String): DataAvailabilityStatus =
            DataAvailabilityStatus.Available
        override suspend fun fetchAndPersistStations(place: String): StationsResult = StationsResult.Empty
        override suspend fun getPersistedStations(place: String): List<Station> = emptyList()
    }

    /** A max_tokens response cut off after emitting a tool_use block (no accompanying text). */
    private val maxTokensWithToolUseBody = """
        {
          "content": [
            { "type": "tool_use", "id": "toolu_cutoff_1", "name": "get_wind_summary", "input": {} }
          ],
          "stop_reason": "max_tokens",
          "usage": { "input_tokens": 30, "output_tokens": 1024 }
        }
    """.trimIndent()

    private val endTurnBody = """
        {
          "content": [ { "type": "text", "text": "Sure — the wind looks good." } ],
          "stop_reason": "end_turn",
          "usage": { "input_tokens": 40, "output_tokens": 9 }
        }
    """.trimIndent()

    /** Builds an [AiRepository] whose API returns [bodies] in order (last body repeats if exhausted). */
    private fun repositoryReturning(bodies: List<String>): AiRepository {
        var call = 0
        val engine = MockEngine {
            val body = bodies[call.coerceAtMost(bodies.lastIndex)]
            call++
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(jsonConfig) }
        }
        return AiRepository(
            anthropicClient = AnthropicClient("test-key", httpClient),
            weatherTools = WeatherTools,
            weatherRepository = StubRepo()
        )
    }

    @Test
    fun maxTokensCutoffMidToolUse_leavesNoDanglingToolUseInHistory() = runTest {
        val repo = repositoryReturning(listOf(maxTokensWithToolUseBody))

        repo.sendMessage("How's the wind in Tarifa?")

        val history = repo.getHistory()
        assertNoDanglingToolUse(history)

        // The synthesized result pairs the exact cut-off tool_use id and is flagged as an error.
        val results = history.flatMap { it.content }.filterIsInstance<AnthropicContent.ToolResult>()
        assertEquals(1, results.size, "expected one synthesized tool_result for the cut-off call")
        assertEquals("toolu_cutoff_1", results.single().toolUseId)
        assertTrue(results.single().isError, "synthesized cut-off result should be marked is_error")
    }

    @Test
    fun subsequentSend_afterMaxTokensCutoff_completesNormally() = runTest {
        // First send is cut off mid tool_use; second send completes normally.
        val repo = repositoryReturning(listOf(maxTokensWithToolUseBody, endTurnBody))

        repo.sendMessage("How's the wind in Tarifa?")
        val result = repo.sendMessage("Thanks — and tomorrow?")

        // Before the fix the second send shipped a dangling tool_use and the API 400'd, surfacing as
        // an "Error: Failed to communicate…" string. A normal response proves the history was valid.
        assertTrue(
            result.responseText.contains("wind looks good"),
            "second turn should complete normally, was: ${result.responseText}"
        )
        assertNoDanglingToolUse(repo.getHistory())
    }

    /**
     * Asserts every assistant `tool_use` is immediately followed by a message carrying a matching
     * `tool_result` — the exact well-formedness the Anthropic API enforces (and 400s on when broken).
     */
    private fun assertNoDanglingToolUse(history: List<ConversationMessage>) {
        history.forEachIndexed { index, message ->
            val toolUseIds = message.content
                .filterIsInstance<AnthropicContent.ToolUse>()
                .map { it.id }
            if (toolUseIds.isEmpty()) return@forEachIndexed

            val nextResultIds = history.getOrNull(index + 1)?.content
                ?.filterIsInstance<AnthropicContent.ToolResult>()
                ?.map { it.toolUseId }
                ?: emptyList()

            toolUseIds.forEach { id ->
                assertTrue(
                    id in nextResultIds,
                    "tool_use '$id' has no paired tool_result in the following message"
                )
            }
        }
    }
}
