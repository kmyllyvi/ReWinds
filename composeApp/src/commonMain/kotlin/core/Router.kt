package core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ai.ChatView
import home.HomeView
import place.MonthlyStatisticsView
import place.PlaceSummaryView
import settings.SettingsView
import ui.theme.ReWindsColors
import org.koin.compose.viewmodel.koinViewModel

// ── Tab bar constants ────────────────────────────────────────────────────────
// Sourced from design spec (screens-v1.html / KIM-267).

private val TabBarBg       = Color(0xF5070F1C)   // rgba(7,15,28,0.96)
private val TabBarBorder   = ReWindsColors.border
private val TabBarActive   = ReWindsColors.accentBlue
private val TabBarInactive = ReWindsColors.textTertiary
private val TabBarHeight   = 72.dp

private data class TabItem(val tab: AppTab, val label: String, val icon: ImageVector)

private val tabItems = listOf(
    TabItem(AppTab.PLACES,   "Places",   Icons.Filled.Place),
    TabItem(AppTab.CHAT,     "Chat",     Icons.Filled.Chat),
    TabItem(AppTab.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun Navigation() {
    // Each tab maintains its own back stack so state is preserved on tab switches.
    val placesStack   = remember { mutableStateListOf<NavRoute>(HomeRoute) }
    val chatStack     = remember { mutableStateListOf<NavRoute>(ChatRoute()) }
    val settingsStack = remember { mutableStateListOf<NavRoute>(SettingsRoute) }

    val placesNav   = remember(placesStack)   { NavigatorImpl(placesStack) }
    val chatNav     = remember(chatStack)     { NavigatorImpl(chatStack) }
    val settingsNav = remember(settingsStack) { NavigatorImpl(settingsStack) }

    val tabVm: TabNavigationViewModel = koinViewModel()
    val activeTab by tabVm.activeTab.collectAsState()

    // Delegate navigator for the Places tab that routes chat/settings to the tab bar
    // instead of pushing onto the Places back stack.
    val placesNavigatorDelegate = remember(placesNav, tabVm) {
        object : Navigator by placesNav {
            override fun navigateToChat(initialMessage: String?) {
                // Note: initial message deep-link is deferred to a follow-up ticket (KIM-267 spec).
                tabVm.selectTab(AppTab.CHAT)
            }
            override fun navigateToSettings() {
                tabVm.selectTab(AppTab.SETTINGS)
            }
        }
    }

    // The current top route of the places stack determines whether we are on a push
    // destination (no tab bar) or a tab root (tab bar visible).
    val currentPlacesRoute = placesStack.lastOrNull() ?: HomeRoute

    if (currentPlacesRoute is PlaceSummaryRoute || currentPlacesRoute is MonthlyStatisticsRoute) {
        // Push destinations fill the screen without a tab bar.
        PushDestination(route = currentPlacesRoute, navigator = placesNav)
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                TabBar(activeTab = activeTab, onTabSelected = tabVm::selectTab)
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (activeTab) {
                    AppTab.PLACES   -> HomeView(navigator = placesNavigatorDelegate)
                    AppTab.CHAT     -> ChatView(initialMessage = null, navigator = chatNav)
                    AppTab.SETTINGS -> SettingsView(navigator = settingsNav)
                }
            }
        }
    }
}

/** Renders a push-destination screen without the tab bar. */
@Composable
private fun PushDestination(route: NavRoute, navigator: Navigator) {
    when (route) {
        is PlaceSummaryRoute -> PlaceSummaryView(
            route = route,
            onBackClick = { navigator.navigateBack() },
            navigator = navigator
        )
        is MonthlyStatisticsRoute -> MonthlyStatisticsView(
            placeName = route.placeName,
            year = route.year,
            month = route.month,
            onBackClick = { navigator.navigateBack() }
        )
        else -> Unit
    }
}

@Composable
private fun TabBar(activeTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    Column {
        HorizontalDivider(
            color = TabBarBorder,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )
        NavigationBar(
            modifier = Modifier.heightIn(min = TabBarHeight),
            containerColor = TabBarBg,
            contentColor = TabBarActive,
            tonalElevation = 0.dp
        ) {
            tabItems.forEach { item ->
                NavigationBarItem(
                    selected = activeTab == item.tab,
                    onClick = { onTabSelected(item.tab) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label
                        )
                    },
                    label = { Text(item.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = TabBarActive,
                        selectedTextColor   = TabBarActive,
                        unselectedIconColor = TabBarInactive,
                        unselectedTextColor = TabBarInactive,
                        indicatorColor      = Color.Transparent
                    )
                )
            }
        }
    }
}
