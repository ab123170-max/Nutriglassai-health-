package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SamsungHealthCard
import com.example.ui.components.RechartsWeightLineChart
import com.example.ui.components.WeightChartPoint
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.WorkoutOrange
import com.example.ui.viewmodel.HealthViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyProgressDashboardScreen(
    viewModel: HealthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCheckIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val measurementPrefs by viewModel.measurementPreferences.collectAsState()
    val allCheckIns by viewModel.allWeeklyCheckIns.collectAsState()
    val latestCheckIn by viewModel.latestWeeklyCheckIn.collectAsState()
    val latestPlan by viewModel.latestWeeklyPlan.collectAsState()

    val isLb = measurementPrefs.weightUnit.equals("lb", ignoreCase = true)
    val unitLabel = if (isLb) "lb" else "kg"

    val currentWeightKg = latestCheckIn?.currentWeightKg ?: profile.weightKg
    val startingWeightKg = latestCheckIn?.startingWeightKg ?: profile.weightKg
    val targetWeightKg = profile.targetWeightKg

    val convertedCurrent = if (isLb) currentWeightKg * 2.20462f else currentWeightKg
    val convertedStarting = if (isLb) startingWeightKg * 2.20462f else startingWeightKg
    val convertedTarget = if (isLb) targetWeightKg * 2.20462f else targetWeightKg

    val weeklyChangeKg = latestCheckIn?.weightChangeWeeklyKg ?: 0f
    val totalChangeKg = convertedCurrent - convertedStarting
    val remainingDiffKg = convertedCurrent - convertedTarget

    val convertedWeeklyChange = if (isLb) weeklyChangeKg * 2.20462f else weeklyChangeKg

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Progress Dashboard 📈", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCheckIn) {
                        Icon(Icons.Default.Add, contentDescription = "New Check-In")
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
                Spacer(modifier = Modifier.height(4.dp))
                // Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HealthNavy),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Current Scale Weight", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text(
                                    text = "${String.format("%.1f", convertedCurrent)} $unitLabel",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    color = Color.White
                                )
                            }
                            Button(
                                onClick = onNavigateToCheckIn,
                                colors = ButtonDefaults.buttonColors(containerColor = HealthTeal)
                            ) {
                                Text("Check In")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Starting Weight", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text("${String.format("%.1f", convertedStarting)} $unitLabel", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Target Goal", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text("${String.format("%.1f", convertedTarget)} $unitLabel", color = CalorieGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Total Progress", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                val sign = if (totalChangeKg <= 0) "" else "+"
                                Text("$sign${String.format("%.1f", totalChangeKg)} $unitLabel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Recharts Line Chart Component
            item {
                val chartPoints = remember(allCheckIns, isLb) {
                    allCheckIns.map { checkIn ->
                        val displayW = if (isLb) checkIn.currentWeightKg * 2.20462f else checkIn.currentWeightKg
                        val displayTarget = if (isLb) checkIn.targetWeightKg * 2.20462f else checkIn.targetWeightKg
                        val displayWeeklyChange = if (isLb) checkIn.weightChangeWeeklyKg * 2.20462f else checkIn.weightChangeWeeklyKg
                        WeightChartPoint(
                            dateLabel = checkIn.checkInDate,
                            rawDateString = checkIn.checkInDate,
                            weightValue = displayW,
                            targetValue = displayTarget,
                            weeklyChange = displayWeeklyChange,
                            status = checkIn.progressStatus
                        )
                    }
                }

                RechartsWeightLineChart(
                    points = chartPoints,
                    unitLabel = unitLabel,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Weekly Habit Metrics
            item {
                SamsungHealthCard(
                    title = "Weekly Habit & Activity Performance",
                    icon = Icons.Default.FitnessCenter
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Weekly Change", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val sign = if (convertedWeeklyChange <= 0) "" else "+"
                                Text(
                                    text = "$sign${String.format("%.1f", convertedWeeklyChange)} $unitLabel",
                                    fontWeight = FontWeight.Bold,
                                    color = if (convertedWeeklyChange <= 0) CalorieGreen else WorkoutOrange,
                                    fontSize = 16.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Remaining to Target", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${String.format("%.1f", abs(remainingDiffKg))} $unitLabel",
                                    fontWeight = FontWeight.Bold,
                                    color = HealthNavy,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // AI Personal Motivation Message Card
            item {
                SamsungHealthCard(
                    title = "Personalized Weekly AI Message",
                    icon = Icons.Default.AutoAwesome
                ) {
                    Text(
                        text = latestCheckIn?.aiMotivationMessage ?: "You showed up for yourself this week! Your consistency is worth celebrating.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = HealthNavy
                    )
                }
            }

            // Check-In History List with Delete Controls
            item {
                Text(
                    text = "Weekly Check-In History (${allCheckIns.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            items(allCheckIns) { checkIn ->
                val displayW = if (isLb) checkIn.currentWeightKg * 2.20462f else checkIn.currentWeightKg
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = checkIn.checkInDate,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${String.format("%.1f", displayW)} $unitLabel • Status: ${checkIn.progressStatus}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = {
                            viewModel.deleteWeeklyCheckIn(checkIn.id)
                            Toast.makeText(context, "Check-in removed", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
