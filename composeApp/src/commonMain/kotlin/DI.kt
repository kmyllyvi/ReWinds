import ai.AiRepository
import ai.AnthropicClient
import ai.ChatRepository
import ai.ChatRepositoryImpl
import ai.ChatViewModel
import ai.WeatherTools
import com.km.rewinds.db.AppDatabase
import core.*
import home.HomeViewModel
import org.koin.core.context.startKoin
import org.koin.core.error.KoinApplicationAlreadyStartedException
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import place.MonthlyStatisticsViewModel
import place.PlaceSummaryViewModel
import settings.SettingsViewModel

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
    single<DatabaseExportImport> { DatabaseExportImport() }

    // AI/Chat components
    single { AnthropicClient(apiKey = getAnthropicApiKey(), enableLogs = enableNetworkLogs) }
    single { WeatherTools }
    single { AiRepository(get(), get(), get()) } // AnthropicClient, WeatherTools, WeatherRepository
    single<ChatRepository> { ChatRepositoryImpl(get()) } // AppDatabase

    // App settings
    single { AppSettingsRepository(get()) }

    // ViewModels
    viewModelOf(::AppViewModel)
    // Navigator is created in Router.kt composable, not through DI
    viewModelOf(::HomeViewModel)
    viewModelOf(::PlaceSummaryViewModel)
    viewModelOf(::MonthlyStatisticsViewModel)
    viewModelOf(::ChatViewModel)
    viewModelOf(::SettingsViewModel)
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
