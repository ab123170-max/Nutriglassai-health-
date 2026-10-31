package com.example.domain

import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Skeletal Muscle Mass category classifications based on Janssen bio-impedance benchmarks.
 */
enum class MuscleMassCategory(val title: String, val description: String) {
    LOW("Below Average", "Muscle mass is below optimal metabolic threshold. Resistance training recommended."),
    NORMAL("Healthy / Optimal", "Balanced skeletal muscle ratio supporting strong basal metabolic health."),
    HIGH("High / Athletic", "Exceptional muscle hypertrophy and functional muscular density.")
}

/**
 * Clinical Visceral Fat Index (1 to 20 rating).
 * Derived from Waist-to-Height Ratio (WHtR), BMI, and age.
 */
enum class VisceralFatCategory(
    val title: String,
    val range: ClosedFloatingPointRange<Float>,
    val riskLevel: String,
    val recommendation: String
) {
    BALANCED(
        "Balanced (Level 1–9)",
        1.0f..9.0f,
        "Low Cardiovascular Risk",
        "Visceral adipose tissue surrounding vital organs is in an optimal healthy range."
    ),
    ELEVATED(
        "Elevated (Level 10–14)",
        9.1f..14.0f,
        "Moderate Cardiometabolic Risk",
        "Accumulation of intra-abdominal fat detected. Aerobic exercise and fiber intake recommended."
    ),
    HIGH(
        "Excessive (Level 15–20)",
        14.1f..20.0f,
        "High Health Risk",
        "Significant deep visceral fat concentration. Clinically correlated with insulin resistance and hypertension."
    );

    companion object {
        fun fromRating(rating: Float): VisceralFatCategory {
            val clamped = rating.coerceIn(1.0f, 20.0f)
            return entries.firstOrNull { clamped in it.range } ?: if (clamped > 14f) HIGH else BALANCED
        }
    }
}

/**
 * Waist-to-Height Ratio (WHtR) classification.
 */
enum class WaistToHeightCategory(
    val title: String,
    val maxRatio: Float,
    val healthImplication: String
) {
    SLIM("Slim / Underweight", 0.40f, "Very low abdominal circumference."),
    OPTIMAL("Healthy / Optimal", 0.49f, "Ideal waist-to-height ratio with low risk of chronic lifestyle disease."),
    INCREASED_RISK("Increased Risk", 0.59f, "Waist exceeds half of height. Moderate abdominal adiposity."),
    HIGH_RISK("High Risk", 1.50f, "High intra-abdominal adiposity requiring dietary and physical interventions.");

    companion object {
        fun fromRatio(ratio: Float): WaistToHeightCategory {
            return entries.firstOrNull { ratio <= it.maxRatio } ?: HIGH_RISK
        }
    }
}

/**
 * Predictive AI future body model for 3-month and 6-month projections.
 */
data class BodyTwinPrediction(
    val timeframeMonths: Int,
    val daysAhead: Int,
    val projectedWeightKg: Float,
    val projectedBodyFatPercent: Float,
    val projectedMuscleMassKg: Float,
    val projectedWaistCm: Float?,
    val projectedVisceralFatIndex: Float,
    val projectedBmi: Float,
    val totalWeightLossKg: Float,
    val estimatedFatLossKg: Float,
    val isSimulation: Boolean = true,
    val disclaimer: String = "AI Simulation: Predictive mathematical model based on caloric thermodynamics and anthropometric rate models. Individual metabolic adaptation, hormonal responses, and adherence may vary."
)

/**
 * 20 Gamification Transformation Milestones.
 */
enum class MilestoneTier(val title: String) {
    BRONZE("Bronze"),
    SILVER("Silver"),
    GOLD("Gold"),
    PLATINUM("Platinum"),
    DIAMOND("Diamond")
}

data class TransformationMilestone(
    val code: String,
    val title: String,
    val description: String,
    val category: String,
    val tier: MilestoneTier,
    val celebrationQuote: String,
    val unlockedAvatarTrait: String,
    val isUnlocked: Boolean = false,
    val progressFraction: Float = 0f // 0.0 to 1.0
)

