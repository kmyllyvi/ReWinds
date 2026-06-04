package ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class ReWindsThemeTest {

    private fun Color.toArgbInt(): Int =
        ((alpha * 255).roundToInt() shl 24) or
        ((red   * 255).roundToInt() shl 16) or
        ((green * 255).roundToInt() shl  8) or
         (blue  * 255).roundToInt()

    @Test
    fun tokenObjectIsNonNull() {
        assertNotNull(ReWindsColors)
    }

    @Test
    fun pageBgMatchesSpec() {
        assertEquals(0xFF030810.toInt(), ReWindsColors.pageBg.toArgbInt())
    }

    @Test
    fun accentBlueMatchesSpec() {
        assertEquals(0xFF8ECFF0.toInt(), ReWindsColors.accentBlue.toArgbInt())
    }

    @Test
    fun allTokensHaveFullOpacity() {
        listOf(
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
        ).forEach { color ->
            assertEquals(1.0f, color.alpha, "Expected full opacity for $color")
        }
    }
}
