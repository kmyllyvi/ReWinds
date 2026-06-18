package settings

import ai.AnthropicClient
import androidx.lifecycle.viewModelScope
import core.ApiKeyChecker
import core.AppSettingsStore
import core.DaysOfInterestFilter
import core.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

    // Track every ViewModel so teardown can cancel its viewModelScope. Without this, a VM's
    // scope (bound to Dispatchers.Main = this StandardTestDispatcher) is still live when
    // resetMain() runs, and any leftover coroutine leaks into the next test class as an
    // UncaughtExceptionsBeforeTest / IllegalStateException.
    private val createdViewModels = mutableListOf<SettingsViewModel>()

    private fun viewModel(
        store: AppSettingsStore,
        apiKeyChecker: ApiKeyChecker = ApiKeyChecker { false }
    ): SettingsViewModel =
        SettingsViewModel(store, dummyClient, apiKeyChecker).also { createdViewModels.add(it) }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        createdViewModels.forEach { it.viewModelScope.cancel() }
        createdViewModels.clear()
        dispatcher.scheduler.advanceUntilIdle() // process the cancellations cleanly
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

        val vm = viewModel(store)

        assertEquals(filter, vm.currentFilter.value)
    }

    @Test
    fun init_noPersistedFilterLeavesNull() {
        val store = FakeSettingsStore()
        val vm = viewModel(store)
        assertNull(vm.currentFilter.value)
    }

    @Test
    fun init_corruptJsonIsIgnoredAndDoesNotThrow() {
        val store = FakeSettingsStore(mapOf(SettingsViewModel.FILTER_KEY to "{not valid json"))
        // Should not throw; currentFilter stays null.
        val vm = viewModel(store)
        assertNull(vm.currentFilter.value)
    }

    @Test
    fun saveFilter_blankCriteriaIsNoOp() {
        val store = FakeSettingsStore()
        val vm = viewModel(store)

        vm.saveFilter("")
        vm.saveFilter("   ")

        // No persistence and state stays Idle (parsing never started).
        assertEquals(0, store.setCount)
        assertTrue(vm.doiState.value is DaysOfInterestUiState.Idle)
    }

    @Test
    fun resetDoiState_returnsToIdle() {
        val store = FakeSettingsStore()
        val vm = viewModel(store)
        vm.resetDoiState()
        assertTrue(vm.doiState.value is DaysOfInterestUiState.Idle)
    }

    @Test
    fun initialDoiStateIsIdle() {
        val store = FakeSettingsStore()
        val vm = viewModel(store)
        assertTrue(vm.doiState.value is DaysOfInterestUiState.Idle)
    }

    // ── KIM-273: grouped-list ViewModel state ────────────────────────────────

    @Test
    fun generalStateHasExpectedDefaults() {
        val vm = viewModel(FakeSettingsStore())
        assertEquals(UnitSystem.METRIC, vm.unitsState.value)
        assertEquals(WindSpeedUnit.KMH, vm.windSpeedUnitState.value)
    }

    @Test
    fun setUnits_updatesUnitsState() {
        val vm = viewModel(FakeSettingsStore())
        vm.setUnits(UnitSystem.IMPERIAL)
        assertEquals(UnitSystem.IMPERIAL, vm.unitsState.value)
    }

    @Test
    fun setWindSpeedUnit_updatesWindSpeedUnitState() {
        val vm = viewModel(FakeSettingsStore())
        vm.setWindSpeedUnit(WindSpeedUnit.KNOTS)
        assertEquals(WindSpeedUnit.KNOTS, vm.windSpeedUnitState.value)
    }

    @Test
    fun setLanguage_updatesLanguageState() {
        val vm = viewModel(FakeSettingsStore())
        vm.setLanguage(Language.GERMAN)
        assertEquals(Language.GERMAN, vm.languageState.value)
        // Restore the global LanguageManager so test order cannot leak.
        vm.setLanguage(Language.ENGLISH)
    }

    @Test
    fun autoRefreshDefaultsOn_andToggles() {
        val vm = viewModel(FakeSettingsStore())
        assertTrue(vm.autoRefreshEnabled.value)
        vm.toggleAutoRefresh()
        assertFalse(vm.autoRefreshEnabled.value)
        vm.setAutoRefreshEnabled(true)
        assertTrue(vm.autoRefreshEnabled.value)
    }

    @Test
    fun wifiOnlyDefaultsOff_andToggles() {
        val vm = viewModel(FakeSettingsStore())
        assertFalse(vm.wifiOnlyEnabled.value)
        vm.toggleWifiOnly()
        assertTrue(vm.wifiOnlyEnabled.value)
        vm.setWifiOnlyEnabled(false)
        assertFalse(vm.wifiOnlyEnabled.value)
    }

    @Test
    fun keyConfiguredFlags_reflectEmptyManagersByDefault() {
        // Anthropic gate is injected as "not configured" so the assertion is deterministic and
        // independent of any ANTHROPIC_API_KEY baked into BuildConfig (env / gradle.properties).
        // The Visual Crossing manager is a process singleton left empty by the other tests' cleanup.
        val vm = viewModel(FakeSettingsStore())
        assertFalse(vm.anthropicKeyConfigured.value)
        assertFalse(vm.visualCrossingKeyConfigured.value)
    }

    @Test
    fun refreshKeyStatus_picksUpAConfiguredVisualCrossingKey() {
        val vm = viewModel(FakeSettingsStore())
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
