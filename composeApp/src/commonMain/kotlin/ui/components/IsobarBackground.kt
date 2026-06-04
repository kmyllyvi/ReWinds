package ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import ui.theme.rewinds

// Design reference: docs/designs/screens-v1.html
// The SVG symbol "iso" defines five concentric rotated ellipses in a 1024×1024 viewbox.
// The container is positioned at top: -480px, right: -415px at 980×980 scale,
// placing the spiral eye off-screen top-right while the outer curves sweep across the screen.

private data class IsobarEllipse(
    val radiusX: Float,   // semi-major axis in viewbox units (1024 space)
    val radiusY: Float,   // semi-minor axis
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

// Viewbox is 1024×1024 with ellipses centred at (512, 512).
private const val VIEWBOX = 1024f

// At 980dp container size the scale factor applied in the design.
private const val CONTAINER_DP = 980f

/**
 * Full-screen decorative background that renders the ReWinds isobar spiral.
 *
 * The spiral is drawn large enough that its centre/eye sits off-screen top-right;
 * only the outer flowing curves are visible. Stroke colour is [MaterialTheme.rewinds.accentBlue]
 * at 6 % opacity. No state, no ViewModel.
 *
 * Place as the bottom layer of a [Box] behind screen content.
 */
@Composable
fun IsobarBackground(modifier: Modifier = Modifier) {
    val strokeColor = MaterialTheme.rewinds.accentBlue.copy(alpha = 0.06f)

    Canvas(modifier = modifier.fillMaxSize()) {
        drawIsobars(strokeColor)
    }
}

private fun DrawScope.drawIsobars(strokeColor: androidx.compose.ui.graphics.Color) {
    val screenWidth = size.width
    val screenHeight = size.height

    // Scale the 1024-unit viewbox to CONTAINER_DP-equivalent pixels.
    // Using the screen's own density: 1 dp = density px, so CONTAINER_DP dp = CONTAINER_DP * density px.
    // We work in px directly since Canvas operates in px.
    val scale = (CONTAINER_DP / VIEWBOX) * density

    // Container top-left position in px:
    //   right: -415dp  →  containerLeft = screenWidth + 415dp_in_px - containerWidth_px
    //   top:  -480dp   →  containerTop  = -480dp_in_px
    val containerWidth  = CONTAINER_DP * density
    val containerHeight = CONTAINER_DP * density
    val containerLeft   = screenWidth  + (415f * density) - containerWidth
    val containerTop    = -(480f * density)

    // Ellipse centre in px (viewbox centre 512,512 mapped to container space)
    val centerX = containerLeft  + (512f / VIEWBOX) * containerWidth
    val centerY = containerTop   + (512f / VIEWBOX) * containerHeight

    for (ellipse in isobarEllipses) {
        val rx = ellipse.radiusX * scale
        val ry = ellipse.radiusY * scale
        val sw = ellipse.strokeWidth * scale

        // Rotate around the shared ellipse centre then draw.
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
