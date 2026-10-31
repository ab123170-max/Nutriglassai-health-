package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.BmiCategory
import com.example.domain.WeightUnit
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.FatRed
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.ProteinBlue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SmartBmiGauge(
    bmi: Float,
    targetBmi: Float,
    minHealthyWeightKg: Float,
    maxHealthyWeightKg: Float,
    currentWeightKg: Float,
    targetWeightKg: Float,
    weightUnit: WeightUnit = WeightUnit.KG,
    modifier: Modifier = Modifier
) {
    val animatedBmi = remember { Animatable(0f) }

    LaunchedEffect(bmi) {
        animatedBmi.animateTo(
            targetValue = bmi,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val category = BmiCategory.fromBmi(bmi)

    val categoryColor = when (category) {
        BmiCategory.SEVERELY_UNDERWEIGHT, BmiCategory.UNDERWEIGHT -> ProteinBlue
        BmiCategory.NORMAL -> HealthTeal
        BmiCategory.OVERWEIGHT -> CarbsAmber
        BmiCategory.OBESE_CLASS_1, BmiCategory.OBESE_CLASS_2, BmiCategory.OBESE_CLASS_3 -> FatRed
    }

    val unitFactor = if (weightUnit == WeightUnit.LB) 2.20462f else 1f
    val unitSymbol = if (weightUnit == WeightUnit.LB) "lb" else "kg"

    val displayMinHealthy = minHealthyWeightKg * unitFactor
    val displayMaxHealthy = maxHealthyWeightKg * unitFactor
    val displayTargetWeight = targetWeightKg * unitFactor

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Smart BMI Indicator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = categoryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = category.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gauge Arc (180 degree semi-circle)
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 135.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(
                    modifier = Modifier
                        .size(width = 240.dp, height = 240.dp)
                        .testTag("bmi_gauge_canvas")
                ) {
                    val arcStroke = 22.dp.toPx()
                    val arcSize = Size(size.width - arcStroke, size.height - arcStroke)
                    val topLeft = Offset(arcStroke / 2f, arcStroke / 2f)

                    // 180 degrees sweep across BMI range [15.0 to 40.0] -> range = 25.0
                    val minBmiScale = 15f
                    val maxBmiScale = 40f
                    val bmiRange = maxBmiScale - minBmiScale

                    fun bmiToAngle(valBmi: Float): Float {
                        val clamped = valBmi.coerceIn(minBmiScale, maxBmiScale)
                        val fraction = (clamped - minBmiScale) / bmiRange
                        return 180f + (fraction * 180f) // start at 180 (left) and sweep clockwise to 360 (right)
                    }

                    // Draw Background Arc Bands
                    // 1. Underweight band (15 to 18.5)
                    val uwSweep = ((18.5f - 15f) / bmiRange) * 180f
                    drawArc(
                        color = ProteinBlue.copy(alpha = 0.4f),
                        startAngle = 180f,
                        sweepAngle = uwSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = arcStroke, cap = StrokeCap.Round)
                    )

                    // 2. Normal band (18.5 to 25.0)
                    val normStart = 180f + uwSweep
                    val normSweep = ((25f - 18.5f) / bmiRange) * 180f
                    drawArc(
                        color = HealthTeal,
                        startAngle = normStart,
                        sweepAngle = normSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = arcStroke)
                    )

                    // 3. Overweight band (25.0 to 30.0)
                    val owStart = normStart + normSweep
                    val owSweep = ((30f - 25f) / bmiRange) * 180f
                    drawArc(
                        color = CarbsAmber,
                        startAngle = owStart,
                        sweepAngle = owSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = arcStroke)
                    )

                    // 4. Obese band (30.0 to 40.0)
                    val obStart = owStart + owSweep
                    val obSweep = ((40f - 30f) / bmiRange) * 180f
                    drawArc(
                        color = FatRed.copy(alpha = 0.85f),
                        startAngle = obStart,
                        sweepAngle = obSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = arcStroke, cap = StrokeCap.Round)
                    )

                    // Draw Target BMI Marker if present
                    if (targetBmi in minBmiScale..maxBmiScale) {
                        val targetAngle = bmiToAngle(targetBmi)
                        val targetRad = Math.toRadians(targetAngle.toDouble())
                        val r = (arcSize.width / 2f)
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val tx = cx + (r * cos(targetRad)).toFloat()
                        val ty = cy + (r * sin(targetRad)).toFloat()

                        drawCircle(
                            color = Color.White,
                            radius = 7.dp.toPx(),
                            center = Offset(tx, ty)
                        )
                        drawCircle(
                            color = Color(0xFF0F172A),
                            radius = 4.dp.toPx(),
                            center = Offset(tx, ty)
                        )
                    }

                    // Draw Active Needle Pointer
                    val currentAngle = bmiToAngle(animatedBmi.value)
                    val needleRad = Math.toRadians(currentAngle.toDouble())
                    val needleLength = (arcSize.width / 2f) - 6.dp.toPx()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val nx = cx + (needleLength * cos(needleRad)).toFloat()
                    val ny = cy + (needleLength * sin(needleRad)).toFloat()

                    drawLine(
                        color = Color.White,
                        start = Offset(cx, cy),
                        end = Offset(nx, ny),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = categoryColor,
                        radius = 5.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }

                // Digital BMI Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = String.format("%.1f", animatedBmi.value),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "BMI kg/m²",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Healthy BMI & Weight Range Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Healthy BMI Range:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "18.5 – 24.9",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HealthTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Healthy Weight for Height:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format("%.1f", displayMinHealthy)} – ${String.format("%.1f", displayMaxHealthy)} $unitSymbol",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (targetBmi > 0f) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target Goal:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val deltaKg = currentWeightKg - targetWeightKg
                            val deltaDisplay = deltaKg * unitFactor
                            val progressText = if (kotlin.math.abs(deltaKg) < 0.2f) {
                                "Goal Reached!"
                            } else if (deltaKg > 0f) {
                                "${String.format("%.1f", deltaDisplay)} $unitSymbol to lose"
                            } else {
                                "${String.format("%.1f", -deltaDisplay)} $unitSymbol to gain"
                            }

                            Text(
                                text = "BMI ${String.format("%.1f", targetBmi)} ($progressText)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = HealthTeal
                            )
                        }
                    }
                }
            }
        }
    }
}
