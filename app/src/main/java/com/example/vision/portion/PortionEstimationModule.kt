package com.example.vision.portion

import com.example.vision.detection.FoodRegion
import kotlin.math.roundToInt

enum class PortionEstimationMethod {
    WEIGHED_SCALE,                  // Physical scale measurement (measured exact)
    USER_ENTERED,                   // User typed exact grams/serving
    CALIBRATED_REFERENCE_OBJECT,    // Plate size / coin / card visual reference
    PLATE_RELATIVE_VOLUME,          // 2D surface area heuristic * standard depth
    PIECE_COUNT_ESTIMATE,           // Counted foods (e.g. 8 momos, 2 eggs)
    DEPTH_SENSOR_ESTIMATE,          // Device hardware depth map (TOF / ARCore)
    DEFAULT_SERVING_HEURISTIC       // Statistical standard serving size fallback
}

enum class PortionConfidence {
    MEASURED_EXACT,                 // Scale or validated physical count (±0%)
    HIGH_CONFIDENCE,                // Known piece count or reference calibrated (±10%)
    MODERATE_ESTIMATE,              // Plate-relative with clear boundaries (±20%)
    UNCERTAIN_NEEDS_CONFIRMATION    // 2D photo with unknown depth/hidden layers (±35%)
}

enum class ReferenceType {
    STANDARD_DINNER_PLATE_26CM,
    SIDE_PLATE_18CM,
    BOWL_14CM,
    COIN_REFERENCE,
    CREDIT_CARD_REFERENCE,
    NO_REFERENCE
}

data class PortionCalibration(
    val referenceType: ReferenceType = ReferenceType.STANDARD_DINNER_PLATE_26CM,
    val referenceRealSizeCm: Float = 26f,
    val cameraAngleDegrees: Float = 45f,
    val isDepthSensorAvailable: Boolean = false
)

data class PortionEstimate(
    val estimatedGrams: Float,
    val lowerBoundGrams: Float,
    val upperBoundGrams: Float,
    val quantity: Float,
    val unit: String,
    val method: PortionEstimationMethod,
    val confidence: PortionConfidence,
    val requiresUserConfirmation: Boolean,
    val explanation: String
)

interface PortionEstimationRepository {
    fun getCalibration(): PortionCalibration
    fun updateCalibration(calibration: PortionCalibration)
    fun getFoodDensity(classId: String): Float
    fun getPieceWeight(classId: String): Float?
}

class StandardPortionEstimationRepository : PortionEstimationRepository {
    private var currentCalibration = PortionCalibration()

    private val densities = mapOf(
        "nep_steamed_rice" to 0.80f,
        "nep_yellow_dal" to 1.02f,
        "nep_black_dal" to 1.05f,
        "nep_chicken_curry" to 0.95f,
        "nep_veg_curry" to 0.85f,
        "nep_chowmein" to 0.70f,
        "nep_chatpate" to 0.45f,
        "nep_dhido" to 1.10f,
        "nep_aloo_tama" to 1.00f,
        "nep_thukpa" to 0.98f
    )

    private val pieceWeights = mapOf(
        "nep_momo_steamed" to 22.5f,
        "nep_sel_roti" to 60.0f,
        "nep_roti_chapati" to 40.0f,
        "nep_samosa" to 90.0f,
        "nep_panipuri" to 25.0f,
        "gen_boiled_egg" to 50.0f,
        "gen_apple" to 150.0f,
        "gen_banana" to 120.0f
    )

    override fun getCalibration(): PortionCalibration = currentCalibration

    override fun updateCalibration(calibration: PortionCalibration) {
        currentCalibration = calibration
    }

    override fun getFoodDensity(classId: String): Float = densities[classId] ?: 0.85f

    override fun getPieceWeight(classId: String): Float? = pieceWeights[classId]
}

interface PortionEstimationEngine {
    fun estimatePortion(
        region: FoodRegion?,
        classId: String,
        userQuantityOverride: Float? = null,
        userUnitOverride: String? = null
    ): PortionEstimate
}

