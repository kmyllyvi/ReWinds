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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
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
 * Full-screen blocking onboarding shown on every launch until a valid Visual Crossing key
 * is configured (KIM-309). Presentational only — all gate logic lives in
 * [VcKeyOnboardingViewModel]. There is deliberately no dismiss/skip/quit/back affordance;
 * the single CTA routes the user into the Settings VC key entry.
 *
 * No `.background()` on the root container — it blends with the system background via
 * [IsobarBackground], per the project UI guidelines.
 */
@Composable
fun VcKeyOnboardingScreen(onConfigureNow: () -> Unit) {
    val strings = LocalAppStrings.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Decorative isobar texture — lowest layer, below all content.
        IsobarBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.rewinds.accentBlue,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = strings.vcKeyNudgeTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.rewinds.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = strings.vcKeyNudgeBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.rewinds.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.visualCrossingApiUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.rewinds.textTertiary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onConfigureNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag(TestTags.ONBOARDING_VC_KEY_CONFIGURE),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.rewinds.accentBlue
                )
            ) {
                Text(
                    text = strings.vcKeyNudgeAction,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
