package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt

data class PreparedImage(
    val jpegBytes: ByteArray,
    val base64Data: String,
    val mimeType: String = "image/jpeg",
    val width: Int,
    val height: Int,
    val byteCount: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PreparedImage
        return jpegBytes.contentEquals(other.jpegBytes) &&
                base64Data == other.base64Data &&
                mimeType == other.mimeType &&
                width == other.width &&
                height == other.height &&
                byteCount == other.byteCount
    }

    override fun hashCode(): Int {
        var result = jpegBytes.contentHashCode()
        result = 31 * result + base64Data.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + byteCount
        return result
    }
}

object ImagePreparationModule {

    const val MIME_TYPE_JPEG = "image/jpeg"
    const val DEFAULT_MAX_DIMENSION = 1024
    const val DEFAULT_JPEG_QUALITY = 85

    /**
     * Verifies that the byte array conforms to the JPEG standard by checking magic bytes:
     * Starts with 0xFF, 0xD8 (SOI - Start of Image)
     * Ends with 0xFF, 0xD9 (EOI - End of Image)
     */
    fun isJpeg(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val startsWithJpeg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
        val endsWithJpeg = bytes[bytes.size - 2] == 0xFF.toByte() && bytes[bytes.size - 1] == 0xD9.toByte()
        return startsWithJpeg && endsWithJpeg
    }

    /**
     * Verifies if byte array is PNG (starts with 0x89 0x50 0x4E 0x47)
     */
    fun isPng(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        return bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() &&
                bytes[3] == 0x47.toByte()
    }

    /**
     * Cleans data URL prefixes (e.g. "data:image/jpeg;base64,") and strips any whitespace or newlines.
     * Also detects and unwraps accidentally double-encoded Base64.
     */
    fun cleanBase64Data(rawInput: String?): String {
        if (rawInput.isNullOrBlank()) {
            throw IllegalArgumentException("Base64 image data is empty or null.")
        }

        var cleaned = rawInput.trim()

        // Strip data URL scheme prefix if present
        if (cleaned.contains(";base64,")) {
            cleaned = cleaned.substringAfter(";base64,").trim()
        } else if (cleaned.startsWith("data:") && cleaned.contains(",")) {
            cleaned = cleaned.substringAfter(",").trim()
        }

        // Remove all whitespace and line breaks
        cleaned = cleaned.replace("\\s".toRegex(), "")

        // Check for accidental double encoding:
        // If decoding once yields another ASCII string that itself is valid Base64 and decodes to an image header,
        // unwrap the outer layer.
        cleaned = unwrapDoubleEncodedBase64(cleaned)

        if (cleaned.isBlank()) {
            throw IllegalArgumentException("Base64 image data is empty after cleaning.")
        }

        return cleaned
    }

    private fun unwrapDoubleEncodedBase64(input: String): String {
        try {
            if (input.length % 4 != 0) return input
            val decodedBytes = Base64.decode(input, Base64.NO_WRAP)
            // If decoded bytes already are JPEG or PNG, it was single encoded
            if (isJpeg(decodedBytes) || isPng(decodedBytes)) {
                return input
            }
            // Check if decoded bytes represent an ASCII base64 string
            val asString = String(decodedBytes, Charsets.US_ASCII).trim()
            if (asString.length >= 8 && asString.matches("^[A-Za-z0-9+/=]+$".toRegex()) && asString.length % 4 == 0) {
                val secondDecoded = Base64.decode(asString, Base64.NO_WRAP)
                if (isJpeg(secondDecoded) || isPng(secondDecoded)) {
                    return asString
                }
            }
        } catch (_: Exception) {
            // Not double encoded, keep original
        }
        return input
    }

    /**
     * Prepares a Bitmap for Gemini Vision API:
     * - Validates dimensions and memory state
     * - Applies EXIF rotation if needed
     * - Resizes oversized images while strictly maintaining aspect ratio
     * - Compresses to standard JPEG
     * - Verifies JPEG magic bytes
     * - Encodes to Base64 without line breaks (NO_WRAP)
     */
    fun prepareBitmapForAi(
        bitmap: Bitmap,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        quality: Int = DEFAULT_JPEG_QUALITY,
        rotationDegrees: Int = 0
    ): PreparedImage {
        if (bitmap.isRecycled) {
            throw IllegalArgumentException("Cannot prepare a recycled bitmap.")
        }
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            throw IllegalArgumentException("Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}")
        }

