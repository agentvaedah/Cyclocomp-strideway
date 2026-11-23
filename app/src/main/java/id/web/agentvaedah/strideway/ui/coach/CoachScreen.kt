package id.web.agentvaedah.strideway.ui.coach

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.web.agentvaedah.strideway.core.coach.CoachAttachment
import id.web.agentvaedah.strideway.data.db.ChatMessageEntity
import id.web.agentvaedah.strideway.data.db.ChatRole
import id.web.agentvaedah.strideway.ui.components.EmptyState
import id.web.agentvaedah.strideway.ui.components.LoadingState
import id.web.agentvaedah.strideway.ui.theme.MetricText
import id.web.agentvaedah.strideway.ui.theme.Palette

private val starters = listOf(
    "Paha sering kram di km 60, apa penyebab dan cara mencegahnya?",
    "Bagaimana pacing untuk half marathon target 2 jam?",
    "Berapa banyak minum dan karbohidrat per jam untuk ride 100 km di siang hari?",
    "Lutut depan nyeri setelah ride panjang, apakah sadel terlalu rendah?",
)

@Composable
fun CoachScreen(onOpenKeys: () -> Unit, onBack: () -> Unit, vm: CoachViewModel = hiltViewModel()) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val cfg by vm.llmSettings.collectAsStateWithLifecycle()
    val hasKey by vm.hasKey.collectAsStateWithLifecycle()
    val sending by vm.sending.collectAsStateWithLifecycle()
    val attachment by vm.attachment.collectAsStateWithLifecycle()
    val parsingFile by vm.parsingFile.collectAsStateWithLifecycle()
    val attachError by vm.attachError.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val needsKey = cfg.provider.keyRequired && !hasKey

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::attachFile)
    }

    LaunchedEffect(messages?.size, sending) {
        val count = (messages?.size ?: 0) + if (sending) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    LaunchedEffect(attachError) {
        attachError?.let {
            snackbar.showSnackbar(it)
            vm.clearAttachError()
        }
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali ke Latihan") }
                Column(Modifier.weight(1f)) {
                    Text("AI Coach", style = MaterialTheme.typography.headlineLarge)
                    Text("${cfg.provider.label} · ${cfg.model}", style = MetricText.Label, color = Palette.TextMuted)
                }
                if (!messages.isNullOrEmpty()) TextButton(onClick = { confirmClear = true }) { Text("Hapus") }
                IconButton(onClick = onOpenKeys) { Icon(Icons.Outlined.VpnKey, contentDescription = "Atur API key dan model") }
            }
            HorizontalDivider(color = Palette.Outline.copy(alpha = 0.6f))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                val list = messages
                when {
                    list == null -> LoadingState("Memuat percakapan")
                    needsKey && list.isEmpty() -> Column(Modifier.fillMaxSize()) {
                        EmptyState(
                            title = "Pakai API key milikmu sendiri",
                            body = "AI Coach memanggil penyedia AI langsung dari ponsel memakai key-mu (OpenAI, Gemini, Groq, Anthropic, atau Ollama lokal). " +
                                "Kamu juga bisa unggah file GPX, TCX, FIT, atau CSV untuk dianalisis AI.",
                            actionLabel = "Masukkan API key",
                            onAction = onOpenKeys,
                        )
                    }
                    list.isEmpty() -> Column(
                        Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "Tanyakan soal biomekanika, pacing, nutrisi, atau unggah file aktivitas (GPX/TCX/FIT/CSV/JSON) agar Coach menganalisis datamu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.TextMuted,
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Palette.Accent.copy(alpha = 0.10f), MaterialTheme.shapes.large)
                                .border(1.dp, Palette.Accent.copy(alpha = 0.35f), MaterialTheme.shapes.large)
                                .clickable(role = Role.Button, enabled = !parsingFile && !sending) {
                                    filePicker.launch(arrayOf("*/*"))
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .background(Palette.Accent.copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Upload file untuk analisis AI", style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                                Text("Mendukung GPX, TCX, FIT, CSV, JSON, dan TXT", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                            }
                        }
                        starters.forEach { q ->
                            OutlinedButton(
                                onClick = { vm.send(q) },
                                enabled = !sending,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(q, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(list, key = { it.id }) { Bubble(it, onOpenKeys) }
                        if (sending) {
                            item {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Palette.Accent)
                                    Text("  Coach sedang menganalisis...", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
                                }
                            }
                        }
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Palette.Surface)
                    .border(1.dp, Palette.Outline.copy(alpha = 0.5f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                attachment?.let { att ->
                    AttachmentChip(att, onClear = vm::clearAttachment)
                    Spacer(Modifier.height(8.dp))
                }
                if (parsingFile) {
                    Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Palette.Accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Membaca file data...", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    IconButton(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        enabled = !parsingFile && !sending,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Palette.SurfaceHigh, CircleShape),
                    ) {
                        Icon(Icons.Outlined.AttachFile, contentDescription = "Unggah file data", tint = Palette.Text)
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.take(4_000) },
                        placeholder = {
                            Text(if (attachment != null) "Tanyakan sesuatu tentang file ini (opsional)" else "Tulis pertanyaan atau unggah file")
                        },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f),
                        maxLines = 5,
                        enabled = !needsKey,
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { vm.send(input); input = "" },
                        enabled = (input.isNotBlank() || attachment != null) && !sending && !parsingFile && !needsKey,
                        modifier = Modifier.size(48.dp),
                    ) { Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Kirim") }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 84.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Hapus seluruh percakapan?") },
            text = { Text("Riwayat chat dan lampiran data di ponsel ini dihapus. Coach tidak lagi mengingat pertanyaan sebelumnya.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clear() }) { Text("Hapus", color = Palette.Error) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun AttachmentChip(att: CoachAttachment, onClear: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Palette.Accent.copy(alpha = 0.12f), MaterialTheme.shapes.medium)
            .border(1.dp, Palette.Accent.copy(alpha = 0.35f), MaterialTheme.shapes.medium)
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Description, contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(att.fileName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(att.summaryLabel, style = MetricText.Label, color = Palette.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = "Hapus lampiran", tint = Palette.TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun Bubble(m: ChatMessageEntity, onOpenKeys: () -> Unit) {
    val user = m.role == ChatRole.USER
    val error = m.role == ChatRole.ERROR
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = if (user) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            Modifier
                .widthIn(max = 340.dp)
                .background(
                    when {
                        user -> Palette.Accent.copy(alpha = 0.16f)
                        error -> Palette.Error.copy(alpha = 0.12f)
                        else -> Palette.Surface
                    },
                    MaterialTheme.shapes.large,
                )
                .border(
                    1.dp,
                    when {
                        user -> Palette.Accent.copy(alpha = 0.35f)
                        error -> Palette.Error.copy(alpha = 0.35f)
                        else -> Palette.Outline.copy(alpha = 0.6f)
                    },
                    MaterialTheme.shapes.large,
                )
                .padding(14.dp),
        ) {
            SelectionContainer {
                Text(m.content, style = MaterialTheme.typography.bodyLarge, color = if (error) Palette.Error else Palette.Text)
            }
            if (!user && m.provider != null) {
                Text(m.provider, style = MetricText.Label, color = Palette.TextMuted, modifier = Modifier.padding(top = 6.dp))
            }
            if (error) TextButton(onClick = onOpenKeys) { Text("Periksa pengaturan AI") }
        }
    }
}
