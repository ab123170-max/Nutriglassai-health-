package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Achievement
import com.example.data.model.FoodItem
import com.example.data.model.FoodScanRecord
import com.example.data.model.MealEntry
import com.example.data.model.MultiFoodScanResult
import com.example.data.model.ProgressPhoto
import com.example.data.model.RecognizedFoodItem
import com.example.data.model.SleepRecord
import com.example.data.model.StepRecord
import com.example.data.model.UserMeasurementPreferences
import com.example.data.model.UserProfile
import com.example.data.model.WaterEntry
import com.example.data.model.WeightRecord
import com.example.data.model.WorkoutSession
import com.example.data.network.GeminiFoodService
import com.example.data.repository.HealthRepository
import com.example.domain.DailyNutritionSummary
import com.example.domain.FoodUnit
import com.example.domain.NutritionEngine
import com.example.domain.UnitConversionEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import org.json.JSONObject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HealthRepository(AppDatabase.getDatabase(application))

    private val _selectedDate = MutableStateFlow(repository.getTodayString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .flatMapLatest { profile ->
            MutableStateFlow(profile ?: UserProfile())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val measurementPreferences: StateFlow<UserMeasurementPreferences> = repository.measurementPreferences
        .flatMapLatest { prefs ->
            MutableStateFlow(prefs ?: UserMeasurementPreferences())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserMeasurementPreferences())

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

    // Multi-Food AI Vision State
    private val _multiScanResult = MutableStateFlow<MultiFoodScanResult?>(null)
    val multiScanResult: StateFlow<MultiFoodScanResult?> = _multiScanResult.asStateFlow()

    private val _scanProgressStage = MutableStateFlow<String?>(null)
    val scanProgressStage: StateFlow<String?> = _scanProgressStage.asStateFlow()

    private val _isScanAnalyzing = MutableStateFlow(false)
    val isScanAnalyzing: StateFlow<Boolean> = _isScanAnalyzing.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private val _scanErrorType = MutableStateFlow<com.example.data.network.GeminiErrorType?>(null)
    val scanErrorType: StateFlow<com.example.data.network.GeminiErrorType?> = _scanErrorType.asStateFlow()

    private var scanJob: kotlinx.coroutines.Job? = null
    var lastScanBitmap: Bitmap? = null
        private set
    var lastScanUri: String? = null
        private set

    val allMeals: StateFlow<List<MealEntry>> = repository.allMeals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWater: StateFlow<List<WaterEntry>> = repository.allWater
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun saveMeasurementPreferences(prefs: UserMeasurementPreferences) {
        viewModelScope.launch {
            repository.saveMeasurementPreferences(prefs)
        }
    }

    fun resetMeasurementPreferences() {
        viewModelScope.launch {
            repository.saveMeasurementPreferences(UserMeasurementPreferences())
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
        portionDesc: String = "${portionGrams.toInt()}g",
        unit: String = "serving",
        quantity: Float = 1f,
        sodium: Float = 0f,
        preparationMethod: String = "Standard",
        estimationMethod: String = "MANUAL",
        confidence: Float = 1.0f,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            repository.logMeal(
                MealEntry(
                    dateString = _selectedDate.value,
                    mealType = mealType,
                    foodName = foodName,
                    nepaliName = nepaliName,
                    portionDescription = portionDesc,
                    quantity = quantity,
                    unit = unit,
                    normalizedWeightGrams = portionGrams,
                    portionGrams = portionGrams,
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    fiber = fiber,
                    sugar = sugar,
                    sodium = sodium,
                    preparationMethod = preparationMethod,
                    estimationMethod = estimationMethod,
                    confidence = confidence,
                    imageUri = imageUri
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
            val distanceKm = steps * 0.00078f
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

    // --- Multi-Food Scanner Workflows (Gemini AI Vision) ---
    fun startMultiFoodScan(bitmap: Bitmap, imageUri: String?) {
        // Prevent duplicate scan requests while a scan is in flight
        if (_isScanAnalyzing.value) return

        lastScanBitmap = bitmap
        lastScanUri = imageUri
        _isScanAnalyzing.value = true
        _scanError.value = null
        _scanErrorType.value = null
        _multiScanResult.value = null

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                _scanProgressStage.value = "Compressing & optimizing image..."
                delay(200)

                _scanProgressStage.value = "Connecting to Gemini (${com.example.data.network.GeminiConfig.activeModel})..."
                delay(250)

                _scanProgressStage.value = "Analyzing meal components & cooking style..."
                val result = com.example.data.network.GeminiFoodService.analyzeFoodImageMulti(bitmap, imageUri)

                _scanProgressStage.value = "Checking portions, nutrients & allergens..."
                delay(200)

                _scanProgressStage.value = "Validating AI schema & calculations..."
                delay(150)

                _scanProgressStage.value = "Ready for review!"
                _multiScanResult.value = result

                // Persist successful scan record in local Room database
                val allAllergens = result.items.flatMap { it.allergens }.distinct().joinToString(", ")
                val allAlts = result.items.flatMap { it.alternatives }.distinct().take(3).joinToString(", ")

                repository.saveScan(
                    FoodScanRecord(
                        foodName = result.mealTitle,
                        nepaliName = result.items.firstOrNull()?.nepaliName ?: "",
                        portion = "${result.items.size} items",
                        estimatedCalories = result.totalCalories,
                        calorieRange = "±${(result.totalCalories * 0.12f).toInt()} kcal",
                        protein = result.totalProtein,
                        carbs = result.totalCarbs,
                        fat = result.totalFat,
                        fiber = result.totalFiber,
                        sugar = result.totalSugar,
                        sodium = result.totalSodium,
                        confidence = result.overallConfidence,
                        uncertaintyNotes = result.uncertaintyNotes,
                        itemsSummary = result.items.joinToString(", ") { "${it.foodName} (${it.estimatedGrams.toInt()}g)" },
                        allergens = allAllergens,
                        healthierAlternatives = allAlts,
                        imageUri = imageUri
                    )
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                _scanError.value = "Scanning cancelled."
                _scanErrorType.value = null
            } catch (e: com.example.data.network.GeminiAnalysisException) {
                // Surface exact diagnosed error rather than hiding it
                _scanError.value = e.message
                _scanErrorType.value = e.errorType
            } catch (e: Exception) {
                _scanError.value = "AI could not analyze this photo: ${e.localizedMessage ?: "Unknown error"}. Check server API key and model."
                _scanErrorType.value = com.example.data.network.GeminiErrorType.INVALID_RESPONSE
            } finally {
                _isScanAnalyzing.value = false
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanAnalyzing.value = false
        _scanProgressStage.value = null
        _scanError.value = "Scan cancelled."
        _scanErrorType.value = null
    }

    fun retryLastScan() {
        val bitmap = lastScanBitmap ?: return
        startMultiFoodScan(bitmap, lastScanUri)
    }

    fun useOfflineProfileForCurrentScan() {
        val fallback = com.example.data.network.GeminiFoodService.generateLocalMultiEstimate(lastScanUri)
        _multiScanResult.value = fallback
        _scanError.value = null
        _scanErrorType.value = null
        _isScanAnalyzing.value = false
    }

    fun clearCurrentScan() {
        _multiScanResult.value = null
        _scanError.value = null
        _scanErrorType.value = null
        _scanProgressStage.value = null
    }

    /**
     * Nutrition summaries for Daily, Weekly (7 days), and Monthly (30 days)
     */
    fun calculateNutritionSummary(days: Int, label: String): com.example.data.model.NutritionPeriodSummary {
        val meals = allMeals.value
        val waterEntries = allWater.value
        val target = userProfile.value.dailyCalorieTarget
        val targetWater = userProfile.value.dailyWaterTargetMl

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val dateStrings = mutableSetOf<String>()
        for (i in 0 until days) {
            val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            dateStrings.add(sdf.format(d.time))
        }

        val filteredMeals = meals.filter { it.dateString in dateStrings }
        val filteredWater = waterEntries.filter { it.dateString in dateStrings }

        val totalCals = filteredMeals.sumOf { it.calories }
        val totalP = filteredMeals.map { it.protein }.sum()
        val totalC = filteredMeals.map { it.carbs }.sum()
        val totalF = filteredMeals.map { it.fat }.sum()
        val totalFib = filteredMeals.map { it.fiber }.sum()
        val totalSod = filteredMeals.map { it.sodium }.sum()
        val totalW = filteredWater.sumOf { it.amountMl }

        val loggedDates = (filteredMeals.map { it.dateString } + filteredWater.map { it.dateString }).distinct()
        val daysCount = if (days == 1) 1 else days.coerceAtLeast(1)

        val targetMetDays = loggedDates.count { dateStr ->
            val dayCals = meals.filter { it.dateString == dateStr }.sumOf { it.calories }
            dayCals in (target - 300)..(target + 300)
        }

        return com.example.data.model.NutritionPeriodSummary(
            periodLabel = label,
            totalCalories = totalCals,
            averageCaloriesPerDay = (totalCals / daysCount.toFloat()).roundToInt(),
            totalProtein = totalP,
            averageProteinPerDay = (totalP / daysCount.toFloat() * 10f).roundToInt() / 10f,
            totalCarbs = totalC,
            averageCarbsPerDay = (totalC / daysCount.toFloat() * 10f).roundToInt() / 10f,
            totalFat = totalF,
            averageFatPerDay = (totalF / daysCount.toFloat() * 10f).roundToInt() / 10f,
            totalFiber = totalFib,
            totalSodium = totalSod,
            totalWaterMl = totalW,
            averageWaterMlPerDay = (totalW / daysCount.toFloat()).roundToInt(),
            daysLogged = loggedDates.size,
            targetMetDays = targetMetDays
        )
    }

    fun updateRecognizedItem(
        itemId: String,
        updatedName: String,
        updatedQuantity: Float,
        updatedUnit: String,
        updatedMethod: String
    ) {
        val current = _multiScanResult.value ?: return
        val foodUnit = FoodUnit.fromSymbol(updatedUnit)
        val updatedItems = current.items.map { item ->
            if (item.id == itemId) {
                val baseFood = allFoods.value.find {
                    it.name.equals(updatedName, true) || it.name.contains(updatedName, true)
                }

                val calsPer100g = baseFood?.getPer100gCalories() ?: (item.calories / item.estimatedGrams.coerceAtLeast(1f) * 100f)
                val pPer100g = baseFood?.getPer100gProtein() ?: (item.protein / item.estimatedGrams.coerceAtLeast(1f) * 100f)
                val cPer100g = baseFood?.getPer100gCarbs() ?: (item.carbs / item.estimatedGrams.coerceAtLeast(1f) * 100f)
                val fPer100g = baseFood?.getPer100gFat() ?: (item.fat / item.estimatedGrams.coerceAtLeast(1f) * 100f)
                val fibPer100g = baseFood?.getPer100gFiber() ?: (item.fiber / item.estimatedGrams.coerceAtLeast(1f) * 100f)
                val sugPer100g = baseFood?.getPer100gSugar() ?: (item.sugar / item.estimatedGrams.coerceAtLeast(1f) * 100f)
                val sodPer100g = baseFood?.sodiumPer100g ?: (item.sodium / item.estimatedGrams.coerceAtLeast(1f) * 100f)

                val calc = UnitConversionEngine.calculateNutrition(
                    caloriesPer100g = calsPer100g,
                    proteinPer100g = pPer100g,
                    carbsPer100g = cPer100g,
                    fatPer100g = fPer100g,
                    fiberPer100g = fibPer100g,
                    sugarPer100g = sugPer100g,
                    sodiumPer100g = sodPer100g,
                    foodName = updatedName,
                    quantity = updatedQuantity,
                    unit = foodUnit,
                    defaultServingGrams = baseFood?.gramsPerServing ?: 100f
                )

                item.copy(
                    foodName = updatedName,
                    nepaliName = baseFood?.nepaliName ?: item.nepaliName,
                    quantity = updatedQuantity,
                    unit = updatedUnit,
                    estimatedGrams = calc.normalizedGrams,
                    calories = calc.calories,
                    protein = calc.protein,
                    carbs = calc.carbs,
                    fat = calc.fat,
                    fiber = calc.fiber,
                    sugar = calc.sugar,
                    sodium = calc.sodium,
                    preparationMethod = updatedMethod
                )
            } else {
                item
            }
        }

        _multiScanResult.value = current.copy(
            items = updatedItems,
            totalCalories = updatedItems.sumOf { it.calories },
            totalProtein = updatedItems.map { it.protein }.sum(),
            totalCarbs = updatedItems.map { it.carbs }.sum(),
            totalFat = updatedItems.map { it.fat }.sum(),
            totalFiber = updatedItems.map { it.fiber }.sum(),
            totalSugar = updatedItems.map { it.sugar }.sum(),
            totalSodium = updatedItems.map { it.sodium }.sum()
        )
    }

    fun addFoodComponentToScan(item: RecognizedFoodItem) {
        val current = _multiScanResult.value ?: return
        val newItems = current.items + item
        _multiScanResult.value = current.copy(
            items = newItems,
            totalCalories = newItems.sumOf { it.calories },
            totalProtein = newItems.map { it.protein }.sum(),
            totalCarbs = newItems.map { it.carbs }.sum(),
            totalFat = newItems.map { it.fat }.sum(),
            totalFiber = newItems.map { it.fiber }.sum(),
            totalSugar = newItems.map { it.sugar }.sum(),
            totalSodium = newItems.map { it.sodium }.sum()
        )
    }

    fun removeFoodComponentFromScan(itemId: String) {
        val current = _multiScanResult.value ?: return
        val newItems = current.items.filterNot { it.id == itemId }
        _multiScanResult.value = current.copy(
            items = newItems,
            totalCalories = newItems.sumOf { it.calories },
            totalProtein = newItems.map { it.protein }.sum(),
            totalCarbs = newItems.map { it.carbs }.sum(),
            totalFat = newItems.map { it.fat }.sum(),
            totalFiber = newItems.map { it.fiber }.sum(),
            totalSugar = newItems.map { it.sugar }.sum(),
            totalSodium = newItems.map { it.sodium }.sum()
        )
    }

    fun confirmAndSaveScannedMeal(mealType: String) {
        val result = _multiScanResult.value ?: return
        viewModelScope.launch {
            for (item in result.items) {
                repository.logMeal(
                    MealEntry(
                        dateString = _selectedDate.value,
                        mealType = mealType,
                        foodName = item.foodName,
                        nepaliName = item.nepaliName,
                        portionDescription = "${item.quantity} ${item.unit} (${item.estimatedGrams.toInt()}g)",
                        quantity = item.quantity,
                        unit = item.unit,
                        normalizedWeightGrams = item.estimatedGrams,
                        portionGrams = item.estimatedGrams,
                        calories = item.calories,
                        protein = item.protein,
                        carbs = item.carbs,
                        fat = item.fat,
                        fiber = item.fiber,
                        sugar = item.sugar,
                        sodium = item.sodium,
                        preparationMethod = item.preparationMethod,
                        estimationMethod = "AI_VISION",
                        confidence = item.confidence,
                        userConfirmed = true,
                        imageUri = result.imageUri
                    )
                )
            }
            _multiScanResult.value = null
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
        val prefs = measurementPreferences.value
        root.put("appName", "NutriGlass AI Health")
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("profile", JSONObject().apply {
            put("name", profile.name)
            put("weightKg", profile.weightKg)
            put("targetWeightKg", profile.targetWeightKg)
            put("goal", profile.fitnessGoal)
            put("dailyCalorieTarget", profile.dailyCalorieTarget)
        })
        root.put("measurementPreferences", JSONObject().apply {
            put("weightUnit", prefs.weightUnit)
            put("heightUnit", prefs.heightUnit)
            put("foodUnit", prefs.foodUnit)
            put("waterUnit", prefs.waterUnit)
            put("distanceUnit", prefs.distanceUnit)
            put("usualGlassSizeMl", prefs.usualGlassSizeMl)
            put("usualBottleSizeMl", prefs.usualBottleSizeMl)
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
