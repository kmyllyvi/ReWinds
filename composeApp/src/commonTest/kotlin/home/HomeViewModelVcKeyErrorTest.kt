package home

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertEquals

/**
 * Unit tests for KIM-246: VC key error states in HomeUiState / HomeViewModel.
 */
class HomeViewModelVcKeyErrorTest {

    @Test
    fun initialStateHasNoVcKeyError() {
        val state = HomeUiState()
        assertNull(state.vcKeyError)
    }

    @Test
    fun vcKeyMissingStateIsDistinctFromInvalid() {
        val missingState = HomeUiState(vcKeyError = VcKeyErrorType.MISSING)
        val invalidState = HomeUiState(vcKeyError = VcKeyErrorType.INVALID)

        assertEquals(VcKeyErrorType.MISSING, missingState.vcKeyError)
        assertEquals(VcKeyErrorType.INVALID, invalidState.vcKeyError)
    }

    @Test
    fun genericErrorDoesNotSetVcKeyError() {
        val state = HomeUiState(error = "Some network error")
        assertNull(state.vcKeyError)
    }

    @Test
    fun dismissingVcKeyErrorClearsIt() {
        val state = HomeUiState(vcKeyError = VcKeyErrorType.INVALID)
        val cleared = state.copy(vcKeyError = null)
        assertNull(cleared.vcKeyError)
    }

    @Test
    fun vcKeyErrorAndGenericErrorAreIndependent() {
        val state = HomeUiState(error = "generic", vcKeyError = VcKeyErrorType.MISSING)
        assertEquals("generic", state.error)
        assertEquals(VcKeyErrorType.MISSING, state.vcKeyError)
    }
}
