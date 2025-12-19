package core
import androidx.compose.runtime.Composable
import home.HomeView
import place.PlaceSummaryView

import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import place.MonthlyStatisticsView
import io.ktor.http.encodeURLPathPart // Ktor utility for URL encoding
import io.ktor.http.decodeURLPart // Ktor utility for URL decoding

// Data classes for arguments
//sealed class ScreenArguments {
//    data class PlaceSummary(val placeName: String) : ScreenArguments()
//}

// Define sealed class for navigation events
//sealed class NavigationEvent {
//    data class NavigateToPlaceSummary(val placeName: String) : NavigationEvent()
//    object NavigateBack : NavigationEvent()
//    // Add other navigation events here
//}
/**
 * enum values that represent the screens in the app
 */
sealed class Screen(val title: String, val route: String) {
    data object Home : Screen(title = "Start", route = "home")
    // data object Places : Screen(title = "Places", route = "places")
    data object PlaceSummary : Screen(
        title = "Place Summary",
        route = "place_summary/{placeName}") {
        fun createRoute(placeName: String) = "place_summary/$placeName"
    }
    data object MonthlyStatistics : Screen(
        title = "Monthly Statistics",
        route = "monthly_statistics/{placeName}/{year}/{month}") {
        fun createRoute(placeName: String, year: Int, month: Int): String {
            val encodedPlaceName = placeName.encodeURLPathPart() // URL Encode the placeName
            Log.d("NAV_DEBUG: encodedPlaceName: '${encodedPlaceName}'")
            return "monthly_statistics/$encodedPlaceName/$year/$month"
        }
    }
}

@Composable
fun Navigation() {
    val navController = rememberNavController()
    // Provide navController to Koin and instantiate Navigator
    val navigator = koinInject<Navigator> { parametersOf(navController) }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Home screen route
        composable(Screen.Home.route) {
            HomeView(navController = navController)
        }

        // Detail screen route with parameter
        composable(
            route = Screen.PlaceSummary.route,
            arguments = listOf(
                navArgument("placeName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("placeName") ?: ""
            PlaceSummaryView(
                onBackClick = { navController.popBackStack() },
                navController = navController
            )
        }

        composable(
            route = Screen.MonthlyStatistics.route,
            arguments = listOf(
                navArgument("placeName") { type = NavType.StringType },
                navArgument("year") { type = NavType.IntType },
                navArgument("month") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val encodedPlaceName = backStackEntry.arguments?.getString("placeName") ?: ""
            val placeName = encodedPlaceName.decodeURLPart() // Decode here
            val year = backStackEntry.arguments?.getInt("year") ?: -1 // Consider default/error handling
            val month = backStackEntry.arguments?.getInt("month") ?: -1

            MonthlyStatisticsView(
                placeName = placeName,
                year = year,
                month = month,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}


// Create a clean architecture Navigator interface
// Navigator.kt
interface Navigator {
    fun navigateToHome()
    // Place summary
    fun navigateToPlaceSummary(placeName: String)

    fun navigateToMonthlyStatistics(placeName: String, year: Int, month: Int)
    fun navigateBack()
}

// Implementation of Navigator
// AppNavigator.kt
class AppNavigator(private val navController: NavHostController) : Navigator {
    override fun navigateToHome() {
        navController.navigate(Screen.Home.route) {
            // Pop up to the start destination
            popUpTo(Screen.Home.route) { inclusive = true }
        }
    }

    override fun navigateToPlaceSummary(placeName: String) {
        navController.navigate(Screen.PlaceSummary.createRoute(placeName))
    }

    override fun navigateToMonthlyStatistics(placeName: String, year: Int, month: Int) {
        navController.navigate(Screen.MonthlyStatistics.createRoute(placeName, year, month))
    }

    override fun navigateBack() {
        navController.popBackStack()
    }
}