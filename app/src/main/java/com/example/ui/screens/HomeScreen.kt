package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.HealthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HomeBackground = Color(0xFFF3F6FC)
private val HomeCard = Color(0xFFFFFFFF)
private val HomeInk = Color(0xFF11182D)
private val HomeMuted = Color(0xFF68718A)
private val HomeBlue = Color(0xFF1473EA)
private val HomeGreen = Color(0xFF20B889)
private val HomeOrange = Color(0xFFFF762F)
private val HomePurple = Color(0xFF8964DB)

@Composable
fun HomeScreen(
    viewModel: HealthViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToFood: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToAiCoach: () -> Unit,
    onNavigateToBodyComposition: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val meals by viewModel.mealsForDate.collectAsState()
    val nutrition by viewModel.nutritionSummary.collectAsState()
    val steps by viewModel.stepRecord.collectAsState()
    val water by viewModel.waterTotalMl.collectAsState()
    val weight by viewModel.latestWeight.collectAsState()

    val currentSteps = steps?.steps ?: 0
    val distanceKm = (steps?.distanceMeters ?: 0f) / 1000f
    val displayWeight = weight?.weightKg ?: profile.weightKg
    val calorieTarget = nutrition.calorieTarget.coerceAtLeast(1)
    val calorieProgress = (nutrition.totalCaloriesConsumed.toFloat() / calorieTarget).coerceIn(0f, 1f)
    val stepTarget = profile.dailyStepGoal.coerceAtLeast(1)
    val stepProgress = (currentSteps.toFloat() / stepTarget).coerceIn(0f, 1f)
    val waterTarget = profile.dailyWaterTargetMl.coerceAtLeast(1)
    val today = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())

    LazyColumn(
        modifier = modifier.fillMaxSize().background(HomeBackground),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(15.dp), color = HomeBlue, modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocalFlorist, null, tint = Color.White, modifier = Modifier.size(27.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("NutriGlass AI", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = HomeBlue)
                        Text("Scan • Track • Transform", fontSize = 11.sp, color = HomeInk, fontWeight = FontWeight.Medium)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateToAiCoach) {
                        Icon(Icons.Default.NotificationsNone, "Notifications", tint = HomeInk, modifier = Modifier.size(25.dp))
                    }
                    IconButton(onClick = onNavigateToProgress) {
                        Icon(Icons.Default.Settings, "Settings and profile", tint = HomeInk, modifier = Modifier.size(25.dp))
                    }
                }
            }
        }

        item {
            Surface(
                color = Color(0xFFDCEAFF),
                shape = RoundedCornerShape(27.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = HomeBlue, modifier = Modifier.size(48.dp))
                        }
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Hello, ${profile.name.ifBlank { "there" }}!",
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HomeInk
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Keep going! Your health journey starts with today's small steps. 💪",
                            fontSize = 12.sp, lineHeight = 16.sp, color = HomeInk)
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(color = Color.White.copy(alpha = 0.75f), shape = RoundedCornerShape(17.dp)) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CalendarMonth, null, tint = HomeBlue, modifier = Modifier.size(21.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(today, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HomeInk)
                            Text("Today", fontSize = 10.sp, color = HomeMuted)
                        }
                    }
                }
            }
        }

        item {
            Surface(color = HomeCard, shape = RoundedCornerShape(27.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✿", color = HomePurple, fontSize = 27.sp)
                            Spacer(Modifier.width(9.dp))
                            Text("Mindfulness & Wellness", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HomeInk)
                        }
                        TextButton(onClick = onNavigateToAiCoach, contentPadding = PaddingValues(0.dp)) {
                            Text("See all  ›", color = HomeInk, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WellnessTile("Mood check-in", "How are you feeling?", "☀", Color(0xFFFFE5E0), Color(0xFFFF795D), onNavigateToAiCoach, Modifier.weight(1f))
                        WellnessTile("Breathing", "Relax and reset", "◉", Color(0xFFECE7FF), HomePurple, onNavigateToAiCoach, Modifier.weight(1f))
                        WellnessTile("Meditation", "Find your calm", "✿", Color(0xFFDDF7EC), HomeGreen, onNavigateToAiCoach, Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetricCard("Heart Rate", "— bpm", "Not available", Icons.Default.Favorite, Color(0xFFFF4778), onNavigateToProgress, Modifier.weight(1f))
                SmallMetricCard("Stress", "—", "Try breathing", Icons.Default.Psychology, HomeOrange, onNavigateToAiCoach, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetricCard("Blood Oxygen", "— %", "No sensor reading", Icons.Default.WaterDrop, HomeBlue, onNavigateToProgress, Modifier.weight(1f))
                Column(Modifier.weight(1f)) {
                    HomeMetricCard(
                        title = "Calories",
                        value = "${nutrition.totalCaloriesConsumed} kcal",
                        subtitle = "Target: ${nutrition.calorieTarget} kcal",
                        icon = Icons.Default.LocalFireDepartment,
                        accent = HomeOrange,
                        progress = calorieProgress,
                        onClick = onNavigateToFood
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeMetricCard(
                    title = "Steps",
                    value = "%,d".format(currentSteps),
                    subtitle = "Target: %,d".format(profile.dailyStepGoal),
                    icon = Icons.Default.DirectionsWalk,
                    accent = HomeBlue,
                    progress = stepProgress,
                    onClick = onNavigateToActivity,
                    modifier = Modifier.weight(1f)
                )
                HomeMetricCard(
                    title = "Walking Distance",
                    value = String.format(Locale.getDefault(), "%.2f km", distanceKm),
                    subtitle = "Today's recorded distance",
                    icon = Icons.Default.LocationOn,
                    accent = HomeBlue,
                    progress = null,
                    onClick = onNavigateToActivity,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeMetricCard(
                    title = "Hydration",
                    value = if (water >= 1000) String.format(Locale.getDefault(), "%.1f L", water / 1000f) else "${water} ml",
                    subtitle = "Target: ${String.format(Locale.getDefault(), "%.1f", waterTarget / 1000f)} L",
                    icon = Icons.Default.LocalDrink,
                    accent = HomeBlue,
                    progress = (water.toFloat() / waterTarget).coerceIn(0f, 1f),
                    onClick = onNavigateToFood,
                    modifier = Modifier.weight(1f)
                )
                HomeMetricCard(
                    title = "Weight",
                    value = "${displayWeight} kg",
                    subtitle = "Your latest recorded weight",
                    icon = Icons.Default.MonitorWeight,
                    accent = HomePurple,
                    progress = null,
                    onClick = onNavigateToBodyComposition,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Surface(
                color = HomeCard,
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToScan)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = Color(0xFFEAF3FF), shape = RoundedCornerShape(19.dp), modifier = Modifier.size(76.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Restaurant, null, tint = HomeGreen, modifier = Modifier.size(43.dp))
                            Icon(Icons.Default.CenterFocusStrong, null, tint = HomeBlue, modifier = Modifier.size(29.dp).align(Alignment.BottomEnd))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("AI Food Scanner", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HomeInk)
                        Spacer(Modifier.height(4.dp))
                        Text("Scan, analyze and review nutrition information.", fontSize = 11.sp, lineHeight = 15.sp, color = HomeMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onNavigateToScan,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HomeBlue),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text("Scan Now  ›", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Surface(color = Color(0xFFE7F1FF), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Insights, null, tint = HomeBlue, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Daily summary", color = HomeInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${meals.size} meals logged • ${nutrition.totalCaloriesConsumed} kcal recorded today",
                            color = HomeMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WellnessTile(
    title: String, subtitle: String, symbol: String, background: Color, accent: Color,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.height(142.dp).clip(RoundedCornerShape(24.dp)).background(background)
            .clickable(onClick = onClick).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(symbol, color = accent, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Column {
            Text(title, color = HomeInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
            Text(subtitle, color = HomeMuted, fontSize = 9.sp, lineHeight = 12.sp)
        }
    }
}

@Composable
private fun SmallMetricCard(
    title: String, value: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Surface(color = HomeCard, shape = RoundedCornerShape(25.dp), modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HomeInk)
            }
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk)
            Text(subtitle, fontSize = 10.sp, color = HomeMuted, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun HomeMetricCard(
    title: String, value: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color,
    progress: Float?, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Surface(color = HomeCard, shape = RoundedCornerShape(25.dp), modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(7.dp))
                Text(title, color = HomeInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Spacer(Modifier.height(10.dp))
            Text(value, color = HomeInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(subtitle, color = HomeMuted, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2)
            if (progress != null) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color(0xFFE9EDF4))) {
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(accent))
                }
            }
        }
    }
}
