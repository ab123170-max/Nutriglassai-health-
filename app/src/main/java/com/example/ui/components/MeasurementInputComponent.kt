package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CircumferenceUnit
import com.example.domain.DecimalInputValidator
import com.example.domain.DistanceUnit
import com.example.domain.ExerciseWeightUnit
import com.example.domain.HeightUnit
import com.example.domain.SpeedUnit
import com.example.domain.UnitConversionEngine
import com.example.domain.WaterUnit
import com.example.domain.WeightUnit
import com.example.ui.theme.FatRed
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal

/**
 * Universal Measurement Input Component with instant in-place unit selector.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementDecimalInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unitSymbol: String,
    onUnitClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    placeholder: String = "0.0",
    conversionPreview: String? = null,
    errorMessage: String? = null,
    testTag: String = "measurement_input"
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = HealthTeal,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 6.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (conversionPreview != null) {
                Text(
                    text = conversionPreview,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { newText ->
                    val sanitized = DecimalInputValidator.sanitizeDecimalInput(newText)
                    onValueChange(sanitized)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTag}_field"),
                placeholder = {
                    Text(text = placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                isError = errorMessage != null,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HealthTeal,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Interactive Unit Button (minimum 48dp x 48dp touch target)
            Surface(
                modifier = Modifier
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onUnitClick)
                    .testTag("${testTag}_unit_btn"),
                color = HealthTeal.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HealthTeal.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = unitSymbol.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = HealthTeal
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Switch unit",
                        tint = HealthTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = FatRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }
        }
    }
}

/**
 * Interactive Height Input supporting CM, M, IN, FT, and split FT/IN.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeightMeasurementInput(
    label: String,
    heightCm: Float,
    currentUnit: HeightUnit,
    onHeightChangedCm: (Float) -> Unit,
    onUnitChanged: (HeightUnit) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String = "height_input"
) {
    var showUnitSheet by remember { mutableStateOf(false) }

    // When unit is FT_IN, we manage feet and inches text separately
    val (ftVal, inVal) = remember(heightCm, currentUnit) {
        if (currentUnit == HeightUnit.FT_IN) {
            UnitConversionEngine.cmToFtIn(heightCm)
        } else {
            0 to 0
        }
    }

    var feetText by remember(heightCm, currentUnit) {
        mutableStateOf(if (currentUnit == HeightUnit.FT_IN) ftVal.toString() else "")
    }
    var inchesText by remember(heightCm, currentUnit) {
        mutableStateOf(if (currentUnit == HeightUnit.FT_IN) inVal.toString() else "")
    }

    // For single units (CM, M, IN, FT)
    var singleUnitText by remember(heightCm, currentUnit) {
        val str = when (currentUnit) {
            HeightUnit.CM -> DecimalInputValidator.formatDecimal(heightCm, 1)
            HeightUnit.M -> DecimalInputValidator.formatDecimal(heightCm / 100f, 2)
            HeightUnit.IN -> DecimalInputValidator.formatDecimal(heightCm / 2.54f, 1)
            HeightUnit.FT -> DecimalInputValidator.formatDecimal(heightCm / 30.48f, 2)
            HeightUnit.FT_IN -> ""
        }
        mutableStateOf(str)
    }

    val previewText = remember(heightCm, currentUnit) {
        when (currentUnit) {
            HeightUnit.CM -> {
                val (ft, inch) = UnitConversionEngine.cmToFtIn(heightCm)
                "≈ $ft' $inch\" (${String.format("%.2f", heightCm / 100f)} m)"
            }
            HeightUnit.FT_IN -> "≈ ${String.format("%.1f", heightCm)} cm"
            HeightUnit.M -> "≈ ${String.format("%.1f", heightCm)} cm"
            HeightUnit.IN -> "≈ ${String.format("%.1f", heightCm)} cm"
            HeightUnit.FT -> "≈ ${String.format("%.1f", heightCm)} cm"
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = HealthTeal,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 6.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = previewText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (currentUnit == HeightUnit.FT_IN) {
            // Split dual inputs: [ 5 Feet ] [ 1 Inch ] [ FT/IN ▼ ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = feetText,
                    onValueChange = { newFt ->
                        val sanitized = newFt.filter { it.isDigit() }.take(2)
                        feetText = sanitized
                        val f = sanitized.toIntOrNull() ?: 0
                        val i = inchesText.toIntOrNull() ?: 0
                        onHeightChangedCm(UnitConversionEngine.ftInToCm(f, i))
                    },
                    label = { Text("Feet") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTag}_feet"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inchesText,
                    onValueChange = { newIn ->
                        val sanitized = newIn.filter { it.isDigit() }.take(2)
                        inchesText = sanitized
                        val f = feetText.toIntOrNull() ?: 0
                        val i = sanitized.toIntOrNull() ?: 0
                        onHeightChangedCm(UnitConversionEngine.ftInToCm(f, i))
                    },
                    label = { Text("Inches") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTag}_inches"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showUnitSheet = true }
                        .testTag("${testTag}_unit_btn"),
                    color = HealthTeal.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HealthTeal.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FT/IN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = HealthTeal
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Switch unit",
                            tint = HealthTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            // Single decimal field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = singleUnitText,
                    onValueChange = { newText ->
                        val sanitized = DecimalInputValidator.sanitizeDecimalInput(newText)
                        singleUnitText = sanitized
                        val value = sanitized.toFloatOrNull() ?: 0f
                        val newCm = when (currentUnit) {
                            HeightUnit.CM -> value
                            HeightUnit.M -> value * 100f
                            HeightUnit.IN -> value * 2.54f
                            HeightUnit.FT -> value * 30.48f
                            HeightUnit.FT_IN -> value
                        }
                        onHeightChangedCm(newCm)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTag}_field"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showUnitSheet = true }
                        .testTag("${testTag}_unit_btn"),
                    color = HealthTeal.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HealthTeal.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentUnit.symbol.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = HealthTeal
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Switch unit",
                            tint = HealthTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showUnitSheet) {
        val options = listOf(
            UnitOption(
                HeightUnit.CM,
                "cm",
                "Centimetres (cm)",
                "सेन्टिमिटर",
                "${String.format("%.1f", heightCm)} cm"
            ),
            UnitOption(
                HeightUnit.M,
                "m",
                "Metres (m)",
                "मिटर",
                "${String.format("%.2f", heightCm / 100f)} m"
            ),
            UnitOption(
                HeightUnit.FT_IN,
                "ft_in",
                "Feet & Inches (ft in)",
                "फिट र इन्च",
                UnitConversionEngine.formatHeight(heightCm, HeightUnit.FT_IN)
            ),
            UnitOption(
                HeightUnit.IN,
                "in",
                "Inches (in)",
                "इन्च",
                "${String.format("%.1f", heightCm / 2.54f)} in"
            ),
            UnitOption(
                HeightUnit.FT,
                "ft",
                "Decimal Feet (ft)",
                "फिट",
                "${String.format("%.2f", heightCm / 30.48f)} ft"
            )
        )

        UnitSelectorBottomSheet(
            title = "Height Unit",
            currentUnit = currentUnit,
            options = options,
            onUnitSelected = { newUnit ->
                onUnitChanged(newUnit)
                showUnitSheet = false
            },
            onDismissRequest = { showUnitSheet = false }
        )
    }
}
