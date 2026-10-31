package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.BodyCompositionRecord
import com.example.data.model.BodyTwinConsentPreferences
import com.example.data.model.BodyTwinSnapshot
import com.example.data.model.UserMeasurementPreferences
import com.example.data.model.UserProfile
import com.example.data.model.WaterEntry
import com.example.data.model.WeightRecord
import com.example.domain.BodyCompositionEngine
import com.example.domain.BodyCompositionResult
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeasurementRepository(private val database: AppDatabase) {

    private val healthDao = database.healthDao()

    fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Profiles & Preferences
    val userProfile: Flow<UserProfile?> = healthDao.getUserProfile()
    val measurementPreferences: Flow<UserMeasurementPreferences?> = healthDao.getMeasurementPreferences()

    suspend fun saveProfile(profile: UserProfile) {
        healthDao.saveUserProfile(profile)
    }

    suspend fun saveMeasurementPreferences(preferences: UserMeasurementPreferences) {
        healthDao.saveMeasurementPreferences(preferences)
    }

    // Weight Records
    val weightHistory: Flow<List<WeightRecord>> = healthDao.getWeightHistory()
    val latestWeight: Flow<WeightRecord?> = healthDao.getLatestWeight()

    suspend fun logWeight(weightKg: Float, waistCm: Float? = null, notes: String = ""): Long {
        return healthDao.insertWeight(
            WeightRecord(
                dateString = getTodayString(),
                weightKg = weightKg,
                waistCm = waistCm,
                notes = notes,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteWeight(id: Long) = healthDao.deleteWeight(id)

    // Body Composition Records
    val latestBodyComposition: Flow<BodyCompositionRecord?> = healthDao.getLatestBodyComposition()
    val allBodyCompositions: Flow<List<BodyCompositionRecord>> = healthDao.getAllBodyCompositions()

    fun getBodyCompositionForDate(date: String): Flow<BodyCompositionRecord?> =
        healthDao.getBodyCompositionForDate(date)

    suspend fun recordBodyComposition(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        gender: String,
        waistCm: Float? = null,
        neckCm: Float? = null,
        hipCm: Float? = null,
        chestCm: Float? = null,
        armCm: Float? = null,
        thighCm: Float? = null,
        calfCm: Float? = null,
        notes: String = ""
    ): Long {
        val analysis = BodyCompositionEngine.analyze(
            weightKg = weightKg,
            heightCm = heightCm,
            age = age,
            gender = gender,
            waistCm = waistCm,
            neckCm = neckCm,
            hipCm = hipCm
        )

        val record = BodyCompositionRecord(
            dateString = getTodayString(),
            timestamp = System.currentTimeMillis(),
            weightKg = weightKg,
            heightCm = heightCm,
            age = age,
            gender = gender,
            waistCm = waistCm,
            neckCm = neckCm,
            hipCm = hipCm,
            chestCm = chestCm,
            armCm = armCm,
            thighCm = thighCm,
            calfCm = calfCm,
            bmi = analysis.bmi,
            bodyFatPercentage = analysis.bodyFatPercentage,
            fatMassKg = analysis.fatMassKg,
            leanMassKg = analysis.leanMassKg,
            calculationMethod = analysis.calculationMethod.name,
            notes = notes
        )

        val id = healthDao.insertBodyComposition(record)

        // Also record to weight history so overall weight trend stays in sync
        healthDao.insertWeight(
            WeightRecord(
                dateString = getTodayString(),
                weightKg = weightKg,
                waistCm = waistCm,
                notes = "Body Composition Log",
                timestamp = System.currentTimeMillis()
            )
        )

        return id
    }

    suspend fun deleteBodyComposition(id: Long) = healthDao.deleteBodyComposition(id)

    // Body Twin & 3D Transformation
    val allBodyTwinSnapshots: Flow<List<BodyTwinSnapshot>> = healthDao.getAllBodyTwinSnapshots()
    val historicalBodyTwinSnapshots: Flow<List<BodyTwinSnapshot>> = healthDao.getHistoricalBodyTwinSnapshots()
    val latestBodyTwinSnapshot: Flow<BodyTwinSnapshot?> = healthDao.getLatestBodyTwinSnapshot()
    val consentPreferences: Flow<BodyTwinConsentPreferences?> = healthDao.getBodyTwinConsentPreferences()

    suspend fun saveBodyTwinSnapshot(snapshot: BodyTwinSnapshot): Long {
        return healthDao.insertBodyTwinSnapshot(snapshot)
    }

    suspend fun deleteBodyTwinSnapshot(id: Long) = healthDao.deleteBodyTwinSnapshot(id)

    suspend fun saveConsentPreferences(prefs: BodyTwinConsentPreferences) {
        healthDao.saveBodyTwinConsentPreferences(prefs)
    }

    suspend fun purgeAllBodyTwinData() {
        healthDao.deleteAllBodyTwinSnapshots()
        healthDao.saveBodyTwinConsentPreferences(BodyTwinConsentPreferences(hasAcceptedPrivacyConsent = false))
    }

    // Water
    fun getWaterForDate(date: String): Flow<List<WaterEntry>> = healthDao.getWaterForDate(date)

    suspend fun logWater(amountMl: Int): Long {
        return healthDao.insertWater(
            WaterEntry(
                dateString = getTodayString(),
                amountMl = amountMl,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeLatestWater(date: String = getTodayString()) {
        healthDao.removeLatestWater(date)
    }
}
