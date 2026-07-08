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

// Visual Crossing API Key - must be provided by platform-specific implementations
expect fun getVisualCrossingApiKey(): String

// Save Visual Crossing API key to platform-specific storage (Keychain on iOS, no-op on Android)
expect fun saveWeatherApiKeyPlatform(key: String)

// Delete Visual Crossing API key from platform-specific storage
expect fun deleteWeatherApiKeyPlatform()

// Persist and load language preference across app restarts
expect fun saveLanguagePreference(code: String)
expect fun loadLanguagePreference(): String?

/**
 * Opens the device email client with a pre-filled message. Subject and body are plain
 * (un-encoded) strings — each platform actual is responsible for building and percent-encoding
 * a valid `mailto:` URL. On Android this uses `ACTION_SENDTO` so only email apps are offered.
 */
expect fun sendEmail(recipient: String, subject: String, body: String)

/**
 * Opens [url] in the device's default web browser (system browser, never an in-app WebView).
 * Used for the "get an API key" links in onboarding/Settings (KIM-252). Invalid or
 * unopenable URLs are a no-op — the caller controls only trusted, hard-coded links.
 */
expect fun openUrl(url: String)

/** Short platform name for diagnostics / feedback triage, e.g. "Android" or "iOS". */
fun platformName(): String = if (isIOS()) "iOS" else "Android"
