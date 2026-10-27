package com.example.nutrition.learning

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Entity(tableName = "food_corrections")
data class FoodCorrection(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val originalPredictedClassId: String,
    val originalPredictedName: String,
    val correctedFoodName: String,
    val correctedNepaliName: String = "",
    val originalGrams: Float,
    val correctedGrams: Float,
    val correctedUnit: String = "serving",
    val modelVersion: String = "1.2.0-mobile",
    val consentForAnonymousTraining: Boolean = false,
    val imageReferenceHash: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

interface CorrectionRepository {
    suspend fun saveCorrection(correction: FoodCorrection)
    suspend fun getAllCorrections(): List<FoodCorrection>
    suspend fun findCorrectionSuggestion(predictedName: String): FoodCorrection?
    suspend fun deleteCorrection(id: String)
    suspend fun clearAllCorrections()
}

class InMemoryCorrectionRepository : CorrectionRepository {
    private val corrections = mutableListOf<FoodCorrection>()

    override suspend fun saveCorrection(correction: FoodCorrection) {
        corrections.removeAll { it.originalPredictedName.equals(correction.originalPredictedName, ignoreCase = true) }
        corrections.add(correction)
    }

    override suspend fun getAllCorrections(): List<FoodCorrection> = corrections.toList()

    override suspend fun findCorrectionSuggestion(predictedName: String): FoodCorrection? {
        return corrections.firstOrNull {
            it.originalPredictedName.equals(predictedName, ignoreCase = true)
        }
    }

    override suspend fun deleteCorrection(id: String) {
        corrections.removeAll { it.id == id }
    }

    override suspend fun clearAllCorrections() {
        corrections.clear()
    }
}

/**
 * Service to export consented training data in standardized COCO / JSON format
 * for fine-tuning models on a secure backend without leaking private unconsented data.
 */
class FeedbackExportService(
    private val repository: CorrectionRepository
) {
    suspend fun exportConsentedDatasetJson(): String {
        val all = repository.getAllCorrections()
        val consented = all.filter { it.consentForAnonymousTraining }

        val root = JSONObject()
        root.put("format", "NutriGlass-FoodVision-Dataset-V1")
        root.put("exportTimestamp", System.currentTimeMillis())
        root.put("totalConsentedSamples", consented.size)

        val array = JSONArray()
        for (item in consented) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("originalPrediction", item.originalPredictedName)
            obj.put("groundTruthLabel", item.correctedFoodName)
            obj.put("groundTruthNepaliLabel", item.correctedNepaliName)
            obj.put("correctedGrams", item.correctedGrams)
            obj.put("modelVersion", item.modelVersion)
            obj.put("imageHash", item.imageReferenceHash ?: "")
            array.put(obj)
        }
        root.put("samples", array)
        return root.toString(2)
    }
}

data class ModelVersionInfo(
    val version: String,
    val releaseDate: String,
    val top1Accuracy: Float,
    val top5Accuracy: Float,
    val nepaliClassesAccuracy: Float,
    val parameterCount: String = "3.2M",
    val isCurrentlyActive: Boolean = false
)

class ModelVersionRegistry {
    private val versions = listOf(
        ModelVersionInfo("1.0.0-mobile", "2025-01-15", 0.72f, 0.88f, 0.65f),
        ModelVersionInfo("1.1.0-mobile", "2025-05-20", 0.79f, 0.92f, 0.76f),
        ModelVersionInfo("1.2.0-mobile", "2026-02-10", 0.86f, 0.95f, 0.88f, "3.2M", true)
    )

    fun getActiveModel(): ModelVersionInfo = versions.first { it.isCurrentlyActive }
    fun getAllVersions(): List<ModelVersionInfo> = versions
}

class LearningSuggestionEngine(
    private val repository: CorrectionRepository
) {
    suspend fun getSuggestionForPrediction(predictedName: String): String? {
        val previousCorrection = repository.findCorrectionSuggestion(predictedName)
        return previousCorrection?.correctedFoodName
    }
}
