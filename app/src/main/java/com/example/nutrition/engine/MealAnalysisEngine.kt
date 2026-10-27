package com.example.nutrition.engine

import com.example.vision.classification.TopKPrediction
import com.example.vision.portion.PortionConfidence
import com.example.vision.portion.PortionEstimate
import java.util.UUID

/**
 * Encapsulates an individual food item within a meal, maintaining immutable
 * separation between the original AI prediction and user edits.
 */
data class MealComponent(
    val id: String = UUID.randomUUID().toString(),
    // Current active (user-verified or AI default) values
    val foodName: String,
    val nepaliName: String = "",
    val quantity: Float,
    val unit: String,
    val grams: Float,
    val nutrients: NutrientBreakdown,

    // Original unedited AI prediction for auditability and model feedback
    val originalPrediction: OriginalAiPrediction? = null,

    val isUserEdited: Boolean = false,
    val requiresConfirmation: Boolean = false,
    val confirmationReason: String = ""
)

data class OriginalAiPrediction(
    val predictedClassId: String,
    val predictedName: String,
    val classificationConfidence: Float,
    val detectionConfidence: Float,
    val portionGrams: Float,
    val portionConfidence: PortionConfidence,
    val modelVersion: String = "1.2.0"
)

data class ComposedMeal(
    val id: String = UUID.randomUUID().toString(),
    val mealType: String = "Lunch", // "Breakfast", "Lunch", "Dinner", "Snacks"
    val dateString: String,
    val title: String,
    val items: List<MealComponent>,
    val totalNutrients: NutrientBreakdown,
    val overallConfidenceScore: Float,
    val isDraft: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

interface MealAnalysisEngine {
    fun composeMeal(
        items: List<MealComponent>,
        mealType: String = "Lunch",
        dateString: String,
        isDraft: Boolean = false
    ): ComposedMeal

    fun mergeDuplicateItems(items: List<MealComponent>): List<MealComponent>
    fun identifyDuplicates(items: List<MealComponent>): List<String> // Returns list of food names that appear multiple times
}

class StandardMealAnalysisEngine(
    private val calorieEngine: CalorieCalculationEngine = StandardCalorieCalculationEngine()
) : MealAnalysisEngine {

    override fun composeMeal(
        items: List<MealComponent>,
        mealType: String,
        dateString: String,
        isDraft: Boolean
    ): ComposedMeal {
        val nutrientList = items.map { it.nutrients }
        val totals = calorieEngine.aggregateMeal(nutrientList)

        val title = if (items.isEmpty()) {
            "Empty Meal"
        } else {
            items.joinToString(", ") { it.foodName }.take(40)
        }

        // Calculate transparent average confidence across components
        val avgConfidence = if (items.isNotEmpty()) {
            items.map {
                it.originalPrediction?.classificationConfidence ?: 1.0f
            }.average().toFloat()
        } else 1.0f

        return ComposedMeal(
            mealType = mealType,
            dateString = dateString,
            title = title,
            items = items,
            totalNutrients = totals,
            overallConfidenceScore = avgConfidence,
            isDraft = isDraft
        )
    }

    override fun mergeDuplicateItems(items: List<MealComponent>): List<MealComponent> {
        val grouped = items.groupBy { it.foodName.lowercase().trim() }
        val mergedList = mutableListOf<MealComponent>()

        for ((_, group) in grouped) {
            if (group.size == 1) {
                mergedList.add(group.first())
            } else {
                // Merge multiple entries of same food
                val base = group.first()
                val totalGrams = group.sumOf { it.grams.toDouble() }.toFloat()
                val totalQty = group.sumOf { it.quantity.toDouble() }.toFloat()

                val mergedNutrients = calorieEngine.aggregateMeal(group.map { it.nutrients })
                mergedList.add(
                    base.copy(
                        id = UUID.randomUUID().toString(),
                        quantity = totalQty,
                        grams = totalGrams,
                        nutrients = mergedNutrients,
                        isUserEdited = true
                    )
                )
            }
        }
        return mergedList
    }

    override fun identifyDuplicates(items: List<MealComponent>): List<String> {
        val counts = items.groupingBy { it.foodName.lowercase().trim() }.eachCount()
        return counts.filter { it.value > 1 }.keys.toList()
    }
}
