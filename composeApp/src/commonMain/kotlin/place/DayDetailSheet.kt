package place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import core.DaysOfInterestFilter
import core.KiteSpotterConfig
import core.LocalAppStrings
import core.utils.shortDayLabel
import place.components.HourlyWindChart
import ui.theme.rewinds

private val CHART_AREA_HEIGHT = 180.dp

/**
 * Bottom-sheet showing the hourly wind detail for a single day.
 *
 * Visibility is driven entirely by [day]: this composable is only placed in the tree when a day is
 * selected, and a swipe-down / scrim tap routes through [onDismiss] back to the ViewModel (MV*).
 * Content has three states — loading (spinner), no data (centred label), and the hourly chart.
 *
 * @param day The selected day's summary; supplies the header date and chart [contentDescription].
 * @param hours The 09:00–21:00 local-time points resolved by the ViewModel; empty means no data.
 * @param isLoading True while [hours] is still being resolved.
 * @param activeFilter The preferred-day filter, used to shade hours meeting the wind criteria. Null
 *   (or a filter with no `minWindSpeedKmh`) renders the chart without shading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(
    day: DayWeatherSummary,
    hours: List<HourlyWindPoint>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    activeFilter: DaysOfInterestFilter? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateLabel = shortDayLabel(day.date)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.rewinds.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.rewinds.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> ChartLoading()
                hours.isEmpty() -> EmptyHourlyData(dateLabel = dateLabel)
                else -> HourlyWindChart(
                    date = dateLabel,
                    points = hours,
                    shadingTiers = criteriaShadingFor(hours, activeFilter)
                )
            }
        }
    }
}

/**
 * Builds the per-slot [ShadingTier] list for [hours] against [filter]'s wind criteria, aligned to
 * the chart's fixed 09:00–21:00 slot frame.
 *
 * Returns an empty list (no shading) when there is no filter or no `minWindSpeedKmh`. A null
 * `sustainedWindHours` falls back to [KiteSpotterConfig.SUSTAINED_WIND_WINDOW_HOURS], matching the
 * sustained-window default the ViewModel uses for [calculateMaxSustainedWindSpeed].
 */
private fun criteriaShadingFor(
    hours: List<HourlyWindPoint>,
    filter: DaysOfInterestFilter?
): List<ShadingTier> {
    val minSpeed = filter?.minWindSpeedKmh ?: return emptyList()
    val sustainedSlots = filter.sustainedWindHours ?: KiteSpotterConfig.SUSTAINED_WIND_WINDOW_HOURS
    return criteriaShading(hourlyWindSlots(hours), minSpeed, sustainedSlots)
}

@Composable
private fun ChartLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHART_AREA_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.rewinds.accentBlue)
    }
}

@Composable
private fun EmptyHourlyData(dateLabel: String) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHART_AREA_HEIGHT),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = strings.noHourlyData,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.rewinds.textTertiary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.rewinds.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
