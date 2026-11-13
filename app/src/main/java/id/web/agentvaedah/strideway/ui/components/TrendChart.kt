package id.web.agentvaedah.strideway.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.contentDescription
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
 * Line chart made to be read by people who are not looking at charts all day: a value grid with numbers on
 * the left, dates along the bottom, the current value marked on the last point, and a legend with each
 * series' latest number. Three gridlines are enough to place a line; more becomes noise on a phone.
 */
@Composable
fun TrendChart(
    series: List<TrendSeries>,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    startLabel: String? = null,
    middleLabel: String? = null,
    endLabel: String? = null,
) {
    val grid = Palette.Outline
    val muted = Palette.TextMuted
    val all = series.flatMap { it.values }
    if (all.size < 2) return
    val rawMin = all.min()
    val rawMax = all.max()
    // Round the axis outwards so the labels read as whole numbers, and never collapse to a flat line.
    val span = (rawMax - rawMin).coerceAtLeast(1f)
    val step = niceStep(span / 2f)
    val min = (rawMin / step).toInt() * step - if (rawMin < 0 && rawMin % step != 0f) step else 0f
    val max = ((rawMax / step).roundToInt() * step).coerceAtLeast(min + step)
    val ticks = listOf(min, (min + max) / 2f, max).distinct()

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            // Axis labels sit outside the canvas so the line area stays clean.
            Column(Modifier.width(34.dp).height(height).padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                ticks.reversed().forEach { t ->
                    Text(t.roundToInt().toString(), style = MetricText.Label, color = muted, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .height(height)
                    .semantics { contentDescription = description },
            ) {
                fun y(v: Float) = size.height - (v - min) / (max - min) * size.height
                // Gridlines, one per axis label.
                ticks.forEach { t ->
                    val gy = y(t)
                    drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx())
                }
                // Zero is called out when the range crosses it: that is the line riders read as "fresh".
                if (min < 0f && max > 0f) {
                    drawLine(muted, Offset(0f, y(0f)), Offset(size.width, y(0f)), strokeWidth = 1.5.dp.toPx())
                }
                series.forEach { s ->
                    if (s.values.size < 2) return@forEach
                    val dx = size.width / (s.values.size - 1)
                    val path = Path()
                    s.values.forEachIndexed { i, v ->
                        val x = i * dx
                        if (i == 0) path.moveTo(x, y(v)) else path.lineTo(x, y(v))
                    }
                    if (s.area) {
                        val fill = Path().apply {
                            addPath(path)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(fill, Brush.verticalGradient(listOf(s.color.copy(alpha = 0.28f), s.color.copy(alpha = 0.02f))))
                    }
                    drawPath(
                        path,
                        s.color,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = if (s.dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 7f)) else null,
                        ),
                    )
                    // A dot on the newest point anchors "where am I now" at the right edge.
                    val lastX = (s.values.size - 1) * dx
                    val lastY = y(s.values.last())
                    drawCircle(Color.White.copy(alpha = 0.9f), 4.dp.toPx(), Offset(lastX, lastY))
                    drawCircle(s.color, 2.5.dp.toPx(), Offset(lastX, lastY))
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
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            series.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(width = 14.dp, height = 3.dp)
                            .background(if (s.dashed) s.color.copy(alpha = 0.7f) else s.color, MaterialTheme.shapes.extraSmall),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${s.label} ${s.values.lastOrNull()?.roundToInt() ?: 0}",
                        style = MetricText.Label,
                        color = Palette.Text,
                    )
                }
            }
        }
    }
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

