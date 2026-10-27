package com.example.vision.core

import android.graphics.Bitmap
import java.security.MessageDigest

/**
 * Encapsulates an image frame captured from CameraX or loaded from storage.
 * Tracks dimensions, orientation, acquisition timestamp, and a hash for duplicate suppression.
 */
data class FoodFrame(
    val bitmap: Bitmap,
    val sourceUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val rotationDegrees: Int = 0,
    val originalWidth: Int = bitmap.width,
    val originalHeight: Int = bitmap.height
) {
    /**
     * Unique hash computed from bitmap dimensions and sample pixel values to identify
     * duplicate inference requests without hashing millions of raw bytes repeatedly.
     */
    val frameHash: String by lazy {
        computeFrameFingerprint(bitmap)
    }

    /**
     * Safely releases the underlying bitmap memory if it is no longer referenced.
     */
    fun release() {
        if (!bitmap.isRecycled) {
            try {
                bitmap.recycle()
            } catch (e: Exception) {
                // Ignore if already recycled or held
            }
        }
    }

    companion object {
        fun computeFrameFingerprint(bmp: Bitmap): String {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                val header = "${bmp.width}x${bmp.height}_${bmp.config}".toByteArray()
                digest.update(header)

                // Sample up to 64 pixels across the image grid for fast deduplication
                val stepX = (bmp.width / 8).coerceAtLeast(1)
                val stepY = (bmp.height / 8).coerceAtLeast(1)
                val sampleBuffer = ByteArray(64 * 4)
                var idx = 0
                for (y in 0 until bmp.height step stepY) {
                    for (x in 0 until bmp.width step stepX) {
                        if (idx + 4 <= sampleBuffer.size && x < bmp.width && y < bmp.height) {
                            val pixel = bmp.getPixel(x, y)
                            sampleBuffer[idx++] = (pixel shr 24 and 0xFF).toByte()
                            sampleBuffer[idx++] = (pixel shr 16 and 0xFF).toByte()
                            sampleBuffer[idx++] = (pixel shr 8 and 0xFF).toByte()
                            sampleBuffer[idx++] = (pixel and 0xFF).toByte()
                        }
                    }
                }
                digest.update(sampleBuffer, 0, idx)
                digest.digest().joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                "${bmp.width}_${bmp.height}_${System.currentTimeMillis()}"
            }
        }
    }
}
