package com.example.nutrition.engine

import kotlin.math.roundToInt

data class MacroSplit(
    val proteinPercentage: Float,
    val carbsPercentage: Float,
    val fatPercentage: Float
)

data class NutritionHealthInsight(
    val balanceScore: Int, // 1 to 10 scale
    val macroSplit: MacroSplit,
    val isHighSodium: Boolean,
    val isHighFiber: Boolean,
    val recommendations: List<String>
)

interface NutritionAnalysisEngine {
    fun analyzeMealNutrition(breakdown: NutrientBreakdown): NutritionHealthInsight
}

class StandardNutritionAnalysisEngine : NutritionAnalysisEngine {

    override fun analyzeMealNutrition(breakdown: NutrientBreakdown): NutritionHealthInsight {
        val totalCalories = breakdown.energyKcal.coerceAtLeast(1).toFloat()

        val proteinCals = breakdown.proteinGrams * 4f
        val carbsCals = breakdown.carbsGrams * 4f
        val fatCals = breakdown.fatGrams * 9f
        val sumCals = (proteinCals + carbsCals + fatCals).coerceAtLeast(1f)

        val split = MacroSplit(
            proteinPercentage = ((proteinCals / sumCals) * 1000f).roundToInt() / 10f,
            carbsPercentage = ((carbsCals / sumCals) * 1000f).roundToInt() / 10f,
            fatPercentage = ((fatCals / sumCals) * 1000f).roundToInt() / 10f
        )

        val recommendations = mutableListOf<String>()
        var score = 7

        // Sodium check (> 800mg in single meal is high)
        val sodium = breakdown.sodiumMg ?: 0f
        val isHighSodium = sodium > 800f
        if (isHighSodium) {
            score -= 1
            recommendations.add("High sodium meal (${sodium.toInt()}mg). Balance with plenty of water.")
        }

        // Fiber check
        val fiber = breakdown.fiberGrams ?: 0f
        val isHighFiber = fiber >= 6f
        if (isHighFiber) {
            score += 1
            recommendations.add("Excellent fiber content (${fiber.toInt()}g), supporting healthy digestion.")
        } else if (fiber < 2f && breakdown.energyKcal > 300) {
            recommendations.add("Consider adding fresh vegetables or green salad to boost dietary fiber.")
        }

        // Protein check
        if (split.proteinPercentage >= 20f) {
            score += 1
            recommendations.add("High protein content supports muscle recovery and satiety.")
        } else if (split.proteinPercentage < 10f && breakdown.energyKcal > 250) {
            recommendations.add("Protein is relatively low; consider adding dal, eggs, or paneer.")
        }

        return NutritionHealthInsight(
            balanceScore = score.coerceIn(1, 10),
            macroSplit = split,
            isHighSodium = isHighSodium,
            isHighFiber = isHighFiber,
            recommendations = recommendations
        )
    }
}
