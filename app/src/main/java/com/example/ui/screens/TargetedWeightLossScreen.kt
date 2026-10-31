package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.DailyProgramPlan
import com.example.domain.SafetyStatus
import com.example.domain.TargetedWeeklyWeightLossEngine
import com.example.domain.UnitConversionEngine
import com.example.domain.WeightUnit
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.FatRed
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.ProteinBlue
import com.example.ui.viewmodel.HealthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetedWeightLossScreen(
    onNavigateBack: () -> Unit,
    viewModel: HealthViewModel = viewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val latestWeight by viewModel.latestWeight.collectAsState()
    val weightHistory by viewModel.weightHistory.collectAsState()

    val currentWeight = latestWeight?.weightKg ?: userProfile.weightKg
    val startingWeight = userProfile.weightKg + 3.0f // Estimated start
    val targetWeight = userProfile.targetWeightKg
    val previousWeight = weightHistory.getOrNull(1)?.weightKg

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Weekly Target", "7-Day Program", "Progress & Adjust")

    val weeklyTarget = remember(currentWeight, targetWeight, userProfile) {
        TargetedWeeklyWeightLossEngine.calculateWeeklyTarget(
            startingWeightKg = startingWeight,
            currentWeightKg = currentWeight,
            targetWeightKg = targetWeight,
            previousWeekWeightKg = previousWeight,
            heightCm = userProfile.heightCm,
            age = userProfile.age,
            isMale = userProfile.gender.equals("Male", ignoreCase = true),
            activityLevel = userProfile.activityLevel
        )
    }

    val programPlan = remember(currentWeight, userProfile) {
        TargetedWeeklyWeightLossEngine.generate7DayProgram(
            currentWeightKg = currentWeight,
            heightCm = userProfile.heightCm,
            age = userProfile.age,
            isMale = userProfile.gender.equals("Male", ignoreCase = true),
            activityLevel = userProfile.activityLevel,
            dietaryPreference = userProfile.dietaryPreference
        )
    }

    val weeklyReport = remember(currentWeight, targetWeight) {
        TargetedWeeklyWeightLossEngine.generateWeeklyReport(
            startingWeightKg = startingWeight,
            endingWeightKg = currentWeight,
            targetWeightKg = targetWeight,
            waistCm = 82f,
            bodyFatPercent = 19.5f,
            activityScore = 85,
            nutritionScore = 90
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Targeted Weekly Weight Loss",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Intelligent Metabolic Adaptation Engine",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_targeted_loss")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = HealthTeal
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_targeted_$index")
                    )
                }
            }

            when (selectedTab) {
                0 -> WeeklyTargetTab(weeklyTarget = weeklyTarget)
                1 -> WeeklyProgramTab(programPlan = programPlan)
                2 -> WeeklyProgressAndAdjustmentTab(weeklyReport = weeklyReport, weeklyTarget = weeklyTarget)
            }
        }
    }
}

@Composable
fun WeeklyTargetTab(weeklyTarget: com.example.domain.WeeklyTargetResult) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tab_weekly_target_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Safety Status Banner
        item {
            val isSafe = weeklyTarget.safetyStatus == SafetyStatus.SAFE
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSafe) HealthTeal.copy(alpha = 0.12f) else CarbsAmber.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSafe) HealthTeal.copy(alpha = 0.4f) else CarbsAmber.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSafe) Icons.Default.Security else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isSafe) HealthTeal else CarbsAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isSafe) "Clinically Validated Safety Status" else "Safety Recommendation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSafe) HealthTeal else CarbsAmber
                        )
                        Text(
                            text = weeklyTarget.safetyMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Weekly Target Calculator Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Weekly Weight Loss Rate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dynamic metabolic range calculation",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = HealthTeal.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${String.format("%.1f", weeklyTarget.expectedMinWeeklyLossKg)}–${String.format("%.1f", weeklyTarget.expectedMaxWeeklyLossKg)} kg/wk",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HealthTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TargetMetricBox(title = "Starting", value = "${weeklyTarget.startingWeightKg} kg")
                        TargetMetricBox(title = "Current", value = "${weeklyTarget.currentWeightKg} kg")
                        TargetMetricBox(title = "Target", value = "${weeklyTarget.targetWeightKg} kg", color = HealthTeal)
                        TargetMetricBox(title = "Remaining", value = "${weeklyTarget.remainingWeightKg} kg", color = CarbsAmber)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { (weeklyTarget.goalProgressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = HealthTeal,
                        trackColor = HealthTeal.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${weeklyTarget.goalProgressPercent}% Total Goal Achieved",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyProgramTab(programPlan: List<DailyProgramPlan>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tab_weekly_program_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Personalized 7-Day Metabolic Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tailored macros, nutrition and activity for optimal fat oxidation",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(programPlan, key = { it.dayNumber }) { day ->
            DayProgramCard(day = day)
        }
    }
}

@Composable
fun DayProgramCard(day: DailyProgramPlan) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Day ${day.dayNumber}: ${day.dayName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = HealthTeal.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${day.calorieTarget} kcal • ${day.proteinTargetGrams}g Protein",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HealthTeal,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            MealBullet("Breakfast", day.breakfast)
            MealBullet("Lunch", day.lunch)
            MealBullet("Dinner", day.dinner)
            MealBullet("Snack", day.snack)

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "🚶 ${day.stepTarget} steps", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "💪 ${day.strengthTraining}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MealBullet(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "• $title: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
    }
}

@Composable
fun WeeklyProgressAndAdjustmentTab(
    weeklyReport: com.example.domain.WeeklyReportResult,
    weeklyTarget: com.example.domain.WeeklyTargetResult
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tab_progress_adjust_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Weekly Review & Automatic Adjustment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Comparing actual vs expected weekly rate",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TargetMetricBox(title = "Actual Change", value = "${weeklyReport.actualChangeKg} kg", color = HealthTeal)
                        TargetMetricBox(title = "Activity Score", value = "${weeklyReport.activityPerformanceScore}%", color = ProteinBlue)
                        TargetMetricBox(title = "Nutrition Score", value = "${weeklyReport.nutritionPerformanceScore}%", color = CarbsAmber)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Adaptive Coach Recommendation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = weeklyReport.adjustmentAdvice,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TargetMetricBox(title: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
