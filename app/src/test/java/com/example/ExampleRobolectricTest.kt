package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
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

  // --- Tests for Weekly Check-In & AI Goal Coaching ---
  @Test
  fun testWeeklyCheckInWeightConversionAndProgressClassification() {
    val checkInProgressing = com.example.data.model.WeeklyCheckInEntity(
      checkInDate = "2026-10-25",
      currentWeightKg = 68.0f,
      previousWeightKg = 68.8f,
      startingWeightKg = 72.0f,
      targetWeightKg = 65.0f,
      weightChangeWeeklyKg = -0.8f,
      weightChangeTotalKg = -4.0f,
      progressStatus = "PROGRESSING"
    )

    assertEquals("PROGRESSING", checkInProgressing.progressStatus)
    assertEquals(-0.8f, checkInProgressing.weightChangeWeeklyKg, 0.01f)

    // Unit Conversion Test
    val lbVal = checkInProgressing.currentWeightKg * 2.20462f
    assertEquals(149.91f, lbVal, 0.5f)
  }

  @Test
  fun testCheckInReminderNextWeekendTimestampCalculation() {
    val saturdayMillis = com.example.domain.CheckInReminderManager.calculateNextCheckInMillis("Saturday", 8, 0)
    val sundayMillis = com.example.domain.CheckInReminderManager.calculateNextCheckInMillis("Sunday", 9, 30)

    assertTrue(saturdayMillis > System.currentTimeMillis())
    assertTrue(sundayMillis > System.currentTimeMillis())
  }

  @Test
  fun testWeeklyAiCoachEngineFallbackPlanGeneration() {
    val profile = com.example.data.model.UserProfile(
      fitnessGoal = "Lose Weight",
      weightKg = 70f,
      targetWeightKg = 65f
    )
    val checkIn = com.example.data.model.WeeklyCheckInEntity(
      checkInDate = "2026-10-25",
      currentWeightKg = 69.5f,
      previousWeightKg = 70.0f,
      startingWeightKg = 72.0f,
      targetWeightKg = 65.0f,
      weightChangeWeeklyKg = -0.5f,
      weightChangeTotalKg = -2.5f,
      progressStatus = "PROGRESSING"
    )
    val questionnaire = com.example.data.model.CheckInQuestionnaireEntity(
      checkInId = 1L,
      dateString = "2026-10-25",
      followedMealPlanDays = 6,
      exerciseSessionsCompleted = 4,
      walkingGoalDaysAchieved = 5,
      sleepQualityRating = "Good"
    )

    val plan = com.example.domain.WeeklyAiCoachEngine.generateWeeklyMotivationAndPlan(
      checkIn = checkIn,
      questionnaire = questionnaire,
      userProfile = profile,
      unit = "kg"
    )

    assertTrue(plan.motivationMessage.contains("69.5"))
    assertTrue(plan.proposedAdjustments.isNotEmpty())
    assertTrue(plan.affordableFoodAlternatives.contains("Lentils"))
  }

  @Test
  fun testRenderMealHeaderDeduplicationAndFormatting() {
    // Null or empty
    assertEquals("Scanned Meal", com.example.domain.NutritionEngine.renderMealHeader(null))
    assertEquals("Scanned Meal", com.example.domain.NutritionEngine.renderMealHeader(emptyList()))

    // Single item
    val single = listOf(object { val name = "Dal Bhat" }).map { it.name }
    assertEquals("Dal Bhat", com.example.domain.NutritionEngine.renderMealHeader(single))

    // Duplicates deduplicated
    val duplicates = listOf("Dal Bhat", "Dal Bhat ", "Dal Bhat")
    assertEquals("Dal Bhat", com.example.domain.NutritionEngine.renderMealHeader(duplicates))

    // Two items
    val twoItems = listOf("Dal Bhat", "Chicken Curry")
    assertEquals("Dal Bhat, Chicken Curry", com.example.domain.NutritionEngine.renderMealHeader(twoItems))

    // Three or more items (+N more)
    val fourItems = listOf("Dal Bhat", "Chicken Curry", "Aloo Saag", "Momo")
    assertEquals("Dal Bhat, Chicken Curry +2 more", com.example.domain.NutritionEngine.renderMealHeader(fourItems))
  }

  @Test
  fun testGpsTrackingEngineCalculationsAndFallback() {
    // Haversine distance between Kathmandu (27.7172, 85.3240) and Lalitpur (27.6670, 85.3206) (~5.6 km)
    val distKm = com.example.domain.GpsTrackingEngine.calculateDistanceKm(27.7172, 85.3240, 27.6670, 85.3206)
    assertEquals(5.6f, distKm.toFloat(), 0.5f)

    // Accuracy Check
    assertTrue(com.example.domain.GpsTrackingEngine.isAccuracyUsable(15f))
    assertFalse(com.example.domain.GpsTrackingEngine.isAccuracyUsable(65f))

    // Jitter filter (> 4 meters)
    assertTrue(com.example.domain.GpsTrackingEngine.isSignificantMovementMeters(5.2f))
    assertFalse(com.example.domain.GpsTrackingEngine.isSignificantMovementMeters(2.1f))

    // Fallback distance calculations
    val estDistFromSteps = com.example.domain.GpsTrackingEngine.calculateFallbackDistanceKm(secondsElapsed = 600, fallbackStepCount = 1000)
    assertEquals(0.75f, estDistFromSteps, 0.01f)

    val estDistFromTime = com.example.domain.GpsTrackingEngine.calculateFallbackDistanceKm(secondsElapsed = 3600, fallbackStepCount = 0)
    assertEquals(4.5f, estDistFromTime, 0.01f)

    // Calories & Pace Formatting
    val cals = com.example.domain.GpsTrackingEngine.calculateCaloriesBurned(2.0f)
    assertEquals(130, cals)

    val pace = com.example.domain.GpsTrackingEngine.formatPace(secondsElapsed = 720, distanceKm = 2.0f) // 6 min/km -> 6'00" /km
    assertEquals("6'00\" /km", pace)
  }

  @Test
  fun testGpsAutoPauseAndResumeDetection() {
    val autoPauseSession = com.example.domain.GpsAutoPauseSession()
    autoPauseSession.startSession()

    assertTrue(autoPauseSession.isTracking)
    assertFalse(autoPauseSession.isAutoPaused)

    // 1. Noisy point (accuracy > 25m) should be ignored
    val noisyIgnored = autoPauseSession.processGpsUpdate(27.7172, 85.3240, accuracyMeters = 30f, speedMps = 1.2f)
    assertFalse(noisyIgnored)

    // 2. Initial valid GPS fix (speed = 1.2 m/s > 0.4 m/s)
    autoPauseSession.processGpsUpdate(27.7172, 85.3240, accuracyMeters = 10f, speedMps = 1.2f)
    assertFalse(autoPauseSession.isAutoPaused)

    // 3. User stops moving (speed = 0.1 m/s < 0.4 m/s) for 5 stationary updates
    for (i in 1..4) {
      autoPauseSession.processGpsUpdate(27.7172, 85.3240, accuracyMeters = 10f, speedMps = 0.1f)
      assertFalse("Should not pause before 5 seconds threshold", autoPauseSession.isAutoPaused)
    }

    // 5th stationary update triggers auto-pause
    val pauseTriggered = autoPauseSession.processGpsUpdate(27.7172, 85.3240, accuracyMeters = 10f, speedMps = 0.1f)
    assertTrue("Auto-pause should trigger on 5th stationary update", pauseTriggered)
    assertTrue(autoPauseSession.isAutoPaused)

    // 4. User resumes walking (speed = 0.8 m/s >= 0.4 m/s)
    autoPauseSession.processGpsUpdate(27.7175, 85.3245, accuracyMeters = 10f, speedMps = 0.8f)
    assertFalse("Session should automatically resume when speed >= 0.4 m/s", autoPauseSession.isAutoPaused)
  }

  // --- Tests for Step 7: Image Preparation & Gemini Vision API Pipeline ---

  @Test
  fun testValidJpegImagePreparation() {
    val bitmap = android.graphics.Bitmap.createBitmap(300, 200, android.graphics.Bitmap.Config.ARGB_8888)
    val prepared = com.example.domain.ImagePreparationModule.prepareBitmapForAi(bitmap, maxDimension = 1024)

    assertEquals("image/jpeg", prepared.mimeType)
    assertTrue("JPEG byte count must be substantial", prepared.byteCount > 100)
    assertTrue("Must verify JPEG magic bytes (SOI/EOI)", com.example.domain.ImagePreparationModule.isJpeg(prepared.jpegBytes))
    assertFalse("Base64 string must not contain line breaks", prepared.base64Data.contains("\n"))
    assertFalse("Base64 string must not contain carriage returns", prepared.base64Data.contains("\r"))
  }

  @Test
  fun testPngConvertedToJpeg() {
    val bitmap = android.graphics.Bitmap.createBitmap(150, 150, android.graphics.Bitmap.Config.ARGB_8888)
    val pngStream = java.io.ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, pngStream)
    val pngBytes = pngStream.toByteArray()

    assertTrue("Raw bytes should start as PNG", com.example.domain.ImagePreparationModule.isPng(pngBytes))

    val converted = com.example.domain.ImagePreparationModule.convertBytesToJpeg(pngBytes)
    assertEquals("image/jpeg", converted.mimeType)
    assertTrue("Converted output must be JPEG magic bytes", com.example.domain.ImagePreparationModule.isJpeg(converted.jpegBytes))
  }

  @Test
  fun testEmptyAndCorruptImageDataRejected() {
    var emptyRejected = false
    try {
      com.example.domain.ImagePreparationModule.convertBytesToJpeg(ByteArray(0))
    } catch (e: IllegalArgumentException) {
      emptyRejected = true
    }
    assertTrue("Empty bytes must throw IllegalArgumentException", emptyRejected)

    var corruptRejected = false
    try {
      val corruptBytes = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04)
      com.example.domain.ImagePreparationModule.convertBytesToJpeg(corruptBytes)
    } catch (e: IllegalArgumentException) {
      corruptRejected = true
    }
    assertTrue("Corrupt bytes must throw IllegalArgumentException", corruptRejected)
  }

  @Test
  fun testBase64WithDataUrlPrefixCleaned() {
    val rawBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
    val withPrefix = "data:image/png;base64, $rawBase64\n\r"
    val cleaned = com.example.domain.ImagePreparationModule.cleanBase64Data(withPrefix)

    assertEquals(rawBase64, cleaned)
    assertFalse("Must not contain data URL prefix", cleaned.contains("data:"))
    assertFalse("Must not contain whitespace", cleaned.contains(" "))
  }

  @Test
  fun testDoubleEncodedBase64Unwrapped() {
    val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
    val prepared = com.example.domain.ImagePreparationModule.prepareBitmapForAi(bitmap)
    val singleEncoded = prepared.base64Data

    // Accidental double encode
    val doubleEncoded = android.util.Base64.encodeToString(singleEncoded.toByteArray(Charsets.US_ASCII), android.util.Base64.NO_WRAP)

    val unwrapped = com.example.domain.ImagePreparationModule.cleanBase64Data(doubleEncoded)
    assertEquals("Should unwrap double-encoded Base64 string", singleEncoded, unwrapped)
  }

  @Test
  fun testRotatedCameraImagePreparation() {
    val bitmap = android.graphics.Bitmap.createBitmap(200, 100, android.graphics.Bitmap.Config.ARGB_8888)
    val prepared = com.example.domain.ImagePreparationModule.prepareBitmapForAi(bitmap, rotationDegrees = 90)

    // A 200x100 image rotated 90 degrees becomes 100x200
    assertEquals(100, prepared.width)
    assertEquals(200, prepared.height)
  }

  @Test
  fun testOversizedImageResizing() {
    val bitmap = android.graphics.Bitmap.createBitmap(2000, 1000, android.graphics.Bitmap.Config.ARGB_8888)
    val prepared = com.example.domain.ImagePreparationModule.prepareBitmapForAi(bitmap, maxDimension = 1000)

    // Max dimension should be clamped to 1000 while maintaining 2:1 aspect ratio -> 1000x500
    assertEquals(1000, prepared.width)
    assertEquals(500, prepared.height)
    assertTrue("JPEG magic bytes valid on downscaled image", com.example.domain.ImagePreparationModule.isJpeg(prepared.jpegBytes))
  }

  @Test
  fun testGeminiRequestSerialization() {
    val prompt = "Identify visible foods and nutrients."
    val dummyBase64 = "U0FNUExF"
    val json = com.example.data.network.GeminiFoodService.buildGeminiRequestBody(prompt, dummyBase64)

    assertTrue("Request must have contents array", json.has("contents"))
    val contents = json.getJSONArray("contents")
    assertEquals(1, contents.length())

    val parts = contents.getJSONObject(0).getJSONArray("parts")
    assertEquals(2, parts.length())
    assertEquals(prompt, parts.getJSONObject(0).getString("text"))

    val inlinePart = parts.getJSONObject(1)
    assertTrue("Must have inline_data schema", inlinePart.has("inline_data"))
    assertEquals("image/jpeg", inlinePart.getJSONObject("inline_data").getString("mime_type"))
    assertEquals(dummyBase64, inlinePart.getJSONObject("inline_data").getString("data"))

    assertTrue("Must have generationConfig", json.has("generationConfig"))
    val genConfig = json.getJSONObject("generationConfig")
    assertEquals("application/json", genConfig.getString("responseMimeType"))
  }

  @Test
  fun testHttp400ErrorResponseParsing() {
    val sampleError400 = """{"error":{"code":400,"message":"Invalid image payload: image too blurry","status":"INVALID_ARGUMENT"}}"""
    val parsed = com.example.data.network.GeminiFoodService.parseErrorMessage(sampleError400)
    assertTrue(parsed.contains("image too blurry"))
  }

  // ================= UNIVERSAL MEASUREMENT & INSTANT UNIT SWITCHING TESTS =================

  @Test
  fun testWeightUnitSwitchingPreservesPhysicalValue() {
    val initialKg = 103.5f
    val manager = com.example.domain.MeasurementUnitManager()

    // 1. Convert to pounds
    val lbDisplay = manager.formatKgToDisplay(initialKg, com.example.domain.WeightUnit.LB)
    assertEquals("228.2", lbDisplay)

    // 2. Convert from lb display back to normalized kg
    val parsedKgFromLb = manager.parseWeightToKg(lbDisplay, com.example.domain.WeightUnit.LB)
    assertEquals(103.5f, parsedKgFromLb!!, 0.1f)

    // 3. Convert to stones
    val stDisplay = manager.formatKgToDisplay(initialKg, com.example.domain.WeightUnit.ST)
    assertEquals("16.3", stDisplay)
    val parsedKgFromSt = manager.parseWeightToKg(stDisplay, com.example.domain.WeightUnit.ST)
    assertEquals(103.5f, parsedKgFromSt!!, 0.2f)

    // 4. In-place string conversion: kg -> lb without leaving screen
    val convertedInPlace = manager.convertWeightDisplayString("103.5", com.example.domain.WeightUnit.KG, com.example.domain.WeightUnit.LB)
    assertEquals("228.2", convertedInPlace)
  }

  @Test
  fun testHeightUnitConversionsAndSplitFeetInches() {
    val heightCm = 155.0f
    val manager = com.example.domain.MeasurementUnitManager()

    // 1. To meters
    val (mStr, _) = manager.formatCmToDisplay(heightCm, com.example.domain.HeightUnit.M)
    assertEquals("1.55", mStr)

    // 2. To inches
    val (inStr, _) = manager.formatCmToDisplay(heightCm, com.example.domain.HeightUnit.IN)
    assertEquals("61.0", inStr)

    // 3. To Feet and Inches (155 cm = ~5 feet 1.02 inches)
    val (ftStr, inStrSplit) = manager.formatCmToDisplay(heightCm, com.example.domain.HeightUnit.FT_IN)
    assertEquals("5", ftStr)
    assertEquals("1", inStrSplit)

    // 4. Parse Feet + Inches back to cm
    val parsedCm = manager.parseHeightToCm("5", com.example.domain.HeightUnit.FT_IN, "1")
    assertEquals(154.94f, parsedCm!!, 0.5f)
  }

  @Test
  fun testCircumferenceUnitConversions() {
    val waistInches = 38.5f
    val inCm = com.example.domain.UnitConversionEngine.convertCircumference(
      waistInches,
      com.example.domain.CircumferenceUnit.IN,
      com.example.domain.CircumferenceUnit.CM
    )
    assertEquals(97.79f, inCm, 0.1f)

    val backToIn = com.example.domain.UnitConversionEngine.convertCircumference(
      inCm,
      com.example.domain.CircumferenceUnit.CM,
      com.example.domain.CircumferenceUnit.IN
    )
    assertEquals(38.5f, backToIn, 0.1f)
  }

  @Test
  fun testDecimalInputValidatorSanitizationAndRange() {
    // 1. Replace commas with dots
    val sanitizedComma = com.example.domain.DecimalInputValidator.sanitizeDecimalInput("103,5")
    assertEquals("103.5", sanitizedComma)

    // 2. Multiple decimal points prevented
    val sanitizedMultiDot = com.example.domain.DecimalInputValidator.sanitizeDecimalInput("103.5.7")
    assertEquals("103.57", sanitizedMultiDot)

    // 3. Range validation
    val validResult = com.example.domain.DecimalInputValidator.validateRange("70.5", 20f, 300f, "Weight", "kg")
    assertTrue(validResult.isValid)
    assertEquals(70.5f, validResult.parsedValue!!, 0.01f)

    val outOfRange = com.example.domain.DecimalInputValidator.validateRange("500", 20f, 300f, "Weight", "kg")
    assertFalse(outOfRange.isValid)
  }

  @Test
  fun testSmartBmiIndicatorAndWhoClassifications() {
    // Height = 175cm, Weight = 70kg -> BMI = 70 / (1.75^2) = 22.86 -> 22.9
    val bmi = com.example.domain.BodyCompositionEngine.calculateBmi(70f, 175f)
    assertEquals(22.9f, bmi, 0.1f)
    val cat = com.example.domain.BmiCategory.fromBmi(bmi)
    assertEquals(com.example.domain.BmiCategory.NORMAL, cat)

    // Overweight test
    val bmiOver = com.example.domain.BodyCompositionEngine.calculateBmi(85f, 175f)
    assertEquals(27.8f, bmiOver, 0.1f)
    assertEquals(com.example.domain.BmiCategory.OVERWEIGHT, com.example.domain.BmiCategory.fromBmi(bmiOver))

    // Healthy weight range for 175cm: min = 18.5 * 1.75^2 = 56.66kg, max = 24.9 * 1.75^2 = 76.26kg
    val (minHealthy, maxHealthy) = com.example.domain.BodyCompositionEngine.calculateHealthyWeightRange(175f)
    assertEquals(56.7f, minHealthy, 0.1f)
    assertEquals(76.3f, maxHealthy, 0.1f)
  }

  @Test
  fun testBodyFatCalculationsNavyAndDeurenberg() {
    // Male Navy Formula Test Vector: Height 175cm, Waist 82cm, Neck 38cm
    val maleBf = com.example.domain.BodyCompositionEngine.calculateNavyBodyFatPercentage(
      isMale = true,
      heightCm = 175f,
      waistCm = 82f,
      neckCm = 38f
    )
    assertTrue("Male body fat percentage must be computed", maleBf != null && maleBf > 0f)
    assertTrue("Realistic male body fat range", maleBf!! in 10f..25f)

    // Female Navy Formula Test Vector: Height 165cm, Waist 74cm, Neck 34cm, Hip 96cm
    val femaleBf = com.example.domain.BodyCompositionEngine.calculateNavyBodyFatPercentage(
      isMale = false,
      heightCm = 165f,
      waistCm = 74f,
      neckCm = 34f,
      hipCm = 96f
    )
    assertTrue("Female body fat percentage must be computed", femaleBf != null && femaleBf > 0f)
    assertTrue("Realistic female body fat range", femaleBf!! in 18f..35f)

    // Fallback Deurenberg calculation
    val fallbackBf = com.example.domain.BodyCompositionEngine.calculateBmiBasedBodyFatPercentage(
      bmi = 22.9f,
      age = 26,
      isMale = true
    )
    assertTrue(fallbackBf in 10f..25f)
  }

  @Test
  fun testFoodVolumeToWeightIngredientSpecificDensity() {
    // 1. Water: 100 ml -> 100g (density = 1.0)
    val waterGrams = com.example.domain.UnitConversionEngine.convertFoodToGrams("Water", 100f, com.example.domain.FoodUnit.ML)
    assertEquals(100f, waterGrams, 0.1f)

    // 2. Cooking Oil: 100 ml -> 92g (density = 0.92, never assume 1ml = 1g!)
    val oilGrams = com.example.domain.UnitConversionEngine.convertFoodToGrams("Mustard Cooking Oil", 100f, com.example.domain.FoodUnit.ML)
    assertEquals(92f, oilGrams, 0.1f)

    // 3. Honey: 100 ml -> 142g (density = 1.42)
    val honeyGrams = com.example.domain.UnitConversionEngine.convertFoodToGrams("Raw Honey", 100f, com.example.domain.FoodUnit.ML)
    assertEquals(142f, honeyGrams, 0.1f)

    // 4. Whole Milk: 100 ml -> 103g (density = 1.03)
    val milkGrams = com.example.domain.UnitConversionEngine.convertFoodToGrams("Cow Milk", 100f, com.example.domain.FoodUnit.ML)
    assertEquals(103f, milkGrams, 0.1f)
  }

  @Test
  fun testRoomDatabaseBodyCompositionOperations() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.local.AppDatabase.getDatabase(context)
    val repository = com.example.data.repository.MeasurementRepository(db)

    val recordId = repository.recordBodyComposition(
      weightKg = 72.5f,
      heightCm = 176.0f,
      age = 27,
      gender = "Male",
      waistCm = 81.5f,
      neckCm = 37.5f,
      hipCm = 94.0f,
      notes = "Unit Test Entry"
    )
    assertTrue(recordId > 0)

    val latestFlow = repository.latestBodyComposition
    val record = latestFlow.first()
    assertTrue(record != null)
    assertEquals(72.5f, record!!.weightKg, 0.1f)
    assertEquals(81.5f, record.waistCm!!, 0.1f)
    assertTrue(record.bmi > 0f)
    assertTrue(record.bodyFatPercentage > 0f)

    repository.deleteBodyComposition(record.id)
  }

  @Test
  fun testBodyTwinEngineBiometrics() {
    // 1. Janssen SMM Calculation for Male (175cm, 70kg, 26 yo)
    val maleSmm = com.example.domain.BodyTwinEngine.calculateSkeletalMuscleMass(
      weightKg = 75f,
      heightCm = 175f,
      age = 26,
      isMale = true
    )
    assertTrue("Male SMM should be between 20kg and 40kg, got $maleSmm", maleSmm in 20f..40f)

    // 2. Female SMM (165cm, 58kg, 26 yo)
    val femaleSmm = com.example.domain.BodyTwinEngine.calculateSkeletalMuscleMass(
      weightKg = 58f,
      heightCm = 165f,
      age = 26,
      isMale = false
    )
    assertTrue("Female SMM should be between 15kg and 30kg, got $femaleSmm", femaleSmm in 15f..30f)

    // 3. SMM Percentage & Category
    val maleSmmPct = com.example.domain.BodyTwinEngine.calculateMuscleMassPercentage(maleSmm, 70f)
    assertTrue(maleSmmPct > 0f)
    val category = com.example.domain.BodyTwinEngine.getMuscleMassCategory(maleSmmPct, isMale = true)
    assertTrue(category in com.example.domain.MuscleMassCategory.entries)

    // 4. Clinical Visceral Fat Index (1 to 20 rating)
    val vfiBalanced = com.example.domain.BodyTwinEngine.calculateVisceralFatIndex(
      waistCm = 68f,
      heightCm = 175f,
      bmi = 22.5f,
      age = 26
    )
    assertTrue("VFI should be in 1..20 scale, got $vfiBalanced", vfiBalanced in 1.0f..20.0f)
    val vfiCategory = com.example.domain.VisceralFatCategory.fromRating(vfiBalanced)
    assertEquals(com.example.domain.VisceralFatCategory.BALANCED, vfiCategory)

    // 5. Waist-to-Height Ratio
    val whtr = com.example.domain.BodyTwinEngine.calculateWaistToHeightRatio(waistCm = 80f, heightCm = 175f)
    assertEquals(0.46f, whtr, 0.02f)
    val whtrCat = com.example.domain.WaistToHeightCategory.fromRatio(whtr)
    assertEquals(com.example.domain.WaistToHeightCategory.OPTIMAL, whtrCat)
  }

  @Test
  fun testBodyTwinEnginePredictiveFutureModel() {
    val prediction3M = com.example.domain.BodyTwinEngine.predictFutureBody(
      currentWeightKg = 80f,
      heightCm = 175f,
      currentBfPercent = 25f,
      currentWaistCm = 92f,
      age = 28,
      isMale = true,
      dailyCalorieDeficitKcal = 500,
      timeframeMonths = 3
    )

    assertEquals(3, prediction3M.timeframeMonths)
    assertEquals(90, prediction3M.daysAhead)
    assertTrue(prediction3M.projectedWeightKg < 80f)
    assertTrue(prediction3M.projectedBodyFatPercent < 25f)
    assertTrue(prediction3M.isSimulation)
    assertTrue(prediction3M.totalWeightLossKg > 0f)
    assertTrue(prediction3M.estimatedFatLossKg > 0f)

    val prediction6M = com.example.domain.BodyTwinEngine.predictFutureBody(
      currentWeightKg = 80f,
      heightCm = 175f,
      currentBfPercent = 25f,
      currentWaistCm = 92f,
      age = 28,
      isMale = true,
      dailyCalorieDeficitKcal = 500,
      timeframeMonths = 6
    )

    assertEquals(6, prediction6M.timeframeMonths)
    assertTrue(prediction6M.projectedWeightKg < prediction3M.projectedWeightKg)
  }

  @Test
  fun testBodyTwinEngineAll20Milestones() {
    val milestones = com.example.domain.BodyTwinEngine.evaluateMilestones(
      startingWeightKg = 85f,
      currentWeightKg = 78f, // 7 kg lost
      targetWeightKg = 75f,
      startingWaistCm = 96f,
      currentWaistCm = 88f, // 8 cm lost
      bodyFatPercent = 18f,
      startingBfPercent = 26f, // 8% lost
      muscleMassKg = 31f,
      startingMuscleMassKg = 29.5f, // +1.5kg
      visceralFatIndex = 7.5f,
      totalSnapshotsCount = 5,
      weeklyCheckInsCount = 4,
      hasProgressPhoto = true
    )

    assertEquals(20, milestones.size)

    val m1Kg = milestones.first { it.code == "WEIGHT_DROP_1KG" }
    assertTrue(m1Kg.isUnlocked)

    val m5Kg = milestones.first { it.code == "WEIGHT_DROP_5KG" }
    assertTrue(m5Kg.isUnlocked)

    val mWaist = milestones.first { it.code == "WAIST_MINUS_5CM" }
    assertTrue(mWaist.isUnlocked)

    val mPhoto = milestones.first { it.code == "PHOTO_LOGGED" }
    assertTrue(mPhoto.isUnlocked)

    val mStreak4 = milestones.first { it.code == "CHECKIN_STREAK_4" }
    assertTrue(mStreak4.isUnlocked)
  }

  @Test
  fun testRoomDatabaseBodyTwinSnapshotOperations() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.local.AppDatabase.getDatabase(context)
    val repository = com.example.data.repository.MeasurementRepository(db)

    // 1. Consent management
    repository.saveConsentPreferences(
      com.example.data.model.BodyTwinConsentPreferences(
        id = 1,
        hasAcceptedPrivacyConsent = true,
        consentedTimestamp = System.currentTimeMillis(),
        allowLocalPhotoStorage = true
      )
    )

    val consent = repository.consentPreferences.first()
    assertTrue(consent != null && consent.hasAcceptedPrivacyConsent)

    // 2. Snapshot insertion
    val snapshotId = repository.saveBodyTwinSnapshot(
      com.example.data.model.BodyTwinSnapshot(
        dateString = "2026-10-30",
        snapshotType = "WEEKLY_CHECKIN",
        title = "Week 1 Twin Checkpoint",
        weightKg = 71.5f,
        heightCm = 175.0f,
        bodyFatPercent = 19.5f,
        muscleMassKg = 30.2f,
        waistCm = 81.0f,
        visceralFatIndex = 7.0f
      )
    )
    assertTrue(snapshotId > 0)

    val snapshots = repository.allBodyTwinSnapshots.first()
    assertTrue(snapshots.any { it.id == snapshotId })

    // 3. Purge all
    repository.purgeAllBodyTwinData()
    val clearedSnapshots = repository.allBodyTwinSnapshots.first()
    assertTrue(clearedSnapshots.isEmpty())
  }

  @Test
  fun testTargetedWeeklyWeightLossEngineCalculations() {
    val targetResult = com.example.domain.TargetedWeeklyWeightLossEngine.calculateWeeklyTarget(
      startingWeightKg = 103f,
      currentWeightKg = 98f,
      targetWeightKg = 60f,
      previousWeekWeightKg = 98.8f,
      heightCm = 172f,
      age = 30,
      isMale = true,
      activityLevel = "Moderate"
    )

    assertEquals(103f, targetResult.startingWeightKg, 0.1f)
    assertEquals(98f, targetResult.currentWeightKg, 0.1f)
    assertEquals(60f, targetResult.targetWeightKg, 0.1f)
    assertTrue(targetResult.remainingWeightKg > 0f)
    assertTrue(targetResult.goalProgressPercent > 0f)
    assertEquals(com.example.domain.SafetyStatus.SAFE, targetResult.safetyStatus)
  }

  @Test
  fun testTargetedWeeklyProgramGenerator() {
    val program = com.example.domain.TargetedWeeklyWeightLossEngine.generate7DayProgram(
      currentWeightKg = 85f,
      heightCm = 175f,
      age = 26,
      isMale = true,
      activityLevel = "Moderate",
      dietaryPreference = "Standard Nepali & Asian"
    )

    assertEquals(7, program.size)
    assertEquals("Monday", program[0].dayName)
    assertTrue(program[0].calorieTarget > 1200)
    assertTrue(program[0].proteinTargetGrams > 50)
  }

  @Test
  fun testAutomaticWeeklyAdjustment() {
    // Slower than expected (plateau)
    val (adjSlower, _) = com.example.domain.TargetedWeeklyWeightLossEngine.performAutomaticWeeklyAdjustment(
      actualWeeklyChangeKg = 0.1f,
      expectedMinLossKg = 0.5f,
      expectedMaxLossKg = 1.0f
    )
    assertTrue(adjSlower < 0f) // negative calorie adjustment

    // Faster than expected (rapid loss)
    val (adjFaster, _) = com.example.domain.TargetedWeeklyWeightLossEngine.performAutomaticWeeklyAdjustment(
      actualWeeklyChangeKg = -1.6f,
      expectedMinLossKg = 0.5f,
      expectedMaxLossKg = 1.0f
    )
    assertTrue(adjFaster > 0f) // positive calorie addition
  }

  @Test
  fun testSafetyEngine() {
    // Underweight risk check
    val (status, _) = com.example.domain.TargetedWeeklyWeightLossEngine.evaluateSafety(
      currentWeightKg = 55f,
      targetWeightKg = 42f,
      heightCm = 175f
    )
    assertEquals(com.example.domain.SafetyStatus.CAUTION_UNDERWEIGHT_RISK, status)
  }
}



