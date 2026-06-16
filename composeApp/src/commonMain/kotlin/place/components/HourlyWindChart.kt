package place.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import core.LocalAppStrings
import core.degreesToCompass
import core.utils.formatDecimal
import place.HOURLY_WIND_SLOT_COUNT
import place.HourlyWindPoint
import place.WINDOW_START_HOUR
import place.windFlowBearing
import place.yAxisTicks
import ui.theme.rewinds

private val CHART_HEIGHT = 180.dp
private val GRID_LINE_COUNT = 4
private val ARROW_VISUAL = 12.dp
private val ARROW_TOUCH = 18.dp
private val LEGEND_SWATCH = 8.dp

/** Left gutter reserved for y-axis labels; the plot area and the rows below are inset by this. */
private val Y_AXIS_WIDTH = 32.dp

/**
 * Dual-series line chart of hourly wind speed and gusts for one day, over the 09:00–21:00 window.
 *
 * Speed and gust polylines are drawn on a transparent Canvas (the sheet surface shows through),
 * with horizontal grid lines. A y-axis scale sits in a reserved left gutter ([Y_AXIS_WIDTH]); the
 * plot and the rows below are inset by that gutter so every column stays aligned. Below the chart
 * sit the hour labels and a per-hour wind-direction arrow row, then a two-item legend. The y-axis is
 * normalised against a rounded ceiling (see [yAxisTicks]) at or above the highest gust, so both
 * series share a scale and the top gridline carries a round value.
 *
 * [unitLabel] is the speed unit shown once on the top y-axis label (km/h today). It is a parameter
 * rather than hardcoded so a future user unit preference can be threaded in without touching the
 * chart internals.
 *
 * The x-axis is a fixed 13-slot grid spanning 09:00–21:00, regardless of how many points exist.
 * Each [HourlyWindPoint] is placed in the slot matching its local hour; slots without data stay
 * empty and break the line (no interpolation, no fabricated samples). This keeps a 6-hour partial
 * day occupying the same horizontal positions as a full day rather than stretching to fill the width.
 *
 * Renders only the available [points] — the caller is responsible for the empty and loading states;
 * this composable assumes [points] is non-empty. All business logic (the local-time window filter)
 * lives in the ViewModel, not here.
 */
