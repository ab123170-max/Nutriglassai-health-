package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

@Dao
interface MealDao {
    @Query("SELECT * FROM meal_entries WHERE dateString = :date ORDER BY timestamp ASC")
    fun getMealsForDate(date: String): Flow<List<MealEntry>>

    @Query("SELECT * FROM meal_entries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMeals(limit: Int = 20): Flow<List<MealEntry>>

    @Query("SELECT * FROM meal_entries ORDER BY timestamp DESC")
    fun getAllMeals(): Flow<List<MealEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntry): Long

    @Update
    suspend fun updateMeal(meal: MealEntry)

    @Delete
    suspend fun deleteMeal(meal: MealEntry)

    @Query("DELETE FROM meal_entries WHERE id = :id")
    suspend fun deleteMealById(id: Long)
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_database WHERE name LIKE '%' || :query || '%' OR nepaliName LIKE '%' || :query || '%' ORDER BY isNepaliDish DESC, name ASC")
    fun searchFoods(query: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_database ORDER BY isNepaliDish DESC, category ASC, name ASC")
    fun getAllFoods(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_database WHERE barcode = :barcode LIMIT 1")
    suspend fun getFoodByBarcode(barcode: String): FoodItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoods(foods: List<FoodItem>)

    @Query("SELECT COUNT(*) FROM food_database")
    suspend fun getFoodCount(): Int
}

@Dao
interface HealthDao {
    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    // Water
    @Query("SELECT * FROM water_entries WHERE dateString = :date ORDER BY timestamp ASC")
    fun getWaterForDate(date: String): Flow<List<WaterEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWater(entry: WaterEntry): Long

    @Query("DELETE FROM water_entries WHERE id = (SELECT id FROM water_entries WHERE dateString = :date ORDER BY timestamp DESC LIMIT 1)")
    suspend fun removeLatestWater(date: String)

    // Steps
    @Query("SELECT * FROM step_records WHERE dateString = :date LIMIT 1")
    fun getStepsForDate(date: String): Flow<StepRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStepRecord(record: StepRecord)

    // Workouts
    @Query("SELECT * FROM workout_sessions WHERE dateString = :date ORDER BY completedAt DESC")
    fun getWorkoutsForDate(date: String): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY completedAt DESC")
    fun getAllWorkouts(): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutSession): Long

    // Weight
    @Query("SELECT * FROM weight_records ORDER BY dateString ASC")
    fun getWeightHistory(): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatestWeight(): Flow<WeightRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(record: WeightRecord): Long

    @Query("DELETE FROM weight_records WHERE id = :id")
    suspend fun deleteWeight(id: Long)

    // Sleep
    @Query("SELECT * FROM sleep_records ORDER BY dateString DESC")
    fun getAllSleepRecords(): Flow<List<SleepRecord>>

    @Query("SELECT * FROM sleep_records WHERE dateString = :date LIMIT 1")
    fun getSleepForDate(date: String): Flow<SleepRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleep(record: SleepRecord): Long

    // Progress Photos
    @Query("SELECT * FROM progress_photos ORDER BY timestamp DESC")
    fun getAllProgressPhotos(): Flow<List<ProgressPhoto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressPhoto(photo: ProgressPhoto): Long

    @Query("DELETE FROM progress_photos WHERE id = :id")
    suspend fun deleteProgressPhoto(id: Long)

    // Scans
    @Query("SELECT * FROM food_scan_records ORDER BY scannedAt DESC")
    fun getAllScans(): Flow<List<FoodScanRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: FoodScanRecord): Long

    @Query("UPDATE food_scan_records SET isLogged = 1 WHERE id = :id")
    suspend fun markScanLogged(id: Long)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY level ASC")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun initAchievements(achievements: List<Achievement>)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :unlockedAt WHERE code = :code AND isUnlocked = 0")
    suspend fun unlock(code: String, unlockedAt: Long = System.currentTimeMillis())
}
