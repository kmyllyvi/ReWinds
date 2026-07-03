package onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import core.LocalAppStrings
import core.TestTags
import ui.components.IsobarBackground
import ui.theme.rewinds

/**
 * Informational welcome / "what is ReWinds" guide (KIM-334). Presentational only — the caller
 * owns visibility and supplies the dismiss action. Used in two places:
 *  - first launch, ahead of the VC-key gate (`buttonLabel` = "Get Started")
 *  - revisited from Settings (`buttonLabel` = "Done")
 *
 * No `.background()` on the root — it blends with the system background via [IsobarBackground],
 * per the project UI guidelines.
 */
@Composable
fun WelcomeView(
    buttonLabel: String,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Decorative isobar texture — lowest layer, below all content.
        IsobarBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Air,
                contentDescription = null,
                tint = MaterialTheme.rewinds.accentBlue,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = strings.welcomeTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.rewinds.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            WelcomeSection(
                heading = strings.welcomeWhatItIsHeading,
                body = strings.welcomeWhatItIsBody
            )

            Spacer(modifier = Modifier.height(20.dp))

            WelcomeSection(
                heading = strings.welcomeWhatItIsNotHeading,
                body = strings.welcomeWhatItIsNotBody
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag(TestTags.WELCOME_DISMISS_BUTTON),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.rewinds.accentBlue
                )
            ) {
                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** A titled paragraph block within the welcome guide. */
@Composable
private fun WelcomeSection(heading: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = heading,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.rewinds.accentBlue
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.rewinds.textSecondary
        )
    }
}
