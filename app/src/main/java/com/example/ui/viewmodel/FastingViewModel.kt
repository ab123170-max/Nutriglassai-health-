package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.FastingHistoryEntity
import com.example.data.model.FastingProfileEntity
import com.example.data.model.FastingProgressEntity
import com.example.data.model.FastingReminderEntity
import com.example.data.model.FastingScheduleEntity
import com.example.data.model.FastingSessionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FastingViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).fastingDao()

    val profile: StateFlow<FastingProfileEntity?> = dao.getProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FastingProfileEntity())

    val schedules: StateFlow<List<FastingScheduleEntity>> = dao.getAllSchedulesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<FastingSessionEntity?> = dao.getActiveSessionFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sessions: StateFlow<List<FastingSessionEntity>> = dao.getAllSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<FastingHistoryEntity>> = dao.getHistoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<FastingReminderEntity>> = dao.getRemindersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestProgress: StateFlow<FastingProgressEntity?> = dao.getLatestProgressFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateProfile(profile: FastingProfileEntity) {
        viewModelScope.launch {
            dao.saveProfile(profile)
        }
    }

    fun startFasting(protocolId: String, fastingHours: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val targetEnd = now + (fastingHours * 3600L * 1000L)
            val session = FastingSessionEntity(
                protocolId = protocolId,
                startTime = now,
                targetEndTime = targetEnd,
                status = "ACTIVE"
            )
            dao.insertSession(session)
        }
    }

    fun endFastingSession(sessionId: Long, actualDurationMinutes: Long, hunger: Int, energy: Int, notes: String) {
        viewModelScope.launch {
            val session = dao.getActiveSession()
            if (session != null && session.id == sessionId) {
                val updated = session.copy(
                    actualEndTime = System.currentTimeMillis(),
                    durationMinutes = actualDurationMinutes,
                    status = "COMPLETED",
                    hungerLevel = hunger,
                    energyLevel = energy,
                    notes = notes
                )
                dao.updateSession(updated)

                // Add to history
                val historyEntry = FastingHistoryEntity(
                    dateString = android.text.format.DateFormat.format("yyyy-MM-dd", System.currentTimeMillis()).toString(),
                    protocolId = session.protocolId,
                    plannedFastingHours = ((session.targetEndTime - session.startTime) / 3600000).toInt(),
                    actualFastingHours = actualDurationMinutes / 60f,
                    status = "COMPLETED",
                    weightKg = profile.value?.weightKg ?: 70f,
                    energyFeedback = "Energy: $energy/10, Hunger: $hunger/10"
                )
                dao.insertHistory(historyEntry)
            }
        }
    }

    fun cancelOrPauseSession(sessionId: Long, newStatus: String) {
        viewModelScope.launch {
            val session = dao.getActiveSession()
            if (session != null && session.id == sessionId) {
                val updated = session.copy(status = newStatus)
                dao.updateSession(updated)
            }
        }
    }

    fun saveReminder(reminder: FastingReminderEntity) {
        viewModelScope.launch {
            dao.saveReminder(reminder)
        }
    }

    fun generateWeeklyProgress(weekNum: Int) {
        viewModelScope.launch {
            val progress = FastingProgressEntity(
                weekNumber = weekNum,
                fastingConsistencyPercentage = 85.0f,
                averageFastingDurationHours = 16.2f,
                eatingWindowAdherencePercentage = 90.0f,
                weightTrendKg = -0.6f,
                nutritionQualityScore = 8.8f,
                exercisePerformanceScore = 9.0f,
                nextWeekRecommendation = "Maintain 16:8 protocol with balanced hydration and high protein intake."
            )
            dao.insertProgress(progress)
        }
    }
}
