package ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import ui.theme.rewinds

private data class IsobarEllipse(
    val radiusX: Float,
    val radiusY: Float,
    val rotationDeg: Float,
    val strokeWidth: Float
)

private val isobarEllipses = listOf(
    IsobarEllipse(410f, 296f,   0f, 9f),
    IsobarEllipse(324f, 233f,  28f, 9f),
    IsobarEllipse(239f, 171f,  56f, 8f),
    IsobarEllipse(154f, 108f,  84f, 7f),
    IsobarEllipse( 74f,  51f, 112f, 6f),
)

private const val VIEWBOX = 1024f
private const val CONTAINER_DP = 680f

/**
 * Centred decorative background that renders the ReWinds isobar spiral.
 *
 * Spiral is centred in the composable bounds so it stays within the content
 * area and never bleeds into system chrome. Stroke colour is accentBlue at 6% opacity.
 * No state, no ViewModel.
 */
@Composable
fun IsobarBackground(modifier: Modifier = Modifier) {
    val strokeColor = MaterialTheme.rewinds.accentBlue.copy(alpha = 0.06f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val scale = (CONTAINER_DP / VIEWBOX) * density
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        for (ellipse in isobarEllipses) {
            val rx = ellipse.radiusX * scale
            val ry = ellipse.radiusY * scale
            val sw = ellipse.strokeWidth * scale

            rotate(degrees = ellipse.rotationDeg, pivot = Offset(centerX, centerY)) {
                drawOval(
                    color = strokeColor,
                    topLeft = Offset(centerX - rx, centerY - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = sw)
                )
            }
        }
    }
}
