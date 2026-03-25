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
import core.LocalAppStrings
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
    return "${(value * 10).roundToInt() / 10.0}\u00B0C"
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
    val strings = LocalAppStrings.current

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
                            Text(strings.monthlySummary, style = MaterialTheme.typography.headlineSmall)
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
                                        Text(
                                            if (isDownloading) strings.downloading
                                            else strings.downloadMissingDays(missingDaysCount)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (currentStats.numberOfDaysWithData > 0) {
                                // Display Kiteable Days count prominently
                                Text(
                                    text = strings.daysOfInterest(currentStats.daysOfInterestCount),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (currentStats.filterSummary.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentStats.filterSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(strings.generalStats, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(strings.daysWithData(currentStats.numberOfDaysWithData.toString()), color = MaterialTheme.colorScheme.onSurface)
                                currentStats.averageMinTemp?.let {
                                    Text(
                                        strings.avgMinTemp(formatTemperature(it)),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                currentStats.averageMaxTemp?.let {
                                    Text(
                                        strings.avgMaxTemp(formatTemperature(it)),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                currentStats.overallAverageTemp?.let {
                                    Text(
                                        strings.overallAvgTemp(formatTemperature(it)),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                currentStats.absoluteMinTemp?.let {
                                    Text(
                                        strings.coldestDay(formatTemperature(it), currentStats.coldestDate ?: ""),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                currentStats.absoluteMaxTemp?.let {
                                    Text(
                                        strings.hottestDay(formatTemperature(it), currentStats.hottestDate ?: ""),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                currentStats.totalRainfall?.let {
                                    Text(
                                        strings.totalRainfall("${"%.1f".format(it)}"),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                currentStats.totalSolarEnergy?.let {
                                    Text(
                                        strings.totalSolarEnergy("${it.roundToInt()}"),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                Text(strings.noWeatherData, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(strings.dailyBreakdown, style = MaterialTheme.typography.titleMedium)
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
