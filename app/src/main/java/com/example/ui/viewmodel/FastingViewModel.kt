package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.FastingHistoryEntity
import com.example.data.model.FastingProfileEntity
import com.example.data.model.FastingReminderEntity
import com.example.data.model.FastingScheduleEntity
import com.example.data.model.FastingSessionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FastingAnalyticsSummary(
    val completedSessions: Int = 0,
    val missedSessions: Int = 0,
    val consistencyPercentage: Float? = null,
    val averageDurationHours: Float? = null
)

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

    /**
     * Calculate the last seven days from actual saved sessions.
     * Active, paused and cancelled sessions are excluded from the consistency denominator.
     */
    val weeklyAnalytics: StateFlow<FastingAnalyticsSummary> = sessions
        .map { allSessions ->
            val now = System.currentTimeMillis()
            val cutoff = now - 7L * 24L * 60L * 60L * 1000L
            val recent = allSessions.filter { it.startTime in cutoff..now }
            val completed = recent.filter { it.status.equals("COMPLETED", ignoreCase = true) }
            val missed = recent.filter { it.status.equals("MISSED", ignoreCase = true) }
            val counted = completed.size + missed.size
            val durations = completed.mapNotNull { session ->
                val minutes = if (session.durationMinutes > 0L) {
                    session.durationMinutes
                } else {
                    val end = session.actualEndTime ?: return@mapNotNull null
                    ((end - session.startTime) / 60000L).coerceAtLeast(0L)
                }
                minutes.takeIf { it > 0L }?.div(60f)
            }

            FastingAnalyticsSummary(
                completedSessions = completed.size,
                missedSessions = missed.size,
                consistencyPercentage = if (counted > 0) completed.size * 100f / counted else null,
                averageDurationHours = durations.takeIf { it.isNotEmpty() }?.average()?.toFloat()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FastingAnalyticsSummary())

    fun updateProfile(profile: FastingProfileEntity) {
        viewModelScope.launch { dao.saveProfile(profile) }
    }

    fun startFasting(protocolId: String, fastingHours: Int) {
        viewModelScope.launch {
            if (dao.getActiveSession() != null || fastingHours <= 0) return@launch
            val now = System.currentTimeMillis()
            val targetEnd = now + (fastingHours * 3600L * 1000L)
            dao.insertSession(
                FastingSessionEntity(
                    protocolId = protocolId,
                    startTime = now,
                    targetEndTime = targetEnd,
                    status = "ACTIVE"
                )
            )
        }
    }

    fun endFastingSession(
        sessionId: Long,
        actualDurationMinutes: Long,
        hunger: Int,
        energy: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val session = dao.getActiveSession()
            if (session != null && session.id == sessionId) {
                val endedAt = System.currentTimeMillis()
                val safeMinutes = actualDurationMinutes.coerceAtLeast(0L)
                dao.updateSession(
                    session.copy(
                        actualEndTime = endedAt,
                        durationMinutes = safeMinutes,
                        status = "COMPLETED",
                        hungerLevel = hunger.coerceIn(1, 10),
                        energyLevel = energy.coerceIn(1, 10),
                        notes = notes
                    )
                )

                val currentWeight = profile.value?.weightKg?.takeIf { it > 0f } ?: 0f
                dao.insertHistory(
                    FastingHistoryEntity(
                        dateString = android.text.format.DateFormat.format("yyyy-MM-dd", endedAt).toString(),
                        protocolId = session.protocolId,
                        plannedFastingHours = ((session.targetEndTime - session.startTime) / 3600000L).toInt(),
                        actualFastingHours = safeMinutes / 60f,
                        status = "COMPLETED",
                        weightKg = currentWeight,
                        energyFeedback = "Energy: ${energy.coerceIn(1, 10)}/10, Hunger: ${hunger.coerceIn(1, 10)}/10"
                    )
                )
            }
        }
    }

    fun cancelOrPauseSession(sessionId: Long, newStatus: String) {
        viewModelScope.launch {
            if (newStatus !in setOf("PAUSED", "CANCELLED", "MISSED")) return@launch
            val session = dao.getActiveSession()
            if (session != null && session.id == sessionId) {
                dao.updateSession(session.copy(status = newStatus))
            }
        }
    }

    fun saveReminder(reminder: FastingReminderEntity) {
        viewModelScope.launch { dao.saveReminder(reminder) }
    }
}
