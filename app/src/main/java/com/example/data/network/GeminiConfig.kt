package com.example.data.network

/**
 * Centralized Gemini Model Configuration.
 *
 * Requirements:
 * - Put model ID in one centralized configuration file so it can be changed easily.
 * - Inspect available Gemini models and supported API methods.
 * - Use Gemini 3.8 only if officially available to this project; otherwise use the newest
 *   officially available suitable model (gemini-2.5-flash) with fallback to gemini-1.5-flash.
 * - Never invent model IDs.
 * - Verify model availability and handle unsupported-model errors gracefully.
 */
object GeminiConfig {

    /**
     * Primary Gemini model: Latest official flash model with multimodal vision & structured JSON.
     * In public Google Generative AI API (generativelanguage.googleapis.com), the officially
     * supported flash models are gemini-2.5-flash and gemini-1.5-flash.
     */
    const val PRIMARY_MODEL = "gemini-2.5-flash"
    const val FALLBACK_MODEL = "gemini-1.5-flash"
    const val BACKUP_MODEL = "gemini-2.0-flash"

    /**
     * Centralized active model ID - easily changed or switched at runtime.
     */
    @Volatile
    var activeModel: String = PRIMARY_MODEL

    /**
     * Officially supported Gemini models in order of priority.
     */
    val SUPPORTED_MODELS = listOf(
        "gemini-2.5-flash",
        "gemini-1.5-flash",
        "gemini-2.0-flash"
    )

    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    fun getGenerateContentUrl(model: String, apiKey: String): String {
        return "$BASE_URL/$model:generateContent?key=$apiKey"
    }

    fun getModelsListUrl(apiKey: String): String {
        return "$BASE_URL?key=$apiKey"
    }
}
