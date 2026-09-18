package com.example.util

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentSpeakingId = MutableStateFlow<Long?>(null)
    val currentSpeakingId: StateFlow<Long?> = _currentSpeakingId.asStateFlow()

    private var onDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                engine.language = Locale.ENGLISH
                // Calm, confident, friendly, and natural personal AI assistant voice tone
                engine.setPitch(0.96f)
                engine.setSpeechRate(0.98f)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentSpeakingId.value = null
                        val cb = onDoneCallback
                        onDoneCallback = null
                        cb?.invoke()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentSpeakingId.value = null
                        val cb = onDoneCallback
                        onDoneCallback = null
                        cb?.invoke()
                    }
                })
                isInitialized = true
            }
        } else {
            Log.e("VoiceManager", "TextToSpeech init failed with status: $status")
        }
    }

    fun speak(
        text: String,
        messageId: Long? = null,
        language: String? = null,
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || tts == null) {
            onDone?.invoke()
            return
        }

        stop()
        this.onDoneCallback = onDone

        // Configure language if available
        tts?.let { engine ->
            if (language == "ur") {
                val urLocale = Locale("ur", "PK")
                if (engine.isLanguageAvailable(urLocale) >= TextToSpeech.LANG_AVAILABLE) {
                    engine.language = urLocale
                }
            } else {
                engine.language = Locale.ENGLISH
            }
            engine.setPitch(0.96f)
            engine.setSpeechRate(0.98f)
        }

        // Clean markdown formatting characters before speech
        val cleanedText = text
            .replace(Regex("[#*`_~>\\[\\]]"), "")
            .replace(Regex("\\bhttps?://\\S+"), "link")
            .trim()

        if (cleanedText.isEmpty()) {
            onDone?.invoke()
            return
        }

        _currentSpeakingId.value = messageId
        _isSpeaking.value = true

        val utteranceId = messageId?.toString() ?: "jarvis_speech_${System.currentTimeMillis()}"
        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        onDoneCallback = null
        if (tts != null && tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isSpeaking.value = false
        _currentSpeakingId.value = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        fun createSpeechIntent(languageCode: String = "en-US"): Intent {
            return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS...")
            }
        }
    }
}
