package id.web.agentvaedah.strideway.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.web.agentvaedah.strideway.core.format.Format
import id.web.agentvaedah.strideway.core.route.EventCutoff
import id.web.agentvaedah.strideway.data.db.EventRoute
import id.web.agentvaedah.strideway.ui.components.ElevationTimeline
import id.web.agentvaedah.strideway.ui.maps.EventCutoffControl
import id.web.agentvaedah.strideway.ui.maps.FinishEstimate
import id.web.agentvaedah.strideway.ui.theme.MetricText
import id.web.agentvaedah.strideway.ui.theme.Palette

@Composable
internal fun NextEventCard(onOpenMaps: () -> Unit, vm: NextEventViewModel = hiltViewModel()) {
    val state by vm.event.collectAsStateWithLifecycle()
    state?.let { NextEventContent(it, onOpenMaps, vm::select, vm::saveCutoff) }
}

@Composable
internal fun NextEventContent(
    state: HomeEvent,
    onOpenMaps: () -> Unit,
    onSelect: (EventRoute?) -> Unit,
    onCutoff: (EventRoute, EventCutoff?) -> Unit,
) {
    val route = state.route
    var choosing by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp),
        shape = MaterialTheme.shapes.large, color = Palette.Surface, contentColor = Palette.Text,
        border = BorderStroke(1.dp, Palette.Outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Event berikutnya", style = MaterialTheme.typography.labelLarge, color = Palette.Accent,
                modifier = Modifier.semantics { heading() })
            if (route == null) {
                val empty = state.choices.isEmpty()
                Text(if (empty) "Belum ada rute tersimpan" else "Pilih rute eventmu",
                    style = MaterialTheme.typography.titleLarge)
                Text(if (empty) "Buat atau impor rute di Peta, lalu pilih di sini."
                    else "Lihat profil elevasi dan cek apakah rencana finis masih masuk COT.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
                OutlinedButton(onClick = { if (empty) onOpenMaps() else choosing = true }) {
                    Text(if (empty) "Buka Peta" else "Pilih rute")
                }
            } else key(route.selectionKey()) {
                Text(route.name, style = MaterialTheme.typography.headlineSmall)
                val climb = when {
                    route.elevationGainM > 0 || state.profile?.low != null -> " · naik ${Format.integer(route.elevationGainM)} m"
                    else -> ""
                }
                Text("${Format.km(route.distanceM)} km$climb", style = MetricText.Small)
                state.profile?.let { ElevationTimeline(it, cursorM = null, height = 64.dp) }
                HorizontalDivider(color = Palette.Outline)
                val cot = route.eventCutoff
                if (cot != null) {
                    FinishEstimate(cot, route.distanceM)
                    Text("${cot.speedKmh} km/j bergerak · ${cot.stopMin} menit berhenti",
                        style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                } else Text("Atur batas waktu, kecepatan, dan jeda untuk melihat estimasi finis.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    EventCutoffControl(route.distanceM, cot, { onCutoff(route, it) })
                    TextButton(onClick = { choosing = true }) { Text("Ganti rute") }
                }
            }
        }
    }
    if (choosing) {
        AlertDialog(
            onDismissRequest = { choosing = false },
            title = { Text("Pilih rute event") },
            text = {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp).selectableGroup()) {
                    items(state.choices, key = { it.selectionKey() }) { choice ->
                        val selected = route == choice
                        ListItem(
                            headlineContent = { Text(choice.name) },
                            supportingContent = { Text("${Format.km(choice.distanceM)} km") },
                            leadingContent = { RadioButton(selected, onClick = null) },
                            modifier = Modifier.selectable(selected, role = Role.RadioButton) {
                                onSelect(choice); choosing = false
                            },
                        )
                    }
                }
            },
            dismissButton = {
                if (route != null) TextButton(onClick = { onSelect(null); choosing = false }) { Text("Lepas pilihan") }
            },
            confirmButton = { TextButton(onClick = { choosing = false }) { Text("Tutup") } },
        )
    }
}
