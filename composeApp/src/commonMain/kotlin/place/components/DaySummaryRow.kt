package place.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import place.DayWeatherSummary
import core.utils.formatGust
import core.utils.formatTemperatureRange
import core.utils.shortDayLabel

/**
 * A single day's collapsed summary row: date, temperature range and peak gust.
 *
 * Tapping anywhere on the card invokes [onClick]; the hosting screen opens the day detail sheet
 * in response. The row holds no expand/selection state of its own — that lives in the ViewModel
 * (MV*).
 *
 * @param daySummary The [DayWeatherSummary] to display.
 * @param onClick Invoked when the card is tapped.
 */
@Composable
fun DaySummaryRow(
    daySummary: DayWeatherSummary,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (daySummary.isMatch)
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else
            CardDefaults.elevatedCardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = shortDayLabel(daySummary.date),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatTemperatureRange(daySummary.minTemp, daySummary.maxTemp),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = formatGust(daySummary.maxWindSpeed),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
