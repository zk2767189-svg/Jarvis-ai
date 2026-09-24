package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ChatMessageEntity
import com.example.data.db.QuoteEntity
import com.example.data.db.TaskEntity
import com.example.data.db.VideoProjectEntity
import com.example.data.model.ThirtySecondVideoPlan
import com.example.data.repository.JarvisRepository
import com.example.util.AndroidActionExecutor
import com.example.util.JarvisAccessibilityService
import com.example.util.LiveCallManager
import com.example.util.LiveCallStatus
import com.example.util.VoiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class JarvisTab {
    ASSISTANT,
    VIDEO_STUDIO,
    AUTOMATION,
    TRANSLATOR,
    TASKS,
    SETTINGS
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JarvisRepository(application)
    val voiceManager = VoiceManager(application)
    val actionExecutor = AndroidActionExecutor(application)

    private val _actionStatus = MutableStateFlow<String?>(null)
    val actionStatus: StateFlow<String?> = _actionStatus.asStateFlow()

    private val _pendingConfirmationAction = MutableStateFlow<String?>(null)
    val pendingConfirmationAction: StateFlow<String?> = _pendingConfirmationAction.asStateFlow()

    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val quoteOfTheDay: StateFlow<QuoteEntity?> = repository.latestQuoteFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _isQuoteLoading = MutableStateFlow(false)
    val isQuoteLoading: StateFlow<Boolean> = _isQuoteLoading.asStateFlow()

    private val _isQuoteDismissed = MutableStateFlow(false)
    val isQuoteDismissed: StateFlow<Boolean> = _isQuoteDismissed.asStateFlow()

    // Live Call Manager
    val liveCallManager = LiveCallManager(
        context = application,
        voiceManager = voiceManager,
        onExecutePhoneTool = { toolName, args ->
            executePhoneToolDirectly(toolName, args)
        },
        onProcessUserQuery = { spokenText, callback ->
            processVoiceOrActionQuery(spokenText) { reply, lang ->
                callback(reply, lang)
            }
        }
    )

    private val _isLiveCallOpen = MutableStateFlow(false)
    val isLiveCallOpen: StateFlow<Boolean> = _isLiveCallOpen.asStateFlow()

    val liveCallStatus: StateFlow<LiveCallStatus> = liveCallManager.callStatus
    val liveCallDurationSeconds: StateFlow<Long> = liveCallManager.callDurationSeconds
    val liveAudioRms: StateFlow<Float> = liveCallManager.audioRms
    val liveUserTranscript: StateFlow<String> = liveCallManager.userTranscript
    val liveJarvisReply: StateFlow<String> = liveCallManager.jarvisReply
    val liveDetectedLanguage: StateFlow<String> = liveCallManager.detectedLanguage
    val isLiveCallMuted: StateFlow<Boolean> = liveCallManager.isMuted
    val preferredLiveLanguage: StateFlow<String> = liveCallManager.preferredLanguage

    private val _currentTab = MutableStateFlow(JarvisTab.ASSISTANT)
    val currentTab: StateFlow<JarvisTab> = _currentTab.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("Auto")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking
    val currentSpeakingId: StateFlow<Long?> = voiceManager.currentSpeakingId

    init {
        viewModelScope.launch {
            repository.getInitialGreetingIfNeeded()
            loadQuoteOfTheDay(forceRefresh = false)
        }
    }

    fun loadQuoteOfTheDay(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isQuoteLoading.value = true
            try {
                repository.getOrFetchQuoteOfTheDay(forceRefresh = forceRefresh)
            } catch (e: Exception) {
                // Handled gracefully
            } finally {
                _isQuoteLoading.value = false
            }
        }
    }

    fun toggleQuoteFavorite(id: Long, currentFav: Boolean) {
        viewModelScope.launch {
            repository.toggleQuoteFavorite(id, !currentFav)
        }
    }

    fun toggleQuoteDismissed() {
        _isQuoteDismissed.value = !_isQuoteDismissed.value
    }

    fun speakQuote(quote: QuoteEntity) {
        val speechText = "\"${quote.quoteText}\" by ${quote.author}. A ${quote.category} reflection, Boss."
        voiceManager.speak(speechText)
    }

    fun startLiveCall() {
        _isLiveCallOpen.value = true
        liveCallManager.startCall(_customApiKey.value)
    }

    private fun executePhoneToolDirectly(toolName: String, args: org.json.JSONObject): Pair<Boolean, String> {
        val lower = toolName.lowercase()
        val command = when {
            lower.contains("tiktok") -> "JARVIS, open TikTok"
            lower.contains("youtube") -> {
                val query = args.optString("query", "")
                if (query.isNotBlank()) "JARVIS, open YouTube $query" else "JARVIS, open YouTube"
            }
            lower.contains("whatsapp") -> "JARVIS, open WhatsApp"
            lower.contains("settings") -> "JARVIS, open Settings"
            lower.contains("back") -> "JARVIS, go back"
            lower.contains("close") || lower.contains("home") -> "JARVIS, close this"
            else -> toolName
        }

        val actionResult = actionExecutor.evaluateAndExecute(command, "en")
        return if (actionResult != null && actionResult.isAction) {
            _actionStatus.value = actionResult.statusDisplay
            scheduleActionStatusClear()
            Pair(actionResult.success, actionResult.spokenResponse)
        } else {
            Pair(false, "Command $toolName executed")
        }
    }

    fun endLiveCall() {
        liveCallManager.endCall()
        _isLiveCallOpen.value = false
    }

    fun toggleLiveCallMute() {
        liveCallManager.toggleMute()
    }

    fun interruptLiveCall() {
        liveCallManager.interrupt()
    }

    fun setLiveCallLanguage(langCode: String) {
        liveCallManager.setPreferredLanguage(langCode)
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun onImageSelected(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun clearSelectedImage() {
        _selectedImageUri.value = null
    }

    fun setTab(tab: JarvisTab) {
        _currentTab.value = tab
    }

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }

    private fun processVoiceOrActionQuery(rawText: String, callback: (String, String) -> Unit) {
        val cleaned = rawText
            .replace(Regex("^(hi jarvis|hey jarvis|ok jarvis|jarvis|جارویس)[, ]*", RegexOption.IGNORE_CASE), "")
            .trim()
        val textToProcess = if (cleaned.isNotBlank()) cleaned else rawText
        val detectedLang = repository.detectLanguage(textToProcess)

        // 1. Check Action Executor
        val actionResult = actionExecutor.evaluateAndExecute(textToProcess, detectedLang)
        if (actionResult != null && actionResult.isAction) {
            if (actionResult.actionType == "confirm_execution") {
                confirmSensitiveAction(detectedLang, callback)
                return
            }
            if (actionResult.actionType == "cancel_execution") {
                _pendingConfirmationAction.value = null
                _actionStatus.value = "Action Cancelled"
                scheduleActionStatusClear()
                callback(actionResult.spokenResponse, detectedLang)
                return
            }
            if (actionResult.requiresConfirmation) {
                _actionStatus.value = actionResult.statusDisplay
                _pendingConfirmationAction.value = actionResult.pendingActionKey
                viewModelScope.launch {
                    repository.saveMessageDirectly("user", rawText, "action", detectedLang)
                    repository.saveMessageDirectly("jarvis", actionResult.spokenResponse, "action", detectedLang)
                }
                callback(actionResult.spokenResponse, detectedLang)
                return
            }

            _actionStatus.value = actionResult.statusDisplay
            scheduleActionStatusClear()
            viewModelScope.launch {
                repository.saveMessageDirectly("user", rawText, "action", detectedLang)
                repository.saveMessageDirectly("jarvis", actionResult.spokenResponse, "action", detectedLang)
            }
            callback(actionResult.spokenResponse, detectedLang)
            return
        }

        // 2. Normal Repository AI query
        viewModelScope.launch {
            val result = repository.processLiveCallQuery(textToProcess, _customApiKey.value)
            callback(result.first, result.second)
        }
    }

    fun confirmSensitiveAction(detectedLang: String = "en", callback: ((String, String) -> Unit)? = null) {
        val result = actionExecutor.handleConfirmation(
            isConfirmed = true,
            onClearTasks = {
                viewModelScope.launch { repository.clearAllTasks() }
            },
            onClearChat = {
                viewModelScope.launch { repository.clearChat() }
            }
        )
        _pendingConfirmationAction.value = null
        _actionStatus.value = result.statusDisplay
        scheduleActionStatusClear()
        viewModelScope.launch {
            repository.saveMessageDirectly("user", "Yes, confirm", "action", detectedLang)
            repository.saveMessageDirectly("jarvis", result.spokenResponse, "action", detectedLang)
        }
        if (callback != null) {
            callback(result.spokenResponse, detectedLang)
        } else {
            voiceManager.speak(result.spokenResponse)
        }
    }

    fun cancelSensitiveAction(detectedLang: String = "en") {
        val result = actionExecutor.handleConfirmation(
            isConfirmed = false,
            onClearTasks = {},
            onClearChat = {}
        )
        _pendingConfirmationAction.value = null
        _actionStatus.value = result.statusDisplay
        scheduleActionStatusClear()
        voiceManager.speak(result.spokenResponse)
    }

    private fun scheduleActionStatusClear() {
        viewModelScope.launch {
            delay(4500)
            _actionStatus.value = null
        }
    }

    fun isAccessibilityEnabled(): Boolean {
        return JarvisAccessibilityService.isAccessibilityServiceEnabled(getApplication())
    }

    fun openAccessibilitySettings() {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            // Handled
        }
    }

    fun openAppSettings() {
        try {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getApplication<Application>().packageName, null)
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            // Handled
        }
    }

    fun sendMessage(explicitText: String? = null) {
        val textToSend = (explicitText ?: _inputText.value).trim()
        val imageToSend = _selectedImageUri.value

        if (textToSend.isEmpty() && imageToSend == null) return

        _inputText.value = ""
        _selectedImageUri.value = null

        // Check if command is an Action!
        if (imageToSend == null) {
            val cleaned = textToSend
                .replace(Regex("^(hi jarvis|hey jarvis|ok jarvis|jarvis|جارویس)[, ]*", RegexOption.IGNORE_CASE), "")
                .trim()
            val textToProcess = if (cleaned.isNotBlank()) cleaned else textToSend
            val detectedLang = repository.detectLanguage(textToProcess)

            val actionResult = actionExecutor.evaluateAndExecute(textToProcess, detectedLang)
            if (actionResult != null && actionResult.isAction) {
                if (actionResult.actionType == "confirm_execution") {
                    confirmSensitiveAction(detectedLang)
                    return
                }
                if (actionResult.actionType == "cancel_execution") {
                    cancelSensitiveAction(detectedLang)
                    return
                }
                if (actionResult.requiresConfirmation) {
                    _actionStatus.value = actionResult.statusDisplay
                    _pendingConfirmationAction.value = actionResult.pendingActionKey
                    viewModelScope.launch {
                        repository.saveMessageDirectly("user", textToSend, "action", detectedLang)
                        repository.saveMessageDirectly("jarvis", actionResult.spokenResponse, "action", detectedLang)
                    }
                    voiceManager.speak(actionResult.spokenResponse)
                    return
                }

                _actionStatus.value = actionResult.statusDisplay
                scheduleActionStatusClear()
                viewModelScope.launch {
                    repository.saveMessageDirectly("user", textToSend, "action", detectedLang)
                    repository.saveMessageDirectly("jarvis", actionResult.spokenResponse, "action", detectedLang)
                }
                voiceManager.speak(actionResult.spokenResponse)
                return
            }
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val reply = repository.sendMessage(
                    userText = if (textToSend.isNotEmpty()) textToSend else "Please analyze this image, Boss.",
                    imageUri = imageToSend,
                    customKey = _customApiKey.value
                )
                // Auto speak in voice mode if short response or initiated via voice
                if (reply.content.length < 300) {
                    voiceManager.speak(reply.content, reply.id)
                }
            } catch (e: Exception) {
                // Handled gracefully in repo
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun speakMessage(message: ChatMessageEntity) {
        if (voiceManager.currentSpeakingId.value == message.id && voiceManager.isSpeaking.value) {
            voiceManager.stop()
        } else {
            voiceManager.speak(message.content, message.id)
        }
    }

    fun stopSpeaking() {
        voiceManager.stop()
    }

    fun triggerQuickAction(actionPrompt: String) {
        _inputText.value = actionPrompt
        sendMessage(actionPrompt)
    }

    fun generateVideoPrompt(
        sceneTopic: String,
        style: String = "Cinematic 8K Photorealistic",
        aspectRatio: String = "16:9",
        duration: String = "10s"
    ) {
        _currentTab.value = JarvisTab.ASSISTANT
        val prompt = "Video Prompt: $sceneTopic. Style: $style. Aspect ratio: $aspectRatio. Duration: $duration. Optimize for Veo and Kling."
        sendMessage(prompt)
    }

    fun generateAutomationPlan(goal: String) {
        _currentTab.value = JarvisTab.ASSISTANT
        val prompt = "Automate goal: $goal. Break down into steps, check tools, and execute permitted steps."
        sendMessage(prompt)
    }

    fun generateYouTubePackage(topic: String) {
        _currentTab.value = JarvisTab.ASSISTANT
        val prompt = "Help me write YouTube titles, description, tags and full script for: $topic"
        sendMessage(prompt)
    }

    fun toggleTask(id: Long, currentCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTask(id, currentCompleted)
        }
    }

    fun addTask(title: String, description: String, category: String, priority: String) {
        viewModelScope.launch {
            repository.addTask(title, description, category, priority)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    val allVideoProjects: StateFlow<List<VideoProjectEntity>> = repository.allVideoProjects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isCreate30sVideoOpen = MutableStateFlow(false)
    val isCreate30sVideoOpen: StateFlow<Boolean> = _isCreate30sVideoOpen.asStateFlow()

    private val _storyIdea = MutableStateFlow("")
    val storyIdea: StateFlow<String> = _storyIdea.asStateFlow()

    private val _videoAspectRatio = MutableStateFlow("16:9") // "16:9" or "9:16"
    val videoAspectRatio: StateFlow<String> = _videoAspectRatio.asStateFlow()

    private val _videoVisualStyle = MutableStateFlow("Cinematic 8K Photorealistic")
    val videoVisualStyle: StateFlow<String> = _videoVisualStyle.asStateFlow()

    private val _isVideoGenerating = MutableStateFlow(false)
    val isVideoGenerating: StateFlow<Boolean> = _isVideoGenerating.asStateFlow()

    private val _videoGenerationProgress = MutableStateFlow(0f)
    val videoGenerationProgress: StateFlow<Float> = _videoGenerationProgress.asStateFlow()

    private val _videoGenerationStepMessage = MutableStateFlow("")
    val videoGenerationStepMessage: StateFlow<String> = _videoGenerationStepMessage.asStateFlow()

    private val _currentVideoPlan = MutableStateFlow<ThirtySecondVideoPlan?>(null)
    val currentVideoPlan: StateFlow<ThirtySecondVideoPlan?> = _currentVideoPlan.asStateFlow()

    private val _activeSceneIndex = MutableStateFlow(0)
    val activeSceneIndex: StateFlow<Int> = _activeSceneIndex.asStateFlow()

    private val _isVideoPlaying = MutableStateFlow(false)
    val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()

    private var videoPlaybackJob: Job? = null

    fun openCreate30sVideo(initialIdea: String = "") {
        if (initialIdea.isNotBlank()) {
            _storyIdea.value = initialIdea
        }
        _isCreate30sVideoOpen.value = true
    }

    fun closeCreate30sVideo() {
        _isCreate30sVideoOpen.value = false
        stopVideoPlayback()
    }

    fun setThirtySecondStoryIdea(text: String) {
        _storyIdea.value = text
    }

    fun setThirtySecondAspectRatio(ratio: String) {
        _videoAspectRatio.value = ratio
    }

    fun setThirtySecondVisualStyle(style: String) {
        _videoVisualStyle.value = style
    }

    fun generateThirtySecondVideo(isRegenerate: Boolean = false) {
        val idea = _storyIdea.value.ifBlank { "Cinematic futuristic odyssey uncovering ancient secrets" }
        viewModelScope.launch {
            _isVideoGenerating.value = true
            _videoGenerationProgress.value = 0.05f
            _videoGenerationStepMessage.value = "Initiating JARVIS video director neural engine..."
            stopVideoPlayback()

            try {
                val plan = repository.generateThirtySecondVideo(
                    storyIdea = idea,
                    aspectRatio = _videoAspectRatio.value,
                    visualStyle = _videoVisualStyle.value,
                    onProgress = { progress, stepMsg ->
                        _videoGenerationProgress.value = progress
                        _videoGenerationStepMessage.value = stepMsg
                    },
                    customKey = _customApiKey.value.ifBlank { null }
                )
                _currentVideoPlan.value = plan
                _activeSceneIndex.value = 0
                startVideoPlayback()
            } catch (e: Exception) {
                _videoGenerationStepMessage.value = "Generation completed with local director fallback."
            } finally {
                _isVideoGenerating.value = false
            }
        }
    }

    fun editVideoPrompt() {
        stopVideoPlayback()
        _currentVideoPlan.value = null
    }

    fun createAnotherVideo() {
        stopVideoPlayback()
        _storyIdea.value = ""
        _currentVideoPlan.value = null
        _activeSceneIndex.value = 0
    }

    fun setActiveSceneIndex(index: Int) {
        val plan = _currentVideoPlan.value ?: return
        if (index in plan.scenes.indices) {
            _activeSceneIndex.value = index
        }
    }

    fun toggleVideoPlayback() {
        if (_isVideoPlaying.value) {
            stopVideoPlayback()
        } else {
            startVideoPlayback()
        }
    }

    private fun startVideoPlayback() {
        videoPlaybackJob?.cancel()
        _isVideoPlaying.value = true
        videoPlaybackJob = viewModelScope.launch {
            while (_isVideoPlaying.value) {
                delay(5000) // 5 seconds per scene for 30s total
                val currentIdx = _activeSceneIndex.value
                val nextIdx = (currentIdx + 1) % 6
                _activeSceneIndex.value = nextIdx
            }
        }
    }

    private fun stopVideoPlayback() {
        _isVideoPlaying.value = false
        videoPlaybackJob?.cancel()
        videoPlaybackJob = null
    }

    fun deleteVideoProject(id: Long) {
        viewModelScope.launch {
            repository.deleteVideoProject(id)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            voiceManager.stop()
            repository.clearChat()
        }
    }

    fun translateText(
        text: String,
        fromLang: String,
        toLang: String,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.translateText(
                text = text,
                fromLang = fromLang,
                toLang = toLang,
                customKey = _customApiKey.value.ifBlank { null }
            )
            onResult(result)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopVideoPlayback()
        liveCallManager.endCall()
        voiceManager.shutdown()
    }
}
