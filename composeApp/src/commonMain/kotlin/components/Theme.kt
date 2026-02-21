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
    primary = Color(0xFF388E3C),          // Dark green for buttons and interactive elements
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9), // Light green for place cards
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF2E7D32),        // Deeper dark green
    onSecondary = Color.White,
    tertiary = Color(0xFF3700B3),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

// Define your dark theme colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF66BB6A),          // Lighter green for dark theme
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF2E7D32), // Darker green container for dark mode
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF81C784),        // Light green secondary
    onSecondary = Color(0xFF1B5E20),
    tertiary = Color(0xFF3700B3),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
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
