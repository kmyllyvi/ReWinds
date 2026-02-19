import androidx.compose.ui.window.ComposeUIViewController

// Koin and Napier are initialized by iOSApp.swift (App.init) before this is called.
// See iOSApp.swift: DIKt.doInitKoin() + IosUtilsKt.doInitLogger()
fun MainViewController() = ComposeUIViewController { App() }
