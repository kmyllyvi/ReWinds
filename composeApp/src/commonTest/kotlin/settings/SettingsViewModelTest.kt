package settings

import ai.AnthropicClient
import core.AppSettingsStore
import core.DaysOfInterestFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for SettingsViewModel focusing on logic reachable without a network call:
 * - loading a persisted DaysOfInterestFilter on init (valid / missing / corrupt JSON)
 * - the blank-criteria guard in saveFilter (must not parse or touch storage)
 * - resetDoiState
 *
 * The happy-path saveFilter() requires a live Anthropic API call (AnthropicClient is a
 * concrete networking class), so it is intentionally not exercised here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /** In-memory key/value store standing in for AppSettingsRepository. */
    private class FakeSettingsStore(initial: Map<String, String> = emptyMap()) : AppSettingsStore {
        val map = initial.toMutableMap()
        var setCount = 0
        override fun getString(key: String): String? = map[key]
        override fun setString(key: String, value: String) {
            setCount++
            map[key] = value
        }
    }

    // AnthropicClient is concrete; constructing with a dummy key is safe as long as we
    // never trigger a path that calls it (blank input short-circuits before parsing).
    private val dummyClient = AnthropicClient(apiKey = "test-key", enableLogs = false)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsPersistedFilter() {
        val filter = DaysOfInterestFilter(
            naturalLanguageCriteria = "wind 20",
            minWindSpeedKmh = 20.0,
            minTempC = 10.0
        )
        val json = Json.encodeToString(DaysOfInterestFilter.serializer(), filter)
        val store = FakeSettingsStore(mapOf(SettingsViewModel.FILTER_KEY to json))

        val vm = SettingsViewModel(store, dummyClient)

        assertEquals(filter, vm.currentFilter.value)
    }

    @Test
    fun init_noPersistedFilterLeavesNull() {
        val store = FakeSettingsStore()
        val vm = SettingsViewModel(store, dummyClient)
        assertNull(vm.currentFilter.value)
    }

    @Test
    fun init_corruptJsonIsIgnoredAndDoesNotThrow() {
        val store = FakeSettingsStore(mapOf(SettingsViewModel.FILTER_KEY to "{not valid json"))
        // Should not throw; currentFilter stays null.
        val vm = SettingsViewModel(store, dummyClient)
        assertNull(vm.currentFilter.value)
    }

    @Test
    fun saveFilter_blankCriteriaIsNoOp() {
        val store = FakeSettingsStore()
        val vm = SettingsViewModel(store, dummyClient)

        vm.saveFilter("")
        vm.saveFilter("   ")

        // No persistence and state stays Idle (parsing never started).
        assertEquals(0, store.setCount)
        assertTrue(vm.doiState.value is DaysOfInterestUiState.Idle)
    }

    @Test
    fun resetDoiState_returnsToIdle() {
        val store = FakeSettingsStore()
        val vm = SettingsViewModel(store, dummyClient)
        vm.resetDoiState()
        assertTrue(vm.doiState.value is DaysOfInterestUiState.Idle)
    }

    @Test
    fun initialDoiStateIsIdle() {
        val store = FakeSettingsStore()
        val vm = SettingsViewModel(store, dummyClient)
        assertTrue(vm.doiState.value is DaysOfInterestUiState.Idle)
    }
}
