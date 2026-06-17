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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import place.HourlyWindPoint
import place.ShadingTier
import place.hourlyWindSlots
import place.thresholdYFraction
import place.WINDOW_START_HOUR
import place.windFlowBearing
import place.yAxisTicks
import ui.theme.rewinds

private val CHART_HEIGHT = 180.dp
private val GRID_LINE_COUNT = 4
private val ARROW_VISUAL = 12.dp
private val ARROW_TOUCH = 18.dp
private val LEGEND_SWATCH = 8.dp
private val LEGEND_RECT_WIDTH = 16.dp
private val LEGEND_RECT_HEIGHT = 8.dp
private val LEGEND_RECT_CORNER = 2.dp

// Criteria-shading opacities (KIM-305), applied to accentBlue. Tier 2 (sustained) reads stronger
// than Tier 1 (threshold); the top-edge stroke marks the qualifying block's upper boundary.
private const val TIER_THRESHOLD_ALPHA = 0.07f
private const val TIER_SUSTAINED_ALPHA = 0.18f
private const val TIER_SUSTAINED_EDGE_ALPHA = 0.55f

/** Physical thickness of the sustained block's top-edge stroke, resolved to px inside the Canvas. */
private val TIER_SUSTAINED_EDGE_WIDTH = 1.5.dp

// Threshold guide line (KIM-306): a faint dashed horizontal line at the user's wind criteria,
// drawn in accentBlue so it reads as the same "criteria" colour family as the shading.
private const val THRESHOLD_LINE_ALPHA = 0.40f
private val THRESHOLD_LINE_WIDTH = 1.dp
private val THRESHOLD_DASH_ON = 8f
private val THRESHOLD_DASH_OFF = 6f

/** Left gutter reserved for y-axis labels; the plot area and the rows below are inset by this. */
private val Y_AXIS_WIDTH = 40.dp

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
    unitLabel: String = "km/h",
    shadingTiers: List<ShadingTier> = emptyList(),
    minThresholdKmh: Double? = null,
    maxThresholdKmh: Double? = null
) {
    val strings = LocalAppStrings.current
    val speedColor = MaterialTheme.rewinds.accentBlue
    val gustColor = MaterialTheme.rewinds.attention
    val gridColor = MaterialTheme.rewinds.border
    val axisColor = MaterialTheme.rewinds.textSecondary
    val arrowTint = MaterialTheme.rewinds.textTertiary
    val labelColor = MaterialTheme.rewinds.textPrimary

    // Shading is active only when the caller supplied a tier per slot; otherwise the chart renders
    // exactly as before (no fills, no extra legend items).
    val showShading = shadingTiers.isNotEmpty()
    val thresholdFill = speedColor.copy(alpha = TIER_THRESHOLD_ALPHA)
    val sustainedFill = speedColor.copy(alpha = TIER_SUSTAINED_ALPHA)
    val sustainedEdge = speedColor.copy(alpha = TIER_SUSTAINED_EDGE_ALPHA)
    val thresholdLineColor = speedColor.copy(alpha = THRESHOLD_LINE_ALPHA)
    val thresholdLineWidth = with(LocalDensity.current) { THRESHOLD_LINE_WIDTH.toPx() }

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

    // Fixed 13-slot frame (09:00–21:00); shared with the shading input so columns stay index-aligned.
    val slots = hourlyWindSlots(points)

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
                // Shading first so grid lines and data series always render on top of it.
                if (showShading) {
                    drawColumnShading(shadingTiers, thresholdFill, sustainedFill, sustainedEdge)
                }
                // Threshold guides sit above shading but below the series, so data reads on top.
                drawThresholdLines(minThresholdKmh, maxThresholdKmh, yMax, thresholdLineColor, thresholdLineWidth)
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
            labelColor = labelColor,
            // Tier swatches only when shading is active — they explain marks the chart isn't drawing otherwise.
            sustainedColor = if (showShading) sustainedFill else null,
            thresholdColor = if (showShading) thresholdFill else null,
            sustainedLabel = strings.hourlySustainedWindowLegend,
            thresholdLabel = strings.hourlyMeetsThresholdLegend
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

/**
 * Series legend: two filled-circle swatches for the speed and gust lines, followed — when shading
 * is active ([sustainedColor]/[thresholdColor] non-null) — by two rounded-rectangle swatches for
 * the criteria-shading tiers. The tier swatches are omitted entirely when shading is off, so a
 * threshold-less filter renders the same legend as before.
 */
@Composable
private fun LegendRow(
    speedColor: Color,
    gustColor: Color,
    speedLabel: String,
    gustLabel: String,
    labelColor: Color,
    sustainedColor: Color?,
    thresholdColor: Color?,
    sustainedLabel: String,
    thresholdLabel: String
) {
    // Split across two rows so items wrap naturally instead of relying on horizontal scroll,
    // which gave children infinite width and broke Text measurement (letters stacked vertically).
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LegendItem(speedColor, speedLabel, labelColor)
            LegendItem(gustColor, gustLabel, labelColor)
        }
        if (sustainedColor != null || thresholdColor != null) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (sustainedColor != null) ShadingLegendItem(sustainedColor, sustainedLabel, labelColor)
                if (thresholdColor != null) ShadingLegendItem(thresholdColor, thresholdLabel, labelColor)
            }
        }
    }
}

