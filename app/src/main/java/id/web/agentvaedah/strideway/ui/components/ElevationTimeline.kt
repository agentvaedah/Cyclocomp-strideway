package id.web.agentvaedah.strideway.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.web.agentvaedah.strideway.core.format.Format
import id.web.agentvaedah.strideway.core.route.ElevationProfile
import id.web.agentvaedah.strideway.ui.theme.Palette

@Composable
fun ElevationTimeline(profile: ElevationProfile, cursorM: Double?, height: Dp = 104.dp) {
    val low = profile.low
    val high = profile.high
    if (low == null || high == null) {
        Text("Belum ada data ketinggian.", color = Palette.TextMuted)
        return
    }
    val span = (profile.endM - profile.startM).coerceAtLeast(1.0)
    val range = (high - low).coerceAtLeast(1.0)
    val accent = Palette.Accent
    val grid = Palette.Outline
    val cursor by rememberUpdatedState(cursorM)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${Format.integer(low)} – ${Format.integer(high)} m", style = MaterialTheme.typography.labelMedium)
            cursorM?.let { d ->
                val label = profile.altitudeAt(d)?.let { "${Format.integer(it)} m" } ?: "–"
                Text("${Format.km(d)} km · $label", color = accent, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.fillMaxWidth().height(height).drawWithCache {
            fun x(d: Double) = ((d - profile.startM) / span).toFloat() * size.width
            fun y(h: Double) = if (low == high) size.height / 2 else
                4.dp.toPx() + ((high - h) / range).toFloat() * (size.height - 8.dp.toPx())
            val path = Path()
            val dots = ArrayList<Offset>()
            var gap = true
            profile.samples.forEach { sample ->
                val h = sample.altitudeM
                if (h == null) gap = true else {
                    val px = x(sample.distanceM)
                    val py = y(h)
                    if (gap) { path.moveTo(px, py); dots += Offset(px, py) } else path.lineTo(px, py)
                    gap = false
                }
            }
            onDrawBehind {
                for (i in 0..2) {
                    val py = size.height * i / 2
                    drawLine(grid, Offset(0f, py), Offset(size.width, py), 1.dp.toPx())
                }
                drawPath(path, accent, style = Stroke(2.dp.toPx()))
                dots.forEach { drawCircle(accent, 2.dp.toPx(), it) }
                cursor?.takeIf { it in profile.startM..profile.endM }?.let { d ->
                    val px = x(d)
                    drawLine(accent.copy(alpha = .6f), Offset(px, 0f), Offset(px, size.height), 1.dp.toPx())
                    profile.altitudeAt(d)?.let { drawCircle(accent, 4.dp.toPx(), Offset(px, y(it))) }
                }
            }
        })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${Format.km(profile.startM)} km", style = MaterialTheme.typography.labelSmall)
            Text("${Format.km(profile.endM)} km", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ExploreElevation(profile: ElevationProfile) {
    var fraction by remember(profile) { mutableFloatStateOf(0f) }
    ElevationTimeline(profile, profile.startM + (profile.endM - profile.startM) * fraction)
    if (profile.low != null && profile.endM > profile.startM) {
        Slider(value = fraction, onValueChange = { fraction = it },
            modifier = Modifier.semantics { contentDescription = "Jelajahi elevasi menurut jarak" })
    }
}
