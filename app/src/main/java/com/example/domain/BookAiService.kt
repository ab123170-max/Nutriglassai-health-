package com.example.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Book translation and study assistance through the server-side Gemini proxy.
 * Never put the Gemini secret in the Android APK.
 */
object BookAiService {
    private const val ENDPOINT = "https://nutriglass-seven.vercel.app/api/gemini"
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun translate(text: String, targetLanguage: String, sourceLanguage: String = "auto"): String =
        generate(
            "Translate the following book passage into $targetLanguage. Source language: $sourceLanguage. " +
                "Preserve meaning, names, paragraph order and dialogue. Use natural, fluent language, not a summary. " +
                "Return only the translation, without commentary.\n\nPASSAGE:\n" + text
        )

    suspend fun summarize(text: String, targetLanguage: String): String =
        generate(
            "Summarize the following book passage in $targetLanguage. Give a faithful, useful study summary " +
                "with key ideas and terms. Do not invent facts.\n\nPASSAGE:\n" + text
        )

    private suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        require(prompt.isNotBlank()) { "There is no readable text to send for AI processing." }
        if (prompt.length > 24000) {
            throw IllegalArgumentException("This passage is too long. Please translate one page or smaller section at a time.")
        }

        val parts = JSONArray().put(JSONObject().put("text", prompt))
        val contents = JSONArray().put(JSONObject().put("parts", parts))
        val payload = JSONObject().put("contents", contents).put(
            "generationConfig",
            JSONObject().put("temperature", 0.2).put("maxOutputTokens", 8192)
        ).toString()

        val request = Request.Builder()
            .url(ENDPOINT)
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw IOException("Could not connect to the AI translation service. Check internet and try again.", e)
        }

        response.use { res ->
            val body = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                val message = try { JSONObject(body).optString("error") } catch (_: Exception) { "" }
                throw IOException(message.ifBlank { "AI request failed (HTTP ${res.code})." })
            }
            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates")
                ?: throw IOException("AI returned no translation. Please retry.")
            val text = buildString {
                for (i in 0 until candidates.length()) {
                    val candidate = candidates.optJSONObject(i) ?: continue
                    val partsArray = candidate.optJSONObject("content")?.optJSONArray("parts") ?: continue
                    for (j in 0 until partsArray.length()) {
                        val value = partsArray.optJSONObject(j)?.optString("text").orEmpty()
                        if (value.isNotBlank()) append(value)
                    }
                }
            }.trim()
            if (text.isBlank()) throw IOException("AI returned an empty translation. Try again or use a smaller passage.")
            text
        }
    }
}
