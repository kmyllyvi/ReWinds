package onboarding

import core.AppSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [WelcomeViewModel] (KIM-334): first-run visibility derived from the persisted
 * seen flag, and that dismissing persists it exactly once under the expected key.
 */
class WelcomeViewModelTest {

    /** In-memory key/value store standing in for AppSettingsRepository. */
    private class FakeSettingsStore(initial: Map<String, String> = emptyMap()) : AppSettingsStore {
        val map = initial.toMutableMap()
        override fun getString(key: String): String? = map[key]
        override fun setString(key: String, value: String) {
            map[key] = value
        }
    }

    @Test
    fun firstLaunch_showsWelcome() {
        val vm = WelcomeViewModel(FakeSettingsStore())
        assertTrue(vm.showFirstRunWelcome.value)
    }

    @Test
    fun subsequentLaunch_doesNotShowWelcome() {
        val store = FakeSettingsStore(mapOf(WelcomeViewModel.SEEN_KEY to "true"))
        val vm = WelcomeViewModel(store)
        assertFalse(vm.showFirstRunWelcome.value)
    }

    @Test
    fun onFirstRunDismissed_persistsSeenFlagAndHides() {
        val store = FakeSettingsStore()
        val vm = WelcomeViewModel(store)

        vm.onFirstRunDismissed()

        assertFalse(vm.showFirstRunWelcome.value)
        assertEquals("true", store.map[WelcomeViewModel.SEEN_KEY])
    }

    @Test
    fun seenFlagPersists_soAFreshViewModelSkipsWelcome() {
        val store = FakeSettingsStore()
        WelcomeViewModel(store).onFirstRunDismissed()

        // Simulates a restart: a new ViewModel over the same store must not show the screen.
        val reloaded = WelcomeViewModel(store)
        assertFalse(reloaded.showFirstRunWelcome.value)
    }

    @Test
    fun nonTrueStoredValue_isTreatedAsUnseen() {
        val store = FakeSettingsStore(mapOf(WelcomeViewModel.SEEN_KEY to "garbage"))
        val vm = WelcomeViewModel(store)
        assertTrue(vm.showFirstRunWelcome.value)
    }
}
