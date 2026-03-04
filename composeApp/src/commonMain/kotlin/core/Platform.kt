package core

import io.ktor.client.HttpClient

// platform specific code, using "actual" implementations

// HttpClient is provided to ktor based on the platform
expect fun httpClient(enableNetworkLogs: Boolean): HttpClient

// Platform detection
expect fun isAndroid(): Boolean
expect fun isIOS(): Boolean

// Anthropic API Key - must be provided by platform-specific implementations
expect fun getAnthropicApiKey(): String

// Save API key to platform-specific storage (Keychain on iOS, no-op on Android)
expect fun saveApiKeyPlatform(key: String)

// Delete API key from platform-specific storage
expect fun deleteApiKeyPlatform()

// Check if a valid API key is configured (not placeholder)
fun isAnthropicApiKeyConfigured(): Boolean {
    val key = getAnthropicApiKey()
    Log.d("Platform: API key check - length=${key.length}, starts_with_sk_ant=${key.startsWith("sk-ant-")}, has_placeholder=${key.contains("placeholder")}")
    return key.isNotBlank() && !key.contains("placeholder") && key.startsWith("sk-ant-")
}
