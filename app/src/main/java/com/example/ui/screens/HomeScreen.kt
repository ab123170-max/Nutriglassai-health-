package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MealEntry
import com.example.domain.NutritionEngine
import com.example.ui.components.CalorieProgressRing
import com.example.ui.components.MacroNutrientRow
import com.example.ui.components.SamsungHealthCard
import com.example.ui.components.WaterQuickAddBar
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.ProteinBlue
import com.example.ui.theme.SleepIndigo
import com.example.ui.theme.WaterCyan
import com.example.ui.viewmodel.HealthViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: HealthViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToFood: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToAiCoach: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val meals by viewModel.mealsForDate.collectAsState()
    val nutritionSummary by viewModel.nutritionSummary.collectAsState()
    val waterTotal by viewModel.waterTotalMl.collectAsState()
    val stepRecord by viewModel.stepRecord.collectAsState()
    val sleepRecord by viewModel.sleepForDate.collectAsState()
    val latestWeight by viewModel.latestWeight.collectAsState()

    var showQuickStepDialog by remember { mutableStateOf(false) }
    var quickStepInput by remember { mutableStateOf("") }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Greeting & Date Selector
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$greeting, ${userProfile.name}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Daily Health & Vitality",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onNavigateToAiCoach() }
                            .testTag("home_ai_coach_badge")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "AI Coach",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Navigator Bar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.goToPreviousDay() }) {
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Day", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = selectedDate,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { viewModel.goToToday() }) {
                                Text("Today", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { viewModel.goToNextDay() }) {
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Day", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Quick Actions: AI Scan, Log Meal, Live Walk, AI Coach
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionPill(
                        icon = Icons.Default.CenterFocusStrong,
                        label = "Scan Food",
                        tint = HealthTeal,
                        onClick = onNavigateToScan,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionPill(
                        icon = Icons.Default.Restaurant,
                        label = "Log Meal",
                        tint = ProteinBlue,
                        onClick = onNavigateToFood,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionPill(
                        icon = Icons.Default.DirectionsWalk,
                        label = "Walk / GPS",
                        tint = CalorieGreen,
                        onClick = onNavigateToActivity,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Daily Calorie & Macronutrient Summary Card
            item {
                SamsungHealthCard(
                    title = "Energy Balance & Macros",
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = CalorieGreen,
                    headerActionText = "View Diary",
                    onHeaderActionClick = onNavigateToFood
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CalorieProgressRing(
                            consumedCalories = nutritionSummary.totalCaloriesConsumed,
                            targetCalories = nutritionSummary.calorieTarget,
                            modifier = Modifier.padding(end = 16.dp)
                        )

                        MacroNutrientRow(
                            proteinGrams = nutritionSummary.totalProteinGrams,
                            targetProtein = nutritionSummary.targetProteinGrams,
                            carbsGrams = nutritionSummary.totalCarbsGrams,
                            targetCarbs = nutritionSummary.targetCarbsGrams,
                            fatGrams = nutritionSummary.totalFatGrams,
                            targetFat = nutritionSummary.targetFatGrams,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(
                            text = "Fiber: ${nutritionSummary.totalFiberGrams}g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Sugar: ${nutritionSummary.totalSugarGrams}g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${meals.size} meals logged",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Steps & Physical Activity Card
            item {
                val currentSteps = stepRecord?.steps ?: 0
                val goalSteps = userProfile.dailyStepGoal
                val distanceKm = (stepRecord?.distanceMeters ?: 0f) / 1000f
                val activeMins = stepRecord?.activeMinutes ?: 0
                val burnedCals = stepRecord?.caloriesBurned ?: (currentSteps * 0.04f).toInt()

                SamsungHealthCard(
                    title = "Daily Activity & Steps",
                    icon = Icons.Default.DirectionsWalk,
                    iconTint = HealthTeal,
                    headerActionText = "+ Log Steps",
                    onHeaderActionClick = { showQuickStepDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$currentSteps",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Goal: $goalSteps steps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("%.2f km", distanceKm),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$activeMins mins • $burnedCals kcal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    val stepProgress = if (goalSteps > 0) (currentSteps.toFloat() / goalSteps.toFloat()).coerceIn(0f, 1f) else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(stepProgress)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(HealthTeal)
                        )
                    }
                }
            }

            // Water Tracker Card
            item {
                SamsungHealthCard(
                    title = "Hydration",
                    icon = Icons.Default.Opacity,
                    iconTint = WaterCyan
                ) {
                    WaterQuickAddBar(
                        totalMl = waterTotal,
                        targetMl = userProfile.dailyWaterTargetMl,
                        onAddMl = { ml -> viewModel.addWater(ml) },
                        onUndo = { viewModel.undoWater() }
                    )
                }
            }

            // Today's Logged Meals Preview
            item {
                SamsungHealthCard(
                    title = "Today's Meals",
                    icon = Icons.Default.Restaurant,
                    iconTint = ProteinBlue,
                    headerActionText = "+ Add Food",
                    onHeaderActionClick = onNavigateToFood
                ) {
                    if (meals.isEmpty()) {
                        Text(
                            text = "No meals logged for this day yet. Tap '+ Add Food' or use the AI Food Scanner to log Dal Bhat, healthy snacks, or custom recipes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            meals.take(4).forEach { meal ->
                                MealRowItem(meal = meal, onDelete = { viewModel.deleteMeal(meal) })
                            }
                            if (meals.size > 4) {
                                Text(
                                    text = "+ ${meals.size - 4} more in Food Diary",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { onNavigateToFood() }
                                        .padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Health Status: Weight & Sleep
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Weight Mini Card
                    SamsungHealthCard(
                        title = "Weight",
                        icon = Icons.Default.MonitorWeight,
                        iconTint = ProteinBlue,
                        modifier = Modifier.weight(1f),
                        headerActionText = "Log",
                        onHeaderActionClick = onNavigateToProgress
                    ) {
                        val displayWeight = latestWeight?.weightKg ?: userProfile.weightKg
                        val bmi = NutritionEngine.calculateBmi(displayWeight, userProfile.heightCm)
                        Text(
                            text = "$displayWeight kg",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "BMI: ${bmi.bmi} (${bmi.category})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sleep Mini Card
                    SamsungHealthCard(
                        title = "Sleep",
                        icon = Icons.Default.Bedtime,
                        iconTint = SleepIndigo,
                        modifier = Modifier.weight(1f),
                        headerActionText = "Log",
                        onHeaderActionClick = onNavigateToProgress
                    ) {
                        val duration = sleepRecord?.durationMinutes ?: 450 // 7.5h default
                        val hours = duration / 60
                        val mins = duration % 60
                        Text(
                            text = "${hours}h ${mins}m",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Quality: ★★★★☆",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom bar
            }
        }
    }

    // Quick Log Steps Dialog
    if (showQuickStepDialog) {
        AlertDialog(
            onDismissRequest = { showQuickStepDialog = false },
            title = { Text("Log Steps", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter steps walked today:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = quickStepInput,
                        onValueChange = { quickStepInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Steps") },
                        placeholder = { Text("e.g. 6500") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("quick_steps_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = quickStepInput.toIntOrNull() ?: 0
                        if (count > 0) {
                            viewModel.logManualSteps(count)
                        }
                        showQuickStepDialog = false
                        quickStepInput = ""
                    },
                    modifier = Modifier.testTag("quick_steps_save_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickStepDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun QuickActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("quick_action_${label.lowercase().replace(" ", "_")}")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MealRowItem(
    meal: MealEntry,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = meal.mealType,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = meal.foodName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (meal.nepaliName.isNotBlank()) {
                Text(
                    text = meal.nepaliName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${meal.portionDescription} • P: ${meal.protein.toInt()}g C: ${meal.carbs.toInt()}g F: ${meal.fat.toInt()}g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${meal.calories} kcal",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
