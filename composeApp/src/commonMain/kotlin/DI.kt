import com.km.rewinds.db.AppDatabase
import core.*
import home.HomeViewModel
import org.koin.core.context.startKoin
import org.koin.core.error.KoinApplicationAlreadyStartedException
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

    // Database Export/Import (platform-specific implementation)
    // single<DatabaseExportImport> { DatabaseExportImport() }

    // ViewModels
    viewModelOf(::AppViewModel)
    // Navigator is created in Router.kt composable, not through DI
    viewModelOf(::HomeViewModel)
    viewModelOf(::PlaceSummaryViewModel)
    viewModelOf(::MonthlyStatisticsViewModel)
}

fun initKoin(databaseDriverFactory: DatabaseDriverFactory) {
    try {
        startKoin {
            modules(appModule(databaseDriverFactory, enableNetworkLogs = true))
        }
    } catch (e: KoinApplicationAlreadyStartedException) {
        // Already initialized. This can happen if called multiple times by mistake,
        // but in the correct architecture (Swift App.init calls this once), it shouldn't occur.
    }
}
