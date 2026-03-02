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
