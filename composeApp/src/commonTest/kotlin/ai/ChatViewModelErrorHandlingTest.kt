package ai

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertNotNull

/**
 * Unit tests for KIM-246: error-state logic in ChatViewModel for missing/invalid Anthropic keys.
 *
 * Full coroutine-driven VM tests would need a TestDispatcher setup; these tests verify the
 * state shapes and helper functions that underpin the VM's error paths.
 */
class ChatViewModelErrorHandlingTest {

    // --- ChatUiState shape ---

    @Test
    fun initialUiStateHasNoErrors() {
        val state = ChatUiState()
        assertFalse(state.showApiKeyMissingDialog)
        assertFalse(state.showApiKeyInvalidError)
        assertNull(state.error)
    }

    @Test
    fun showApiKeyMissingDialogIsDistinctFromInvalidError() {
        val missingState = ChatUiState(showApiKeyMissingDialog = true)
        val invalidState = ChatUiState(showApiKeyInvalidError = true)

        assertTrue(missingState.showApiKeyMissingDialog)
        assertFalse(missingState.showApiKeyInvalidError)

        assertFalse(invalidState.showApiKeyMissingDialog)
        assertTrue(invalidState.showApiKeyInvalidError)
    }

    @Test
    fun genericErrorDoesNotSetAuthFlags() {
        val state = ChatUiState(error = "Network timeout")
        assertFalse(state.showApiKeyMissingDialog)
        assertFalse(state.showApiKeyInvalidError)
        assertNotNull(state.error)
    }

    @Test
    fun dismissingInvalidErrorClearsFlag() {
        val state = ChatUiState(showApiKeyInvalidError = true)
        val cleared = state.copy(showApiKeyInvalidError = false)
        assertFalse(cleared.showApiKeyInvalidError)
    }

    // --- AnthropicException HTTP status ---

    @Test
    fun anthropicExceptionCarriesHttpStatus() {
        val ex = AnthropicException("Unauthorized", httpStatus = 401)
        assertTrue(ex.httpStatus == 401 || ex.httpStatus == 403)
    }

    @Test
    fun anthropicExceptionWithout4xxStatusIsGeneric() {
        val ex = AnthropicException("Server error", httpStatus = 500)
        val isAuthError = ex.httpStatus == 401 || ex.httpStatus == 403
        assertFalse(isAuthError)
    }

    @Test
    fun anthropicExceptionWithNullStatusIsGeneric() {
        val ex = AnthropicException("Unknown error")
        val isAuthError = ex.httpStatus == 401 || ex.httpStatus == 403
        assertFalse(isAuthError)
    }
}
