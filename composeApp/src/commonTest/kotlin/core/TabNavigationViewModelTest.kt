package core

import kotlin.test.Test
import kotlin.test.assertEquals

class TabNavigationViewModelTest {

    @Test
    fun initialTabIsPlaces() {
        val vm = TabNavigationViewModel()
        assertEquals(AppTab.PLACES, vm.activeTab.value)
    }

    @Test
    fun selectTabUpdatesState() {
        val vm = TabNavigationViewModel()
        vm.selectTab(AppTab.CHAT)
        assertEquals(AppTab.CHAT, vm.activeTab.value)
    }

    @Test
    fun selectTabCyclesThroughAllTabs() {
        val vm = TabNavigationViewModel()
        AppTab.entries.forEach { tab ->
            vm.selectTab(tab)
            assertEquals(tab, vm.activeTab.value)
        }
    }
}