/** Circle swatch + label for a data series. */
@Composable
private fun LegendItem(swatch: Color, label: String, labelColor: Color) {
    LegendRowItem(label, labelColor) {
        Box(
            modifier = Modifier
                .size(LEGEND_SWATCH)
                .clip(CircleShape)
                .background(swatch)
        )
    }
}

/** Rounded-rectangle swatch + label for a criteria-shading tier (KIM-305). */
@Composable
private fun ShadingLegendItem(swatch: Color, label: String, labelColor: Color) {
    LegendRowItem(label, labelColor) {
        Box(
            modifier = Modifier
                .size(width = LEGEND_RECT_WIDTH, height = LEGEND_RECT_HEIGHT)
                .clip(RoundedCornerShape(LEGEND_RECT_CORNER))
                .background(swatch)
        )
    }
}

/** Shared swatch-plus-label layout so series and tier legend items align identically. */
@Composable
private fun LegendRowItem(label: String, labelColor: Color, swatch: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        swatch()
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor
        )
    }
}

/**
 * Fills each slot's full-width, full-height column with its criteria-shading tier colour and draws
 * a single top-edge stroke spanning each contiguous run of sustained slots (KIM-305).
 *
 * [tiers] is one entry per x-axis slot, parallel to the chart's slot grid; its size sets the column
 * width so shading stays aligned with the series and hour labels. Threshold and sustained slots get
 * their fills; [ShadingTier.NONE] slots are left clear. Adjacent sustained slots share one stroke so
 * an unbroken band shows no interior seams.
 */
private fun DrawScope.drawColumnShading(
    tiers: List<ShadingTier>,
    thresholdFill: Color,
    sustainedFill: Color,
    sustainedEdge: Color
) {
    if (tiers.isEmpty()) return
    val columnWidth = size.width / tiers.size

    tiers.forEachIndexed { index, tier ->
        val fill = when (tier) {
            ShadingTier.THRESHOLD -> thresholdFill
            ShadingTier.SUSTAINED -> sustainedFill
            ShadingTier.NONE -> null
        } ?: return@forEachIndexed
        drawRect(
            color = fill,
            topLeft = Offset(columnWidth * index, 0f),
            size = Size(columnWidth, size.height)
        )
    }

    // One top-edge stroke per contiguous run of sustained slots, so a merged band has no seams.
    var runStart = -1
    fun flush(endExclusive: Int) {
        if (runStart < 0) return
        drawLine(
            color = sustainedEdge,
            start = Offset(columnWidth * runStart, 0f),
            end = Offset(columnWidth * endExclusive, 0f),
            strokeWidth = TIER_SUSTAINED_EDGE_WIDTH.toPx()
        )
        runStart = -1
    }
    tiers.forEachIndexed { index, tier ->
        if (tier == ShadingTier.SUSTAINED) {
            if (runStart < 0) runStart = index
        } else {
            flush(index)
        }
    }
    flush(tiers.size)
}

/**
 * Draws a full-width dashed horizontal line for each non-null threshold (KIM-306), so the user can
 * read which hours sit above or below their wind criteria without comparing the line to a number.
 *
 * Each value is normalised by [thresholdYFraction] against the same [yMax] the series use, so the
 * line aligns with the scale; a value above [yMax] clamps to the top edge rather than drawing off
 * the canvas. Drawn after shading but before the grid and series, so data always reads on top.
 */
private fun DrawScope.drawThresholdLines(
    minKmh: Double?,
    maxKmh: Double?,
    yMax: Double,
    color: Color,
    strokeWidth: Float
) {
    if (yMax <= 0.0) return
    val dash = PathEffect.dashPathEffect(floatArrayOf(THRESHOLD_DASH_ON, THRESHOLD_DASH_OFF), 0f)
    listOfNotNull(minKmh, maxKmh).forEach { value ->
        val y = size.height * thresholdYFraction(value, yMax)
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
            pathEffect = dash
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
