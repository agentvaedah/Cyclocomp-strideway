package id.web.agentvaedah.strideway.ui.maps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.web.agentvaedah.strideway.core.format.Format
import id.web.agentvaedah.strideway.core.route.EventCutoff
import java.util.Locale
import kotlin.math.abs

@Composable
fun EventCutoffControl(distanceM: Double, event: EventCutoff?, onSave: (EventCutoff?) -> Unit, suggestedSpeed: Double? = null) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = true }) {
        Text(if (event == null) "Atur COT finis" else "COT finis · ${durationInput(event.limitMin)}")
    }
    if (!open) return
    var duration by rememberSaveable { mutableStateOf(event?.let { durationInput(it.limitMin) }.orEmpty()) }
    var speed by rememberSaveable { mutableStateOf((event?.speedKmh ?: suggestedSpeed)?.let { Format.oneDecimal(it) }.orEmpty()) }
    var stops by rememberSaveable { mutableStateOf((event?.stopMin ?: 0).toString()) }
    val draft = EventCutoff.parse(duration, stops, speed)
    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text("Batas waktu finis") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Rute ${Format.km(distanceM)} km. Masukkan durasi maksimum dari start resmi, termasuk semua jeda.")
                OutlinedTextField(duration, { duration = it }, label = { Text("COT (jam:menit)") },
                    placeholder = { Text("13:30") }, singleLine = true)
                OutlinedTextField(speed, { speed = it }, label = { Text("Kecepatan bergerak (km/j)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(stops, { stops = it }, label = { Text("Total berhenti (menit)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                if (draft != null) {
                    val time = draft.estimatedMs(distanceM)
                    val margin = draft.remainingMs(time)
                    Text("Estimasi finis ${Format.duration(time)} setelah start", style = MaterialTheme.typography.titleMedium)
                    Text(when {
                        margin < 0 -> "Lewat batas ${Format.duration(abs(margin))}"
                        margin > 0 -> "Cadangan ${Format.duration(margin)}"
                        else -> "Tepat di batas waktu"
                    })
                } else Text("Gunakan jam:menit, kecepatan 0,1–200 km/j, dan jeda lebih singkat dari COT.", style = MaterialTheme.typography.bodySmall)
                Text("Saat navigasi, hitung mundur dimulai ketika Rekam dimulai. Mulai rekaman saat start resmi.",
                    style = MaterialTheme.typography.bodySmall)
                if (event != null) TextButton(onClick = { onSave(null); open = false }) { Text("Hapus COT") }
            }
        },
        confirmButton = { TextButton(enabled = draft != null, onClick = { onSave(draft); open = false }) { Text("Terapkan") } },
        dismissButton = { TextButton(onClick = { open = false }) { Text("Batal") } },
    )
}

private fun durationInput(min: Int) = String.format(Locale.US, "%d:%02d", min / 60, min % 60)
