package ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.theme.rewinds
import androidx.compose.foundation.layout.size as layoutSize
import androidx.compose.material3.MaterialTheme

private data class IsobarRing(
    val radiusX: Float,
    val radiusY: Float,
    val rotationDeg: Float,
    val opacity: Float,
    val strokeWidth: Float
)

private val isobarRings = listOf(
    IsobarRing(410f, 296f,   0f, 0.40f, 11f),
    IsobarRing(324f, 233f,  28f, 0.52f, 11f),
    IsobarRing(239f, 171f,  56f, 0.65f, 10f),
    IsobarRing(154f, 108f,  84f, 0.78f,  9f),
    IsobarRing( 74f,  51f, 112f, 0.90f,  8f),
)

private const val CENTRE_RADIUS = 23f
private const val VIEWBOX = 1024f

/**
 * Small Canvas rendering of the ReWinds isobar spiral app icon.
 *
 * Five concentric rotated ellipses plus a filled centre dot, all in accentBlue
 * with increasing opacity toward the centre. Transparent background — the host
 * (e.g. AppHeader) supplies its own. No state, no ViewModel.
 */
@Composable
fun IsobarLogo(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val accent = MaterialTheme.rewinds.accentBlue

    Canvas(modifier = modifier.layoutSize(size)) {
        // Map the 1024 viewBox onto the actual composable bounds.
        val scale = this.size.minDimension / VIEWBOX
        val centerX = this.size.width / 2f
        val centerY = this.size.height / 2f

        for (ring in isobarRings) {
            val rx = ring.radiusX * scale
            val ry = ring.radiusY * scale
            val sw = ring.strokeWidth * scale

            rotate(degrees = ring.rotationDeg, pivot = Offset(centerX, centerY)) {
                drawOval(
                    color = accent.copy(alpha = ring.opacity),
                    topLeft = Offset(centerX - rx, centerY - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = sw)
                )
            }
        }

        drawCircle(
            color = accent,
            radius = CENTRE_RADIUS * scale,
            center = Offset(centerX, centerY)
        )
    }
}