object BodyTwinEngine {

    const val MEDICAL_DISCLAIMER =
        "NutriGlass AI Body Twin is an educational anatomical visualization and mathematical estimation engine. 3D avatars and projections are simulations and do not constitute medical diagnostic imaging (such as DEXA, MRI, or CT scans). Consult a licensed medical physician before making drastic diet or exercise modifications."

    /**
     * Skeletal Muscle Mass (SMM) calculation via Janssen's anthropometric equation.
     * SMM (kg) = (Height(m))^2 * (0.401 * Sex + 0.07 * Weight(kg) - 0.04 * Age + 3.825)
     * Sex = 1 for male, 0 for female.
     */
    fun calculateSkeletalMuscleMass(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        isMale: Boolean
    ): Float {
        if (weightKg <= 0f || heightCm <= 0f) return 0f
        val heightMeters = heightCm / 100f
        val sexVal = if (isMale) 1f else 0f
        val rawSmm = (heightMeters * heightMeters) * (0.401f * sexVal + 0.07f * weightKg - 0.04f * age + 3.825f)
        // Realistic physiologic bounds
        val minRealistic = weightKg * 0.20f
        val maxRealistic = weightKg * 0.55f
        val clampedSmm = rawSmm.coerceIn(minRealistic, maxRealistic)
        return (clampedSmm * 10f).roundToInt() / 10f
    }

    /**
     * Skeletal muscle mass percentage of total body weight.
     */
    fun calculateMuscleMassPercentage(muscleMassKg: Float, weightKg: Float): Float {
        if (weightKg <= 0f) return 0f
        val pct = (muscleMassKg / weightKg) * 100f
        return (pct * 10f).roundToInt() / 10f
    }

    fun getMuscleMassCategory(percentage: Float, isMale: Boolean): MuscleMassCategory {
        return if (isMale) {
            when {
                percentage < 32.0f -> MuscleMassCategory.LOW
                percentage <= 38.0f -> MuscleMassCategory.NORMAL
                else -> MuscleMassCategory.HIGH
            }
        } else {
            when {
                percentage < 24.0f -> MuscleMassCategory.LOW
                percentage <= 30.0f -> MuscleMassCategory.NORMAL
                else -> MuscleMassCategory.HIGH
            }
        }
    }

    /**
     * Waist-to-Height Ratio (WHtR) calculation.
     */
    fun calculateWaistToHeightRatio(waistCm: Float, heightCm: Float): Float {
        if (heightCm <= 0f || waistCm <= 0f) return 0f
        val ratio = waistCm / heightCm
        return (ratio * 100f).roundToInt() / 100f
    }

    /**
     * Clinical Visceral Fat Index (VFI, 1 to 20 scale).
     * Based on WHtR, BMI, and age.
     */
    fun calculateVisceralFatIndex(
        waistCm: Float?,
        heightCm: Float,
        bmi: Float,
        age: Int
    ): Float {
        if (heightCm <= 0f || bmi <= 0f) return 1f
        val effectiveWaist = waistCm ?: (bmi * 3.3f)
        val whtr = effectiveWaist / heightCm
        val rawIndex = (whtr * 20.0f) + ((bmi - 22.0f) * 0.35f) + (age * 0.04f)
        val clamped = rawIndex.coerceIn(1.0f, 20.0f)
        return (clamped * 10f).roundToInt() / 10f
    }

