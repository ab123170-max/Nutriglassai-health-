package com.example.domain

import com.example.data.model.MealEntry
import com.example.data.model.MonthlyGoalTarget
import com.example.data.model.StepRecord
import com.example.data.model.UserProfile
import com.example.data.model.WaterEntry
import com.example.data.model.WeightRecord
import com.example.data.model.WorkoutSession
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthlyReportSummary(
    val monthKey: String, // "YYYY-MM"
    val monthDisplayName: String, // e.g. "October 2026"
    val totalCaloriesConsumed: Int,
    val avgDailyCalorieIntake: Int,
    val totalExerciseCalories: Int,
    val avgDailyNetCalories: Int,
    val daysWithinCalorieTarget: Int,
    val daysExceedingCalorieTarget: Int,
    val daysLoggedMeals: Int,
    
    val totalWalkingDistanceKm: Float,
    val totalWalkingDurationMinutes: Int,
    val totalWorkoutSessions: Int,
    val totalActivityCalories: Int,
    val avgDailySteps: Int,
    val daysActivityGoalAchieved: Int,
    
    val startingWeightKg: Float,
    val latestWeightKg: Float,
    val netWeightChangeKg: Float,
    val targetWeightKg: Float,
    
    val weightTrend: List<Pair<String, Float>>,
    val walkingTrend: List<Pair<String, Float>>,
    val calorieBalanceTrend: List<Triple<String, Int, Int>>, // date, intake, exercise
    
    val goalsCompletedCount: Int,
    val goalsPartiallyCompletedCount: Int,
    val goalsMissedCount: Int,
    val overallMonthlyCompletionPct: Int,
    
    val insights: List<String>,
    val goalTarget: MonthlyGoalTarget
)

object MonthlyReportEngine {

    fun generateMonthlySummary(
        monthKey: String, // "YYYY-MM"
        meals: List<MealEntry>,
        workouts: List<WorkoutSession>,
        steps: List<StepRecord>,
        weights: List<WeightRecord>,
        userProfile: UserProfile,
        goalTarget: MonthlyGoalTarget
    ): MonthlyReportSummary {
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val cal = Calendar.getInstance()
        
        try {
            val date = sdfMonth.parse(monthKey)
            if (date != null) {
                cal.time = date
            }
        } catch (e: Exception) {
            // fallback to current month
        }

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthDisplayName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)

        // Filter records for selected month
        val monthMeals = meals.filter { it.dateString.startsWith(monthKey) }
        val monthWorkouts = workouts.filter { it.dateString.startsWith(monthKey) }
        val monthSteps = steps.filter { it.dateString.startsWith(monthKey) }
        val monthWeights = weights.filter { it.dateString.startsWith(monthKey) }.sortedBy { it.dateString }

        // Food & Calories
        val totalCaloriesConsumed = monthMeals.sumOf { it.calories }
        val uniqueMealDays = monthMeals.map { it.dateString }.distinct()
        val daysLoggedMeals = uniqueMealDays.size
        val avgDailyCalorieIntake = if (daysLoggedMeals > 0) totalCaloriesConsumed / daysLoggedMeals else 0

        // Daily calorie totals map
        val dailyCaloriesMap = monthMeals.groupBy { it.dateString }
            .mapValues { entry -> entry.value.sumOf { it.calories } }

        val dailyExerciseMap = monthWorkouts.groupBy { it.dateString }
            .mapValues { entry -> entry.value.sumOf { it.caloriesBurned } }

        var daysWithinCalorieTarget = 0
        var daysExceedingCalorieTarget = 0

        val calorieTarget = goalTarget.dailyCalorieTarget
        dailyCaloriesMap.forEach { (_, cals) ->
            if (cals <= calorieTarget) {
                daysWithinCalorieTarget++
            } else {
                daysExceedingCalorieTarget++
            }
        }

        // Exercise & Walking
        val totalWorkoutSessions = monthWorkouts.size
        val totalExerciseCalories = monthWorkouts.sumOf { it.caloriesBurned }
        
        val walkingWorkouts = monthWorkouts.filter { it.workoutType.contains("Walk", true) || it.distanceMeters > 0 }
        val totalWalkingDistanceMeters = walkingWorkouts.sumOf { it.distanceMeters.toDouble() }.toFloat()
        val totalWalkingDistanceKm = totalWalkingDistanceMeters / 1000f
        val totalWalkingDurationMinutes = (walkingWorkouts.sumOf { it.durationSeconds } / 60L).toInt()

