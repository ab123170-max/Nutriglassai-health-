package com.example.domain

import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Standard WHO BMI Classifications with health category and styling hints.
 */
enum class BmiCategory(
    val title: String,
    val minBmi: Float,
    val maxBmi: Float,
    val healthRisk: String
) {
    SEVERELY_UNDERWEIGHT("Severely Underweight", 0f, 15.99f, "High Risk of Nutritional Deficiency"),
    UNDERWEIGHT("Underweight", 16.0f, 18.49f, "Moderate Risk"),
    NORMAL("Normal Weight", 18.5f, 24.99f, "Low Risk (Healthy Range)"),
    OVERWEIGHT("Overweight", 25.0f, 29.99f, "Increased Risk"),
    OBESE_CLASS_1("Obese Class I", 30.0f, 34.99f, "High Risk"),
    OBESE_CLASS_2("Obese Class II", 35.0f, 39.99f, "Very High Risk"),
    OBESE_CLASS_3("Obese Class III", 40.0f, 100f, "Extremely High Risk");

    companion object {
        fun fromBmi(bmi: Float): BmiCategory {
            return entries.firstOrNull { bmi in it.minBmi..it.maxBmi }
                ?: if (bmi < 16f) SEVERELY_UNDERWEIGHT else OBESE_CLASS_3
        }
    }
}

/**
 * Body Fat Classifications based on American Council on Exercise (ACE).
 */
enum class BodyFatCategory(
    val title: String,
    val maleRange: ClosedFloatingPointRange<Float>,
    val femaleRange: ClosedFloatingPointRange<Float>,
    val description: String
) {
    ESSENTIAL_FAT("Essential Fat", 2.0f..5.9f, 10.0f..13.9f, "Minimum fat necessary for basic physiological survival"),
    ATHLETES("Athletic", 6.0f..13.9f, 14.0f..20.9f, "Optimal body composition for athletic endurance & performance"),
    FITNESS("Fitness", 14.0f..17.9f, 21.0f..24.9f, "Lean, healthy muscular definition with low chronic disease risk"),
    AVERAGE("Acceptable", 18.0f..24.9f, 25.0f..31.9f, "Standard healthy range for general active population"),
    OBESE("Elevated / Obese", 25.0f..70.0f, 32.0f..70.0f, "Increased cardiovascular and metabolic health risk");

    companion object {
        fun fromPercentage(percentage: Float, isMale: Boolean): BodyFatCategory {
            val clamped = percentage.coerceIn(2f, 65f)
            return entries.firstOrNull {
                val range = if (isMale) it.maleRange else it.femaleRange
                clamped in range
            } ?: if (clamped > 30f) OBESE else ESSENTIAL_FAT
        }
    }
}

enum class CalculationMethod(val displayName: String) {
    NAVY_CIRCUMFERENCE("U.S. Navy Circumference Method"),
    BMI_AGE_ESTIMATION("Jackson-Pollock / Deurenberg BMI Formula")
}

data class BodyCompositionResult(
    val bmi: Float,
    val bmiCategory: BmiCategory,
    val healthyBmiMin: Float = 18.5f,
    val healthyBmiMax: Float = 24.9f,
    val minHealthyWeightKg: Float,
    val maxHealthyWeightKg: Float,
    val targetBmi: Float,
    val weightDeltaToTargetKg: Float,
    val bodyFatPercentage: Float,
    val bodyFatCategory: BodyFatCategory,
    val fatMassKg: Float,
    val leanMassKg: Float,
    val calculationMethod: CalculationMethod,
    val isCircumferenceMeasured: Boolean,
    val essentialFatKg: Float,
    val storageFatKg: Float,
    val waistToHeightRatio: Float? = null,
    val waistToHipRatio: Float? = null
)

object BodyCompositionEngine {

    const val MEDICAL_DISCLAIMER =
        "Educational anatomical visualization and mathematical estimation. BMI and circumference equations do not measure regional visceral fat directly and are not a medical diagnosis. Consult a physician for clinical health assessments."

    /**
     * Calculate Body Mass Index (BMI = kg / m^2).
     */
    fun calculateBmi(weightKg: Float, heightCm: Float): Float {
        if (heightCm <= 0f || weightKg <= 0f) return 0f
        val heightMeters = heightCm / 100f
        val bmi = weightKg / (heightMeters * heightMeters)
        return (bmi * 10f).roundToInt() / 10f
    }

    /**
     * Compute healthy weight range (BMI 18.5 to 24.9) for a given height in cm.
     */
    fun calculateHealthyWeightRange(heightCm: Float): Pair<Float, Float> {
        if (heightCm <= 0f) return Pair(0f, 0f)
        val heightMeters = heightCm / 100f
        val heightSquared = heightMeters * heightMeters
        val minKg = (18.5f * heightSquared * 10f).roundToInt() / 10f
        val maxKg = (24.9f * heightSquared * 10f).roundToInt() / 10f
        return Pair(minKg, maxKg)
    }

