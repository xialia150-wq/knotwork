package app.knotwork.design.components.pipelineeditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/** Paints the editor's cubic connection and its optional tangent-aligned arrow. */
fun DrawScope.drawCanvasEdge(
    start: Offset,
    control1: Offset,
    control2: Offset,
    end: Offset,
    color: Color,
    width: Float,
    highlighted: Boolean,
    portScale: Float = 1f,
) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)
    }
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round))
    if (highlighted) {
        val dx = end.x - control2.x
        val dy = end.y - control2.y
        val length = sqrt(dx * dx + dy * dy)
        if (length > 0f) {
            val ux = dx / length
            val uy = dy / length
            val size = width * ARROW_LENGTH_FACTOR
            val setback = NodePortGeometry.DOT_RADIUS.dp.toPx() * portScale + width
            val tipX = end.x - ux * setback
            val tipY = end.y - uy * setback
            val arrow = Path().apply {
                moveTo(tipX, tipY)
                lineTo(
                    tipX - ux * size - uy * size * ARROW_HALF_WIDTH_FACTOR,
                    tipY - uy * size + ux * size * ARROW_HALF_WIDTH_FACTOR,
                )
                lineTo(
                    tipX - ux * size + uy * size * ARROW_HALF_WIDTH_FACTOR,
                    tipY - uy * size - ux * size * ARROW_HALF_WIDTH_FACTOR,
                )
                close()
            }
            drawPath(arrow, color)
        }
    }
}

private const val ARROW_LENGTH_FACTOR = 2.5f
private const val ARROW_HALF_WIDTH_FACTOR = 0.45f
