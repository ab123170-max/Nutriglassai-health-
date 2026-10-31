package com.example.vision.core

import android.graphics.Bitmap
import com.example.data.model.ExtractedNutritionData
import com.example.domain.ImagePreparationModule
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class OcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractNutritionData(bitmap: Bitmap, rotationDegrees: Int): ExtractedNutritionData {
        val binarizedBitmap = ImagePreparationModule.binarizeBitmap(bitmap)
        val image = InputImage.fromBitmap(binarizedBitmap, rotationDegrees)
        
        return try {
            val result = recognizer.process(image).await()
            parseNutritionText(result.text)
        } catch (e: Exception) {
            ExtractedNutritionData(null, null, null, null, null, null, null, null)
        } finally {
            if (binarizedBitmap != bitmap && !binarizedBitmap.isRecycled) {
                binarizedBitmap.recycle()
            }
        }
    }

    private fun parseNutritionText(text: String): ExtractedNutritionData {
        // Simple regex parsing for demonstration
        val calories = Regex("""(\d+)\s*kcal""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val protein = Regex("""Protein\s*(\d+\.?\d*)""").find(text)?.groupValues?.get(1)?.toFloatOrNull()
        // ... (etc.)
        return ExtractedNutritionData(null, calories, protein, null, null, null, null, null)
    }
}
