package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CheckInQuestionnaireEntity
import com.example.ui.components.SamsungHealthCard
import com.example.ui.components.WeeklyCheckInSettingsDialog
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.WorkoutOrange
import com.example.ui.viewmodel.HealthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyCheckInScreen(
    viewModel: HealthViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val measurementPrefs by viewModel.measurementPreferences.collectAsState()
    val latestCheckIn by viewModel.latestWeeklyCheckIn.collectAsState()
    val latestPlan by viewModel.latestWeeklyPlan.collectAsState()
    val checkInPrefs by viewModel.weeklyCheckInPreferences.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    val isLb = measurementPrefs.weightUnit.equals("lb", ignoreCase = true)
    val unitLabel = if (isLb) "lb" else "kg"

    // Form State
    var weightInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var showQuestionnaire by remember { mutableStateOf(false) }

    // Questionnaire fields
    var mealPlanDays by remember { mutableIntStateOf(5) }
    var exerciseDays by remember { mutableIntStateOf(3) }
    var walkingDays by remember { mutableIntStateOf(4) }
    var sleepQuality by remember { mutableStateOf("Good") }
    var selectedDifficulties by remember { mutableStateOf(setOf<String>()) }
    var isDietAffordable by remember { mutableStateOf(true) }
    var biggestChallenge by remember { mutableStateOf("") }
    var nextWeekGoal by remember { mutableStateOf("") }

    val todayDateString = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekend Weight Check-In ⚖️", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HealthNavy.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null, tint = HealthNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Weekly Reflection & Progress Goal",
                                fontWeight = FontWeight.Bold,
                                color = HealthNavy,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Record your weight today to review your weekly habits, celebrate progress, and get your personalized 7-day plan.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Step 1: Weight Input Section
            item {
                SamsungHealthCard(
                    title = "Enter Current Weight ($unitLabel)",
                    icon = Icons.Default.MonitorWeight
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Previous Weight:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val prevDisplay = latestCheckIn?.currentWeightKg ?: profile.weightKg
                            val convertedPrev = if (isLb) prevDisplay * 2.20462f else prevDisplay
                            Text(
                                text = "${String.format("%.1f", convertedPrev)} $unitLabel",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target Weight Goal:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val targetDisplay = profile.targetWeightKg
                            val convertedTarget = if (isLb) targetDisplay * 2.20462f else targetDisplay
                            Text(
                                text = "${String.format("%.1f", convertedTarget)} $unitLabel",
                                fontWeight = FontWeight.Bold,
                                color = HealthTeal,
                                fontSize = 14.sp
                            )
                        }

                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Current Scale Weight ($unitLabel)") },
                            placeholder = { Text("e.g. 68.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Notes / Reflections (Optional)") },
                            placeholder = { Text("e.g. Felt energetic, stayed hydrated...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Step 2: Questionnaire Toggle / Form
            item {
                SamsungHealthCard(
                    title = "Weekly Habits & Experience Survey",
                    icon = Icons.Default.Psychology
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showQuestionnaire = !showQuestionnaire },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showQuestionnaire) "Hide Follow-Up Questions" else "Answer Short 8-Question Review (Optional)",
                                fontWeight = FontWeight.SemiBold,
                                color = HealthTeal,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = if (showQuestionnaire) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = HealthTeal
                            )
                        }

                        AnimatedVisibility(visible = showQuestionnaire) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                HorizontalDivider()

                                // Question 1: Meals
                                Column {
                                    Text(
                                        text = "1. Days followed planned meal guidance ($mealPlanDays / 7 days):",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Slider(
                                        value = mealPlanDays.toFloat(),
                                        onValueChange = { mealPlanDays = it.toInt() },
                                        valueRange = 0f..7f,
                                        steps = 6,
                                        colors = SliderDefaults.colors(thumbColor = HealthTeal)
                                    )
                                }

                                // Question 2: Exercise
                                Column {
                                    Text(
                                        text = "2. Workouts / Exercise sessions completed ($exerciseDays sessions):",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Slider(
                                        value = exerciseDays.toFloat(),
                                        onValueChange = { exerciseDays = it.toInt() },
                                        valueRange = 0f..7f,
                                        steps = 6,
                                        colors = SliderDefaults.colors(thumbColor = HealthTeal)
                                    )
                                }

                                // Question 3: Walking
                                Column {
                                    Text(
                                        text = "3. Walking / Step goal days achieved ($walkingDays / 7 days):",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Slider(
                                        value = walkingDays.toFloat(),
                                        onValueChange = { walkingDays = it.toInt() },
                                        valueRange = 0f..7f,
                                        steps = 6,
                                        colors = SliderDefaults.colors(thumbColor = HealthTeal)
                                    )
                                }

                                // Question 4: Sleep Quality
                                Column {
                                    Text(
                                        text = "4. Overall sleep quality this week:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Good", "Fair", "Poor").forEach { rating ->
                                            FilterChip(
                                                selected = sleepQuality == rating,
                                                onClick = { sleepQuality = rating },
                                                label = { Text(rating) }
                                            )
                                        }
                                    }
                                }

                                // Question 5: Difficulties
                                Column {
                                    Text(
                                        text = "5. Any challenges or obstacles faced?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val options = listOf("Hunger", "Cravings", "Stress", "Busy Schedule", "Illness/Pain", "None")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        options.take(3).forEach { opt ->
                                            FilterChip(
                                                selected = selectedDifficulties.contains(opt),
                                                onClick = {
                                                    selectedDifficulties = if (selectedDifficulties.contains(opt)) {
                                                        selectedDifficulties - opt
                                                    } else {
                                                        selectedDifficulties + opt
                                                    }
                                                },
                                                label = { Text(opt, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        options.drop(3).forEach { opt ->
                                            FilterChip(
                                                selected = selectedDifficulties.contains(opt),
                                                onClick = {
                                                    selectedDifficulties = if (selectedDifficulties.contains(opt)) {
                                                        selectedDifficulties - opt
                                                    } else {
                                                        selectedDifficulties + opt
                                                    }
                                                },
                                                label = { Text(opt, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }

                                // Question 6: Diet Affordability
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "6. Was diet easy & affordable?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Switch(
                                        checked = isDietAffordable,
                                        onCheckedChange = { isDietAffordable = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = HealthTeal)
                                    )
                                }

                                // Question 7: Biggest Challenge
                                OutlinedTextField(
                                    value = biggestChallenge,
                                    onValueChange = { biggestChallenge = it },
                                    label = { Text("7. What was your biggest challenge?") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Question 8: Improvement Goal
                                OutlinedTextField(
                                    value = nextWeekGoal,
                                    onValueChange = { nextWeekGoal = it },
                                    label = { Text("8. What would you like to improve next week?") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Step 3: Complete Check-In Action
            item {
                Button(
                    onClick = {
                        val parsedWeight = weightInput.toFloatOrNull()
                        if (parsedWeight == null || parsedWeight <= 0f) {
                            Toast.makeText(context, "Please enter a valid weight measurement.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val weightKg = if (isLb) parsedWeight / 2.20462f else parsedWeight

                        val questionnaire = if (showQuestionnaire) {
                            CheckInQuestionnaireEntity(
                                checkInId = 0L,
                                dateString = todayDateString,
                                followedMealPlanDays = mealPlanDays,
                                exerciseSessionsCompleted = exerciseDays,
                                walkingGoalDaysAchieved = walkingDays,
                                sleepQualityRating = sleepQuality,
                                difficultiesFacing = if (selectedDifficulties.isEmpty()) "None" else selectedDifficulties.joinToString(", "),
                                dietAffordableAndEasy = isDietAffordable,
                                biggestChallenge = biggestChallenge,
                                nextWeekImprovementGoal = nextWeekGoal
                            )
                        } else null

                        viewModel.recordWeeklyCheckIn(
                            weightKg = weightKg,
                            checkInDate = todayDateString,
                            notes = notesInput,
                            questionnaire = questionnaire
                        )

                        Toast.makeText(context, "Check-In Logged & AI Plan Generated! 🎉", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HealthTeal),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Check-In & Generate AI Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Display Latest Generated AI Plan & Recommendations
            if (latestCheckIn != null && latestPlan != null) {
                item {
                    SamsungHealthCard(
                        title = "AI Coaching Analysis & Proposed Plan",
                        icon = Icons.Default.AutoAwesome
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Motivation Message
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = HealthNavy.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = latestCheckIn?.aiMotivationMessage ?: "Great work showing up for yourself!",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = HealthNavy
                                    )
                                }
                            }

                            // Obstacles Analysis
                            if (latestPlan?.summaryReasoning?.isNotBlank() == true) {
                                Text(
                                    text = "💡 Clinical Insights on Body Weight Trends:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = latestPlan?.summaryReasoning ?: "",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Proposed Adjustments with Accept/Reject Toggles
                            Text(
                                text = "🎯 Proposed Weekly Adjustments (Tap to Accept/Reject):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            val rawAdjustments = latestPlan?.proposedAdjustmentsJson ?: "[]"
                            val adjustmentsList = remember(rawAdjustments) {
                                val list = mutableListOf<Triple<String, String, Boolean>>()
                                try {
                                    val arr = org.json.JSONArray(rawAdjustments)
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.getJSONObject(i)
                                        list.add(
                                            Triple(
                                                obj.optString("id", "adj_$i"),
                                                "${obj.optString("title")}: ${obj.optString("description")}",
                                                obj.optBoolean("isAccepted", true)
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                list
                            }

                            adjustmentsList.forEach { (adjId, adjDesc, isAccepted) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isAccepted) CalorieGreen.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.2f))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = adjDesc,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Checkbox(
                                        checked = isAccepted,
                                        onCheckedChange = { accepted ->
                                            viewModel.toggleAdjustmentAcceptance(adjId, accepted)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = HealthTeal)
                                    )
                                }
                            }

                            // Affordable Alternatives
                            if (latestPlan?.affordableFoodAlternatives?.isNotBlank() == true) {
                                HorizontalDivider()
                                Text(
                                    text = "🥬 Local & Affordable Healthy Swaps:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = latestPlan?.affordableFoodAlternatives ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showSettingsDialog) {
        WeeklyCheckInSettingsDialog(
            currentPrefs = checkInPrefs,
            onDismiss = { showSettingsDialog = false },
            onSave = { updated ->
                viewModel.saveWeeklyCheckInPreferences(updated)
                Toast.makeText(context, "Preferences Saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
