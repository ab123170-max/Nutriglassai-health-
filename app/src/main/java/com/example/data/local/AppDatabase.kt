package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        MealEntry::class,
        FoodItem::class,
        WaterEntry::class,
        StepRecord::class,
        WorkoutSession::class,
        WeightRecord::class,
        SleepRecord::class,
        Achievement::class,
        ProgressPhoto::class,
        FoodScanRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao
    abstract fun foodDao(): FoodDao
    abstract fun healthDao(): HealthDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutriglass_health.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                // Prepopulate Nepali food items
                                database.foodDao().insertFoods(PreloadedData.nepaliFoodItems)
                                // Prepopulate 20 achievement milestones
                                database.achievementDao().initAchievements(PreloadedData.allAchievements)
                                // Prepopulate default profile
                                database.healthDao().saveUserProfile(UserProfile())
                            }
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
