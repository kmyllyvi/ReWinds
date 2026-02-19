package core

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PlatformFunctionsTest {
    @Test
    fun testIsIOSReturnsTrue() {
        assertTrue(isIOS(), "isIOS() should return true on iOS")
    }

    @Test
    fun testIsAndroidReturnsFalse() {
        assertFalse(isAndroid(), "isAndroid() should return false on iOS")
    }
}