class StandardPortionEstimator(
    private val repository: PortionEstimationRepository = StandardPortionEstimationRepository()
) : PortionEstimationEngine {

    override fun estimatePortion(
        region: FoodRegion?,
        classId: String,
        userQuantityOverride: Float?,
        userUnitOverride: String?
    ): PortionEstimate {
        if (userQuantityOverride != null && userQuantityOverride > 0f) {
            val unit = userUnitOverride ?: "serving"
            val grams = convertToGrams(userQuantityOverride, unit, classId)
            return PortionEstimate(
                estimatedGrams = grams,
                lowerBoundGrams = grams,
                upperBoundGrams = grams,
                quantity = userQuantityOverride,
                unit = unit,
                method = PortionEstimationMethod.USER_ENTERED,
                confidence = PortionConfidence.MEASURED_EXACT,
                requiresUserConfirmation = false,
                explanation = "User confirmed $userQuantityOverride $unit (~${grams.roundToInt()}g)"
            )
        }

        val pieceWeight = repository.getPieceWeight(classId)
        if (pieceWeight != null) {
            val pieces = when (classId) {
                "nep_momo_steamed" -> 8f
                "nep_panipuri" -> 6f
                "nep_sel_roti" -> 2f
                "nep_roti_chapati" -> 2f
                else -> 1f
            }
            val totalGrams = pieces * pieceWeight
            return PortionEstimate(
                estimatedGrams = totalGrams,
                lowerBoundGrams = (totalGrams * 0.90f),
                upperBoundGrams = (totalGrams * 1.10f),
                quantity = pieces,
                unit = "piece",
                method = PortionEstimationMethod.PIECE_COUNT_ESTIMATE,
                confidence = PortionConfidence.HIGH_CONFIDENCE,
                requiresUserConfirmation = false,
                explanation = "Counted estimated $pieces pieces (~${totalGrams.roundToInt()}g)"
            )
        }

        val cal = repository.getCalibration()
        val areaFraction = region?.relativeAreaFraction ?: 0.35f
        val plateRadiusCm = cal.referenceRealSizeCm / 2f
        val plateAreaCm2 = Math.PI * plateRadiusCm * plateRadiusCm
        val foodAreaCm2 = (plateAreaCm2 * areaFraction).coerceIn(20.0, 450.0)
        val estimatedDepthCm = 2.5f
        val estimatedVolumeCm3 = (foodAreaCm2 * estimatedDepthCm).toFloat()
        val density = repository.getFoodDensity(classId)
        val estimatedGrams = (estimatedVolumeCm3 * density).coerceIn(50f, 600f)

        val lowerBound = estimatedGrams * 0.75f
        val upperBound = estimatedGrams * 1.25f

        return PortionEstimate(
            estimatedGrams = estimatedGrams,
            lowerBoundGrams = lowerBound,
            upperBoundGrams = upperBound,
            quantity = 1f,
            unit = "serving",
            method = PortionEstimationMethod.PLATE_RELATIVE_VOLUME,
            confidence = PortionConfidence.UNCERTAIN_NEEDS_CONFIRMATION,
            requiresUserConfirmation = true,
            explanation = "Visual 2D area estimate (~${estimatedGrams.roundToInt()}g). Please verify serving size."
        )
    }

    private fun convertToGrams(quantity: Float, unit: String, classId: String): Float {
        val density = repository.getFoodDensity(classId)
        return when (unit.lowercase()) {
            "g", "gram", "grams" -> quantity
            "kg" -> quantity * 1000f
            "mg" -> quantity / 1000f
            "oz" -> quantity * 28.3495f
            "lb" -> quantity * 453.592f
            "piece", "pc" -> quantity * (repository.getPieceWeight(classId) ?: 100f)
            "cup" -> quantity * 240f * density
            "bowl", "kachaura" -> quantity * 200f * density
            "plate", "thali" -> quantity * 350f
            "tbsp" -> quantity * 15f * density
            "tsp" -> quantity * 5f * density
            "mana" -> quantity * 380f
            "muthi" -> quantity * 45f
            else -> quantity * 150f
        }
    }
}
