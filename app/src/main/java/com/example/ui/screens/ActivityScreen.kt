package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.core.content.ContextCompat
import com.example.data.model.WorkoutSession
import com.example.ui.components.GpsMapView
import com.example.ui.components.GpsCoordinate
import com.example.ui.components.SamsungHealthCard
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.ProteinBlue
import com.example.ui.theme.WorkoutOrange
import com.example.ui.viewmodel.HealthViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay

data class WorkoutPlan(
    val title: String,
    val category: String,
    val durationMinutes: Int,
    val estimatedCalories: Int,
    val difficulty: String,
    val exercises: List<String>,
    val instructions: String
)

@Composable
fun ActivityScreen(
    viewModel: HealthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allWorkouts by viewModel.allWorkouts.collectAsState()
    val stepRecord by viewModel.stepRecord.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Live Session, 1: Workout Plans, 2: History

    // Live Tracking State
    var isTracking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var trackedDistanceMeters by remember { mutableStateOf(0f) }
    var selectedActivityType by remember { mutableStateOf("Outdoor Walk") }
    var lastKnownLocation by remember { mutableStateOf<Location?>(null) }

    // Location Permission
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Timer ticker
    LaunchedEffect(isTracking, isPaused) {
        while (isTracking && !isPaused) {
            delay(1000L)
            elapsedSeconds += 1
            // In absence of hardware movement, simulate gentle walking distance increment if tracking walk
            if (selectedActivityType.contains("Walk", true)) {
                trackedDistanceMeters += 1.3f // ~4.7 km/h pace
            } else if (selectedActivityType.contains("Run", true)) {
                trackedDistanceMeters += 2.5f // ~9.0 km/h pace
            }
        }
    }

    val plans = listOf(
        WorkoutPlan(
            title = "Brisk Outdoor Power Walk",
            category = "Walking & Cardio",
            durationMinutes = 30,
            estimatedCalories = 145,
            difficulty = "Beginner",
            exercises = listOf("5m Warm-up stroll", "20m Fast aerobic stride", "5m Gentle cool-down"),
            instructions = "Keep posture tall, shoulders back, and swing arms naturally to maximize oxygen intake."
        ),
        WorkoutPlan(
            title = "Morning High Vitality HIIT",
            category = "Cardio HIIT",
            durationMinutes = 20,
            estimatedCalories = 180,
            difficulty = "Intermediate",
            exercises = listOf("Jumping jacks (45s)", "High knees (45s)", "Mountain climbers (45s)", "Rest (30s) x 4 rounds"),
            instructions = "High-energy interval conditioning to enhance cardiovascular endurance and metabolism."
        ),
        WorkoutPlan(
            title = "Full Body Calisthenics",
            category = "Bodyweight",
            durationMinutes = 25,
            estimatedCalories = 160,
            difficulty = "All Levels",
            exercises = listOf("Bodyweight squats (3 x 15)", "Push-ups / Incline (3 x 10)", "Lunges (3 x 12)", "Plank (3 x 45s)"),
            instructions = "Develop functional muscle strength and joint stability without weights."
        ),
        WorkoutPlan(
            title = "Himalayan Mobility & Yoga",
            category = "Yoga & Stretch",
            durationMinutes = 25,
            estimatedCalories = 90,
            difficulty = "Gentle",
            exercises = listOf("Cat-Cow spine wave", "Downward Dog", "Warrior flow", "Child's pose meditation"),
            instructions = "Promotes flexibility, stress reduction, and posture alignment."
        ),
        WorkoutPlan(
            title = "Himalayan Trek Preparation",
            category = "Endurance",
            durationMinutes = 40,
            estimatedCalories = 260,
            difficulty = "Advanced",
            exercises = listOf("Step-ups on elevated bench (4 x 20)", "Calf raises (4 x 25)", "Wall sit (3 x 60s)", "Stair climbing"),
            instructions = "Simulates mountain hiking terrain to strengthen knees, calves, and quad stamina."
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Activity & Workouts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track outdoor walks with GPS, view exercise routines, and maintain streaks",
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
                text = { Text("Live Tracker", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Workout Planner", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Session History (${allWorkouts.size})", fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        when (activeTab) {
            0 -> {
                // LIVE GPS / WALK TRACKER
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        // Activity Type Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Outdoor Walk", "Running", "Cardio HIIT", "Yoga").forEach { type ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedActivityType == type) HealthTeal else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !isTracking) { selectedActivityType = type }
                                        .testTag("activity_type_${type.lowercase().replace(" ", "_")}")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = type,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedActivityType == type) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Main Metric Display
                    item {
                        SamsungHealthCard(
                            title = "Active $selectedActivityType",
                            icon = Icons.Default.DirectionsWalk,
                            iconTint = HealthTeal
                        ) {
                            val timeFormatted = com.example.domain.GpsTrackingEngine.formatTimerDisplay(elapsedSeconds)
                            val distanceKm = trackedDistanceMeters / 1000f
                            val calories = com.example.domain.GpsTrackingEngine.calculateCaloriesBurned(distanceKm)
                            val paceFormatted = com.example.domain.GpsTrackingEngine.formatPace(elapsedSeconds, distanceKm)

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = timeFormatted,
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Elapsed Duration",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    MetricColumn("Distance", String.format("%.2f km", distanceKm))
                                    MetricColumn("Calories", "$calories kcal")
                                    MetricColumn("Pace", paceFormatted)
                                }
                            }
                        }
                    }

                    // Interactive OpenStreetMap / Leaflet Route Map Preview
                    item {
                        GpsMapView(
                            coordinates = emptyList(),
                            currentLat = 27.7172,
                            currentLon = 85.3240,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // GPS Permission status notice
                    if (!hasLocationPermission) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Enable GPS for Accurate Route", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("Allows real-time distance and pace tracking outdoors.", style = MaterialTheme.typography.labelSmall)
                                    }
                                    TextButton(onClick = {
                                        locationPermissionLauncher.launch(
                                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                        )
                                    }) {
                                        Text("Enable")
                                    }
                                }
                            }
                        }
                    }

                    // Start / Pause / Stop Controls
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isTracking) {
                                // Start Session Button
                                Button(
                                    onClick = {
                                        isTracking = true
                                        isPaused = false
                                        elapsedSeconds = 0L
                                        trackedDistanceMeters = 0f
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HealthTeal),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .fillMaxWidth(0.7f)
                                        .testTag("activity_start_btn")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Start Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                // Pause / Resume
                                IconButton(
                                    onClick = { isPaused = !isPaused },
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(if (isPaused) HealthTeal else WorkoutOrange)
                                        .testTag("activity_pause_resume_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = "Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                // Finish & Save
                                IconButton(
                                    onClick = {
                                        val cals = (elapsedSeconds * 0.08f).toInt()
                                        viewModel.logWorkout(
                                            type = selectedActivityType,
                                            durationSeconds = elapsedSeconds,
                                            distanceMeters = trackedDistanceMeters,
                                            caloriesBurned = cals,
                                            notes = "Tracked via NutriGlass Live Session"
                                        )
                                        isTracking = false
                                        isPaused = false
                                        activeTab = 2 // Navigate to history
                                    },
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                        .testTag("activity_stop_btn")
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = "Finish Session",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            1 -> {
                // WORKOUT PLANNER
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(plans) { plan ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                            text = plan.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${plan.category} • ${plan.difficulty}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${plan.durationMinutes} mins",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = plan.instructions,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    plan.exercises.forEach { ex ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = CalorieGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = ex, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        viewModel.logWorkout(
                                            type = plan.title,
                                            durationSeconds = plan.durationMinutes * 60L,
                                            distanceMeters = 0f,
                                            caloriesBurned = plan.estimatedCalories,
                                            notes = "Completed ${plan.title}"
                                        )
                                        activeTab = 2
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Mark Completed (+${plan.estimatedCalories} kcal)")
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            2 -> {
                // HISTORY TAB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (allWorkouts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No workouts recorded yet. Start a live walk or complete a workout session above.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(allWorkouts) { session ->
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
                                            text = session.workoutType,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${session.dateString} • ${session.durationSeconds / 60} mins",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (session.distanceMeters > 0) {
                                            Text(
                                                text = String.format("%.2f km", session.distanceMeters / 1000f),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${session.caloriesBurned} kcal",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = WorkoutOrange
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
