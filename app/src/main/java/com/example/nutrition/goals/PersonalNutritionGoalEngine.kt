package com.example.nutrition.goals

import kotlin.math.roundToInt

enum class FitnessGoalType(val displayName: String, val dailyCalorieDelta: Int) {
    LOSE_WEIGHT("Gradual Weight Loss", -400),
    MAINTAIN_WEIGHT("Maintain Weight", 0),
    GAIN_MUSCLE("Muscle Gain / Lean Bulk", +300),
    IMPROVE_FITNESS("General Health & Vitality", 0)
}

enum class ActivityLevel(val multiplier: Float, val description: String) {
    SEDENTARY(1.20f, "Little or no exercise, desk work"),
    LIGHT(1.375f, "Light exercise or sports 1-3 days/week"),
    MODERATE(1.55f, "Moderate exercise or sports 3-5 days/week"),
    ACTIVE(1.725f, "Hard exercise or active physical work 6-7 days/week"),
    VERY_ACTIVE(1.90f, "Very hard exercise, heavy physical labor or athletic training")
}

data class EnergyRequirementCalculation(
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val recommendedTargetKcal: Int,
    val isSafetyFlagged: Boolean,
    val safetyWarning: String?,
    val formulaUsed: String = "Mifflin-St Jeor Equation (1990)",
    val disclaimer: String = "Energy estimates are mathematical approximations. Individual metabolism and body composition vary. Do not adopt extreme calorie deficits without consulting a registered dietitian or doctor."
)

data class TargetMacroGrams(
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val fiberGrams: Float
)

data class RemainingNutrients(
    val remainingCalories: Int,
    val remainingProtein: Float,
    val remainingCarbs: Float,
    val remainingFat: Float,
    val remainingFiber: Float,
    val isCalorieSurplus: Boolean
)

interface PersonalNutritionGoalEngine {
    fun calculateEnergyRequirements(
        age: Int,
        gender: String,
        heightCm: Float,
        weightKg: Float,
        activityLevel: ActivityLevel,
        goal: FitnessGoalType
    ): EnergyRequirementCalculation

    fun calculateMacroTargets(dailyCalories: Int, goal: FitnessGoalType): TargetMacroGrams

    fun computeRemainingDailyNutrients(
        targetCalories: Int,
        targetMacros: TargetMacroGrams,
        consumedCalories: Int,
        consumedProtein: Float,
        consumedCarbs: Float,
        consumedFat: Float,
        consumedFiber: Float
    ): RemainingNutrients

    fun convertWeight(value: Float, fromUnit: String, toUnit: String): Float
    fun convertHeight(value: Float, fromUnit: String, toUnit: String): Float
}

class StandardPersonalNutritionGoalEngine : PersonalNutritionGoalEngine {

    companion object {
        // Clinical floor recommendations (American College of Sports Medicine / WHO)
        const val MIN_SAFE_FEMALE_CALORIES = 1200
        const val MIN_SAFE_MALE_CALORIES = 1500
    }

    override fun calculateEnergyRequirements(
        age: Int,
        gender: String,
        heightCm: Float,
        weightKg: Float,
        activityLevel: ActivityLevel,
        goal: FitnessGoalType
    ): EnergyRequirementCalculation {
        // Mifflin-St Jeor formula:
        // Men: BMR = 10 * weight(kg) + 6.25 * height(cm) - 5 * age(y) + 5
        // Women: BMR = 10 * weight(kg) + 6.25 * height(cm) - 5 * age(y) - 161
        val isMale = gender.equals("Male", ignoreCase = true)
        val s = if (isMale) 5f else -161f
        val rawBmr = (10f * weightKg) + (6.25f * heightCm) - (5f * age) + s
        val bmr = rawBmr.roundToInt().coerceAtLeast(800)

        val tdee = (bmr * activityLevel.multiplier).roundToInt()
        val target = tdee + goal.dailyCalorieDelta

        val safeFloor = if (isMale) MIN_SAFE_MALE_CALORIES else MIN_SAFE_FEMALE_CALORIES
        val isFlagged = target < safeFloor

        val warning = if (isFlagged) {
            "Configured calorie goal ($target kcal) is below the minimum recommended physiological floor ($safeFloor kcal/day). Extreme caloric restriction risks nutritional deficiencies, muscle loss, and metabolic slowdown."
        } else null

        val finalTarget = if (isFlagged) safeFloor else target

        return EnergyRequirementCalculation(
            bmrKcal = bmr,
            tdeeKcal = tdee,
            recommendedTargetKcal = finalTarget,
            isSafetyFlagged = isFlagged,
            safetyWarning = warning
        )
    }

