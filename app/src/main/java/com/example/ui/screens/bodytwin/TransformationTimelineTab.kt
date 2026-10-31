package com.example.ui.screens.bodytwin

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BodyTwinSnapshot
import com.example.domain.CircumferenceUnit
import com.example.domain.UnitConversionEngine
import com.example.domain.WeightUnit
import com.example.ui.components.body3d.InteractiveBody3dViewer
import com.example.ui.theme.FatRed
import com.example.ui.theme.HealthTeal
import java.io.File
import java.io.FileOutputStream

@Composable
fun TransformationTimelineTab(
    snapshots: List<BodyTwinSnapshot>,
    currentWeightKg: Float,
    targetWeightKg: Float,
    currentBf: Float,
    targetBf: Float,
    isMale: Boolean,
    weightUnit: WeightUnit,
    circumferenceUnit: CircumferenceUnit,
    isPrivacyConsented: Boolean,
    onRequestPrivacyConsent: () -> Unit,
    onSavePhotoSnapshot: (String) -> Unit,
    onDeleteSnapshot: (Long) -> Unit
) {
    val context = LocalContext.current
    var compareMode by remember { mutableStateOf("START_VS_CURRENT") } // START_VS_CURRENT or CURRENT_VS_GOAL

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            // Securely copy photo to local private application storage
            val localPath = savePhotoPrivately(context, sourceUri)
            if (localPath != null) {
                onSavePhotoSnapshot(localPath)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tab_transformation_timeline"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Multi-Way Comparison Card
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
                        Column {
                            Text(
                                text = "Anatomical Morph Comparison",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Side-by-side silhouette evolution",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Toggle comparison target
                        Surface(
                            color = HealthTeal.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clickable {
                                    compareMode = if (compareMode == "START_VS_CURRENT") "CURRENT_VS_GOAL" else "START_VS_CURRENT"
                                }
                                .testTag("btn_toggle_compare_mode")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CompareArrows, contentDescription = null, tint = HealthTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (compareMode == "START_VS_CURRENT") "Starting vs Now" else "Now vs Goal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HealthTeal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val startBf = (currentBf + 4.0f).coerceAtMost(45f)
                    val effectiveTargetBf = if (compareMode == "START_VS_CURRENT") currentBf else targetBf

                    InteractiveBody3dViewer(
                        isMale = isMale,
                        currentBodyFat = if (compareMode == "START_VS_CURRENT") startBf else currentBf,
                        targetBodyFat = effectiveTargetBf,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Private Photo Vault Section
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = HealthTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Private On-Device Photo Vault",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Stored exclusively on your phone with zero cloud transmission.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (!isPrivacyConsented) {
                                onRequestPrivacyConsent()
                            } else {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HealthTeal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_add_vault_photo")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Photo", fontSize = 12.sp)
                    }
                }
            }
        }

        // Timeline Checkpoint Entries
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = HealthTeal, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Timeline Checkpoints (${snapshots.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (snapshots.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Checkpoints Yet",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Log weekly check-ins or tap [+] to save your first 3D Avatar snapshot.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(snapshots, key = { it.id }) { snapshot ->
                SnapshotTimelineCard(
                    snapshot = snapshot,
                    weightUnit = weightUnit,
                    circumferenceUnit = circumferenceUnit,
                    onDelete = { onDeleteSnapshot(snapshot.id) }
                )
            }
        }
    }
}

@Composable
fun SnapshotTimelineCard(
    snapshot: BodyTwinSnapshot,
    weightUnit: WeightUnit,
    circumferenceUnit: CircumferenceUnit,
    onDelete: () -> Unit
) {
    val displayWeight = UnitConversionEngine.formatWeight(snapshot.weightKg, weightUnit)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("snapshot_card_${snapshot.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = HealthTeal.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (snapshot.photoPath != null) Icons.Default.Photo else Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = HealthTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = snapshot.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = snapshot.dateString,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weight: $displayWeight",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Body Fat: ${String.format("%.1f", snapshot.bodyFatPercent)}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "SMM: ${String.format("%.1f", snapshot.muscleMassKg)} kg",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Snapshot",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Saves a picked image securely into app-private isolated internal files directory.
 */
private fun savePhotoPrivately(context: Context, uri: Uri): String? {
    return try {
        val scansDir = File(context.filesDir, "body_scans")
        if (!scansDir.exists()) scansDir.mkdirs()

        val destFile = File(scansDir, "scan_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        null
    }
}
