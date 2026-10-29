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
}

