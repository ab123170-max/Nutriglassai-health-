package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.example.data.model.FastingProfileEntity
import com.example.data.model.FastingScheduleEntity
import com.example.data.model.FastingSessionEntity
import com.example.data.model.FastingHistoryEntity
import com.example.data.model.FastingReminderEntity
import com.example.data.model.FastingProgressEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        com.example.data.model.UserMeasurementPreferences::class,
        com.example.data.model.MonthlyGoalTarget::class,
        MealEntry::class,
        FoodItem::class,
        WaterEntry::class,
        StepRecord::class,
        WorkoutSession::class,
        WeightRecord::class,
        SleepRecord::class,
        Achievement::class,
        ProgressPhoto::class,
        FoodScanRecord::class,
        com.example.data.model.BookEntity::class,
        com.example.data.model.BookmarkEntity::class,
        com.example.data.model.ReadingSessionRecord::class,
        com.example.data.model.AudioItemEntity::class,
        com.example.data.model.VoiceCoachingPreferences::class,
        com.example.data.model.WeeklyCheckInEntity::class,
        com.example.data.model.CheckInQuestionnaireEntity::class,
        com.example.data.model.WeeklyPlanEntity::class,
        com.example.data.model.WeeklyCheckInPreferences::class,
        com.example.data.model.BodyCompositionRecord::class,
        com.example.data.model.BodyTwinSnapshot::class,
        com.example.data.model.BodyTwinConsentPreferences::class,
        FastingProfileEntity::class,
        FastingScheduleEntity::class,
        FastingSessionEntity::class,
        FastingHistoryEntity::class,
        FastingReminderEntity::class,
        FastingProgressEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao
    abstract fun foodDao(): FoodDao
    abstract fun healthDao(): HealthDao
    abstract fun achievementDao(): AchievementDao
    abstract fun fastingDao(): FastingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Version 9 and 10 retain the same table structure. Preserve all rows while
         * updating Room's version. Replace only the old untouched 70 kg placeholder
         * with the canonical onboarding weight when that profile exists.
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE fasting_profile
                    SET weightKg = (SELECT weightKg FROM user_profiles WHERE id = 1)
                    WHERE id = 1
                      AND weightKg = 70.0
                      AND EXISTS (SELECT 1 FROM user_profiles WHERE id = 1)
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutriglass_health.db"
                )
                    .addMigrations(MIGRATION_9_10)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.foodDao().insertFoods(PreloadedData.nepaliFoodItems)
                                    database.achievementDao().initAchievements(PreloadedData.allAchievements)
                                    database.healthDao().saveUserProfile(UserProfile())
                                    database.healthDao().saveMeasurementPreferences(
                                        com.example.data.model.UserMeasurementPreferences()
                                    )
                                    database.fastingDao().insertSchedules(FastingPreloadedData.defaultSchedules)
                                    database.fastingDao().saveProfile(FastingProfileEntity())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
