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
}
