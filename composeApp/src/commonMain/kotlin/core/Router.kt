package core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import ai.ChatView
import home.HomeView
import place.MonthlyStatisticsView
import place.PlaceSummaryView
import settings.SettingsView

@Composable
fun Navigation() {
    // Create multiplatform navigation back stack
    val backStack = remember { mutableStateListOf<NavRoute>(HomeRoute) }

    // Create navigator instance
    val navigator = remember(backStack) {
        NavigatorImpl(backStack)
    }

    // Display current route
    val currentRoute = backStack.lastOrNull() ?: HomeRoute

    when (currentRoute) {
        is HomeRoute -> {
            HomeView(navigator = navigator)
        }
        is PlaceSummaryRoute -> {
            PlaceSummaryView(
                route = currentRoute,
                onBackClick = { navigator.navigateBack() },
                navigator = navigator
            )
        }
        is MonthlyStatisticsRoute -> {
            MonthlyStatisticsView(
                placeName = currentRoute.placeName,
                year = currentRoute.year,
                month = currentRoute.month,
                onBackClick = { navigator.navigateBack() }
            )
        }
        is ChatRoute -> {
            ChatView(navigator = navigator)
        }
        is SettingsRoute -> {
            SettingsView(navigator = navigator)
        }
    }
}
