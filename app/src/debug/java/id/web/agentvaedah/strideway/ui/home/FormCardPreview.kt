package id.web.agentvaedah.strideway.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.web.agentvaedah.strideway.core.physio.TrainingLoad
import id.web.agentvaedah.strideway.data.prefs.ThemeMode
import id.web.agentvaedah.strideway.ui.theme.Palette
import id.web.agentvaedah.strideway.ui.theme.StridewayTheme
import java.time.LocalDate

private val last = LocalDate.of(2026, 11, 27).toEpochDay()
private val demo = TrainingLoad.series((0..83).map { i ->
    val week = listOf(0.0, 52.0, 18.0, 80.0, 0.0, 112.0, 30.0)
    TrainingLoad.DailyLoad(last - 83 + i, week[i % 7] * if (i / 7 % 4 == 2) 0.45 else 1.0)
}, last - 41, last)

@Composable
private fun Sample(theme: ThemeMode, populated: Boolean = true, points: List<TrainingLoad.FitnessPoint> = demo) {
    StridewayTheme(theme) {
        Box(Modifier.background(Palette.Bg).padding(vertical = 16.dp)) {
            FormCard(points, estimated = false, hasActivities = populated)
        }
    }
}

@Preview(widthDp = 360)
@Composable
fun FormLightPreview() = Sample(ThemeMode.LIGHT)

@Preview(widthDp = 360)
@Composable
fun FormDarkPreview() = Sample(ThemeMode.DARK)

@Preview(widthDp = 320, fontScale = 1.5f)
@Composable
fun FormLargeTypePreview() = Sample(ThemeMode.LIGHT, points = demo.dropLast(1) +
    TrainingLoad.FitnessPoint(last, fitness = 6.0, fatigue = 74.0))

@Preview(widthDp = 320)
@Composable
fun EmptyFormPreview() = Sample(ThemeMode.LIGHT, populated = false)
