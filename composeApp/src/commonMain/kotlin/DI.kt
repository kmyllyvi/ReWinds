import androidx.navigation.NavHostController
import com.km.rewinds.db.AppDatabase
import core.*
import home.HomeViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import place.MonthlyStatisticsViewModel
import place.PlaceSummaryViewModel

fun appModule(databaseDriverFactory: DatabaseDriverFactory, enableNetworkLogs: Boolean) = module {
    // Provide the factory from the platform
    single { databaseDriverFactory }

    // Database
    single<Database> { SqlDelightDatabase(get()) }
    single<AppDatabase> { createDatabase(get()) }

    // Networking
    single<Networking> { NetworkService(enableNetworkLogs) }

    // Repository
    single<WeatherRepository> { WeatherRepositoryImpl(get(), get(), enableNetworkLogs) }

    // Navigation
    // The NavHostController is provided at runtime from the Composable
    factory<Navigator> { (navController: NavHostController) -> AppNavigator(navController) }

    // ViewModels
    viewModelOf(::HomeViewModel)
    viewModelOf(::PlaceSummaryViewModel)
    viewModelOf(::MonthlyStatisticsViewModel)
}

fun initKoin(databaseDriverFactory: DatabaseDriverFactory) {
    startKoin {
        modules(appModule(databaseDriverFactory, enableNetworkLogs = true))
    }
}
