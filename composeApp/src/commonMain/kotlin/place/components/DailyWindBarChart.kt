package place.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import place.DayWeatherSummary
import ui.theme.rewinds
import androidx.compose.material3.MaterialTheme

private val BAR_WIDTH = 10.dp
private val BAR_GAP = 4.dp
private val CHART_HEIGHT = 120.dp

/**
 * Horizontal-scrolling Canvas bar chart of per-day sustained wind speed.
 *
 * One vertical bar per [DayWeatherSummary]; heights are normalised against the
 * month's peak value. The bar at [peakIndex] is drawn in the [attention] colour,
 * all others in [accentBlue]. Peak selection is decided by the ViewModel and
 * passed in — this composable performs no business logic.
 */
@Composable
fun DailyWindBarChart(
    summaries: List<DayWeatherSummary>,
    peakIndex: Int,
    modifier: Modifier = Modifier
) {
    if (summaries.isEmpty()) return

    val accent = MaterialTheme.rewinds.accentBlue
    val attention = MaterialTheme.rewinds.attention
    val track = MaterialTheme.rewinds.accentBlue.copy(alpha = 0.1f)

    val maxWind = summaries.mapNotNull { it.sustainedWindSpeed }.maxOrNull() ?: 0.0
    // Total canvas width so every bar gets a slot and the row can scroll.
    val chartWidth = (BAR_WIDTH + BAR_GAP) * summaries.size

    Canvas(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .height(CHART_HEIGHT)
            .width(chartWidth)
            .padding(vertical = 4.dp)
    ) {
        val bw = BAR_WIDTH.toPx()
        val gap = BAR_GAP.toPx()
        val slot = bw + gap
        val corner = CornerRadius(bw / 2f, bw / 2f)

        summaries.forEachIndexed { index, day ->
            val x = index * slot
            val wind = day.sustainedWindSpeed ?: 0.0
            val fraction = if (maxWind > 0.0) (wind / maxWind).toFloat().coerceIn(0f, 1f) else 0f
            val barHeight = size.height * fraction

            // Faint full-height track so empty days still register on the axis.
            drawRoundRect(
                color = track,
                topLeft = Offset(x, 0f),
                size = Size(bw, size.height),
                cornerRadius = corner
            )

            if (barHeight > 0f) {
                drawRoundRect(
                    color = if (index == peakIndex) attention else accent,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(bw, barHeight),
                    cornerRadius = corner
                )
            }
        }
    }
}
