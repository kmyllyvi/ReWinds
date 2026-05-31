package core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for KIM-246: NetworkException carries HTTP status for auth-error detection.
 */
class NetworkExceptionTest {

    @Test
    fun networkExceptionWithHttpStatusPreservesIt() {
        val ex = NetworkException("Unauthorized", httpStatus = 401)
        assertEquals(401, ex.httpStatus)
    }

    @Test
    fun networkExceptionWithoutStatusIsNull() {
        val ex = NetworkException("Connection failed")
        assertNull(ex.httpStatus)
    }

    @Test
    fun authStatusDetectionFor401() {
        val ex = NetworkException("Unauthorized", httpStatus = 401)
        val isAuth = ex.httpStatus == 401 || ex.httpStatus == 403
        assertTrue(isAuth)
    }

    @Test
    fun authStatusDetectionFor403() {
        val ex = NetworkException("Forbidden", httpStatus = 403)
        val isAuth = ex.httpStatus == 401 || ex.httpStatus == 403
        assertTrue(isAuth)
    }

    @Test
    fun nonAuthStatusIsNotAuth() {
        val ex = NetworkException("Server error", httpStatus = 500)
        val isAuth = ex.httpStatus == 401 || ex.httpStatus == 403
        assertFalse(isAuth)
    }
}
