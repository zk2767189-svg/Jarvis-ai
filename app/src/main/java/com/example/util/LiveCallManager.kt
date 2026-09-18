package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class LiveCallStatus {
    IDLE,
    CONNECTING,
    RECONNECTING,
    LISTENING,
    THINKING,
    SPEAKING,
    INTERRUPTED,
    ERROR,
    ENDED
}

class LiveCallManager(
    private val context: Context,
    private val voiceManager: VoiceManager,
    private val onExecutePhoneTool: ((name: String, args: org.json.JSONObject) -> Pair<Boolean, String>)? = null,
    private val onProcessUserQuery: suspend (String, (String, String) -> Unit) -> Unit
) {
    companion object {
        private const val TAG = "LiveCallManager"

        val STOP_KEYWORDS = listOf(
            "stop", "wait", "hold on", "pause", "quiet", "shut up",
            "خاموش شه", "خاموش", "روک", "چپ", "بس", "bas", "ruko", "thehro"
        )

        val WAKE_KEYWORDS = listOf(
            "jarvis", "جارویس", "hey jarvis", "ok jarvis", "hi jarvis"
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var speechRecognizer: SpeechRecognizer? = null
    private var timerJob: Job? = null
    private var isCallActive = false

    private val _callStatus = MutableStateFlow(LiveCallStatus.IDLE)
    val callStatus: StateFlow<LiveCallStatus> = _callStatus.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0L)
    val callDurationSeconds: StateFlow<Long> = _callDurationSeconds.asStateFlow()

    private val _audioRms = MutableStateFlow(0.15f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _userTranscript = MutableStateFlow("")
    val userTranscript: StateFlow<String> = _userTranscript.asStateFlow()

    private val _jarvisReply = MutableStateFlow("")
    val jarvisReply: StateFlow<String> = _jarvisReply.asStateFlow()

    private val _detectedLanguage = MutableStateFlow("English")
    val detectedLanguage: StateFlow<String> = _detectedLanguage.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _preferredLanguage = MutableStateFlow("auto")
    val preferredLanguage: StateFlow<String> = _preferredLanguage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Persistent WebSocket Client for Gemini Live API
    private var liveWebSocketClient: GeminiLiveWebSocketClient? = null
    private var isLiveWsConnected = false

    fun setPreferredLanguage(langCode: String) {
        _preferredLanguage.value = langCode
        if (_callStatus.value == LiveCallStatus.LISTENING) {
            restartListening()
        }
    }

    fun startCall(customApiKey: String? = null) {
        if (isCallActive) return
        isCallActive = true
        _callStatus.value = LiveCallStatus.CONNECTING
        _callDurationSeconds.value = 0L
        _userTranscript.value = ""
        _errorMessage.value = null
        _jarvisReply.value = "Connecting real-time neural link..."

        startTimer()

        // Initialize and connect Gemini Live WebSocket
        initializeWebSocket(customApiKey)

        // Speak warm, natural, calm JARVIS opening greeting
        val openingGreeting = "JARVIS live voice link online. How can I help you, Boss?"
        _jarvisReply.value = openingGreeting
        _callStatus.value = LiveCallStatus.SPEAKING

        voiceManager.speak(
            text = openingGreeting,
            onDone = {
                if (isCallActive && !_isMuted.value) {
                    _callStatus.value = LiveCallStatus.LISTENING
                    startListening()
                }
            }
        )
    }

    private fun initializeWebSocket(customApiKey: String?) {
        liveWebSocketClient = GeminiLiveWebSocketClient(
            context = context,
            onToolCallReceived = { name, args, callId ->
                mainHandler.post {
                    handleLiveToolCall(name, args, callId)
                }
            },
            onTextReceived = { liveText ->
                mainHandler.post {
                    if (isCallActive) {
                        _jarvisReply.value = liveText
                    }
                }
            },
            onAudioReceived = {
                // Multimodal audio chunk from Gemini Live
                // Audio rendering is paired with VoiceManager / PCM
            },
            onTurnComplete = {
                mainHandler.post {
                    if (isCallActive && !_isMuted.value && _callStatus.value == LiveCallStatus.SPEAKING) {
                        _callStatus.value = LiveCallStatus.LISTENING
                        startListening()
                    }
                }
            },
            onInterrupted = {
                mainHandler.post {
                    interrupt()
                }
            },
            onStatusChanged = { state, errorMsg ->
                mainHandler.post {
                    when (state) {
                        GeminiLiveWebSocketClient.SessionState.CONNECTED -> {
                            isLiveWsConnected = true
                            _errorMessage.value = null
                            if (_callStatus.value == LiveCallStatus.CONNECTING) {
                                _callStatus.value = LiveCallStatus.LISTENING
                            }
                        }
                        GeminiLiveWebSocketClient.SessionState.RECONNECTING -> {
                            _callStatus.value = LiveCallStatus.RECONNECTING
                            _errorMessage.value = errorMsg
                        }
                        GeminiLiveWebSocketClient.SessionState.ERROR -> {
                            isLiveWsConnected = false
                            _errorMessage.value = errorMsg
                            _callStatus.value = LiveCallStatus.ERROR
                            _jarvisReply.value = errorMsg ?: "Live connection failed"
                        }
                        GeminiLiveWebSocketClient.SessionState.DISCONNECTED -> {
                            isLiveWsConnected = false
                        }
                        GeminiLiveWebSocketClient.SessionState.CONNECTING -> {
                            if (_callStatus.value != LiveCallStatus.SPEAKING) {
                                _callStatus.value = LiveCallStatus.CONNECTING
                            }
                        }
                    }
                }
            }
        )

        liveWebSocketClient?.connect(customApiKey)
    }

    private fun handleLiveToolCall(name: String, args: org.json.JSONObject, callId: String) {
        val result = onExecutePhoneTool?.invoke(name, args) ?: Pair(false, "Tool not handled")
        val responseJson = org.json.JSONObject().apply {
            put("status", if (result.first) "success" else "failure")
            put("details", result.second)
        }
        liveWebSocketClient?.sendToolResponse(callId, name, responseJson)

        val spokenConfirmation = result.second
        _jarvisReply.value = spokenConfirmation
        _callStatus.value = LiveCallStatus.SPEAKING
        voiceManager.speak(spokenConfirmation) {
            if (isCallActive && !_isMuted.value) {
                _callStatus.value = LiveCallStatus.LISTENING
                startListening()
            }
        }
    }

    fun endCall() {
        isCallActive = false
        stopTimer()
        liveWebSocketClient?.disconnect()
        liveWebSocketClient = null
        isLiveWsConnected = false
        voiceManager.stop()
        stopListening()
        _callStatus.value = LiveCallStatus.ENDED
        _audioRms.value = 0f
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        if (newMute) {
            stopListening()
            voiceManager.stop()
            _callStatus.value = LiveCallStatus.INTERRUPTED
        } else {
            if (_callStatus.value != LiveCallStatus.SPEAKING && _callStatus.value != LiveCallStatus.THINKING) {
                _callStatus.value = LiveCallStatus.LISTENING
                startListening()
            }
        }
    }

    /**
     * Immediate manual or keyword interruption:
     * If user says "Stop", "Wait", "خاموش شه", or taps interrupt button.
     */
    fun interrupt() {
        Log.d(TAG, "Interrupt triggered by user")
        voiceManager.stop()
        _callStatus.value = LiveCallStatus.INTERRUPTED
        _jarvisReply.value = "Understood Boss. Listening..."

        mainHandler.postDelayed({
            if (isCallActive && !_isMuted.value) {
                _callStatus.value = LiveCallStatus.LISTENING
                startListening()
            }
        }, 300)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && isCallActive) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun startListening() {
        if (!isCallActive || _isMuted.value) return

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    if (SpeechRecognizer.isRecognitionAvailable(context)) {
                        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                            setRecognitionListener(createRecognitionListener())
                        }
                    } else {
                        Log.w(TAG, "SpeechRecognizer not available on device")
                        return@post
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    val pref = _preferredLanguage.value
                    if (pref != "auto") {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, pref)
                    } else {
                        // Allow auto language detection by standard Recognizer
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    }
                }

                speechRecognizer?.startListening(intent)
                _callStatus.value = LiveCallStatus.LISTENING
            } catch (e: Exception) {
                Log.e(TAG, "Failed starting speech recognizer", e)
            }
        }
    }

    private fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recognizer", e)
            }
        }
    }

    private fun restartListening() {
        stopListening()
        mainHandler.postDelayed({
            if (isCallActive && !_isMuted.value && _callStatus.value == LiveCallStatus.LISTENING) {
                startListening()
            }
        }, 200)
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _audioRms.value = 0.2f
        }

        override fun onBeginningOfSpeech() {
            _audioRms.value = 0.5f
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize RMS from dB (-2..10 approx) to 0.0 .. 1.0 for UI visualizer
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
            _audioRms.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _audioRms.value = 0.15f
        }

        override fun onError(error: Int) {
            _audioRms.value = 0.1f
            // If recognizer had no speech or error, restart listening loop if call is active
            if (isCallActive && !_isMuted.value && _callStatus.value == LiveCallStatus.LISTENING) {
                mainHandler.postDelayed({
                    if (isCallActive && _callStatus.value == LiveCallStatus.LISTENING) {
                        startListening()
                    }
                }, 500)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val spokenText = matches?.firstOrNull()?.trim().orEmpty()
            handleSpokenInput(spokenText)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim().orEmpty()
            if (partial.isNotBlank()) {
                _userTranscript.value = partial

                // Interruption check: If user says "Stop", "Wait", or "خاموش شه"
                val lowerPartial = partial.lowercase()
                if (STOP_KEYWORDS.any { lowerPartial.contains(it) }) {
                    interrupt()
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun processSpokenInput(spokenText: String) {
        handleSpokenInput(spokenText)
    }

    private fun handleSpokenInput(spokenText: String) {
        if (spokenText.isBlank()) {
            if (isCallActive && !_isMuted.value && _callStatus.value == LiveCallStatus.LISTENING) {
                startListening()
            }
            return
        }

        _userTranscript.value = spokenText
        val lower = spokenText.lowercase().trim()

        // 1. Interruption check
        if (STOP_KEYWORDS.any { lower.contains(it) }) {
            interrupt()
            return
        }

        // 2. Wake / Attention check: If user says "JARVIS"
        if (WAKE_KEYWORDS.any { lower.contains(it) } && lower.split(" ").size <= 2) {
            voiceManager.stop()
            _callStatus.value = LiveCallStatus.SPEAKING
            val promptReply = "Yes Boss? At your command."
            _jarvisReply.value = promptReply
            voiceManager.speak(promptReply) {
                if (isCallActive && !_isMuted.value) {
                    _callStatus.value = LiveCallStatus.LISTENING
                    startListening()
                }
            }
            return
        }

        // 3. Process query through Jarvis system
        _callStatus.value = LiveCallStatus.THINKING
        _audioRms.value = 0.35f

        scope.launch {
            onProcessUserQuery(spokenText) { replyText, lang ->
                if (!isCallActive) return@onProcessUserQuery

                _jarvisReply.value = replyText
                _detectedLanguage.value = when (lang) {
                    "ur" -> "Urdu (اردو)"
                    "ps" -> "Pashto (پښتو)"
                    "roman_ur" -> "Roman Urdu"
                    else -> "English"
                }

                _callStatus.value = LiveCallStatus.SPEAKING
                voiceManager.speak(
                    text = replyText,
                    language = lang,
                    onDone = {
                        if (isCallActive && !_isMuted.value) {
                            _callStatus.value = LiveCallStatus.LISTENING
                            startListening()
                        }
                    }
                )
            }
        }
    }
}
