package ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Archive-box icon (lid + body with a front pull-slot), built in-house because the core
 * Material icon set bundled here has no archive glyph and pulling in the extended icon
 * dependency for a single icon isn't worth it. Drawn as strokes so the pull-slot reads as
 * a cut-out without winding-rule tricks. Deliberately distinct from the trash/Delete icon
 * used elsewhere — archiving retains data, it does not delete.
 */
val ArchiveBoxIcon: ImageVector by lazy {
    val stroke = SolidColor(Color.Black)
    ImageVector.Builder(
        name = "ArchiveBox",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Lid across the top.
        path(
            stroke = stroke,
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 4.5f)
            horizontalLineTo(21f)
            verticalLineTo(8f)
            horizontalLineTo(3f)
            close()
        }
        // Body below the lid.
        path(
            stroke = stroke,
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4.5f, 8f)
            verticalLineTo(19.5f)
            horizontalLineTo(19.5f)
            verticalLineTo(8f)
        }
        // Front pull-slot.
        path(
            stroke = stroke,
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(9.5f, 11.5f)
            horizontalLineTo(14.5f)
        }
    }.build()
}
