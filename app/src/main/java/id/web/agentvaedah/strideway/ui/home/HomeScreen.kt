package id.web.agentvaedah.strideway.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.web.agentvaedah.strideway.core.challenge.Award
import id.web.agentvaedah.strideway.core.format.Format
import id.web.agentvaedah.strideway.data.prefs.UserProfile
import id.web.agentvaedah.strideway.ui.components.ActivityRow
import id.web.agentvaedah.strideway.ui.components.Avatar
import id.web.agentvaedah.strideway.ui.components.EmptyState
import id.web.agentvaedah.strideway.ui.components.ErrorState
import id.web.agentvaedah.strideway.ui.components.LoadingState
import id.web.agentvaedah.strideway.ui.components.SectionTitle
import id.web.agentvaedah.strideway.ui.motion.AnimatedNumber
import id.web.agentvaedah.strideway.ui.motion.Motion
import id.web.agentvaedah.strideway.ui.motion.reduceMotion
import id.web.agentvaedah.strideway.ui.motion.staggeredEnter
import id.web.agentvaedah.strideway.ui.theme.MetricText
import id.web.agentvaedah.strideway.ui.theme.Palette
import id.web.agentvaedah.strideway.ui.training.IntensityStrip
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val idLocale = Locale.forLanguageTag("id-ID")
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", idLocale)

