package core

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests [NetworkService]'s HTTP error-mapping — the only place HTTP failures are translated into
 * the app's [NetworkException]. Previously untested (docs/agent/testing/COVERAGE-GAP-ANALYSIS.md §2.3).
 *
 * Uses Ktor's [MockEngine] via the [NetworkService] test constructor seam, so no real network
 * I/O happens. Lives in androidUnitTest (JVM) because ktor-client-mock is wired there.
 *
 * IMPORTANT — two client configurations are tested deliberately:
 *  - [serviceWithSuccessValidation] sets `expectSuccess = true`, which makes Ktor raise
 *    [io.ktor.client.plugins.ClientRequestException] on 4xx/5xx. This is the ONLY configuration
 *    under which NetworkService's `catch (ClientRequestException)` branch runs and `httpStatus`
 *    gets populated.
 *  - [serviceMirroringProduction] mirrors the real platform httpClient (Platform.android.kt /
 *    Platform.apple.kt), which does NOT set `expectSuccess`. Under that config a 4xx body that
 *    doesn't deserialize into the expected model falls through to the generic `catch (Exception)`
 *    branch instead — so `httpStatus` is null. See FINDING in the production-behaviour tests below.
 */
class NetworkServiceTest {

    private val jsonConfig = Json { ignoreUnknownKeys = true }

    private fun mockEngineReturning(status: HttpStatusCode, body: String) = MockEngine {
        respond(
            content = body,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }

    /** Client with response validation on — exercises the ClientRequestException error path. */
    private fun serviceWithSuccessValidation(status: HttpStatusCode, body: String): NetworkService {
        val client = HttpClient(mockEngineReturning(status, body)) {
            install(ContentNegotiation) { json(jsonConfig) }
            expectSuccess = true
        }
        return NetworkService(client)
    }

    /** Client matching the production platform config (no expectSuccess). */
    private fun serviceMirroringProduction(status: HttpStatusCode, body: String): NetworkService {
        val client = HttpClient(mockEngineReturning(status, body)) {
            install(ContentNegotiation) { json(jsonConfig) }
        }
        return NetworkService(client)
    }

    private fun serviceThrowing(error: Throwable): NetworkService {
        val client = HttpClient(MockEngine { throw error }) {
            install(ContentNegotiation) { json(jsonConfig) }
        }
        return NetworkService(client)
    }

    // ── ClientRequestException path (expectSuccess = true) ──────────────────────

    @Test
    fun fetchWeatherData_clientError_mapsToNetworkExceptionWithStatusAndBody() = runTest {
        val service = serviceWithSuccessValidation(
            HttpStatusCode.Unauthorized, """{"message":"Invalid API key"}"""
        )

        try {
            service.fetchWeatherData("https://example.test/weather")
            fail("Expected NetworkException")
        } catch (e: NetworkException) {
            assertEquals(401, e.httpStatus)
            assertTrue(
                e.message?.contains("Invalid API key") == true,
                "Response body text should be carried in the message, was: ${e.message}"
            )
        }
    }

    @Test
    fun fetchWeatherData_serverError_mapsToNetworkExceptionWith5xxStatus() = runTest {
        val service = serviceWithSuccessValidation(HttpStatusCode.ServiceUnavailable, "upstream down")

        try {
            service.fetchWeatherData("https://example.test/weather")
            fail("Expected NetworkException")
        } catch (e: NetworkException) {
            assertEquals(503, e.httpStatus)
            assertTrue(e.message?.contains("upstream down") == true)
        }
    }

    @Test
    fun fetchGeoSearchData_clientError_mapsToNetworkExceptionWithStatusAndBody() = runTest {
        val service = serviceWithSuccessValidation(
            HttpStatusCode.NotFound, """{"error":"no such resource"}"""
        )

        try {
            service.fetchGeoSearchData("https://example.test/geo")
            fail("Expected NetworkException")
        } catch (e: NetworkException) {
            assertEquals(404, e.httpStatus)
            assertTrue(e.message?.contains("no such resource") == true)
        }
    }

    // ── Connection-failure path (generic Exception branch) ──────────────────────

    @Test
    fun fetchWeatherData_connectionFailure_mapsToNetworkExceptionWithoutStatus() = runTest {
        val service = serviceThrowing(IOException("connection refused"))

        try {
            service.fetchWeatherData("https://example.test/weather")
            fail("Expected NetworkException")
        } catch (e: NetworkException) {
            assertNull(e.httpStatus, "A connection failure carries no HTTP status")
            assertTrue(
                e.message?.contains("Network request failed") == true,
                "Generic failures use the wrapped message, was: ${e.message}"
            )
        }
    }

    @Test
    fun fetchGeoSearchData_connectionFailure_mapsToNetworkExceptionWithoutStatus() = runTest {
        val service = serviceThrowing(IOException("host unreachable"))

        try {
            service.fetchGeoSearchData("https://example.test/geo")
            fail("Expected NetworkException")
        } catch (e: NetworkException) {
            assertNull(e.httpStatus)
            assertTrue(e.message?.contains("Network request failed") == true)
        }
    }

    // ── Production-behaviour guard (no expectSuccess) ───────────────────────────

    @Test
    fun fetchWeatherData_clientError_underProductionConfig_fallsThroughToGenericBranch() = runTest {
        // FINDING: the real platform httpClient does NOT set expectSuccess, so a 4xx with a body
        // that isn't a valid WeatherResponse is NOT caught as ClientRequestException — it fails
        // during body deserialization and surfaces via the generic branch with httpStatus = null.
        // This test pins that real-world behaviour so a future change to expectSuccess is noticed.
        val service = serviceMirroringProduction(
            HttpStatusCode.Unauthorized, """{"message":"Invalid API key"}"""
        )

        try {
            service.fetchWeatherData("https://example.test/weather")
            fail("Expected NetworkException")
        } catch (e: NetworkException) {
            assertNull(
                e.httpStatus,
                "Without expectSuccess the 4xx status is lost — the ClientRequestException branch never runs"
            )
        }
    }

    // ── Happy path ──────────────────────────────────────────────────────────────

    @Test
    fun fetchGeoSearchData_success_returnsParsedResponse() = runTest {
        val service = serviceMirroringProduction(
            HttpStatusCode.OK,
            """{"results":[{"id":1,"name":"Helsinki","latitude":60.17,"longitude":24.94}]}"""
        )

        val result = service.fetchGeoSearchData("https://example.test/geo")
        assertEquals(1, result.results?.size)
        assertEquals("Helsinki", result.results?.single()?.name)
    }
}
