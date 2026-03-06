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
        // Priority 1: Use API key from build config (Info.plist) if available
        if let buildConfigKey = Bundle.main.infoDictionary?["ANTHROPIC_API_KEY"] as? String,
           !buildConfigKey.isEmpty && buildConfigKey != "sk-ant-" && !buildConfigKey.contains("$") {
            print("✓ Using API key from build configuration")
            IosKeychainKt.setApiKeyFromKeychain(key: buildConfigKey)
        }
        // Priority 2: Load from Keychain
        else if let savedKey = KeychainHelper.shared.load() {
            print("✓ Using API key from Keychain")
            IosKeychainKt.setApiKeyFromKeychain(key: savedKey)
        }

        // Register KeychainBridge callbacks for saving/deleting API keys
        IosKeychainKt.registerKeychainCallbacks(
            onSave: { key in
                _ = KeychainHelper.shared.save(key)
            },
            onDelete: {
                _ = KeychainHelper.shared.delete()
            }
        )

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
