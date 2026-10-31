package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.BodyCompositionEngine
import com.example.domain.CircumferenceUnit
import com.example.domain.DecimalInputValidator
import com.example.domain.HeightUnit
import com.example.domain.UnitConversionEngine
import com.example.domain.WeightUnit
import com.example.ui.components.HeightMeasurementInput
import com.example.ui.components.MeasurementDecimalInput
import com.example.ui.components.UnitOption
import com.example.ui.components.UnitSelectorBottomSheet
import com.example.ui.screens.bodytwin.BodyTwinPrivacyConsentDialog
import com.example.ui.screens.bodytwin.BodyTwinTab
import com.example.ui.screens.bodytwin.CelebrationDialog
import com.example.ui.screens.bodytwin.CompositionDashboardTab
import com.example.ui.screens.bodytwin.MilestonesGamificationTab
import com.example.ui.screens.bodytwin.TransformationTimelineTab
import com.example.ui.theme.FatRed
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.viewmodel.BodyCompositionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyCompositionScreen(
    onNavigateBack: () -> Unit,
    viewModel: BodyCompositionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentAnalysis by viewModel.currentAnalysis.collectAsState()
    val targetAnalysis by viewModel.targetAnalysis.collectAsState()
    val skeletalMuscleMassKg by viewModel.skeletalMuscleMassKg.collectAsState()
    val muscleMassPercent by viewModel.muscleMassPercent.collectAsState()
    val muscleMassCategory by viewModel.muscleMassCategory.collectAsState()
    val visceralFatIndex by viewModel.visceralFatIndex.collectAsState()
    val visceralFatCategory by viewModel.visceralFatCategory.collectAsState()
    val waistToHeightRatio by viewModel.waistToHeightRatio.collectAsState()
    val waistToHeightCategory by viewModel.waistToHeightCategory.collectAsState()
    val prediction3M by viewModel.prediction3M.collectAsState()
    val prediction6M by viewModel.prediction6M.collectAsState()
    val milestones by viewModel.milestones.collectAsState()
    val allSnapshots by viewModel.allSnapshots.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("AI Body Twin", "Dashboard", "Timeline", "Milestones")

    var showUnitSelectorSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Body Twin & 3D",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Native Android 16 Health Intelligence",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_body_comp")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Privacy & Data Vault action button
                    IconButton(
                        onClick = { viewModel.showPrivacyConsentDialog(true) },
                        modifier = Modifier.testTag("btn_open_privacy_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy Shield",
                            tint = if (uiState.isPrivacyConsentAccepted) HealthTeal else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick unit toggle pill (KG / LB)
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val nextUnit = if (uiState.weightUnit == WeightUnit.KG) WeightUnit.LB else WeightUnit.KG
                                viewModel.setWeightUnit(nextUnit)
                            }
                            .testTag("btn_quick_unit_toggle"),
                        color = HealthTeal.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (uiState.weightUnit == WeightUnit.KG) "KG • CM" else "LB • IN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HealthTeal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showInputDialog(true) },
                containerColor = HealthTeal,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_measurements")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Record Measurements"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // M3 Primary Tab Row
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = HealthTeal
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_body_comp_$index")
                    )
                }
            }

            when (selectedTab) {
                0 -> BodyTwinTab(
                    currentAnalysis = currentAnalysis,
                    targetAnalysis = targetAnalysis,
                    prediction3M = prediction3M,
                    prediction6M = prediction6M,
                    isMale = uiState.isMalePreview,
                    weightKg = uiState.weightKg,
                    targetWeightKg = uiState.targetWeightKg,
                    waistCm = uiState.waistCm,
                    neckCm = uiState.neckCm,
                    hipCm = uiState.hipCm,
                    skeletalMuscleMassKg = skeletalMuscleMassKg,
                    activePerspective = uiState.activePerspective,
                    selectedZone = uiState.selectedZone,
                    weightUnit = uiState.weightUnit,
                    circumferenceUnit = uiState.circumferenceUnit,
                    onPerspectiveChanged = { viewModel.setAvatarPerspective(it) },
                    onZoneSelected = { viewModel.selectZone(it) },
                    onToggleGenderPreview = { viewModel.toggleGenderPreview(it) }
                )
                1 -> CompositionDashboardTab(
                    currentAnalysis = currentAnalysis,
                    targetAnalysis = targetAnalysis,
                    weightKg = uiState.weightKg,
                    targetWeightKg = uiState.targetWeightKg,
                    startingWeightKg = uiState.startingWeightKg,
                    heightCm = uiState.heightCm,
                    waistCm = uiState.waistCm,
                    skeletalMuscleMassKg = skeletalMuscleMassKg,
                    muscleMassPercent = muscleMassPercent,
                    muscleMassCategory = muscleMassCategory,
                    visceralFatIndex = visceralFatIndex,
                    visceralFatCategory = visceralFatCategory,
                    waistToHeightRatio = waistToHeightRatio,
                    waistToHeightCategory = waistToHeightCategory,
                    weightUnit = uiState.weightUnit,
                    heightUnit = uiState.heightUnit,
                    circumferenceUnit = uiState.circumferenceUnit,
                    onOpenUnitSelector = { showUnitSelectorSheet = true }
                )
                2 -> TransformationTimelineTab(
                    snapshots = allSnapshots,
                    currentWeightKg = uiState.weightKg,
                    targetWeightKg = uiState.targetWeightKg,
                    currentBf = currentAnalysis.bodyFatPercentage,
                    targetBf = targetAnalysis.bodyFatPercentage,
                    isMale = uiState.isMalePreview,
                    weightUnit = uiState.weightUnit,
                    circumferenceUnit = uiState.circumferenceUnit,
                    isPrivacyConsented = uiState.isPrivacyConsentAccepted,
                    onRequestPrivacyConsent = { viewModel.showPrivacyConsentDialog(true) },
                    onSavePhotoSnapshot = { photoPath -> viewModel.savePhotoSnapshot(photoPath) },
                    onDeleteSnapshot = { snapshotId -> viewModel.deleteSnapshot(snapshotId) }
                )
                3 -> MilestonesGamificationTab(
                    milestones = milestones,
                    activeCelebration = uiState.celebrationMilestone,
                    onDismissCelebration = { viewModel.dismissCelebration() },
                    onSelectMilestone = { milestone -> viewModel.triggerCelebration(milestone) }
                )
            }
        }
    }

    // Modal Input Dialog for Universal Measurements
    if (uiState.isInputDialogVisible) {
        MeasurementInputDialog(
            initialWeightKg = uiState.weightKg,
            initialHeightCm = uiState.heightCm,
            initialTargetWeightKg = uiState.targetWeightKg,
            initialWaistCm = uiState.waistCm,
            initialNeckCm = uiState.neckCm,
            initialHipCm = uiState.hipCm,
            initialChestCm = uiState.chestCm,
            initialArmCm = uiState.armCm,
            initialThighCm = uiState.thighCm,
            initialCalfCm = uiState.calfCm,
            currentWeightUnit = uiState.weightUnit,
            currentHeightUnit = uiState.heightUnit,
            currentCircumferenceUnit = uiState.circumferenceUnit,
            isMale = uiState.gender.equals("Male", ignoreCase = true),
            onSave = { w, h, tw, waist, neck, hip, chest, arm, thigh, calf ->
                viewModel.saveMeasurements(w, h, tw, waist, neck, hip, chest, arm, thigh, calf)
            },
            onDismiss = { viewModel.showInputDialog(false) }
        )
    }

    // Privacy & Data Protection Dialog
    if (uiState.isPrivacyConsentDialogVisible) {
        BodyTwinPrivacyConsentDialog(
            isConsented = uiState.isPrivacyConsentAccepted,
            onDismiss = { viewModel.showPrivacyConsentDialog(false) },
            onAcceptConsent = { viewModel.acceptPrivacyConsent() },
            onPurgeAllData = { viewModel.purgeAllBodyTwinData() }
        )
    }

    // Milestone Celebration Dialog
    uiState.celebrationMilestone?.let { milestone ->
        CelebrationDialog(
            milestone = milestone,
            onDismiss = { viewModel.dismissCelebration() }
        )
    }

    // Universal Unit Selector Bottom Sheet
    if (showUnitSelectorSheet) {
        val unitOptions = WeightUnit.entries.map {
            UnitOption(unit = it, symbol = it.symbol, displayName = it.displayName, nepaliName = it.displayName)
        }
        UnitSelectorBottomSheet(
            title = "Select Primary Weight Unit",
            currentUnit = uiState.weightUnit,
            options = unitOptions,
            onUnitSelected = { selectedUnit ->
                viewModel.setWeightUnit(selectedUnit)
            },
            onDismissRequest = { showUnitSelectorSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementInputDialog(
    initialWeightKg: Float,
    initialHeightCm: Float,
    initialTargetWeightKg: Float,
    initialWaistCm: Float?,
    initialNeckCm: Float?,
    initialHipCm: Float?,
    initialChestCm: Float?,
    initialArmCm: Float?,
    initialThighCm: Float?,
    initialCalfCm: Float?,
    currentWeightUnit: WeightUnit,
    currentHeightUnit: HeightUnit,
    currentCircumferenceUnit: CircumferenceUnit,
    isMale: Boolean,
    onSave: (
        weightKg: Float,
        heightCm: Float,
        targetWeightKg: Float,
        waistCm: Float?,
        neckCm: Float?,
        hipCm: Float?,
        chestCm: Float?,
        armCm: Float?,
        thighCm: Float?,
        calfCm: Float?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var activeWeightUnit by remember { mutableStateOf(currentWeightUnit) }
    var activeHeightUnit by remember { mutableStateOf(currentHeightUnit) }
    var activeCircumferenceUnit by remember { mutableStateOf(currentCircumferenceUnit) }

    // Normalized physical values in canonical base units (KG and CM)
    var weightKg by remember { mutableStateOf(initialWeightKg) }
    var heightCm by remember { mutableStateOf(initialHeightCm) }
    var targetWeightKg by remember { mutableStateOf(initialTargetWeightKg) }
    var waistCm by remember { mutableStateOf(initialWaistCm) }
    var neckCm by remember { mutableStateOf(initialNeckCm) }
    var hipCm by remember { mutableStateOf(initialHipCm) }
    var chestCm by remember { mutableStateOf(initialChestCm) }
    var armCm by remember { mutableStateOf(initialArmCm) }
    var thighCm by remember { mutableStateOf(initialThighCm) }
    var calfCm by remember { mutableStateOf(initialCalfCm) }

    // Text field display values
    var weightText by remember(activeWeightUnit) {
        val converted = UnitConversionEngine.convertWeight(weightKg, WeightUnit.KG, activeWeightUnit)
        val decimals = if (activeWeightUnit == WeightUnit.G) 0 else 1
        mutableStateOf(DecimalInputValidator.formatDecimal(converted, decimals))
    }

    var targetWeightText by remember(activeWeightUnit) {
        val converted = UnitConversionEngine.convertWeight(targetWeightKg, WeightUnit.KG, activeWeightUnit)
        val decimals = if (activeWeightUnit == WeightUnit.G) 0 else 1
        mutableStateOf(DecimalInputValidator.formatDecimal(converted, decimals))
    }

    var waistText by remember(activeCircumferenceUnit) {
        val v = waistCm?.let { UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, activeCircumferenceUnit) }
        mutableStateOf(v?.let { DecimalInputValidator.formatDecimal(it, 1) } ?: "")
    }

    var neckText by remember(activeCircumferenceUnit) {
        val v = neckCm?.let { UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, activeCircumferenceUnit) }
        mutableStateOf(v?.let { DecimalInputValidator.formatDecimal(it, 1) } ?: "")
    }

    var hipText by remember(activeCircumferenceUnit) {
        val v = hipCm?.let { UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, activeCircumferenceUnit) }
        mutableStateOf(v?.let { DecimalInputValidator.formatDecimal(it, 1) } ?: "")
    }

    var chestText by remember(activeCircumferenceUnit) {
        val v = chestCm?.let { UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, activeCircumferenceUnit) }
        mutableStateOf(v?.let { DecimalInputValidator.formatDecimal(it, 1) } ?: "")
    }

    var armText by remember(activeCircumferenceUnit) {
        val v = armCm?.let { UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, activeCircumferenceUnit) }
        mutableStateOf(v?.let { DecimalInputValidator.formatDecimal(it, 1) } ?: "")
    }

    var thighText by remember(activeCircumferenceUnit) {
        val v = thighCm?.let { UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, activeCircumferenceUnit) }
        mutableStateOf(v?.let { DecimalInputValidator.formatDecimal(it, 1) } ?: "")
    }

    var calfText by remember(activeCircumferenceUnit) {
        val v = calfCm?.let { UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, activeCircumferenceUnit) }
        mutableStateOf(v?.let { DecimalInputValidator.formatDecimal(it, 1) } ?: "")
    }

    var showWeightUnitSheet by remember { mutableStateOf(false) }
    var showCircumferenceUnitSheet by remember { mutableStateOf(false) }

    // Live calculation preview badge
    val liveAnalysis = remember(weightKg, heightCm, waistCm, neckCm, hipCm) {
        BodyCompositionEngine.analyze(
            weightKg = weightKg,
            heightCm = heightCm,
            age = 26,
            gender = if (isMale) "Male" else "Female",
            targetWeightKg = targetWeightKg,
            waistCm = waistCm,
            neckCm = neckCm,
            hipCm = hipCm
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Record Measurements",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Instant unit switching enabled for all fields",
                    fontSize = 12.sp,
                    color = HealthTeal
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live calculation preview badge
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = HealthTeal.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HealthTeal.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("BMI", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = String.format("%.1f", liveAnalysis.bmi),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Body Fat %", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${String.format("%.1f", liveAnalysis.bodyFatPercentage)}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = HealthTeal
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Lean Mass", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${String.format("%.1f", liveAnalysis.leanMassKg)} kg",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Weight Input
                item {
                    MeasurementDecimalInput(
                        label = "Body Weight",
                        value = weightText,
                        onValueChange = { newText ->
                            weightText = newText
                            val parsed = DecimalInputValidator.parseDecimal(newText)
                            if (parsed != null) {
                                weightKg = UnitConversionEngine.convertWeight(parsed, activeWeightUnit, WeightUnit.KG)
                            }
                        },
                        unitSymbol = activeWeightUnit.symbol,
                        onUnitClick = { showWeightUnitSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Target Weight Input
                item {
                    MeasurementDecimalInput(
                        label = "Target Goal Weight",
                        value = targetWeightText,
                        onValueChange = { newText ->
                            targetWeightText = newText
                            val parsed = DecimalInputValidator.parseDecimal(newText)
                            if (parsed != null) {
                                targetWeightKg = UnitConversionEngine.convertWeight(parsed, activeWeightUnit, WeightUnit.KG)
                            }
                        },
                        unitSymbol = activeWeightUnit.symbol,
                        onUnitClick = { showWeightUnitSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Height Input
                item {
                    HeightMeasurementInput(
                        label = "Height",
                        heightCm = heightCm,
                        currentUnit = activeHeightUnit,
                        onHeightChangedCm = { newHeightCm -> heightCm = newHeightCm },
                        onUnitChanged = { newUnit -> activeHeightUnit = newUnit },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Circumferences Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Circumferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showCircumferenceUnitSheet = true },
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${activeCircumferenceUnit.symbol.uppercase()} ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HealthTeal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Waist Circumference (Crucial for U.S. Navy & Visceral fat)
                item {
                    MeasurementDecimalInput(
                        label = "Waist Circumference (Navel Level)",
                        value = waistText,
                        onValueChange = { newText ->
                            waistText = newText
                            val parsed = DecimalInputValidator.parseDecimal(newText)
                            waistCm = parsed?.let { UnitConversionEngine.convertCircumference(it, activeCircumferenceUnit, CircumferenceUnit.CM) }
                        },
                        unitSymbol = activeCircumferenceUnit.symbol,
                        onUnitClick = { showCircumferenceUnitSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Neck Circumference
                item {
                    MeasurementDecimalInput(
                        label = "Neck Circumference",
                        value = neckText,
                        onValueChange = { newText ->
                            neckText = newText
                            val parsed = DecimalInputValidator.parseDecimal(newText)
                            neckCm = parsed?.let { UnitConversionEngine.convertCircumference(it, activeCircumferenceUnit, CircumferenceUnit.CM) }
                        },
                        unitSymbol = activeCircumferenceUnit.symbol,
                        onUnitClick = { showCircumferenceUnitSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Hip Circumference
                item {
                    MeasurementDecimalInput(
                        label = "Hip Circumference (Widest Point)",
                        value = hipText,
                        onValueChange = { newText ->
                            hipText = newText
                            val parsed = DecimalInputValidator.parseDecimal(newText)
                            hipCm = parsed?.let { UnitConversionEngine.convertCircumference(it, activeCircumferenceUnit, CircumferenceUnit.CM) }
                        },
                        unitSymbol = activeCircumferenceUnit.symbol,
                        onUnitClick = { showCircumferenceUnitSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Chest Circumference
                item {
                    MeasurementDecimalInput(
                        label = "Chest Circumference",
                        value = chestText,
                        onValueChange = { newText ->
                            chestText = newText
                            val parsed = DecimalInputValidator.parseDecimal(newText)
                            chestCm = parsed?.let { UnitConversionEngine.convertCircumference(it, activeCircumferenceUnit, CircumferenceUnit.CM) }
                        },
                        unitSymbol = activeCircumferenceUnit.symbol,
                        onUnitClick = { showCircumferenceUnitSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Arm Circumference
                item {
                    MeasurementDecimalInput(
                        label = "Arm (Bicep) Circumference",
                        value = armText,
                        onValueChange = { newText ->
                            armText = newText
                            val parsed = DecimalInputValidator.parseDecimal(newText)
                            armCm = parsed?.let { UnitConversionEngine.convertCircumference(it, activeCircumferenceUnit, CircumferenceUnit.CM) }
                        },
                        unitSymbol = activeCircumferenceUnit.symbol,
                        onUnitClick = { showCircumferenceUnitSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Thigh Circumference
                item {
                    MeasurementDecimalInput(
                        label = "Thigh Circumference",
                        value = thighText,
                        onValueChange = { newText ->
                            thighText = newText
                            val parsed = DecimalInputValidator.parseDecimal(newText)
                            thighCm = parsed?.let { UnitConversionEngine.convertCircumference(it, activeCircumferenceUnit, CircumferenceUnit.CM) }
                        },
                        unitSymbol = activeCircumferenceUnit.symbol,
                        onUnitClick = { showCircumferenceUnitSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Calf Circumference
                item {
                    MeasurementDecimalInput(
                        label = "Calf Circumference",
                        value = calfText,
                        onValueChange = { newText ->
                            calfText = newText
                            val parsed = DecimalInputValidator.parseDecimal(newText)
                            calfCm = parsed?.let { UnitConversionEngine.convertCircumference(it, activeCircumferenceUnit, CircumferenceUnit.CM) }
                        },
                        unitSymbol = activeCircumferenceUnit.symbol,
                        onUnitClick = { showCircumferenceUnitSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(weightKg, heightCm, targetWeightKg, waistCm, neckCm, hipCm, chestCm, armCm, thighCm, calfCm)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HealthTeal),
                modifier = Modifier.testTag("btn_save_measurements")
            ) {
                Text("Save & Morph Avatar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Weight Unit Bottom Sheet
    if (showWeightUnitSheet) {
        val options = WeightUnit.entries.map {
            UnitOption(unit = it, symbol = it.symbol, displayName = it.displayName, nepaliName = it.displayName)
        }
        UnitSelectorBottomSheet(
            title = "Select Weight Unit",
            currentUnit = activeWeightUnit,
            options = options,
            onUnitSelected = { selectedUnit ->
                val convertedWeight = UnitConversionEngine.convertWeight(weightKg, WeightUnit.KG, selectedUnit)
                val convertedTarget = UnitConversionEngine.convertWeight(targetWeightKg, WeightUnit.KG, selectedUnit)
                activeWeightUnit = selectedUnit
                weightText = DecimalInputValidator.formatDecimal(convertedWeight, if (selectedUnit == WeightUnit.G) 0 else 1)
                targetWeightText = DecimalInputValidator.formatDecimal(convertedTarget, if (selectedUnit == WeightUnit.G) 0 else 1)
            },
            onDismissRequest = { showWeightUnitSheet = false }
        )
    }

    // Circumference Unit Bottom Sheet
    if (showCircumferenceUnitSheet) {
        val options = CircumferenceUnit.entries.map {
            UnitOption(unit = it, symbol = it.symbol, displayName = it.displayName, nepaliName = it.displayName)
        }
        UnitSelectorBottomSheet(
            title = "Select Circumference Unit",
            currentUnit = activeCircumferenceUnit,
            options = options,
            onUnitSelected = { selectedUnit ->
                activeCircumferenceUnit = selectedUnit
                waistCm?.let { waistText = DecimalInputValidator.formatDecimal(UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, selectedUnit), 1) }
                neckCm?.let { neckText = DecimalInputValidator.formatDecimal(UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, selectedUnit), 1) }
                hipCm?.let { hipText = DecimalInputValidator.formatDecimal(UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, selectedUnit), 1) }
                chestCm?.let { chestText = DecimalInputValidator.formatDecimal(UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, selectedUnit), 1) }
                armCm?.let { armText = DecimalInputValidator.formatDecimal(UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, selectedUnit), 1) }
                thighCm?.let { thighText = DecimalInputValidator.formatDecimal(UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, selectedUnit), 1) }
                calfCm?.let { calfText = DecimalInputValidator.formatDecimal(UnitConversionEngine.convertCircumference(it, CircumferenceUnit.CM, selectedUnit), 1) }
            },
            onDismissRequest = { showCircumferenceUnitSheet = false }
        )
    }
}
