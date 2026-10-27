package com.example

import com.example.domain.NutritionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testBmrCalculation() {
        val maleBmr = NutritionEngine.calculateBmr(weightKg = 70f, heightCm = 175f, age = 25, gender = "Male")
        assertEquals(1673.75f, maleBmr, 0.5f)
    }

    @Test
    fun testMacroDistribution() {
        val macros = NutritionEngine.calculateMacroDistribution(calorieTarget = 2000, weightKg = 70f)
        assertTrue(macros.proteinGrams >= 100f)
    }
}
