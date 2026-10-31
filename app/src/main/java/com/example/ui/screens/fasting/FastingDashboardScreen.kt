package com.example.ui.screens.fasting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.FastingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingDashboardScreen(
    viewModel: FastingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChooseMethod: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToTimer: () -> Unit,
    onNavigateToEatingWindow: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToNutrition: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSafety: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val schedules by viewModel.schedules.collectAsState()

    val currentProtocol = schedules.find { it.protocolId == profile?.selectedProtocolId } ?: schedules.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intelligent Fasting Engine") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSafety, modifier = Modifier.testTag("safety_info_btn")) {
                        Icon(Icons.Default.Security, contentDescription = "Medical Safety")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = currentProtocol?.protocolName ?: "16:8 LeanGains Fast",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (activeSession != null) "Session Active" else "Ready to Fast",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Button(
                                onClick = {
                                    if (activeSession != null) onNavigateToTimer()
                                    else viewModel.startFasting(currentProtocol?.protocolId ?: "16_8", currentProtocol?.fastingHours ?: 16)
                                },
                                modifier = Modifier.testTag("quick_start_fast_btn")
                            ) {
                                Text(if (activeSession != null) "View Timer" else "Start Fast")
                            }
                        }
                    }
                }
            }

            // Quick Navigation Grid
            item {
                Text(
                    text = "Fasting Management Modules",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModuleCard(
                            title = "Choose Method",
                            subtitle = "9 Protocols",
                            icon = Icons.Default.Tune,
                            modifier = Modifier.weight(1f),
                            testTag = "nav_choose_method",
                            onClick = onNavigateToChooseMethod
                        )
                        ModuleCard(
                            title = "Personal Setup",
                            subtitle = "Preferences & Schedule",
                            icon = Icons.Default.Person,
                            modifier = Modifier.weight(1f),
                            testTag = "nav_personal_setup",
                            onClick = onNavigateToSetup
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModuleCard(
                            title = "Live Timer",
                            subtitle = "Countdowns & State",
                            icon = Icons.Default.DateRange,
                            modifier = Modifier.weight(1f),
                            testTag = "nav_live_timer",
                            onClick = onNavigateToTimer
                        )
                        ModuleCard(
                            title = "Eating Window",
                            subtitle = "Meals & Intervals",
                            icon = Icons.Default.Restaurant,
                            modifier = Modifier.weight(1f),
                            testTag = "nav_eating_window",
                            onClick = onNavigateToEatingWindow
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModuleCard(
                            title = "Fasting Calendar",
                            subtitle = "Weekly & Monthly",
                            icon = Icons.Default.CalendarMonth,
                            modifier = Modifier.weight(1f),
                            testTag = "nav_fasting_calendar",
                            onClick = onNavigateToCalendar
                        )
                        ModuleCard(
                            title = "Fasting History",
                            subtitle = "Completed & Missed",
                            icon = Icons.Default.History,
                            modifier = Modifier.weight(1f),
                            testTag = "nav_fasting_history",
                            onClick = onNavigateToHistory
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModuleCard(
                            title = "USDA Nutrition",
                            subtitle = "Eating Window Macros",
                            icon = Icons.Default.Fastfood,
                            modifier = Modifier.weight(1f),
                            testTag = "nav_usda_nutrition",
                            onClick = onNavigateToNutrition
                        )
                        ModuleCard(
                            title = "Analytics",
                            subtitle = "Consistency & Trends",
                            icon = Icons.Default.Analytics,
                            modifier = Modifier.weight(1f),
                            testTag = "nav_analytics",
                            onClick = onNavigateToAnalytics
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModuleCard(
                            title = "Medical Safety",
                            subtitle = "Screening & Guidelines",
                            icon = Icons.Default.MedicalInformation,
                            modifier = Modifier.weight(1f),
                            testTag = "nav_medical_safety",
                            onClick = onNavigateToSafety
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
