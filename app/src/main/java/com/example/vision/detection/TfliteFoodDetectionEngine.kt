package com.example.vision.detection

import android.content.Context
import com.example.vision.core.FoodFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.task.vision.detector.ObjectDetector.ObjectDetectorOptions
import org.tensorflow.lite.support.image.TensorImage
import java.io.IOException

/**
 * Real on-device object detector adapter.
 *
 * Required asset: app/src/main/assets/food_detector.tflite
 * The model must be an object-detection TFLite model with Task Library metadata
 * and a label map. A generic COCO model is NOT suitable for identifying Nepali
 * dishes; train/export a food-specific model before expecting useful food boxes.
 */
class TfliteFoodDetectionEngine(
    context: Context,
    private val modelAssetName: String = "food_detector.tflite"
) : FoodDetectionEngine {

    private val appContext = context.applicationContext
    @Volatile private var detector: ObjectDetector? = null
    @Volatile private var initializationError: String? = null

    private fun getDetector(config: DetectionConfig): ObjectDetector? {
        detector?.let { return it }
        synchronized(this) {
            detector?.let { return it }
            return try {
                val options = ObjectDetectorOptions.builder()
                    .setScoreThreshold(config.minConfidenceThreshold)
                    .setMaxResults(config.maxDetections.coerceIn(1, 50))
                    .build()
                ObjectDetector.createFromFileAndOptions(appContext, modelAssetName, options)
                    .also { detector = it }
            } catch (e: Exception) {
                initializationError = when (e) {
                    is IOException -> "Food detection model '$modelAssetName' is missing from app/src/main/assets."
                    else -> "Food detection model could not be loaded: ${e.message}"
                }
                null
            }
        }
    }

    fun getModelStatus(): String =
        initializationError ?: if (detector != null) "Food detection model loaded" else
            "Food detection model has not been loaded yet"

    override suspend fun detectFoodObjects(
        frame: FoodFrame,
        config: DetectionConfig
    ): List<DetectedFoodObject> = withContext(Dispatchers.Default) {
        val model = getDetector(config) ?: return@withContext emptyList()
        val bitmap = frame.bitmap
        if (bitmap.isRecycled || bitmap.width < 2 || bitmap.height < 2) {
            return@withContext emptyList()
        }

        val tensorImage = TensorImage.fromBitmap(bitmap)
        val results = try {
            model.detect(tensorImage)
        } catch (e: Exception) {
            initializationError = "Food detection inference failed: ${e.message}"
            return@withContext emptyList()
        }

        val detections = results.mapNotNull { detection ->
            val category = detection.categories
                .filter { it.score >= config.minConfidenceThreshold }
                .maxByOrNull { it.score } ?: return@mapNotNull null
            val label = category.label.trim()
            if (label.isBlank()) return@mapNotNull null
            if (config.rejectNonFoodClasses && NON_FOOD_LABELS.any {
                    label.equals(it, ignoreCase = true)
                }) return@mapNotNull null

            val rect = detection.boundingBox
            val left = (rect.left / bitmap.width).coerceIn(0f, 1f)
            val top = (rect.top / bitmap.height).coerceIn(0f, 1f)
            val right = (rect.right / bitmap.width).coerceIn(0f, 1f)
            val bottom = (rect.bottom / bitmap.height).coerceIn(0f, 1f)
            if (right <= left || bottom <= top) return@mapNotNull null

            DetectedFoodObject(
                box = BoundingBox(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    confidence = category.score.coerceIn(0f, 1f),
                    classId = category.index.toString(),
                    className = label,
                    isFoodItem = true
                )
            )
        }

        // Greedy NMS prevents multiple overlapping boxes for the same food.
        val sorted = detections.sortedByDescending { it.box.confidence }
        val selected = mutableListOf<DetectedFoodObject>()
        for (candidate in sorted) {
            if (selected.none { intersectionOverUnion(it.box, candidate.box) > config.nmsIouThreshold }) {
                selected += candidate
            }
            if (selected.size >= config.maxDetections) break
        }
        selected
    }

    override fun release() {
        synchronized(this) {
            detector?.close()
            detector = null
        }
    }

    private fun intersectionOverUnion(a: BoundingBox, b: BoundingBox): Float {
        val left = maxOf(a.left, b.left)
        val top = maxOf(a.top, b.top)
        val right = minOf(a.right, b.right)
        val bottom = minOf(a.bottom, b.bottom)
        val intersection = (right - left).coerceAtLeast(0f) * (bottom - top).coerceAtLeast(0f)
        val union = a.area + b.area - intersection
        return if (union <= 0f) 0f else intersection / union
    }

    companion object {
        private val NON_FOOD_LABELS = setOf(
            "plate", "bowl", "cup", "table", "fork", "spoon", "knife",
            "napkin", "hand", "tray", "person", "background"
        )
    }
}
