package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.WorkoutOrange
import kotlin.math.abs

data class WeightChartPoint(
    val dateLabel: String,
    val rawDateString: String,
    val weightValue: Float,
    val targetValue: Float,
    val weeklyChange: Float,
    val status: String
)

@Composable
fun RechartsWeightLineChart(
    points: List<WeightChartPoint>,
    unitLabel: String = "kg",
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Log 2 or more weight entries to view your Recharts trend curve.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val sortedPoints = remember(points) { points.sortedBy { it.rawDateString } }

    val targetValue = sortedPoints.lastOrNull()?.targetValue ?: 65f
    val minWeight = (sortedPoints.minOfOrNull { it.weightValue } ?: 50f).coerceAtMost(targetValue - 2f) - 1.5f
    val maxWeight = (sortedPoints.maxOfOrNull { it.weightValue } ?: 100f).coerceAtLeast(targetValue + 2f) + 1.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Recharts Header / Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recharts Weight Curve",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(HealthTeal, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Weight", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .background(CalorieGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Target", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Tooltip Display
        val activePoint = selectedPointIndex?.let { sortedPoints.getOrNull(it) } ?: sortedPoints.lastOrNull()
        if (activePoint != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
                    Column {
                        Text(
                            text = "Date: ${activePoint.dateLabel}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format("%.1f", activePoint.weightValue)} $unitLabel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = HealthNavy
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val changeSign = if (activePoint.weeklyChange <= 0) "" else "+"
                        Text(
                            text = "Weekly: $changeSign${String.format("%.1f", activePoint.weeklyChange)} $unitLabel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (activePoint.weeklyChange <= 0) CalorieGreen else WorkoutOrange
                        )
                        Text(
                            text = activePoint.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HealthTeal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Canvas Recharts Area
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(sortedPoints) {
                    detectTapGestures { offset ->
                        val chartWidth = size.width - 90f // 90f padding for y-axis
                        val stepX = if (sortedPoints.size > 1) chartWidth / (sortedPoints.size - 1) else chartWidth
                        val clickedX = (offset.x - 60f).coerceIn(0f, chartWidth)
                        val closestIdx = (clickedX / stepX).toInt().coerceIn(0, sortedPoints.size - 1)
                        selectedPointIndex = closestIdx
                    }
                }
        ) {
            val paddingLeft = 60f
            val paddingRight = 30f
            val paddingTop = 20f
            val paddingBottom = 40f

            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom

            // 1. Y-Axis Grid Lines & Ticks (4 levels)
            val gridLevels = 4
            for (i in 0 until gridLevels) {
                val ratio = i.toFloat() / (gridLevels - 1)
                val y = paddingTop + (chartHeight * (1f - ratio))
                val wVal = minWeight + (ratio * (maxWeight - minWeight))

                // Dashed Grid Line
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            }

            // 2. Target Goal Reference Line
            val targetY = paddingTop + (chartHeight * (1f - ((targetValue - minWeight) / (maxWeight - minWeight).coerceAtLeast(1f))))
            if (targetY in paddingTop..(paddingTop + chartHeight)) {
                drawLine(
                    color = CalorieGreen,
                    start = Offset(paddingLeft, targetY),
                    end = Offset(size.width - paddingRight, targetY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )
            }

            // 3. Compute Chart Data Points
            val stepX = if (sortedPoints.size > 1) chartWidth / (sortedPoints.size - 1) else chartWidth / 2f
            val canvasPoints = sortedPoints.mapIndexed { index, point ->
                val x = paddingLeft + (index * stepX)
                val y = paddingTop + (chartHeight * (1f - ((point.weightValue - minWeight) / (maxWeight - minWeight).coerceAtLeast(1f))))
                Offset(x, y)
            }

            // 4. Smooth Bezier Curve Path & Area Gradient Fill
            if (canvasPoints.isNotEmpty()) {
                val linePath = Path()
                val fillPath = Path()

                linePath.moveTo(canvasPoints.first().x, canvasPoints.first().y)
                fillPath.moveTo(canvasPoints.first().x, paddingTop + chartHeight)
                fillPath.lineTo(canvasPoints.first().x, canvasPoints.first().y)

                for (i in 0 until canvasPoints.size - 1) {
                    val p1 = canvasPoints[i]
                    val p2 = canvasPoints[i + 1]
                    val controlX1 = p1.x + (p2.x - p1.x) / 2f
                    val controlY1 = p1.y
                    val controlX2 = p1.x + (p2.x - p1.x) / 2f
                    val controlY2 = p2.y

                    linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                }

                fillPath.lineTo(canvasPoints.last().x, paddingTop + chartHeight)
                fillPath.close()

                // Draw Area Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            HealthTeal.copy(alpha = 0.35f),
                            HealthTeal.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        startY = paddingTop,
                        endY = paddingTop + chartHeight
                    )
                )

                // Draw Smooth Line
                drawPath(
                    path = linePath,
                    color = HealthTeal,
                    style = Stroke(width = 6f)
                )
            }

            // 5. Draw Point Glowing Dots & Highlight Column
            val selectedIdx = selectedPointIndex ?: (canvasPoints.size - 1)
            canvasPoints.forEachIndexed { idx, p ->
                val isSelected = (idx == selectedIdx)

                if (isSelected) {
                    // Vertical guide line
                    drawLine(
                        color = HealthNavy.copy(alpha = 0.4f),
                        start = Offset(p.x, paddingTop),
                        end = Offset(p.x, paddingTop + chartHeight),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )

                    // Outer Glow Circle
                    drawCircle(
                        color = HealthTeal.copy(alpha = 0.3f),
                        radius = 16f,
                        center = p
                    )
                }

                // Main Point Dot
                drawCircle(
                    color = if (isSelected) HealthNavy else HealthTeal,
                    radius = if (isSelected) 8f else 6f,
                    center = p
                )
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 4f else 3f,
                    center = p
                )
            }
        }
    }
}
