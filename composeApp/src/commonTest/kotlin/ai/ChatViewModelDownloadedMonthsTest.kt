package ai

import androidx.lifecycle.viewModelScope
import core.DataAvailabilityStatus
import core.GeoSearchResult
import core.Station
import core.StationsResult
import core.WeatherRepository
import core.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/**
 * KIM-321: ChatViewModel must collect each saved place's downloaded-months Flow into its own
 * state at init (MV*-compliant, logic in the VM) and assemble the system prompt from that live
 * state before each send.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelDownloadedMonthsTest {

    private val dispatcher = StandardTestDispatcher()
    private val created = mutableListOf<ChatViewModel>()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }

    @AfterTest
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        created.clear()
        dispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
    }

    /** Records the system prompt the VM pushes; returns canned responses. */
    private class RecordingAi : AiConversationRepository {
        var lastSystemPrompt: String? = null
        override suspend fun sendMessage(userMessage: String): AiMessageResult =
            AiMessageResult(responseText = "ok")
        override fun clearHistory() {}
        override fun loadHistory(messages: List<ConversationMessage>) {}
        override fun setSystemPrompt(prompt: String) { lastSystemPrompt = prompt }
    }

    private class MonthsFakeWeatherRepository(
        private val monthsByPlace: Map<String, Set<String>>
    ) : WeatherRepository {
        override suspend fun getSavedPlaceNames(): List<String> = monthsByPlace.keys.toList()
        override fun observeDownloadedMonths(place: String): Flow<Set<String>> =
            flowOf(monthsByPlace[place] ?: emptySet())
        override suspend fun getDownloadedMonths(place: String): Set<String> =
            monthsByPlace[place] ?: emptySet()
        override suspend fun getPlaceDayCounts(): Map<String, Long> = emptyMap()
        override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse? = null
        override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse =
            TestWeatherRepositoryFactory.createWeatherResponse(place)
        override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse =
            TestWeatherRepositoryFactory.createWeatherResponse(place)
        override suspend fun deletePlace(placeName: String) {}
        override suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse =
            TestWeatherRepositoryFactory.createWeatherResponse(place)
        override suspend fun searchForLocations(query: String): List<GeoSearchResult> = emptyList()
        override suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse =
            TestWeatherRepositoryFactory.createWeatherResponse(place.name)
        override suspend fun checkDataAvailability(place: String, fromDate: String, toDate: String): DataAvailabilityStatus =
            DataAvailabilityStatus.Available
        override suspend fun fetchAndPersistStations(place: String): StationsResult = StationsResult.Empty
        override suspend fun getPersistedStations(place: String): List<Station> = emptyList()
    }

    private fun viewModel(ai: AiConversationRepository, weather: WeatherRepository): ChatViewModel {
        val chatRepo = FakeChatRepository()
        return ChatViewModel(
            ai, weather, chatRepo,
            ioDispatcher = dispatcher,
            apiKeyChecker = AlwaysConfiguredKeyChecker
        ).also { created.add(it) }
    }

    @Test
    fun collectsDownloadedMonthsPerPlaceIntoState() = runTest(dispatcher) {
        val weather = MonthsFakeWeatherRepository(
            mapOf(
                "Helsinki" to setOf("2025-10", "2025-11"),
                "Tarifa" to setOf("2025-06")
            )
        )
        val vm = viewModel(RecordingAi(), weather)
        advanceUntilIdle()

        val state = vm.downloadedMonthsByPlace.value
        assertEquals(setOf("2025-10", "2025-11"), state["Helsinki"])
        assertEquals(setOf("2025-06"), state["Tarifa"])
    }

    @Test
    fun assemblesSystemPromptFromStateBeforeSend() = runTest(dispatcher) {
        val ai = RecordingAi()
        val weather = MonthsFakeWeatherRepository(mapOf("Helsinki" to setOf("2025-10")))
        val vm = viewModel(ai, weather)
        advanceUntilIdle()

        vm.sendMessage("what was the wind like?")
        advanceUntilIdle()

        val prompt = ai.lastSystemPrompt
        assertContains(prompt ?: "", "Helsinki: October 2025")
    }
}

/** Test ApiKeyChecker that always reports the Anthropic key as present. */
private object AlwaysConfiguredKeyChecker : core.ApiKeyChecker {
    override fun isAnthropicKeyConfigured(): Boolean = true
}
