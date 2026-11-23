package id.web.agentvaedah.strideway.ui.record

import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.web.agentvaedah.strideway.service.RecordingState
import id.web.agentvaedah.strideway.core.format.Format
import id.web.agentvaedah.strideway.core.route.ElevationProfile
import id.web.agentvaedah.strideway.ui.components.ElevationTimeline
import id.web.agentvaedah.strideway.ui.theme.Palette

@Composable
fun RecordScreen(onOpenActivity: (Long) -> Unit, onOpenWeather: () -> Unit, vm: RecordViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val recovered by vm.recovered.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val active = state is RecordingState.Active

    // Volume keys act as head-unit buttons only while a recording is on screen.
    DisposableEffect(active) {
        vm.setKeysActive(active)
        onDispose { vm.setKeysActive(false) }
    }
    DisposableEffect(active) {
        val window = (context as? android.app.Activity)?.window
        if (active) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(state) {
        val s = state
        if (s is RecordingState.Finished) {
            vm.acknowledgeFinished()
            onOpenActivity(s.activityId)
        }
    }

    recovered?.let { id ->
        AlertDialog(
            onDismissRequest = vm::dismissRecovered,
            title = { Text("Rekaman terakhir terputus") },
            text = { Text("Aplikasi tertutup saat merekam. Titik yang sempat tersimpan sudah dijadikan aktivitas.") },
            confirmButton = {
                TextButton(onClick = { vm.dismissRecovered(); onOpenActivity(id) }) { Text("Lihat aktivitas") }
            },
            dismissButton = { TextButton(onClick = vm::dismissRecovered) { Text("Tutup") } },
        )
    }

    when (val s = state) {
        is RecordingState.Active -> LiveDashboard(s, vm, onOpenWeather)
        else -> StandbyScreen(vm)
    }
}

@Composable
private fun LiveDashboard(s: RecordingState.Active, vm: RecordViewModel, onOpenWeather: () -> Unit) {
    val display by vm.display.collectAsStateWithLifecycle()
    val mapBase by vm.mapBase.collectAsStateWithLifecycle()
    val navRoute by vm.navRoute.collectAsStateWithLifecycle()
    val athlete by vm.athlete.collectAsStateWithLifecycle()
    val nav by vm.nav.collectAsStateWithLifecycle()
    val workout by vm.workoutState.collectAsStateWithLifecycle()
    val reroute by vm.rerouteLine.collectAsStateWithLifecycle()
    val camera by vm.camera.collectAsStateWithLifecycle()
    val tilt3d by vm.tilt3d.collectAsStateWithLifecycle()
    val headingUp by vm.headingUp.collectAsStateWithLifecycle()
    val routeAlerts by vm.routeAlerts.collectAsStateWithLifecycle()
    val savedRoutes by vm.savedRoutes.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    var confirmStop by remember { mutableStateOf(false) }
    var pickRoute by remember { mutableStateOf(false) }
    var pickDisplay by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        when (display) {
            DisplayMode.ELEVATION -> Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val route = navRoute
                var showRoute by rememberSaveable(route?.id) { mutableStateOf(route?.profile?.low != null) }
                Text("Profil elevasi", style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!showRoute, { showRoute = false }, { Text("Rekaman") })
                    FilterChip(showRoute, { showRoute = true }, { Text("Rute") }, enabled = route != null)
                }
                if (showRoute && route != null) {
                    Text(route.name, style = MaterialTheme.typography.titleMedium)
                    val progress = nav?.takeUnless { it.offRoute || reroute != null }?.progressM
                    ElevationTimeline(route.profile, progress, height = 155.dp)
                    if (progress == null) Text("Posisi pada rute belum tersedia.", color = Palette.TextMuted)
                } else {
                    val profile = remember(s.elevationProfile) { ElevationProfile(s.elevationProfile) }
                    Text("Jarak terekam · ${Format.km(s.metrics.distanceM)} km", style = MaterialTheme.typography.titleMedium)
                    ElevationTimeline(profile, profile.endM, height = 155.dp)
                }
            }
            // Maximised: numbers only, the way Strava fills the screen while riding.
            DisplayMode.METRICS -> ExpandedMetrics(
                state = s,
                athlete = athlete,
                onMinimize = { vm.setDisplay(DisplayMode.SPLIT) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                nav = nav,
                waitingRouteM = navRoute?.lengthM?.takeIf { nav == null },
                workout = workout,
            )
            else -> {
                Column(Modifier.fillMaxWidth().weight(1f)) {
                    val mapOnly = display == DisplayMode.MAP
                    LiveMap(
                        state = s,
                        base = mapBase,
                        navRoute = reroute ?: navRoute?.points.orEmpty(),
                        nav = nav,
                        waitingRouteM = navRoute?.lengthM?.takeIf { nav == null && reroute == null },
                        camera = camera,
                        tilt3d = tilt3d,
                        headingUp = headingUp,
                        routeAlerts = routeAlerts,
                        bottomInset = 0.dp,
                        onCamera = vm::setCamera,
                        onToggle3d = vm::toggle3d,
                        onToggleHeadingUp = vm::toggleHeadingUp,
                        onToggleAlerts = vm::toggleRouteAlerts,
                        onSwitchRoute = { pickRoute = true },
                        weather = weather,
                        onOpenWeather = onOpenWeather,
                        modifier = Modifier.fillMaxWidth().weight(if (mapOnly) 1f else if (workout != null) 0.62f else 0.7f),
                        showSpeed = mapOnly,
                    )
                    if (!mapOnly) {
                        MinimizedMetrics(
                            state = s,
                            athlete = athlete,
                            onMaximize = { vm.setDisplay(DisplayMode.METRICS) },
                            // A workout adds its strip, so the metrics share grows a little to keep both readable.
                            modifier = Modifier.fillMaxWidth().weight(if (workout != null) 0.38f else 0.3f).heightIn(min = 190.dp),
                            workout = workout,
                        )
                    }
                }
            }
        }
        ControlBar(
            paused = s.metrics.manualPaused,
            onPause = vm::togglePause,
            onStop = { confirmStop = true },
            onCycle = { pickDisplay = true },
        )
    }

    if (pickDisplay) {
        AlertDialog(onDismissRequest = { pickDisplay = false }, title = { Text("Tampilan rekam") },
            text = { Column {
                DisplayMode.entries.forEach { mode ->
                    TextButton(onClick = { vm.setDisplay(mode); pickDisplay = false }, modifier = Modifier.fillMaxWidth()) {
                        Text(mode.label)
                    }
                }
            } },
            confirmButton = { TextButton(onClick = { pickDisplay = false }) { Text("Tutup") } },
        )
    }

    if (pickRoute) {
        RouteSwitchSheet(
            routes = savedRoutes,
            activeId = navRoute?.id,
            onSelect = { id -> vm.selectRoute(id); pickRoute = false },
            onDismiss = { pickRoute = false },
        )
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Selesai merekam?") },
            text = { Text("Simpan untuk menambah aktivitas ke Home, atau buang jika rekaman ini tidak dipakai.") },
            confirmButton = { TextButton(onClick = { confirmStop = false; vm.stop() }) { Text("Simpan") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { confirmStop = false; vm.discard() }) { Text("Buang", color = Palette.Error) }
                    TextButton(onClick = { confirmStop = false }) { Text("Lanjut rekam") }
                }
            },
        )
    }
}
