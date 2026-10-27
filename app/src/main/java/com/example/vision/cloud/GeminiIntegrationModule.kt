package com.example.vision.cloud

import android.graphics.Bitmap
import com.example.data.model.MultiFoodScanResult
import com.example.data.network.GeminiConfig
import com.example.data.network.GeminiFoodService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CloudAnalysisRequest(
    val bitmap: Bitmap,
    val sourceUri: String? = null,
    val userConsentGranted: Boolean = true
)

sealed class CloudAnalysisResult {
    data class Success(val result: MultiFoodScanResult) : CloudAnalysisResult()
    data class Unavailable(val reason: String) : CloudAnalysisResult()
    data class Error(val message: String, val isRecoverable: Boolean = true) : CloudAnalysisResult()
}

interface GeminiAssistant {
    suspend fun analyzeFood(request: CloudAnalysisRequest): CloudAnalysisResult
    fun getActiveModelId(): String
}

class StandardGeminiAssistant : GeminiAssistant {

    override fun getActiveModelId(): String = GeminiConfig.activeModel

    override suspend fun analyzeFood(request: CloudAnalysisRequest): CloudAnalysisResult =
        withContext(Dispatchers.IO) {
            if (!request.userConsentGranted) {
                return@withContext CloudAnalysisResult.Unavailable("User has not granted consent for cloud AI processing.")
            }

            if (!GeminiConfig.isConfigured()) {
                return@withContext CloudAnalysisResult.Unavailable(
                    "Gemini API key is not configured. Falling back to on-device food vision."
                )
            }

            try {
                val serviceResult = GeminiFoodService.analyzeFoodImageMulti(
                    request.bitmap,
                    request.sourceUri
                )
                CloudAnalysisResult.Success(serviceResult)
            } catch (e: Exception) {
                CloudAnalysisResult.Error(
                    message = e.message ?: "Cloud AI food analysis failed.",
                    isRecoverable = true
                )
            }
        }
}
