package core
// File: AppModule.kt
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import home.HomeViewModel
import org.koin.core.module.dsl.viewModel
import place.PlaceSummaryViewModel
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModelOf // For the constructor DSL
import place.MonthlyStatisticsViewModel

// all injectable dependencies
val appModule = module {
    // Singleton navigation controller
    single<Navigator> { (navController: NavHostController) -> AppNavigator(navController) }
    single<Networking> { NetworkService() }
    single<Database> { RealmDatabase() }
    single<WeatherRepository> { WeatherRepositoryImpl(get(), get()) }
    viewModelOf(::HomeViewModel)
    viewModelOf(::PlaceSummaryViewModel)
    viewModelOf(::MonthlyStatisticsViewModel)
//    viewModel { (handle: SavedStateHandle) ->
//        MonthlyStatisticsViewModel(
//            savedStateHandle = handle,
//            weatherRepository = get<WeatherRepository>()
//        )
//    }
}