@Composable
fun HomeScreen(
    onOpenActivity: (Long) -> Unit,
    onStartRecording: () -> Unit,
    onImport: () -> Unit,
    onOpenGarage: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenTraining: () -> Unit = {},
    onRide: () -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    (state as? HomeState.Ready)?.newAwards?.takeIf { it.isNotEmpty() }?.let { awards ->
        AwardDialog(awards, onDismiss = { vm.acknowledgeAwards(awards) })
    }
    when (val s = state) {
        HomeState.Loading -> LoadingState("Memuat aktivitas", Modifier.fillMaxSize())
        is HomeState.Error -> ErrorState(s.message, Modifier.fillMaxSize())
        is HomeState.Ready -> LazyColumn(Modifier.fillMaxSize()) {
            item { TodayHeader(s.profile, onOpenProfile) }
            item { WeekSummary(s, Modifier.padding(horizontal = 16.dp).staggeredEnter(0)) }
            item {
                TrainingCard(
                    s.weekLoad,
                    s.today,
                    onRide = { t -> vm.ride(t); onRide() },
                    onOpenTraining = onOpenTraining,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).staggeredEnter(1),
                )
            }
            item { FormCard(s.fitness, s.anyEstimatedLoad, hasActivities = s.feed.isNotEmpty()) }
            item { InsightsCarousel(s, vm, onOpenGarage) }
            item { SectionTitle("Tantangan bulan ini") }
            items(s.challenges, key = { it.progress.id.name }) { ChallengeRow(it, onJoin = { j -> vm.setJoined(it.progress.id, j) }) }
            item { SectionTitle("Aktivitas") }
            if (s.feed.isEmpty()) {
                item {
                    EmptyState(
                        title = "Belum ada aktivitas",
                        body = "Rekam ride atau run pertamamu, atau impor file .gpx, .tcx, .fit dari event Audax lewat tab Peta.",
                        actionLabel = "Mulai rekam",
                        onAction = onStartRecording,
                        secondaryLabel = "Impor file",
                        onSecondary = onImport,
                    )
                }
            }
            itemsIndexed(s.feed, key = { _, it -> it.activity.id }) { i, it ->
                ActivityRow(it.activity, it.path, onClick = { onOpenActivity(it.activity.id) }, mapHeight = 190.dp, modifier = Modifier.staggeredEnter(i + 2))
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** "Today" and the date, Strava-style, with the rider's picture leading to the profile. */
@Composable
private fun TodayHeader(profile: UserProfile, onOpenProfile: () -> Unit) {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", idLocale)).replaceFirstChar { it.uppercase() }
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Today", style = MaterialTheme.typography.headlineLarge)
            Text(date, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
        }
        Avatar(
            profile.photoPath,
            profile.name.ifBlank { "Strideway" },
            44.dp,
            Modifier.clickable(role = Role.Button, onClickLabel = "Buka profil", onClick = onOpenProfile),
        )
    }
}

/** One line of the week: distance, climb and today's form. */
@Composable
private fun WeekSummary(s: HomeState.Ready, modifier: Modifier = Modifier) {
    val form = s.fitness.lastOrNull()?.form?.roundToInt() ?: 0
    Row(
        modifier
            .fillMaxWidth()
            .background(Palette.Surface, MaterialTheme.shapes.large)
            .border(1.dp, Palette.Outline.copy(alpha = 0.6f), MaterialTheme.shapes.large)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(
            Triple("MINGGU INI", s.weekly.thisWeek.distanceKm, "km"),
            Triple("NAIK", s.weekly.thisWeek.elevationM, "m"),
            Triple("FORM", form.toDouble(), ""),
        ).forEach { (label, v, unit) ->
            Column(Modifier.weight(1f)) {
                Text(label, style = MetricText.Label, color = Palette.TextMuted)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    if (label == "FORM" && s.feed.isEmpty()) {
                        Text("—", style = MetricText.Medium, color = Palette.TextMuted)
                    } else AnimatedNumber(
                        v,
                        format = { x -> if (label == "FORM") (if (x >= 0.5) "+" else "") + x.roundToInt() else x.roundToInt().toString() },
                        style = MetricText.Medium,
                        color = Palette.Text,
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(Modifier.width(3.dp))
                        Text(unit, style = MetricText.Label, color = Palette.TextMuted, modifier = Modifier.padding(bottom = 5.dp))
                    }
                }
            }
        }
    }
}

/**
 * Training at a glance: this week's load per day in smooth gradient capsules, then today's
 * planned session with its zone-colored interval profile.
 */
@Composable
private fun TrainingCard(load: WeekLoad?, today: TodaySession?, onRide: (TodaySession) -> Unit, onOpenTraining: () -> Unit, modifier: Modifier = Modifier) {
    val accent = Palette.Accent
    val track = Palette.SurfaceHigh
    val muted = Palette.TextMuted
    val reduce = reduceMotion()
    val grow = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) grow.animateTo(1f, tween(Motion.LONG + 200)) }
    Column(
        modifier
            .fillMaxWidth()
            .background(Palette.Surface, MaterialTheme.shapes.large)
            .border(1.dp, Palette.Outline.copy(alpha = 0.6f), MaterialTheme.shapes.large)
            .padding(16.dp),
    ) {
        if (load != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("BEBAN LATIHAN MINGGU INI", style = MetricText.Label, color = Palette.TextMuted, modifier = Modifier.weight(1f))
                Text(load.total.roundToInt().toString(), style = MetricText.Medium, color = Palette.Text)
                Spacer(Modifier.width(4.dp))
                Text("TRIMP", style = MetricText.Label, color = Palette.Accent, modifier = Modifier.padding(bottom = 5.dp))
            }
            val vs = when {
                load.lastWeek <= 0.0 -> "Minggu lalu belum ada latihan tercatat"
                else -> "Minggu lalu ${load.lastWeek.roundToInt()} · ${((load.total / load.lastWeek) * 100).roundToInt()}% dari minggu lalu"
            }
            Text(vs, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
            Spacer(Modifier.height(14.dp))
            val top = (load.days.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
            Canvas(Modifier.fillMaxWidth().height(72.dp).semantics { contentDescription = "Beban latihan per hari minggu ini, total ${load.total.roundToInt()}" }) {
                val gap = 10.dp.toPx()
                val slotW = (size.width - gap * 6) / 7f
                val barW = minOf(slotW, 22.dp.toPx())
                val radius = CornerRadius(barW / 2f, barW / 2f)
                load.days.forEachIndexed { i, v ->
                    val x = i * (slotW + gap) + (slotW - barW) / 2f
                    // Soft background capsule track for every day
                    drawRoundRect(
                        color = if (i == load.todayIndex) accent.copy(alpha = 0.12f) else track.copy(alpha = 0.7f),
                        topLeft = Offset(x, 0f),
                        size = Size(barW, size.height),
                        cornerRadius = radius,
                    )
                    if (v > 0.0) {
                        val h = (size.height * (v / top).toFloat() * grow.value).coerceAtLeast(barW)
                        val y = size.height - h
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(accent, accent.copy(alpha = 0.55f)),
                                startY = y,
                                endY = size.height,
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barW, h),
                            cornerRadius = radius,
                        )
                    } else if (i == load.todayIndex) {
                        // Subtle anchor dot for today when no load yet
                        drawCircle(accent, 3.5.dp.toPx(), Offset(x + barW / 2f, size.height - 8.dp.toPx()))
                    } else {
                        drawCircle(muted.copy(alpha = 0.25f), 2.5.dp.toPx(), Offset(x + barW / 2f, size.height - 8.dp.toPx()))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEachIndexed { idx, d ->
                    val isToday = idx == load.todayIndex
                    Text(
                        d,
                        style = MetricText.Label,
                        color = if (isToday) Palette.Accent else Palette.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            HorizontalDivider(color = Palette.Outline.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 14.dp))
        }
        if (today != null) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        listOfNotNull("LATIHAN HARI INI", today.phase?.let { "${it.label.uppercase()} · M${today.week}" }).joinToString(" · "),
                        style = MetricText.Label,
                        color = Palette.Accent,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(today.workout.name, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    today.workout.durationS.let { d -> if (d >= 3600) "${d / 3600}j ${(d % 3600) / 60}m" else "${d / 60}m" },
                    style = MetricText.Label,
                    color = Palette.Text,
                    modifier = Modifier.background(Palette.SurfaceHigh, RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
            IntensityStrip(today.workout, Modifier.fillMaxWidth().height(48.dp).padding(vertical = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (today.done) {
                    Text("Sudah dikayuh hari ini", style = MaterialTheme.typography.bodyMedium, color = Palette.Success, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    Button(onClick = { onRide(today) }, shape = MaterialTheme.shapes.medium) { Text("Kayuh sesi ini") }
                }
                OutlinedButton(onClick = onOpenTraining, shape = MaterialTheme.shapes.medium) { Text("Rencana") }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Belum ada sesi terjadwal hari ini.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenTraining) { Text("Buka Latihan") }
            }
        }
    }
}

/**
 * Challenges are opt-in: an unjoined one shows its rule and a Join button, a joined one shows progress,
 * and a completed one shows the award it earned.
 */
@Composable
private fun ChallengeRow(c: ChallengeCard, onJoin: (Boolean) -> Unit) {
    val p = c.progress
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.id.title, style = MaterialTheme.typography.titleSmall)
                Text(p.id.rule, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
            }
            when {
                c.earned || (c.joined && p.completed) -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tercapai", style = MaterialTheme.typography.labelLarge, color = Palette.Accent)
                }
                c.joined -> TextButton(onClick = { onJoin(false) }) { Text("Keluar", color = Palette.TextMuted) }
                else -> OutlinedButton(onClick = { onJoin(true) }) { Text("Ikuti") }
            }
        }
        if (c.joined) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { p.fraction.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Palette.Accent,
                trackColor = Palette.SurfaceHigh,
                drawStopIndicator = {},
            )
            Text(
                "${Format.oneDecimal(p.currentKm)} / ${p.targetKm.toInt()} km",
                style = MaterialTheme.typography.labelMedium,
                color = Palette.TextMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun AwardDialog(awards: List<Award>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(40.dp)) },
        title = { Text(if (awards.size == 1) "Tantangan tercapai" else "${awards.size} tantangan tercapai") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                awards.forEach { a ->
                    val month = a.month.format(monthFmt).replaceFirstChar { it.titlecase(idLocale) }
                    Text("${a.id.title} · $month", style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    "Penghargaan tersimpan di You > Progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextMuted,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Mantap") } },
    )
}

@Composable
internal fun CalendarSection(cal: CalendarData, onPrev: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya") }
            Text(
                cal.month.format(monthFmt).replaceFirstChar { it.titlecase(idLocale) },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Bulan berikutnya") }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach {
                Text(it, style = MaterialTheme.typography.labelMedium, color = Palette.TextMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        val first = cal.month.atDay(1)
        val lead = first.dayOfWeek.value - DayOfWeek.MONDAY.value
        val days = cal.month.lengthOfMonth()
        val cells = lead + days
        val rows = (cells + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - lead + 1
                    Box(Modifier.weight(1f).aspectRatio(1.4f), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            val active = day in cal.activeDays
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .background(if (active) Palette.Accent else Palette.Surface, CircleShape)
                                    .semantics { contentDescription = if (active) "Tanggal $day, ada latihan" else "Tanggal $day" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (active) Palette.OnAccent else Palette.TextMuted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
