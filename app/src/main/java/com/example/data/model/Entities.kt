package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Health Enthusiast",
    val age: Int = 26,
    val gender: String = "Male", // "Male", "Female", "Other"
    val heightCm: Float = 175f,
    val weightKg: Float = 70f,
    val targetWeightKg: Float = 66f,
    val activityLevel: String = "Moderate", // "Sedentary", "Light", "Moderate", "Active", "Very Active"
    val dietaryPreference: String = "Standard Nepali & Asian", // "Standard Nepali & Asian", "Vegetarian", "Vegan", "High Protein", "Low Carb"
    val fitnessGoal: String = "Lose Weight", // "Lose Weight", "Maintain Weight", "Gain Muscle", "Improve Fitness", "Eat Healthier"
    val preferredLanguage: String = "English / Nepali",
    val allergies: String = "None",
    val dailyStepGoal: Int = 8000,
    val dailyCalorieTarget: Int = 2000,
    val dailyWaterTargetMl: Int = 2500,
    val sleepTargetHours: Float = 8f,
    val isAdult: Boolean = true,
    val onboardingCompleted: Boolean = false
)

@Entity(tableName = "user_measurement_preferences")
data class UserMeasurementPreferences(
    @PrimaryKey val id: Int = 1,
    val weightUnit: String = "kg", // "kg", "g", "lb", "st"
    val heightUnit: String = "cm", // "cm", "m", "ft_in", "in"
    val foodUnit: String = "g", // "g", "oz", "serving", "cup"
    val waterUnit: String = "ml", // "ml", "L", "fl_oz", "glass", "bottle"
    val distanceUnit: String = "km", // "km", "mi", "m"
    val usualGlassSizeMl: Int = 250,
    val usualBottleSizeMl: Int = 750
)

@Entity(tableName = "monthly_goal_targets")
data class MonthlyGoalTarget(
    @PrimaryKey val monthKey: String, // Format: "YYYY-MM" (e.g., "2026-10")
    val targetWeightChangeKg: Float = -1.5f,
    val targetWalkingDays: Int = 20,
    val targetMonthlyWalkingDistanceKm: Float = 60f,
    val targetWorkoutSessions: Int = 15,
    val dailyCalorieTarget: Int = 2000,
    val targetDaysMeetingNutritionGoal: Int = 20
)

@Entity(tableName = "meal_entries")
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodItemId: Long = 0,
    val dateString: String, // YYYY-MM-DD
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snacks"
    val foodName: String,
    val nepaliName: String = "",
    val portionDescription: String = "1 serving",
    val quantity: Float = 1f,
    val unit: String = "serving",
    val normalizedWeightGrams: Float = 200f,
    val portionGrams: Float = 200f,
    val calories: Int,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 0f,
    val imageUri: String? = null,
    val imageReference: String? = null,
    val estimationMethod: String = "MANUAL", // "AI_VISION", "MANUAL", "BARCODE", "DATABASE"
    val confidence: Float = 1.0f,
    val userConfirmed: Boolean = true,
    val isVerified: Boolean = true,
    val preparationMethod: String = "Standard",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_database")
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val alternativeNames: String = "",
    val nepaliName: String = "",
    val category: String = "Nepali Staples",
    val servingUnit: String = "1 serving",
    val gramsPerServing: Float = 100f,
    val calories: Int = 150,
    val protein: Float = 5f,
    val carbs: Float = 20f,
    val fat: Float = 3f,
    val fiber: Float = 2f,
    val sugar: Float = 1f,
    val sodiumPer100g: Float = 50f,
    val caloriesPer100g: Float = 150f,
    val proteinPer100g: Float = 5f,
    val carbsPer100g: Float = 20f,
    val fatPer100g: Float = 3f,
    val fibrePer100g: Float = 2f,
    val sugarPer100g: Float = 1f,
    val defaultServingGrams: Float = 100f,
    val nutritionSource: String = "NutriGlass Verified DB",
    val lastUpdated: Long = System.currentTimeMillis(),
    val isNepaliDish: Boolean = true,
    val isVerified: Boolean = true,
    val barcode: String? = null
) {
    fun getPer100gCalories(): Float =
        if (caloriesPer100g > 0 && caloriesPer100g != 150f) caloriesPer100g else (calories.toFloat() / gramsPerServing.coerceAtLeast(1f)) * 100f

    fun getPer100gProtein(): Float =
        if (proteinPer100g > 0 && proteinPer100g != 5f) proteinPer100g else (protein / gramsPerServing.coerceAtLeast(1f)) * 100f

    fun getPer100gCarbs(): Float =
        if (carbsPer100g > 0 && carbsPer100g != 20f) carbsPer100g else (carbs / gramsPerServing.coerceAtLeast(1f)) * 100f

    fun getPer100gFat(): Float =
        if (fatPer100g > 0 && fatPer100g != 3f) fatPer100g else (fat / gramsPerServing.coerceAtLeast(1f)) * 100f

    fun getPer100gFiber(): Float =
        if (fibrePer100g > 0 && fibrePer100g != 2f) fibrePer100g else (fiber / gramsPerServing.coerceAtLeast(1f)) * 100f

    fun getPer100gSugar(): Float =
        if (sugarPer100g > 0 && sugarPer100g != 1f) sugarPer100g else (sugar / gramsPerServing.coerceAtLeast(1f)) * 100f
}

