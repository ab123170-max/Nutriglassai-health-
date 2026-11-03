package com.example.vision.classification

import android.graphics.Bitmap

data class TopKPrediction(
    val classId: String,
    val displayName: String,
    val nepaliName: String,
    val confidence: Float,
    val category: String,
    val defaultServingGrams: Float = 150f,
    val defaultServingUnit: String = "serving",
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float = 1.5f,
    val sodiumPer100g: Float = 40f,
    val likelyAllergens: List<String> = emptyList(),
    val isPieceCountFood: Boolean = false,
    val typicalGramsPerPiece: Float = 0f
)

data class ClassificationMetadata(
    val modelName: String = "No on-device food model installed",
    val modelVersion: String = "unavailable",
    val classesSupportedCount: Int = 0,
    val topKRequested: Int = 5,
    val license: String = "",
    val inputResolution: String = "not applicable"
)

data class ClassificationOutput(
    val predictions: List<TopKPrediction>,
    val topPrediction: TopKPrediction?,
    val isConfident: Boolean,
    val metadata: ClassificationMetadata,
    val unsupportedClassDetected: Boolean = true
)

interface FoodClassificationEngine {
    suspend fun classifyFood(
        regionBitmap: Bitmap,
        topK: Int = 5,
        hintCategory: String? = null
    ): ClassificationOutput

    fun getSupportedTaxonomy(): List<FoodTaxonomyEntry>
}

data class FoodTaxonomyEntry(
    val classId: String,
    val englishName: String,
    val nepaliName: String,
    val category: String,
    val isNepaliStaple: Boolean = true
)

/**
 * No trained TFLite classifier is currently bundled with this APK.
 * Do not return hard-coded high-confidence momo/dal-bhat/etc. guesses.
 * Food recognition is handled by GeminiFoodService through the configured Vercel proxy.
 */
class HybridFoodClassifier : FoodClassificationEngine {
    override suspend fun classifyFood(
        regionBitmap: Bitmap,
        topK: Int,
        hintCategory: String?
    ): ClassificationOutput = ClassificationOutput(
        predictions = emptyList(),
        topPrediction = null,
        isConfident = false,
        metadata = ClassificationMetadata(topKRequested = topK.coerceAtLeast(1)),
        unsupportedClassDetected = true
    )

    override fun getSupportedTaxonomy(): List<FoodTaxonomyEntry> = emptyList()
}
