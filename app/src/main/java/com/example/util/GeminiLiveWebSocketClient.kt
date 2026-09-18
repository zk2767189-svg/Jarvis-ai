package com.example.util

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Manages a persistent WebSocket Live Session with Gemini Live Multimodal API
 * (wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent)
 *
 * Supported real-time audio/multimodal model:
 * 'gemini-2.5-flash-native-audio-preview-12-2025'
 *
 * Features:
 * - Real-time persistent bidirectional WebSocket connection
 * - Automatic reconnection with backoff if connection drops
 * - Explicit error reporting when connection fails (never pretends call is connected)
 * - Real-time client content streaming (audio PCM / text / tools)
 * - Automatic function/tool calling dispatch (e.g. openTikTok, openYouTube, openWhatsApp, openSettings, goBack)
 * - Real-time interruption notification
 */
class GeminiLiveWebSocketClient(
    private val context: Context,
    private val onToolCallReceived: (name: String, args: JSONObject, callId: String) -> Unit,
    private val onTextReceived: (String) -> Unit,
    private val onAudioReceived: (ByteArray) -> Unit,
    private val onTurnComplete: () -> Unit,
    private val onInterrupted: () -> Unit,
    private val onStatusChanged: (SessionState, String?) -> Unit
) {
    companion object {
        private const val TAG = "GeminiLiveWebSocket"
        const val LIVE_MODEL = "models/gemini-2.5-flash-native-audio-preview-12-2025"
        private const val WS_HOST = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"

        const val LIVE_SYSTEM_PROMPT = """
You are JARVIS, my advanced real-time personal voice AI assistant.
We are in a LIVE VOICE CALL.

Core Instructions:
1. Speak naturally, clearly, warmly, and concisely like a human personal assistant. Keep responses short and conversational (1-2 sentences).
2. Fluent in English, Urdu (اردو), Roman Urdu, and Pashto (پښتو). Always reply in the EXACT language I speak.
3. If I ask to open apps or take system actions (e.g., "open TikTok", "open YouTube", "open WhatsApp", "open Settings", "go back", "close this"), invoke the corresponding tool function immediately.
4. Stop speaking immediately if I interrupt or say "Stop".
5. Address me respectfully as Boss.
"""
    }

    enum class SessionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        ERROR
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null

    private val _sessionState = MutableStateFlow(SessionState.DISCONNECTED)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var activeApiKey: String = ""
    private var isIntentionallyConnected = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3
    private var reconnectJob: Job? = null

    init {
        okHttpClient = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive persistent websocket
            .connectTimeout(15, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }

    fun connect(customApiKey: String? = null) {
        val key = if (!customApiKey.isNullOrBlank()) {
            customApiKey.trim()
        } else {
            try {
                val bk = BuildConfig.GEMINI_API_KEY
                if (bk.isNotBlank() && bk != "MY_GEMINI_API_KEY") bk.trim() else ""
            } catch (e: Throwable) {
                ""
            }
        }

        if (key.isBlank()) {
            _sessionState.value = SessionState.ERROR
            _errorMessage.value = "Gemini API key is not configured. Add it in Settings to enable Live Call."
            onStatusChanged(SessionState.ERROR, _errorMessage.value)
            return
        }

        activeApiKey = key
        isIntentionallyConnected = true
        reconnectAttempts = 0
        initiateWebSocket()
    }

    private fun initiateWebSocket() {
        if (!isIntentionallyConnected) return

        _sessionState.value = if (reconnectAttempts > 0) SessionState.RECONNECTING else SessionState.CONNECTING
        _errorMessage.value = null
        onStatusChanged(_sessionState.value, null)

        val wsUrl = "$WS_HOST?key=$activeApiKey"
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        try {
            webSocket?.cancel()
            webSocket = okHttpClient?.newWebSocket(request, createWebSocketListener())
        } catch (e: Exception) {
            Log.e(TAG, "Failed initiating live websocket", e)
            handleConnectionFailure("Failed to initiate Live session: ${e.localizedMessage}")
        }
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected successfully to Gemini Live")
            reconnectAttempts = 0
            _sessionState.value = SessionState.CONNECTED
            _errorMessage.value = null
            onStatusChanged(SessionState.CONNECTED, null)

            // Send initial setup payload with live model and phone action tool declarations
            sendSetupMessage(webSocket)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleIncomingLiveMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing code=$code reason=$reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed code=$code reason=$reason")
            if (isIntentionallyConnected) {
                handleConnectionFailure("Connection closed ($reason)")
            } else {
                _sessionState.value = SessionState.DISCONNECTED
                onStatusChanged(SessionState.DISCONNECTED, null)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket live error: ${t.message}", t)
            val errText = when {
                response?.code == 400 || response?.code == 403 -> "Live API Authentication failed (HTTP ${response.code}). Please verify your Gemini API key."
                response?.code == 404 -> "Gemini Live model endpoint unavailable."
                t.message?.contains("Unable to resolve host") == true -> "Network offline. Check your internet connection."
                else -> t.localizedMessage ?: "Live connection failed"
            }
            handleConnectionFailure(errText)
        }
    }

    private fun sendSetupMessage(ws: WebSocket) {
        try {
            val setupJson = JSONObject().apply {
                put("setup", JSONObject().apply {
                    put("model", LIVE_MODEL)
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply {
                            put("AUDIO")
                        })
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", "Kore") // Natural calm voice
                                })
                            })
                        })
                    })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", LIVE_SYSTEM_PROMPT) })
                        })
                    })
                    // Declare Android action tools for Gemini function calling
                    put("tools", JSONArray().apply {
                        put(buildPhoneToolsDeclaration())
                    })
                })
            }
            ws.send(setupJson.toString())
            Log.d(TAG, "Sent setup configuration message to Gemini Live")
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending setup message", e)
        }
    }

    private fun buildPhoneToolsDeclaration(): JSONObject {
        return JSONObject().apply {
            put("functionDeclarations", JSONArray().apply {
                // openTikTok
                put(JSONObject().apply {
                    put("name", "openTikTok")
                    put("description", "Launch the TikTok application on the Android device")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject())
                    })
                })
                // openYouTube
                put(JSONObject().apply {
                    put("name", "openYouTube")
                    put("description", "Launch the YouTube application on the Android device")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("query", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "Optional search query to open in YouTube")
                            })
                        })
                    })
                })
                // openWhatsApp
                put(JSONObject().apply {
                    put("name", "openWhatsApp")
                    put("description", "Launch the WhatsApp application on the Android device")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject())
                    })
                })
                // openSettings
                put(JSONObject().apply {
                    put("name", "openSettings")
                    put("description", "Open Android system settings")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject())
                    })
                })
                // goBack
                put(JSONObject().apply {
                    put("name", "goBack")
                    put("description", "Perform the Android system back navigation action")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject())
                    })
                })
                // closeApp
                put(JSONObject().apply {
                    put("name", "closeApp")
                    put("description", "Minimize current screen or return to Android home")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject())
                    })
                })
            })
        }
    }

    private fun handleIncomingLiveMessage(text: String) {
        try {
            val json = JSONObject(text)

            // Check for serverContent
            val serverContent = json.optJSONObject("serverContent")
            if (serverContent != null) {
                // Interruption check from server
                if (serverContent.optBoolean("interrupted", false)) {
                    Log.d(TAG, "Server reported interruption")
                    onInterrupted()
                }

                val modelTurn = serverContent.optJSONObject("modelTurn")
                val parts = modelTurn?.optJSONArray("parts")
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val textPart = part.optString("text")
                        if (textPart.isNotBlank()) {
                            onTextReceived(textPart)
                        }

                        // Audio inlineData
                        val inlineData = part.optJSONObject("inlineData")
                        if (inlineData != null) {
                            val mime = inlineData.optString("mimeType")
                            val base64Data = inlineData.optString("data")
                            if (base64Data.isNotBlank()) {
                                try {
                                    val audioBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                                    onAudioReceived(audioBytes)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error decoding audio part", e)
                                }
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    onTurnComplete()
                }
            }

            // Check for toolCall (function calling from Gemini Live)
            val toolCall = json.optJSONObject("toolCall")
            if (toolCall != null) {
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null) {
                    for (i in 0 until functionCalls.length()) {
                        val call = functionCalls.getJSONObject(i)
                        val callName = call.optString("name")
                        val callId = call.optString("id")
                        val args = call.optJSONObject("args") ?: JSONObject()
                        Log.d(TAG, "Received tool call: $callName id=$callId args=$args")
                        onToolCallReceived(callName, args, callId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing live message: ${e.message}", e)
        }
    }

    /**
     * Send tool response back to Gemini Live
     */
    fun sendToolResponse(callId: String, functionName: String, result: JSONObject) {
        val ws = webSocket ?: return
        try {
            val responseJson = JSONObject().apply {
                put("toolResponse", JSONObject().apply {
                    put("functionResponses", JSONArray().apply {
                        put(JSONObject().apply {
                            put("id", callId)
                            put("name", functionName)
                            put("response", JSONObject().apply {
                                put("result", result)
                            })
                        })
                    })
                })
            }
            ws.send(responseJson.toString())
            Log.d(TAG, "Sent tool response for $functionName id=$callId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending tool response", e)
        }
    }

    /**
     * Send user spoken text prompt into the live session
     */
    fun sendUserText(text: String) {
        val ws = webSocket ?: return
        try {
            val messageJson = JSONObject().apply {
                put("clientContent", JSONObject().apply {
                    put("turns", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", text) })
                            })
                        })
                    })
                    put("turnComplete", true)
                })
            }
            ws.send(messageJson.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending user text to Live session", e)
        }
    }

    /**
     * Send raw PCM audio chunks into the live session
     */
    fun sendAudioChunk(pcmData: ByteArray) {
        val ws = webSocket ?: return
        try {
            val base64Audio = android.util.Base64.encodeToString(pcmData, android.util.Base64.NO_WRAP)
            val chunkJson = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().apply {
                        put(JSONObject().apply {
                            put("mimeType", "audio/pcm;rate=16000")
                            put("data", base64Audio)
                        })
                    })
                })
            }
            ws.send(chunkJson.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending audio chunk", e)
        }
    }

    private fun handleConnectionFailure(errorMessage: String) {
        Log.w(TAG, "Live connection failure: $errorMessage. Attempt $reconnectAttempts/$maxReconnectAttempts")

        if (isIntentionallyConnected && reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            _sessionState.value = SessionState.RECONNECTING
            _errorMessage.value = "Connection lost. Reconnecting ($reconnectAttempts/$maxReconnectAttempts)..."
            onStatusChanged(SessionState.RECONNECTING, _errorMessage.value)

            reconnectJob?.cancel()
            reconnectJob = scope.launch {
                delay(2000L * reconnectAttempts)
                if (isActive && isIntentionallyConnected) {
                    initiateWebSocket()
                }
            }
        } else {
            _sessionState.value = SessionState.ERROR
            _errorMessage.value = errorMessage
            onStatusChanged(SessionState.ERROR, errorMessage)
        }
    }

    fun disconnect() {
        isIntentionallyConnected = false
        reconnectJob?.cancel()
        reconnectJob = null
        try {
            webSocket?.close(1000, "User ended live call")
        } catch (e: Exception) {
            // Ignored
        }
        webSocket = null
        _sessionState.value = SessionState.DISCONNECTED
        _errorMessage.value = null
        onStatusChanged(SessionState.DISCONNECTED, null)
    }
}