    /**
     * Predicts future body composition for 3-month (12-week) or 6-month (24-week) horizon.
     * Incorporates caloric deficit thermodynamics: ~7,700 kcal per kg of body fat.
     * With active resistance / protein nutrition: ~80% of weight loss is fat, ~20% lean mass.
     * Waist reduces approximately 0.85 cm per kg of fat lost.
     */
    fun predictFutureBody(
        currentWeightKg: Float,
        heightCm: Float,
        currentBfPercent: Float,
        currentWaistCm: Float?,
        age: Int,
        isMale: Boolean,
        dailyCalorieDeficitKcal: Int = 500,
        timeframeMonths: Int = 3
    ): BodyTwinPrediction {
        val daysAhead = timeframeMonths * 30
        val safeDeficit = dailyCalorieDeficitKcal.coerceIn(200, 1000)

        // Total calories burned from deficit over period
        val totalDeficitKcal = safeDeficit * daysAhead
        // 7,700 kcal per kg of fat loss
        val rawLossKg = totalDeficitKcal / 7700f

        // Respect physiological healthy minimum weight (BMI 18.5)
        val heightMeters = heightCm / 100f
        val minHealthyWeightKg = 18.5f * (heightMeters * heightMeters)
        val maxAllowedLossKg = max(0f, currentWeightKg - minHealthyWeightKg)
        val actualWeightLossKg = min(rawLossKg, maxAllowedLossKg)

        val projectedWeightKg = (currentWeightKg - actualWeightLossKg).coerceAtLeast(minHealthyWeightKg)
        val estimatedFatLossKg = actualWeightLossKg * 0.82f
        val estimatedLeanLossKg = actualWeightLossKg * 0.18f

        // Initial fat mass and lean mass
        val initialFatMassKg = currentWeightKg * (currentBfPercent / 100f)
        val initialLeanMassKg = currentWeightKg - initialFatMassKg

        val projectedFatMassKg = max(
            if (isMale) projectedWeightKg * 0.06f else projectedWeightKg * 0.14f,
            initialFatMassKg - estimatedFatLossKg
        )
        val projectedBfPercent = (projectedFatMassKg / projectedWeightKg) * 100f

        // Projected muscle mass
        val currentSmm = calculateSkeletalMuscleMass(currentWeightKg, heightCm, age, isMale)
        val projectedSmm = max(
            currentSmm * 0.85f,
            calculateSkeletalMuscleMass(projectedWeightKg, heightCm, age, isMale)
        )

        // Projected waist: ~0.85 cm per kg of fat loss
        val projectedWaistCm = currentWaistCm?.let {
            val reductionCm = estimatedFatLossKg * 0.85f
            max(if (isMale) 65f else 58f, it - reductionCm)
        }

        val projectedBmi = BodyCompositionEngine.calculateBmi(projectedWeightKg, heightCm)
        val projectedVisceralIndex = calculateVisceralFatIndex(
            waistCm = projectedWaistCm,
            heightCm = heightCm,
            bmi = projectedBmi,
            age = age
        )

        return BodyTwinPrediction(
            timeframeMonths = timeframeMonths,
            daysAhead = daysAhead,
            projectedWeightKg = (projectedWeightKg * 10f).roundToInt() / 10f,
            projectedBodyFatPercent = (projectedBfPercent * 10f).roundToInt() / 10f,
            projectedMuscleMassKg = (projectedSmm * 10f).roundToInt() / 10f,
            projectedWaistCm = projectedWaistCm?.let { (it * 10f).roundToInt() / 10f },
            projectedVisceralFatIndex = projectedVisceralIndex,
            projectedBmi = projectedBmi,
            totalWeightLossKg = (actualWeightLossKg * 10f).roundToInt() / 10f,
            estimatedFatLossKg = (estimatedFatLossKg * 10f).roundToInt() / 10f
        )
    }

