package id.web.agentvaedah.strideway.ui.coach

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.web.agentvaedah.strideway.core.coach.CoachAttachment
import id.web.agentvaedah.strideway.core.coach.CoachContext
import id.web.agentvaedah.strideway.core.coach.CoachFileAnalyzer
import id.web.agentvaedah.strideway.core.coach.RecentActivity
import id.web.agentvaedah.strideway.core.physio.TrainingLoad
import id.web.agentvaedah.strideway.data.db.ChatMessageEntity
import id.web.agentvaedah.strideway.data.db.ChatRole
import id.web.agentvaedah.strideway.data.files.TrackFileIo
import id.web.agentvaedah.strideway.data.prefs.SettingsRepository
import id.web.agentvaedah.strideway.data.remote.llm.LlmClient
import id.web.agentvaedah.strideway.data.remote.llm.LlmMessage
import id.web.agentvaedah.strideway.data.remote.llm.LlmProviderId
import id.web.agentvaedah.strideway.data.remote.llm.LlmResult
import id.web.agentvaedah.strideway.data.remote.llm.LlmSettings
import id.web.agentvaedah.strideway.data.repo.ActivityRepository
import id.web.agentvaedah.strideway.data.repo.ChatRepository
import id.web.agentvaedah.strideway.data.security.SecureKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CoachViewModel @Inject constructor(
    private val chat: ChatRepository,
    private val llm: LlmClient,
    private val settings: SettingsRepository,
    private val activities: ActivityRepository,
    private val files: TrackFileIo,
    keys: SecureKeyStore,
) : ViewModel() {
    val messages: StateFlow<List<ChatMessageEntity>?> = chat.messages().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val llmSettings: StateFlow<LlmSettings> = settings.llm.stateIn(viewModelScope, SharingStarted.Eagerly, LlmSettings())

    val hasKey: StateFlow<Boolean> = settings.llm
        .flatMapLatest { keys.hasKey(it.provider) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _attachment = MutableStateFlow<CoachAttachment?>(null)
    val attachment: StateFlow<CoachAttachment?> = _attachment.asStateFlow()

    private val _parsingFile = MutableStateFlow(false)
    val parsingFile: StateFlow<Boolean> = _parsingFile.asStateFlow()

    private val _attachError = MutableStateFlow<String?>(null)
    val attachError: StateFlow<String?> = _attachError.asStateFlow()

    /** Keeps recently uploaded file data available to follow-up turns in this session. */
    private val uploadedContexts = ArrayList<String>()

    fun attachFile(uri: Uri) {
        if (_parsingFile.value) return
        _parsingFile.value = true
        _attachError.value = null
        viewModelScope.launch {
            try {
                val raw = files.readRaw(uri).getOrElse {
                    _attachError.value = it.message ?: "File tidak bisa dibaca."
                    return@launch
                }
                val athlete = settings.athlete.first()
                val parsed = withContext(Dispatchers.Default) {
                    CoachFileAnalyzer.analyze(raw.first, raw.second, athlete)
                }
                parsed.onSuccess { _attachment.value = it }
                    .onFailure { _attachError.value = it.message ?: "File tidak bisa dianalisis." }
            } finally {
                _parsingFile.value = false
            }
        }
    }

    fun clearAttachment() {
        _attachment.value = null
    }

    fun clearAttachError() {
        _attachError.value = null
    }

    fun send(text: String) {
        val trimmed = text.trim()
        val pending = _attachment.value
        if ((trimmed.isEmpty() && pending == null) || _sending.value) return
        _sending.value = true
        _attachment.value = null
        viewModelScope.launch {
            try {
                ask(trimmed, pending)
            } finally {
                _sending.value = false
            }
        }
    }

    private suspend fun ask(trimmed: String, pending: CoachAttachment?) {
        val cfg = settings.llm.first()
        val displayMsg = if (pending != null) {
            val prompt = trimmed.ifEmpty { "Analisis data dari file ${pending.fileName} ini dan berikan evaluasi serta saran latihan." }
            "[File: ${pending.fileName} · ${pending.summaryLabel}]\n$prompt"
        } else {
            trimmed
        }
        if (pending != null) {
            uploadedContexts += pending.promptContext
            if (uploadedContexts.size > 3) uploadedContexts.removeAt(0)
        }
        chat.add(ChatRole.USER, displayMsg, null)
        // Error bubbles are shown to the athlete but never sent back to the model.
        val history = chat.history()
            .filter { it.role == ChatRole.USER || it.role == ChatRole.ASSISTANT }
            .takeLast(HISTORY_LIMIT)
            .map { LlmMessage(it.role, it.content) }
            .dropWhile { it.role != ChatRole.USER }
        val system = buildString {
            append(systemPrompt())
            if (uploadedContexts.isNotEmpty()) {
                appendLine()
                appendLine()
                appendLine("Data file yang diunggah pengguna untuk dianalisis:")
                uploadedContexts.forEach { appendLine(it) }
            }
        }
        when (val r = llm.complete(cfg, system, history)) {
            is LlmResult.Ok -> chat.add(ChatRole.ASSISTANT, r.text, providerLabel(cfg))
            is LlmResult.Failure -> chat.add(ChatRole.ERROR, r.reason, providerLabel(cfg))
        }
    }

    fun clear() = viewModelScope.launch {
        uploadedContexts.clear()
        _attachment.value = null
        chat.clear()
    }

    private fun providerLabel(cfg: LlmSettings) = "${cfg.provider.label} · ${cfg.model}"

    private suspend fun systemPrompt(): String {
        val zone = ZoneId.systemDefault()
        val athlete = settings.athlete.first()
        val list = activities.activities().first()
        val today = LocalDate.now(zone).toEpochDay()
        val loads = list.map { TrainingLoad.DailyLoad(Instant.ofEpochMilli(it.startMs).atZone(zone).toLocalDate().toEpochDay(), it.trimp) }
        val fitness = if (list.isEmpty()) null else TrainingLoad.series(loads, today, today).lastOrNull()
        val recent = list.take(5).map { RecentActivity(it.sport, it.startMs, it.distanceM, it.movingMs, it.elevationGainM, it.avgHr) }
        return CoachContext.systemPrompt(athlete, fitness, recent, zone)
    }

    private companion object {
        const val HISTORY_LIMIT = 20
    }
}

@HiltViewModel
class CoachKeysViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val keys: SecureKeyStore,
    private val llm: LlmClient,
) : ViewModel() {
    val active: StateFlow<LlmSettings?> = settings.llm.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _editing = MutableStateFlow<LlmSettings?>(null)
    val editing: StateFlow<LlmSettings?> = _editing.asStateFlow()

    private val _hasKey = MutableStateFlow(false)
    val hasKey: StateFlow<Boolean> = _hasKey.asStateFlow()

    private val _status = MutableStateFlow<KeyStatus>(KeyStatus.Idle)
    val status: StateFlow<KeyStatus> = _status.asStateFlow()

    init {
        viewModelScope.launch { select(settings.llm.first().provider) }
    }

    fun select(provider: LlmProviderId) {
        viewModelScope.launch {
            _editing.value = settings.llmFor(provider)
            _hasKey.value = keys.hasKey(provider).first()
            _status.value = KeyStatus.Idle
        }
    }

    fun edit(transform: (LlmSettings) -> LlmSettings) {
        _editing.value = _editing.value?.let(transform)
        _status.value = KeyStatus.Idle
    }

    /** Saves model, base URL and (if typed) a new key, then makes this provider the active one. */
    fun save(newKey: String) {
        val cfg = _editing.value ?: return
        viewModelScope.launch {
            if (newKey.isNotBlank()) keys.put(cfg.provider, newKey)
            settings.saveLlm(cfg)
            _hasKey.value = keys.hasKey(cfg.provider).first()
            _status.value = KeyStatus.Saved
        }
    }

    fun test(newKey: String) {
        val cfg = _editing.value ?: return
        _status.value = KeyStatus.Testing
        viewModelScope.launch {
            if (newKey.isNotBlank()) {
                keys.put(cfg.provider, newKey)
                _hasKey.value = true
            }
            _status.value = when (val r = llm.test(cfg)) {
                is LlmResult.Ok -> KeyStatus.TestOk
                is LlmResult.Failure -> KeyStatus.TestFailed(r.reason)
            }
        }
    }

    fun removeKey() {
        val cfg = _editing.value ?: return
        viewModelScope.launch {
            keys.remove(cfg.provider)
            _hasKey.value = false
            _status.value = KeyStatus.Removed
        }
    }
}

sealed interface KeyStatus {
    data object Idle : KeyStatus
    data object Saved : KeyStatus
    data object Removed : KeyStatus
    data object Testing : KeyStatus
    data object TestOk : KeyStatus
    data class TestFailed(val reason: String) : KeyStatus
}
