package place.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import place.DayWeatherSummary

/**
 * A composable that displays a summary of weather for a single day in a row format.
 * It shows the date, description, average wind speed, and average temperature.
 *
 * @param daySummary The [DayWeatherSummary] object containing the data to display.
 */
@Composable
fun DaySummaryRow(daySummary: DayWeatherSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            daySummary.date?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            Text(text = daySummary.description ?: "", style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(text = "Wind: ${daySummary.avgWindSpeed} km/h", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Temp: ${daySummary.avgTemp}°C", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
