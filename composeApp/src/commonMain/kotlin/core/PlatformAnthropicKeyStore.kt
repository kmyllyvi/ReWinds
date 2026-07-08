package core

import settings.AnthropicKeyStore

/**
 * Production [AnthropicKeyStore]: persists to the platform key store (iOS Keychain /
 * Android SharedPreferences) and mirrors the value into the in-memory [ApiKeyManager] so
 * the running session picks it up immediately (KIM-252).
 */
object PlatformAnthropicKeyStore : AnthropicKeyStore {
    override fun save(key: String) {
        saveApiKeyPlatform(key)
        ApiKeyManager.setApiKey(key)
    }

    override fun delete() {
        deleteApiKeyPlatform()
        ApiKeyManager.setApiKey("")
    }
}
