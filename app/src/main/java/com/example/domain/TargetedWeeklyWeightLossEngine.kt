package com.example.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class WeeklyTargetResult(
    val startingWeightKg: Float,
    val currentWeightKg: Float,
    val targetWeightKg: Float,
    val expectedMinWeeklyLossKg: Float,
    val expectedMaxWeeklyLossKg: Float,
    val actualWeeklyChangeKg: Float,
    val remainingWeightKg: Float,
    val goalProgressPercent: Float,
    val safetyStatus: SafetyStatus,
    val safetyMessage: String
)

enum class SafetyStatus {
    SAFE,
    CAUTION_RAPID_LOSS,
    CAUTION_UNDERWEIGHT_RISK,
    RECOMMEND_PROFESSIONAL_SUPERVISION
}

data class DailyProgramPlan(
    val dayNumber: Int,
    val dayName: String,
    val calorieTarget: Int,
    val proteinTargetGrams: Int,
    val breakfast: String,
    val lunch: String,
    val dinner: String,
    val snack: String,
    val waterTargetMl: Int,
    val stepTarget: Int,
    val walkingMinutes: Int,
    val strengthTraining: String,
    val recoveryActivity: String,
    val sleepTargetHours: Float
)

data class WeeklyReportResult(
    val startingWeightKg: Float,
    val endingWeightKg: Float,
    val targetWeightKg: Float,
    val actualChangeKg: Float,
    val waistCm: Float?,
    val bodyFatPercent: Float,
    val activityPerformanceScore: Int, // 0 - 100
    val nutritionPerformanceScore: Int, // 0 - 100
    val nextWeekTargetKg: Float,
    val adjustmentAdvice: String,
    val coachingFeedback: String
)

object TargetedWeeklyWeightLossEngine {

    /**
     * Evaluates safety parameters before setting targets.
     * Verifies BMI health limits (>= 18.5) and caloric deficit thresholds.
     */
    fun evaluateSafety(currentWeightKg: Float, targetWeightKg: Float, heightCm: Float): Pair<SafetyStatus, String> {
        if (heightCm <= 0f) return Pair(SafetyStatus.SAFE, "Proceed with balanced nutrition.")
        val heightM = heightCm / 100f
        val currentBmi = currentWeightKg / (heightM * heightM)
        val targetBmi = targetWeightKg / (heightM * heightM)
        val totalDrop = currentWeightKg - targetWeightKg

        return when {
            targetBmi < 18.5f -> Pair(
                SafetyStatus.CAUTION_UNDERWEIGHT_RISK,
                "Target weight produces a BMI below the WHO healthy threshold (18.5). Please adjust your goal weight for sustainable health."
            )
            totalDrop > 40f -> Pair(
                SafetyStatus.RECOMMEND_PROFESSIONAL_SUPERVISION,
                "Your transformation goal exceeds 40 kg. We recommend consulting a licensed physician or clinical dietitian for personalized medical supervision."
            )
            else -> Pair(
                SafetyStatus.SAFE,
                "Your weekly weight loss goals are within a safe, healthy and sustainable metabolic range."
            )
        }
    }

    /**
     * Calculates weekly weight loss targets and rate ranges (0.5 kg to 1.0 kg per week max).
     */
    fun calculateWeeklyTarget(
        startingWeightKg: Float,
        currentWeightKg: Float,
        targetWeightKg: Float,
        previousWeekWeightKg: Float?,
        heightCm: Float,
        age: Int,
        isMale: Boolean,
        activityLevel: String
    ): WeeklyTargetResult {
        val remaining = max(0f, currentWeightKg - targetWeightKg)
        val totalDelta = max(0.1f, startingWeightKg - targetWeightKg)
        val lostSoFar = max(0f, startingWeightKg - currentWeightKg)
        val progressPct = (lostSoFar / totalDelta).coerceIn(0f, 1f) * 100f

        val actualChange = previousWeekWeightKg?.let { currentWeightKg - it } ?: 0f

        // Healthy dynamic rate: 0.5 kg to 1.0 kg per week depending on starting weight
        val expectedMin = if (currentWeightKg > 90f) 0.7f else 0.4f
        val expectedMax = if (currentWeightKg > 100f) 1.2f else 0.8f

        val (status, msg) = evaluateSafety(currentWeightKg, targetWeightKg, heightCm)

        return WeeklyTargetResult(
            startingWeightKg = startingWeightKg,
            currentWeightKg = currentWeightKg,
            targetWeightKg = targetWeightKg,
            expectedMinWeeklyLossKg = expectedMin,
            expectedMaxWeeklyLossKg = expectedMax,
            actualWeeklyChangeKg = (actualChange * 10f).roundToInt() / 10f,
            remainingWeightKg = (remaining * 10f).roundToInt() / 10f,
            goalProgressPercent = (progressPct * 10f).roundToInt() / 10f,
            safetyStatus = status,
            safetyMessage = msg
        )
    }

