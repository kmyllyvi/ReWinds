package ai

import core.DataAvailabilityStatus
import core.Station
import core.StationsResult
import core.WeatherRepository
import core.WeatherResponse
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * KIM-286 regression: the chat screen crashed on launch with
 * `NoDefinitionFoundException: No definition found for type
 * 'kotlinx.coroutines.CoroutineDispatcher'`.
 *
 * Cause: ChatViewModel gained a `ioDispatcher: CoroutineDispatcher = Dispatchers.IO`
 * constructor parameter. The production binding used `viewModelOf(::ChatViewModel)`,
 * which resolves every constructor parameter via reflection — Kotlin default values
 * are ignored — so Koin tried (and failed) to resolve a CoroutineDispatcher that was
 * never registered, and the whole chat screen failed to build.
 *
 * The fix binds ChatViewModel with an explicit lambda that supplies only its three
 * real dependencies, letting `ioDispatcher` fall back to its default.
 *
 * This test pins that binding shape: ChatViewModel must be resolvable from the graph
 * WITHOUT a CoroutineDispatcher definition. It deliberately mirrors the production
 * lambda binding rather than calling DI.appModule(), which needs a platform
 * DatabaseDriverFactory unavailable in commonTest.
 */
class ChatViewModelKoinGraphTest {

    @AfterTest
    fun tearDown() {
        // Defensive: this test only uses koinApplication {}, but guard against leakage.
        runCatching { stopKoin() }
    }

    @Test
    fun chatViewModelResolvesWithoutCoroutineDispatcherInGraph() {
        val app = koinApplication {
            modules(
                module {
                    single<WeatherRepository> { GraphFakeWeatherRepository() }
                    single<AiConversationRepository> { AiRepository(AnthropicClient(apiKey = "test", enableLogs = false), WeatherTools, get()) }
                    single<ChatRepository> { FakeChatRepository() }

                    // Must match DI.kt: explicit lambda, NOT viewModelOf. No
                    // CoroutineDispatcher is registered anywhere in this graph.
                    factory { ChatViewModel(get(), get(), get()) }
                }
            )
        }

        val vm = app.koin.get<ChatViewModel>()
        assertNotNull(vm, "ChatViewModel must be constructable without a CoroutineDispatcher in the Koin graph")
        app.close()
    }
}

/** Minimal WeatherRepository fake; only used to satisfy ChatViewModel construction. */
private class GraphFakeWeatherRepository : WeatherRepository {
    override suspend fun getSavedPlaceNames(): List<String> = emptyList()
    override suspend fun getPlaceDayCounts(): Map<String, Long> = emptyMap()
    override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse? = null
    override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse =
        TestWeatherRepositoryFactory.createWeatherResponse(place)
    override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse =
        TestWeatherRepositoryFactory.createWeatherResponse(place)
    override suspend fun deletePlace(placeName: String) {}
    override suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse =
        TestWeatherRepositoryFactory.createWeatherResponse(place)
    override suspend fun searchForLocations(query: String): List<core.GeoSearchResult> = emptyList()
    override suspend fun addPlaceFromSearch(place: core.GeoSearchResult): WeatherResponse =
        TestWeatherRepositoryFactory.createWeatherResponse(place.name)
    override suspend fun checkDataAvailability(place: String, fromDate: String, toDate: String): DataAvailabilityStatus =
        DataAvailabilityStatus.Available
    override suspend fun fetchAndPersistStations(place: String): StationsResult = StationsResult.Empty
    override suspend fun getPersistedStations(place: String): List<Station> = emptyList()
}
