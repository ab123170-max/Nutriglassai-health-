package com.example.vision.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Configuration options for image normalization before neural network or heuristic analysis.
 */
data class PreprocessingConfig(
    val targetWidth: Int = 224,
    val targetHeight: Int = 224,
    val maintainAspectRatio: Boolean = true,
    val maxDimension: Int = 1024,
    val compressionQuality: Int = 85,
    val normalizeToZeroOne: Boolean = true
)

/**
 * Result of preprocessing an input image frame.
 * Retains spatial transformation metrics to map model predictions back to the original image coordinates.
 */
data class PreprocessedFrame(
    val processedBitmap: Bitmap,
    val scaleX: Float,
    val scaleY: Float,
    val padX: Float,
    val padY: Float,
    val originalWidth: Int,
    val originalHeight: Int,
    val compressedJpegBytes: ByteArray? = null
) {
    /**
     * Maps normalized box coordinates [0, 1] relative to the preprocessed bitmap
     * back to the original uncropped frame's pixel coordinates.
     */
    fun mapBoxToOriginal(normLeft: Float, normTop: Float, normRight: Float, normBottom: Float): RectF {
        val pw = processedBitmap.width.toFloat()
        val ph = processedBitmap.height.toFloat()

        // Un-pad
        val unpaddedLeft = (normLeft * pw - padX).coerceAtLeast(0f)
        val unpaddedTop = (normTop * ph - padY).coerceAtLeast(0f)
        val unpaddedRight = (normRight * pw - padX).coerceAtLeast(0f)
        val unpaddedBottom = (normBottom * ph - padY).coerceAtLeast(0f)

        // Un-scale
        val origLeft = (unpaddedLeft / scaleX).coerceIn(0f, originalWidth.toFloat())
        val origTop = (unpaddedTop / scaleY).coerceIn(0f, originalHeight.toFloat())
        val origRight = (unpaddedRight / scaleX).coerceIn(0f, originalWidth.toFloat())
        val origBottom = (unpaddedBottom / scaleY).coerceIn(0f, originalHeight.toFloat())

        return RectF(origLeft, origTop, origRight, origBottom)
    }

    fun release() {
        if (!processedBitmap.isRecycled) {
            try {
                processedBitmap.recycle()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}

interface ImagePreprocessor {
    suspend fun preprocess(frame: FoodFrame, config: PreprocessingConfig): PreprocessedFrame
    suspend fun compressForUpload(bitmap: Bitmap, maxDimension: Int = 1024, quality: Int = 80): ByteArray
}

class StandardImagePreprocessor : ImagePreprocessor {

    override suspend fun preprocess(frame: FoodFrame, config: PreprocessingConfig): PreprocessedFrame =
        withContext(Dispatchers.Default) {
            val srcBitmap = frame.bitmap
            val srcWidth = srcBitmap.width
            val srcHeight = srcBitmap.height

            // Handle rotation if needed
            val rotatedBitmap = if (frame.rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(frame.rotationDegrees.toFloat()) }
                Bitmap.createBitmap(srcBitmap, 0, 0, srcWidth, srcHeight, matrix, true)
            } else {
                srcBitmap
            }

            val curWidth = rotatedBitmap.width
            val curHeight = rotatedBitmap.height

            if (!config.maintainAspectRatio) {
                val scaled = Bitmap.createScaledBitmap(rotatedBitmap, config.targetWidth, config.targetHeight, true)
                val scaleX = config.targetWidth.toFloat() / curWidth
                val scaleY = config.targetHeight.toFloat() / curHeight
                return@withContext PreprocessedFrame(
                    processedBitmap = scaled,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    padX = 0f,
                    padY = 0f,
                    originalWidth = curWidth,
                    originalHeight = curHeight
                )
            }

            // Letterboxing to maintain exact aspect ratio
            val scale = min(
                config.targetWidth.toFloat() / curWidth,
                config.targetHeight.toFloat() / curHeight
            )
            val scaledW = (curWidth * scale).toInt().coerceAtLeast(1)
            val scaledH = (curHeight * scale).toInt().coerceAtLeast(1)

            val padX = (config.targetWidth - scaledW) / 2f
            val padY = (config.targetHeight - scaledH) / 2f

            val output = Bitmap.createBitmap(config.targetWidth, config.targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawColor(android.graphics.Color.BLACK)

            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            val destRect = Rect(padX.toInt(), padY.toInt(), (padX + scaledW).toInt(), (padY + scaledH).toInt())
            canvas.drawBitmap(rotatedBitmap, null, destRect, paint)

            PreprocessedFrame(
                processedBitmap = output,
                scaleX = scale,
                scaleY = scale,
                padX = padX,
                padY = padY,
                originalWidth = curWidth,
                originalHeight = curHeight
            )
        }

    override suspend fun compressForUpload(bitmap: Bitmap, maxDimension: Int, quality: Int): ByteArray =
        withContext(Dispatchers.IO) {
            val width = bitmap.width
            val height = bitmap.height

            val scaledBitmap = if (max(width, height) > maxDimension) {
                val ratio = maxDimension.toFloat() / max(width, height)
                val newW = (width * ratio).toInt().coerceAtLeast(1)
                val newH = (height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, newW, newH, true)
            } else {
                bitmap
            }

            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 100), stream)
            val bytes = stream.toByteArray()
            stream.close()

            if (scaledBitmap != bitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
            bytes
        }
}
