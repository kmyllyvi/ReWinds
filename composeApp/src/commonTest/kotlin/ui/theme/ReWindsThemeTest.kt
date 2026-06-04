package ui.theme

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

/**
 * Compile-time / runtime assertions that the palette token object is properly
 * initialised and that key colour values match the design spec exactly.
 */
class ReWindsThemeTest {

    @Test
    fun tokenObjectIsNonNull() {
        // If the object failed to initialise this would NPE before reaching the assertion.
        assertNotNull(ReWindsColors)
    }

    @Test
    fun pageBgMatchesSpec() {
        // #030810 → ARGB 0xFF030810
        assertEquals(0xFF030810.toInt(), ReWindsColors.pageBg.value.toInt().and(0xFFFFFFFF.toInt()).or(0xFF000000.toInt()))
    }

    @Test
    fun accentBlueMatchesSpec() {
        // #8ECFF0 → ARGB 0xFF8ECFF0
        val expected = 0xFF8ECFF0.toInt()
        val actual = ReWindsColors.accentBlue.value.toInt().and(0xFFFFFFFF.toInt()).or(0xFF000000.toInt())
        assertEquals(expected, actual)
    }

    @Test
    fun allTokensHaveFullOpacity() {
        // Every named palette colour must be fully opaque (alpha = 0xFF).
        // Derived transparent tokens (containers) are excluded.
        val opaqueTokens = listOf(
            ReWindsColors.pageBg,
            ReWindsColors.surface,
            ReWindsColors.surfaceRaised,
            ReWindsColors.border,
            ReWindsColors.textPrimary,
            ReWindsColors.textSecondary,
            ReWindsColors.textTertiary,
            ReWindsColors.accentBlue,
            ReWindsColors.accentBluePressed,
            ReWindsColors.attention,
            ReWindsColors.error,
        )
        opaqueTokens.forEach { color ->
            val alpha = (color.value shr 56).and(0xFFu).toInt()
            assertEquals(0xFF, alpha, "Expected full opacity for $color")
        }
    }
}
