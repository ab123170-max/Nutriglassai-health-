package com.example.vision.offline

import android.content.Context
import java.io.File

data class OfflineModelMetadata(
    val modelId: String,
    val fileName: String,
    val expectedSizeBytes: Long,
    val format: String = "TFLite / LiteRT",
    val license: String = "Apache 2.0",
    val requiredMemoryMb: Int = 45,
    val isBundledInApk: Boolean = false
)

enum class ModelLoadStatus {
    READY,
    FILE_MISSING,
    CORRUPTED,
    UNSUPPORTED_DEVICE,
    NOT_INITIALIZED
}

interface OfflineModelValidator {
    fun checkModelStatus(metadata: OfflineModelMetadata): ModelLoadStatus
    fun getAvailableStorageBytes(): Long
}

class StandardOfflineModelValidator(
    private val context: Context
) : OfflineModelValidator {

    override fun checkModelStatus(metadata: OfflineModelMetadata): ModelLoadStatus {
        // Models are stored in app internal files dir under models/
        val modelsDir = File(context.filesDir, "models")
        val modelFile = File(modelsDir, metadata.fileName)

        if (!modelFile.exists()) {
            return ModelLoadStatus.FILE_MISSING
        }

        if (modelFile.length() < 1024) {
            return ModelLoadStatus.CORRUPTED
        }

        return ModelLoadStatus.READY
    }

    override fun getAvailableStorageBytes(): Long {
        return context.filesDir.freeSpace
    }
}

/**
 * Controller for offline neural inference.
 * Reuses model sessions across frames to avoid high memory allocation overhead.
 */
class OfflineInferenceManager(
    private val validator: OfflineModelValidator
) {
    private val loadedModelSessions = mutableMapOf<String, Boolean>()

    val registeredModels = listOf(
        OfflineModelMetadata(
            modelId = "yolov8n_food_detector",
            fileName = "yolov8n_food.tflite",
            expectedSizeBytes = 6_500_000L,
            requiredMemoryMb = 35
        ),
        OfflineModelMetadata(
            modelId = "mobilenetv3_nepali_food",
            fileName = "mobilenet_v3_nepali_food.tflite",
            expectedSizeBytes = 4_200_000L,
            requiredMemoryMb = 25
        )
    )

    fun initializeModel(modelId: String): Result<Boolean> {
        val meta = registeredModels.firstOrNull { it.modelId == modelId }
            ?: return Result.failure(IllegalArgumentException("Unknown model: $modelId"))

        val status = validator.checkModelStatus(meta)
        return if (status == ModelLoadStatus.READY) {
            loadedModelSessions[modelId] = true
            Result.success(true)
        } else {
            Result.failure(IllegalStateException("Model '$modelId' is not ready. Status: $status. Please ensure model file is downloaded to /models/${meta.fileName}"))
        }
    }

    fun isModelReady(modelId: String): Boolean {
        return loadedModelSessions[modelId] == true
    }

    fun releaseAll() {
        loadedModelSessions.clear()
    }
}
