package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonthlyGoalTarget
import com.example.domain.MonthlyReportEngine
import com.example.ui.components.SamsungHealthCard
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.ProteinBlue
import com.example.ui.theme.WorkoutOrange
import com.example.ui.viewmodel.HealthViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    viewModel: HealthViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsState()
    val summary by viewModel.monthlyReportSummary.collectAsState()
    val allMeals by viewModel.allMeals.collectAsState()
    val allWorkouts by viewModel.allWorkouts.collectAsState()

    var showEditTargetDialog by remember { mutableStateOf(false) }
    var showRestoreBackupDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    // Generate month options (last 6 months)
    val monthOptions = remember {
        val list = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance()
        val sdfKey = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfName = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        for (i in 0..5) {
            val key = sdfKey.format(cal.time)
            val name = if (i == 0) "Current (${sdfName.format(cal.time)})" else sdfName.format(cal.time)
            list.add(key to name)
            cal.add(Calendar.MONTH, -1)
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Health Report", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Selector
            item {
                Column {
                    Text(
                        text = "Select Reporting Month",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(monthOptions) { (key, label) ->
                            val isSelected = selectedMonthKey == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setMonthKey(key) },
                                label = { Text(label, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HealthTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Monthly Goals & Target Planning Card
            item {
                SamsungHealthCard(
                    title = "Monthly Goal Targets",
                    icon = Icons.Default.Flag,
                    iconTint = HealthTeal,
                    headerActionText = "Edit Goals",
                    onHeaderActionClick = { showEditTargetDialog = true }
                ) {
                    val t = summary.goalTarget
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MonthlyGoalRow(
                            label = "Walking Distance Target",
                            actualStr = String.format(Locale.US, "%.1f km", summary.totalWalkingDistanceKm),
                            targetStr = String.format(Locale.US, "%.1f km", t.targetMonthlyWalkingDistanceKm),
                            remainingStr = String.format(Locale.US, "%.1f km", (t.targetMonthlyWalkingDistanceKm - summary.totalWalkingDistanceKm).coerceAtLeast(0f)),
                            pct = if (t.targetMonthlyWalkingDistanceKm > 0) ((summary.totalWalkingDistanceKm / t.targetMonthlyWalkingDistanceKm) * 100).toInt() else 100
                        )

                        MonthlyGoalRow(
                            label = "Workout Sessions Target",
                            actualStr = "${summary.totalWorkoutSessions} sessions",
                            targetStr = "${t.targetWorkoutSessions} sessions",
                            remainingStr = "${(t.targetWorkoutSessions - summary.totalWorkoutSessions).coerceAtLeast(0)} sessions",
                            pct = if (t.targetWorkoutSessions > 0) ((summary.totalWorkoutSessions.toFloat() / t.targetWorkoutSessions) * 100).toInt() else 100
                        )

                        MonthlyGoalRow(
                            label = "Nutrition Target Days",
                            actualStr = "${summary.daysWithinCalorieTarget} days",
                            targetStr = "${t.targetDaysMeetingNutritionGoal} days",
                            remainingStr = "${(t.targetDaysMeetingNutritionGoal - summary.daysWithinCalorieTarget).coerceAtLeast(0)} days",
                            pct = if (t.targetDaysMeetingNutritionGoal > 0) ((summary.daysWithinCalorieTarget.toFloat() / t.targetDaysMeetingNutritionGoal) * 100).toInt() else 100
                        )

                        MonthlyGoalRow(
                            label = "Target Weight Change",
                            actualStr = String.format(Locale.US, "%.1f kg", summary.netWeightChangeKg),
                            targetStr = String.format(Locale.US, "%.1f kg", t.targetWeightChangeKg),
                            remainingStr = String.format(Locale.US, "%.1f kg", Math.abs(t.targetWeightChangeKg - summary.netWeightChangeKg)),
                            pct = summary.overallMonthlyCompletionPct
                        )
                    }
                }
            }

            // Food & Calorie Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = CalorieGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nutrition & Calorie Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatBox("Total Consumed", "${summary.totalCaloriesConsumed} kcal")
                            StatBox("Daily Avg Intake", "${summary.avgDailyCalorieIntake} kcal")
                            StatBox("Exercise Burned", "${summary.totalExerciseCalories} kcal")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatBox("Daily Net Avg", "${summary.avgDailyNetCalories} kcal")
                            StatBox("Days On Target", "${summary.daysWithinCalorieTarget} days")
                            StatBox("Days Exceeding", "${summary.daysExceedingCalorieTarget} days")
                        }
                    }
                }
            }

            // Walking & Exercise Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = WorkoutOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Walking & Exercise Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatBox("Total Distance", String.format(Locale.US, "%.1f km", summary.totalWalkingDistanceKm))
                            StatBox("Total Workouts", "${summary.totalWorkoutSessions}")
                            StatBox("Total Duration", "${summary.totalWalkingDurationMinutes} mins")
                        }
                    }
                }
            }

            // Weight Progress Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Scale, contentDescription = null, tint = HealthTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Weight Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatBox("Start Weight", "${summary.startingWeightKg} kg")
                            StatBox("Latest Weight", "${summary.latestWeightKg} kg")
                            StatBox("Net Change", String.format(Locale.US, "%.1f kg", summary.netWeightChangeKg))
                        }

                        if (summary.weightTrend.size >= 2) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Weight Trend Chart", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            SimpleLineChart(
                                points = summary.weightTrend.map { it.second },
                                color = HealthTeal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            )
                        }
                    }
                }
            }

            // Monthly Insights Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Monthly Insights & Feedback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        summary.insights.forEach { insight ->
                            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CalorieGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(insight, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Export, Backup & Share Actions Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Export, Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Button(
                            onClick = {
                                val csv = MonthlyReportEngine.exportMonthlyReportCsv(summary, allMeals, allWorkouts)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_SUBJECT, "NutriGlass Monthly Report - ${summary.monthDisplayName}")
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share CSV Report"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = HealthTeal)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share CSV Monthly Report")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.generateBackupJsonString { json ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("NutriGlass Database Backup", json))
                                    Toast.makeText(context, "Database JSON copied to clipboard!", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backup Database to JSON")
                        }

                        OutlinedButton(
                            onClick = { showRestoreBackupDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore Database from JSON")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }

    // Edit Target Dialog
    if (showEditTargetDialog) {
        val t = summary.goalTarget
        var walkingTarget by remember { mutableStateOf(t.targetMonthlyWalkingDistanceKm.toInt().toString()) }
        var workoutTarget by remember { mutableStateOf(t.targetWorkoutSessions.toString()) }
        var nutritionDaysTarget by remember { mutableStateOf(t.targetDaysMeetingNutritionGoal.toString()) }
        var dailyCalTarget by remember { mutableStateOf(t.dailyCalorieTarget.toString()) }

        AlertDialog(
            onDismissRequest = { showEditTargetDialog = false },
            title = { Text("Edit Goals for ${summary.monthDisplayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = walkingTarget,
                        onValueChange = { walkingTarget = it },
                        label = { Text("Target Walking Distance (km)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = workoutTarget,
                        onValueChange = { workoutTarget = it },
                        label = { Text("Target Workout Sessions") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nutritionDaysTarget,
                        onValueChange = { nutritionDaysTarget = it },
                        label = { Text("Target Nutrition Days") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dailyCalTarget,
                        onValueChange = { dailyCalTarget = it },
                        label = { Text("Daily Calorie Target (kcal)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTarget = MonthlyGoalTarget(
                            monthKey = selectedMonthKey,
                            targetMonthlyWalkingDistanceKm = walkingTarget.toFloatOrNull() ?: 60f,
                            targetWorkoutSessions = workoutTarget.toIntOrNull() ?: 15,
                            targetDaysMeetingNutritionGoal = nutritionDaysTarget.toIntOrNull() ?: 20,
                            dailyCalorieTarget = dailyCalTarget.toIntOrNull() ?: 2000
                        )
                        viewModel.saveMonthlyGoalTarget(newTarget)
                        showEditTargetDialog = false
                    }
                ) {
                    Text("Save Goals")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTargetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Backup Dialog
    if (showRestoreBackupDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreBackupDialog = false },
            title = { Text("Restore Database Backup") },
            text = {
                Column {
                    Text("Paste JSON backup data below to restore your health records:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("Paste JSON string here...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonText.isNotBlank()) {
                            viewModel.restoreDatabaseFromJson(restoreJsonText) { success ->
                                if (success) {
                                    Toast.makeText(context, "Database restored successfully!", Toast.LENGTH_LONG).show()
                                    showRestoreBackupDialog = false
                                } else {
                                    Toast.makeText(context, "Invalid JSON backup format", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Restore Records")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreBackupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MonthlyGoalRow(
    label: String,
    actualStr: String,
    targetStr: String,
    remainingStr: String,
    pct: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("$pct%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = HealthTeal)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (pct / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (pct >= 100) CalorieGreen else HealthTeal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Actual: $actualStr • Target: $targetStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Remaining: $remainingStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SimpleLineChart(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val min = points.minOrNull() ?: 0f
    val max = points.maxOrNull() ?: 1f
    val range = (max - min).coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1).coerceAtLeast(1)

        val path = Path()
        points.forEachIndexed { i, value ->
            val x = i * stepX
            val y = height - ((value - min) / range * height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}