    /**
     * U.S. Navy Body Fat Formula using circumference measurements.
     * Male formula: 495 / (1.0324 - 0.19077 * log10(waist - neck) + 0.15456 * log10(height)) - 450
     * Female formula: 495 / (1.29579 - 0.35004 * log10(waist + hip - neck) + 0.22100 * log10(height)) - 450
     * All measurements must be in centimeters.
     */
    fun calculateNavyBodyFatPercentage(
        isMale: Boolean,
        heightCm: Float,
        waistCm: Float,
        neckCm: Float,
        hipCm: Float? = null
    ): Float? {
        if (heightCm <= 50f || waistCm <= 30f || neckCm <= 15f) return null

        return try {
            val bodyFat: Double = if (isMale) {
                val diff = (waistCm - neckCm).toDouble()
                if (diff <= 0.0) return null
                val denominator = 1.0324 - (0.19077 * log10(diff)) + (0.15456 * log10(heightCm.toDouble()))
                if (denominator <= 0.0) return null
                (495.0 / denominator) - 450.0
            } else {
                val hip = hipCm ?: (waistCm * 1.15f) // Fallback if hip not provided
                val sumDiff = (waistCm + hip - neckCm).toDouble()
                if (sumDiff <= 0.0) return null
                val denominator = 1.29579 - (0.35004 * log10(sumDiff)) + (0.22100 * log10(heightCm.toDouble()))
                if (denominator <= 0.0) return null
                (495.0 / denominator) - 450.0
            }

            if (bodyFat.isNaN() || bodyFat.isInfinite()) return null
            val clamped = bodyFat.toFloat().coerceIn(3.0f, 65.0f)
            (clamped * 10f).roundToInt() / 10f
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Deurenberg formula for adult body fat estimation based on BMI, age, and biological sex.
     * BF% = (1.20 * BMI) + (0.23 * Age) - (10.8 * sex) - 5.4, where sex = 1 for male, 0 for female.
     */
    fun calculateBmiBasedBodyFatPercentage(
        bmi: Float,
        age: Int,
        isMale: Boolean
    ): Float {
        val sexFactor = if (isMale) 1.0f else 0.0f
        val safeAge = age.coerceIn(18, 100)
        val estimatedBf = (1.20f * bmi) + (0.23f * safeAge) - (10.8f * sexFactor) - 5.4f
        val clamped = estimatedBf.coerceIn(4.0f, 65.0f)
        return (clamped * 10f).roundToInt() / 10f
    }

    /**
     * Complete body composition analysis.
     */
    fun analyze(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        gender: String,
        targetWeightKg: Float = weightKg,
        waistCm: Float? = null,
        neckCm: Float? = null,
        hipCm: Float? = null
    ): BodyCompositionResult {
        val isMale = !gender.equals("Female", ignoreCase = true)
        val bmi = calculateBmi(weightKg, heightCm)
        val bmiCategory = BmiCategory.fromBmi(bmi)
        val (minHealthyKg, maxHealthyKg) = calculateHealthyWeightRange(heightCm)
        val targetBmi = calculateBmi(targetWeightKg, heightCm)
        val weightDelta = (weightKg - targetWeightKg * 10f).roundToInt() / 10f

        // Try Navy circumference method first if measurements are present
        val navyBf = if (waistCm != null && neckCm != null && waistCm > neckCm) {
            calculateNavyBodyFatPercentage(isMale, heightCm, waistCm, neckCm, hipCm)
        } else {
            null
        }

        val (bodyFatPercent, method, isCircumference) = if (navyBf != null) {
            Triple(navyBf, CalculationMethod.NAVY_CIRCUMFERENCE, true)
        } else {
            val fallbackBf = calculateBmiBasedBodyFatPercentage(bmi, age, isMale)
            Triple(fallbackBf, CalculationMethod.BMI_AGE_ESTIMATION, false)
        }

        val fatMassKg = ((weightKg * (bodyFatPercent / 100f)) * 10f).roundToInt() / 10f
        val leanMassKg = ((weightKg - fatMassKg) * 10f).roundToInt() / 10f
        val bodyFatCat = BodyFatCategory.fromPercentage(bodyFatPercent, isMale)

        // Essential vs storage fat breakdown
        val essentialPct = if (isMale) 3.5f else 12.0f
        val essentialKg = ((weightKg * (essentialPct / 100f)) * 10f).roundToInt() / 10f
        val storageKg = (fatMassKg - essentialKg).coerceAtLeast(0f)

        // Waist ratios
        val waistToHeight = if (waistCm != null && heightCm > 0f) {
            (waistCm / heightCm * 100f).roundToInt() / 100f
        } else null

        val waistToHip = if (waistCm != null && hipCm != null && hipCm > 0f) {
            (waistCm / hipCm * 100f).roundToInt() / 100f
        } else null

        return BodyCompositionResult(
            bmi = bmi,
            bmiCategory = bmiCategory,
            minHealthyWeightKg = minHealthyKg,
            maxHealthyWeightKg = maxHealthyKg,
            targetBmi = targetBmi,
            weightDeltaToTargetKg = weightDelta,
            bodyFatPercentage = bodyFatPercent,
            bodyFatCategory = bodyFatCat,
            fatMassKg = fatMassKg,
            leanMassKg = leanMassKg,
            calculationMethod = method,
            isCircumferenceMeasured = isCircumference,
            essentialFatKg = essentialKg,
            storageFatKg = storageKg,
            waistToHeightRatio = waistToHeight,
            waistToHipRatio = waistToHip
        )
    }

    // Unit conversion utilities
    fun kgToLbs(kg: Float): Float = (kg * 2.20462f * 10f).roundToInt() / 10f
    fun lbsToKg(lbs: Float): Float = (lbs / 2.20462f * 10f).roundToInt() / 10f
    fun cmToInches(cm: Float): Float = (cm / 2.54f * 10f).roundToInt() / 10f
    fun inchesToCm(inches: Float): Float = (inches * 2.54f * 10f).roundToInt() / 10f

    fun cmToFeetInches(cm: Float): Pair<Int, Int> {
        val totalInches = (cm / 2.54f).roundToInt()
        val feet = totalInches / 12
        val inches = totalInches % 12
        return Pair(feet, inches)
    }

    fun feetInchesToCm(feet: Int, inches: Int): Float {
        val totalInches = (feet * 12) + inches
        return (totalInches * 2.54f * 10f).roundToInt() / 10f
    }
}
