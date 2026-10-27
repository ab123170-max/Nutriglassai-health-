package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
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
import com.example.data.network.GeminiFoodService
import com.example.data.repository.HealthRepository
import com.example.domain.DailyNutritionSummary
import com.example.domain.NutritionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HealthRepository(AppDatabase.getDatabase(application))

    private val _selectedDate = MutableStateFlow(repository.getTodayString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .flatMapLatest { profile ->
            MutableStateFlow(profile ?: UserProfile())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val mealsForDate: StateFlow<List<MealEntry>> = _selectedDate
        .flatMapLatest { date -> repository.getMealsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nutritionSummary: StateFlow<DailyNutritionSummary> = combine(
        mealsForDate,
        userProfile
    ) { meals, profile ->
        NutritionEngine.summarizeDayMeals(
            meals = meals,
            calorieTarget = profile.dailyCalorieTarget,
            weightKg = profile.weightKg
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailyNutritionSummary(0, 0f, 0f, 0f, 0f, 0f, 2000, 2000, 100f, 250f, 60f)
    )

    val waterEntries: StateFlow<List<WaterEntry>> = _selectedDate
        .flatMapLatest { date -> repository.getWaterForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterTotalMl: StateFlow<Int> = waterEntries
        .flatMapLatest { entries ->
            MutableStateFlow(entries.sumOf { it.amountMl })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val stepRecord: StateFlow<StepRecord?> = _selectedDate
        .flatMapLatest { date -> repository.getStepsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val workoutsForDate: StateFlow<List<WorkoutSession>> = _selectedDate
        .flatMapLatest { date -> repository.getWorkoutsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWorkouts: StateFlow<List<WorkoutSession>> = repository.allWorkouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sleepForDate: StateFlow<SleepRecord?> = _selectedDate
        .flatMapLatest { date -> repository.getSleepForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSleepRecords: StateFlow<List<SleepRecord>> = repository.allSleepRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightHistory: StateFlow<List<WeightRecord>> = repository.weightHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestWeight: StateFlow<WeightRecord?> = repository.latestWeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allAchievements: StateFlow<List<Achievement>> = repository.allAchievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progressPhotos: StateFlow<List<ProgressPhoto>> = repository.progressPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allScans: StateFlow<List<FoodScanRecord>> = repository.allScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFoods: StateFlow<List<FoodItem>> = repository.allFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Health Coach State
    private val _aiCoachAdvice = MutableStateFlow<String?>(null)
    val aiCoachAdvice: StateFlow<String?> = _aiCoachAdvice.asStateFlow()

    private val _isAiCoachLoading = MutableStateFlow(false)
    val isAiCoachLoading: StateFlow<Boolean> = _isAiCoachLoading.asStateFlow()

    fun setDate(dateString: String) {
        _selectedDate.value = dateString
    }

    fun goToPreviousDay() {
        adjustDay(-1)
    }

    fun goToNextDay() {
        adjustDay(1)
    }

    fun goToToday() {
        _selectedDate.value = repository.getTodayString()
    }

    private fun adjustDay(days: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val date = sdf.parse(_selectedDate.value) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, days)
            }
            _selectedDate.value = sdf.format(cal.time)
        } catch (e: Exception) {
            _selectedDate.value = repository.getTodayString()
        }
    }

    fun logMeal(
        foodName: String,
        nepaliName: String,
        mealType: String,
        portionGrams: Float,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        fiber: Float,
        sugar: Float,
        portionDesc: String = "${portionGrams.toInt()}g"
    ) {
        viewModelScope.launch {
            repository.logMeal(
                MealEntry(
                    dateString = _selectedDate.value,
                    mealType = mealType,
                    foodName = foodName,
                    nepaliName = nepaliName,
                    portionDescription = portionDesc,
                    portionGrams = portionGrams,
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    fiber = fiber,
                    sugar = sugar
                )
            )
        }
    }

    fun deleteMeal(meal: MealEntry) {
        viewModelScope.launch {
            repository.deleteMeal(meal)
        }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            repository.addWater(_selectedDate.value, amountMl)
        }
    }

    fun undoWater() {
        viewModelScope.launch {
            repository.removeLatestWater(_selectedDate.value)
        }
    }

    fun logManualSteps(steps: Int) {
        viewModelScope.launch {
            val distanceKm = steps * 0.00078f // approximate step length 78cm
            val distanceM = distanceKm * 1000f
            val calories = (steps * 0.04f).toInt()
            val activeMinutes = (steps / 100).coerceAtLeast(1)
            repository.updateSteps(_selectedDate.value, steps, distanceM, calories, activeMinutes)
        }
    }

    fun logWorkout(
        type: String,
        durationSeconds: Long,
        distanceMeters: Float,
        caloriesBurned: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val pace = if (distanceMeters > 0) (durationSeconds / 60f) / (distanceMeters / 1000f) else 0f
            repository.logWorkout(
                WorkoutSession(
                    dateString = _selectedDate.value,
                    workoutType = type,
                    durationSeconds = durationSeconds,
                    distanceMeters = distanceMeters,
                    caloriesBurned = caloriesBurned,
                    paceMinPerKm = pace,
                    notes = notes
                )
            )
        }
    }

    fun logWeight(weightKg: Float, waistCm: Float? = null, notes: String = "") {
        viewModelScope.launch {
            repository.logWeight(
                WeightRecord(
                    dateString = _selectedDate.value,
                    weightKg = weightKg,
                    waistCm = waistCm,
                    notes = notes
                )
            )
            // Also update current weight in profile
            val cur = userProfile.value
            val bmr = NutritionEngine.calculateBmr(weightKg, cur.heightCm, cur.age, cur.gender)
            val tdee = NutritionEngine.calculateTdee(bmr, cur.activityLevel)
            val newTarget = NutritionEngine.calculateCalorieTarget(tdee, cur.fitnessGoal, cur.gender)
            repository.saveProfile(cur.copy(weightKg = weightKg, dailyCalorieTarget = newTarget))
        }
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch {
            repository.deleteWeight(id)
        }
    }

    fun logSleep(durationMinutes: Int, bedtimeMillis: Long, wakeMillis: Long, quality: Int, notes: String) {
        viewModelScope.launch {
            repository.logSleep(
                SleepRecord(
                    dateString = _selectedDate.value,
                    bedtimeMillis = bedtimeMillis,
                    wakeTimeMillis = wakeMillis,
                    durationMinutes = durationMinutes,
                    qualityRating = quality,
                    notes = notes
                )
            )
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    fun saveScanRecord(scan: FoodScanRecord) {
        viewModelScope.launch {
            repository.saveScan(scan)
        }
    }

    fun requestAiCoachAdvice() {
        viewModelScope.launch {
            _isAiCoachLoading.value = true
            val profile = userProfile.value
            val summary = nutritionSummary.value
            val water = waterTotalMl.value
            val steps = stepRecord.value?.steps ?: 0

            val advice = GeminiFoodService.getHealthCoachGuidance(
                userGoal = profile.fitnessGoal,
                caloriesConsumedToday = summary.totalCaloriesConsumed,
                targetCalories = summary.calorieTarget,
                waterConsumedMl = water,
                stepsTaken = steps
            )
            _aiCoachAdvice.value = advice
            _isAiCoachLoading.value = false
            repository.unlockAchievement("ai_health_coaching")
        }
    }

    fun exportHealthDataJson(): String {
        val root = JSONObject()
        val profile = userProfile.value
        root.put("appName", "NutriGlass AI Health")
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("profile", JSONObject().apply {
            put("name", profile.name)
            put("weightKg", profile.weightKg)
            put("targetWeightKg", profile.targetWeightKg)
            put("goal", profile.fitnessGoal)
            put("dailyCalorieTarget", profile.dailyCalorieTarget)
        })
        root.put("todayNutrition", JSONObject().apply {
            val sum = nutritionSummary.value
            put("caloriesConsumed", sum.totalCaloriesConsumed)
            put("proteinGrams", sum.totalProteinGrams)
            put("carbsGrams", sum.totalCarbsGrams)
            put("fatGrams", sum.totalFatGrams)
            put("waterIntakeMl", waterTotalMl.value)
            put("steps", stepRecord.value?.steps ?: 0)
        })
        return root.toString(2)
    }
}
