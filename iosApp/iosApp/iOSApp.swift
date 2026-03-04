import SwiftUI
import ComposeApp // This exposes the whole Kotlin code!
// defined in build.gradle iosTarget.binaries.framework { baseName=x }

/**
 IMPORTANT!!!
 To access ANY kt method:
 Call via filenameKt.doMethodName()
 Generated Swift APIs: .../composeApp/build/bin/<TARGET>/debugFramework/ComposeApp.framework/Headers
*/
@main
struct iOSApp: App {

    init() {
        // TODO: Keychain integration
        // For now, using placeholder API key from Platform.apple.kt
        // Keychain feature can be enabled once framework bindings are stable

        // Initialize Koin and database
        // call Kotlin (koin init) see https://insert-koin.io/docs/quickstart/kmp/
        // So DI.kt file becomes "DIKt"
        let driverFactory = DatabaseDriverFactory()
        DIKt.doInitKoin(databaseDriverFactory: driverFactory)

        // napier logger init https://github.com/AAkira/Napier?tab=readme-ov-file#ios
        IosUtilsKt.doInitLogger()
    }

	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}
