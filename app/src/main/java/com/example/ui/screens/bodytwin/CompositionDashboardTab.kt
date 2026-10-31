package com.example.ui.screens.bodytwin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.domain.BodyCompositionEngine
import com.example.domain.BodyCompositionResult
import com.example.domain.CircumferenceUnit
import com.example.domain.HeightUnit
import com.example.domain.MuscleMassCategory
import com.example.domain.UnitConversionEngine
import com.example.domain.VisceralFatCategory
import com.example.domain.WaistToHeightCategory
import com.example.domain.WeightUnit
import com.example.ui.components.InteractiveBodyCompositionPieChart
import com.example.ui.components.SmartBmiGauge
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.FatRed
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.ProteinBlue

@Composable
fun CompositionDashboardTab(
    currentAnalysis: BodyCompositionResult,
    targetAnalysis: BodyCompositionResult,
    weightKg: Float,
    targetWeightKg: Float,
    startingWeightKg: Float,
    heightCm: Float,
    waistCm: Float?,
    skeletalMuscleMassKg: Float,
    muscleMassPercent: Float,
    muscleMassCategory: MuscleMassCategory,
    visceralFatIndex: Float,
    visceralFatCategory: VisceralFatCategory,
    waistToHeightRatio: Float?,
    waistToHeightCategory: WaistToHeightCategory?,
    weightUnit: WeightUnit,
    heightUnit: HeightUnit,
    circumferenceUnit: CircumferenceUnit,
    onOpenUnitSelector: () -> Unit
) {
    val weightLostKg = kotlin.math.max(0f, startingWeightKg - weightKg)
    val remainingWeightKg = kotlin.math.max(0f, weightKg - targetWeightKg)

    val displayCurrentWeight = UnitConversionEngine.formatWeight(weightKg, weightUnit)
    val displayTargetWeight = UnitConversionEngine.formatWeight(targetWeightKg, weightUnit)
    val displayWeightLost = UnitConversionEngine.formatWeight(weightLostKg, weightUnit)
    val displayRemainingWeight = UnitConversionEngine.formatWeight(remainingWeightKg, weightUnit)

    val displayFatMass = UnitConversionEngine.formatWeight(currentAnalysis.fatMassKg, weightUnit)
    val displayLeanMass = UnitConversionEngine.formatWeight(currentAnalysis.leanMassKg, weightUnit)
    val displaySmm = UnitConversionEngine.formatWeight(skeletalMuscleMassKg, weightUnit)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tab_composition_dashboard"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // MODULE 1: SMART BMI INDICATOR CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Smart BMI Indicator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "WHO Clinical Classification Gauge",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = HealthTeal.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable(onClick = onOpenUnitSelector)
                        ) {
                            Text(
                                text = "${weightUnit.symbol.uppercase()} • ${heightUnit.symbol.uppercase()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HealthTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SmartBmiGauge(
                        bmi = currentAnalysis.bmi,
                        targetBmi = currentAnalysis.targetBmi,
                        minHealthyWeightKg = currentAnalysis.minHealthyWeightKg,
                        maxHealthyWeightKg = currentAnalysis.maxHealthyWeightKg,
                        currentWeightKg = weightKg,
                        targetWeightKg = targetWeightKg,
                        weightUnit = weightUnit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // WEIGHT JOURNEY & TARGET PROGRESS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Weight Transformation Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        JourneyMetricBox(title = "Current", value = displayCurrentWeight, color = MaterialTheme.colorScheme.onSurface)
                        JourneyMetricBox(title = "Target", value = displayTargetWeight, color = HealthTeal)
                        JourneyMetricBox(title = "Lost", value = displayWeightLost, color = Color(0xFF10B981))
                        JourneyMetricBox(title = "Remaining", value = displayRemainingWeight, color = CarbsAmber)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val totalDelta = (startingWeightKg - targetWeightKg).coerceAtLeast(0.1f)
                    val progressFraction = (weightLostKg / totalDelta).coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = HealthTeal,
                        trackColor = HealthTeal.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${(progressFraction * 100).toInt()}% of Target Weight Goal Achieved",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // MODULE 2: BODY FAT & MUSCLE MASS BIOMETRICS (SMM + VISCERAL FAT + WHTR)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Biometric Tissue Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Janssen Anthropometric SMM & Clinical Visceral Index",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2x2 Grid of Biometrics
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BiometricTile(
                            title = "Skeletal Muscle",
                            value = displaySmm,
                            subtitle = "${String.format("%.1f", muscleMassPercent)}% of Weight",
                            badgeText = muscleMassCategory.title,
                            badgeColor = ProteinBlue,
                            modifier = Modifier.weight(1f)
                        )
                        BiometricTile(
                            title = "Body Fat Ratio",
                            value = "${String.format("%.1f", currentAnalysis.bodyFatPercentage)}%",
                            subtitle = displayFatMass,
                            badgeText = currentAnalysis.bodyFatCategory.title,
                            badgeColor = FatRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val vfiColor = when (visceralFatCategory) {
                            VisceralFatCategory.BALANCED -> HealthTeal
                            VisceralFatCategory.ELEVATED -> CarbsAmber
                            VisceralFatCategory.HIGH -> FatRed
                        }
                        BiometricTile(
                            title = "Visceral Fat Index",
                            value = "Level ${String.format("%.1f", visceralFatIndex)}",
                            subtitle = visceralFatCategory.riskLevel,
                            badgeText = visceralFatCategory.title,
                            badgeColor = vfiColor,
                            modifier = Modifier.weight(1f)
                        )

                        val whtrDisplay = waistToHeightRatio?.let { String.format("%.2f", it) } ?: "N/A"
                        val whtrCategoryTitle = waistToHeightCategory?.title ?: "Enter Waist"
                        val whtrColor = if ((waistToHeightRatio ?: 0f) <= 0.50f) HealthTeal else CarbsAmber

                        BiometricTile(
                            title = "Waist-to-Height",
                            value = whtrDisplay,
                            subtitle = if ((waistToHeightRatio ?: 0f) <= 0.50f) "Optimal (< 0.50)" else "Elevated (> 0.50)",
                            badgeText = whtrCategoryTitle,
                            badgeColor = whtrColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // MODULE 3: INTERACTIVE DONUT PIE CHART
        item {
            InteractiveBodyCompositionPieChart(
                bodyFatPercentage = currentAnalysis.bodyFatPercentage,
                fatMassKg = currentAnalysis.fatMassKg,
                leanMassKg = currentAnalysis.leanMassKg,
                essentialFatKg = currentAnalysis.essentialFatKg,
                storageFatKg = currentAnalysis.storageFatKg,
                weightUnit = weightUnit,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // MEDICAL DISCLAIMER CARD
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Medical Disclaimer",
                        tint = HealthTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = BodyCompositionEngine.MEDICAL_DISCLAIMER,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun JourneyMetricBox(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun BiometricTile(
    title: String,
    value: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
