package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.Achievement
import com.example.data.model.FoodItem
import com.example.data.model.FoodScanRecord
import com.example.data.model.MealEntry
import com.example.data.model.ProgressPhoto
import com.example.data.model.SleepRecord
import com.example.data.model.StepRecord
import com.example.data.model.UserProfile
import com.example.data.model.WaterEntry
import com.example.data.model.WeightRecord
import com.example.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthRepository(private val database: AppDatabase) {

    private val mealDao = database.mealDao()
    private val foodDao = database.foodDao()
    private val healthDao = database.healthDao()
    private val achievementDao = database.achievementDao()

    fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // User Profile & Measurement Preferences
    val userProfile: Flow<UserProfile?> = healthDao.getUserProfile()
    val measurementPreferences: Flow<com.example.data.model.UserMeasurementPreferences?> = healthDao.getMeasurementPreferences()

    suspend fun saveProfile(profile: UserProfile) {
        healthDao.saveUserProfile(profile)
    }

    suspend fun saveMeasurementPreferences(preferences: com.example.data.model.UserMeasurementPreferences) {
        healthDao.saveMeasurementPreferences(preferences)
    }

    // Food Database
    val allFoods: Flow<List<FoodItem>> = foodDao.getAllFoods()

    fun searchFoods(query: String): Flow<List<FoodItem>> = foodDao.searchFoods(query)

    suspend fun insertCustomFood(food: FoodItem): Long = foodDao.insertFood(food)

    suspend fun getFoodByBarcode(barcode: String): FoodItem? = foodDao.getFoodByBarcode(barcode)

    suspend fun getFoodByName(name: String): FoodItem? = foodDao.getFoodByName(name)

    suspend fun getFoodById(id: Long): FoodItem? = foodDao.getFoodById(id)

    // Meals
    fun getMealsForDate(date: String): Flow<List<MealEntry>> = mealDao.getMealsForDate(date)

    val allMeals: Flow<List<MealEntry>> = mealDao.getAllMeals()

    suspend fun logMeal(meal: MealEntry): Long {
        val id = mealDao.insertMeal(meal)
        // Check achievements
        achievementDao.unlock("first_log")
        if (meal.nepaliName.isNotBlank() || meal.foodName.contains("Dal", true) || meal.foodName.contains("Dhido", true) || meal.foodName.contains("Momo", true)) {
            achievementDao.unlock("nepali_nutrition")
        }
        return id
    }

    suspend fun deleteMeal(meal: MealEntry) {
        mealDao.deleteMeal(meal)
    }

    // Water
    fun getWaterForDate(date: String): Flow<List<WaterEntry>> = healthDao.getWaterForDate(date)
    val allWater: Flow<List<WaterEntry>> = healthDao.getAllWaterEntries()

    suspend fun addWater(date: String, amountMl: Int): Long {
        val id = healthDao.insertWater(WaterEntry(dateString = date, amountMl = amountMl))
        achievementDao.unlock("daily_hydration_1")
        return id
    }

    suspend fun removeLatestWater(date: String) {
        healthDao.removeLatestWater(date)
    }

    // Steps
    fun getStepsForDate(date: String): Flow<StepRecord?> = healthDao.getStepsForDate(date)

    suspend fun updateSteps(date: String, steps: Int, distanceMeters: Float, caloriesBurned: Int, activeMinutes: Int) {
        val record = StepRecord(
            dateString = date,
            steps = steps,
            distanceMeters = distanceMeters,
            caloriesBurned = caloriesBurned,
            activeMinutes = activeMinutes,
            lastUpdated = System.currentTimeMillis()
        )
        healthDao.saveStepRecord(record)
        if (steps >= 5000) achievementDao.unlock("steps_5k")
        if (steps >= 8000) achievementDao.unlock("steps_8k")
        if (steps >= 10000) achievementDao.unlock("steps_10k")
    }

    // Workouts
    fun getWorkoutsForDate(date: String): Flow<List<WorkoutSession>> = healthDao.getWorkoutsForDate(date)
    val allWorkouts: Flow<List<WorkoutSession>> = healthDao.getAllWorkouts()

    suspend fun logWorkout(workout: WorkoutSession): Long {
        val id = healthDao.insertWorkout(workout)
        achievementDao.unlock("first_workout")
        if (workout.workoutType.contains("Walk", true) && workout.distanceMeters > 500) {
            achievementDao.unlock("first_walk_gps")
        }
        return id
    }

    // Weight
    val weightHistory: Flow<List<WeightRecord>> = healthDao.getWeightHistory()
    val latestWeight: Flow<WeightRecord?> = healthDao.getLatestWeight()

    suspend fun logWeight(record: WeightRecord): Long {
        val id = healthDao.insertWeight(record)
        achievementDao.unlock("weight_milestone")
        return id
    }

    suspend fun deleteWeight(id: Long) {
        healthDao.deleteWeight(id)
    }

    // Sleep
    val allSleepRecords: Flow<List<SleepRecord>> = healthDao.getAllSleepRecords()
    fun getSleepForDate(date: String): Flow<SleepRecord?> = healthDao.getSleepForDate(date)

    suspend fun logSleep(record: SleepRecord): Long {
        val id = healthDao.insertSleep(record)
        if (record.durationMinutes >= 420) { // 7+ hours
            achievementDao.unlock("rest_and_restore")
        }
        return id
    }

    // Progress Photos
    val progressPhotos: Flow<List<ProgressPhoto>> = healthDao.getAllProgressPhotos()

    suspend fun saveProgressPhoto(photo: ProgressPhoto): Long = healthDao.insertProgressPhoto(photo)
    suspend fun deleteProgressPhoto(id: Long) = healthDao.deleteProgressPhoto(id)

    // AI Food Scans
    val allScans: Flow<List<FoodScanRecord>> = healthDao.getAllScans()

    suspend fun saveScan(scan: FoodScanRecord): Long {
        val id = healthDao.insertScan(scan)
        achievementDao.unlock("first_scan")
        return id
    }

    suspend fun markScanLogged(id: Long) = healthDao.markScanLogged(id)

    // Achievements
    val allAchievements: Flow<List<Achievement>> = achievementDao.getAllAchievements()

    suspend fun unlockAchievement(code: String) {
        achievementDao.unlock(code)
    }

    // Monthly Goal Targets
    fun getMonthlyGoalTarget(monthKey: String): Flow<com.example.data.model.MonthlyGoalTarget?> =
        healthDao.getMonthlyGoalTarget(monthKey)

    val allMonthlyGoalTargets: Flow<List<com.example.data.model.MonthlyGoalTarget>> =
        healthDao.getAllMonthlyGoalTargets()

    suspend fun saveMonthlyGoalTarget(target: com.example.data.model.MonthlyGoalTarget) {
        healthDao.saveMonthlyGoalTarget(target)
    }

    // Database Restore from JSON Backup
    suspend fun restoreFromBackupJson(jsonString: String): Boolean {
        return try {
            val root = org.json.JSONObject(jsonString)
            if (root.has("profile")) {
                val p = root.getJSONObject("profile")
                val profile = UserProfile(
                    name = p.optString("name", "Health Enthusiast"),
                    age = p.optInt("age", 26),
                    gender = p.optString("gender", "Male"),
                    heightCm = p.optDouble("heightCm", 175.0).toFloat(),
                    weightKg = p.optDouble("weightKg", 70.0).toFloat(),
                    targetWeightKg = p.optDouble("targetWeightKg", 66.0).toFloat(),
                    dailyCalorieTarget = p.optInt("dailyCalorieTarget", 2000),
                    dailyStepGoal = p.optInt("dailyStepGoal", 8000),
                    onboardingCompleted = true
                )
                healthDao.saveUserProfile(profile)
            }

            if (root.has("meals")) {
                val arr = root.getJSONArray("meals")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val meal = MealEntry(
                        dateString = o.optString("dateString"),
                        mealType = o.optString("mealType", "Lunch"),
                        foodName = o.optString("foodName"),
                        nepaliName = o.optString("nepaliName", ""),
                        portionDescription = o.optString("portionDescription", "1 serving"),
                        quantity = o.optDouble("quantity", 1.0).toFloat(),
                        unit = o.optString("unit", "serving"),
                        calories = o.optInt("calories", 0),
                        protein = o.optDouble("protein", 0.0).toFloat(),
                        carbs = o.optDouble("carbs", 0.0).toFloat(),
                        fat = o.optDouble("fat", 0.0).toFloat(),
                        fiber = o.optDouble("fiber", 0.0).toFloat(),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                    mealDao.insertMeal(meal)
                }
            }

            if (root.has("weights")) {
                val arr = root.getJSONArray("weights")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val w = WeightRecord(
                        dateString = o.optString("dateString"),
                        weightKg = o.optDouble("weightKg", 70.0).toFloat(),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                    healthDao.insertWeight(w)
                }
            }

            if (root.has("workouts")) {
                val arr = root.getJSONArray("workouts")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val w = WorkoutSession(
                        dateString = o.optString("dateString"),
                        workoutType = o.optString("workoutType", "Outdoor Walk"),
                        durationSeconds = o.optLong("durationSeconds", 1800L),
                        distanceMeters = o.optDouble("distanceMeters", 0.0).toFloat(),
                        caloriesBurned = o.optInt("caloriesBurned", 150),
                        completedAt = o.optLong("completedAt", System.currentTimeMillis())
                    )
                    healthDao.insertWorkout(w)
                }
            }

            if (root.has("monthlyTargets")) {
                val arr = root.getJSONArray("monthlyTargets")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val t = com.example.data.model.MonthlyGoalTarget(
                        monthKey = o.optString("monthKey"),
                        targetWeightChangeKg = o.optDouble("targetWeightChangeKg", -1.5).toFloat(),
                        targetWalkingDays = o.optInt("targetWalkingDays", 20),
                        targetMonthlyWalkingDistanceKm = o.optDouble("targetMonthlyWalkingDistanceKm", 60.0).toFloat(),
                        targetWorkoutSessions = o.optInt("targetWorkoutSessions", 15),
                        dailyCalorieTarget = o.optInt("dailyCalorieTarget", 2000),
                        targetDaysMeetingNutritionGoal = o.optInt("targetDaysMeetingNutritionGoal", 20)
                    )
                    healthDao.saveMonthlyGoalTarget(t)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Books & PDF Reader
    val allBooks: Flow<List<com.example.data.model.BookEntity>> = healthDao.getAllBooks()

    fun getBookById(id: Long): Flow<com.example.data.model.BookEntity?> = healthDao.getBookById(id)

    suspend fun getBookByIdDirect(id: Long): com.example.data.model.BookEntity? = healthDao.getBookByIdDirect(id)

    suspend fun insertBook(book: com.example.data.model.BookEntity): Long = healthDao.insertBook(book)

    suspend fun updateBook(book: com.example.data.model.BookEntity) = healthDao.updateBook(book)

    suspend fun deleteBook(id: Long) = healthDao.deleteBook(id)

    fun getBookmarksForBook(bookId: Long): Flow<List<com.example.data.model.BookmarkEntity>> = healthDao.getBookmarksForBook(bookId)

    val allBookmarks: Flow<List<com.example.data.model.BookmarkEntity>> = healthDao.getAllBookmarks()

    suspend fun insertBookmark(bookmark: com.example.data.model.BookmarkEntity): Long = healthDao.insertBookmark(bookmark)

    suspend fun deleteBookmark(id: Long) = healthDao.deleteBookmark(id)

    val allReadingSessions: Flow<List<com.example.data.model.ReadingSessionRecord>> = healthDao.getAllReadingSessions()

    suspend fun logReadingSession(session: com.example.data.model.ReadingSessionRecord): Long {
        val id = healthDao.insertReadingSession(session)
        achievementDao.unlock("walk_and_learn_first")
        return id
    }

    // Walk & Listen Audio Items
    val allAudioItems: Flow<List<com.example.data.model.AudioItemEntity>> = healthDao.getAllAudioItems()

    fun getAudioItemsByCategory(category: String): Flow<List<com.example.data.model.AudioItemEntity>> =
        healthDao.getAudioItemsByCategory(category)

    val favoriteAudioItems: Flow<List<com.example.data.model.AudioItemEntity>> = healthDao.getFavoriteAudioItems()

    suspend fun getAudioItemByIdDirect(id: Long): com.example.data.model.AudioItemEntity? = healthDao.getAudioItemByIdDirect(id)

    suspend fun insertAudioItem(item: com.example.data.model.AudioItemEntity): Long = healthDao.insertAudioItem(item)

    suspend fun updateAudioItem(item: com.example.data.model.AudioItemEntity) = healthDao.updateAudioItem(item)

    suspend fun deleteAudioItem(id: Long) = healthDao.deleteAudioItem(id)

    // Voice Coaching Preferences
    val voiceCoachingPreferences: Flow<com.example.data.model.VoiceCoachingPreferences?> = healthDao.getVoiceCoachingPreferences()

    suspend fun saveVoiceCoachingPreferences(prefs: com.example.data.model.VoiceCoachingPreferences) {
        healthDao.saveVoiceCoachingPreferences(prefs)
    }

    // Weekly Check-Ins
    val allWeeklyCheckIns: Flow<List<com.example.data.model.WeeklyCheckInEntity>> = healthDao.getAllWeeklyCheckIns()
    val latestWeeklyCheckIn: Flow<com.example.data.model.WeeklyCheckInEntity?> = healthDao.getLatestWeeklyCheckIn()

    suspend fun getLatestWeeklyCheckInDirect(): com.example.data.model.WeeklyCheckInEntity? = healthDao.getLatestWeeklyCheckInDirect()

    suspend fun insertWeeklyCheckIn(checkIn: com.example.data.model.WeeklyCheckInEntity): Long {
        val id = healthDao.insertWeeklyCheckIn(checkIn)
        achievementDao.unlock("weekly_check_in_first")
        return id
    }

    suspend fun deleteWeeklyCheckIn(id: Long) = healthDao.deleteWeeklyCheckIn(id)

    // Questionnaires
    fun getQuestionnaireForCheckIn(checkInId: Long): Flow<com.example.data.model.CheckInQuestionnaireEntity?> =
        healthDao.getQuestionnaireForCheckIn(checkInId)

    suspend fun insertQuestionnaire(questionnaire: com.example.data.model.CheckInQuestionnaireEntity): Long =
        healthDao.insertQuestionnaire(questionnaire)

    // Weekly Plans
    val latestWeeklyPlan: Flow<com.example.data.model.WeeklyPlanEntity?> = healthDao.getLatestWeeklyPlan()
    val allWeeklyPlans: Flow<List<com.example.data.model.WeeklyPlanEntity>> = healthDao.getAllWeeklyPlans()

    suspend fun insertWeeklyPlan(plan: com.example.data.model.WeeklyPlanEntity): Long = healthDao.insertWeeklyPlan(plan)

    suspend fun updateWeeklyPlan(plan: com.example.data.model.WeeklyPlanEntity) = healthDao.updateWeeklyPlan(plan)

    // Weekly Check-In Preferences
    val weeklyCheckInPreferences: Flow<com.example.data.model.WeeklyCheckInPreferences?> = healthDao.getWeeklyCheckInPreferences()

    suspend fun saveWeeklyCheckInPreferences(prefs: com.example.data.model.WeeklyCheckInPreferences) {
        healthDao.saveWeeklyCheckInPreferences(prefs)
    }
}


