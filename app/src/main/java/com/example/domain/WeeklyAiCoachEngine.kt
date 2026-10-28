package com.example.domain

import com.example.data.model.CheckInQuestionnaireEntity
import com.example.data.model.UserProfile
import com.example.data.model.WeeklyCheckInEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

data class ProposedAdjustment(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "Portions", "Meal Swap", "Activity", "Sleep", "Hydration"
    var isAccepted: Boolean = true
)

data class GeneratedWeeklyPlanResponse(
    val motivationMessage: String,
    val obstaclesAnalysis: String,
    val proposedAdjustments: List<ProposedAdjustment>,
    val mealPlan7Days: String,
    val activityPlan7Days: String,
    val affordableFoodAlternatives: String
)

object WeeklyAiCoachEngine {

    private const val PROXY_URL = "https://nutriglass-seven.vercel.app/api/gemini"

    fun generateWeeklyMotivationAndPlan(
        checkIn: WeeklyCheckInEntity,
        questionnaire: CheckInQuestionnaireEntity?,
        userProfile: UserProfile,
        unit: String = "kg"
    ): GeneratedWeeklyPlanResponse {
        val unitLabel = if (unit.equals("lb", ignoreCase = true)) "lb" else "kg"
        val status = checkIn.progressStatus // PROGRESSING, UNCHANGED, INCREASED, MISSED

        // Attempt online generation via proxy
        val onlineResult = tryOnlineGeminiCall(checkIn, questionnaire, userProfile, unitLabel)
        if (onlineResult != null) {
            return onlineResult
        }

        // Offline / Fallback generator with clinically sound & empathetic recommendations
        return generateFallbackPlan(checkIn, questionnaire, userProfile, unitLabel)
    }