    override fun calculateMacroTargets(dailyCalories: Int, goal: FitnessGoalType): TargetMacroGrams {
        // Standard balanced distribution: 25% Protein, 45% Carbs, 30% Fat
        // High protein for muscle gain/fat loss: 30% Protein, 40% Carbs, 30% Fat
        val (pRatio, cRatio, fRatio) = when (goal) {
            FitnessGoalType.LOSE_WEIGHT, FitnessGoalType.GAIN_MUSCLE -> Triple(0.28f, 0.42f, 0.30f)
            else -> Triple(0.20f, 0.50f, 0.30f)
        }

        val pGrams = ((dailyCalories * pRatio) / 4f)
        val cGrams = ((dailyCalories * cRatio) / 4f)
        val fGrams = ((dailyCalories * fRatio) / 9f)
        val fiberGrams = (dailyCalories / 1000f) * 14f // 14g fiber per 1000 kcal (USDA guideline)

        return TargetMacroGrams(
            proteinGrams = (pGrams * 10f).roundToInt() / 10f,
            carbsGrams = (cGrams * 10f).roundToInt() / 10f,
            fatGrams = (fGrams * 10f).roundToInt() / 10f,
            fiberGrams = (fiberGrams * 10f).roundToInt() / 10f
        )
    }

    override fun computeRemainingDailyNutrients(
        targetCalories: Int,
        targetMacros: TargetMacroGrams,
        consumedCalories: Int,
        consumedProtein: Float,
        consumedCarbs: Float,
        consumedFat: Float,
        consumedFiber: Float
    ): RemainingNutrients {
        val remCals = targetCalories - consumedCalories
        val remP = targetMacros.proteinGrams - consumedProtein
        val remC = targetMacros.carbsGrams - consumedCarbs
        val remF = targetMacros.fatGrams - consumedFat
        val remFib = targetMacros.fiberGrams - consumedFiber

        return RemainingNutrients(
            remainingCalories = remCals,
            remainingProtein = remP,
            remainingCarbs = remC,
            remainingFat = remF,
            remainingFiber = remFib,
            isCalorieSurplus = remCals < 0
        )
    }

    override fun convertWeight(value: Float, fromUnit: String, toUnit: String): Float {
        if (fromUnit.equals(toUnit, ignoreCase = true)) return value
        val kg = when (fromUnit.lowercase()) {
            "lb", "lbs" -> value * 0.453592f
            "st" -> value * 6.35029f
            "g" -> value / 1000f
            else -> value
        }
        return when (toUnit.lowercase()) {
            "lb", "lbs" -> kg / 0.453592f
            "st" -> kg / 6.35029f
            "g" -> kg * 1000f
            else -> kg
        }
    }

    override fun convertHeight(value: Float, fromUnit: String, toUnit: String): Float {
        if (fromUnit.equals(toUnit, ignoreCase = true)) return value
        val cm = when (fromUnit.lowercase()) {
            "in", "inch", "inches" -> value * 2.54f
            "m" -> value * 100f
            "ft" -> value * 30.48f
            else -> value
        }
        return when (toUnit.lowercase()) {
            "in", "inch", "inches" -> cm / 2.54f
            "m" -> cm / 100f
            "ft" -> cm / 30.48f
            else -> cm
        }
    }
}
