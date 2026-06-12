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
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
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
    single { AppSettingsRepository(get()) } bind AppSettingsStore::class

    // ViewModels
    viewModelOf(::AppViewModel)
    // Navigator is created in Router.kt composable, not through DI
    viewModelOf(::HomeViewModel)
    viewModelOf(::PlaceSummaryViewModel)
    viewModelOf(::MonthlyStatisticsViewModel)
    // Explicit lambda (not viewModelOf): ioDispatcher has a default and must not be
    // resolved from the graph. Constructor-reflection would try to inject a
    // CoroutineDispatcher, which isn't registered, and crash the chat screen.
    viewModel { ChatViewModel(get(), get(), get()) } // AiRepository, WeatherRepository, ChatRepository
    viewModelOf(::SettingsViewModel)
    viewModelOf(::TabNavigationViewModel)
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