    private fun tryOnlineGeminiCall(
        checkIn: WeeklyCheckInEntity,
        questionnaire: CheckInQuestionnaireEntity?,
        userProfile: UserProfile,
        unitLabel: String
    ): GeneratedWeeklyPlanResponse? {
        return try {
            val promptText = """
                You are NutriGlass AI, a warm, evidence-based health and weight-loss coach.
                Analyze this user's weekly check-in:
                - Progress Status: ${checkIn.progressStatus}
                - Current Weight: ${checkIn.currentWeightKg} kg
                - Previous Weight: ${checkIn.previousWeightKg ?: "None"} kg
                - Target Weight: ${checkIn.targetWeightKg} kg
                - Weekly Change: ${checkIn.weightChangeWeeklyKg} kg
                - Total Change: ${checkIn.weightChangeTotalKg} kg
                - User Goal: ${userProfile.fitnessGoal}
                - Dietary Preference: ${userProfile.dietaryPreference}
                - Followed Meal Plan Days: ${questionnaire?.followedMealPlanDays ?: "N/A"}
                - Exercise Sessions Completed: ${questionnaire?.exerciseSessionsCompleted ?: "N/A"}
                - Walking Days Achieved: ${questionnaire?.walkingGoalDaysAchieved ?: "N/A"}
                - Sleep Quality: ${questionnaire?.sleepQualityRating ?: "N/A"}
                - Difficulties Faced: ${questionnaire?.difficultiesFacing ?: "N/A"}
                - Diet Easy/Affordable: ${questionnaire?.dietAffordableAndEasy ?: true}
                - Biggest Challenge: ${questionnaire?.biggestChallenge ?: "None"}
                - Improvement Goal: ${questionnaire?.nextWeekImprovementGoal ?: "Build consistency"}

                Return JSON only with keys:
                "motivationMessage": short encouraging statement (1-2 sentences) celebrating effort or explaining normal body weight fluctuations without shame.
                "obstaclesAnalysis": 2-3 sentences explaining factors (e.g. water retention, muscle adaptation, salt, digestion, stress, sleep).
                "proposedAdjustments": array of objects [{"id": "adj_1", "title": "...", "description": "...", "category": "..."}] (max 3 practical changes).
                "mealPlan7Days": string summary of 7-day meal guidance.
                "activityPlan7Days": string summary of 7-day activity guidance.
                "affordableFoodAlternatives": list of local budget-friendly healthy swaps.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", promptText)
                    }))
                }))
            }

            val url = URL(PROXY_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.doOutput = true

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val root = JSONObject(sb.toString())
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val rawText = parts.getJSONObject(0).optString("text", "")
                        return parseJsonResponse(rawText)
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseJsonResponse(rawText: String): GeneratedWeeklyPlanResponse? {
        return try {
            val cleanJson = rawText.substringAfter("{").substringBeforeLast("}")
            val jsonObject = JSONObject("{$cleanJson}")

            val motivation = jsonObject.optString("motivationMessage", "")
            val obstacles = jsonObject.optString("obstaclesAnalysis", "")
            val mealPlan = jsonObject.optString("mealPlan7Days", "")
            val activityPlan = jsonObject.optString("activityPlan7Days", "")
            val alternatives = jsonObject.optString("affordableFoodAlternatives", "")

            val adjustmentsList = mutableListOf<ProposedAdjustment>()
            val adjArray = jsonObject.optJSONArray("proposedAdjustments")
            if (adjArray != null) {
                for (i in 0 until adjArray.length()) {
                    val obj = adjArray.getJSONObject(i)
                    adjustmentsList.add(
                        ProposedAdjustment(
                            id = obj.optString("id", "adj_$i"),
                            title = obj.optString("title", "Adjust Portion"),
                            description = obj.optString("description", "Slightly adjust daily portions."),
                            category = obj.optString("category", "Portions"),
                            isAccepted = true
                        )
                    )
                }
            }

            if (motivation.isNotEmpty()) {
                GeneratedWeeklyPlanResponse(
                    motivationMessage = motivation,
                    obstaclesAnalysis = obstacles,
                    proposedAdjustments = adjustmentsList,
                    mealPlan7Days = mealPlan,
                    activityPlan7Days = activityPlan,
                    affordableFoodAlternatives = alternatives
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun generateFallbackPlan(
        checkIn: WeeklyCheckInEntity,
        questionnaire: CheckInQuestionnaireEntity?,
        userProfile: UserProfile,
        unitLabel: String
    ): GeneratedWeeklyPlanResponse {
        val changeVal = checkIn.weightChangeWeeklyKg
        val absChange = abs(changeVal)

        val (motivation, obstacles) = when (checkIn.progressStatus) {
            "PROGRESSING" -> {
                val msg = "🎉 Outstanding work! You checked in at ${checkIn.currentWeightKg} $unitLabel (down ${String.format("%.1f", absChange)} $unitLabel this week). Your consistent habit of logging meals and staying active is moving you directly toward your target of ${checkIn.targetWeightKg} $unitLabel!"
                val obs = "Your body is responding positively to consistent calorie control and daily movement. Muscle preservation remains high while fat mass is decreasing steadily."
                Pair(msg, obs)
            }
            "UNCHANGED" -> {
                val msg = "One week does not define your journey! Your weight stayed flat at ${checkIn.currentWeightKg} $unitLabel. Weight plateaus are normal and often mean your body is holding temporary water weight while converting body composition."
                val obs = "Temporary weight stabilization can occur due to sodium intake, digestive retention, stress cortisol spikes, or natural glycogen storage from new workouts."
                Pair(msg, obs)
            }
            "INCREASED" -> {
                val msg = "Weight can naturally fluctuate from day to day! Your scale reading showed ${checkIn.currentWeightKg} $unitLabel. Let's look at your weekly habits and longer-term trend before modifying your core plan."
                val obs = "A single higher scale reading usually reflects temporary fluid retention, higher sodium intake, muscle repair swelling after strength training, or gastrointestinal transit time—not fat gain."
                Pair(msg, obs)
            }
            else -> {
                val msg = "Whenever you're ready, updating your check-in keeps your progress on track! Every step forward counts."
                val obs = "Consistency in tracking allows us to tune your nutrition and movement targets to match your actual daily energy expenditure."
                Pair(msg, obs)
            }
        }

        val adjustments = mutableListOf<ProposedAdjustment>()

        if (questionnaire != null && questionnaire.followedMealPlanDays < 4) {
            adjustments.add(
                ProposedAdjustment(
                    id = "adj_meal_prep",
                    title = "Simplify Weekly Meal Prep",
                    description = "Batch-prepare 2-3 staple meals (e.g., Dal Bhat with extra veggies or grilled protein) to make following meal plans effortless on busy days.",
                    category = "Meal Swap",
                    isAccepted = true
                )
            )
        } else {
            adjustments.add(
                ProposedAdjustment(
                    id = "adj_fiber_hydration",
                    title = "Boost Hydration & Dietary Fiber",
                    description = "Add 1 extra glass of water before each meal and include seasonal green vegetables (Gundruk, Saag) to support digestion and satiety.",
                    category = "Hydration",
                    isAccepted = true
                )
            )
        }

        if (questionnaire != null && questionnaire.walkingGoalDaysAchieved < 4) {
            adjustments.add(
                ProposedAdjustment(
                    id = "adj_walking_boost",
                    title = "15-Minute Post-Meal Walk",
                    description = "Take a comfortable 15-minute walk after lunch or dinner to improve glucose clearance and add 2,000 steps effortless daily.",
                    category = "Activity",
                    isAccepted = true
                )
            )
        } else {
            adjustments.add(
                ProposedAdjustment(
                    id = "adj_strength",
                    title = "Maintain Consistent Weekly Movement",
                    description = "Aim for 3 structured 20-minute bodyweight or walking sessions spread evenly across the upcoming week.",
                    category = "Activity",
                    isAccepted = true
                )
            )
        }

        val mealPlan = """
            Day 1-2: Focus on high-protein breakfasts (Eggs, Chana, or Curd) and controlled portion rice bowls.
            Day 3-4: Include fiber-dense greens (Rayako Saag, Tarkari) and clear soups (Gundruk/Dal).
            Day 5-7: Balanced whole food meals, keeping evening snacks to fruit or roasted chickpeas.
        """.trimIndent()

        val activityPlan = """
            Monday/Wednesday/Friday: 30 minutes active brisk walking or Walk & Listen session.
            Tuesday/Thursday: 15-minute bodyweight core/mobility circuit.
            Saturday/Sunday: Relaxed evening walk and weekend check-in review.
        """.trimIndent()

        val alternatives = """
            • Protein: Chickpeas (Chana), Black Lentils (Kalo Dal), Cottage Cheese (Paneer), Eggs, Soy Chunks (Masaura).
            • Fiber & Carbs: Buckwheat (Phapar), Millet (Kodo Dhido), Seasonal Saag, Oats, Whole Wheat Roti.
            • Healthy Fats: Roasted peanuts, flaxseeds, mustard oil in moderation.
        """.trimIndent()

        return GeneratedWeeklyPlanResponse(
            motivationMessage = motivation,
            obstaclesAnalysis = obstacles,
            proposedAdjustments = adjustments,
            mealPlan7Days = mealPlan,
            activityPlan7Days = activityPlan,
            affordableFoodAlternatives = alternatives
        )
    }
}
