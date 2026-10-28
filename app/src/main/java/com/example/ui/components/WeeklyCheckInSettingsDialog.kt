package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeeklyCheckInPreferences
import com.example.ui.theme.HealthTeal

@Composable
fun WeeklyCheckInSettingsDialog(
    currentPrefs: WeeklyCheckInPreferences,
    onDismiss: () -> Unit,
    onSave: (WeeklyCheckInPreferences) -> Unit
) {
    var selectedDay by remember { mutableStateOf(currentPrefs.preferredDay) }
    var selectedHour by remember { mutableIntStateOf(currentPrefs.reminderTimeHour) }
    var selectedMinute by remember { mutableIntStateOf(currentPrefs.reminderTimeMinute) }
    var remindersEnabled by remember { mutableStateOf(currentPrefs.remindersEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Weekend Check-In Reminders ⏰",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Enable Reminders", fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { remindersEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = HealthTeal)
                    )
                }

                if (remindersEnabled) {
                    Text(
                        text = "Preferred Weekend Day",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Saturday", "Sunday").forEach { day ->
                            FilterChip(
                                selected = selectedDay.equals(day, ignoreCase = true),
                                onClick = { selectedDay = day },
                                label = { Text(day) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        text = "Preferred Reminder Time",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Hour", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = {
                                selectedHour = (selectedHour + 1) % 24
                            }) {
                                Text(text = String.format("%02d", selectedHour), fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(":", fontWeight = FontWeight.Bold, fontSize = 20.sp)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Minute", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = {
                                selectedMinute = (selectedMinute + 15) % 60
                            }) {
                                Text(text = String.format("%02d", selectedMinute), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentPrefs.copy(
                            preferredDay = selectedDay,
                            reminderTimeHour = selectedHour,
                            reminderTimeMinute = selectedMinute,
                            remindersEnabled = remindersEnabled
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = HealthTeal)
            ) {
                Text("Save Preferences")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
