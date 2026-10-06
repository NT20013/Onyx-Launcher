package com.example.util

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object GeminiAssistantHelper {

    private const val MODEL_NAME = "gemini-3.1-flash-lite-preview"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Streams responses from Gemini 3.1 Flash Lite preview with real-time SSE token emission.
     */
    fun streamChatResponse(
        history: List<ChatMessage>,
        newPrompt: String
    ): Flow<String> = flow {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            emit("Будь ласка, вкажіть дійсний GEMINI_API_KEY у панелі Secrets в AI Studio для активації Gemini Flash Lite.")
            return@flow
        }

        val contentsArray = JSONArray()

        // Include conversation history turns
        for (msg in history.takeLast(10)) {
            val role = if (msg.role == "model") "model" else "user"
            val parts = JSONArray().apply {
                put(JSONObject().apply { put("text", msg.text) })
            }
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", parts)
            })
        }

        // Add the current prompt
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", newPrompt) })
            })
        })

        val requestBodyJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "Ти — швидкий і лаконічний AI-асистент Onyx Launcher на базі Gemini Flash Lite. Відповідай коротко, структуровано, точно і зрозуміло українською мовою (або мовою користувача).")
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.6)
                put("topP", 0.95)
            })
        }

        val url = "$BASE_URL/$MODEL_NAME:streamGenerateContent?alt=sse&key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                emit("Помилка запиту ($errorBody)")
                return@flow
            }

            val body = response.body ?: run {
                emit("Порожня відповідь сервера.")
                return@flow
            }

            body.byteStream().bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line?.trim() ?: continue
                    if (!currentLine.startsWith("data:")) continue
                    val payload = currentLine.removePrefix("data:").trim()
                    if (payload.isEmpty() || payload == "[DONE]") continue

                    try {
                        val json = JSONObject(payload)
                        val candidates = json.optJSONArray("candidates")
                        val firstCandidate = candidates?.optJSONObject(0)
                        val content = firstCandidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val textChunk = parts?.optJSONObject(0)?.optString("text")

                        if (!textChunk.isNullOrEmpty()) {
                            emit(textChunk)
                        }
                    } catch (e: Exception) {
                        // Ignore JSON parsing fragment errors in stream
                    }
                }
            }
        } catch (e: Exception) {
            emit("Не вдалося підключитися до Gemini: ${e.localizedMessage ?: "мережева помилка"}")
        }
    }.flowOn(Dispatchers.IO)
}
