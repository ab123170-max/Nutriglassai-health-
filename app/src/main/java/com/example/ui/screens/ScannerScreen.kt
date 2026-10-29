package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.RecognizedFoodItem
import com.example.domain.FoodUnit
import com.example.ui.components.SamsungHealthCard
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.FatRed
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.ProteinBlue
import com.example.ui.viewmodel.HealthViewModel
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    viewModel: HealthViewModel,
    onNavigateBack: () -> Unit,
    onMealLogged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scans by viewModel.allScans.collectAsState()
    val multiScanResult by viewModel.multiScanResult.collectAsState()
    val isAnalyzing by viewModel.isScanAnalyzing.collectAsState()
    val scanProgressStage by viewModel.scanProgressStage.collectAsState()
    val scanError by viewModel.scanError.collectAsState()
    val visionMode by viewModel.visionMode.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Live Lens, 1: Scan History

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var selectedMealType by remember { mutableStateOf("Lunch") }

    // Dialog state for adding an extra food component manually
    var showAddExtraDialog by remember { mutableStateOf(false) }
    var extraFoodName by remember { mutableStateOf("") }
    var extraQuantity by remember { mutableStateOf("1") }
    var extraUnit by remember { mutableStateOf("serving") }

    // Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = com.example.domain.ImagePreparationModule.decodeAndOrientImage(context, it)
                if (bitmap != null) {
                    viewModel.startMultiFoodScan(bitmap, it.toString())
                }
            } catch (e: Exception) {
                // Handled gracefully in ViewModel
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Tab Header: Camera Lens vs Scan History
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = HealthNavy,
            contentColor = Color.White
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("AI Food Lens", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Scan History (${scans.size})", fontWeight = FontWeight.Bold) }
            )
        }

        if (activeTab == 1) {
            // SCAN HISTORY TAB
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (scans.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No scan records yet. Aim your camera at a meal or pick a photo from gallery to begin.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(scans) { scan ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = scan.foodName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (scan.nepaliName.isNotBlank()) {
                                            Text(
                                                text = scan.nepaliName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (scan.itemsSummary.isNotBlank()) {
                                            Text(
                                                text = "Components: ${scan.itemsSummary}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${scan.estimatedCalories} kcal",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CalorieGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "P: ${scan.protein.toInt()}g • C: ${scan.carbs.toInt()}g • F: ${scan.fat.toInt()}g • Na: ${scan.sodium.toInt()}mg",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = scan.uncertaintyNotes,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        } else {
            // CAMERA SCANNER VIEWPORT OR CONFIRMATION SCREEN
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture

                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        capture
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Reticle overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 30.dp, vertical = 110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(290.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(2.dp, HealthTeal.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                        )
                    }

                    // Top instructions & Vision Mode Selector
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.70f)
                        ) {
                            Text(
                                text = "Point at single food or full meal with multiple items",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Vision Mode Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            com.example.vision.core.VisionProcessingMode.values().forEach { mode ->
                                val isSelected = visionMode == mode
                                val label = when (mode) {
                                    com.example.vision.core.VisionProcessingMode.AUTO_HYBRID -> "Auto / Hybrid"
                                    com.example.vision.core.VisionProcessingMode.ON_DEVICE_ONLY -> "On-Device Offline"
                                    com.example.vision.core.VisionProcessingMode.CLOUD_ASSISTED -> "Cloud Gemini"
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) HealthTeal else Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .clickable { viewModel.setVisionMode(mode) }
                                        .testTag("mode_chip_${mode.name.lowercase()}")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Camera Controls
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 90.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 30.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pick from gallery
                            IconButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .testTag("scanner_gallery_btn")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                            }

                            // Capture photo button
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(HealthTeal)
                                    .clickable {
                                        val capture = imageCapture
                                        if (capture != null && !isAnalyzing) {
                                            val executor = Executors.newSingleThreadExecutor()
                                            capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                                    try {
                                                        val bitmap = com.example.domain.ImagePreparationModule.imageProxyToRotatedBitmap(imageProxy)
                                                        imageProxy.close()
                                                        viewModel.startMultiFoodScan(bitmap, null)
                                                    } catch (e: Exception) {
                                                        imageProxy.close()
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    // handled
                                                }
                                            })
                                        }
                                    }
                                    .testTag("scanner_capture_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, Color.White, CircleShape)
                                )
                            }

                            // Flip Camera
                            IconButton(
                                onClick = {
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                        CameraSelector.LENS_FACING_FRONT
                                    } else {
                                        CameraSelector.LENS_FACING_BACK
                                    }
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .testTag("scanner_switch_cam_btn")
                            ) {
                                Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                            }
                        }
                    }
                } else {
                    // Permission Request Prompt
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Camera Access Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "NutriGlass AI uses the camera to recognize food items and compute nutrition.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.testTag("request_camera_perm_btn")
                            ) {
                                Text("Allow Camera Access")
                            }
                            TextButton(onClick = { galleryLauncher.launch("image/*") }) {
                                Text("Or Choose Photo from Gallery")
                            }
                        }
                    }
                }

                // Multi-stage analyzing progress overlay
                if (isAnalyzing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.82f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(color = HealthTeal, strokeWidth = 4.dp)
                            Text(
                                text = scanProgressStage ?: "NutriGlass Vision AI Running...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Detecting multiple dishes, portions, ingredients & allergens",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.cancelScan() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("Cancel Scan")
                            }
                        }
                    }
                }

                // Error Diagnostic Card with Retry and Offline Profile options
                if (scanError != null && !isAnalyzing && multiScanResult == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.80f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "AI Food Analysis Diagnostic",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = scanError ?: "An error occurred during food scanning.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Button(
                                    onClick = { viewModel.retryLastScan() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = HealthTeal)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Scan", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.useOfflineProfileForCurrentScan() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Use Offline Nepali Food Profile")
                                }

                                TextButton(
                                    onClick = { viewModel.clearCurrentScan() }
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }

                // MULTI-FOOD CONFIRMATION AND EDITING SCREEN (Module 2 & 5)
                multiScanResult?.let { result ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = result.mealTitle,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "AI Confidence: ${(result.overallConfidence * 100).toInt()}% • Multi-Food Recognition",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CalorieGreen,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        IconButton(onClick = { viewModel.clearCurrentScan() }) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                                        }
                                    }
                                }

                                // Combined Meal Nutrition Banner
                                item {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Total Combined Meal",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${result.totalCalories} kcal",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = CalorieGreen
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceAround
                                            ) {
                                                MacroItem("Protein", "${result.totalProtein.toInt()}g", ProteinBlue)
                                                MacroItem("Carbs", "${result.totalCarbs.toInt()}g", CarbsAmber)
                                                MacroItem("Fat", "${result.totalFat.toInt()}g", FatRed)
                                                MacroItem("Fiber", "${result.totalFiber.toInt()}g", Color(0xFF8B5CF6))
                                                MacroItem("Sodium", "${result.totalSodium.toInt()}mg", Color.Gray)
                                            }
                                        }
                                    }
                                }

                                // Meal Type Selector (Breakfast, Lunch, Dinner, Snacks)
                                item {
                                    Text("Log to Meal Category:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { type ->
                                            FilterChip(
                                                selected = selectedMealType == type,
                                                onClick = { selectedMealType = type },
                                                label = { Text(type, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }

                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recognized Components (${result.items.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(onClick = { showAddExtraDialog = true }) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add Food", fontSize = 12.sp)
                                        }
                                    }
                                }

                                // List of editable food items
                                items(result.items) { item ->
                                    EditableFoodComponentCard(
                                        item = item,
                                        onUpdate = { updatedName, qty, unit, method ->
                                            viewModel.updateRecognizedItem(item.id, updatedName, qty, unit, method)
                                        },
                                        onRemove = {
                                            viewModel.removeFoodComponentFromScan(item.id)
                                        }
                                    )
                                }

                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Photo-based calorie calculations are estimates. Cooking oils, salt, and dense sauces may alter values.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Action Buttons (Module 2.D)
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.confirmAndSaveScannedMeal(selectedMealType)
                                                onMealLogged()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .testTag("confirm_and_save_scan_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = HealthTeal)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Confirm and Save Meal (${result.totalCalories} kcal)", fontWeight = FontWeight.Bold)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.clearCurrentScan() },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Scan Again")
                                            }

                                            OutlinedButton(
                                                onClick = { viewModel.clearCurrentScan() },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Cancel")
                                            }
                                        }
                                    }
                                }

                                item { Spacer(modifier = Modifier.height(40.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Extra Food Component Dialog
    if (showAddExtraDialog) {
        AlertDialog(
            onDismissRequest = { showAddExtraDialog = false },
            title = { Text("Add Food to Meal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = extraFoodName,
                        onValueChange = { extraFoodName = it },
                        label = { Text("Food Name (e.g. Papad, Pickle, Tea)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = extraQuantity,
                            onValueChange = { extraQuantity = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = extraUnit,
                            onValueChange = { extraUnit = it },
                            label = { Text("Unit (g, piece, bowl)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (extraFoodName.isNotBlank()) {
                        val qty = extraQuantity.toFloatOrNull() ?: 1f
                        val newFood = RecognizedFoodItem(
                            id = UUID.randomUUID().toString(),
                            foodName = extraFoodName,
                            quantity = qty,
                            unit = extraUnit,
                            estimatedGrams = qty * 100f,
                            calories = 120,
                            protein = 3f,
                            carbs = 18f,
                            fat = 3f,
                            confidence = 1.0f
                        )
                        viewModel.addFoodComponentToScan(newFood)
                        extraFoodName = ""
                        showAddExtraDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExtraDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditableFoodComponentCard(
    item: RecognizedFoodItem,
    onUpdate: (name: String, quantity: Float, unit: String, method: String) -> Unit,
    onRemove: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember(item) { mutableStateOf(item.foodName) }
    var editQuantity by remember(item) { mutableStateOf(item.quantity.toString()) }
    var editUnit by remember(item) { mutableStateOf(item.unit) }
    var editMethod by remember(item) { mutableStateOf(item.preparationMethod) }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.nepaliName.isNotBlank()) {
                        Text(
                            text = item.nepaliName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${item.quantity} ${item.unit} (~${item.estimatedGrams.toInt()}g) • ${item.preparationMethod}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.calories} kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CalorieGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = { isEditing = !isEditing }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Portion confirmation note if uncertain
            if (item.requiresPortionConfirmation || item.confidence < 0.8f) {
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = "⚠️ Please confirm serving size — visual depth uncertain",
                        color = Color(0xFF92400E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Quick nutritional stats
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "P: ${item.protein}g • C: ${item.carbs}g • F: ${item.fat}g • Fib: ${item.fiber}g • Na: ${item.sodium.toInt()}mg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )

            // Allergens tags
            if (item.allergens.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Allergens:", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    item.allergens.forEach { allergen ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = allergen,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Ingredients and visible dressings
            if (item.ingredients.isNotEmpty()) {
                Text(
                    text = "Ingredients: ${item.ingredients.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }

            // Healthier alternatives
            if (item.alternatives.isNotEmpty()) {
                Text(
                    text = "Healthier swaps: ${item.alternatives.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = HealthTeal,
                    fontSize = 10.sp
                )
            }

            // Expandable inline editor
            if (isEditing) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            editName = it
                            onUpdate(it, editQuantity.toFloatOrNull() ?: 1f, editUnit, editMethod)
                        },
                        label = { Text("Food Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editQuantity,
                            onValueChange = {
                                editQuantity = it
                                val q = it.toFloatOrNull() ?: 1f
                                onUpdate(editName, q, editUnit, editMethod)
                            },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f)
                        )

                        // Unit dropdown selector
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = editUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit") },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Restaurant,
                                        contentDescription = null,
                                        modifier = Modifier.clickable { unitMenuExpanded = true }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { unitMenuExpanded = true }
                            )

                            DropdownMenu(
                                expanded = unitMenuExpanded,
                                onDismissRequest = { unitMenuExpanded = false }
                            ) {
                                listOf(
                                    FoodUnit.G,
                                    FoodUnit.BOWL,
                                    FoodUnit.PLATE,
                                    FoodUnit.SERVING,
                                    FoodUnit.CUP,
                                    FoodUnit.KATORI,
                                    FoodUnit.ROTI,
                                    FoodUnit.MOMO_PIECE,
                                    FoodUnit.PIECE,
                                    FoodUnit.SLICE,
                                    FoodUnit.GLASS,
                                    FoodUnit.OZ,
                                    FoodUnit.TBSP
                                ).forEach { fUnit ->
                                    DropdownMenuItem(
                                        text = { Text(fUnit.displayName) },
                                        onClick = {
                                            editUnit = fUnit.symbol
                                            unitMenuExpanded = false
                                            onUpdate(editName, editQuantity.toFloatOrNull() ?: 1f, fUnit.symbol, editMethod)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editMethod,
                        onValueChange = {
                            editMethod = it
                            onUpdate(editName, editQuantity.toFloatOrNull() ?: 1f, editUnit, it)
                        },
                        label = { Text("Preparation (e.g. Steamed, Boiled, Fried)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    return com.example.domain.ImagePreparationModule.imageProxyToRotatedBitmap(image)
}
