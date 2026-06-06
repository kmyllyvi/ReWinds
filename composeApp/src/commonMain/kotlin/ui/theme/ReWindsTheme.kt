package ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Midnight Blue palette ────────────────────────────────────────────────────
// All values sourced from docs/designs/color-themes.html (Design System v1)

object ReWindsColors {
    val pageBg           = Color(0xFF030810)
    val surface          = Color(0xFF0D1B2E)
    val surfaceRaised    = Color(0xFF19304D)
    val border           = Color(0xFF1A3050)
    val textPrimary      = Color(0xFFDDEEF8)
    val textSecondary    = Color(0xFF7AB8D8)
    val textTertiary     = Color(0xFF4A7A9B)
    // Muted subtitle tone used under titles (e.g. map sheet subtitle)
    val textMuted        = Color(0xFF5A90B0)
    val accentBlue       = Color(0xFF8ECFF0)
    val accentBluePressed = Color(0xFF5BB3E0)
    val attention        = Color(0xFFE8A030)
    val error            = Color(0xFFD95060)

    // Derived semantic tokens used to populate MaterialTheme slots
    val attentionContainer  = Color(0x1FE8A030)  // attention at ~12 % opacity
    val errorContainer      = Color(0x1FD95060)  // error at ~12 % opacity
}

// ── MaterialTheme colour scheme ──────────────────────────────────────────────
// Semantic M3 slots mapped to our palette so existing screens referencing
// MaterialTheme.colorScheme keep working without hard-coding hex values.

private val MidnightBlueColorScheme = darkColorScheme(
    background            = ReWindsColors.pageBg,
    surface               = ReWindsColors.surface,
    surfaceVariant        = ReWindsColors.surfaceRaised,
    primary               = ReWindsColors.accentBlue,
    onPrimary             = ReWindsColors.pageBg,
    primaryContainer      = ReWindsColors.surfaceRaised,
    onPrimaryContainer    = ReWindsColors.textPrimary,
    secondary             = ReWindsColors.textTertiary,
    onSecondary           = ReWindsColors.textPrimary,
    secondaryContainer    = ReWindsColors.border,
    onSecondaryContainer  = ReWindsColors.textSecondary,
    tertiary              = ReWindsColors.textSecondary,
    onTertiary            = ReWindsColors.pageBg,
    tertiaryContainer     = ReWindsColors.surface,
    onTertiaryContainer   = ReWindsColors.textSecondary,
    onBackground          = ReWindsColors.textPrimary,
    onSurface             = ReWindsColors.textPrimary,
    onSurfaceVariant      = ReWindsColors.textSecondary,
    outline               = ReWindsColors.border,
    outlineVariant        = ReWindsColors.border,
    error                 = ReWindsColors.error,
    onError               = ReWindsColors.textPrimary,
    errorContainer        = ReWindsColors.errorContainer,
    onErrorContainer      = ReWindsColors.error,
)

// ── CompositionLocal so screens can reach extended tokens directly ───────────

private val LocalReWindsColors = staticCompositionLocalOf { ReWindsColors }

/** Access extended palette tokens (e.g. attention, accentBluePressed) from any composable. */
val MaterialTheme.rewinds: ReWindsColors
    @Composable
    @ReadOnlyComposable
    get() = LocalReWindsColors.current

// ── Root theme composable ────────────────────────────────────────────────────

/**
 * Single dark-only theme for ReWinds. Wraps [MaterialTheme] with the Midnight Blue
 * colour scheme and exposes extended tokens via [MaterialTheme.rewinds].
 *
 * No colour parameters are accepted — a single dark scheme is the only supported
 * mode per the design specification.
 */
@Composable
fun ReWindsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MidnightBlueColorScheme,
        content = content
    )
}
