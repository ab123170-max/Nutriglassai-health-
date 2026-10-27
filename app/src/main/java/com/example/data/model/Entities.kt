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

@Entity(tableName = "meal_entries")
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // YYYY-MM-DD
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snacks"
    val foodName: String,
    val nepaliName: String = "",
    val portionDescription: String = "1 serving",
    val portionGrams: Float = 200f,
    val calories: Int,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val imageUri: String? = null,
    val isVerified: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_database")
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nepaliName: String = "",
    val category: String, // "Nepali Staples", "Dal & Pulses", "Vegetables & Saag", "Snacks & Street Food", "Proteins", "Beverages"
    val servingUnit: String,
    val gramsPerServing: Float,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val isNepaliDish: Boolean = true,
    val isVerified: Boolean = true,
    val barcode: String? = null
)

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
    val workoutType: String, // "Outdoor Walk", "Running", "Cardio HIIT", "Strength Training", "Bodyweight", "Yoga & Stretch"
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
    val qualityRating: Int = 4, // 1 to 5
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val code: String,
    val title: String,
    val description: String,
    val category: String, // "Nutrition", "Hydration", "Steps", "Workout", "Habits"
    val level: Int, // 1 to 20
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
    val confidence: Float = 0.90f,
    val uncertaintyNotes: String = "",
    val imageUri: String? = null,
    val scannedAt: Long = System.currentTimeMillis(),
    val isLogged: Boolean = false
)
