package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.domain.NutritionEngine
import com.example.domain.DecimalInputValidator
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.ProteinBlue
import com.example.ui.viewmodel.HealthViewModel

@Composable
fun OnboardingScreen(
    viewModel: HealthViewModel,
    onOnboardingFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) } // 0: Welcome, 1: Basic Info, 2: Goals & Diet, 3: Estimated Plan & Finish

    var name by remember { mutableStateOf("Aayush") }
    var age by remember { mutableStateOf("26") }
    var gender by remember { mutableStateOf("Male") }
    var heightCm by remember { mutableStateOf("175") }
    var weightKg by remember { mutableStateOf("72") }
    var targetWeightKg by remember { mutableStateOf("68") }
    var activityLevel by remember { mutableStateOf("Moderate") }
    var dietaryPreference by remember { mutableStateOf("Standard Nepali & Asian") }
    var fitnessGoal by remember { mutableStateOf("Lose Weight") }
    var stepGoal by remember { mutableStateOf("8000") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Step indicator dots
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (0..3).forEach { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (step == index) 24.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (step == index) HealthTeal else Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }

        AnimatedContent(targetState = step, label = "onboarding_step") { targetStep ->
            when (targetStep) {
                0 -> {
                    // Welcome & Mission
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(HealthTeal.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = HealthTeal,
                                modifier = Modifier.size(50.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "NutriGlass AI Health",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Precision nutrition, native AI food scanner, authentic Nepali food intelligence, and holistic fitness tracking inspired by Samsung Health.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BenefitBullet("Authentic Nepali Food Database (Dal Bhat, Dhido, Gundruk, Momo)")
                                BenefitBullet("CameraX Vision AI: Estimates calories, macros, and portion sizes")
                                BenefitBullet("100% Offline-First: SQLite local storage with zero forced logins")
                            }
                        }
                    }
                }

                1 -> {
                    // Basic Body Metrics
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "About You",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Used to estimate your Basal Metabolic Rate (BMR) and daily energy expenditure.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Your Name") },
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Age") },
                                modifier = Modifier.weight(1f).testTag("onboarding_age_input")
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gender", style = MaterialTheme.typography.labelSmall)
                                Row {
                                    listOf("Male", "Female").forEach { g ->
                                        FilterChip(
                                            selected = gender == g,
                                            onClick = { gender = g },
                                            label = { Text(g, fontSize = 11.sp) }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = heightCm,
                                onValueChange = { heightCm = DecimalInputValidator.sanitizeDecimalInput(it) },
                                label = { Text("Height (cm)") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = weightKg,
                                onValueChange = { weightKg = DecimalInputValidator.sanitizeDecimalInput(it) },
                                label = { Text("Current Wt (kg)") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("onboarding_weight_input")
                            )
                        }

                        OutlinedTextField(
                            value = targetWeightKg,
                            onValueChange = { targetWeightKg = DecimalInputValidator.sanitizeDecimalInput(it) },
                            label = { Text("Target Weight (kg)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                2 -> {
                    // Goals & Lifestyle
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Health & Nutrition Goals",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text("Select your primary objective:", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Lose Weight", "Maintain Weight", "Gain Muscle").forEach { g ->
                                FilterChip(
                                    selected = fitnessGoal == g,
                                    onClick = { fitnessGoal = g },
                                    label = { Text(g, fontSize = 11.sp) }
                                )
                            }
                        }

                        Text("Activity Level:", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Sedentary", "Light", "Moderate", "Active").forEach { act ->
                                FilterChip(
                                    selected = activityLevel == act,
                                    onClick = { activityLevel = act },
                                    label = { Text(act, fontSize = 11.sp) }
                                )
                            }
                        }

                        Text("Dietary Style:", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Standard Nepali", "Vegetarian", "Vegan").forEach { d ->
                                FilterChip(
                                    selected = dietaryPreference.contains(d),
                                    onClick = { dietaryPreference = d },
                                    label = { Text(d, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = stepGoal,
                            onValueChange = { stepGoal = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Daily Step Target") },
                            placeholder = { Text("e.g. 8000") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                3 -> {
                    // Calculated Plan Summary
                    val w = weightKg.toFloatOrNull() ?: 70f
                    val h = heightCm.toFloatOrNull() ?: 175f
                    val a = age.toIntOrNull() ?: 25
                    val bmr = NutritionEngine.calculateBmr(w, h, a, gender)
                    val tdee = NutritionEngine.calculateTdee(bmr, activityLevel)
                    val targetCals = NutritionEngine.calculateCalorieTarget(tdee, fitnessGoal, gender)
                    val bmi = NutritionEngine.calculateBmi(w, h)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Your Personalized Health Blueprint",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Estimated BMR (Resting Energy):", style = MaterialTheme.typography.bodyMedium)
                                    Text("${bmr.toInt()} kcal", fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("TDEE (Daily Burn):", style = MaterialTheme.typography.bodyMedium)
                                    Text("${tdee.toInt()} kcal", fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Daily Calorie Budget:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HealthTeal)
                                    Text("$targetCals kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = HealthTeal)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Initial BMI:", style = MaterialTheme.typography.bodyMedium)
                                    Text("${bmi.bmi} (${bmi.category})", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Text(
                            text = "Notice: Calorie targets and BMI metrics are scientific estimates based on the Mifflin-St Jeor equation. Adjust portion sizes according to your appetite and bodily sensations.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                TextButton(onClick = { step -= 1 }) {
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    if (step < 3) {
                        step += 1
                    } else {
                        // Save profile and finish
                        val w = weightKg.toFloatOrNull() ?: 70f
                        val h = heightCm.toFloatOrNull() ?: 175f
                        val a = age.toIntOrNull() ?: 25
                        val targetW = targetWeightKg.toFloatOrNull() ?: 68f
                        val bmr = NutritionEngine.calculateBmr(w, h, a, gender)
                        val tdee = NutritionEngine.calculateTdee(bmr, activityLevel)
                        val targetCals = NutritionEngine.calculateCalorieTarget(tdee, fitnessGoal, gender)

                        viewModel.saveProfile(
                            UserProfile(
                                name = name.ifBlank { "Aayush" },
                                age = a,
                                gender = gender,
                                heightCm = h,
                                weightKg = w,
                                targetWeightKg = targetW,
                                activityLevel = activityLevel,
                                dietaryPreference = dietaryPreference,
                                fitnessGoal = fitnessGoal,
                                dailyStepGoal = stepGoal.toIntOrNull() ?: 8000,
                                dailyCalorieTarget = targetCals,
                                dailyWaterTargetMl = 2500,
                                onboardingCompleted = true
                            )
                        )
                        onOnboardingFinished()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HealthTeal),
                modifier = Modifier.testTag("onboarding_next_finish_btn")
            ) {
                Text(if (step == 3) "Start My Health Journey" else "Continue")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(if (step == 3) Icons.Default.Check else Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun BenefitBullet(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, contentDescription = null, tint = CalorieGreen, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}
