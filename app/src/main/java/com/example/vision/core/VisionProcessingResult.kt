package com.example.vision.core

/**
 * Processing stages of the NutriGlass computer vision pipeline.
 */
enum class VisionProcessingStage {
    IDLE,
    FRAME_ACQUISITION,
    PREPROCESSING,
    DETECTION,
    SEGMENTATION,
    CLASSIFICATION,
    PORTION_ESTIMATION,
    NUTRITION_MAPPING,
    COMPLETED
}

/**
 * Concrete categorization of errors encountered during camera or vision inference.
 */
enum class VisionErrorType {
    PERMISSION_DENIED,
    CAMERA_UNAVAILABLE,
    OUT_OF_MEMORY,
    IMAGE_DECODE_FAILED,
    MODEL_NOT_FOUND,
    INFERENCE_FAILED,
    NETWORK_ERROR,
    DUPLICATE_REQUEST,
    UNCERTAIN_VISUAL_QUALITY,
    UNSUPPORTED_FOOD_CLASS
}

/**
 * Type-safe outcome container for all computer vision operations.
 */
sealed class VisionProcessingResult<out T> {
    data class Success<out T>(
        val data: T,
        val processingDurationMs: Long,
        val stageBreakdown: Map<String, Long> = emptyMap(),
        val modelUsed: String = "on-device-hybrid"
    ) : VisionProcessingResult<T>()

    data class Error(
        val message: String,
        val errorType: VisionErrorType,
        val throwable: Throwable? = null,
        val isRecoverable: Boolean = true,
        val failedStage: VisionProcessingStage = VisionProcessingStage.IDLE
    ) : VisionProcessingResult<Nothing>()

    object Cancelled : VisionProcessingResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isCancelled: Boolean get() = this is Cancelled
}
