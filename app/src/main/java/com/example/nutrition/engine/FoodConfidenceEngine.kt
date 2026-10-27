package com.example.nutrition.engine

import com.example.vision.portion.PortionConfidence

data class ConfidenceDiagnostic(
    val classificationConfidence: Float,
    val detectionConfidence: Float,
    val portionConfidence: PortionConfidence,
    val isAmbiguous: Boolean,
    val isLowLightingOrBlurry: Boolean,
    val requiresUserConfirmation: Boolean,
    val confirmationReasons: List<String>
)

interface FoodConfidenceEngine {
    fun evaluateConfidence(
        classificationScore: Float,
        secondTopClassificationScore: Float?,
        detectionScore: Float,
        portionConfidence: PortionConfidence,
        imageBrightness: Float = 0.5f,
        isSupportedClass: Boolean = true
    ): ConfidenceDiagnostic
}

class StandardFoodConfidenceEngine : FoodConfidenceEngine {

    override fun evaluateConfidence(
        classificationScore: Float,
        secondTopClassificationScore: Float?,
        detectionScore: Float,
        portionConfidence: PortionConfidence,
        imageBrightness: Float,
        isSupportedClass: Boolean
    ): ConfidenceDiagnostic {
        val reasons = mutableListOf<String>()

        if (!isSupportedClass) {
            reasons.add("This item appears to be outside our recognized food library.")
        }

        val isAmbiguous = secondTopClassificationScore != null &&
                (classificationScore - secondTopClassificationScore) < 0.15f &&
                classificationScore < 0.85f

        if (isAmbiguous) {
            reasons.add("Visual appearance is ambiguous between multiple similar dishes.")
        }

        if (classificationScore < 0.70f) {
            reasons.add("Low model confidence (${(classificationScore * 100).toInt()}%). Please verify food name.")
        }

        if (detectionScore < 0.60f) {
            reasons.add("Food boundary or dish separation is partially obscured.")
        }

        if (portionConfidence == PortionConfidence.UNCERTAIN_NEEDS_CONFIRMATION) {
            reasons.add("Portion depth cannot be verified from a 2D photograph alone. Please confirm serving size.")
        }

        val isPoorLighting = imageBrightness < 0.15f || imageBrightness > 0.95f
        if (isPoorLighting) {
            reasons.add("Challenging lighting conditions detected. Try scanning under even light.")
        }

        val needsConfirmation = reasons.isNotEmpty() || portionConfidence == PortionConfidence.UNCERTAIN_NEEDS_CONFIRMATION

        return ConfidenceDiagnostic(
            classificationConfidence = classificationScore,
            detectionConfidence = detectionScore,
            portionConfidence = portionConfidence,
            isAmbiguous = isAmbiguous,
            isLowLightingOrBlurry = isPoorLighting,
            requiresUserConfirmation = needsConfirmation,
            confirmationReasons = reasons
        )
    }
}
