package core

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [WeatherApiKeyManager] — the single source of truth behind the KIM-309 hard
 * gate. Focus is the validity boundary (what counts as a "valid" VC key) and the reactive
 * [WeatherApiKeyManager.hasValidKeyFlow] emissions that drive the gate clear/return in-session.
 *
 * WeatherApiKeyManager is a global object, so it is reset to the empty (gate-active) state
 * before and after every test to keep them order-independent.
 */
class WeatherApiKeyManagerTest {

    @BeforeTest
    fun setUp() {
        WeatherApiKeyManager.setApiKey("")
    }

    @AfterTest
    fun tearDown() {
        WeatherApiKeyManager.setApiKey("")
    }

    // ── Validity boundary ────────────────────────────────────────────────────

    @Test
    fun emptyKeyIsNotValid() {
        WeatherApiKeyManager.setApiKey("")
        assertFalse(WeatherApiKeyManager.hasValidKey(), "Empty key must not be valid")
    }

    @Test
    fun whitespaceOnlyKeyIsNotValid() {
        WeatherApiKeyManager.setApiKey("   ")
        assertFalse(
            WeatherApiKeyManager.hasValidKey(),
            "A whitespace-only key is blank and must not satisfy the gate"
        )
    }

    @Test
    fun tabAndNewlineOnlyKeyIsNotValid() {
        WeatherApiKeyManager.setApiKey("\t\n ")
        assertFalse(
            WeatherApiKeyManager.hasValidKey(),
            "Tabs/newlines are blank whitespace and must not satisfy the gate"
        )
    }

    @Test
    fun placeholderKeyIsNotValid() {
        WeatherApiKeyManager.setApiKey("your-placeholder-key-here")
        assertFalse(
            WeatherApiKeyManager.hasValidKey(),
            "A key containing 'placeholder' must not be valid"
        )
    }

    @Test
    fun realLookingKeyIsValid() {
        WeatherApiKeyManager.setApiKey("ABC123XYZ789REALKEY")
        assertTrue(
            WeatherApiKeyManager.hasValidKey(),
            "A non-blank key without 'placeholder' must be valid"
        )
    }

    @Test
    fun keyWithSurroundingWhitespaceButRealContentIsValid() {
        // isBlank() is false when there are non-whitespace chars; the manager does not trim,
        // so this documents current behaviour: a key with real content is accepted as-is.
        WeatherApiKeyManager.setApiKey("  realkey  ")
        assertTrue(
            WeatherApiKeyManager.hasValidKey(),
            "A key with real content surrounded by whitespace is non-blank and valid"
        )
        assertEquals("  realkey  ", WeatherApiKeyManager.getApiKey())
    }

    // ── Reactive flow: gate clear / return in-session ────────────────────────

    @Test
    fun flowEmitsTrueWhenValidKeySaved() = runTest {
        WeatherApiKeyManager.setApiKey("")
        assertFalse(WeatherApiKeyManager.hasValidKeyFlow.first(), "Precondition: gate active")

        WeatherApiKeyManager.setApiKey("valid-key-001")
        assertTrue(
            WeatherApiKeyManager.hasValidKeyFlow.first(),
            "Saving a valid key must flip the reactive flow to true (gate clears in-session)"
        )
    }

    @Test
    fun flowEmitsFalseWhenKeyClearedAfterSetup() = runTest {
        // Simulate: user configured a key, then deleted it from Settings → gate must return.
        WeatherApiKeyManager.setApiKey("valid-key-002")
        assertTrue(WeatherApiKeyManager.hasValidKeyFlow.first(), "Precondition: gate cleared")

        WeatherApiKeyManager.setApiKey("")
        assertFalse(
            WeatherApiKeyManager.hasValidKeyFlow.first(),
            "Clearing the key after setup must flip the flow back to false (gate returns)"
        )
    }

    @Test
    fun flowReturnsToFalseWhenKeyReplacedWithPlaceholder() = runTest {
        WeatherApiKeyManager.setApiKey("valid-key-003")
        assertTrue(WeatherApiKeyManager.hasValidKeyFlow.first())

        WeatherApiKeyManager.setApiKey("placeholder")
        assertFalse(
            WeatherApiKeyManager.hasValidKeyFlow.first(),
            "Replacing a valid key with a placeholder must re-activate the gate"
        )
    }
}
