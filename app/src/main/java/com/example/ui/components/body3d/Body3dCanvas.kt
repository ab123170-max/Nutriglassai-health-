package com.example.ui.components.body3d

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FatRed
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveBody3dViewer(
    isMale: Boolean,
    currentBodyFat: Float,
    targetBodyFat: Float,
    muscleMassScale: Float = 1.0f,
    waistCm: Float? = null,
    hipCm: Float? = null,
    neckCm: Float? = null,
    targetMuscleMassScale: Float = 1.1f,
    targetWaistCm: Float? = null,
    modifier: Modifier = Modifier,
    selectedZone: BodyZone = BodyZone.ALL,
    onZoneSelected: (BodyZone) -> Unit = {},
    isSimulationMode: Boolean = false,
    simulationLabel: String = ""
) {
    var rotationY by remember { mutableFloatStateOf(0f) } // Yaw: 0 = Front, PI = Back
    var rotationX by remember { mutableFloatStateOf(0f) } // Pitch: up/down tilt
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Transformation Morph Progress [0.0 = Current, 1.0 = Target]
    var morphProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Generate base current and target meshes
    val currentMesh = remember(isMale, currentBodyFat, muscleMassScale, waistCm, hipCm, neckCm) {
        BodyMeshGenerator.createBodyMesh(
            isMale = isMale,
            bodyFatPercent = currentBodyFat,
            muscleMassScale = muscleMassScale,
            waistCm = waistCm,
            hipCm = hipCm,
            neckCm = neckCm,
            isTargetModel = false
        )
    }
    val targetMesh = remember(isMale, targetBodyFat, targetMuscleMassScale, targetWaistCm, hipCm, neckCm) {
        BodyMeshGenerator.createBodyMesh(
            isMale = isMale,
            bodyFatPercent = targetBodyFat,
            muscleMassScale = targetMuscleMassScale,
            waistCm = targetWaistCm,
            hipCm = hipCm,
            neckCm = neckCm,
            isTargetModel = true
        )
    }

    // Interpolated active mesh
    val activeMesh = remember(currentMesh, targetMesh, morphProgress) {
        if (morphProgress <= 0.01f) currentMesh
        else if (morphProgress >= 0.99f) targetMesh
        else BodyMeshGenerator.interpolateMesh(currentMesh, targetMesh, morphProgress)
    }

    val displayBodyFat = remember(currentBodyFat, targetBodyFat, morphProgress) {
        currentBodyFat + ((targetBodyFat - currentBodyFat) * morphProgress)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(16.dp)
    ) {
        if (isSimulationMode) {
            Surface(
                color = Color(0xFF6366F1).copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔮", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (simulationLabel.isNotEmpty()) simulationLabel else "AI Simulation Mode (Educational Projection)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF818CF8)
                    )
                }
            }
        }

        // Header with status & view presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (morphProgress > 0.5f) "Target Silhouette" else "Current Silhouette",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (morphProgress > 0.5f) HealthTeal.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${String.format("%.1f", displayBodyFat)}% BF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (morphProgress > 0.5f) HealthTeal else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Drag to rotate 360° • Pinch to zoom • Tap body zone",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reset camera button
            IconButton(
                onClick = {
                    rotationY = 0f
                    rotationX = 0f
                    zoomScale = 1f
                    panOffsetX = 0f
                    panOffsetY = 0f
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Camera",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset Camera Angle Pills: Front, Side, Rear, 3D Orbit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf(
                "Front" to 0f,
                "Side" to (Math.PI.toFloat() / 2f),
                "Rear" to Math.PI.toFloat(),
                "3D Orbit" to 0.45f
            )
            presets.forEach { (label, angle) ->
                val isAngleActive = kotlin.math.abs(rotationY - angle) < 0.25f
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            rotationY = angle
                            rotationX = if (label == "3D Orbit") 0.20f else 0f
                        },
                    color = if (isAngleActive) HealthTeal.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isAngleActive) androidx.compose.foundation.BorderStroke(1.dp, HealthTeal) else null
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isAngleActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAngleActive) HealthTeal else MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3D Canvas Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.6f, 2.5f)
                        panOffsetX += pan.x
                        panOffsetY += pan.y
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // 1-finger drag rotates horizontal yaw and vertical pitch
                        rotationY += dragAmount.x * 0.012f
                        rotationX = (rotationX - (dragAmount.y * 0.010f)).coerceIn(-0.8f, 0.8f)
                    }
                }
                .testTag("body_3d_canvas"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height
                val centerX = (canvasW / 2f) + panOffsetX
                val centerY = (canvasH / 2f) + panOffsetY
                val baseScale = canvasH * 0.48f * zoomScale

                // 1. Transform vertices by Pitch (X) and Yaw (Y)
                val transformedVertices = activeMesh.vertices.map { p ->
                    val pPitch = p.rotateX(rotationX)
                    val pYaw = pPitch.rotateY(rotationY)
                    pYaw
                }

                // 2. Project 3D points to 2D Screen Coordinates (Orthographic/Weak Perspective)
                val projectedPoints = transformedVertices.map { p ->
                    // Z depth perspective factor
                    val perspectiveFactor = 1.0f / (1.0f - (p.z * 0.25f)).coerceAtLeast(0.4f)
                    val sx = centerX + (p.x * baseScale * perspectiveFactor)
                    val sy = centerY - (p.y * baseScale * perspectiveFactor) // invert Y for screen space
                    Offset(sx, sy)
                }

                // 3. Depth-sort faces by average Z (Painter's algorithm: draw farthest first)
                val sortedFaces = activeMesh.faces.map { face ->
                    val avgZ = face.vertexIndices.map { transformedVertices[it].z }.average().toFloat()
                    val p1 = transformedVertices[face.vertexIndices[0]]
                    val p2 = transformedVertices[face.vertexIndices[1]]
                    val p3 = transformedVertices[face.vertexIndices[2]]
                    val normal = Point3D.normal(p1, p2, p3)
                    face to (avgZ to normal)
                }.sortedBy { it.second.first } // sort by avgZ ascending (back to front)

                // Directional Light Vector from top-front-right (0.5, 0.7, 1.0)
                val lightX = 0.4f
                val lightY = 0.6f
                val lightZ = 0.9f
                val lightLen = kotlin.math.sqrt(lightX * lightX + lightY * lightY + lightZ * lightZ)
                val normLx = lightX / lightLen
                val normLy = lightY / lightLen
                val normLz = lightZ / lightLen

                // Render faces
                sortedFaces.forEach { (face, zAndNormal) ->
                    val normal = zAndNormal.second

                    // Diffuse lighting intensity
                    val dot = (normal.x * normLx) + (normal.y * normLy) + (normal.z * normLz)
                    val intensity = (0.25f + (0.75f * dot.coerceAtLeast(0f))).coerceIn(0.2f, 1.0f)

                    val isZoneHighlighted = selectedZone != BodyZone.ALL && face.zone == selectedZone

                    val baseColor = when {
                        isZoneHighlighted -> {
                            // Glowing vibrant highlight for selected zone
                            when (selectedZone) {
                                BodyZone.ABDOMEN -> Color(0xFFFF5C5C) // Coral red for abdomen
                                BodyZone.CHEST -> Color(0xFF3B82F6) // Cyan-blue for chest
                                BodyZone.ARMS -> Color(0xFFF59E0B) // Amber for arms
                                BodyZone.LEGS -> Color(0xFF10B981) // Emerald for legs
                                else -> HealthTeal
                            }
                        }
                        morphProgress > 0.5f -> {
                            // Target model: lean vibrant athletic mint/teal
                            Color(0xFF00E5A3)
                        }
                        else -> {
                            // Current model: stylized slate/teal
                            Color(0xFF64748B)
                        }
                    }

                    // Shaded color
                    val faceColor = Color(
                        red = (baseColor.red * intensity).coerceIn(0f, 1f),
                        green = (baseColor.green * intensity).coerceIn(0f, 1f),
                        blue = (baseColor.blue * intensity).coerceIn(0f, 1f),
                        alpha = if (isZoneHighlighted) 0.95f else 0.88f
                    )

                    val path = Path().apply {
                        val firstPt = projectedPoints[face.vertexIndices[0]]
                        moveTo(firstPt.x, firstPt.y)
                        for (i in 1 until face.vertexIndices.size) {
                            val pt = projectedPoints[face.vertexIndices[i]]
                            lineTo(pt.x, pt.y)
                        }
                        close()
                    }

                    drawPath(path = path, color = faceColor)

                    // Edge wireframe contour
                    val strokeColor = if (isZoneHighlighted) Color.White.copy(alpha = 0.5f) else Color(0xFF1E293B).copy(alpha = 0.4f)
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (isZoneHighlighted) 1.5f else 0.8f)
                    )
                }
            }

            // Educational Disclaimer Pill (MANDATORY REQUIREMENT)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Educational Anatomical Model • Not a Medical Scan",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Body Zone Highlight Filter Chips
        Text(
            text = "Inspect Body Zone & Fat Distribution:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val zones = listOf(
                BodyZone.ALL to "Full",
                BodyZone.ABDOMEN to "Abdomen",
                BodyZone.CHEST to "Chest",
                BodyZone.ARMS to "Arms",
                BodyZone.LEGS to "Legs"
            )

            zones.forEach { (zone, label) ->
                val isSelected = selectedZone == zone
                FilterChip(
                    selected = isSelected,
                    onClick = { onZoneSelected(zone) },
                    label = { Text(text = label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HealthTeal,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("zone_chip_${zone.name.lowercase()}")
                )
            }
        }

        // Zone Educational Health Insight Card
        if (selectedZone != BodyZone.ALL) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = selectedZone.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedZone.healthInsight,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Animated Transformation Timeline Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Transformation Timeline",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (morphProgress < 0.1f) "Current" else if (morphProgress > 0.9f) "Target Goal" else "${(morphProgress * 100).toInt()}% Morph",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HealthTeal
                    )
                }

                // Play / Animate button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            animatedProgress.snapTo(0f)
                            morphProgress = 0f
                            animatedProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 2200)
                            ) {
                                morphProgress = value
                            }
                        }
                    },
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_play_transformation"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HealthTeal)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Animate", fontSize = 11.sp, color = Color.White)
                }
            }

            Slider(
                value = morphProgress,
                onValueChange = { morphProgress = it },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = HealthTeal,
                    activeTrackColor = HealthTeal,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag("transformation_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current (${String.format("%.1f", currentBodyFat)}%)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Target (${String.format("%.1f", targetBodyFat)}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HealthTeal
                )
            }
        }
    }
}
