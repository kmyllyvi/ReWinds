package place.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.theme.rewinds

/**
 * One summary tile in the Month Summary stat grid.
 *
 * Purely presentational: label on top in [textSecondary], a large [value] in
 * [textPrimary] with the [unit] trailing in [textTertiary], on a [surface] card.
 */
@Composable
fun MonthStatCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.rewinds.surface)
            .padding(horizontal = 11.dp, vertical = 10.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.rewinds.textSecondary
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.rewinds.textPrimary
                    )
                ) {
                    append(value)
                }
                withStyle(
                    SpanStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.rewinds.textTertiary
                    )
                ) {
                    append(unit)
                }
            }
        )
    }
}
