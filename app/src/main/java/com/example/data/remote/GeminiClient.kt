package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    const val DEFAULT_MODEL = "gemini-3.5-flash"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    fun isApiKeyConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    data class GeminiResponse(
        val text: String,
        val latencyMs: Long,
        val inputTokens: Int,
        val outputTokens: Int,
        val isSuccessful: Boolean,
        val errorMessage: String? = null
    )

    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null,
        jsonSchema: String? = null,
        temperature: Float = 0.2f,
        modelName: String = DEFAULT_MODEL,
        customApiKey: String? = null
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResponse(
                text = "",
                latencyMs = 0,
                inputTokens = 0,
                outputTokens = 0,
                isSuccessful = false,
                errorMessage = "API key not configured. Using local benchmark simulation engine."
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val url = "$BASE_URL/$modelName:generateContent?key=$apiKey"
            val requestJson = JSONObject()

            // Contents
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // System instruction if present
            if (!systemInstruction.isNullOrBlank()) {
                val sysContent = JSONObject()
                val sysParts = JSONArray()
                val sysPart = JSONObject()
                sysPart.put("text", systemInstruction)
                sysParts.put(sysPart)
                sysContent.put("parts", sysParts)
                requestJson.put("systemInstruction", sysContent)
            }

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", temperature)
            genConfig.put("topP", 0.95)

            if (!jsonSchema.isNullOrBlank()) {
                val responseFormat = JSONObject()
                responseFormat.put("type", "application/json")
                try {
                    val schemaObj = JSONObject(jsonSchema)
                    responseFormat.put("schema", schemaObj)
                } catch (e: Exception) {
                    // fallback if string is schema format
                }
                genConfig.put("responseMimeType", "application/json")
            }
            requestJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API failed: ${response.code} $responseBody")
                return@withContext GeminiResponse(
                    text = "",
                    latencyMs = latency,
                    inputTokens = prompt.length / 4,
                    outputTokens = 0,
                    isSuccessful = false,
                    errorMessage = "HTTP ${response.code}: $responseBody"
                )
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            val usageMetadata = respJson.optJSONObject("usageMetadata")
            val promptTokens = usageMetadata?.optInt("promptTokenCount") ?: (prompt.length / 4)
            val candidatesTokens = usageMetadata?.optInt("candidatesTokenCount") ?: (text.length / 4)

            GeminiResponse(
                text = text.trim(),
                latencyMs = latency,
                inputTokens = promptTokens,
                outputTokens = candidatesTokens,
                isSuccessful = true
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            Log.e(TAG, "Error in Gemini API call", e)
            GeminiResponse(
                text = "",
                latencyMs = latency,
                inputTokens = prompt.length / 4,
                outputTokens = 0,
                isSuccessful = false,
                errorMessage = e.message ?: "Unknown network error"
            )
        }
    }
}
