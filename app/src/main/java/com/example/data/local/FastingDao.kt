package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FastingHistoryEntity
import com.example.data.model.FastingProfileEntity
import com.example.data.model.FastingProgressEntity
import com.example.data.model.FastingReminderEntity
import com.example.data.model.FastingScheduleEntity
import com.example.data.model.FastingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingDao {

    @Query("SELECT * FROM fasting_profile WHERE id = 1")
    fun getProfileFlow(): Flow<FastingProfileEntity?>

    @Query("SELECT * FROM fasting_profile WHERE id = 1")
    suspend fun getProfile(): FastingProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: FastingProfileEntity)

    @Query("SELECT * FROM fasting_schedule")
    fun getAllSchedulesFlow(): Flow<List<FastingScheduleEntity>>

    @Query("SELECT * FROM fasting_schedule")
    suspend fun getAllSchedules(): List<FastingScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<FastingScheduleEntity>)

    @Query("SELECT * FROM fasting_session WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<FastingSessionEntity?>

    @Query("SELECT * FROM fasting_session WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    suspend fun getActiveSession(): FastingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FastingSessionEntity): Long

    @Update
    suspend fun updateSession(session: FastingSessionEntity)

    @Query("SELECT * FROM fasting_session ORDER BY id DESC")
    fun getAllSessionsFlow(): Flow<List<FastingSessionEntity>>

    @Query("SELECT * FROM fasting_history ORDER BY dateString DESC")
    fun getHistoryFlow(): Flow<List<FastingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: FastingHistoryEntity)

    @Query("SELECT * FROM fasting_reminder")
    fun getRemindersFlow(): Flow<List<FastingReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReminder(reminder: FastingReminderEntity)

    @Query("SELECT * FROM fasting_progress ORDER BY weekNumber DESC LIMIT 1")
    fun getLatestProgressFlow(): Flow<FastingProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: FastingProgressEntity)
}
