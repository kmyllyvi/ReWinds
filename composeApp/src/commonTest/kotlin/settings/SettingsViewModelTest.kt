package settings

import ai.AnthropicClient
import core.AppSettingsStore
import core.DaysOfInterestFilter
import core.Language
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
import kotlin.test.assertFalse
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

    // ── KIM-273: grouped-list ViewModel state ────────────────────────────────

    @Test
    fun generalStateHasExpectedDefaults() {
        val vm = SettingsViewModel(FakeSettingsStore(), dummyClient)
        assertEquals(UnitSystem.METRIC, vm.unitsState.value)
        assertEquals(WindSpeedUnit.KMH, vm.windSpeedUnitState.value)
    }

    @Test
    fun setUnits_updatesUnitsState() {
        val vm = SettingsViewModel(FakeSettingsStore(), dummyClient)
        vm.setUnits(UnitSystem.IMPERIAL)
        assertEquals(UnitSystem.IMPERIAL, vm.unitsState.value)
    }

    @Test
    fun setWindSpeedUnit_updatesWindSpeedUnitState() {
        val vm = SettingsViewModel(FakeSettingsStore(), dummyClient)
        vm.setWindSpeedUnit(WindSpeedUnit.KNOTS)
        assertEquals(WindSpeedUnit.KNOTS, vm.windSpeedUnitState.value)
    }

    @Test
    fun setLanguage_updatesLanguageState() {
        val vm = SettingsViewModel(FakeSettingsStore(), dummyClient)
        vm.setLanguage(Language.GERMAN)
        assertEquals(Language.GERMAN, vm.languageState.value)
        // Restore the global LanguageManager so test order cannot leak.
        vm.setLanguage(Language.ENGLISH)
    }

    @Test
    fun autoRefreshDefaultsOn_andToggles() {
        val vm = SettingsViewModel(FakeSettingsStore(), dummyClient)
        assertTrue(vm.autoRefreshEnabled.value)
        vm.toggleAutoRefresh()
        assertFalse(vm.autoRefreshEnabled.value)
        vm.setAutoRefreshEnabled(true)
        assertTrue(vm.autoRefreshEnabled.value)
    }

    @Test
    fun wifiOnlyDefaultsOff_andToggles() {
        val vm = SettingsViewModel(FakeSettingsStore(), dummyClient)
        assertFalse(vm.wifiOnlyEnabled.value)
        vm.toggleWifiOnly()
        assertTrue(vm.wifiOnlyEnabled.value)
        vm.setWifiOnlyEnabled(false)
        assertFalse(vm.wifiOnlyEnabled.value)
    }

    @Test
    fun keyConfiguredFlags_reflectEmptyManagersByDefault() {
        // No key has been set on either manager in the test process, so both are absent.
        val vm = SettingsViewModel(FakeSettingsStore(), dummyClient)
        assertFalse(vm.anthropicKeyConfigured.value)
        assertFalse(vm.visualCrossingKeyConfigured.value)
    }

    @Test
    fun refreshKeyStatus_picksUpAConfiguredVisualCrossingKey() {
        val vm = SettingsViewModel(FakeSettingsStore(), dummyClient)
        try {
            core.WeatherApiKeyManager.setApiKey("real-vc-key")
            vm.refreshKeyStatus()
            assertTrue(vm.visualCrossingKeyConfigured.value)
        } finally {
            // Reset shared singleton so other tests see a clean slate.
            core.WeatherApiKeyManager.setApiKey("")
        }
    }
}
