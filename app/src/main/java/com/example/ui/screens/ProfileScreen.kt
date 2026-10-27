package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.NutritionEngine
import com.example.ui.components.SamsungHealthCard
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.ProteinBlue
import com.example.ui.viewmodel.HealthViewModel

@Composable
fun ProfileScreen(
    viewModel: HealthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(userProfile.name) }
    var editAge by remember { mutableStateOf(userProfile.age.toString()) }
    var editHeight by remember { mutableStateOf(userProfile.heightCm.toInt().toString()) }
    var editWeight by remember { mutableStateOf(userProfile.weightKg.toInt().toString()) }
    var editTargetWeight by remember { mutableStateOf(userProfile.targetWeightKg.toInt().toString()) }
    var editGoal by remember { mutableStateOf(userProfile.fitnessGoal) }
    var editActivity by remember { mutableStateOf(userProfile.activityLevel) }

    // Settings switches
    var waterReminders by remember { mutableStateOf(true) }
    var mealReminders by remember { mutableStateOf(true) }
    var stepReminders by remember { mutableStateOf(true) }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Profile Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(HealthTeal.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = HealthTeal,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = userProfile.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${userProfile.gender} • ${userProfile.age} yrs • ${userProfile.heightCm.toInt()} cm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                editName = userProfile.name
                                editAge = userProfile.age.toString()
                                editHeight = userProfile.heightCm.toInt().toString()
                                editWeight = userProfile.weightKg.toInt().toString()
                                editTargetWeight = userProfile.targetWeightKg.toInt().toString()
                                editGoal = userProfile.fitnessGoal
                                editActivity = userProfile.activityLevel
                                showEditProfileDialog = true
                            },
                            modifier = Modifier.testTag("edit_profile_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProfileStatBadge("Current", "${userProfile.weightKg} kg")
                        ProfileStatBadge("Target", "${userProfile.targetWeightKg} kg")
                        ProfileStatBadge("Target Cal", "${userProfile.dailyCalorieTarget} kcal")
                        ProfileStatBadge("Steps", "${userProfile.dailyStepGoal}")
                    }
                }
            }
        }

        // Dietary & Fitness Goals Card
        item {
            SamsungHealthCard(
                title = "Health Objectives",
                icon = Icons.Default.Tune,
                iconTint = ProteinBlue
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalRow("Primary Goal", userProfile.fitnessGoal)
                    GoalRow("Activity Level", userProfile.activityLevel)
                    GoalRow("Dietary Preference", userProfile.dietaryPreference)
                    GoalRow("Daily Water Goal", "${userProfile.dailyWaterTargetMl} ml")
                    GoalRow("Target Sleep", "${userProfile.sleepTargetHours.toInt()} hrs / night")
                }
            }
        }

        // Reminders & Notifications Card
        item {
            SamsungHealthCard(
                title = "Notifications & Reminders",
                icon = Icons.Default.Notifications,
                iconTint = Color(0xFFF59E0B)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReminderToggleRow("Meal Tracking Reminders", "Breakfast, lunch, and dinner logging prompts", mealReminders) { mealReminders = it }
                    ReminderToggleRow("Hydration Alerts", "Reminders every 2-3 hours to sip water", waterReminders) { waterReminders = it }
                    ReminderToggleRow("Walking Goal Alerts", "Progress notifications toward step goals", stepReminders) { stepReminders = it }
                }
            }
        }

        // Data Management, Export & Privacy
        item {
            SamsungHealthCard(
                title = "Privacy & Data Management",
                icon = Icons.Default.Security,
                iconTint = CalorieGreen
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            exportedJsonText = viewModel.exportHealthDataJson()
                            showExportDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_data_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Health Data (JSON Summary)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Database Storage", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("Local Room SQLite • Full offline functionality • Zero unauthorized cloud sharing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Personal Profile", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = editAge,
                                onValueChange = { editAge = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Age") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editHeight,
                                onValueChange = { editHeight = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Height (cm)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = editWeight,
                                onValueChange = { editWeight = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Weight (kg)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editTargetWeight,
                                onValueChange = { editTargetWeight = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Target (kg)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Text("Fitness Goal:", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Lose Weight", "Maintain Weight", "Gain Muscle").forEach { g ->
                                FilterChip(
                                    selected = editGoal == g,
                                    onClick = { editGoal = g },
                                    label = { Text(g, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val age = editAge.toIntOrNull() ?: userProfile.age
                        val height = editHeight.toFloatOrNull() ?: userProfile.heightCm
                        val weight = editWeight.toFloatOrNull() ?: userProfile.weightKg
                        val targetWeight = editTargetWeight.toFloatOrNull() ?: userProfile.targetWeightKg

                        val bmr = NutritionEngine.calculateBmr(weight, height, age, userProfile.gender)
                        val tdee = NutritionEngine.calculateTdee(bmr, editActivity)
                        val newCalTarget = NutritionEngine.calculateCalorieTarget(tdee, editGoal, userProfile.gender)

                        viewModel.saveProfile(
                            userProfile.copy(
                                name = editName,
                                age = age,
                                heightCm = height,
                                weightKg = weight,
                                targetWeightKg = targetWeight,
                                fitnessGoal = editGoal,
                                activityLevel = editActivity,
                                dailyCalorieTarget = newCalTarget
                            )
                        )
                        showEditProfileDialog = false
                    },
                    modifier = Modifier.testTag("save_profile_dialog_btn")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Data Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exported Health Records", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Your complete health log is formatted in standard JSON for easy export or physician sharing:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.height(180.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(10.dp)) {
                            item {
                                Text(
                                    text = exportedJsonText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("NutriGlass Export", exportedJsonText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Export copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    }
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ProfileStatBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GoalRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReminderToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
