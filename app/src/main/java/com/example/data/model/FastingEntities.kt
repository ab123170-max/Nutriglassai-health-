package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fasting_profile")
data class FastingProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val age: Int = 30,
    val biologicalSex: String = "Not Specified",
    val heightCm: Float = 170.0f,
    // Zero means the user has not supplied a verified weight yet.
    val weightKg: Float = 0f,
    val activityLevel: String = "Moderate",
    val dietaryPreferences: String = "Balanced",
    val hasMedicalLimitations: Boolean = false,
    val hasDiabetesOrHypoglycemiaMeds: Boolean = false,
    val hasEatingDisorderHistory: Boolean = false,
    val isPregnantOrBreastfeeding: Boolean = false,
    val sleepHours: Float = 7.5f,
    val workHours: String = "9 AM - 5 PM",
    val selectedProtocolId: String = "16_8",
    val fastingStartTime: String = "20:00",
    val eatingStartTime: String = "12:00",
    val eatingEndTime: String = "20:00",
    val mealsPerDay: Int = 3,
    val mealIntervalHours: Float = 4.0f,
    val safetyScreeningPassed: Boolean = true,
    val fastingOptedOut: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "fasting_schedule")
data class FastingScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val protocolId: String,
    val protocolName: String,
    val fastingHours: Int,
    val eatingHours: Int,
    val difficulty: String,
    val safetyInfo: String,
    val isRecommended: Boolean = false
)

@Entity(tableName = "fasting_session")
data class FastingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val protocolId: String,
    val startTime: Long,
    val targetEndTime: Long,
    val actualEndTime: Long? = null,
    val durationMinutes: Long = 0L,
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, PAUSED, CANCELLED, MISSED
    val moodFeedback: String = "",
    val hungerLevel: Int = 5, // 1-10
    val energyLevel: Int = 5, // 1-10
    val notes: String = ""
)

@Entity(tableName = "fasting_history")
data class FastingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateString: String, // YYYY-MM-DD
    val protocolId: String,
    val plannedFastingHours: Int,
    val actualFastingHours: Float,
    val status: String, // COMPLETED, MISSED, MODIFIED
    val caloriesConsumed: Int = 0,
    val proteinConsumed: Int = 0,
    val weightKg: Float = 0f,
    val energyFeedback: String = ""
)

@Entity(tableName = "fasting_reminder")
data class FastingReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val reminderType: String, // FASTING_START, EATING_OPEN, MEAL_REMINDER, HYDRATION, COMPLETION
    val reminderTime: String, // HH:mm
    val isEnabled: Boolean = true,
    val message: String
)

@Entity(tableName = "fasting_progress")
data class FastingProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val weekNumber: Int,
    val fastingConsistencyPercentage: Float,
    val averageFastingDurationHours: Float,
    val eatingWindowAdherencePercentage: Float,
    val weightTrendKg: Float,
    val nutritionQualityScore: Float,
    val exercisePerformanceScore: Float,
    val nextWeekRecommendation: String,
    val timestamp: Long = System.currentTimeMillis()
)
