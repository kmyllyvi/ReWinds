package place

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.LocalAppStrings
import core.MonthlyStatisticsRoute
import core.TestTags
import core.isIOS
import core.utils.formatDecimal
import core.utils.monthName
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import place.components.DailyWindBarChart
import place.components.DaySummaryRow
import place.components.MonthStatCard
import ui.components.IsobarBackground
import ui.theme.rewinds
import kotlin.math.roundToInt

// Rounds to one decimal place for display; KMP-safe (no String.format).
private fun formatTempValue(value: Double?): String {
    if (value == null) return "--"
    return formatDecimal((value * 10).roundToInt() / 10.0)
}

private fun formatWholeNumber(value: Double?): String {
    if (value == null) return "--"
    return value.roundToInt().toString()
}

@Composable
fun MonthlyStatisticsView(
    placeName: String,
    year: Int,
    month: Int, // 1-12
    onBackClick: () -> Unit,
    vm: MonthlyStatisticsViewModel = koinViewModel(key = placeName) {
        parametersOf(MonthlyStatisticsRoute(placeName, year, month))
    }
) {
    val statistics by vm.statistics.collectAsState()
    val dailySummaries by vm.dailySummaries.collectAsState()
    val isDownloading by vm.isDownloading.collectAsState()
    val currentYear by vm.year.collectAsState()
    val currentMonth by vm.month.collectAsState()
    val peakWindDayIndex by vm.peakWindDayIndex.collectAsState()
    val selectedDay by vm.selectedDay.collectAsState()
    val selectedDayHours by vm.selectedDayHours.collectAsState()
    val isLoadingHours by vm.isLoadingHours.collectAsState()
    val strings = LocalAppStrings.current

    // No LaunchedEffect to (re)load here: the ViewModel's init already loads the initial
    // month, and month navigation is driven by navigateToPreviousMonth/navigateToNextMonth.
    // A LaunchedEffect(placeName, year, month) double-fired on first entry (KIM-278).

    Box(modifier = Modifier.fillMaxSize()) {
        // Decorative isobar background — rendered first so it sits below all content.
        IsobarBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            MonthSummaryHeader(
                placeName = placeName,
                monthTitle = "${monthName(currentMonth)} $currentYear",
                daysRecorded = dailySummaries.size,
                onBackClick = onBackClick,
                onPreviousMonth = { vm.navigateToPreviousMonth() },
                onNextMonth = { vm.navigateToNextMonth() }
            )

            val currentStats = statistics
            if (dailySummaries.isEmpty() && currentStats == null) {
                // Nothing loaded yet — show skeleton day rows for immediate feedback
                // instead of blocking the whole screen on a single spinner (KIM-278).
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    repeat(3) {
                        DaySummaryRowSkeleton()
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TestTags.MONTH_DAY_LIST)
                        .padding(horizontal = 12.dp)
                ) {
                    item {
                        val missingDaysCount = vm.getMissingDaysCount(dailySummaries)
                        if (missingDaysCount > 0) {
                            DownloadMissingDaysRow(
                                count = missingDaysCount,
                                isDownloading = isDownloading,
                                onDownload = { vm.downloadFullMonth() }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Progressive render: day rows are already available, but the stats
                        // card waits on the (cheap) aggregate calculation. Show a spinner only
                        // where the card will land rather than holding back the whole list.
                        if (currentStats == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = strings.dailyBreakdown,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.rewinds.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        } else if (currentStats.numberOfDaysWithData > 0) {
                            StatCardGrid(stats = currentStats)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = strings.dailyWind,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.rewinds.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            DailyWindBarChart(
                                summaries = dailySummaries,
                                peakIndex = peakWindDayIndex
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = strings.dailyBreakdown,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.rewinds.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            Text(
                                text = strings.noWeatherData,
                                color = MaterialTheme.rewinds.textSecondary
                            )
                        }
                    }

                    items(dailySummaries) { daySummary ->
                        DaySummaryRow(daySummary, onClick = { vm.selectDay(daySummary) })
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        selectedDay?.let { day ->
            DayDetailSheet(
                day = day,
                hours = selectedDayHours,
                isLoading = isLoadingHours,
                onDismiss = { vm.dismissDaySheet() }
            )
        }
    }
}

@Composable
private fun MonthSummaryHeader(
    placeName: String,
    monthTitle: String,
    daysRecorded: Int,
    onBackClick: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val strings = LocalAppStrings.current
    val topMargin = if (isIOS()) 0.dp else 35.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = topMargin, end = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = MaterialTheme.rewinds.textPrimary
                    )
                }
                Text(
                    text = placeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.rewinds.textSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.testTag(TestTags.MONTH_PREVIOUS_BUTTON)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = strings.previousMonth,
                        tint = MaterialTheme.rewinds.accentBlue
                    )
                }
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.testTag(TestTags.MONTH_NEXT_BUTTON)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = strings.nextMonth,
                        tint = MaterialTheme.rewinds.accentBlue
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
            Text(
                text = monthTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.rewinds.textPrimary
            )
            Text(
                text = "$placeName · ${strings.daysRecorded(daysRecorded)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.rewinds.textSecondary
            )
        }
    }
}

@Composable
private fun StatCardGrid(stats: CalculatedStats) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier.testTag(TestTags.MONTH_STAT_CARD_GRID),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MonthStatCard(
                label = strings.statTempLabel,
                value = strings.statTempValue(
                    formatTempValue(stats.averageMinTemp),
                    formatTempValue(stats.averageMaxTemp)
                ),
                unit = strings.unitCelsius,
                modifier = Modifier.weight(1f)
            )
            MonthStatCard(
                label = strings.statWindLabel,
                value = formatWholeNumber(stats.averageSustainedWindSpeed),
                unit = strings.unitKmh,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MonthStatCard(
                label = strings.statRainfallLabel,
                value = formatWholeNumber(stats.totalRainfall),
                unit = strings.unitMm,
                modifier = Modifier.weight(1f)
            )
            MonthStatCard(
                label = strings.statKiteableDaysLabel,
                value = stats.daysOfInterestCount.toString(),
                unit = strings.unitDays,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Muted placeholder shaped like a [DaySummaryRow], shown while the month's days load.
 * Static muted bars — no shimmer — matching the Home skeleton approach (KIM-278).
 */
@Composable
private fun DaySummaryRowSkeleton() {
    val placeholderColor = MaterialTheme.rewinds.textTertiary.copy(alpha = 0.18f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.rewinds.surface.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholderColor)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholderColor)
            )
        }
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(placeholderColor)
        )
    }
}

@Composable
private fun DownloadMissingDaysRow(
    count: Int,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onDownload, enabled = !isDownloading) {
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
                else strings.downloadMissingDays(count)
            )
        }
    }
}