        // 1. Rotate if needed
        val orientedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            rotated
        } else {
            bitmap
        }

        // 2. Resize oversized image while maintaining aspect ratio
        val currentMax = max(orientedBitmap.width, orientedBitmap.height)
        val scaledBitmap = if (currentMax > maxDimension) {
            val ratio = maxDimension.toFloat() / currentMax
            val targetW = (orientedBitmap.width * ratio).roundToInt().coerceAtLeast(1)
            val targetH = (orientedBitmap.height * ratio).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(orientedBitmap, targetW, targetH, true)
        } else {
            orientedBitmap
        }

        // 3. Compress to JPEG
        val stream = ByteArrayOutputStream()
        val compressSuccess = scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 100), stream)
        val jpegBytes = stream.toByteArray()
        stream.close()

        // Clean up temporary bitmaps if they were newly allocated
        if (orientedBitmap != bitmap && !orientedBitmap.isRecycled) {
            orientedBitmap.recycle()
        }
        if (scaledBitmap != bitmap && scaledBitmap != orientedBitmap && !scaledBitmap.isRecycled) {
            scaledBitmap.recycle()
        }

        if (!compressSuccess || jpegBytes.size < 100) {
            throw IllegalArgumentException("Failed to compress image to JPEG format or resulting byte size is too small (${jpegBytes.size} bytes).")
        }

        // 4. Verify JPEG magic bytes
        if (!isJpeg(jpegBytes)) {
            throw IllegalArgumentException("Image data does not contain valid JPEG magic bytes (SOI/EOI).")
        }

        // 5. Standard Base64 encoding without line breaks (NO_WRAP)
        val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

        return PreparedImage(
            jpegBytes = jpegBytes,
            base64Data = base64,
            mimeType = MIME_TYPE_JPEG,
            width = scaledBitmap.width,
            height = scaledBitmap.height,
            byteCount = jpegBytes.size
        )
    }

    /**
     * Checks if byte array starts with recognizable image format magic bytes (JPEG, PNG, GIF, WEBP, BMP)
     */
    fun isValidImageHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        // JPEG: FF D8
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) return true
        // PNG: 89 50 4E 47
        if (bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) return true
        // GIF: 47 49 46 38
        if (bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x38.toByte()) return true
        // WEBP: RIFF
        if (bytes.size >= 12 && bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()) return true
        // BMP: 42 4D
        if (bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte()) return true
        return false
    }

    /**
     * Converts raw image bytes (such as PNG or WebP) into valid, compressed JPEG bytes
     */
    fun convertBytesToJpeg(rawBytes: ByteArray, maxDimension: Int = DEFAULT_MAX_DIMENSION, quality: Int = DEFAULT_JPEG_QUALITY): PreparedImage {
        if (rawBytes.isEmpty()) {
            throw IllegalArgumentException("Image byte array is empty.")
        }
        if (!isValidImageHeader(rawBytes)) {
            throw IllegalArgumentException("Corrupt image data: unrecognized image header or magic bytes.")
        }

        // If already valid JPEG and within acceptable dimension/size, we can prepare or recompress
        val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: throw IllegalArgumentException("Could not decode image from provided byte array.")

        try {
            return prepareBitmapForAi(bitmap, maxDimension, quality)
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    /**
     * Extracts ImageProxy from CameraX into a correctly oriented Bitmap
     */
    fun imageProxyToRotatedBitmap(image: ImageProxy): Bitmap {
        val rotation = image.imageInfo.rotationDegrees

        // Try official CameraX toBitmap() extension first
        val bitmap: Bitmap? = try {
            image.toBitmap()
        } catch (_: Exception) {
            null
        } ?: run {
            // Fallback: decode plane 0
            try {
                val plane = image.planes[0]
                val buffer: ByteBuffer = plane.buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) {
                null
            }
        }

        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
            throw IllegalArgumentException("Failed to decode ImageProxy into Bitmap.")
        }

        return if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            rotated
        } else {
            bitmap
        }
    }

    /**
     * Reads photo from Content URI, reads EXIF orientation and downsamples large photos
     */
    fun decodeAndOrientImage(context: Context, uri: Uri, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap? {
        return try {
            // 1. Check EXIF orientation
            var rotationDegrees = 0
            context.contentResolver.openInputStream(uri)?.use { stream ->
                try {
                    val exif = ExifInterface(stream)
                    rotationDegrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                } catch (_: Exception) {}
            }

            // 2. Decode bounds for downsampling
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            var inSampleSize = 1
            val origMax = max(boundsOptions.outWidth, boundsOptions.outHeight)
            while (origMax / inSampleSize > maxDimension * 2) {
                inSampleSize *= 2
            }

            // 3. Decode actual bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 4. Apply orientation rotation if needed
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                if (rotated != decoded && !decoded.isRecycled) {
                    decoded.recycle()
                }
                rotated
            } else {
                decoded
            }
        } catch (e: Exception) {
            null
        }
    }
    /**
     * Applies simple thresholding to binarize an image for OCR.
     */
    fun binarizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val binarized = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(binarized)
        val paint = android.graphics.Paint()
        val colorMatrix = android.graphics.ColorMatrix()
        colorMatrix.setSaturation(0f) // Grayscale
        // Contrast enhancement and threshold-like adjustment
        val cm = android.graphics.ColorMatrix(floatArrayOf(
            2f, 0f, 0f, 0f, -128f,
            0f, 2f, 0f, 0f, -128f,
            0f, 0f, 2f, 0f, -128f,
            0f, 0f, 0f, 1f, 0f
        ))
        colorMatrix.postConcat(cm)
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return binarized
    }
}
