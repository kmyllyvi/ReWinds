package place.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import core.LocalAppStrings
import core.degreesToCompass
import place.DayWeatherSummary
import core.utils.formatDecimal
import core.utils.formatGust
import core.utils.formatTemperatureRange
import core.utils.shortDayLabel

/**
 * A composable that displays a summary of weather for a single day in a row format.
 * It shows the date, description, average wind speed, and average temperature.
 *
 * @param daySummary The [DayWeatherSummary] object containing the data to display.
 */
@Composable
fun DaySummaryRow(daySummary: DayWeatherSummary) {
    val strings = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (daySummary.isMatch)
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else
            CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Always visible content
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
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) strings.collapse else strings.expand
                    )
                }
            }

            // Collapsible content
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = daySummary.description ?: strings.noDetails, style = MaterialTheme.typography.bodySmall)

                    // Min / Max temperature row
                    val minTemp = daySummary.minTemp
                    val maxTemp = daySummary.maxTemp
                    if (minTemp != null || maxTemp != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            minTemp?.let {
                                Text(
                                    text = strings.dayMinTemp(formatDecimal(it)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            maxTemp?.let {
                                Text(
                                    text = strings.dayMaxTemp(formatDecimal(it)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Dominant wind direction
                    daySummary.windDirection?.let { degrees ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.dayWindDirection(degreesToCompass(degrees)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Rainfall
                    daySummary.precipitation?.let { precip ->
                        if (precip > 0.0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.dayRainfall(formatDecimal(precip)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Sunrise / Sunset row
                    val sunrise = daySummary.sunrise
                    val sunset = daySummary.sunset
                    if (sunrise != null || sunset != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            sunrise?.let {
                                Text(
                                    text = strings.daySunrise(it),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            sunset?.let {
                                Text(
                                    text = strings.daySunset(it),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Solar energy
                    daySummary.solarenergy?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = strings.solarEnergy(it.toString()), style = MaterialTheme.typography.bodySmall)
                    }

                    // Fog / Low visibility info
                    if (daySummary.isFoggy) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.VisibilityOff,
                                contentDescription = strings.lowVisibilityDesc,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.lowVisibilityHours(daySummary.foggyHours),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}