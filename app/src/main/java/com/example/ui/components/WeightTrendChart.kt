package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeightRecord
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.WorkoutOrange

@Composable
fun WeightTrendChart(
    weightRecords: List<WeightRecord>,
    targetWeightKg: Float = 65f,
    unitLabel: String = "kg",
    modifier: Modifier = Modifier
) {
    if (weightRecords.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No historical weight records found. Log your weight to generate the trend chart.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val isLb = unitLabel.equals("lb", ignoreCase = true)
    val sortedRecords = remember(weightRecords) { weightRecords.sortedBy { it.dateString } }

    val displayTarget = if (isLb) targetWeightKg * 2.20462f else targetWeightKg
    val displayWeights = remember(sortedRecords, isLb) {
        sortedRecords.map { if (isLb) it.weightKg * 2.20462f else it.weightKg }
    }

    val minVal = (displayWeights.minOrNull() ?: 50f).coerceAtMost(displayTarget - 2f) - 1.5f
    val maxVal = (displayWeights.maxOrNull() ?: 100f).coerceAtLeast(displayTarget + 2f) + 1.5f

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historical Weight Trend Chart",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(HealthTeal, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scale Weight", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(12.dp).height(2.dp).background(CalorieGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Target", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tooltip card
        val activeIdx = selectedIndex ?: (sortedRecords.size - 1)
        val activeRecord = sortedRecords.getOrNull(activeIdx)
        val activeDisplayW = displayWeights.getOrNull(activeIdx) ?: 0f

        if (activeRecord != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = HealthNavy.copy(alpha = 0.06f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date: ${activeRecord.dateString}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${String.format("%.1f", activeDisplayW)} $unitLabel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = HealthTeal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Custom Canvas Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(sortedRecords) {
                    detectTapGestures { offset ->
                        val paddingLeft = 50f
                        val paddingRight = 20f
                        val chartW = size.width - paddingLeft - paddingRight
                        val stepX = if (sortedRecords.size > 1) chartW / (sortedRecords.size - 1) else chartW
                        val clickX = (offset.x - paddingLeft).coerceIn(0f, chartW)
                        val closest = (clickX / stepX).toInt().coerceIn(0, sortedRecords.size - 1)
                        selectedIndex = closest
                    }
                }
        ) {
            val paddingLeft = 50f
            val paddingRight = 20f
            val paddingTop = 20f
            val paddingBottom = 30f

            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom

            // 1. Grid Lines
            val gridCount = 4
            for (i in 0 until gridCount) {
                val ratio = i.toFloat() / (gridCount - 1)
                val y = paddingTop + (chartHeight * (1f - ratio))
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            }

            // 2. Target Line
            val targetY = paddingTop + (chartHeight * (1f - ((displayTarget - minVal) / (maxVal - minVal).coerceAtLeast(1f))))
            if (targetY in paddingTop..(paddingTop + chartHeight)) {
                drawLine(
                    color = CalorieGreen,
                    start = Offset(paddingLeft, targetY),
                    end = Offset(size.width - paddingRight, targetY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )
            }

            // 3. Points & Curve
            val stepX = if (sortedRecords.size > 1) chartWidth / (sortedRecords.size - 1) else chartWidth / 2f
            val points = displayWeights.mapIndexed { i, w ->
                val x = paddingLeft + (i * stepX)
                val y = paddingTop + (chartHeight * (1f - ((w - minVal) / (maxVal - minVal).coerceAtLeast(1f))))
                Offset(x, y)
            }

            if (points.isNotEmpty()) {
                val linePath = Path()
                val fillPath = Path()

                linePath.moveTo(points.first().x, points.first().y)
                fillPath.moveTo(points.first().x, paddingTop + chartHeight)
                fillPath.lineTo(points.first().x, points.first().y)

                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val controlX1 = p1.x + (p2.x - p1.x) / 2f
                    val controlY1 = p1.y
                    val controlX2 = p1.x + (p2.x - p1.x) / 2f
                    val controlY2 = p2.y

                    linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                }

                fillPath.lineTo(points.last().x, paddingTop + chartHeight)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(HealthTeal.copy(alpha = 0.3f), Color.Transparent),
                        startY = paddingTop,
                        endY = paddingTop + chartHeight
                    )
                )

                drawPath(
                    path = linePath,
                    color = HealthTeal,
                    style = Stroke(width = 5f)
                )
            }

            // 4. Draw Selected Column & Dots
            points.forEachIndexed { i, p ->
                val isSelected = (i == activeIdx)
                if (isSelected) {
                    drawLine(
                        color = HealthNavy.copy(alpha = 0.3f),
                        start = Offset(p.x, paddingTop),
                        end = Offset(p.x, paddingTop + chartHeight),
                        strokeWidth = 2f
                    )
                    drawCircle(color = HealthTeal.copy(alpha = 0.3f), radius = 14f, center = p)
                }
                drawCircle(color = if (isSelected) HealthNavy else HealthTeal, radius = if (isSelected) 7f else 5f, center = p)
                drawCircle(color = Color.White, radius = 3f, center = p)
            }
        }
    }
}
