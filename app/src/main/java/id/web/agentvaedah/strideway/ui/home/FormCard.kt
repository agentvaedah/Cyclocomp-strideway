package id.web.agentvaedah.strideway.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.web.agentvaedah.strideway.core.physio.TrainingLoad.FitnessPoint
import id.web.agentvaedah.strideway.ui.components.TrendChart
import id.web.agentvaedah.strideway.ui.components.TrendSeries
import id.web.agentvaedah.strideway.ui.theme.MetricText
import id.web.agentvaedah.strideway.ui.theme.Palette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val locale = Locale.forLanguageTag("id-ID")
private val shortDate = DateTimeFormatter.ofPattern("d MMM", locale)
private val detailDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
private fun date(point: FitnessPoint, format: DateTimeFormatter) = LocalDate.ofEpochDay(point.epochDay).format(format)
private fun signed(value: Int) = if (value > 0) "+$value" else value.toString().replace('-', '−')
private fun decimal(value: Double) = String.format(locale, "%.1f", value).replace('-', '−')

@Composable
internal fun FormCard(points: List<FitnessPoint>, estimated: Boolean, hasActivities: Boolean) {
    val today = points.lastOrNull()?.takeIf { hasActivities }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .background(Palette.Surface, MaterialTheme.shapes.large)
            .border(1.dp, Palette.Outline, MaterialTheme.shapes.large)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(18.dp, 2.dp).background(Palette.Accent))
            Text("ANALISIS LATIHAN", style = MetricText.Label, color = Palette.TextMuted)
        }
        Text("Form hari ini", style = MaterialTheme.typography.headlineSmall, color = Palette.Text,
            modifier = Modifier.padding(top = 4.dp).semantics { heading() })
        if (today == null) {
            Spacer(Modifier.height(12.dp))
            Text("Belum ada data", style = MaterialTheme.typography.titleSmall, color = Palette.Text)
            Text("Rekam atau impor aktivitas pertama untuk melihat keseimbangan beban latihanmu.",
                style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
            return@Column
        }

        val score = today.form.roundToInt()
        val (label, advice) = when {
            score > 15 -> "Segar" to "Beban belakangan ini jauh di bawah rata-rata jangka panjang."
            score > 5 -> "Siap" to "Beban jangka pendek lebih ringan dari biasanya."
            score > -10 -> "Seimbang" to "Beban jangka pendek mendekati rata-rata jangka panjang."
            score > -30 -> "Membangun" to "Beban belakangan ini lebih tinggi dari biasanya."
            else -> "Lelah" to "Beban jangka pendek jauh di atas rata-rata jangka panjang."
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp)) {
            val number: @Composable () -> Unit = {
                Text(signed(score), style = MetricText.Large.copy(fontSize = 56.sp, lineHeight = 58.sp), color = Palette.Text)
            }
            val interpretation: @Composable () -> Unit = {
                Column {
                    Text(label, style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                    Text(advice, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                }
            }
            if (maxWidth / LocalDensity.current.fontScale < 240.dp) {
                Column { number(); interpretation() }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    number()
                    Box(Modifier.size(1.dp, 34.dp).background(Palette.Outline))
                    interpretation()
                }
            }
        }
        HorizontalDivider(color = Palette.Outline)
        FormHistory(points)
        if (estimated) Text("Sebagian beban diperkirakan dari durasi karena data detak jantung tidak tersedia.",
            style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun FormHistory(points: List<FitnessPoint>) {
    var days by rememberSaveable { mutableIntStateOf(42) }
    val window = remember(points, days) { points.takeLast(days) }
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (n in listOf(7, 14, 42)) {
            val active = days == n
            Box(
                Modifier.weight(1f).selectable(active, role = Role.Tab, onClick = { days = n })
                    .heightIn(min = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("$n hari", style = MaterialTheme.typography.labelLarge,
                    color = if (active) Palette.Text else Palette.TextMuted)
                if (active) Box(Modifier.align(Alignment.BottomCenter).size(24.dp, 2.dp).background(Palette.Accent))
            }
        }
    }
    val ink = Palette.Text
    val blue = Palette.Info
    val orange = Palette.Accent
    val series = remember(window, ink, blue, orange) {
        listOf(
            TrendSeries("Fitness", window.map { it.fitness.toFloat() }, blue),
            TrendSeries("Fatigue", window.map { it.fatigue.toFloat() }, orange, dashed = true),
            TrendSeries("Form", window.map { it.form.toFloat() }, ink, area = true),
        )
    }
    TrendChart(
        series, "Pilih titik untuk membaca Form, Fitness, dan Fatigue selama $days hari.",
        describePoint = { i ->
            val p = window[i]
            "${date(p, detailDate)}. Form ${decimal(p.form)}, Fitness ${decimal(p.fitness)}, " +
                "Fatigue ${decimal(p.fatigue)}, beban ${decimal(p.dailyLoad)} TRIMP."
        },
        modifier = Modifier.padding(top = 14.dp), height = 108.dp,
        startLabel = date(window.first(), shortDate), endLabel = "Hari ini",
    ) { i ->
        val p = window[i]
        val previous = points.getOrNull(points.size - window.size + i - 1)
        HorizontalDivider(color = Palette.Outline, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp))
        Text(date(p, detailDate), style = MaterialTheme.typography.titleSmall, color = Palette.Text)
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FormMetric("Form", decimal(p.form), ink, Modifier.weight(1f))
            FormMetric("Fitness", decimal(p.fitness), blue, Modifier.weight(1f))
            FormMetric("Fatigue", decimal(p.fatigue), orange, Modifier.weight(1f))
        }
        val change = previous?.let { (p.form - it.form) * 10 }?.roundToInt()
        val delta = change?.let { " · Form ${if (it > 0) "+" else ""}${decimal(it / 10.0)} dari kemarin" }.orEmpty()
        Text("Beban harian ${decimal(p.dailyLoad)} TRIMP$delta", style = MaterialTheme.typography.bodySmall, color = Palette.Text)
        Text("Geser grafik untuk memilih hari. Form = Fitness (42 hari) − Fatigue (7 hari).",
            style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun FormMetric(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
        }
        Text(value, style = MetricText.Small, color = Palette.Text)
    }
}
