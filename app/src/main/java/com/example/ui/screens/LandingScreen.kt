package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal

@Composable
fun LandingScreen(onGetStarted: () -> Unit) {
    val blue = Color(0xFF1473EA)
    val pale = Color(0xFFF3F8FC)
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(pale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(390.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF092B4C), Color(0xFF1473EA)))),
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=1200&auto=format&fit=crop",
                    contentDescription = "Fresh healthy meal with vegetables and balanced food",
                    modifier = Modifier.fillMaxWidth().height(245.dp).align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp).padding(top = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(color = Color.White.copy(alpha = 0.14f), shape = CircleShape) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Favorite, null, tint = Color(0xFF8EF0D0), modifier = Modifier.size(17.dp))
                            Text("  YOUR PERSONAL HEALTH COMPANION", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("NutriGlass AI", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Eat smarter. Move better.\nFeel stronger every day.",
                        color = Color.White, fontSize = 21.sp, lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Your nutrition, activity and wellness journey — in one place.",
                        color = Color.White.copy(alpha = 0.88f), fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 20.dp)) {
                Text("A healthier you, made simpler", color = HealthNavy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Helpful tools that work together to support your daily routine.",
                    color = Color(0xFF607286), fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 14.dp))
                FeatureCard(Icons.Default.CameraAlt, "AI Food Lens", "Scan a meal and review estimated food, portions and nutrition.", blue)
                FeatureCard(Icons.Default.Restaurant, "Personalized Nutrition", "Build practical meal goals around your food preferences.", Color(0xFF159B79))
                FeatureCard(Icons.Default.DirectionsWalk, "Walking & Activity", "Track steps, walking sessions and your daily movement.", Color(0xFFEB9237))
                FeatureCard(Icons.Default.MonitorWeight, "Weight Journey", "Follow your measurements and progress over time.", Color(0xFF8265D5))
                FeatureCard(Icons.Default.Opacity, "Water & Daily Habits", "Keep an eye on hydration and healthy routines.", Color(0xFF269BC6))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFFE5F7F1)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, null, tint = HealthTeal, modifier = Modifier.size(25.dp))
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Made for your real life", color = HealthNavy, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Start with your details and get a plan shaped around your goals.",
                                color = Color(0xFF607286), fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = blue)
                ) {
                    Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.padding(start = 9.dp))
                }
                Text("Your journey starts with a few simple questions.",
                    color = Color(0xFF718196), fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 28.dp),
                    textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun FeatureCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, accent: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(25.dp))
            }
            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                Text(title, color = HealthNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(description, color = Color(0xFF647589), fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}
