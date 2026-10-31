package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BodyCompositionRecord
import com.example.data.model.BodyTwinConsentPreferences
import com.example.data.model.BodyTwinSnapshot
import com.example.data.model.UserMeasurementPreferences
import com.example.data.model.UserProfile
import com.example.data.repository.MeasurementRepository
import com.example.domain.BodyCompositionEngine
import com.example.domain.BodyCompositionResult
import com.example.domain.BodyTwinEngine
import com.example.domain.BodyTwinPrediction
import com.example.domain.CircumferenceUnit
import com.example.domain.HeightUnit
import com.example.domain.MeasurementUnitManager
import com.example.domain.MuscleMassCategory
import com.example.domain.TransformationMilestone
import com.example.domain.VisceralFatCategory
import com.example.domain.WaistToHeightCategory
import com.example.domain.WeightUnit
import com.example.ui.components.body3d.BodyZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AvatarViewPerspective(val title: String) {
    CURRENT("Current Body"),
    STARTING("Starting Baseline"),
    GOAL("Target Goal"),
    PREDICTION_3M("3-Month AI Future"),
    PREDICTION_6M("6-Month AI Future")
}

data class BodyCompositionUiState(
    val weightKg: Float = 70f,
    val startingWeightKg: Float = 75f,
    val targetWeightKg: Float = 65f,
    val heightCm: Float = 175f,
    val age: Int = 26,
    val gender: String = "Male",
    val waistCm: Float? = 82f,
    val startingWaistCm: Float? = 86f,
    val neckCm: Float? = 38f,
    val hipCm: Float? = 95f,
    val chestCm: Float? = 96f,
    val armCm: Float? = 33f,
    val thighCm: Float? = 55f,
    val calfCm: Float? = 37f,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val heightUnit: HeightUnit = HeightUnit.CM,
    val circumferenceUnit: CircumferenceUnit = CircumferenceUnit.CM,
    val selectedZone: BodyZone = BodyZone.ALL,
    val isMalePreview: Boolean = true,
    val activePerspective: AvatarViewPerspective = AvatarViewPerspective.CURRENT,
    val isInputDialogVisible: Boolean = false,
    val isPrivacyConsentDialogVisible: Boolean = false,
    val celebrationMilestone: TransformationMilestone? = null,
    val isPrivacyConsentAccepted: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BodyCompositionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MeasurementRepository(AppDatabase.getDatabase(application))
    val unitManager = MeasurementUnitManager()

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .flatMapLatest { profile ->
            MutableStateFlow(profile ?: UserProfile())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val measurementPreferences: StateFlow<UserMeasurementPreferences> = repository.measurementPreferences
        .flatMapLatest { prefs ->
            val p = prefs ?: UserMeasurementPreferences()
            unitManager.updateFromPreferences(p)
            MutableStateFlow(p)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserMeasurementPreferences())

    val activeUnits = unitManager.activeUnits

    val allRecords: StateFlow<List<BodyCompositionRecord>> = repository.allBodyCompositions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestRecord: StateFlow<BodyCompositionRecord?> = repository.latestBodyComposition
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSnapshots: StateFlow<List<BodyTwinSnapshot>> = repository.allBodyTwinSnapshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val consentPreferences: StateFlow<BodyTwinConsentPreferences?> = repository.consentPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(BodyCompositionUiState())
    val uiState: StateFlow<BodyCompositionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(userProfile, latestRecord, activeUnits, consentPreferences) { profile, record, units, consent ->
                val currentWeight = record?.weightKg ?: profile.weightKg
                val currentHeight = record?.heightCm ?: profile.heightCm
                val isMale = (record?.gender ?: profile.gender).equals("Male", ignoreCase = true)

                _uiState.value = _uiState.value.copy(
                    weightKg = currentWeight,
                    startingWeightKg = profile.weightKg + 2.5f,
                    targetWeightKg = profile.targetWeightKg,
                    heightCm = currentHeight,
                    age = record?.age ?: profile.age,
                    gender = record?.gender ?: profile.gender,
                    waistCm = record?.waistCm ?: 82f,
                    neckCm = record?.neckCm ?: 38f,
                    hipCm = record?.hipCm ?: 95f,
                    chestCm = record?.chestCm ?: 96f,
                    armCm = record?.armCm ?: 33f,
                    thighCm = record?.thighCm ?: 55f,
                    calfCm = record?.calfCm ?: 37f,
                    weightUnit = units.weightUnit,
                    heightUnit = units.heightUnit,
                    circumferenceUnit = units.circumferenceUnit,
                    isMalePreview = isMale,
                    isPrivacyConsentAccepted = consent?.hasAcceptedPrivacyConsent ?: false
                )
            }.collect {}
        }
    }

    // Dynamic current analysis derived from active state
    val currentAnalysis: StateFlow<BodyCompositionResult> = combine(
        uiState,
        latestRecord
    ) { state, _ ->
        BodyCompositionEngine.analyze(
            weightKg = state.weightKg,
            heightCm = state.heightCm,
            age = state.age,
            gender = state.gender,
            targetWeightKg = state.targetWeightKg,
            waistCm = state.waistCm,
            neckCm = state.neckCm,
            hipCm = state.hipCm
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BodyCompositionEngine.analyze(70f, 175f, 26, "Male")
    )

    // Dynamic target analysis
    val targetAnalysis: StateFlow<BodyCompositionResult> = combine(
        uiState,
        currentAnalysis
    ) { state, _ ->
        BodyCompositionEngine.analyze(
            weightKg = state.targetWeightKg,
            heightCm = state.heightCm,
            age = state.age,
            gender = state.gender,
            targetWeightKg = state.targetWeightKg
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BodyCompositionEngine.analyze(65f, 175f, 26, "Male")
    )

    // Skeletal Muscle Mass (Janssen Formula)
    val skeletalMuscleMassKg: StateFlow<Float> = combine(uiState, currentAnalysis) { state, _ ->
        BodyTwinEngine.calculateSkeletalMuscleMass(
            weightKg = state.weightKg,
            heightCm = state.heightCm,
            age = state.age,
            isMale = state.isMalePreview
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 28.5f)

    val muscleMassPercent: StateFlow<Float> = combine(skeletalMuscleMassKg, uiState) { smm, state ->
        BodyTwinEngine.calculateMuscleMassPercentage(smm, state.weightKg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 38.0f)

    val muscleMassCategory: StateFlow<MuscleMassCategory> = combine(muscleMassPercent, uiState) { pct, state ->
        BodyTwinEngine.getMuscleMassCategory(pct, state.isMalePreview)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MuscleMassCategory.NORMAL)

    // Clinical Visceral Fat Index (1 to 20 scale)
    val visceralFatIndex: StateFlow<Float> = combine(uiState, currentAnalysis) { state, analysis ->
        BodyTwinEngine.calculateVisceralFatIndex(
            waistCm = state.waistCm,
            heightCm = state.heightCm,
            bmi = analysis.bmi,
            age = state.age
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7.5f)

    val visceralFatCategory: StateFlow<VisceralFatCategory> = visceralFatIndex.flatMapLatest { vfi ->
        MutableStateFlow(VisceralFatCategory.fromRating(vfi))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VisceralFatCategory.BALANCED)

    // Waist to Height Ratio
    val waistToHeightRatio: StateFlow<Float?> = uiState.flatMapLatest { state ->
        val ratio = state.waistCm?.let { BodyTwinEngine.calculateWaistToHeightRatio(it, state.heightCm) }
        MutableStateFlow(ratio)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.47f)

    val waistToHeightCategory: StateFlow<WaistToHeightCategory?> = waistToHeightRatio.flatMapLatest { ratio ->
        MutableStateFlow(ratio?.let { WaistToHeightCategory.fromRatio(it) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WaistToHeightCategory.OPTIMAL)

    // AI Future Predictions (3-Month & 6-Month)
    val prediction3M: StateFlow<BodyTwinPrediction> = combine(uiState, currentAnalysis) { state, analysis ->
        BodyTwinEngine.predictFutureBody(
            currentWeightKg = state.weightKg,
            heightCm = state.heightCm,
            currentBfPercent = analysis.bodyFatPercentage,
            currentWaistCm = state.waistCm,
            age = state.age,
            isMale = state.isMalePreview,
            dailyCalorieDeficitKcal = 500,
            timeframeMonths = 3
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BodyTwinEngine.predictFutureBody(70f, 175f, 20f, 82f, 26, true, 500, 3)
    )

    val prediction6M: StateFlow<BodyTwinPrediction> = combine(uiState, currentAnalysis) { state, analysis ->
        BodyTwinEngine.predictFutureBody(
            currentWeightKg = state.weightKg,
            heightCm = state.heightCm,
            currentBfPercent = analysis.bodyFatPercentage,
            currentWaistCm = state.waistCm,
            age = state.age,
            isMale = state.isMalePreview,
            dailyCalorieDeficitKcal = 500,
            timeframeMonths = 6
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BodyTwinEngine.predictFutureBody(70f, 175f, 20f, 82f, 26, true, 500, 6)
    )

    // 20 Gamification Transformation Milestones
    val milestones: StateFlow<List<TransformationMilestone>> = combine(
        uiState,
        currentAnalysis,
        skeletalMuscleMassKg,
        visceralFatIndex,
        allSnapshots
    ) { state, analysis, smm, vfi, snapshots ->
        BodyTwinEngine.evaluateMilestones(
            startingWeightKg = state.startingWeightKg,
            currentWeightKg = state.weightKg,
            targetWeightKg = state.targetWeightKg,
            startingWaistCm = state.startingWaistCm,
            currentWaistCm = state.waistCm,
            bodyFatPercent = analysis.bodyFatPercentage,
            startingBfPercent = analysis.bodyFatPercentage + 2.5f, // Baseline comparison
            muscleMassKg = smm,
            startingMuscleMassKg = smm - 0.5f,
            visceralFatIndex = vfi,
            totalSnapshotsCount = snapshots.size,
            weeklyCheckInsCount = snapshots.count { it.snapshotType == "WEEKLY_CHECKIN" },
            hasProgressPhoto = snapshots.any { !it.photoPath.isNullOrEmpty() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAvatarPerspective(perspective: AvatarViewPerspective) {
        _uiState.value = _uiState.value.copy(activePerspective = perspective)
    }

    fun setWeightUnit(unit: WeightUnit) {
        unitManager.setWeightUnit(unit)
        _uiState.value = _uiState.value.copy(weightUnit = unit)
        viewModelScope.launch {
            val currentPrefs = measurementPreferences.value
            repository.saveMeasurementPreferences(currentPrefs.copy(weightUnit = unit.symbol))
        }
    }

    fun setHeightUnit(unit: HeightUnit) {
        unitManager.setHeightUnit(unit)
        _uiState.value = _uiState.value.copy(heightUnit = unit)
        viewModelScope.launch {
            val currentPrefs = measurementPreferences.value
            repository.saveMeasurementPreferences(currentPrefs.copy(heightUnit = unit.symbol))
        }
    }

    fun setCircumferenceUnit(unit: CircumferenceUnit) {
        unitManager.setCircumferenceUnit(unit)
        _uiState.value = _uiState.value.copy(circumferenceUnit = unit)
        viewModelScope.launch {
            val currentPrefs = measurementPreferences.value
            repository.saveMeasurementPreferences(currentPrefs.copy(bodyMeasurementUnit = unit.symbol))
        }
    }

    fun selectZone(zone: BodyZone) {
        _uiState.value = _uiState.value.copy(
            selectedZone = if (_uiState.value.selectedZone == zone) BodyZone.ALL else zone
        )
    }

    fun toggleGenderPreview(isMale: Boolean) {
        _uiState.value = _uiState.value.copy(isMalePreview = isMale)
    }

    fun showInputDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(isInputDialogVisible = show)
    }

    fun showPrivacyConsentDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(isPrivacyConsentDialogVisible = show)
    }

    fun acceptPrivacyConsent() {
        viewModelScope.launch {
            repository.saveConsentPreferences(
                BodyTwinConsentPreferences(
                    id = 1,
                    hasAcceptedPrivacyConsent = true,
                    consentedTimestamp = System.currentTimeMillis(),
                    allowLocalPhotoStorage = true
                )
            )
            _uiState.value = _uiState.value.copy(
                isPrivacyConsentAccepted = true,
                isPrivacyConsentDialogVisible = false
            )
        }
    }

    fun saveMeasurements(
        weightKg: Float,
        heightCm: Float,
        targetWeightKg: Float,
        waistCm: Float?,
        neckCm: Float?,
        hipCm: Float?,
        chestCm: Float?,
        armCm: Float?,
        thighCm: Float?,
        calfCm: Float?,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.recordBodyComposition(
                weightKg = weightKg,
                heightCm = heightCm,
                age = _uiState.value.age,
                gender = _uiState.value.gender,
                waistCm = waistCm,
                neckCm = neckCm,
                hipCm = hipCm,
                chestCm = chestCm,
                armCm = armCm,
                thighCm = thighCm,
                calfCm = calfCm,
                notes = notes
            )

            // Update user profile so entire app is kept synchronized
            val currentP = userProfile.value
            repository.saveProfile(
                currentP.copy(
                    weightKg = weightKg,
                    heightCm = heightCm,
                    targetWeightKg = targetWeightKg
                )
            )

            // Create automatic snapshot checkpoint
            val analysis = BodyCompositionEngine.analyze(
                weightKg = weightKg,
                heightCm = heightCm,
                age = _uiState.value.age,
                gender = _uiState.value.gender,
                waistCm = waistCm,
                neckCm = neckCm,
                hipCm = hipCm
            )
            val smm = BodyTwinEngine.calculateSkeletalMuscleMass(
                weightKg, heightCm, _uiState.value.age, _uiState.value.isMalePreview
            )
            val vfi = BodyTwinEngine.calculateVisceralFatIndex(
                waistCm, heightCm, analysis.bmi, _uiState.value.age
            )

            repository.saveBodyTwinSnapshot(
                BodyTwinSnapshot(
                    dateString = repository.getTodayString(),
                    snapshotType = "WEEKLY_CHECKIN",
                    title = "Progress Snapshot: ${String.format("%.1f", weightKg)} kg",
                    weightKg = weightKg,
                    heightCm = heightCm,
                    bodyFatPercent = analysis.bodyFatPercentage,
                    muscleMassKg = smm,
                    waistCm = waistCm,
                    neckCm = neckCm,
                    hipCm = hipCm,
                    visceralFatIndex = vfi,
                    notes = notes
                )
            )

            _uiState.value = _uiState.value.copy(
                weightKg = weightKg,
                heightCm = heightCm,
                targetWeightKg = targetWeightKg,
                waistCm = waistCm,
                neckCm = neckCm,
                hipCm = hipCm,
                chestCm = chestCm,
                armCm = armCm,
                thighCm = thighCm,
                calfCm = calfCm,
                isInputDialogVisible = false
            )
        }
    }

    fun savePhotoSnapshot(photoPath: String, title: String = "Vault Photo Snapshot") {
        viewModelScope.launch {
            val state = _uiState.value
            val analysis = currentAnalysis.value
            val smm = skeletalMuscleMassKg.value
            val vfi = visceralFatIndex.value

            repository.saveBodyTwinSnapshot(
                BodyTwinSnapshot(
                    dateString = repository.getTodayString(),
                    snapshotType = "USER_PHOTO",
                    title = title,
                    weightKg = state.weightKg,
                    heightCm = state.heightCm,
                    bodyFatPercent = analysis.bodyFatPercentage,
                    muscleMassKg = smm,
                    waistCm = state.waistCm,
                    neckCm = state.neckCm,
                    hipCm = state.hipCm,
                    visceralFatIndex = vfi,
                    photoPath = photoPath,
                    notes = "Encrypted local vault photo"
                )
            )
        }
    }

    fun deleteSnapshot(id: Long) {
        viewModelScope.launch {
            repository.deleteBodyTwinSnapshot(id)
        }
    }

    fun purgeAllBodyTwinData() {
        viewModelScope.launch {
            repository.purgeAllBodyTwinData()
            _uiState.value = _uiState.value.copy(
                isPrivacyConsentAccepted = false,
                celebrationMilestone = null
            )
        }
    }

    fun triggerCelebration(milestone: TransformationMilestone) {
        _uiState.value = _uiState.value.copy(celebrationMilestone = milestone)
    }

    fun dismissCelebration() {
        _uiState.value = _uiState.value.copy(celebrationMilestone = null)
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteBodyComposition(id)
        }
    }
}
