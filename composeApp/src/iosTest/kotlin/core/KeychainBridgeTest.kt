package core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the Swift<->Kotlin keychain bridge objects and their iOS wrapper functions.
 *
 * [KeychainBridge] / [WeatherKeychainBridge] hold mutable global callback state, so every test
 * resets both bridges in teardown to stay order-independent.
 */
class KeychainBridgeTest {

    @AfterTest
    fun resetBridges() {
        KeychainBridge.saveKeyCallback = null
        KeychainBridge.deleteKeyCallback = null
        WeatherKeychainBridge.saveKeyCallback = null
        WeatherKeychainBridge.deleteKeyCallback = null
    }

    @Test
    fun keychainBridge_saveKey_invokesRegisteredCallbackWithKey() {
        var captured: String? = null
        KeychainBridge.saveKeyCallback = { captured = it }

        KeychainBridge.saveKey("sk-ant-123")

        assertEquals("sk-ant-123", captured)
    }

    @Test
    fun keychainBridge_deleteKey_invokesRegisteredCallback() {
        var deleted = false
        KeychainBridge.deleteKeyCallback = { deleted = true }

        KeychainBridge.deleteKey()

        assertTrue(deleted)
    }

    @Test
    fun keychainBridge_noCallback_isNoOp() {
        // Reset by teardown contract; absence of a callback must not throw.
        KeychainBridge.saveKey("ignored")
        KeychainBridge.deleteKey()
        assertNull(KeychainBridge.saveKeyCallback)
    }

    @Test
    fun registerKeychainCallbacks_wiresBothCallbacksIntoBridge() {
        var saved: String? = null
        var deleted = false

        registerKeychainCallbacks(onSave = { saved = it }, onDelete = { deleted = true })

        KeychainBridge.saveKey("key-A")
        KeychainBridge.deleteKey()

        assertEquals("key-A", saved)
        assertTrue(deleted)
    }

    @Test
    fun setApiKeyFromKeychain_updatesApiKeyManager() {
        setApiKeyFromKeychain("sk-ant-from-keychain")

        assertEquals("sk-ant-from-keychain", ApiKeyManager.getApiKey())
    }

    @Test
    fun weatherKeychainBridge_saveAndDelete_invokeCallbacks() {
        var saved: String? = null
        var deleted = false
        WeatherKeychainBridge.saveKeyCallback = { saved = it }
        WeatherKeychainBridge.deleteKeyCallback = { deleted = true }

        WeatherKeychainBridge.saveKey("vc-key")
        WeatherKeychainBridge.deleteKey()

        assertEquals("vc-key", saved)
        assertTrue(deleted)
    }

    @Test
    fun registerWeatherKeychainCallbacks_wiresBothCallbacksIntoBridge() {
        var saved: String? = null
        var deleted = false

        registerWeatherKeychainCallbacks(onSave = { saved = it }, onDelete = { deleted = true })

        WeatherKeychainBridge.saveKey("vc-key-B")
        WeatherKeychainBridge.deleteKey()

        assertEquals("vc-key-B", saved)
        assertTrue(deleted)
    }

    @Test
    fun setWeatherApiKeyFromKeychain_updatesWeatherApiKeyManager() {
        setWeatherApiKeyFromKeychain("vc-from-keychain")

        assertEquals("vc-from-keychain", WeatherApiKeyManager.getApiKey())
        assertTrue(WeatherApiKeyManager.hasValidKey())
    }
}
