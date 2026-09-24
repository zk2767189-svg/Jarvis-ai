package com.example.data.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ThirtySecondVideoPlan
import com.example.util.ThirtySecondVideoEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class GeminiApiClient(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        const val DEFAULT_MODEL = "gemini-3.5-flash"
        const val IMAGE_MODEL = "gemini-2.5-flash-image"

        const val JARVIS_SYSTEM_INSTRUCTION = """
You are JARVIS, my personal AI voice assistant.

Core Principles:
1. Start every session ready for voice commands.
2. Understand Urdu, Roman Urdu, Pashto, and English fluently. Reply in the exact same language I use.
   - If I speak Urdu, respond in natural Urdu.
   - If I speak Roman Urdu, respond in natural Roman Urdu.
   - If I speak Pashto, respond in natural Pashto.
   - If I speak English, respond in English.
3. When I give a command:
   - Step 1: Understand it.
   - Step 2: Give a short, direct answer.
   - Step 3: If an available tool can perform the task, use it.
   - Step 4: Tell me clearly what was completed.
4. Voice Mode:
   - Speak naturally, clearly, and conversationally.
   - Keep responses short and crisp during live conversation (1-3 sentences max).
   - Stop speaking immediately when I say "Stop".
   - Continue listening after answering.
   - Do not pretend to perform actions that you cannot actually perform (e.g. placing outside carrier phone calls or cellular SMS).
5. Be helpful, fast, respectful, and privacy-conscious. Address me naturally as Boss.
"""

        const val JARVIS_LIVE_CALL_INSTRUCTION = """
You are JARVIS, my personal AI voice assistant. We are in a LIVE VOICE CALL.

Live Voice Directives:
1. Speak naturally, clearly, and conversationally like a human personal assistant.
2. Keep responses short, fast, and concise (strictly 1 to 2 sentences) during live conversation.
3. Understand Urdu, Roman Urdu, Pashto, and English. Reply in the EXACT same language I use.
4. When I give a command:
   - Understand it.
   - Give a short answer.
   - If an available tool can perform the task (Tasks, Quotes, Video Prompts, YouTube scripts), execute it.
   - Tell me clearly what was completed.
5. Do not repeat my complete question unless necessary.
6. If I say "Stop", "Wait", or "خاموش شه", stop speaking immediately.
7. Continue listening after answering.
8. Do not pretend to perform actions that you cannot actually perform.
9. Address me respectfully as Boss. Protect my privacy.
"""
    }

    /**
     * Resolves the API key from custom override or BuildConfig
     */
    fun getApiKey(customKey: String? = null): String {
        if (!customKey.isNullOrBlank()) return customKey.trim()
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey.trim() else ""
    }

    suspend fun generateResponse(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(), // Pair(role, text)
        imageUri: Uri? = null,
        customKey: String? = null,
        modelName: String = DEFAULT_MODEL
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(customKey)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("GEMINI_API_KEY_NOT_CONFIGURED")
            )
        }

        try {
            val selectedModel = if (imageUri != null) IMAGE_MODEL else modelName
            val url = "$BASE_URL$selectedModel:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", JARVIS_SYSTEM_INSTRUCTION) })
                    })
                })

                // Generation Config
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                    put("topK", 40)
                })

                // Contents array
                val contentsArray = JSONArray()

                // Recent history (last 6 turns for context efficiency)
                val recentHistory = history.takeLast(6)
                for ((role, text) in recentHistory) {
                    val apiRole = if (role.lowercase() == "user") "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", apiRole)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", text) })
                        })
                    })
                }

                // Current user message
                val userParts = JSONArray()
                userParts.put(JSONObject().apply { put("text", prompt) })

                // Multimodal inline data if image provided
                if (imageUri != null) {
                    val base64Image = readImageAsBase64(imageUri)
                    if (base64Image != null) {
                        userParts.put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    }
                }

                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", userParts)
                })

                put("contents", contentsArray)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseText = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API failed code: ${response.code}, body: $responseText")
                val errorMsg = try {
                    val errorObj = JSONObject(responseText).optJSONObject("error")
                    errorObj?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseText"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseText)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val partObj = parts.getJSONObject(i)
                        val text = partObj.optString("text")
                        if (text.isNotEmpty()) sb.append(text)
                    }
                    val resultText = sb.toString()
                    if (resultText.isNotBlank()) {
                        return@withContext Result.success(resultText)
                    }
                }
            }

            Result.failure(Exception("Received empty response from JARVIS core."))
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking Gemini API", e)
            Result.failure(e)
        }
    }

    suspend fun generateLiveCallResponse(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(),
        customKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(customKey)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("GEMINI_API_KEY_NOT_CONFIGURED")
            )
        }

        try {
            val url = "$BASE_URL$DEFAULT_MODEL:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", JARVIS_LIVE_CALL_INSTRUCTION) })
                    })
                })

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.6)
                    put("maxOutputTokens", 180)
                })

                val contentsArray = JSONArray()
                // Take last 4 turns for live conversation speed
                val recentHistory = history.takeLast(4)
                for ((role, text) in recentHistory) {
                    val apiRole = if (role.lowercase() == "user") "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", apiRole)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", text) })
                        })
                    })
                }

                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })

                put("contents", contentsArray)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseText = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(TAG, "Live call Gemini API failed code: ${response.code}")
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val jsonResponse = JSONObject(responseText)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val partObj = parts.getJSONObject(i)
                        val text = partObj.optString("text")
                        if (text.isNotEmpty()) sb.append(text)
                    }
                    val resultText = sb.toString().trim()
                    if (resultText.isNotBlank()) {
                        return@withContext Result.success(resultText)
                    }
                }
            }

            Result.failure(Exception("Empty live response"))
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateLiveCallResponse", e)
            Result.failure(e)
        }
    }

    suspend fun generateThirtySecondVideoPlan(
        storyIdea: String,
        aspectRatio: String,
        visualStyle: String,
        customKey: String? = null
    ): Result<ThirtySecondVideoPlan> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(customKey)
        if (apiKey.isBlank()) {
            return@withContext Result.success(
                ThirtySecondVideoEngine.generateLocalThirtySecondPlan(storyIdea, aspectRatio, visualStyle)
            )
        }

        try {
            val url = "$BASE_URL$DEFAULT_MODEL:generateContent?key=$apiKey"
            val promptInstruction = """
You are JARVIS AI Video Director. Generate a complete 30-second AI video plan.
Story/Idea: "$storyIdea"
Aspect Ratio: $aspectRatio (optimize for Shorts/TikTok if 9:16, or YouTube if 16:9)
Visual Style: $visualStyle

Requirements:
1. Divide precisely into 6 sequential 5-second scenes covering 00:00 to 00:30.
2. Character continuity: Define a strict "characterContinuitySheet" with identical outfit, distinctive hair, facial build, and colors that persist across all scenes.
3. Return ONLY valid raw JSON with this exact schema:
{
  "title": "Short cinematic video title",
  "characterContinuitySheet": "Detailed permanent traits of characters for visual consistency",
  "scenes": [
    {
      "sceneNumber": 1,
      "timeRange": "00:00 - 00:05",
      "title": "Scene 1 title",
      "characterDetails": "Character appearance and emotional state in this scene",
      "actionDescription": "Detailed visual action taking place",
      "cameraMovement": "Specific camera movement (e.g. tracking, push-in, crane, orbit)",
      "lighting": "Cinematic lighting setup and color temperature",
      "dialogue": "Short spoken line or voiceover",
      "soundFx": "Atmospheric audio and sound effects",
      "veoKlingPrompt": "Full synthesis prompt for Google Veo or Kling AI"
    }
  ]
}
"""

            val jsonBody = JSONObject().apply {
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 2048)
                })
                val contentsArray = JSONArray()
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", promptInstruction) })
                    })
                })
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful && responseBody.isNotBlank()) {
                val root = JSONObject(responseBody)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        val plan = ThirtySecondVideoEngine.parseGeminiPlanResponse(text, storyIdea, aspectRatio, visualStyle)
                        return@withContext Result.success(plan)
                    }
                }
            }
            Result.success(ThirtySecondVideoEngine.generateLocalThirtySecondPlan(storyIdea, aspectRatio, visualStyle))
        } catch (e: Exception) {
            Log.e(TAG, "Error generating 30s video with Gemini API, falling back to local engine", e)
            Result.success(ThirtySecondVideoEngine.generateLocalThirtySecondPlan(storyIdea, aspectRatio, visualStyle))
        }
    }

    private fun readImageAsBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // Scale down bitmap to max 1024x1024 for fast and optimal transmission
                val maxDim = 1024
                val ratio = Math.min(
                    maxDim.toFloat() / bitmap.width,
                    maxDim.toFloat() / bitmap.height
                )
                val finalBitmap = if (ratio < 1.0f) {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * ratio).toInt(),
                        (bitmap.height * ratio).toInt(),
                        true
                    )
                } else bitmap

                val outputStream = ByteArrayOutputStream()
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading image uri: $uri", e)
            null
        }
    }

    suspend fun translateText(
        text: String,
        fromLang: String,
        toLang: String,
        customKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(customKey)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY_NOT_CONFIGURED"))
        }

        try {
            val url = "$BASE_URL$DEFAULT_MODEL:generateContent?key=$apiKey"
            val prompt = "You are JARVIS Neural Translation Subroutine. Translate the following text from language '$fromLang' into language '$toLang'. Output ONLY the clean, translated text with no extra conversational commentary, quotes, or markdown annotations:\n\n$text"

            val jsonBody = JSONObject().apply {
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 1024)
                })
                val contentsArray = JSONArray()
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful && responseBody.isNotBlank()) {
                val root = JSONObject(responseBody)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val resultText = parts.getJSONObject(0).optString("text").trim()
                        if (resultText.isNotBlank()) {
                            return@withContext Result.success(resultText)
                        }
                    }
                }
            }
            Result.failure(Exception("Empty translation response"))
        } catch (e: Exception) {
            Log.e(TAG, "Error translating text with Gemini API", e)
            Result.failure(e)
        }
    }
}
