package place

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import core.MonthlyStatisticsRoute
import core.utils.monthName
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import place.components.DaySummaryRow
import kotlin.math.roundToInt
import components.AppHeader

// Helper function to format temperature consistently
private fun formatTemperature(value: Double?): String {
    if (value == null) return "--"
    return "${(value * 10).roundToInt() / 10.0}°C"
}

@Composable
fun MonthlyStatisticsView(
    placeName: String,
    year: Int,
    month: Int, // 1-12
    onBackClick: () -> Unit,
    vm: MonthlyStatisticsViewModel = koinViewModel {
        parametersOf(MonthlyStatisticsRoute(placeName, year, month))
    }
) {
    val statistics by vm.statistics.collectAsState()
    val dailySummaries by vm.dailySummaries.collectAsState()
    val isDownloading by vm.isDownloading.collectAsState()

    key(year, month) {
        // Reload data when month or year changes
        LaunchedEffect(year, month) {
            vm.reloadStatistics(year = year, month = month)
        }

        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = "$placeName - ${monthName(month)} $year Stats",
                onBackClick = onBackClick
            )
            Box(
                modifier = Modifier
                    .padding(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 16.dp)
                    .fillMaxSize()
            ) {
                val currentStats = statistics
                if (currentStats == null) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text("Monthly Summary", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Show download button if data is incomplete
                            val missingDaysCount = vm.getMissingDaysCount(dailySummaries)
                            if (missingDaysCount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                                        8.dp
                                    )
                                ) {
                                    Button(
                                        onClick = { vm.downloadFullMonth() },
                                        enabled = !isDownloading
                                    ) {
                                        if (isDownloading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier
                                                    .width(16.dp)
                                                    .height(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(if (isDownloading) "Downloading..." else "Download $missingDaysCount missing day${if (missingDaysCount > 1) "s" else ""}")
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (currentStats.numberOfDaysWithData > 0) {
                                // Display Kiteable Days count prominently
                                Text(
                                    text = "Kiteable Days: ${currentStats.kiteableDaysCount}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("General Stats", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Days with data: ${currentStats.numberOfDaysWithData}", color = MaterialTheme.colorScheme.onSurface)
                                currentStats.averageMinTemp?.let {
                                    Text(
                                        "Average Min Temp: ${
                                            formatTemperature(
                                                it
                                            )
                                        }",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                currentStats.averageMaxTemp?.let {
                                    Text(
                                        "Average Max Temp: ${
                                            formatTemperature(
                                                it
                                            )
                                        }",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                currentStats.overallAverageTemp?.let {
                                    Text(
                                        "Overall Average Temp: ${
                                            formatTemperature(
                                                it
                                            )
                                        }",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                currentStats.absoluteMinTemp?.let {
                                    Text(
                                        "Coldest Day: ${
                                            formatTemperature(
                                                it
                                            )
                                        } (on ${currentStats.coldestDate})",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                currentStats.absoluteMaxTemp?.let {
                                    Text(
                                        "Hottest Day: ${
                                            formatTemperature(
                                                it
                                            )
                                        } (on ${currentStats.hottestDate})",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                currentStats.totalSolarEnergy?.let { Text("Total Solar Energy: ${it.roundToInt()} kWh/m²", color = MaterialTheme.colorScheme.onSurface) }
                            } else {
                                Text("No detailed weather data available for calculations in this month, or data is still loading.", color = MaterialTheme.colorScheme.onSurface)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Daily Breakdown", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(dailySummaries) { daySummary ->
                            DaySummaryRow(daySummary)
                        }
                    }
                }
            }
        }
    }
}
