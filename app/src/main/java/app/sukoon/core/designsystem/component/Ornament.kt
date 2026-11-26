package app.sukoon.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Faint eight-point-star (khatam) lattice. Purely decorative: used behind section headers only.
 */
@Composable
fun GeometricOrnament(
    color: Color,
    modifier: Modifier = Modifier,
    cell: Dp = 56.dp,
    strokeWidth: Dp = 1.dp,
) {
    Canvas(modifier) {
        val c = cell.toPx()
        val stroke = Stroke(width = strokeWidth.toPx())
        clipRect {
            var y = 0f
            var row = 0
            while (y < size.height + c) {
                var x = if (row % 2 == 0) 0f else c / 2f
                while (x < size.width + c) {
                    drawKhatam(Offset(x, y), c * 0.42f, color, stroke)
                    x += c
                }
                y += c * 0.86f
                row++
            }
        }
    }
}

private fun DrawScope.drawKhatam(center: Offset, radius: Float, color: Color, stroke: Stroke) {
    val path = Path()
    val points = 16
    for (i in 0 until points) {
        val r = if (i % 2 == 0) radius else radius * 0.62f
        val angle = (PI / 8 * i - PI / 2).toFloat()
        val px = center.x + r * cos(angle)
        val py = center.y + r * sin(angle)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color, style = stroke)
}

/** Small eight-point star used as a divider glyph. */
@Composable
fun StarGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val path = Path()
        for (i in 0 until 16) {
            val rr = if (i % 2 == 0) r else r * 0.5f
            val angle = (PI / 8 * i - PI / 2).toFloat()
            val px = center.x + rr * cos(angle)
            val py = center.y + rr * sin(angle)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        drawPath(path, color)
    }
}
