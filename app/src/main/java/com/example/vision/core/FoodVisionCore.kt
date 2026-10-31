package com.example.vision.core

import android.content.Context
import com.example.data.network.GeminiConfig
import com.example.data.network.GeminiFoodService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class VisionProcessingMode {
    AUTO_HYBRID,        // Runs on-device vision pipeline first; enhances via Gemini if online & permitted
    ON_DEVICE_ONLY,     // Strictly offline heuristic / TFLite pipeline (zero internet)
    CLOUD_ASSISTED      // Gemini multimodal cloud analysis
}

/**
 * Central orchestrator for NutriGlass AI's computer vision and food analysis system.
 */
class FoodVisionCore(
    private val onDevicePipeline: FoodImageProcessor = StandardFoodVisionPipeline()
) {
    companion object {
        /** Create the production pipeline using the optional bundled TFLite food detector. */
        fun withTfliteModel(context: Context): FoodVisionCore =
            FoodVisionCore(StandardFoodVisionPipeline.withTfliteModel(context.applicationContext))
    }
    private val _currentStage = MutableStateFlow(VisionProcessingStage.IDLE)
    val currentStage: StateFlow<VisionProcessingStage> = _currentStage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Deduplication cache: stores recent (frameHash -> Result) to prevent redundant re-inference
    private val deduplicationCache = mutableMapOf<String, CachedVisionResult>()

    private data class CachedVisionResult(
        val result: VisionProcessingResult<FoodVisionOutput>,
        val timestamp: Long
    )

    private var activeMode: VisionProcessingMode = VisionProcessingMode.AUTO_HYBRID

    fun setProcessingMode(mode: VisionProcessingMode) {
        activeMode = mode
    }

    fun getProcessingMode(): VisionProcessingMode = activeMode

    suspend fun analyzeFoodFrame(
        frame: FoodFrame,
        forceRefresh: Boolean = false
    ): VisionProcessingResult<FoodVisionOutput> = withContext(Dispatchers.Default) {
        val hash = frame.frameHash

        // Check deduplication cache (within 15 seconds)
        if (!forceRefresh) {
            val cached = deduplicationCache[hash]
            if (cached != null && (System.currentTimeMillis() - cached.timestamp) < 15_000L) {
                return@withContext cached.result
            }
        }

        _isProcessing.value = true
        _currentStage.value = VisionProcessingStage.FRAME_ACQUISITION

        try {
            val result = onDevicePipeline.processFrame(frame) { stage ->
                _currentStage.value = stage
            }

            if (result is VisionProcessingResult.Success) {
                deduplicationCache[hash] = CachedVisionResult(result, System.currentTimeMillis())
            }

            result
        } catch (e: CancellationException) {
            VisionProcessingResult.Cancelled
        } catch (e: Exception) {
            VisionProcessingResult.Error(
                message = e.message ?: "Failed during food vision analysis.",
                errorType = VisionErrorType.INFERENCE_FAILED,
                throwable = e,
                failedStage = _currentStage.value
            )
        } finally {
            _isProcessing.value = false
            _currentStage.value = VisionProcessingStage.IDLE
        }
    }

    fun cancelActiveAnalysis() {
        onDevicePipeline.cancelProcessing()
        _isProcessing.value = false
        _currentStage.value = VisionProcessingStage.IDLE
    }

    fun clearCache() {
        deduplicationCache.clear()
    }

    fun release() {
        onDevicePipeline.release()
        clearCache()
    }
}
