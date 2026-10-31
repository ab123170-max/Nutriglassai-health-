package com.example.ui.screens.bodytwin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.domain.BodyCompositionResult
import com.example.domain.BodyTwinEngine
import com.example.domain.BodyTwinPrediction
import com.example.domain.CircumferenceUnit
import com.example.domain.UnitConversionEngine
import com.example.domain.WeightUnit
import com.example.ui.components.body3d.BodyZone
import com.example.ui.components.body3d.InteractiveBody3dViewer
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.FatRed
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.ProteinBlue
import com.example.ui.viewmodel.AvatarViewPerspective

@Composable
fun BodyTwinTab(
    currentAnalysis: BodyCompositionResult,
    targetAnalysis: BodyCompositionResult,
    prediction3M: BodyTwinPrediction,
    prediction6M: BodyTwinPrediction,
    isMale: Boolean,
    weightKg: Float,
    targetWeightKg: Float,
    waistCm: Float?,
    neckCm: Float?,
    hipCm: Float?,
    skeletalMuscleMassKg: Float,
    activePerspective: AvatarViewPerspective,
    selectedZone: BodyZone,
    weightUnit: WeightUnit,
    circumferenceUnit: CircumferenceUnit,
    onPerspectiveChanged: (AvatarViewPerspective) -> Unit,
    onZoneSelected: (BodyZone) -> Unit,
    onToggleGenderPreview: (Boolean) -> Unit
) {
    // Determine active target body fat & parameters based on perspective
    val (targetBf, simulationLabel, isSimulation) = when (activePerspective) {
        AvatarViewPerspective.CURRENT -> Triple(targetAnalysis.bodyFatPercentage, "", false)
        AvatarViewPerspective.STARTING -> Triple(currentAnalysis.bodyFatPercentage + 3.5f, "Starting Baseline Model", false)
        AvatarViewPerspective.GOAL -> Triple(targetAnalysis.bodyFatPercentage, "Target Goal Silhouette", false)
        AvatarViewPerspective.PREDICTION_3M -> Triple(
            prediction3M.projectedBodyFatPercent,
            "3-Month Projected AI Twin (12-Week Deficit Model)",
            true
        )
        AvatarViewPerspective.PREDICTION_6M -> Triple(
            prediction6M.projectedBodyFatPercent,
            "6-Month Projected AI Twin (24-Week Deficit Model)",
            true
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tab_body_twin"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Perspective Switcher Pills (Horizontal Scroll)
        item {
            Column {
                Text(
                    text = "AI Avatar Perspectives",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvatarViewPerspective.entries.forEach { perspective ->
                        val isSelected = perspective == activePerspective
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPerspectiveChanged(perspective) }
                                .testTag("perspective_pill_${perspective.name}"),
                            color = if (isSelected) HealthTeal else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (perspective.name.contains("PREDICTION")) {
                                    Text("🔮", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = perspective.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3D Avatar Interactive Viewer (Module 1, 2 & 5)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Gender silhouette preview toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Personalized 3D Avatar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Anatomical vertex morphing • Offline rendering",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Gender Switcher
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                Surface(
                                    color = if (isMale) HealthTeal else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { onToggleGenderPreview(true) }
                                ) {
                                    Text(
                                        text = "Male",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMale) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = if (!isMale) HealthTeal else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { onToggleGenderPreview(false) }
                                ) {
                                    Text(
                                        text = "Female",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isMale) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    InteractiveBody3dViewer(
                        isMale = isMale,
                        currentBodyFat = currentAnalysis.bodyFatPercentage,
                        targetBodyFat = targetBf,
                        muscleMassScale = 1.0f + ((skeletalMuscleMassKg - 28f) / 50f).coerceIn(-0.2f, 0.4f),
                        waistCm = waistCm,
                        hipCm = hipCm,
                        neckCm = neckCm,
                        selectedZone = selectedZone,
                        onZoneSelected = onZoneSelected,
                        isSimulationMode = isSimulation,
                        simulationLabel = simulationLabel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // AI Future Prediction Insights (When Prediction selected or always accessible)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔮", fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI Future Body Projections",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Thermodynamic calorie deficit simulations",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PredictionComparisonCard(
                            prediction = prediction3M,
                            weightUnit = weightUnit,
                            modifier = Modifier.weight(1f)
                        )
                        PredictionComparisonCard(
                            prediction = prediction6M,
                            weightUnit = weightUnit,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Predictions model an active 500 kcal daily deficit with resistance training. Projections are educational simulations and individual metabolism may adapt.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Anatomical Body Zone Insights
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Anatomical Zone Inspector",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = HealthTeal.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = selectedZone.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HealthTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = selectedZone.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = selectedZone.healthInsight,
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
fun PredictionComparisonCard(
    prediction: BodyTwinPrediction,
    weightUnit: WeightUnit,
    modifier: Modifier = Modifier
) {
    val displayProjectedWeight = UnitConversionEngine.formatWeight(prediction.projectedWeightKg, weightUnit)
    val displayLoss = UnitConversionEngine.formatWeight(prediction.totalWeightLossKg, weightUnit)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${prediction.timeframeMonths} Months",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF6366F1)
                )
                Surface(
                    color = Color(0xFF6366F1).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "SIMULATION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = displayProjectedWeight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "-$displayLoss Projected",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF10B981),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${String.format("%.1f", prediction.projectedBodyFatPercent)}% Body Fat",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            prediction.projectedWaistCm?.let {
                Text(
                    text = "Waist: ${String.format("%.1f", it)} cm",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