@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String,
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "step_records")
data class StepRecord(
    @PrimaryKey val dateString: String,
    val steps: Int = 0,
    val distanceMeters: Float = 0f,
    val caloriesBurned: Int = 0,
    val activeMinutes: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String,
    val workoutType: String,
    val durationSeconds: Long,
    val distanceMeters: Float = 0f,
    val caloriesBurned: Int,
    val paceMinPerKm: Float = 0f,
    val notes: String = "",
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weight_records")
data class WeightRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String,
    val weightKg: Float,
    val waistCm: Float? = null,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String,
    val bedtimeMillis: Long,
    val wakeTimeMillis: Long,
    val durationMinutes: Int,
    val qualityRating: Int = 4,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val code: String,
    val title: String,
    val description: String,
    val category: String,
    val level: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)

@Entity(tableName = "progress_photos")
data class ProgressPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String,
    val imageUri: String,
    val weightKg: Float,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_scan_records")
data class FoodScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodName: String,
    val nepaliName: String = "",
    val portion: String = "1 serving",
    val estimatedCalories: Int,
    val calorieRange: String = "±50 kcal",
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 50f,
    val confidence: Float = 0.90f,
    val uncertaintyNotes: String = "",
    val preparationMethod: String = "Standard",
    val itemsSummary: String = "",
    val allergens: String = "",
    val healthierAlternatives: String = "",
    val imageUri: String? = null,
    val scannedAt: Long = System.currentTimeMillis(),
    val isLogged: Boolean = false
)

// UI and Scanner Multi-Food Models
data class RecognizedFoodItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val foodName: String,
    val nepaliName: String = "",
    val preparationMethod: String = "Standard",
    val quantity: Float = 1f,
    val unit: String = "serving",
    val estimatedGrams: Float = 150f,
    val calories: Int = 200,
    val protein: Float = 5f,
    val carbs: Float = 30f,
    val fat: Float = 5f,
    val fiber: Float = 2f,
    val sugar: Float = 1f,
    val sodium: Float = 50f,
    val confidence: Float = 0.85f,
    val uncertaintyNotes: String = "",
    val ingredients: List<String> = emptyList(),
    val likelyOilsOrSauces: String = "",
    val allergens: List<String> = emptyList(),
    val alternatives: List<String> = emptyList(),
    val requiresPortionConfirmation: Boolean = false
)

data class MultiFoodScanResult(
    val mealTitle: String = "Identified Meal",
    val items: List<RecognizedFoodItem> = emptyList(),
    val totalCalories: Int = 0,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val totalFiber: Float = 0f,
    val totalSugar: Float = 0f,
    val totalSodium: Float = 0f,
    val overallConfidence: Float = 0.85f,
    val uncertaintyNotes: String = "",
    val disclaimer: String = "Nutritional values are AI-estimated approximations and should not replace clinical medical advice. Please confirm portion sizes before logging.",
    val imageUri: String? = null,
    val modelUsed: String = "gemini-2.5-flash",
    val isOfflineFallback: Boolean = false
)

data class NutritionPeriodSummary(
    val periodLabel: String,
    val totalCalories: Int,
    val averageCaloriesPerDay: Int,
    val totalProtein: Float,
    val averageProteinPerDay: Float,
    val totalCarbs: Float,
    val averageCarbsPerDay: Float,
    val totalFat: Float,
    val averageFatPerDay: Float,
    val totalFiber: Float,
    val totalSodium: Float,
    val totalWaterMl: Int,
    val averageWaterMlPerDay: Int,
    val daysLogged: Int,
    val targetMetDays: Int
)

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriString: String,
    val title: String,
    val author: String = "Unknown Author",
    val pageCount: Int = 1,
    val lastPageRead: Int = 0,
    val readingProgressPct: Float = 0f,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val speechSpeed: Float = 1.0f,
    val languageCode: String = "en",
    val coverImageUri: String? = null,
    val totalReadingTimeSeconds: Long = 0,
    val totalWalkingListeningMeters: Float = 0f
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val pageNumber: Int,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_sessions")
data class ReadingSessionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val bookTitle: String,
    val dateString: String,
    val durationSeconds: Long,
    val pagesReadCount: Int,
    val distanceWalkedMeters: Float = 0f,
    val caloriesBurned: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audio_items")
data class AudioItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriString: String,
    val title: String,
    val artistOrAuthor: String = "Unknown Artist",
    val category: String = "Audiobooks", // "Audiobooks", "Music", "Speeches & Podcasts", "Guided Walking"
    val durationMs: Long = 0L,
    val lastPlaybackPositionMs: Long = 0L,
    val isFavorite: Boolean = false,
    val isOnline: Boolean = false,
    val sourceLicenseInfo: String = "Local File / Open License",
    val coverArtUri: String? = null,
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "voice_coaching_preferences")
data class VoiceCoachingPreferences(
    @PrimaryKey val id: Int = 1,
    val isCoachingEnabled: Boolean = true,
    val announcementFrequencyKm: Float = 1.0f,
    val announcementFrequencyMinutes: Int = 5,
    val announceDistance: Boolean = true,
    val announceTime: Boolean = true,
    val announceTargetProgress: Boolean = true,
    val languageCode: String = "en"
)



