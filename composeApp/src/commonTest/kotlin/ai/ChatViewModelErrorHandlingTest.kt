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

    // --- Send-button enablement (KIM-271) ---

    @Test
    fun sendDisabledWhenInputBlank() {
        assertFalse(ChatUiState(inputText = "").isSendEnabled)
        assertFalse(ChatUiState(inputText = "   ").isSendEnabled)
    }

    @Test
    fun sendEnabledWhenInputPresentAndNotLoading() {
        assertTrue(ChatUiState(inputText = "hello").isSendEnabled)
    }

    @Test
    fun sendDisabledWhileLoading() {
        assertFalse(ChatUiState(inputText = "hello", isLoading = true).isSendEnabled)
    }

    // --- Context chips (KIM-271) ---

    @Test
    fun defaultStateHasAllPlacesChipSelected() {
        val chips = ChatUiState().contextChips
        assertTrue(chips.size == 1)
        val allPlaces = chips.single()
        assertNull(allPlaces.placeName)
        assertTrue(allPlaces.isSelected)
    }

    @Test
    fun chipSelectionIsMutuallyExclusive() {
        val chips = listOf(
            ContextChip(placeName = null, isSelected = true),
            ContextChip(placeName = "Konstanz", isSelected = false),
            ContextChip(placeName = "Helsinki", isSelected = false)
        )
        // Mirrors ChatViewModel.selectContextChip mapping logic.
        val updated = chips.map { it.copy(isSelected = it.placeName == "Konstanz") }
        assertTrue(updated.single { it.isSelected }.placeName == "Konstanz")
        assertFalse(updated.first { it.placeName == null }.isSelected)
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