        val totalActivityCalories = totalExerciseCalories + monthSteps.sumOf { it.caloriesBurned }
        val totalSteps = monthSteps.sumOf { it.steps }
        val avgDailySteps = if (monthSteps.isNotEmpty()) totalSteps / monthSteps.size else 0
        
        val daysActivityGoalAchieved = monthSteps.count { it.steps >= userProfile.dailyStepGoal } + 
                monthWorkouts.map { it.dateString }.distinct().size.coerceAtMost(daysInMonth)

        val totalNetCalories = totalCaloriesConsumed - totalExerciseCalories
        val avgDailyNetCalories = if (daysLoggedMeals > 0) totalNetCalories / daysLoggedMeals else 0

        // Weight Progress
        val startingWeightKg = if (monthWeights.isNotEmpty()) monthWeights.first().weightKg else userProfile.weightKg
        val latestWeightKg = if (monthWeights.isNotEmpty()) monthWeights.last().weightKg else userProfile.weightKg
        val netWeightChangeKg = latestWeightKg - startingWeightKg

        // Trends
        val weightTrend = monthWeights.map { it.dateString to it.weightKg }
        
        val walkingTrendMap = monthWorkouts.groupBy { it.dateString }
            .mapValues { entry -> entry.value.sumOf { (it.distanceMeters / 1000f).toDouble() }.toFloat() }
        val walkingTrend = walkingTrendMap.map { it.key to it.value }.sortedBy { it.first }

        val calorieBalanceTrend = (1..daysInMonth).map { day ->
            val dayString = String.format(Locale.US, "%s-%02d", monthKey, day)
            val intake = dailyCaloriesMap[dayString] ?: 0
            val exercise = dailyExerciseMap[dayString] ?: 0
            Triple(dayString, intake, exercise)
        }

        // Goals Achievement
        var completed = 0
        var partial = 0
        var missed = 0

        // Goal 1: Monthly Walking Distance
        val walkingPct = if (goalTarget.targetMonthlyWalkingDistanceKm > 0) {
            (totalWalkingDistanceKm / goalTarget.targetMonthlyWalkingDistanceKm * 100).toInt()
        } else 100
        when {
            walkingPct >= 100 -> completed++
            walkingPct >= 50 -> partial++
            else -> missed++
        }

        // Goal 2: Workout Sessions
        val workoutPct = if (goalTarget.targetWorkoutSessions > 0) {
            (totalWorkoutSessions.toFloat() / goalTarget.targetWorkoutSessions * 100).toInt()
        } else 100
        when {
            workoutPct >= 100 -> completed++
            workoutPct >= 50 -> partial++
            else -> missed++
        }

        // Goal 3: Nutrition Adherence Days
        val nutritionPct = if (goalTarget.targetDaysMeetingNutritionGoal > 0) {
            (daysWithinCalorieTarget.toFloat() / goalTarget.targetDaysMeetingNutritionGoal * 100).toInt()
        } else 100
        when {
            nutritionPct >= 100 -> completed++
            nutritionPct >= 50 -> partial++
            else -> missed++
        }

        // Goal 4: Weight Progress
        val weightTargetPct = if (goalTarget.targetWeightChangeKg != 0f) {
            val progress = (startingWeightKg - latestWeightKg)
            ((progress / Math.abs(goalTarget.targetWeightChangeKg)) * 100).toInt().coerceIn(0, 100)
        } else 100
        when {
            weightTargetPct >= 100 -> completed++
            weightTargetPct >= 50 -> partial++
            else -> missed++
        }

        val totalGoalsTracked = 4
        val overallMonthlyCompletionPct = ((completed * 100 + partial * 50) / totalGoalsTracked).coerceIn(0, 100)

        // Generate factual, supportive insights
        val insightsList = mutableListOf<String>()
        if (totalWalkingDistanceKm > 0) {
            insightsList.add("You walked String.format(%.1f km) this month across recorded sessions.".replace("String.format(%.1f km)", String.format(Locale.US, "%.1f km", totalWalkingDistanceKm)))
        }
        if (daysWithinCalorieTarget > 0) {
            insightsList.add("You maintained your daily calorie target on $daysWithinCalorieTarget days.")
        }
        if (totalWorkoutSessions > 0) {
            insightsList.add("You logged $totalWorkoutSessions workout sessions, burning approximately $totalExerciseCalories kcal.")
        }
        if (monthWeights.size >= 2) {
            val diffStr = String.format(Locale.US, "%.1f kg", Math.abs(netWeightChangeKg))
            if (netWeightChangeKg <= 0) {
                insightsList.add("Your recorded weight decreased by $diffStr this month.")
            } else {
                insightsList.add("Your recorded weight changed by +$diffStr this month.")
            }
        }
        if (daysLoggedMeals > 0) {
            insightsList.add("You logged meals on $daysLoggedMeals days with an average intake of $avgDailyCalorieIntake kcal/day.")
        }
        if (insightsList.isEmpty()) {
            insightsList.add("Start logging your meals, walks, and weight to generate detailed monthly insights!")
        }

