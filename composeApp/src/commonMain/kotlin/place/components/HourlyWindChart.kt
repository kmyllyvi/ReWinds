package place.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import core.LocalAppStrings
import core.degreesToCompass
import core.utils.formatDecimal
import place.HourlyWindPoint
import ui.theme.rewinds

private val CHART_HEIGHT = 180.dp
private val GRID_LINE_COUNT = 4
private val ARROW_VISUAL = 12.dp
private val ARROW_TOUCH = 18.dp
private val LEGEND_SWATCH = 8.dp

/**
 * Dual-series line chart of hourly wind speed and gusts for one day, over the 09:00–21:00 window.
 *
 * Speed and gust polylines are drawn on a transparent Canvas (the sheet surface shows through),
 * with horizontal grid lines. Below the chart sit the hour labels and a per-hour wind-direction
 * arrow row, then a two-item legend. The y-axis is normalised against the highest gust so both
 * series share a scale.
 *
 * Renders only the available [points] — no interpolation, no fabricated samples. The caller is
 * responsible for the empty and loading states; this composable assumes [points] is non-empty.
 * All business logic (the local-time window filter) lives in the ViewModel, not here.
 */
@Composable
fun HourlyWindChart(
    date: String,
    points: List<HourlyWindPoint>,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val speedColor = MaterialTheme.rewinds.accentBlue
    val gustColor = MaterialTheme.rewinds.attention
    val gridColor = MaterialTheme.rewinds.border
    val axisColor = MaterialTheme.rewinds.textSecondary
    val arrowTint = MaterialTheme.rewinds.textTertiary
    val labelColor = MaterialTheme.rewinds.textPrimary

    val speeds = points.mapNotNull { it.windspeed }
    val gusts = points.mapNotNull { it.windgust }
    val minSpeed = speeds.minOrNull() ?: 0.0
    val maxSpeed = speeds.maxOrNull() ?: 0.0
    val maxGust = gusts.maxOrNull() ?: 0.0
    // Shared y-scale: gusts are always ≥ speed, so the highest gust is the ceiling.
    val yMax = maxOf(maxGust, maxSpeed)

    val chartDesc = strings.hourlyWindChartDesc(
        date,
        formatDecimal(minSpeed),
        formatDecimal(maxSpeed),
        formatDecimal(maxGust)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT)
                .semantics { contentDescription = chartDesc }
        ) {
            drawGrid(gridColor)
            drawSeries(points.map { it.windspeed }, yMax, speedColor)
            drawSeries(points.map { it.windgust }, yMax, gustColor)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Hour labels aligned to the data columns (one equal-weight cell per point).
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = axisColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Wind-direction arrows below the x-axis, one per hour, rotated by raw bearing.
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { point ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    point.winddir?.let { degrees ->
                        Icon(
                            imageVector = Icons.Filled.Navigation,
                            contentDescription = strings.windDirectionDesc(degreesToCompass(degrees)),
                            tint = arrowTint,
                            modifier = Modifier
                                .size(ARROW_TOUCH)
                                .padding((ARROW_TOUCH - ARROW_VISUAL) / 2)
                                .rotate(degrees.toFloat())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LegendRow(
            speedColor = speedColor,
            gustColor = gustColor,
            speedLabel = strings.hourlyWindSpeedLegend,
            gustLabel = strings.hourlyWindGustLegend,
            labelColor = labelColor
        )
    }
}

/** Two filled-circle swatch + label items, in the speed and gust series colours. */
@Composable
private fun LegendRow(
    speedColor: Color,
    gustColor: Color,
    speedLabel: String,
    gustLabel: String,
    labelColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LegendItem(speedColor, speedLabel, labelColor)
        LegendItem(gustColor, gustLabel, labelColor)
    }
}

@Composable
private fun LegendItem(swatch: Color, label: String, labelColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(LEGEND_SWATCH)
                .clip(CircleShape)
                .background(swatch)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor
        )
    }
}

/** Evenly-spaced horizontal grid lines spanning the full chart width. */
private fun DrawScope.drawGrid(color: Color) {
    val step = size.height / GRID_LINE_COUNT
    for (i in 0..GRID_LINE_COUNT) {
        val y = step * i
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
    }
}

/**
 * Draws one series as a connected polyline. Null samples break the line — segments are only drawn
 * between consecutive non-null points, so partial data leaves gaps rather than interpolating.
 */
private fun DrawScope.drawSeries(values: List<Double?>, yMax: Double, color: Color) {
    if (values.isEmpty() || yMax <= 0.0) return

    // Each point sits at the centre of its equal-width column to align with the labels below.
    val columnWidth = size.width / values.size
    fun xAt(index: Int) = columnWidth * index + columnWidth / 2f
    fun yAt(value: Double) = size.height * (1f - (value / yMax).toFloat().coerceIn(0f, 1f))

    val path = Path()
    var penDown = false
    values.forEachIndexed { index, value ->
        if (value == null) {
            penDown = false
            return@forEachIndexed
        }
        val x = xAt(index)
        val y = yAt(value)
        if (penDown) path.lineTo(x, y) else path.moveTo(x, y)
        penDown = true
    }
    drawPath(path = path, color = color, style = Stroke(width = 3f))
}
