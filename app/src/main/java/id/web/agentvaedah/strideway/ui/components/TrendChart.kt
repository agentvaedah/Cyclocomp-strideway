package id.web.agentvaedah.strideway.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.web.agentvaedah.strideway.ui.theme.MetricText
import id.web.agentvaedah.strideway.ui.theme.Palette
import kotlin.math.roundToInt

/** One line of a [TrendChart]; [area] fills underneath it, [dashed] marks a derived value. */
data class TrendSeries(
    val label: String,
    val values: List<Float>,
    val color: Color,
    val dashed: Boolean = false,
    val area: Boolean = false,
)

/**
 * Smooth cubic-Bezier multi-series trend chart with dashed hairlines, gradient area fill,
 * glowing active endpoints, and interactive tap/drag scrubbing.
 */
@Composable
fun TrendChart(
    series: List<TrendSeries>,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 156.dp,
    startLabel: String? = null,
    middleLabel: String? = null,
    endLabel: String? = null,
    detail: (@Composable (Int) -> Unit)? = null,
) {
    val grid = Palette.Outline.copy(alpha = 0.55f)
    val muted = Palette.TextMuted
    val surface = Palette.Surface
    val all = series.flatMap { it.values }
    if (all.size < 2) return
    val maxCount = series.maxOf { it.values.size }
    var scrubIndex by remember(series) { mutableStateOf<Int?>(null) }

    val rawMin = all.min()
    val rawMax = all.max()
    val span = (rawMax - rawMin).coerceAtLeast(1f)
    val step = niceStep(span / 2f)
    val min = (rawMin / step).toInt() * step - if (rawMin < 0 && rawMin % step != 0f) step else 0f
    val max = (kotlin.math.ceil(rawMax / step) * step).coerceAtLeast(min + step)
    val ticks = listOf(min, (min + max) / 2f, max).distinct()

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Column(
                Modifier.width(34.dp).height(height).padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                ticks.reversed().forEach { t ->
                    Text(
                        t.roundToInt().toString(),
                        style = MetricText.Label,
                        color = muted,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .height(height)
                    .semantics {
                        contentDescription = description
                        val current = scrubIndex ?: maxCount - 1
                        customActions = buildList {
                            if (current > 0) add(CustomAccessibilityAction("Titik sebelumnya") { scrubIndex = current - 1; true })
                            if (current < maxCount - 1) add(CustomAccessibilityAction("Titik berikutnya") { scrubIndex = current + 1; true })
                        }
                    }
                    .pointerInput(maxCount) {
                        detectTapGestures { offset ->
                            val idx = ((offset.x / size.width) * (maxCount - 1)).roundToInt().coerceIn(0, maxCount - 1)
                            scrubIndex = if (scrubIndex == idx) null else idx
                        }
                    }
                    .pointerInput(maxCount) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                scrubIndex = ((offset.x / size.width) * (maxCount - 1)).roundToInt().coerceIn(0, maxCount - 1)
                            },
                            onDragEnd = { },
                        ) { change, _ ->
                            change.consume()
                            scrubIndex = ((change.position.x / size.width) * (maxCount - 1)).roundToInt().coerceIn(0, maxCount - 1)
                        }
                    },
            ) {
                val padV = 8.dp.toPx()
                val usableH = (size.height - padV * 2).coerceAtLeast(1f)
                fun y(v: Float) = padV + usableH - (v - min) / (max - min) * usableH

                val dashGrid = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                ticks.forEach { t ->
                    val gy = y(t)
                    drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx(), pathEffect = dashGrid)
                }
                if (min < 0f && max > 0f) {
                    drawLine(muted.copy(alpha = 0.55f), Offset(0f, y(0f)), Offset(size.width, y(0f)), strokeWidth = 1.2.dp.toPx())
                }

                series.forEach { s ->
                    if (s.values.size < 2) return@forEach
                    val dx = size.width / (s.values.size - 1)
                    val pts = s.values.mapIndexed { i, v -> Offset(i * dx, y(v)) }
                    val path = smoothPath(pts)
                    if (s.area) {
                        val fill = Path().apply {
                            addPath(path)
                            lineTo(pts.last().x, size.height)
                            lineTo(pts.first().x, size.height)
                            close()
                        }
                        drawPath(
                            fill,
                            Brush.verticalGradient(
                                colors = listOf(s.color.copy(alpha = 0.26f), s.color.copy(alpha = 0.0f)),
                                startY = padV,
                                endY = size.height,
                            ),
                        )
                    }
                    drawPath(
                        path,
                        s.color,
                        style = Stroke(
                            width = if (s.dashed) 2.2.dp.toPx() else 2.6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = if (s.dashed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) else null,
                        ),
                    )

                    val activeIdx = (scrubIndex ?: s.values.lastIndex).coerceIn(0, s.values.lastIndex)
                    val pt = pts[activeIdx]
                    drawCircle(s.color.copy(alpha = 0.22f), 8.dp.toPx(), pt)
                    drawCircle(surface, 4.5.dp.toPx(), pt)
                    drawCircle(s.color, 3.dp.toPx(), pt)
                }

                scrubIndex?.let { idx ->
                    val x = (idx.toFloat() / (maxCount - 1).coerceAtLeast(1)) * size.width
                    drawLine(
                        muted.copy(alpha = 0.5f),
                        Offset(x, 0f),
                        Offset(x, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                    )
                }
            }
        }
        if (startLabel != null || middleLabel != null || endLabel != null) {
            Row(Modifier.fillMaxWidth().padding(start = 34.dp, top = 6.dp)) {
                Text(startLabel.orEmpty(), style = MetricText.Label, color = muted, modifier = Modifier.weight(1f))
                Text(middleLabel.orEmpty(), style = MetricText.Label, color = muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(endLabel.orEmpty(), style = MetricText.Label, color = muted, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            series.forEach { s ->
                val idx = (scrubIndex ?: s.values.lastIndex).coerceIn(0, s.values.lastIndex)
                val value = s.values.getOrNull(idx)?.roundToInt() ?: 0
                Row(
                    Modifier
                        .background(Palette.SurfaceHigh.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).background(s.color, CircleShape))
                    Spacer(Modifier.width(5.dp))
                    Text("${s.label} $value", style = MetricText.Label, color = Palette.Text)
                }
            }
        }
        detail?.invoke(scrubIndex ?: maxCount - 1)
    }
}

/** Builds a smooth Catmull-Rom-like cubic Bezier path through [pts] without sharp kinks. */
internal fun smoothPath(pts: List<Offset>): Path {
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

/** 1, 2, 5, 10, 20, 50... so axis labels are round numbers a rider can hold in their head. */
private fun niceStep(target: Float): Float {
    if (target <= 0f) return 1f
    val mag = Math.pow(10.0, Math.floor(Math.log10(target.toDouble()))).toFloat()
    val norm = target / mag
    val mult = when {
        norm <= 1f -> 1f
        norm <= 2f -> 2f
        norm <= 5f -> 5f
        else -> 10f
    }
    return (mag * mult).coerceAtLeast(1f)
}
