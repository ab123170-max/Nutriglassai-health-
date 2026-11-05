package com.example.data.network

import com.example.BuildConfig

/**
 * Centralized Gemini Model & Endpoint Configuration.
 */
object GeminiConfig {

    const val PRIMARY_MODEL = "gemini-3.8-flash"
    const val FALLBACK_MODEL = "gemini-3.6-flash"
    const val BACKUP_MODEL = "gemini-3.5-flash-lite"

    /**
     * Vercel Backend Proxy endpoint hosting secure server-side GEMINI_API_KEY
     */
    const val PROXY_URL = "https://nutriglass-seven.vercel.app/api/gemini"

    /**
     * Google direct endpoint
     */
    const val DIRECT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    @Volatile
    var activeModel: String = PRIMARY_MODEL

    val SUPPORTED_MODELS = listOf(
        "gemini-3.8-flash",
        "gemini-3.6-flash",
        "gemini-3.5-flash-lite"
    )

    fun getGenerateContentUrl(model: String, apiKey: String): String {
        return "$DIRECT_BASE_URL/$model:generateContent?key=$apiKey"
    }

    fun hasDirectApiKey(): Boolean {
        val key = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        return key.isNotBlank() &&
                !key.equals("YOUR_GEMINI_API_KEY", ignoreCase = true) &&
                !key.equals("MY_GEMINI_API_KEY", ignoreCase = true) &&
                !key.equals("BUILD_WITHOUT_API_KEY", ignoreCase = true)
    }

    fun isConfigured(): Boolean {
        // App is configured either if direct key is provided OR backend proxy is available
        return PROXY_URL.startsWith("https://") || hasDirectApiKey()
    }
}
