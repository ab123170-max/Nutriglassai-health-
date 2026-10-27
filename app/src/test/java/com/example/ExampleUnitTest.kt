package com.example

import com.example.domain.NutritionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testBmrCalculation() {
        // Mifflin-St Jeor:
        // Male: 10 * 70 + 6.25 * 175 - 5 * 25 + 5 = 700 + 1093.75 - 125 + 5 = 1673.75
        val maleBmr = NutritionEngine.calculateBmr(weightKg = 70f, heightCm = 175f, age = 25, gender = "Male")
        assertEquals(1673.75f, maleBmr, 0.5f)

        // Female: 10 * 60 + 6.25 * 165 - 5 * 30 - 161 = 600 + 1031.25 - 150 - 161 = 1320.25
        val femaleBmr = NutritionEngine.calculateBmr(weightKg = 60f, heightCm = 165f, age = 30, gender = "Female")
        assertEquals(1320.25f, femaleBmr, 0.5f)
    }

    @Test
    fun testTdeeAndCalorieTarget() {
        val bmr = 1600f
        val tdee = NutritionEngine.calculateTdee(bmr, "Moderate") // 1.55 multiplier = 2480
        assertEquals(2480f, tdee, 1f)

        // Lose weight: -450 kcal
        val target = NutritionEngine.calculateCalorieTarget(tdee, "Lose Weight", "Male")
        assertEquals(2030, target)

        // Minimum floor safety check for female (min 1200)
        val lowTarget = NutritionEngine.calculateCalorieTarget(1000f, "Lose Weight", "Female")
        assertTrue(lowTarget >= 1200)
    }

    @Test
    fun testBmiCalculation() {
        // 70kg, 175cm -> 70 / (1.75 * 1.75) = 22.86 -> Normal weight
        val bmiResult = NutritionEngine.calculateBmi(70f, 175f)
        assertEquals(22.9f, bmiResult.bmi, 0.1f)
        assertEquals("Normal weight", bmiResult.category)
    }

    @Test
    fun testMacroDistribution() {
        val macros = NutritionEngine.calculateMacroDistribution(calorieTarget = 2000, weightKg = 70f)
        assertTrue(macros.proteinGrams >= 100f)
        assertTrue(macros.carbsGrams >= 150f)
        assertTrue(macros.fatGrams >= 40f)
        assertTrue(macros.fiberGrams >= 20f)
    }

    @Test
    fun testWeightUnitConversions() {
        // 70 kg in pounds is ~154.32 lb
        val lb = com.example.domain.UnitConversionEngine.convertWeight(70f, com.example.domain.WeightUnit.KG, com.example.domain.WeightUnit.LB)
        assertEquals(154.32f, lb, 0.5f)

        // 140 lb back to kg is ~63.5 kg
        val kg = com.example.domain.UnitConversionEngine.convertWeight(140f, com.example.domain.WeightUnit.LB, com.example.domain.WeightUnit.KG)
        assertEquals(63.5f, kg, 0.5f)

        // 1 stone is 14 lb = 6.35029 kg
        val st = com.example.domain.UnitConversionEngine.convertWeight(63.5f, com.example.domain.WeightUnit.KG, com.example.domain.WeightUnit.ST)
        assertEquals(10f, st, 0.2f)

        // Grams: 70 kg = 70000 g
        val g = com.example.domain.UnitConversionEngine.convertWeight(70f, com.example.domain.WeightUnit.KG, com.example.domain.WeightUnit.G)
        assertEquals(70000f, g, 1f)
    }

    @Test
    fun testHeightUnitConversions() {
        // 175 cm to feet and inches: 5 ft 9 in
        val formattedFtIn = com.example.domain.UnitConversionEngine.formatHeight(175f, com.example.domain.HeightUnit.FT_IN)
        assertTrue(formattedFtIn.contains("5'") && formattedFtIn.contains("9\""))

        // 175 cm to meters: 1.75 m
        val meters = com.example.domain.UnitConversionEngine.convertHeight(175f, com.example.domain.HeightUnit.CM, com.example.domain.HeightUnit.M)
        assertEquals(1.75f, meters, 0.01f)

        // 6 feet to cm: 6 * 12 * 2.54 = 182.88 cm
        val cmFromFt = com.example.domain.UnitConversionEngine.ftInToCm(6, 0)
        assertEquals(182.88f, cmFromFt, 0.5f)
    }

    @Test
    fun testWaterUnitConversions() {
        // 1000 ml with standard 250ml glass = 4 glasses
        val glasses = com.example.domain.UnitConversionEngine.convertWater(
            valueMl = 1000f,
            to = com.example.domain.WaterUnit.GLASS,
            glassSizeMl = 250
        )
        assertEquals(4f, glasses, 0.01f)

        // 1500 ml with 500ml bottle = 3 bottles
        val bottles = com.example.domain.UnitConversionEngine.convertWater(
            valueMl = 1500f,
            to = com.example.domain.WaterUnit.BOTTLE,
            bottleSizeMl = 500
        )
        assertEquals(3f, bottles, 0.01f)

        // 1.5 Liters = 1500 ml
        val mlFromL = com.example.domain.UnitConversionEngine.lToMl(1.5f)
        assertEquals(1500f, mlFromL, 0.1f)
    }

    @Test
    fun testDistanceUnitConversions() {
        // 5 km to miles: ~3.10686 mi
        val miles = com.example.domain.UnitConversionEngine.convertDistance(
            valueKm = 5f,
            to = com.example.domain.DistanceUnit.MI
        )
        assertEquals(3.11f, miles, 0.05f)

        // 5 km to meters: 5000 m
        val meters = com.example.domain.UnitConversionEngine.convertDistance(
            valueKm = 5f,
            to = com.example.domain.DistanceUnit.M
        )
        assertEquals(5000f, meters, 0.1f)
    }

    @Test
    fun testFoodDensityAndPortionConversions() {
        // Cooked rice: 1 cup is ~160g (density aware)
        val riceCupGrams = com.example.domain.UnitConversionEngine.convertFoodToGrams(
            foodName = "Cooked Basmati Rice",
            quantity = 1f,
            unit = com.example.domain.FoodUnit.CUP
        )
        assertEquals(160f, riceCupGrams, 1f)

        // Cooking oil: 1 tbsp oil is ~13.8g (15 ml * 0.92 g/ml)
        val oilTbspGrams = com.example.domain.UnitConversionEngine.convertFoodToGrams(
            foodName = "Mustard Oil",
            quantity = 1f,
            unit = com.example.domain.FoodUnit.TBSP
        )
        assertEquals(13.8f, oilTbspGrams, 0.5f)

        // Roti: 2 rotis = 80g
        val rotiGrams = com.example.domain.UnitConversionEngine.convertFoodToGrams(
            foodName = "Roti",
            quantity = 2f,
            unit = com.example.domain.FoodUnit.ROTI
        )
        assertEquals(80f, rotiGrams, 0.1f)

        // Momo: 6 pieces = 150g (25g each)
        val momoGrams = com.example.domain.UnitConversionEngine.convertFoodToGrams(
            foodName = "Chicken Momo",
            quantity = 6f,
            unit = com.example.domain.FoodUnit.MOMO_PIECE
        )
        assertEquals(150f, momoGrams, 0.1f)

        // Dal: 1 bowl dal = 200g
        val dalGrams = com.example.domain.UnitConversionEngine.convertFoodToGrams(
            foodName = "Yellow Dal",
            quantity = 1f,
            unit = com.example.domain.FoodUnit.BOWL
        )
        assertEquals(200f, dalGrams, 0.1f)
    }

    @Test
    fun testNutritionRecalculationForPortion() {
        // Food item: 130 kcal per 100g (cooked rice)
        // 1 cup = 160g -> 130 * 1.6 = 208 kcal
        val calculated = com.example.domain.UnitConversionEngine.calculateNutrition(
            caloriesPer100g = 130f,
            proteinPer100g = 2.7f,
            carbsPer100g = 28f,
            fatPer100g = 0.3f,
            fiberPer100g = 0.4f,
            sugarPer100g = 0.1f,
            sodiumPer100g = 1f,
            foodName = "Cooked Rice",
            quantity = 1f,
            unit = com.example.domain.FoodUnit.CUP
        )
        assertEquals(208, calculated.calories)
        assertEquals(160f, calculated.normalizedGrams, 0.1f)
    }

    @Test
    fun testGeminiConfigAndModels() {
        assertEquals("gemini-2.5-flash", com.example.data.network.GeminiConfig.PRIMARY_MODEL)
        assertEquals("gemini-1.5-flash", com.example.data.network.GeminiConfig.FALLBACK_MODEL)
        assertTrue(com.example.data.network.GeminiConfig.SUPPORTED_MODELS.contains("gemini-2.5-flash"))
        assertTrue(com.example.data.network.GeminiConfig.SUPPORTED_MODELS.contains("gemini-1.5-flash"))

        val url = com.example.data.network.GeminiConfig.getGenerateContentUrl("gemini-2.5-flash", "test-key-123")
        assertTrue(url.contains("gemini-2.5-flash:generateContent?key=test-key-123"))
    }

    @Test
    fun testGeminiErrorClassification() {
        val missingKeyEx = com.example.data.network.GeminiAnalysisException(
            message = "Gemini API key is not configured.",
            errorType = com.example.data.network.GeminiErrorType.API_KEY_MISSING
        )
        assertEquals(com.example.data.network.GeminiErrorType.API_KEY_MISSING, missingKeyEx.errorType)

        val model404Ex = com.example.data.network.GeminiAnalysisException(
            message = "Model not found",
            errorType = com.example.data.network.GeminiErrorType.MODEL_UNAVAILABLE,
            httpCode = 404
        )
        assertEquals(404, model404Ex.httpCode)
        assertEquals(com.example.data.network.GeminiErrorType.MODEL_UNAVAILABLE, model404Ex.errorType)

        val rateLimitEx = com.example.data.network.GeminiAnalysisException(
            message = "Quota exceeded",
            errorType = com.example.data.network.GeminiErrorType.QUOTA_EXCEEDED,
            httpCode = 429
        )
        assertEquals(429, rateLimitEx.httpCode)
        assertEquals(com.example.data.network.GeminiErrorType.QUOTA_EXCEEDED, rateLimitEx.errorType)
    }

    @Test
    fun testLocalMultiEstimateStructureAndAllergens() {
        val fallbackResult = com.example.data.network.GeminiFoodService.generateLocalMultiEstimate(null)
        assertTrue(fallbackResult.items.size >= 3)
        assertTrue(fallbackResult.totalCalories > 400)
        assertTrue(fallbackResult.totalProtein > 10f)

        // Verify allergen detection exists in items
        val allAllergens = fallbackResult.items.flatMap { it.allergens }
        assertTrue(allAllergens.contains("Mustard"))

        // Verify disclaimer and uncertainty notes exist
        assertTrue(fallbackResult.disclaimer.isNotBlank())
        assertTrue(fallbackResult.uncertaintyNotes.isNotBlank())
    }
}

