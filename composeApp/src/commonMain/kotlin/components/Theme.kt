package components

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// https://venngage.com/blog/pastel-color-palettes/
// https://www.composables.com/colorconverter
val button_bg_color = Color.Cyan

// Month status colors
val monthFullyLoadedColor = Color(0xFF2E7D32) // Dark green
val monthPartiallyLoadedColor = Color.Blue
val monthNotLoadedColor = Color.Gray

// Define your light theme colors
private val LightColorScheme = lightColorScheme(
    primary = Color.Magenta,
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFF3700B3),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    // You can define other colors like error, surfaceVariant, etc.
    // primaryContainer, onPrimaryContainer, etc.
)

// Define your dark theme colors (optional, but good practice)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC), // A lighter purple for dark theme
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFF3700B3),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // typography = Typography, // Assuming you have a Typography.kt
        // shapes = Shapes, // Assuming you have a Shapes.kt (optional)
        content = content
    )
}