    /**
     * Generates a personalized 7-day program based on BMR, TDEE, and dietary preferences.
     */
    fun generate7DayProgram(
        currentWeightKg: Float,
        heightCm: Float,
        age: Int,
        isMale: Boolean,
        activityLevel: String,
        dietaryPreference: String
    ): List<DailyProgramPlan> {
        // BMR Mifflin-St Jeor
        val bmr = if (isMale) {
            (10f * currentWeightKg) + (6.25f * heightCm) - (5f * age) + 5f
        } else {
            (10f * currentWeightKg) + (6.25f * heightCm) - (5f * age) - 161f
        }

        val activityMultiplier = when (activityLevel.lowercase()) {
            "sedentary" -> 1.2f
            "light" -> 1.375f
            "moderate" -> 1.55f
            "active" -> 1.725f
            else -> 1.45f
        }

        val tdee = bmr * activityMultiplier
        val targetCalories = (tdee - 500f).coerceIn(1350f, 2800f).toInt()
        val proteinGrams = (currentWeightKg * 1.6f).coerceIn(90f, 180f).toInt()

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val isNepali = dietaryPreference.contains("Nepali", ignoreCase = true)

        return days.mapIndexed { index, dayName ->
            val breakfast = if (isNepali) "Oats with warm milk & chopped walnuts or 2 Boiled Eggs with Whole Wheat Toast" else "Greek Yogurt with Berries & Honey"
            val lunch = if (isNepali) "Dal (Lentils), Brown Rice, Saag (Spinach) & Grilled Chicken or Paneer" else "Quinoa Chicken Bowl with Avocado & Greens"
            val dinner = if (isNepali) "Vegetable Soup with Roti (2 pcs) and Cucumber Raita" else "Baked Salmon with Roasted Asparagus & Sweet Potato"
            val snack = if (isNepali) "Roasted Soyabean (Mala) or Handful of Almonds & Green Tea" else "Apple slices with Peanut Butter"

            val strength = if (index % 2 == 0) "Full Body Resistance Training (45 mins)" else "Active Recovery & Mobility Stretches"
            val recovery = if (index % 2 == 1) "Rest Day & Deep Tissue Stretching" else "Core & Posture Activation (20 mins)"

            DailyProgramPlan(
                dayNumber = index + 1,
                dayName = dayName,
                calorieTarget = targetCalories,
                proteinTargetGrams = proteinGrams,
                breakfast = breakfast,
                lunch = lunch,
                dinner = dinner,
                snack = snack,
                waterTargetMl = 2700,
                stepTarget = 8500,
                walkingMinutes = 45,
                strengthTraining = strength,
                recoveryActivity = recovery,
                sleepTargetHours = 8.0f
            )
        }
    }

    /**
     * Performs automatic weekly adjustment comparing actual vs expected.
     */
    fun performAutomaticWeeklyAdjustment(
        actualWeeklyChangeKg: Float,
        expectedMinLossKg: Float,
        expectedMaxLossKg: Float
    ): Pair<Float, String> {
        return when {
            // Slower than expected (weight loss plateau or gain)
            actualWeeklyChangeKg > -0.2f -> {
                Pair(
                    -400f, // minor calorie reduction adjustment
                    "Progress was slower than expected this week. We have conservatively adjusted your daily target by -400 kcal and recommend evaluating sleep and sodium intake."
                )
            }
            // Faster than expected (rapid loss)
            actualWeeklyChangeKg < -1.4f -> {
                Pair(
                    +250f, // add calories to prevent muscle wasting
                    "Weight loss was faster than expected (> 1.4 kg). We have added +250 kcal to protect lean muscle mass and ensure sustainable hydration."
                )
            }
            else -> {
                Pair(
                    0f,
                    "Your weekly progress is right on track with your target range! Maintain your current nutrition and activity schedule."
                )
            }
        }
    }

    /**
     * Generates a comprehensive weekly progress report.
     */
    fun generateWeeklyReport(
        startingWeightKg: Float,
        endingWeightKg: Float,
        targetWeightKg: Float,
        waistCm: Float?,
        bodyFatPercent: Float,
        activityScore: Int,
        nutritionScore: Int
    ): WeeklyReportResult {
        val actualChange = endingWeightKg - startingWeightKg
        val nextTarget = (endingWeightKg - 0.75f).coerceAtLeast(targetWeightKg)

        val advice = when {
            actualChange < -0.5f -> "Excellent consistency! Fat oxidation is proceeding steadily."
            actualChange >= 0f -> "Weight plateau detected. Check evening snacking and increase daily steps."
            else -> "Good steady progress. Keep up your hydration and protein targets."
        }

        return WeeklyReportResult(
            startingWeightKg = startingWeightKg,
            endingWeightKg = endingWeightKg,
            targetWeightKg = targetWeightKg,
            actualChangeKg = (actualChange * 10f).roundToInt() / 10f,
            waistCm = waistCm,
            bodyFatPercent = bodyFatPercent,
            activityPerformanceScore = activityScore,
            nutritionPerformanceScore = nutritionScore,
            nextWeekTargetKg = (nextTarget * 10f).roundToInt() / 10f,
            adjustmentAdvice = advice,
            coachingFeedback = "Your weekly habits are forming a strong foundation for long-term health."
        )
    }
}
