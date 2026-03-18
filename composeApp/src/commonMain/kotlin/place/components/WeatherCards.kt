package place.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import core.KiteSpotterConfig
import core.LocalAppStrings
import place.DayWeatherSummary
import kotlin.math.roundToInt

@Composable
fun StoredDaysList(storedDays: List<DayWeatherSummary>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(storedDays) { daySummary ->
            // Use the new sustained wind speed and centralized config for the highlighting logic
            val isDayOfInterest = (daySummary.sustainedWindSpeed ?: 0.0) >= KiteSpotterConfig.MIN_SUSTAINED_WIND_SPEED_KMH &&
                    (daySummary.avgTemp ?: 0.0) >= KiteSpotterConfig.MIN_TEMP_CELSIUS

            DayWeatherSummaryCard(
                daySummary = daySummary,
                isDayOfInterest = isDayOfInterest
            )
        }
    }
}

private fun formatTemperature(value: Double?): String {
    if (value == null) return "--"
    return "${(value * 10).roundToInt() / 10.0}\u00B0C"
}

private fun formatWindSpeed(value: Double?): String {
    if (value == null) return "--"
    return "${(value * 10).roundToInt() / 10.0} km/h"
}

@Composable
fun DayWeatherSummaryCard(
    daySummary: DayWeatherSummary,
    isDayOfInterest: Boolean
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isDayOfInterest) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = daySummary.date ?: strings.noDate,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = daySummary.description ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Temperatures
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                InfoColumn(strings.maxTemp, formatTemperature(daySummary.maxTemp))
                InfoColumn(strings.avgTemp, formatTemperature(daySummary.avgTemp))
                InfoColumn(strings.minTemp, formatTemperature(daySummary.minTemp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Wind Speeds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround // Changed for better spacing
            ) {
                InfoColumn(strings.avgWind, formatWindSpeed(daySummary.avgWindSpeed))
                // Add the new Sustained Wind column
                InfoColumn(strings.sustWind, formatWindSpeed(daySummary.sustainedWindSpeed))
                InfoColumn(strings.maxWind, formatWindSpeed(daySummary.maxWindSpeed))
            }
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
