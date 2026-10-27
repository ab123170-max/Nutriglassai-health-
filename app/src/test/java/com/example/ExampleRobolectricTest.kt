package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NutriGlass AI", appName)
  }

  // --- Tests for FoodDetectionModule & NMS ---
  @Test
  fun testBoundingBoxIoUAndNms() {
    val detector = com.example.vision.detection.ChromaticSaliencyFoodDetector()
    val boxA = com.example.vision.detection.BoundingBox(0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "rice", "Rice")
    val boxB = com.example.vision.detection.BoundingBox(0.1f, 0.1f, 0.5f, 0.5f, 0.8f, "rice", "Rice")
    val boxC = com.example.vision.detection.BoundingBox(0.6f, 0.6f, 0.9f, 0.9f, 0.85f, "dal", "Dal")

    val iouSame = detector.computeIoU(boxA, boxB)
    assertEquals(1.0f, iouSame, 0.01f)

    val iouDisjoint = detector.computeIoU(boxA, boxC)
    assertEquals(0.0f, iouDisjoint, 0.01f)

    val nmsResult = detector.applyNonMaxSuppression(listOf(boxA, boxB, boxC), iouThreshold = 0.45f)
    assertEquals(2, nmsResult.size)
    assertTrue(nmsResult.contains(boxA))
    assertTrue(nmsResult.contains(boxC))
    assertFalse(nmsResult.contains(boxB))
  }

  @Test
  fun testBoundingBoxCoordinateMapping() {
    val box = com.example.vision.detection.BoundingBox(0.1f, 0.2f, 0.5f, 0.8f, 0.9f, "dish", "Dish")
    val imageCoords = box.toImageCoordinates(1000, 2000)
    assertEquals(100f, imageCoords.left, 0.1f)
    assertEquals(400f, imageCoords.top, 0.1f)
    assertEquals(500f, imageCoords.right, 0.1f)
    assertEquals(1600f, imageCoords.bottom, 0.1f)
  }

  // --- Tests for FoodClassificationModule & Taxonomy ---
  @Test
  fun testFoodClassificationTaxonomyAndNepaliDishes() {
    val classifier = com.example.vision.classification.HybridFoodClassifier()
    val taxonomy = classifier.getSupportedTaxonomy()
    assertTrue(taxonomy.size >= 20)

    val englishNames = taxonomy.map { it.englishName }
    assertTrue(englishNames.contains("Dal Bhat Set"))
    assertTrue(englishNames.contains("Steamed Momo"))
    assertTrue(englishNames.contains("Sel Roti"))
    assertTrue(englishNames.contains("Chatpate"))
    assertTrue(englishNames.contains("Panipuri (with spiced water)"))
    assertTrue(englishNames.contains("Dhido (Millet / Buckwheat)"))

    val nepaliNames = taxonomy.map { it.nepaliName }
    assertTrue(nepaliNames.contains("दाल भात"))
    assertTrue(nepaliNames.contains("ममः (८ पिस)"))
    assertTrue(nepaliNames.contains("सेल रोटी"))
  }

  // --- Tests for PortionEstimationModule ---
  @Test
  fun testPortionEstimationPieceCountAndUncertaintyBounds() {
    val estimator = com.example.vision.portion.StandardPortionEstimator()

    val momoEstimate = estimator.estimatePortion(null, "nep_momo_steamed")
    assertEquals(180f, momoEstimate.estimatedGrams, 0.5f)
    assertEquals(com.example.vision.portion.PortionEstimationMethod.PIECE_COUNT_ESTIMATE, momoEstimate.method)
    assertEquals(com.example.vision.portion.PortionConfidence.HIGH_CONFIDENCE, momoEstimate.confidence)
    assertFalse(momoEstimate.requiresUserConfirmation)
    assertTrue(momoEstimate.lowerBoundGrams < momoEstimate.estimatedGrams)
    assertTrue(momoEstimate.upperBoundGrams > momoEstimate.estimatedGrams)

    val riceEstimate = estimator.estimatePortion(null, "nep_steamed_rice")
    assertTrue(riceEstimate.requiresUserConfirmation)
    assertEquals(com.example.vision.portion.PortionConfidence.UNCERTAIN_NEEDS_CONFIRMATION, riceEstimate.confidence)
    assertTrue(riceEstimate.explanation.contains("verify"))
  }

  @Test
  fun testPortionEstimationUserOverrideAuthority() {
    val estimator = com.example.vision.portion.StandardPortionEstimator()
    val userOverride = estimator.estimatePortion(
      region = null,
      classId = "nep_steamed_rice",
      userQuantityOverride = 250f,
      userUnitOverride = "g"
    )
    assertEquals(250f, userOverride.estimatedGrams, 0.01f)
    assertEquals(com.example.vision.portion.PortionEstimationMethod.USER_ENTERED, userOverride.method)
    assertEquals(com.example.vision.portion.PortionConfidence.MEASURED_EXACT, userOverride.confidence)
    assertFalse(userOverride.requiresUserConfirmation)
  }

  // --- Tests for CalorieCalculationEngine & NutritionAnalysisEngine ---
  @Test
  fun testCalorieCalculationAtwaterAndKjConversion() {
    val engine = com.example.nutrition.engine.StandardCalorieCalculationEngine()
    val breakdown = engine.calculateNutrients(
      baseCaloriesPer100g = 165f,
      baseProteinPer100g = 10f,
      baseCarbsPer100g = 20f,
      baseFatPer100g = 5f,
      portionGrams = 100f,
      cookingMethod = com.example.nutrition.engine.CookingMethod.STEAMED
    )
    assertEquals(165, breakdown.energyKcal)
    assertEquals(690, breakdown.energyKj)
    assertEquals(10f, breakdown.proteinGrams, 0.1f)
    assertEquals(20f, breakdown.carbsGrams, 0.1f)
    assertEquals(5f, breakdown.fatGrams, 0.1f)
  }

  @Test
  fun testCookingMethodOilAdjustment() {
    val engine = com.example.nutrition.engine.StandardCalorieCalculationEngine()
    val steamed = engine.calculateNutrients(100f, 5f, 20f, 0f, portionGrams = 100f, cookingMethod = com.example.nutrition.engine.CookingMethod.STEAMED)
    val deepFried = engine.calculateNutrients(100f, 5f, 20f, 0f, portionGrams = 100f, cookingMethod = com.example.nutrition.engine.CookingMethod.DEEP_FRIED)
    assertTrue(deepFried.energyKcal > steamed.energyKcal)
    assertTrue(deepFried.fatGrams > steamed.fatGrams)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testCalorieEngineRejectsNegativeGrams() {
    val engine = com.example.nutrition.engine.StandardCalorieCalculationEngine()
    engine.calculateNutrients(100f, 5f, 20f, 0f, portionGrams = -50f)
  }

  @Test
  fun testNutritionAnalysisMacroSplitAndSodiumFlag() {
    val analysisEngine = com.example.nutrition.engine.StandardNutritionAnalysisEngine()
    val highSodiumMeal = com.example.nutrition.engine.NutrientBreakdown(
      energyKcal = 500,
      energyKj = 2092,
      proteinGrams = 25f,
      carbsGrams = 50f,
      fatGrams = 15f,
      sodiumMg = 950f,
      fiberGrams = 7f
    )
    val insight = analysisEngine.analyzeMealNutrition(highSodiumMeal)
    assertTrue(insight.isHighSodium)
    assertTrue(insight.isHighFiber)
    assertTrue(insight.recommendations.any { it.contains("High sodium") })
  }

  // --- Tests for MealAnalysisEngine ---
  @Test
  fun testMealCompositionAndDeduplication() {
    val mealEngine = com.example.nutrition.engine.StandardMealAnalysisEngine()
    val item1 = com.example.nutrition.engine.MealComponent(
      foodName = "Cooked Rice",
      quantity = 1f,
      unit = "bowl",
      grams = 180f,
      nutrients = com.example.nutrition.engine.NutrientBreakdown(234, 979, 4.8f, 50.7f, 0.5f)
    )
    val item2 = com.example.nutrition.engine.MealComponent(
      foodName = "Cooked Rice",
      quantity = 1f,
      unit = "bowl",
      grams = 180f,
      nutrients = com.example.nutrition.engine.NutrientBreakdown(234, 979, 4.8f, 50.7f, 0.5f)
    )
    val duplicates = mealEngine.identifyDuplicates(listOf(item1, item2))
    assertEquals(1, duplicates.size)
    assertEquals("cooked rice", duplicates.first())

    val merged = mealEngine.mergeDuplicateItems(listOf(item1, item2))
    assertEquals(1, merged.size)
    assertEquals(360f, merged.first().grams, 0.1f)
    assertEquals(468, merged.first().nutrients.energyKcal)
  }

  // --- Tests for FoodConfidenceEngine ---
  @Test
  fun testFoodConfidenceEngineDiagnosticRules() {
    val confidenceEngine = com.example.nutrition.engine.StandardFoodConfidenceEngine()

    val highConf = confidenceEngine.evaluateConfidence(
      classificationScore = 0.94f,
      secondTopClassificationScore = 0.60f,
      detectionScore = 0.92f,
      portionConfidence = com.example.vision.portion.PortionConfidence.HIGH_CONFIDENCE
    )
    assertFalse(highConf.isAmbiguous)
    assertFalse(highConf.requiresUserConfirmation)

    val ambiguous = confidenceEngine.evaluateConfidence(
      classificationScore = 0.76f,
      secondTopClassificationScore = 0.70f,
      detectionScore = 0.85f,
      portionConfidence = com.example.vision.portion.PortionConfidence.MODERATE_ESTIMATE
    )
    assertTrue(ambiguous.isAmbiguous)
    assertTrue(ambiguous.requiresUserConfirmation)
    assertTrue(ambiguous.confirmationReasons.any { it.contains("ambiguous") })
  }

  // --- Tests for PersonalNutritionGoalEngine ---
  @Test
  fun testPersonalNutritionGoalMifflinStJeorAndSafeFloor() {
    val goalEngine = com.example.nutrition.goals.StandardPersonalNutritionGoalEngine()
    val maleResult = goalEngine.calculateEnergyRequirements(
      age = 25,
      gender = "Male",
      heightCm = 175f,
      weightKg = 70f,
      activityLevel = com.example.nutrition.goals.ActivityLevel.MODERATE,
      goal = com.example.nutrition.goals.FitnessGoalType.LOSE_WEIGHT
    )
    assertEquals(1674, maleResult.bmrKcal)
    assertEquals(2595, maleResult.tdeeKcal)
    assertEquals(2195, maleResult.recommendedTargetKcal)
    assertFalse(maleResult.isSafetyFlagged)

    val femaleStarvationCheck = goalEngine.calculateEnergyRequirements(
      age = 70,
      gender = "Female",
      heightCm = 145f,
      weightKg = 40f,
      activityLevel = com.example.nutrition.goals.ActivityLevel.SEDENTARY,
      goal = com.example.nutrition.goals.FitnessGoalType.LOSE_WEIGHT
    )
    assertTrue(femaleStarvationCheck.recommendedTargetKcal >= 1200)
  }

  @Test
  fun testNutritionGoalMetricImperialConversions() {
    val goalEngine = com.example.nutrition.goals.StandardPersonalNutritionGoalEngine()
    val kg = goalEngine.convertWeight(154.32f, "lb", "kg")
    assertEquals(70f, kg, 0.2f)

    val cm = goalEngine.convertHeight(68.898f, "in", "cm")
    assertEquals(175f, cm, 0.2f)
  }

  // --- Tests for FoodCorrectionAndLearningModule ---
  @Test
  fun testFoodCorrectionAndConsentedTrainingExport() = kotlinx.coroutines.runBlocking {
    val repo = com.example.nutrition.learning.InMemoryCorrectionRepository()
    val exportService = com.example.nutrition.learning.FeedbackExportService(repo)
    val suggestionEngine = com.example.nutrition.learning.LearningSuggestionEngine(repo)

    val correction = com.example.nutrition.learning.FoodCorrection(
      originalPredictedClassId = "nep_veg_curry",
      originalPredictedName = "Mixed Veg Curry",
      correctedFoodName = "Aloo Gobi Tarkari",
      originalGrams = 150f,
      correctedGrams = 180f,
      consentForAnonymousTraining = true
    )
    repo.saveCorrection(correction)

    val suggestion = suggestionEngine.getSuggestionForPrediction("Mixed Veg Curry")
    assertEquals("Aloo Gobi Tarkari", suggestion)

    val exportedJson = exportService.exportConsentedDatasetJson()
    assertTrue(exportedJson.contains("NutriGlass-FoodVision-Dataset-V1"))
    assertTrue(exportedJson.contains("Aloo Gobi Tarkari"))
  }

  // --- Tests for MonthlyReportEngine & Backup/Restore ---
  @Test
  fun testMonthlyReportEngineSummaryAndBackupJson() {
    val monthKey = "2026-10"
    val meals = listOf(
      com.example.data.model.MealEntry(dateString = "2026-10-01", mealType = "Lunch", foodName = "Dal Bhat", calories = 650),
      com.example.data.model.MealEntry(dateString = "2026-10-02", mealType = "Dinner", foodName = "Chicken Curry", calories = 550)
    )
    val workouts = listOf(
      com.example.data.model.WorkoutSession(dateString = "2026-10-01", workoutType = "Outdoor Walk", durationSeconds = 1800, distanceMeters = 2500f, caloriesBurned = 150)
    )
    val weights = listOf(
      com.example.data.model.WeightRecord(dateString = "2026-10-01", weightKg = 70f),
      com.example.data.model.WeightRecord(dateString = "2026-10-15", weightKg = 68.8f)
    )
    val profile = com.example.data.model.UserProfile(dailyCalorieTarget = 2000)
    val goal = com.example.data.model.MonthlyGoalTarget(monthKey = monthKey, targetMonthlyWalkingDistanceKm = 50f)

    val summary = com.example.domain.MonthlyReportEngine.generateMonthlySummary(
      monthKey = monthKey,
      meals = meals,
      workouts = workouts,
      steps = emptyList(),
      weights = weights,
      userProfile = profile,
      goalTarget = goal
    )

    assertEquals(1200, summary.totalCaloriesConsumed)
    assertEquals(150, summary.totalExerciseCalories)
    assertEquals(2.5f, summary.totalWalkingDistanceKm, 0.01f)
    assertEquals(2, summary.daysLoggedMeals)
    assertEquals(-1.2f, summary.netWeightChangeKg, 0.1f)

    val csv = com.example.domain.MonthlyReportEngine.exportMonthlyReportCsv(summary, meals, workouts)
    assertTrue(csv.contains("Dal Bhat"))
    assertTrue(csv.contains("1200 kcal"))

    val jsonBackup = com.example.domain.MonthlyReportEngine.generateDatabaseBackupJson(
      profile = profile,
      meals = meals,
      weights = weights,
      workouts = workouts,
      steps = emptyList(),
      water = emptyList(),
      monthlyTargets = listOf(goal)
    )
    assertTrue(jsonBackup.contains("NutriGlass AI"))
    assertTrue(jsonBackup.contains("Dal Bhat"))
  }
}

