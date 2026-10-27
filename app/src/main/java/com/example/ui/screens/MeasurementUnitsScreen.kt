package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserMeasurementPreferences
import com.example.domain.DistanceUnit
import com.example.domain.FoodUnit
import com.example.domain.HeightUnit
import com.example.domain.WaterUnit
import com.example.domain.WeightUnit
import com.example.ui.components.SamsungHealthCard
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.ProteinBlue
import com.example.ui.theme.WaterCyan
import com.example.ui.theme.WorkoutOrange
import com.example.ui.viewmodel.HealthViewModel

@Composable
fun MeasurementUnitsScreen(
    viewModel: HealthViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.measurementPreferences.collectAsState()

    var selectedWeightUnit by remember(prefs) { mutableStateOf(prefs.weightUnit) }
    var selectedHeightUnit by remember(prefs) { mutableStateOf(prefs.heightUnit) }
    var selectedFoodUnit by remember(prefs) { mutableStateOf(prefs.foodUnit) }
    var selectedWaterUnit by remember(prefs) { mutableStateOf(prefs.waterUnit) }
    var selectedDistanceUnit by remember(prefs) { mutableStateOf(prefs.distanceUnit) }
    var glassSizeInput by remember(prefs) { mutableStateOf(prefs.usualGlassSizeMl.toString()) }
    var bottleSizeInput by remember(prefs) { mutableStateOf(prefs.usualBottleSizeMl.toString()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("units_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Measurement Units",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Global unit preferences across food, weight, height & water",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weight Units Card
            item {
                SamsungHealthCard(
                    title = "Body Weight Units",
                    icon = Icons.Default.MonitorWeight,
                    iconTint = ProteinBlue
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Select preferred unit for scale weight and target weights:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                WeightUnit.KG,
                                WeightUnit.G,
                                WeightUnit.LB,
                                WeightUnit.ST
                            ).forEach { unit ->
                                FilterChip(
                                    selected = selectedWeightUnit == unit.symbol,
                                    onClick = {
                                        selectedWeightUnit = unit.symbol
                                        viewModel.saveMeasurementPreferences(prefs.copy(weightUnit = unit.symbol))
                                    },
                                    label = { Text(unit.displayName, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("unit_weight_${unit.symbol}")
                                )
                            }
                        }
                    }
                }
            }

            // Height Units Card
            item {
                SamsungHealthCard(
                    title = "Height Units",
                    icon = Icons.Default.Height,
                    iconTint = CalorieGreen
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Used for BMR and BMI calculations:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                HeightUnit.CM,
                                HeightUnit.M,
                                HeightUnit.FT_IN,
                                HeightUnit.IN
                            ).forEach { unit ->
                                FilterChip(
                                    selected = selectedHeightUnit == unit.symbol,
                                    onClick = {
                                        selectedHeightUnit = unit.symbol
                                        viewModel.saveMeasurementPreferences(prefs.copy(heightUnit = unit.symbol))
                                    },
                                    label = { Text(unit.displayName, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("unit_height_${unit.symbol}")
                                )
                            }
                        }
                    }
                }
            }

            // Food Measurement Units Card
            item {
                SamsungHealthCard(
                    title = "Default Food Quantity Unit",
                    icon = Icons.Default.Restaurant,
                    iconTint = WorkoutOrange
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Default unit pre-selected in food logging and AI scanner:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                FoodUnit.G to "Grams (g)",
                                FoodUnit.OZ to "Ounces (oz)",
                                FoodUnit.SERVING to "Serving",
                                FoodUnit.BOWL to "Bowl",
                                FoodUnit.PLATE to "Plate",
                                FoodUnit.CUP to "Cup"
                            ).forEach { (fUnit, label) ->
                                FilterChip(
                                    selected = selectedFoodUnit == fUnit.symbol,
                                    onClick = {
                                        selectedFoodUnit = fUnit.symbol
                                        viewModel.saveMeasurementPreferences(prefs.copy(foodUnit = fUnit.symbol))
                                    },
                                    label = { Text(label, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("unit_food_${fUnit.symbol}")
                                )
                            }
                        }
                    }
                }
            }

            // Water Intake Units & Glass/Bottle Size
            item {
                SamsungHealthCard(
                    title = "Water Intake & Container Sizes",
                    icon = Icons.Default.Opacity,
                    iconTint = WaterCyan
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Preferred unit for daily hydration goals and logging:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                WaterUnit.ML,
                                WaterUnit.L,
                                WaterUnit.FL_OZ,
                                WaterUnit.GLASS,
                                WaterUnit.BOTTLE
                            ).forEach { wUnit ->
                                FilterChip(
                                    selected = selectedWaterUnit == wUnit.symbol,
                                    onClick = {
                                        selectedWaterUnit = wUnit.symbol
                                        viewModel.saveMeasurementPreferences(prefs.copy(waterUnit = wUnit.symbol))
                                    },
                                    label = { Text(wUnit.displayName, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("unit_water_${wUnit.symbol}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = glassSizeInput,
                                onValueChange = {
                                    glassSizeInput = it.filter { ch -> ch.isDigit() }
                                    it.toIntOrNull()?.let { size ->
                                        viewModel.saveMeasurementPreferences(prefs.copy(usualGlassSizeMl = size))
                                    }
                                },
                                label = { Text("Glass Size (ml)") },
                                modifier = Modifier.weight(1f).testTag("glass_size_input")
                            )
                            OutlinedTextField(
                                value = bottleSizeInput,
                                onValueChange = {
                                    bottleSizeInput = it.filter { ch -> ch.isDigit() }
                                    it.toIntOrNull()?.let { size ->
                                        viewModel.saveMeasurementPreferences(prefs.copy(usualBottleSizeMl = size))
                                    }
                                },
                                label = { Text("Bottle Size (ml)") },
                                modifier = Modifier.weight(1f).testTag("bottle_size_input")
                            )
                        }
                    }
                }
            }

            // Exercise & Distance Units
            item {
                SamsungHealthCard(
                    title = "Exercise & Distance Units",
                    icon = Icons.Default.DirectionsWalk,
                    iconTint = HealthTeal
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Used in GPS session tracking and workout history:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                DistanceUnit.KM,
                                DistanceUnit.MI,
                                DistanceUnit.M
                            ).forEach { dUnit ->
                                FilterChip(
                                    selected = selectedDistanceUnit == dUnit.symbol,
                                    onClick = {
                                        selectedDistanceUnit = dUnit.symbol
                                        viewModel.saveMeasurementPreferences(prefs.copy(distanceUnit = dUnit.symbol))
                                    },
                                    label = { Text(dUnit.displayName, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("unit_distance_${dUnit.symbol}")
                                )
                            }
                        }
                    }
                }
            }

            // Reset to defaults button
            item {
                Button(
                    onClick = {
                        viewModel.resetMeasurementPreferences()
                        selectedWeightUnit = "kg"
                        selectedHeightUnit = "cm"
                        selectedFoodUnit = "g"
                        selectedWaterUnit = "ml"
                        selectedDistanceUnit = "km"
                        glassSizeInput = "250"
                        bottleSizeInput = "750"
                    },
                    colors = ButtonDefaults.outlinedButtonColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_units_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset to Defaults (kg, cm, g, ml, km)")
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
