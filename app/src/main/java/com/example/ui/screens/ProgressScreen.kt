package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.NutritionEngine
import com.example.ui.components.SamsungHealthCard
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.ProteinBlue
import com.example.ui.theme.SleepIndigo
import com.example.ui.theme.WorkoutOrange
import com.example.ui.viewmodel.HealthViewModel

@Composable
fun ProgressScreen(
    viewModel: HealthViewModel,
    modifier: Modifier = Modifier
) {
    val weightHistory by viewModel.weightHistory.collectAsState()
    val latestWeight by viewModel.latestWeight.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val allAchievements by viewModel.allAchievements.collectAsState()
    val allSleepRecords by viewModel.allSleepRecords.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Weight & BMI, 1: 20 Achievements, 2: Sleep Diary

    // Weight Dialog
    var showWeightDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf(userProfile.weightKg.toString()) }
    var waistInput by remember { mutableStateOf("") }
    var weightNotes by remember { mutableStateOf("") }

    // Sleep Dialog
    var showSleepDialog by remember { mutableStateOf(false) }
    var sleepHoursInput by remember { mutableStateOf("7.5") }
    var sleepQualityInput by remember { mutableIntStateOf(4) }
    var sleepNotesInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Progress & Health Milestones",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track weight trends, unlock 20 health achievements, and review sleep patterns",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ScrollableTabRow(
            selectedTabIndex = activeTab,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Weight & BMI", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("20 Achievements", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Sleep Tracker", fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        when (activeTab) {
            0 -> {
                // WEIGHT TRACKING & BMI
                val currentWeight = latestWeight?.weightKg ?: userProfile.weightKg
                val targetWeight = userProfile.targetWeightKg
                val diffFromTarget = currentWeight - targetWeight
                val bmi = NutritionEngine.calculateBmi(currentWeight, userProfile.heightCm)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SamsungHealthCard(
                            title = "Weight Trend & Goal",
                            icon = Icons.Default.MonitorWeight,
                            iconTint = ProteinBlue,
                            headerActionText = "+ Log Weight",
                            onHeaderActionClick = { showWeightDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "$currentWeight kg",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (diffFromTarget > 0) "${String.format("%.1f", diffFromTarget)} kg to target ($targetWeight kg)"
                                        else "Target achieved!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = "BMI ${bmi.bmi}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(text = bmi.category, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Interactive Trend Canvas
                            Text("Weight History Trend:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))

                            WeightTrendGraph(
                                weightHistory = weightHistory.ifEmpty {
                                    listOf(
                                        com.example.data.model.WeightRecord(dateString = "Start", weightKg = userProfile.weightKg),
                                        com.example.data.model.WeightRecord(dateString = "Now", weightKg = currentWeight)
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Healthy BMI weight range for your height: ${bmi.healthyWeightMinKg} - ${bmi.healthyWeightMaxKg} kg",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Weight logs history
                    item {
                        Text("Recorded Weights (${weightHistory.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    if (weightHistory.isEmpty()) {
                        item {
                            Text("No weight entries logged yet. Tap '+ Log Weight' to start tracking your progress over time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(weightHistory.reversed()) { record ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${record.weightKg} kg",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${record.dateString}${if (record.waistCm != null) " • Waist: ${record.waistCm}cm" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (record.notes.isNotBlank()) {
                                            Text(text = record.notes, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    IconButton(onClick = { viewModel.deleteWeight(record.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            1 -> {
                // 20 HEALTH ACHIEVEMENTS & LEVELS
                val unlockedCount = allAchievements.count { it.isUnlocked }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SamsungHealthCard(
                            title = "Achievement Levels ($unlockedCount / 20 Unlocked)",
                            icon = Icons.Default.EmojiEvents,
                            iconTint = WorkoutOrange
                        ) {
                            Text(
                                text = "Complete daily habits, log authentic meals, reach step counts, and maintain streaks to conquer all 20 milestones.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(allAchievements) { ach ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (ach.isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (ach.isUnlocked) CalorieGreen.copy(alpha = 0.15f)
                                            else Color.Gray.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (ach.isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (ach.isUnlocked) CalorieGreen else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = ach.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (ach.isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "Lvl ${ach.level}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = ach.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (ach.isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            2 -> {
                // SLEEP TRACKER
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        SamsungHealthCard(
                            title = "Sleep Diary",
                            icon = Icons.Default.Bedtime,
                            iconTint = SleepIndigo,
                            headerActionText = "+ Log Sleep",
                            onHeaderActionClick = { showSleepDialog = true }
                        ) {
                            Text(
                                text = "Restorative sleep is essential for hormonal balance, appetite regulation, and cellular recovery.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (allSleepRecords.isEmpty()) {
                        item {
                            Text(
                                text = "No sleep logs recorded yet. Tap '+ Log Sleep' to log your bedtime, wake time, and rest quality.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(allSleepRecords) { record ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${record.durationMinutes / 60}h ${record.durationMinutes % 60}m",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Date: ${record.dateString}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (record.notes.isNotBlank()) {
                                            Text(text = record.notes, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Row {
                                        repeat(record.qualityRating) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFF59E0B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Log Weight Dialog
    if (showWeightDialog) {
        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("Log Weight", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg)") },
                        trailingIcon = { Text("kg", modifier = Modifier.padding(end = 12.dp)) },
                        modifier = Modifier.fillMaxWidth().testTag("log_weight_input")
                    )
                    OutlinedTextField(
                        value = waistInput,
                        onValueChange = { waistInput = it },
                        label = { Text("Waist circumference (cm, optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = weightNotes,
                        onValueChange = { weightNotes = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = weightInput.toFloatOrNull()
                        if (w != null && w > 20f) {
                            val waist = waistInput.toFloatOrNull()
                            viewModel.logWeight(w, waist, weightNotes)
                        }
                        showWeightDialog = false
                    },
                    modifier = Modifier.testTag("save_weight_dialog_btn")
                ) {
                    Text("Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeightDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Log Sleep Dialog
    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("Log Sleep Duration", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sleepHoursInput,
                        onValueChange = { sleepHoursInput = it },
                        label = { Text("Duration (Hours)") },
                        placeholder = { Text("e.g. 7.5") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Sleep Quality (1 - 5 stars):", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (star <= sleepQualityInput) Color(0xFFF59E0B) else Color.LightGray,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { sleepQualityInput = star }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = sleepNotesInput,
                        onValueChange = { sleepNotesInput = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hours = sleepHoursInput.toFloatOrNull() ?: 7.5f
                        val minutes = (hours * 60).toInt()
                        viewModel.logSleep(
                            durationMinutes = minutes,
                            bedtimeMillis = System.currentTimeMillis() - (minutes * 60000L),
                            wakeMillis = System.currentTimeMillis(),
                            quality = sleepQualityInput,
                            notes = sleepNotesInput
                        )
                        showSleepDialog = false
                    }
                ) {
                    Text("Save Sleep")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WeightTrendGraph(
    weightHistory: List<com.example.data.model.WeightRecord>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val weights = weightHistory.map { it.weightKg }
            val minW = (weights.minOrNull() ?: 60f) - 1f
            val maxW = (weights.maxOrNull() ?: 75f) + 1f
            val rangeW = (maxW - minW).coerceAtLeast(1f)

            val width = size.width
            val height = size.height

            if (weights.size >= 2) {
                val stepX = width / (weights.size - 1)
                val path = Path()

                weights.forEachIndexed { index, w ->
                    val x = index * stepX
                    val y = height - ((w - minW) / rangeW * height)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    drawCircle(
                        color = Color(0xFF3B82F6),
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                drawPath(
                    path = path,
                    color = Color(0xFF3B82F6),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}