        return MonthlyReportSummary(
            monthKey = monthKey,
            monthDisplayName = monthDisplayName,
            totalCaloriesConsumed = totalCaloriesConsumed,
            avgDailyCalorieIntake = avgDailyCalorieIntake,
            totalExerciseCalories = totalExerciseCalories,
            avgDailyNetCalories = avgDailyNetCalories,
            daysWithinCalorieTarget = daysWithinCalorieTarget,
            daysExceedingCalorieTarget = daysExceedingCalorieTarget,
            daysLoggedMeals = daysLoggedMeals,
            totalWalkingDistanceKm = totalWalkingDistanceKm,
            totalWalkingDurationMinutes = totalWalkingDurationMinutes,
            totalWorkoutSessions = totalWorkoutSessions,
            totalActivityCalories = totalActivityCalories,
            avgDailySteps = avgDailySteps,
            daysActivityGoalAchieved = daysActivityGoalAchieved,
            startingWeightKg = startingWeightKg,
            latestWeightKg = latestWeightKg,
            netWeightChangeKg = netWeightChangeKg,
            targetWeightKg = userProfile.targetWeightKg,
            weightTrend = weightTrend,
            walkingTrend = walkingTrend,
            calorieBalanceTrend = calorieBalanceTrend,
            goalsCompletedCount = completed,
            goalsPartiallyCompletedCount = partial,
            goalsMissedCount = missed,
            overallMonthlyCompletionPct = overallMonthlyCompletionPct,
            insights = insightsList,
            goalTarget = goalTarget
        )
    }

    // CSV Export Generator
    fun exportMonthlyReportCsv(summary: MonthlyReportSummary, meals: List<MealEntry>, workouts: List<WorkoutSession>): String {
        val sb = StringBuilder()
        sb.append("NUTRIGLASS AI - MONTHLY HEALTH REPORT\n")
        sb.append("Month,${summary.monthDisplayName}\n")
        sb.append("Total Calories Consumed,${summary.totalCaloriesConsumed} kcal\n")
        sb.append("Average Daily Calorie Intake,${summary.avgDailyCalorieIntake} kcal\n")
        sb.append("Total Exercise Calories Burned,${summary.totalExerciseCalories} kcal\n")
        sb.append("Average Daily Net Calories,${summary.avgDailyNetCalories} kcal\n")
        sb.append("Total Walking Distance,${String.format(Locale.US, "%.2f km", summary.totalWalkingDistanceKm)}\n")
        sb.append("Total Workout Sessions,${summary.totalWorkoutSessions}\n")
        sb.append("Starting Weight,${summary.startingWeightKg} kg\n")
        sb.append("Latest Weight,${summary.latestWeightKg} kg\n")
        sb.append("Net Weight Change,${String.format(Locale.US, "%.1f kg", summary.netWeightChangeKg)}\n")
        sb.append("Monthly Goal Completion,${summary.overallMonthlyCompletionPct}%\n\n")

        sb.append("MEAL LOGS FOR ${summary.monthDisplayName}\n")
        sb.append("Date,Meal Type,Food Name,Quantity,Calories,Protein(g),Carbs(g),Fat(g)\n")
        val monthMeals = meals.filter { it.dateString.startsWith(summary.monthKey) }
        monthMeals.forEach { m ->
            sb.append("${m.dateString},\"${m.mealType}\",\"${m.foodName.replace("\"", "\"\"")}\",\"${m.portionDescription}\",${m.calories},${m.protein},${m.carbs},${m.fat}\n")
        }

        sb.append("\nWORKOUT LOGS FOR ${summary.monthDisplayName}\n")
        sb.append("Date,Workout Type,Duration (min),Distance (km),Calories Burned\n")
        val monthWorkouts = workouts.filter { it.dateString.startsWith(summary.monthKey) }
        monthWorkouts.forEach { w ->
            val distKm = w.distanceMeters / 1000f
            sb.append("${w.dateString},\"${w.workoutType}\",${w.durationSeconds / 60},${String.format(Locale.US, "%.2f", distKm)},${w.caloriesBurned}\n")
        }

        return sb.toString()
    }

    // JSON Database Backup Generator
    fun generateDatabaseBackupJson(
        profile: UserProfile,
        meals: List<MealEntry>,
        weights: List<WeightRecord>,
        workouts: List<WorkoutSession>,
        steps: List<StepRecord>,
        water: List<WaterEntry>,
        monthlyTargets: List<MonthlyGoalTarget>
    ): String {
        val root = JSONObject()
        root.put("app", "NutriGlass AI")
        root.put("version", 3)
        root.put("timestamp", System.currentTimeMillis())

        // User Profile
        val profileObj = JSONObject()
        profileObj.put("name", profile.name)
        profileObj.put("age", profile.age)
        profileObj.put("gender", profile.gender)
        profileObj.put("heightCm", profile.heightCm)
        profileObj.put("weightKg", profile.weightKg)
        profileObj.put("targetWeightKg", profile.targetWeightKg)
        profileObj.put("dailyCalorieTarget", profile.dailyCalorieTarget)
        profileObj.put("dailyStepGoal", profile.dailyStepGoal)
        root.put("profile", profileObj)

        // Meals
        val mealsArr = JSONArray()
        meals.forEach { m ->
            val o = JSONObject()
            o.put("id", m.id)
            o.put("dateString", m.dateString)
            o.put("mealType", m.mealType)
            o.put("foodName", m.foodName)
            o.put("nepaliName", m.nepaliName)
            o.put("portionDescription", m.portionDescription)
            o.put("quantity", m.quantity)
            o.put("unit", m.unit)
            o.put("calories", m.calories)
            o.put("protein", m.protein)
            o.put("carbs", m.carbs)
            o.put("fat", m.fat)
            o.put("fiber", m.fiber)
            o.put("timestamp", m.timestamp)
            mealsArr.put(o)
        }
        root.put("meals", mealsArr)

        // Weights
        val weightsArr = JSONArray()
        weights.forEach { w ->
            val o = JSONObject()
            o.put("id", w.id)
            o.put("dateString", w.dateString)
            o.put("weightKg", w.weightKg)
            o.put("timestamp", w.timestamp)
            weightsArr.put(o)
        }
        root.put("weights", weightsArr)

        // Workouts
        val workoutsArr = JSONArray()
        workouts.forEach { w ->
            val o = JSONObject()
            o.put("id", w.id)
            o.put("dateString", w.dateString)
            o.put("workoutType", w.workoutType)
            o.put("durationSeconds", w.durationSeconds)
            o.put("distanceMeters", w.distanceMeters)
            o.put("caloriesBurned", w.caloriesBurned)
            o.put("completedAt", w.completedAt)
            workoutsArr.put(o)
        }
        root.put("workouts", workoutsArr)

        // Steps
        val stepsArr = JSONArray()
        steps.forEach { s ->
            val o = JSONObject()
            o.put("dateString", s.dateString)
            o.put("steps", s.steps)
            o.put("distanceMeters", s.distanceMeters)
            o.put("caloriesBurned", s.caloriesBurned)
            stepsArr.put(o)
        }
        root.put("steps", stepsArr)

        // Water
        val waterArr = JSONArray()
        water.forEach { wt ->
            val o = JSONObject()
            o.put("id", wt.id)
            o.put("dateString", wt.dateString)
            o.put("amountMl", wt.amountMl)
            o.put("timestamp", wt.timestamp)
            waterArr.put(o)
        }
        root.put("water", waterArr)

        // Monthly Targets
        val targetsArr = JSONArray()
        monthlyTargets.forEach { t ->
            val o = JSONObject()
            o.put("monthKey", t.monthKey)
            o.put("targetWeightChangeKg", t.targetWeightChangeKg)
            o.put("targetWalkingDays", t.targetWalkingDays)
            o.put("targetMonthlyWalkingDistanceKm", t.targetMonthlyWalkingDistanceKm)
            o.put("targetWorkoutSessions", t.targetWorkoutSessions)
            o.put("dailyCalorieTarget", t.dailyCalorieTarget)
            o.put("targetDaysMeetingNutritionGoal", t.targetDaysMeetingNutritionGoal)
            targetsArr.put(o)
        }
        root.put("monthlyTargets", targetsArr)

        return root.toString(2)
    }
}
