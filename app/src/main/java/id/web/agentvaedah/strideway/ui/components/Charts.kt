package id.web.agentvaedah.strideway.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.web.agentvaedah.strideway.core.model.LatLon
import id.web.agentvaedah.strideway.ui.theme.Palette
import kotlin.math.cos

data class Series(val values: List<Float>, val color: Color, val dashed: Boolean = false)

/**
 * Minimal multi-series line chart. All series share one y-range so their levels compare;
 * [zeroLine] draws a baseline when the range crosses zero.
 */
@Composable
fun LineChart(
    series: List<Series>,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    fillFirst: Boolean = false,
    zeroLine: Boolean = false,
    /** 0..1 along the x axis; draws a vertical playback cursor when set. */
    cursor: Float? = null,
) {
    val outline = Palette.Outline
    val cursorColor = Palette.Text
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description },
    ) {
        val all = series.flatMap { it.values }
        if (all.size < 2) return@Canvas
        var min = all.min()
        var max = all.max()
        if (max - min < 1e-3f) {
            min -= 1f
            max += 1f
        }
        val pad = 4.dp.toPx()
        val h = size.height - pad * 2
        fun y(v: Float) = pad + h - (v - min) / (max - min) * h

        if (zeroLine && min < 0 && max > 0) {
            drawLine(outline, Offset(0f, y(0f)), Offset(size.width, y(0f)), strokeWidth = 1.dp.toPx())
        }
        series.forEachIndexed { index, s ->
            if (s.values.size < 2) return@forEachIndexed
            val step = size.width / (s.values.size - 1)
            val pts = s.values.mapIndexed { i, v -> Offset(i * step, y(v)) }
            val path = smoothPath(pts)
            if (fillFirst && index == 0) {
                val fill = Path().apply {
                    addPath(path)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    fill,
                    Brush.verticalGradient(
                        colors = listOf(s.color.copy(alpha = 0.24f), s.color.copy(alpha = 0.0f)),
                        startY = pad,
                        endY = size.height,
                    ),
                )
            }
            drawPath(
                path,
                s.color,
                style = Stroke(
                    width = 2.4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = if (s.dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 6f)) else null,
                ),
            )
        }
        cursor?.let { c ->
            val x = c.coerceIn(0f, 1f) * size.width
            drawLine(cursorColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
        }
    }
}

/** Builds a smooth cubic Bezier path through the recorded samples. */
private fun smoothPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts.first().x, pts.first().y)
    if (pts.size == 2) {
        path.lineTo(pts[1].x, pts[1].y)
        return path
    }
    for (i in 0 until pts.lastIndex) {
        val p0 = pts[(i - 1).coerceAtLeast(0)]
        val p1 = pts[i]
        val p2 = pts[i + 1]
        val p3 = pts[(i + 2).coerceAtMost(pts.lastIndex)]
        val c1x = p1.x + (p2.x - p0.x) / 6f
        val c1y = p1.y + (p2.y - p0.y) / 6f
        val c2x = p2.x - (p3.x - p1.x) / 6f
        val c2y = p2.y - (p3.y - p1.y) / 6f
        path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
    }
    return path
}

/** Offline route sketch from stored coordinates; no map tiles, so the feed renders without network. */
@Composable
fun TrackThumbnail(path: List<LatLon>, modifier: Modifier = Modifier, color: Color = Palette.Accent) {
    val background = Palette.SurfaceHigh
    Canvas(modifier.semantics { contentDescription = "Sketsa jalur aktivitas" }) {
        drawRect(background)
        if (path.size < 2) return@Canvas
        val midLat = path.sumOf { it.lat } / path.size
        val kx = cos(Math.toRadians(midLat))
        val xs = path.map { it.lon * kx }
        val ys = path.map { it.lat }
        val minX = xs.min(); val maxX = xs.max()
        val minY = ys.min(); val maxY = ys.max()
        val spanX = (maxX - minX).coerceAtLeast(1e-9)
        val spanY = (maxY - minY).coerceAtLeast(1e-9)
        val pad = 10.dp.toPx()
        val scale = minOf((size.width - 2 * pad) / spanX, (size.height - 2 * pad) / spanY)
        val offX = (size.width - spanX * scale) / 2
        val offY = (size.height - spanY * scale) / 2
        val p = Path()
        path.forEachIndexed { i, _ ->
            val x = (offX + (xs[i] - minX) * scale).toFloat()
            val y = (offY + (maxY - ys[i]) * scale).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        drawPath(p, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