@Composable
fun HourlyWindChart(
    date: String,
    points: List<HourlyWindPoint>,
    modifier: Modifier = Modifier,
    unitLabel: String = "km/h"
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
    // Shared y-scale: gusts are always ≥ speed, so the highest gust drives the data ceiling.
    val dataMax = maxOf(maxGust, maxSpeed)
    // Round the ceiling up to a nice tick value so the top gridline carries a round label and the
    // series normalises against that same value — keeping the highest point on, not above, the top line.
    val ticks = yAxisTicks(dataMax)
    val yMax = ticks.first()

    val chartDesc = strings.hourlyWindChartDesc(
        date,
        formatDecimal(minSpeed),
        formatDecimal(maxSpeed),
        formatDecimal(maxGust)
    )

    // Fixed 13-slot frame: index 0 == 09:00 … index 12 == 21:00. A point lands in the slot matching
    // its local hour; unfilled slots stay null so the axis width never depends on how many hours exist.
    val slots = arrayOfNulls<HourlyWindPoint>(HOURLY_WIND_SLOT_COUNT)
    points.forEach { point ->
        val slot = point.hour - WINDOW_START_HOUR
        if (slot in slots.indices) slots[slot] = point
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Unit shown once above the gutter so it never wraps against, or overlaps, the top tick.
        Text(
            text = unitLabel,
            style = MaterialTheme.typography.labelSmall,
            color = axisColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(Y_AXIS_WIDTH).padding(end = 4.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))

        // Chart row: y-axis labels in the reserved left gutter, plot Canvas inset to its right.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT)
        ) {
            YAxisLabels(
                ticks = ticks,
                color = axisColor,
                modifier = Modifier
                    .width(Y_AXIS_WIDTH)
                    .height(CHART_HEIGHT)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CHART_HEIGHT)
                    .padding(start = Y_AXIS_WIDTH)
                    .semantics { contentDescription = chartDesc }
            ) {
                drawGrid(gridColor)
                drawSeries(slots.map { it?.windspeed }, yMax, speedColor)
                drawSeries(slots.map { it?.windgust }, yMax, gustColor)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Hour labels on the fixed 13-slot grid: the slot's own label, or its frame hour if empty.
        // Inset by the y-axis gutter so they align under the plot area, not the full canvas width.
        Row(modifier = Modifier.fillMaxWidth().padding(start = Y_AXIS_WIDTH)) {
            slots.forEachIndexed { index, point ->
                val label = point?.label ?: (WINDOW_START_HOUR + index).toString().padStart(2, '0')
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = axisColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Wind-direction arrows on the same 13-slot grid; empty slots draw no arrow but keep alignment.
        // Same y-axis inset as the labels and plot so every column stays vertically aligned.
        Row(modifier = Modifier.fillMaxWidth().padding(start = Y_AXIS_WIDTH)) {
            slots.forEach { point ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    point?.winddir?.let { degrees ->
                        // `winddir` is meteorological: the direction the wind blows *from* (e.g. a
                        // West wind is 270°). The Navigation arrow points North (up) at 0°, so we
                        // show the *flow* direction the wind blows toward — matching the convention
                        // users expect from wind apps (W wind → arrow points East).
                        val flowDegrees = windFlowBearing(degrees)
                        Icon(
                            imageVector = Icons.Filled.Navigation,
                            contentDescription = strings.windDirectionDesc(degreesToCompass(degrees)),
                            tint = arrowTint,
                            modifier = Modifier
                                .size(ARROW_TOUCH)
                                .padding((ARROW_TOUCH - ARROW_VISUAL) / 2)
                                .rotate(flowDegrees)
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

/**
 * Y-axis scale in the reserved left gutter: tick values (high to low) right-aligned against the
 * plot edge, each label centred on the exact y-coordinate of its gridline in the plot area.
 *
 * [ticks] come from [yAxisTicks] ordered high-to-low. Rather than relying on even [Arrangement]
 * spacing — which drifts because it ignores text height and gives no anchor to the Canvas plot
 * area — each label is positioned with the same normalisation the data points use:
 * `y = plotHeight * (1 - value / yMax)`. The label is then vertically centred on that y so its
 * middle sits on the gridline, matching what the eye expects.
 *
 * Tick values are always whole-number ceilings (see [yAxisTicks]), so they render as integers
 * with no decimal point. The unit is rendered separately above the gutter by the caller, so it
 * never overlaps or wraps against the top tick.
 */
@Composable
private fun YAxisLabels(
    ticks: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val yMax = ticks.firstOrNull() ?: return
    val labelHeight = with(LocalDensity.current) {
        MaterialTheme.typography.labelSmall.lineHeight.toDp()
    }

    BoxWithConstraints(
        modifier = modifier.padding(end = 4.dp)
    ) {
        val plotHeight = maxHeight

        ticks.forEach { value ->
            val gridlineY = plotHeight * (1f - (value / yMax).toFloat())
            // Centre the label text on the gridline by lifting it half its own height.
            val labelTop = (gridlineY - labelHeight / 2).coerceIn(0.dp, plotHeight - labelHeight)
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = labelTop)
            )
        }
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
 * Draws one series as a connected polyline over the fixed-slot grid. [values] is one entry per
 * x-axis slot (13 of them); null entries break the line, so partial data leaves gaps in place
 * rather than interpolating or stretching to fill the width.
 */
private fun DrawScope.drawSeries(values: List<Double?>, yMax: Double, color: Color) {
    if (values.isEmpty() || yMax <= 0.0) return

    // One equal-width column per slot, so points stay aligned with the labels and arrows below.
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
