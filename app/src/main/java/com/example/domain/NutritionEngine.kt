package com.example.domain

import com.example.data.model.MealEntry
import kotlin.math.roundToInt

data class MacroDistribution(
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val fiberGrams: Float
)

data class BmiResult(
    val bmi: Float,
    val category: String,
    val healthyWeightMinKg: Float,
    val healthyWeightMaxKg: Float
)

data class DailyNutritionSummary(
    val totalCaloriesConsumed: Int,
    val totalProteinGrams: Float,
    val totalCarbsGrams: Float,
    val totalFatGrams: Float,
    val totalFiberGrams: Float,
    val totalSugarGrams: Float,
    val calorieTarget: Int,
    val remainingCalories: Int,
    val targetProteinGrams: Float,
    val targetCarbsGrams: Float,
    val targetFatGrams: Float
)

object NutritionEngine {

    /**
     * Mifflin-St Jeor equation for Basal Metabolic Rate (BMR)
     */
    fun calculateBmr(weightKg: Float, heightCm: Float, age: Int, gender: String): Float {
        val base = (10f * weightKg) + (6.25f * heightCm) - (5f * age)
        return if (gender.equals("Female", ignoreCase = true)) {
            base - 161f
        } else {
            base + 5f
        }
    }

    /**
     * Total Daily Energy Expenditure (TDEE) based on physical activity level
     */
    fun calculateTdee(bmr: Float, activityLevel: String): Float {
        val multiplier = when (activityLevel.lowercase()) {
            "sedentary" -> 1.20f
            "light" -> 1.375f
            "moderate" -> 1.55f
            "active" -> 1.725f
            "very active" -> 1.90f
            else -> 1.45f
        }
        return bmr * multiplier
    }

    /**
     * Safe personalized calorie target based on health goal with clinical minimum floors
     */
    fun calculateCalorieTarget(tdee: Float, fitnessGoal: String, gender: String): Int {
        val minFloor = if (gender.equals("Female", ignoreCase = true)) 1200 else 1500

        val target = when (fitnessGoal.lowercase()) {
            "lose weight" -> (tdee - 450f).roundToInt()
            "gain muscle", "gain weight" -> (tdee + 350f).roundToInt()
            else -> tdee.roundToInt() // "maintain weight", "improve fitness", "eat healthier"
        }

        return target.coerceAtLeast(minFloor)
    }

    /**
     * Balanced macronutrient breakdown
     */
    fun calculateMacroDistribution(calorieTarget: Int, weightKg: Float): MacroDistribution {
        // High quality protein: ~1.8g to 2.0g per kg of body weight
        val proteinGrams = (weightKg * 1.8f).coerceIn(55f, 180f)
        val proteinCalories = proteinGrams * 4f

        // Healthy dietary fats: 25-30% of total daily calorie budget (9 kcal/g)
        val fatCalories = calorieTarget * 0.28f
        val fatGrams = fatCalories / 9f

        // Remaining calorie budget allocated to complex carbohydrates (4 kcal/g)
        val remainingCaloriesForCarbs = (calorieTarget - proteinCalories - fatCalories).coerceAtLeast(200f)
        val carbsGrams = remainingCaloriesForCarbs / 4f

        // Recommended dietary fiber: ~14g per 1000 kcal consumed
        val fiberGrams = (calorieTarget / 1000f) * 14f

        return MacroDistribution(
            proteinGrams = (proteinGrams * 10).roundToInt() / 10f,
            carbsGrams = (carbsGrams * 10).roundToInt() / 10f,
            fatGrams = (fatGrams * 10).roundToInt() / 10f,
            fiberGrams = (fiberGrams * 10).roundToInt() / 10f
        )
    }

    /**
     * Body Mass Index (BMI) & classification
     */
    fun calculateBmi(weightKg: Float, heightCm: Float): BmiResult {
        val heightM = heightCm / 100f
        val bmi = if (heightM > 0) weightKg / (heightM * heightM) else 22f

        val category = when {
            bmi < 18.5f -> "Underweight"
            bmi < 25.0f -> "Normal weight"
            bmi < 30.0f -> "Overweight"
            else -> "Obesity"
        }

        val healthyMin = 18.5f * (heightM * heightM)
        val healthyMax = 24.9f * (heightM * heightM)

        return BmiResult(
            bmi = (bmi * 10).roundToInt() / 10f,
            category = category,
            healthyWeightMinKg = (healthyMin * 10).roundToInt() / 10f,
            healthyWeightMaxKg = (healthyMax * 10).roundToInt() / 10f
        )
    }

    /**
     * Compute totals from logged meal entries
     */
    fun summarizeDayMeals(
        meals: List<MealEntry>,
        calorieTarget: Int,
        weightKg: Float
    ): DailyNutritionSummary {
        var totalCals = 0
        var totalProtein = 0f
        var totalCarbs = 0f
        var totalFat = 0f
        var totalFiber = 0f
        var totalSugar = 0f

        for (meal in meals) {
            totalCals += meal.calories
            totalProtein += meal.protein
            totalCarbs += meal.carbs
            totalFat += meal.fat
            totalFiber += meal.fiber
            totalSugar += meal.sugar
        }

        val macros = calculateMacroDistribution(calorieTarget, weightKg)

        return DailyNutritionSummary(
            totalCaloriesConsumed = totalCals,
            totalProteinGrams = (totalProtein * 10).roundToInt() / 10f,
            totalCarbsGrams = (totalCarbs * 10).roundToInt() / 10f,
            totalFatGrams = (totalFat * 10).roundToInt() / 10f,
            totalFiberGrams = (totalFiber * 10).roundToInt() / 10f,
            totalSugarGrams = (totalSugar * 10).roundToInt() / 10f,
            calorieTarget = calorieTarget,
            remainingCalories = calorieTarget - totalCals,
            targetProteinGrams = macros.proteinGrams,
            targetCarbsGrams = macros.carbsGrams,
            targetFatGrams = macros.fatGrams
        )
    }

    /**
     * Cleanly formats a meal header from a list of food component names.
     * 1. Removes duplicate names using a Set (distinct after trim).
     * 2. Formats cleanly based on count:
     *    - 1 item: "Name"
     *    - 2 items: "Name1, Name2"
     *    - 3+ items: "Name1, Name2 +N more"
     */
    fun renderMealHeader(componentNames: List<String>?): String {
        if (componentNames.isNullOrEmpty()) return "Scanned Meal"

        val uniqueNames = componentNames
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (uniqueNames.isEmpty()) return "Scanned Meal"

        return when {
            uniqueNames.size == 1 -> uniqueNames[0]
            uniqueNames.size <= 2 -> uniqueNames.joinToString(", ")
            else -> "${uniqueNames[0]}, ${uniqueNames[1]} +${uniqueNames.size - 2} more"
        }
    }
}
