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

private val PageBg = Color(0xFFF1F1F4)
private val CardBg = Color(0xFFFCFCFE)
private val Ink = Color(0xFF25252A)
private val Muted = Color(0xFF85858D)
private val Orange = Color(0xFFFF6B24)
private val Blue = Color(0xFF4D9DF5)
private val Green = Color(0xFF39B980)

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

    LazyColumn(
        modifier = modifier.fillMaxSize().background(PageBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("NutriGlass AI", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateToProgress) {
                        Icon(Icons.Default.StarBorder, "Progress", tint = Color.Black, modifier = Modifier.size(29.dp))
                    }
                    IconButton(onClick = onNavigateToAiCoach) {
                        Icon(Icons.Default.MoreVert, "AI coach", tint = Color.Black)
                    }
                }
            }
        }

        item {
            Surface(color = CardBg, shape = RoundedCornerShape(30.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircleIcon(Icons.Default.SelfImprovement, Color(0xFF88888D))
                        Spacer(Modifier.width(12.dp))
                        Text("Mindfulness", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Spacer(Modifier.width(10.dp))
                        Surface(color = Color(0xFFAAAAAE), shape = CircleShape) {
                            Text("Daily", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MindfulnessTile("Mood\ncheck-in", "☀", Color(0xFFF2E2DC), Color(0xFFE78A69), onNavigateToAiCoach, Modifier.weight(1f))
                        MindfulnessTile("Breathing\nexercises", "◉", Color(0xFFEAE7F5), Color(0xFFB4A1E8), onNavigateToAiCoach, Modifier.weight(1f))
                        MindfulnessTile("Meditation", "❋", Color(0xFFDDF0EB), Color(0xFF66C5A6), onNavigateToAiCoach, Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            MetricCard(
                title = "Heart rate",
                subtitle = "No heart-rate sensor reading available",
                icon = Icons.Default.Favorite,
                accent = Orange,
                rightContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("—", color = Orange, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("BPM", color = Muted, fontSize = 10.sp)
                    }
                },
                onClick = onNavigateToProgress
            )
        }
        item {
            MetricCard(
                title = "Stress",
                subtitle = "Explore breathing and relaxation tools",
                icon = Icons.Default.Bolt,
                accent = Color(0xFFFFB21B),
                rightContent = {
                    Row(Modifier.width(112.dp).height(13.dp).clip(CircleShape)) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF16B7D8)))
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF75D63B)))
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFFFC52C)))
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFFF9828)))
                    }
                },
                onClick = onNavigateToAiCoach
            )
        }
        item {
            MetricCard(
                title = "Blood oxygen",
                subtitle = "SpO₂ readings need a compatible device",
                icon = Icons.Default.WaterDrop,
                accent = Blue,
                rightContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("—", color = Blue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("SpO₂", color = Muted, fontSize = 10.sp)
                    }
                },
                onClick = onNavigateToProgress
            )
        }

        item {
            Text("Your daily health", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp))
        }
        item {
            MetricCard(
                title = "Calories & nutrition",
                subtitle = "${nutrition.totalCaloriesConsumed} / ${nutrition.calorieTarget} kcal • ${meals.size} meals logged",
                icon = Icons.Default.LocalFireDepartment,
                accent = Orange,
                rightContent = { Text("Food diary  ›", color = Orange, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                onClick = onNavigateToFood
            )
        }
        item {
            MetricCard(
                title = "Steps & walking",
                subtitle = "${steps?.steps ?: 0} steps • ${String.format("%.2f", (steps?.distanceMeters ?: 0f) / 1000f)} km",
                icon = Icons.Default.DirectionsWalk,
                accent = Green,
                rightContent = { Text("Start walk  ›", color = Green, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                onClick = onNavigateToActivity
            )
        }
        item {
            MetricCard(
                title = "Water",
                subtitle = "${water} ml of ${profile.dailyWaterTargetMl} ml",
                icon = Icons.Default.Opacity,
                accent = Blue,
                rightContent = { Text("+ Add  ›", color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                onClick = onNavigateToFood
            )
        }
        item {
            MetricCard(
                title = "Weight & body journey",
                subtitle = "${weight?.weightKg ?: profile.weightKg} kg • BMI and body progress",
                icon = Icons.Default.MonitorWeight,
                accent = Color(0xFF9275D8),
                rightContent = { Text("View  ›", color = Color(0xFF9275D8), fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                onClick = onNavigateToBodyComposition
            )
        }
        item {
            Surface(color = CardBg, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Quick actions", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickButton("Scan food", Icons.Default.CameraAlt, onNavigateToScan, Modifier.weight(1f))
                        QuickButton("Log meal", Icons.Default.Restaurant, onNavigateToFood, Modifier.weight(1f))
                        QuickButton("AI coach", Icons.Default.Psychology, onNavigateToAiCoach, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Surface(shape = CircleShape, color = tint, modifier = Modifier.size(42.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
private fun MindfulnessTile(
    title: String, symbol: String, background: Color, symbolColor: Color,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Column(
        modifier.height(148.dp).clip(RoundedCornerShape(30.dp)).background(background)
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(symbol, color = symbolColor, fontSize = 42.sp, fontWeight = FontWeight.Bold)
        Text(title, color = Ink, fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MetricCard(
    title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color, rightContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        color = CardBg, shape = RoundedCornerShape(30.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIcon(icon, Color(0xFF88888D))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(subtitle, color = Muted, fontSize = 13.sp, lineHeight = 17.sp)
            }
            Spacer(Modifier.width(8.dp))
            rightContent()
        }
    }
}

@Composable
private fun QuickButton(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(PageBg).clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Color(0xFF1473EA), modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(5.dp))
        Text(label, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