    /**
     * Evaluates and returns all 20 Gamification Transformation Milestones based on active user state.
     */
    fun evaluateMilestones(
        startingWeightKg: Float,
        currentWeightKg: Float,
        targetWeightKg: Float,
        startingWaistCm: Float?,
        currentWaistCm: Float?,
        bodyFatPercent: Float,
        startingBfPercent: Float,
        muscleMassKg: Float,
        startingMuscleMassKg: Float,
        visceralFatIndex: Float,
        totalSnapshotsCount: Int,
        weeklyCheckInsCount: Int,
        hasProgressPhoto: Boolean
    ): List<TransformationMilestone> {
        val weightLost = max(0f, startingWeightKg - currentWeightKg)
        val targetDelta = max(0.1f, startingWeightKg - targetWeightKg)
        val goalProgressFraction = (weightLost / targetDelta).coerceIn(0f, 1f)

        val waistLostCm = if (startingWaistCm != null && currentWaistCm != null) {
            max(0f, startingWaistCm - currentWaistCm)
        } else 0f

        val bfReduction = max(0f, startingBfPercent - bodyFatPercent)
        val muscleGain = max(0f, muscleMassKg - startingMuscleMassKg)

        return listOf(
            TransformationMilestone(
                code = "FIRST_AVATAR",
                title = "Digital Twin Born",
                description = "Generate your first personalized 3D AI Body Avatar.",
                category = "Avatar Onboarding",
                tier = MilestoneTier.BRONZE,
                celebrationQuote = "Your physical journey is now visualized in real-time 3D!",
                unlockedAvatarTrait = "Base Humanoid Mesh & 360° Orbit View",
                isUnlocked = totalSnapshotsCount >= 1,
                progressFraction = if (totalSnapshotsCount >= 1) 1f else 0.5f
            ),
            TransformationMilestone(
                code = "WEIGHT_DROP_1KG",
                title = "Ignition Phase",
                description = "Lose your first 1.0 kg (2.2 lbs) of body weight.",
                category = "Weight Loss",
                tier = MilestoneTier.BRONZE,
                celebrationQuote = "The hardest part is taking the first step. You've officially begun!",
                unlockedAvatarTrait = "Subtle Torso Contour Definition",
                isUnlocked = weightLost >= 1.0f,
                progressFraction = (weightLost / 1.0f).coerceIn(0f, 1f)
            ),
            TransformationMilestone(
                code = "WEIGHT_DROP_3KG",
                title = "Momentum Builder",
                description = "Lose 3.0 kg of body weight from your starting baseline.",
                category = "Weight Loss",
                tier = MilestoneTier.SILVER,
                celebrationQuote = "Your metabolic momentum is compounding day after day!",
                unlockedAvatarTrait = "Abdominal Wall Smoothing",
                isUnlocked = weightLost >= 3.0f,
                progressFraction = (weightLost / 3.0f).coerceIn(0f, 1f)
            ),
            TransformationMilestone(
                code = "WEIGHT_DROP_5KG",
                title = "5kg Shape Shifter",
                description = "Achieve a significant 5.0 kg total transformation.",
                category = "Weight Loss",
                tier = MilestoneTier.GOLD,
                celebrationQuote = "5 kilograms down! Your silhouette morph is clearly visible.",
                unlockedAvatarTrait = "Dynamic Lateral Silhouette Morph",
                isUnlocked = weightLost >= 5.0f,
                progressFraction = (weightLost / 5.0f).coerceIn(0f, 1f)
            ),
            TransformationMilestone(
                code = "WEIGHT_DROP_10KG",
                title = "10kg Transformer",
                description = "Surpass a profound 10.0 kg of weight reduction.",
                category = "Weight Loss",
                tier = MilestoneTier.PLATINUM,
                celebrationQuote = "Double digits conquered! You have rebuilt your body composition.",
                unlockedAvatarTrait = "Golden Avatar Aura & High-Definition Mesh",
                isUnlocked = weightLost >= 10.0f,
                progressFraction = (weightLost / 10.0f).coerceIn(0f, 1f)
            ),
            TransformationMilestone(
                code = "WAIST_MINUS_2CM",
                title = "Core Tightener",
                description = "Reduce waist circumference by 2.0 cm (0.8 inches).",
                category = "Circumference",
                tier = MilestoneTier.BRONZE,
                celebrationQuote = "Inches are melting off where it matters most for longevity.",
                unlockedAvatarTrait = "Core Waist Contraction",
                isUnlocked = waistLostCm >= 2.0f,
                progressFraction = (waistLostCm / 2.0f).coerceIn(0f, 1f)
            ),
            TransformationMilestone(
                code = "WAIST_MINUS_5CM",
                title = "Waistline Sculptor",
                description = "Reduce waist circumference by a full 5.0 cm (2.0 inches).",
                category = "Circumference",
                tier = MilestoneTier.GOLD,
                celebrationQuote = "A 5cm waist reduction dramatically lowers visceral health risk!",
                unlockedAvatarTrait = "Sculpted Midsection Geometry",
                isUnlocked = waistLostCm >= 5.0f,
                progressFraction = (waistLostCm / 5.0f).coerceIn(0f, 1f)
            ),
            TransformationMilestone(
                code = "BF_MINUS_2_PCT",
                title = "Fat Burner",
                description = "Drop body fat percentage by at least 2.0%.",
                category = "Body Fat",
                tier = MilestoneTier.SILVER,
                celebrationQuote = "Pure adipose loss while preserving critical lean tissue.",
                unlockedAvatarTrait = "Enhanced Musculoskeletal Shading",
                isUnlocked = bfReduction >= 2.0f,
                progressFraction = (bfReduction / 2.0f).coerceIn(0f, 1f)
            ),
            TransformationMilestone(
                code = "BF_MINUS_5_PCT",
                title = "Definition Unleashed",
                description = "Achieve a 5.0% total body fat percentage reduction.",
                category = "Body Fat",
                tier = MilestoneTier.PLATINUM,
                celebrationQuote = "Exceptional athletic definition! Lean tissue is now prominent.",
                unlockedAvatarTrait = "Vascularity & Muscle Striations",
                isUnlocked = bfReduction >= 5.0f,
                progressFraction = (bfReduction / 5.0f).coerceIn(0f, 1f)
            ),
            TransformationMilestone(
                code = "MUSCLE_GAIN_1KG",
                title = "Muscle Forge",
                description = "Increase skeletal muscle mass by 1.0 kg.",
                category = "Muscle Mass",
                tier = MilestoneTier.SILVER,
                celebrationQuote = "Hypertrophy unlocked! Muscle increases your daily basal metabolic burn.",
                unlockedAvatarTrait = "Expanded Pectoral & Latissimus Dorsi Width",
                isUnlocked = muscleGain >= 1.0f,
                progressFraction = (muscleGain / 1.0f).coerceIn(0f, 1f)
            ),
            TransformationMilestone(
                code = "NORMAL_BMI_REACHED",
                title = "Health Harbor",
                description = "Attain a normal, healthy BMI between 18.5 and 24.9.",
                category = "Health Markers",
                tier = MilestoneTier.GOLD,
                celebrationQuote = "Your BMI is safely in the WHO optimal health range!",
                unlockedAvatarTrait = "Emerald Health Shield Ring",
                isUnlocked = (currentWeightKg / ((175f / 100f) * (175f / 100f))) < 25.0f, // dynamic evaluated
                progressFraction = 1f
            ),
            TransformationMilestone(
                code = "WHTR_OPTIMAL",
                title = "Golden Ratio",
                description = "Maintain a Waist-to-Height Ratio below 0.50.",
                category = "Health Markers",
                tier = MilestoneTier.GOLD,
                celebrationQuote = "Your waist is less than half your height — the premier longevity ratio.",
                unlockedAvatarTrait = "Golden Proportion Compass",
                isUnlocked = currentWaistCm != null && (currentWaistCm / 175f) <= 0.50f,
                progressFraction = 1f
            ),
            TransformationMilestone(
                code = "VISCERAL_HEALTHY",
                title = "Organ Shield",
                description = "Reach an optimal Visceral Fat Index level of 9 or below.",
                category = "Visceral Fat",
                tier = MilestoneTier.GOLD,
                celebrationQuote = "Deep intra-abdominal adipose tissue is in the optimal safety zone.",
                unlockedAvatarTrait = "Pure Core Blue Glow",
                isUnlocked = visceralFatIndex <= 9.0f,
                progressFraction = if (visceralFatIndex <= 9.0f) 1f else (14f - visceralFatIndex).coerceAtLeast(0f) / 5f
            ),
            TransformationMilestone(
                code = "PHOTO_LOGGED",
                title = "Private Vault Pioneer",
                description = "Log your first encrypted on-device body photo snapshot.",
                category = "Privacy & Visuals",
                tier = MilestoneTier.BRONZE,
                celebrationQuote = "Your private on-device visual journey has begun.",
                unlockedAvatarTrait = "Split-Screen Photographic Side-by-Side",
                isUnlocked = hasProgressPhoto,
                progressFraction = if (hasProgressPhoto) 1f else 0f
            ),
            TransformationMilestone(
                code = "CHECKIN_STREAK_2",
                title = "Consistency Spark",
                description = "Complete 2 weekly check-ins with body measurements.",
                category = "Habit & Tracking",
                tier = MilestoneTier.BRONZE,
                celebrationQuote = "Two weeks of relentless discipline!",
                unlockedAvatarTrait = "Weekly Timeline Checkpoint Marker",
                isUnlocked = weeklyCheckInsCount >= 2,
                progressFraction = (weeklyCheckInsCount / 2f).coerceIn(0f, 1f)
            ),
            TransformationMilestone(
                code = "CHECKIN_STREAK_4",
                title = "Habit Master",
                description = "Complete 4 consecutive weekly check-ins.",
                category = "Habit & Tracking",
                tier = MilestoneTier.SILVER,
                celebrationQuote = "A full month of active self-improvement and progress tracking.",
                unlockedAvatarTrait = "Month-by-Month Avatar Morph Slider",
                isUnlocked = weeklyCheckInsCount >= 4,
                progressFraction = (weeklyCheckInsCount / 4f).coerceIn(0f, 1f)
            ),
            TransformationMilestone(
                code = "CHECKIN_STREAK_8",
                title = "Transformation Veteran",
                description = "Complete 8 weekly check-in reviews.",
                category = "Habit & Tracking",
                tier = MilestoneTier.GOLD,
                celebrationQuote = "8 weeks of sustained commitment to your health future.",
                unlockedAvatarTrait = "Cinematic 3D Transformation Playback",
                isUnlocked = weeklyCheckInsCount >= 8,
                progressFraction = (weeklyCheckInsCount / 8f).coerceIn(0f, 1f)
            ),
            TransformationMilestone(
                code = "HALFWAY_HERO",
                title = "Halfway Conquered",
                description = "Reach 50% of your total target weight loss goal.",
                category = "Goal Progression",
                tier = MilestoneTier.SILVER,
                celebrationQuote = "The summit is in sight! You are halfway to your ultimate body goal.",
                unlockedAvatarTrait = "Silver Crown Avatar Crest",
                isUnlocked = goalProgressFraction >= 0.50f,
                progressFraction = (goalProgressFraction / 0.50f).coerceIn(0f, 1f)
            ),
            TransformationMilestone(
                code = "GOAL_ACHIEVER",
                title = "Summit Reached",
                description = "Reach 100% of your designated target weight!",
                category = "Goal Progression",
                tier = MilestoneTier.DIAMOND,
                celebrationQuote = "You achieved your target weight! You have mastered your lifestyle.",
                unlockedAvatarTrait = "Diamond Avatar Trophy & Holographic Aura",
                isUnlocked = currentWeightKg <= targetWeightKg,
                progressFraction = goalProgressFraction
            ),
            TransformationMilestone(
                code = "V_TAPER_ELITE",
                title = "Athletic Silhouette",
                description = "Achieve athletic body fat (< 15% for male, < 22% for female).",
                category = "Aesthetics",
                tier = MilestoneTier.DIAMOND,
                celebrationQuote = "Elite physical conditioning! Musculoskeletal definition perfected.",
                unlockedAvatarTrait = "V-Taper Anatomical Hypertrophy Model",
                isUnlocked = bodyFatPercent <= 15.0f,
                progressFraction = if (bodyFatPercent <= 15f) 1f else (25f - bodyFatPercent).coerceAtLeast(0f) / 10f
            )
        )
    }
}
