package id.web.agentvaedah.strideway.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.web.agentvaedah.strideway.ui.theme.MetricText
import id.web.agentvaedah.strideway.ui.theme.Palette
import kotlin.math.roundToInt

/** [area] emphasizes the signed distance to zero. */
data class TrendSeries(
    val label: String,
    val values: List<Float>,
    val color: Color,
    val dashed: Boolean = false,
    val area: Boolean = false,
)

/** Scrubbable trend with a readout for the selected day. */
@Composable
fun TrendChart(
    series: List<TrendSeries>,
    description: String,
    describePoint: (Int) -> String,
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
    if (all.isEmpty()) return
    val maxCount = series.maxOf { it.values.size }
    var scrubIndex by remember(series) { mutableStateOf<Int?>(null) }

    val rawMin = minOf(0f, all.min())
    val rawMax = maxOf(0f, all.max())
    val span = (rawMax - rawMin).coerceAtLeast(1f)
    val step = niceStep(span / 3f)
    val min = (rawMin / step).toInt() * step - if (rawMin < 0 && rawMin % step != 0f) step else 0f
    val max = (kotlin.math.ceil(rawMax / step) * step).coerceAtLeast(min + step)
    val ticks = (0..((max - min) / step).roundToInt()).map { min + it * step }
    val labels = ticks.asReversed().map { it.roundToInt().toString() }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelSizes = labels.map { measurer.measure(it, MetricText.Label, maxLines = 1).size }
    val axisWidth = with(density) { labelSizes.maxOf { it.width }.toDp() + 6.dp }.coerceAtLeast(34.dp)
    val labelHeight = with(density) { labelSizes.maxOf { it.height }.toDp() }
    val chartHeight = maxOf(height, labelHeight * labels.size + 2.dp * (labels.size - 1))

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Column(
                Modifier.width(axisWidth).height(chartHeight).padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                labels.forEach { label ->
                    Text(
                        label,
                        style = MetricText.Label,
                        color = muted,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .height(chartHeight)
                    .semantics {
                        contentDescription = description
                        val current = scrubIndex ?: maxCount - 1
                        stateDescription = describePoint(current)
                        customActions = buildList {
                            if (current > 0) add(CustomAccessibilityAction("Titik sebelumnya") { scrubIndex = current - 1; true })
                            if (current < maxCount - 1) add(CustomAccessibilityAction("Titik berikutnya") { scrubIndex = current + 1; true })
                        }
                    }
                    .pointerInput(series) {
                        detectTapGestures { offset ->
                            val idx = ((offset.x / size.width) * (maxCount - 1)).roundToInt().coerceIn(0, maxCount - 1)
                            scrubIndex = if (scrubIndex == idx) null else idx
                        }
                    }
                    .pointerInput(series) {
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
                val padV = labelHeight.toPx() / 2f
                val usableH = (size.height - padV * 2).coerceAtLeast(1f)
                fun y(v: Float) = padV + usableH - (v - min) / (max - min) * usableH

                ticks.forEach { t ->
                    val gy = y(t)
                    drawLine(if (t == 0f) muted.copy(alpha = 0.5f) else grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx())
                }

                series.forEach { s ->
                    if (s.values.size == 1) {
                        drawCircle(s.color, 2.6.dp.toPx(), Offset(size.width, y(s.values.single())))
                        return@forEach
                    }
                    if (s.values.isEmpty()) return@forEach
                    val dx = size.width / (s.values.size - 1)
                    val pts = s.values.mapIndexed { i, v -> Offset(i * dx, y(v)) }
                    val path = smoothPath(pts)
                    if (s.area) {
                        val fill = Path().apply {
                            addPath(path)
                            lineTo(pts.last().x, y(0f))
                            lineTo(pts.first().x, y(0f))
                            close()
                        }
                        drawPath(fill, s.color.copy(alpha = 0.055f))
                    }
                    drawPath(
                        path,
                        s.color,
                        style = Stroke(
                            width = if (s.area) 2.6.dp.toPx() else 1.7.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = if (s.dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null,
                        ),
                    )

                    val activeIdx = (scrubIndex ?: s.values.lastIndex).coerceIn(0, s.values.lastIndex)
                    val pt = pts[activeIdx]
                    if (scrubIndex != null || s.area) {
                        drawCircle(surface, 4.dp.toPx(), pt)
                        drawCircle(s.color, 2.6.dp.toPx(), pt)
                    }
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
            Row(Modifier.fillMaxWidth().padding(start = axisWidth, top = 6.dp)) {
                Text(startLabel.orEmpty(), style = MetricText.Label, color = muted, modifier = Modifier.weight(1f))
                if (middleLabel != null) Text(middleLabel, style = MetricText.Label, color = muted,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(endLabel.orEmpty(), style = MetricText.Label, color = muted, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
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
