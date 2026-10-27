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
}
