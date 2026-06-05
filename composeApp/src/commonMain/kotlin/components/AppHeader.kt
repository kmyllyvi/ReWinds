package components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.LocalAppStrings
import core.isIOS
import ui.theme.rewinds

/**
 * Reusable app header component used across all screens.
 *
 * Supports two main configurations:
 * 1. Home screen: Shows logo + app name on left, custom actions on right
 * 2. Detail screens: Shows back button + title on left, custom actions on right
 *
 * Layout: Split header with left content and right content areas
 * Background: Uses system background for seamless integration
 * Divider: Always stays at the bottom of header content, regardless of padding
 */
@Composable
fun AppHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    showLogo: Boolean = false,
    /** When set, overrides the typography body size. Useful for screens that require a specific sp value. */
    titleSizeSp: Int? = null,
    rightContent: @Composable (() -> Unit)? = null,
) {
    val strings = LocalAppStrings.current
    // TOP MARGIN adjustment for iOS
    val topMargin = if (isIOS()) 0.dp else 35.dp
    Column(modifier = modifier.fillMaxWidth()) {
        // Header content with padding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = topMargin, end = 12.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left content: Back button + title OR Logo + title
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClick != null) {
                    // Back button for detail screens
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(start = 0.dp, end = 4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = MaterialTheme.rewinds.textPrimary
                        )
                    }
                } else if (showLogo) {
                    // Logo for home screen
                    Icon(
                        Icons.Filled.Air,
                        contentDescription = strings.appTitle,
                        tint = MaterialTheme.rewinds.accentBlue,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                // Title
                Text(
                    title,
                    style = if (titleSizeSp != null)
                        MaterialTheme.typography.headlineSmall.copy(fontSize = titleSizeSp.sp)
                    else
                        MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.rewinds.textPrimary
                )
            }

            // Right content: Custom actions (icons, buttons, etc.)
            if (rightContent != null) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rightContent()
                }
            }
        }

        // Fade divider: Always stuck at the bottom of header, edge-to-edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.rewinds.border.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
