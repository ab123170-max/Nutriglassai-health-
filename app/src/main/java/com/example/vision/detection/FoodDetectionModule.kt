package com.example.vision.detection

import android.graphics.RectF
import com.example.vision.core.FoodFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float,
    val classId: String,
    val className: String,
    val isFoodItem: Boolean = true
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val area: Float get() = width * height

    fun toImageCoordinates(imageWidth: Int, imageHeight: Int): RectF = RectF(
        (left * imageWidth).coerceIn(0f, imageWidth.toFloat()),
        (top * imageHeight).coerceIn(0f, imageHeight.toFloat()),
        (right * imageWidth).coerceIn(0f, imageWidth.toFloat()),
        (bottom * imageHeight).coerceIn(0f, imageHeight.toFloat())
    )
}

data class DetectedFoodObject(
    val id: String = java.util.UUID.randomUUID().toString(),
    val box: BoundingBox,
    val isPlateOrContainer: Boolean = false,
    val relativePlateAreaFraction: Float = 0.0f
)

data class DetectionConfig(
    val minConfidenceThreshold: Float = 0.25f,
    val nmsIouThreshold: Float = 0.45f,
    val maxDetections: Int = 10,
    val rejectNonFoodClasses: Boolean = true
)

interface FoodDetectionEngine {
    suspend fun detectFoodObjects(
        frame: FoodFrame,
        config: DetectionConfig = DetectionConfig()
    ): List<DetectedFoodObject>
}

/**
 * Safe placeholder used until a real, trained object-detection model is packaged.
 *
 * The previous implementation invented a plate at the centre of every photo and
 * divided it into three arbitrary food regions. That produced false detections.
 * Returning no detections is intentional: the app's cloud Gemini vision scanner
 * remains the supported food-identification path.
 */
class ChromaticSaliencyFoodDetector : FoodDetectionEngine {
    override suspend fun detectFoodObjects(
        frame: FoodFrame,
        config: DetectionConfig
    ): List<DetectedFoodObject> = withContext(Dispatchers.Default) {
        // Never fabricate bounding boxes or food labels from image position/colour.
        emptyList()
    }

    fun applyNonMaxSuppression(
        boxes: List<BoundingBox>,
        iouThreshold: Float
    ): List<BoundingBox> {
        val sorted = boxes.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<BoundingBox>()
        while (sorted.isNotEmpty()) {
            val current = sorted.removeAt(0)
            selected.add(current)
            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                if (computeIoU(current, iterator.next()) >= iouThreshold) iterator.remove()
            }
        }
        return selected
    }

    fun computeIoU(a: BoundingBox, b: BoundingBox): Float {
        val left = max(a.left, b.left)
        val top = max(a.top, b.top)
        val right = min(a.right, b.right)
        val bottom = min(a.bottom, b.bottom)
        val intersection = (right - left).coerceAtLeast(0f) * (bottom - top).coerceAtLeast(0f)
        val union = a.area + b.area - intersection
        return if (union <= 0f) 0f else intersection / union
    }
}
