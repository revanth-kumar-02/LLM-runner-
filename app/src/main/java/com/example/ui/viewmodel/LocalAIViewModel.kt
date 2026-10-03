package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.hardware.HardwareTelemetryProvider
import com.example.data.model.AppSettings
import com.example.data.model.ChatMessage
import com.example.data.model.HardwareTelemetry
import com.example.data.model.ModelCapability
import com.example.data.model.ModelMetadata
import com.example.data.repository.ConversationRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.manager.ModelManager
import com.example.engine.ModelLoadingState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LocalAIViewModel @JvmOverloads constructor(
    application: Application,
    private val modelManager: ModelManager = ModelManager(application),
    private val conversationRepository: ConversationRepository = ConversationRepository(application),
    private val settingsRepository: SettingsRepository = SettingsRepository(application),
    private val telemetryProvider: HardwareTelemetryProvider = HardwareTelemetryProvider(application)
) : AndroidViewModel(application) {

    val models: StateFlow<List<ModelMetadata>> = modelManager.models
    val activeModel: StateFlow<ModelMetadata?> = modelManager.activeModel
    val chatMessages: StateFlow<List<ChatMessage>> = conversationRepository.messages
    val appSettings: StateFlow<AppSettings> = settingsRepository.settings

    // Dynamic model capabilities derived from the currently active model
    val activeCapabilities: StateFlow<Set<ModelCapability>> = activeModel
        .map { it?.capabilities ?: setOf(ModelCapability.TEXT) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, setOf(ModelCapability.TEXT))

    val isVisionSupported: StateFlow<Boolean> = activeCapabilities
        .map { it.contains(ModelCapability.VISION) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isAudioSupported: StateFlow<Boolean> = activeCapabilities
        .map { it.contains(ModelCapability.AUDIO) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _telemetry = MutableStateFlow(
        telemetryProvider.getTelemetry(
            context = application,
            chatsFile = conversationRepository.chatFile
        )
    )
    val telemetry: StateFlow<HardwareTelemetry> = _telemetry.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _currentTokensCount = MutableStateFlow(0)
    val currentTokensCount: StateFlow<Int> = _currentTokensCount.asStateFlow()

    private val _currentStreamingMsgId = MutableStateFlow<String?>(null)
    val currentStreamingMsgId: StateFlow<String?> = _currentStreamingMsgId.asStateFlow()

    private val _loadingState = MutableStateFlow<ModelLoadingState?>(null)
    val loadingState: StateFlow<ModelLoadingState?> = _loadingState.asStateFlow()

    private val _loadingModel = MutableStateFlow<ModelMetadata?>(null)
    val loadingModel: StateFlow<ModelMetadata?> = _loadingModel.asStateFlow()

    private val _selectedModel = MutableStateFlow<ModelMetadata?>(null)
    val selectedModel: StateFlow<ModelMetadata?> = _selectedModel.asStateFlow()

    private var generationJob: Job? = null
    private var loadingJob: Job? = null

    init {
        refreshTelemetry()
        // If an active model was previously chosen, load it into volatile RAM in the background
        viewModelScope.launch {
            val current = activeModel.value
            if (current != null && !modelManager.engine.isModelLoaded()) {
                val threads = appSettings.value.cpuThreads
                val ctxLength = appSettings.value.contextLength
                modelManager.loadModel(current.id, threads, ctxLength).collect { state ->
                    _loadingState.value = state
                }
            }
        }
    }

    fun refreshTelemetry() {
        _telemetry.value = telemetryProvider.getTelemetry(
            context = getApplication(),
            chatsFile = conversationRepository.chatFile
        )
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun selectModelForDetails(modelId: String) {
        _selectedModel.value = models.value.find { it.id == modelId }
    }

    fun importModel(uri: Uri): ModelMetadata {
        val imported = modelManager.importModel(uri, _telemetry.value)
        _selectedModel.value = imported
        refreshTelemetry()
        return imported
    }

    fun linkProjector(modelId: String, uri: Uri) {
        val path = "Android/data/storage/${uri.lastPathSegment ?: "projector.gguf"}"
        modelManager.linkProjector(modelId, path, _telemetry.value)
        selectModelForDetails(modelId)
    }

    fun startModelLoading(modelId: String, onCompleted: (() -> Unit)? = null) {
        val targetModel = models.value.find { it.id == modelId } ?: return
        loadingJob?.cancel()
        _loadingModel.value = targetModel

        val threads = appSettings.value.cpuThreads
        val ctxLength = appSettings.value.contextLength

        loadingJob = viewModelScope.launch {
            modelManager.loadModel(modelId, threads, ctxLength).collect { state ->
                _loadingState.value = state
                if (state.isComplete) {
                    settingsRepository.setActiveModelId(modelId)
                    refreshTelemetry()
                    onCompleted?.invoke()
                }
            }
        }
    }

    fun cancelModelLoading() {
        loadingJob?.cancel()
        _loadingModel.value?.let { model ->
            modelManager.cancelLoading(model.id)
        }
        _loadingState.value = null
        _loadingModel.value = null
    }

    fun unloadModel(modelId: String) {
        modelManager.unloadModel(modelId)
        if (settingsRepository.settings.value.activeModelId == modelId) {
            settingsRepository.setActiveModelId(null)
        }
        refreshTelemetry()
    }

    fun removeModel(modelId: String) {
        modelManager.removeModel(modelId)
        if (settingsRepository.settings.value.activeModelId == modelId) {
            settingsRepository.setActiveModelId(null)
        }
        refreshTelemetry()
    }

    fun sendMessage(
        overrideText: String? = null,
        attachedImageUri: String? = null,
        attachedAudioDuration: Int? = null
    ) {
        val active = activeModel.value
        if (active == null) {
            // Inference requires an active loaded GGUF model
            return
        }

        val textToSend = (overrideText ?: _inputText.value).trim()
        if (textToSend.isEmpty() && attachedImageUri == null && attachedAudioDuration == null) return
        if (_isGenerating.value) return

        _inputText.value = ""
        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val modelLabel = active.name

        // 1. Add user message
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = textToSend,
            timestamp = "$timeStr · $modelLabel",
            isStreaming = false,
            attachedImageUri = attachedImageUri,
            attachedAudioDurationSec = attachedAudioDuration
        )
        conversationRepository.addMessage(userMsg)

        // 2. Add streaming assistant message
        val assistantMsgId = UUID.randomUUID().toString()
        val assistantMsg = ChatMessage(
            id = assistantMsgId,
            isUser = false,
            text = "",
            timestamp = timeStr,
            isStreaming = true,
            tokenRate = null,
            tokenCount = 0,
            modelName = modelLabel
        )
        conversationRepository.addMessage(assistantMsg)
        _currentStreamingMsgId.value = assistantMsgId
        _isGenerating.value = true
        _currentTokensCount.value = 0

        generationJob = viewModelScope.launch {
            val temp = appSettings.value.temperature
            val maxTokens = appSettings.value.maxResponseTokens
            val hasImages = if (attachedImageUri != null) listOf(ByteArray(1)) else emptyList()
            val audioBytes = if (attachedAudioDuration != null) ByteArray(1) else null

            // Ensure model is loaded in memory before generating
            if (!modelManager.engine.isModelLoaded()) {
                android.util.Log.i("LocalAIViewModel", "Active model ${active.name} is not loaded in RAM. Loading now...")
                val threads = appSettings.value.cpuThreads
                val ctxLength = appSettings.value.contextLength
                var loadSuccess = false
                modelManager.loadModel(active.id, threads, ctxLength).collect { state ->
                    _loadingState.value = state
                    if (state.isComplete) loadSuccess = true
                }
                if (!loadSuccess) {
                    val errMsg = "Failed to load ${active.name} into device RAM. Please check storage & memory."
                    val errUpdate = assistantMsg.copy(
                        text = errMsg,
                        isStreaming = false
                    )
                    conversationRepository.updateMessage(errUpdate)
                    _isGenerating.value = false
                    _currentStreamingMsgId.value = null
                    return@launch
                }
            }

            val enableThinking = appSettings.value.enableThinking
            modelManager.engine.generateStream(textToSend, hasImages, audioBytes, temp, maxTokens, enableThinking).collect { chunk ->
                _currentTokensCount.value = chunk.currentTokensCount
                val filteredText = stripThinkingProcess(chunk.accumulatedText, chunk.isDone)
                val (snippet, lang, cleanText) = extractCodeIfPresent(filteredText)

                val updated = assistantMsg.copy(
                    text = cleanText,
                    codeSnippet = snippet,
                    codeLanguage = lang,
                    tokenCount = chunk.currentTokensCount,
                    tokenRate = null, // Hide live token counters from UI
                    isStreaming = !chunk.isDone
                )
                conversationRepository.updateMessage(updated)

                if (chunk.isDone) {
                    _isGenerating.value = false
                    _currentStreamingMsgId.value = null
                    refreshTelemetry()
                }
            }
        }
    }

    fun stopGenerating() {
        generationJob?.cancel()
        modelManager.engine.stopGeneration()
        _currentStreamingMsgId.value?.let { msgId ->
            chatMessages.value.find { it.id == msgId }?.let { msg ->
                conversationRepository.updateMessage(msg.copy(isStreaming = false))
            }
        }
        _isGenerating.value = false
        _currentStreamingMsgId.value = null
    }

    fun clearAllConversations() {
        conversationRepository.clearConversations()
        refreshTelemetry()
    }

    fun updateContextLength(newLength: Int) {
        settingsRepository.updateContextLength(newLength)
    }

    fun updateTemperature(temp: Float) {
        settingsRepository.updateTemperature(temp)
    }

    fun updateCpuThreads(threads: Int) {
        settingsRepository.updateCpuThreads(threads)
    }

    fun toggleConversationHistory(enabled: Boolean) {
        settingsRepository.updateHistoryEnabled(enabled)
    }

    fun updateEnableThinking(enabled: Boolean) {
        settingsRepository.updateEnableThinking(enabled)
    }

    fun completeOnboarding() {
        settingsRepository.setOnboardingCompleted(true)
    }

    private fun stripThinkingProcess(raw: String, isDone: Boolean): String {
        if (raw.isEmpty()) return ""

        val trimmedLeading = raw.trimStart()
        if (trimmedLeading.startsWith("<think>")) {
            if (trimmedLeading.contains("</think>")) {
                val afterThink = trimmedLeading.substringAfter("</think>").trimStart()
                return afterThink.replace(Regex("<think>[\\s\\S]*?</think>"), "").trimStart()
            } else {
                return "" // Inside <think> block: keep UI completely clean
            }
        } else if (trimmedLeading.startsWith("<") && "<think>".startsWith(trimmedLeading)) {
            return "" // Partial opening tag streaming
        }

        if (raw.contains("<think>")) {
            if (raw.contains("</think>")) {
                return raw.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
            } else {
                return raw.substringBefore("<think>").trim()
            }
        }

        return raw
    }

    private fun extractCodeIfPresent(raw: String): Triple<String?, String?, String> {
        val regex = Regex("```([a-zA-Z0-9_-]*)\\n([\\s\\S]*?)```")
        val match = regex.find(raw)
        return if (match != null) {
            val lang = match.groupValues[1].ifEmpty { "python" }
            val code = match.groupValues[2].trimEnd()
            val remaining = raw.replace(match.value, "").trim()
            Triple(code, lang, remaining)
        } else {
            Triple(null, null, raw)
        }
    }
}
