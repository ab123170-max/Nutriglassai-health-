package com.example.nutrition.engine

import kotlin.math.roundToInt

data class NutrientBreakdown(
    val energyKcal: Int,
    val energyKj: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val saturatedFatGrams: Float? = null,
    val fiberGrams: Float? = null,
    val sugarGrams: Float? = null,
    val sodiumMg: Float? = null,
    val preparationMethod: String = "Standard",
    val isApproximate: Boolean = true,
    val dataSource: String = "NutriGlass Verified Food Composition DB",
    val calculationMethod: String = "Atwater 4-4-9 Standard"
)

enum class CookingMethod(val oilAbsorptionGramsPer100g: Float, val calorieFactor: Float) {
    RAW(0f, 1.0f),
    BOILED(0f, 0.95f),
    STEAMED(0f, 0.98f),
    BAKED(2f, 1.05f),
    SHALLOW_FRIED(6f, 1.35f),
    DEEP_FRIED(14f, 1.70f)
}

interface CalorieCalculationEngine {
    fun calculateNutrients(
        baseCaloriesPer100g: Float,
        baseProteinPer100g: Float,
        baseCarbsPer100g: Float,
        baseFatPer100g: Float,
        baseFiberPer100g: Float? = null,
        baseSugarPer100g: Float? = null,
        baseSodiumPer100g: Float? = null,
        portionGrams: Float,
        cookingMethod: CookingMethod = CookingMethod.STEAMED
    ): NutrientBreakdown

    fun aggregateMeal(items: List<NutrientBreakdown>): NutrientBreakdown
}

class StandardCalorieCalculationEngine : CalorieCalculationEngine {

    companion object {
        const val KCAL_TO_KJ_RATIO = 4.184f
    }

    override fun calculateNutrients(
        baseCaloriesPer100g: Float,
        baseProteinPer100g: Float,
        baseCarbsPer100g: Float,
        baseFatPer100g: Float,
        baseFiberPer100g: Float?,
        baseSugarPer100g: Float?,
        baseSodiumPer100g: Float?,
        portionGrams: Float,
        cookingMethod: CookingMethod
    ): NutrientBreakdown {
        // Validation: reject negative or invalid portion sizes
        require(portionGrams >= 0f) { "Portion grams cannot be negative: $portionGrams" }
        val grams = portionGrams.coerceIn(0f, 5000f) // Sanity upper cap: 5kg
        val factor = grams / 100f

        // Cooking method adjustment: added oil increases fat and calories
        val addedFatGrams = (cookingMethod.oilAbsorptionGramsPer100g * factor)
        val finalFat = (baseFatPer100g * factor) + addedFatGrams
        val finalProtein = (baseProteinPer100g * factor)
        val finalCarbs = (baseCarbsPer100g * factor)

        // Atwater standard calculation (4 kcal/g for protein & carbs, 9 kcal/g for fat)
        val computedKcal = ((finalProtein * 4f) + (finalCarbs * 4f) + (finalFat * 9f)).roundToInt()
        val finalKcal = if (computedKcal > 0) computedKcal else (baseCaloriesPer100g * factor * cookingMethod.calorieFactor).roundToInt()
        val finalKj = (finalKcal * KCAL_TO_KJ_RATIO).roundToInt()

        val finalFiber = baseFiberPer100g?.let { it * factor }
        val finalSugar = baseSugarPer100g?.let { it * factor }
        val finalSodium = baseSodiumPer100g?.let { it * factor }
        val finalSaturatedFat = finalFat * 0.30f // Typical ~30% saturated fatty acid ratio

        return NutrientBreakdown(
            energyKcal = finalKcal,
            energyKj = finalKj,
            proteinGrams = (finalProtein * 10f).roundToInt() / 10f,
            carbsGrams = (finalCarbs * 10f).roundToInt() / 10f,
            fatGrams = (finalFat * 10f).roundToInt() / 10f,
            saturatedFatGrams = (finalSaturatedFat * 10f).roundToInt() / 10f,
            fiberGrams = finalFiber?.let { (it * 10f).roundToInt() / 10f },
            sugarGrams = finalSugar?.let { (it * 10f).roundToInt() / 10f },
            sodiumMg = finalSodium?.let { (it * 10f).roundToInt() / 10f },
            preparationMethod = cookingMethod.name,
            isApproximate = true
        )
    }

    override fun aggregateMeal(items: List<NutrientBreakdown>): NutrientBreakdown {
        var totalKcal = 0
        var totalP = 0f
        var totalC = 0f
        var totalF = 0f
        var totalSatFat = 0f
        var totalFib: Float? = null
        var totalSug: Float? = null
        var totalSod: Float? = null

        for (item in items) {
            totalKcal += item.energyKcal
            totalP += item.proteinGrams
            totalC += item.carbsGrams
            totalF += item.fatGrams
            item.saturatedFatGrams?.let { totalSatFat += it }

            item.fiberGrams?.let {
                totalFib = (totalFib ?: 0f) + it
            }
            item.sugarGrams?.let {
                totalSug = (totalSug ?: 0f) + it
            }
            item.sodiumMg?.let {
                totalSod = (totalSod ?: 0f) + it
            }
        }

        return NutrientBreakdown(
            energyKcal = totalKcal,
            energyKj = (totalKcal * KCAL_TO_KJ_RATIO).roundToInt(),
            proteinGrams = (totalP * 10f).roundToInt() / 10f,
            carbsGrams = (totalC * 10f).roundToInt() / 10f,
            fatGrams = (totalF * 10f).roundToInt() / 10f,
            saturatedFatGrams = (totalSatFat * 10f).roundToInt() / 10f,
            fiberGrams = totalFib?.let { (it * 10f).roundToInt() / 10f },
            sugarGrams = totalSug?.let { (it * 10f).roundToInt() / 10f },
            sodiumMg = totalSod?.let { (it * 10f).roundToInt() / 10f },
            preparationMethod = "Mixed Meal Aggregation",
            isApproximate = true
        )
    }
}
