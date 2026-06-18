package onboarding

import core.WeatherApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the KIM-309 hard gate ViewModel.
 *
 * WeatherApiKeyManager is a global object so it is reset between tests. The gate observes the
 * manager reactively; the key transition is the core behaviour under test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VcKeyOnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        WeatherApiKeyManager.setApiKey("")
    }

    @AfterTest
    fun tearDown() {
        WeatherApiKeyManager.setApiKey("")
        Dispatchers.resetMain()
    }

    @Test
    fun gateIsActiveWhenNoKeyConfigured() = runTest(dispatcher) {
        val vm = VcKeyOnboardingViewModel()
        advanceUntilIdle()
        assertFalse(
            vm.isWeatherKeyConfigured.value,
            "Gate must be active (false) when no valid VC key is present"
        )
    }

    @Test
    fun gateClearsOnFalseToTrueTransitionWhenValidKeySaved() = runTest(dispatcher) {
        val vm = VcKeyOnboardingViewModel()
        advanceUntilIdle()
        assertFalse(vm.isWeatherKeyConfigured.value, "Precondition: gate active without a key")

        // Saving a valid key flips the reactive signal — the gate clears in the same session.
        WeatherApiKeyManager.setApiKey("valid-vc-key-abc123")
        advanceUntilIdle()

        assertTrue(
            vm.isWeatherKeyConfigured.value,
            "Gate must clear (true) once a valid VC key is saved"
        )
    }

    @Test
    fun gateReturnsWhenKeyRemovedAfterSetup() = runTest(dispatcher) {
        // Configure a valid key first so the gate clears, then remove it (Settings "delete").
        // The gate must reactively return in the same session — true → false transition.
        WeatherApiKeyManager.setApiKey("valid-vc-key-abc123")
        val vm = VcKeyOnboardingViewModel()
        advanceUntilIdle()
        assertTrue(vm.isWeatherKeyConfigured.value, "Precondition: gate cleared by valid key")

        WeatherApiKeyManager.setApiKey("")
        advanceUntilIdle()

        assertFalse(
            vm.isWeatherKeyConfigured.value,
            "Removing the key after setup must re-activate the gate in the same session"
        )
    }

    @Test
    fun whitespaceOnlyKeyDoesNotClearGate() = runTest(dispatcher) {
        val vm = VcKeyOnboardingViewModel()
        WeatherApiKeyManager.setApiKey("   ")
        advanceUntilIdle()
        assertFalse(
            vm.isWeatherKeyConfigured.value,
            "A whitespace-only key is blank and must keep the gate active"
        )
    }

    @Test
    fun placeholderKeyDoesNotClearGate() = runTest(dispatcher) {
        val vm = VcKeyOnboardingViewModel()
        WeatherApiKeyManager.setApiKey("your-placeholder-key")
        advanceUntilIdle()
        assertFalse(
            vm.isWeatherKeyConfigured.value,
            "A placeholder key is not valid and must keep the gate active"
        )
    }

    @Test
    fun configureNowClickShowsKeyEntry() = runTest(dispatcher) {
        val vm = VcKeyOnboardingViewModel()
        assertFalse(vm.showKeyEntry.value)
        vm.onConfigureNowClicked()
        assertTrue(vm.showKeyEntry.value, "CTA must route the user into the Settings key entry")
    }
}
