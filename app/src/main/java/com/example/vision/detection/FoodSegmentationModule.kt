package com.example.vision.detection

import android.graphics.Bitmap
import android.graphics.PointF
import com.example.vision.core.FoodFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Geometric region representing a distinct food segment on a plate or bowl.
 */
data class FoodRegion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val boundingBox: BoundingBox,
    val contourPoints: List<PointF> = emptyList(),
    val relativeAreaFraction: Float = 0.25f,
    val pixelAreaApprox: Int = 0,
    val isOverlapping: Boolean = false,
    val boundaryCertainty: Float = 0.85f,
    val dominantColorHex: String = "#E0E0E0"
)

/**
 * Collection of segmented regions and spatial mask metadata.
 */
data class FoodMask(
    val width: Int,
    val height: Int,
    val regions: List<FoodRegion> = emptyList(),
    val maskBitmap: Bitmap? = null,
    val totalFoodAreaFraction: Float = 0.70f
)

interface FoodSegmentationEngine {
    suspend fun segmentRegions(
        frame: FoodFrame,
        detectedBoxes: List<BoundingBox>
    ): FoodMask
}

/**
 * Salience and Contour Segmenter.
 * Identifies distinct food regions from bounding boxes, calculates contour approximations,
 * detects overlapping boundaries, and assigns realistic boundary certainty scores.
 */
class SalienceAndContourFoodSegmenter : FoodSegmentationEngine {

    override suspend fun segmentRegions(
        frame: FoodFrame,
        detectedBoxes: List<BoundingBox>
    ): FoodMask = withContext(Dispatchers.Default) {
        val bmp = frame.bitmap
        val width = bmp.width
        val height = bmp.height

        val regions = mutableListOf<FoodRegion>()

        // Generate segmented regions for each detected food bounding box
        for (i in detectedBoxes.indices) {
            val box = detectedBoxes[i]

            // Check for potential overlap with other boxes
            var hasOverlap = false
            for (j in detectedBoxes.indices) {
                if (i != j) {
                    val other = detectedBoxes[j]
                    val overlapX = (box.right.coerceAtMost(other.right) - box.left.coerceAtLeast(other.left)).coerceAtLeast(0f)
                    val overlapY = (box.bottom.coerceAtMost(other.bottom) - box.top.coerceAtLeast(other.top)).coerceAtLeast(0f)
                    if (overlapX * overlapY > 0.05f * box.area) {
                        hasOverlap = true
                        break
                    }
                }
            }

            // Approximate polygon contour around the bounding box
            val contour = listOf(
                PointF(box.left, (box.top + box.bottom) / 2f),
                PointF((box.left + box.right) / 2f, box.top),
                PointF(box.right, (box.top + box.bottom) / 2f),
                PointF((box.left + box.right) / 2f, box.bottom)
            )

            // Boundary certainty is reduced when foods visually blend or overlap (e.g. curry poured over rice)
            val certainty = if (hasOverlap) 0.65f else 0.88f
            val approxPixels = (box.area * width * height).toInt()

            regions.add(
                FoodRegion(
                    id = "region_${box.classId}_$i",
                    label = box.className,
                    boundingBox = box,
                    contourPoints = contour,
                    relativeAreaFraction = box.area,
                    pixelAreaApprox = approxPixels,
                    isOverlapping = hasOverlap,
                    boundaryCertainty = certainty
                )
            )
        }

        FoodMask(
            width = width,
            height = height,
            regions = regions,
            totalFoodAreaFraction = regions.sumOf { it.relativeAreaFraction.toDouble() }.toFloat().coerceIn(0f, 1f)
        )
    }
}
