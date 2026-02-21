package components

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Month status colors
val monthFullyLoadedColor = Color(0xFF2E7D32) // Dark green
val monthPartiallyLoadedColor = Color.Blue
val monthNotLoadedColor = Color.Gray

// Define your light theme colors
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),          // Vibrant dark green for buttons
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9), // Light green for place cards
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF388E3C),        // Secondary green
    onSecondary = Color.White,
    tertiary = Color(0xFF4CAF50),
    onTertiary = Color.White,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFF49454E),
)

// Define your dark theme colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),          // Bright green for dark theme
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF2E7D32), // Dark green container
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF66BB6A),        // Medium green secondary
    onSecondary = Color(0xFF1B5E20),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF1B5E20),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC7D0),
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
