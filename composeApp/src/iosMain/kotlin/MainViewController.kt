import androidx.compose.ui.window.ComposeUIViewController
import core.DatabaseDriverFactory
import initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

// Initialize Koin on first app launch
private var koinInitialized = false

fun MainViewController() = ComposeUIViewController {
    // Initialize Koin only once
    if (!koinInitialized) {
        // Initialize Napier logger
        Napier.base(DebugAntilog())

        // Initialize Koin with database driver
        initKoin(DatabaseDriverFactory())
        koinInitialized = true
    }

    App()
}