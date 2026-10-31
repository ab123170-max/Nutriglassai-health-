package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.WeightUnit
import com.example.ui.theme.FatRed
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.ProteinBlue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class CompositionSlice {
    FAT_MASS,
    LEAN_MASS
}

@Composable
fun InteractiveBodyCompositionPieChart(
    bodyFatPercentage: Float,
    fatMassKg: Float,
    leanMassKg: Float,
    essentialFatKg: Float,
    storageFatKg: Float,
    weightUnit: WeightUnit = WeightUnit.KG,
    modifier: Modifier = Modifier
) {
    var selectedSlice by remember { mutableStateOf<CompositionSlice?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(bodyFatPercentage, fatMassKg) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }

    val safeBf = bodyFatPercentage.coerceIn(3f, 65f)
    val safeLean = (100f - safeBf).coerceIn(35f, 97f)

    // Unit conversion for mass display
    val unitFactor = if (weightUnit == WeightUnit.LB) 2.20462f else 1f
    val unitSymbol = if (weightUnit == WeightUnit.LB) "lb" else "kg"

    val displayFatMass = fatMassKg * unitFactor
    val displayLeanMass = leanMassKg * unitFactor
    val displayEssential = essentialFatKg * unitFactor
    val displayStorage = storageFatKg * unitFactor

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Body Composition Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Tap slice to inspect",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Donut Chart with Center Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .pointerInput(safeBf) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = tapOffset.x - center.x
                                val dy = tapOffset.y - center.y
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                // Start angle is 270 (top)
                                val normalizedAngle = (angle - 270f + 360f) % 360f
                                val fatSweep = (safeBf / 100f) * 360f

                                selectedSlice = if (normalizedAngle <= fatSweep) {
                                    if (selectedSlice == CompositionSlice.FAT_MASS) null else CompositionSlice.FAT_MASS
                                } else {
                                    if (selectedSlice == CompositionSlice.LEAN_MASS) null else CompositionSlice.LEAN_MASS
                                }
                            }
                        }
                        .testTag("body_composition_pie_canvas")
                ) {
                    val canvasSize = size.minDimension
                    val strokeWidth = 36.dp.toPx()
                    val arcSize = Size(canvasSize - strokeWidth, canvasSize - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                    val totalSweep = 360f * animationProgress.value
                    val fatSweep = (safeBf / 100f) * totalSweep
                    val leanSweep = (safeLean / 100f) * totalSweep

                    val startAngle = -90f // 12 o'clock

                    // Draw Fat Mass Arc (Warm Coral / Red)
                    val fatColor = if (selectedSlice == CompositionSlice.FAT_MASS) Color(0xFFFF5C5C) else Color(0xFFFF7E7E)
                    val fatStroke = if (selectedSlice == CompositionSlice.FAT_MASS) strokeWidth * 1.15f else strokeWidth
                    drawArc(
                        color = fatColor,
                        startAngle = startAngle,
                        sweepAngle = fatSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = fatStroke, cap = StrokeCap.Round)
                    )

                    // Draw Lean Mass Arc (Athletic Emerald / Teal)
                    val leanColor = if (selectedSlice == CompositionSlice.LEAN_MASS) HealthTeal else HealthTeal.copy(alpha = 0.85f)
                    val leanStroke = if (selectedSlice == CompositionSlice.LEAN_MASS) strokeWidth * 1.15f else strokeWidth
                    drawArc(
                        color = leanColor,
                        startAngle = startAngle + fatSweep + 2f,
                        sweepAngle = (leanSweep - 4f).coerceAtLeast(0f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = leanStroke, cap = StrokeCap.Round)
                    )
                }

                // Central Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (selectedSlice) {
                        CompositionSlice.FAT_MASS -> {
                            Text(
                                text = "FAT MASS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5C5C)
                            )
                            Text(
                                text = "${String.format("%.1f", safeBf)}%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format("%.1f", displayFatMass)} $unitSymbol",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        CompositionSlice.LEAN_MASS -> {
                            Text(
                                text = "LEAN MASS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HealthTeal
                            )
                            Text(
                                text = "${String.format("%.1f", safeLean)}%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format("%.1f", displayLeanMass)} $unitSymbol",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        null -> {
                            Text(
                                text = "BODY FAT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format("%.1f", safeBf)}%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Lean: ${String.format("%.1f", safeLean)}%",
                                fontSize = 12.sp,
                                color = HealthTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Slice Legend & Exploded Breakdown Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Fat Mass Legend Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            selectedSlice = if (selectedSlice == CompositionSlice.FAT_MASS) null else CompositionSlice.FAT_MASS
                        }
                        .testTag("legend_fat_mass"),
                    color = if (selectedSlice == CompositionSlice.FAT_MASS) Color(0xFFFF5C5C).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = if (selectedSlice == CompositionSlice.FAT_MASS) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5C5C)) else null
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF5C5C))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Body Fat",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%.1f", safeBf)}% • ${String.format("%.1f", displayFatMass)} $unitSymbol",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF5C5C)
                        )
                        Text(
                            text = "Essential: ${String.format("%.1f", displayEssential)} $unitSymbol\nStorage: ${String.format("%.1f", displayStorage)} $unitSymbol",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Lean Mass Legend Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            selectedSlice = if (selectedSlice == CompositionSlice.LEAN_MASS) null else CompositionSlice.LEAN_MASS
                        }
                        .testTag("legend_lean_mass"),
                    color = if (selectedSlice == CompositionSlice.LEAN_MASS) HealthTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = if (selectedSlice == CompositionSlice.LEAN_MASS) androidx.compose.foundation.BorderStroke(1.dp, HealthTeal) else null
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(HealthTeal)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lean Mass",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%.1f", safeLean)}% • ${String.format("%.1f", displayLeanMass)} $unitSymbol",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HealthTeal
                        )
                        Text(
                            text = "Skeletal Muscle, Bone & Vital Organs",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
