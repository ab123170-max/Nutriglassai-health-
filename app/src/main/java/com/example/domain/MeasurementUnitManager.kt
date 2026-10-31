package com.example.domain

import com.example.data.model.UserMeasurementPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.roundToInt

data class ActiveMeasurementUnits(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val heightUnit: HeightUnit = HeightUnit.CM,
    val circumferenceUnit: CircumferenceUnit = CircumferenceUnit.CM,
    val waterUnit: WaterUnit = WaterUnit.ML,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val speedUnit: SpeedUnit = SpeedUnit.KM_H,
    val exerciseWeightUnit: ExerciseWeightUnit = ExerciseWeightUnit.KG,
    val foodUnit: FoodUnit = FoodUnit.G
)

/**
 * Universal Measurement Unit Manager.
 * Orchestrates live unit switching, preservation of underlying physical base values,
 * and seamless conversions across all app modules.
 */
class MeasurementUnitManager(
    initialPreferences: UserMeasurementPreferences? = null
) {
    private val _activeUnits = MutableStateFlow(
        ActiveMeasurementUnits(
            weightUnit = WeightUnit.fromSymbol(initialPreferences?.weightUnit ?: "kg"),
            heightUnit = HeightUnit.fromSymbol(initialPreferences?.heightUnit ?: "cm"),
            circumferenceUnit = CircumferenceUnit.fromSymbol(initialPreferences?.bodyMeasurementUnit ?: "cm"),
            waterUnit = WaterUnit.fromSymbol(initialPreferences?.waterUnit ?: "ml"),
            distanceUnit = DistanceUnit.fromSymbol(initialPreferences?.distanceUnit ?: "km"),
            speedUnit = SpeedUnit.fromSymbol(initialPreferences?.speedUnit ?: "km/h"),
            exerciseWeightUnit = ExerciseWeightUnit.fromSymbol(initialPreferences?.exerciseWeightUnit ?: "kg"),
            foodUnit = FoodUnit.fromSymbol(initialPreferences?.foodUnit ?: "g")
        )
    )
    val activeUnits: StateFlow<ActiveMeasurementUnits> = _activeUnits.asStateFlow()

    fun updateFromPreferences(preferences: UserMeasurementPreferences) {
        _activeUnits.value = _activeUnits.value.copy(
            weightUnit = WeightUnit.fromSymbol(preferences.weightUnit),
            heightUnit = HeightUnit.fromSymbol(preferences.heightUnit),
            circumferenceUnit = CircumferenceUnit.fromSymbol(preferences.bodyMeasurementUnit),
            waterUnit = WaterUnit.fromSymbol(preferences.waterUnit),
            distanceUnit = DistanceUnit.fromSymbol(preferences.distanceUnit),
            speedUnit = SpeedUnit.fromSymbol(preferences.speedUnit),
            exerciseWeightUnit = ExerciseWeightUnit.fromSymbol(preferences.exerciseWeightUnit),
            foodUnit = FoodUnit.fromSymbol(preferences.foodUnit)
        )
    }

    fun setWeightUnit(unit: WeightUnit) {
        _activeUnits.value = _activeUnits.value.copy(weightUnit = unit)
    }

    fun setHeightUnit(unit: HeightUnit) {
        _activeUnits.value = _activeUnits.value.copy(heightUnit = unit)
    }

    fun setCircumferenceUnit(unit: CircumferenceUnit) {
        _activeUnits.value = _activeUnits.value.copy(circumferenceUnit = unit)
    }

    fun setWaterUnit(unit: WaterUnit) {
        _activeUnits.value = _activeUnits.value.copy(waterUnit = unit)
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        _activeUnits.value = _activeUnits.value.copy(distanceUnit = unit)
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        _activeUnits.value = _activeUnits.value.copy(speedUnit = unit)
    }

    fun setExerciseWeightUnit(unit: ExerciseWeightUnit) {
        _activeUnits.value = _activeUnits.value.copy(exerciseWeightUnit = unit)
    }

    fun setFoodUnit(unit: FoodUnit) {
        _activeUnits.value = _activeUnits.value.copy(foodUnit = unit)
    }

    // --- INSTANT WEIGHT CONVERSION HELPERS ---
    /**
     * Converts a displayed weight string when the user switches units in-place,
     * maintaining the exact physical weight without mutating the underlying normalized kg.
     */
    fun convertWeightDisplayString(
        currentText: String,
        fromUnit: WeightUnit,
        toUnit: WeightUnit
    ): String {
        val value = DecimalInputValidator.parseDecimal(currentText) ?: return ""
        val converted = UnitConversionEngine.convertWeight(value, fromUnit, toUnit)
        val decimals = if (toUnit == WeightUnit.G) 0 else 1
        return DecimalInputValidator.formatDecimal(converted, decimals)
    }

    fun parseWeightToKg(text: String, unit: WeightUnit): Float? {
        val value = DecimalInputValidator.parseDecimal(text) ?: return null
        return UnitConversionEngine.convertWeight(value, unit, WeightUnit.KG)
    }

    fun formatKgToDisplay(kg: Float, unit: WeightUnit): String {
        val converted = UnitConversionEngine.convertWeight(kg, WeightUnit.KG, unit)
        val decimals = if (unit == WeightUnit.G) 0 else 1
        return DecimalInputValidator.formatDecimal(converted, decimals)
    }

    // --- INSTANT HEIGHT CONVERSION HELPERS ---
    fun convertHeightDisplayString(
        currentText: String,
        fromUnit: HeightUnit,
        toUnit: HeightUnit
    ): Pair<String, String?> {
        // If from FT_IN, text might be feet and second param is inches
        val value = DecimalInputValidator.parseDecimal(currentText) ?: return Pair("", null)
        val cmValue = when (fromUnit) {
            HeightUnit.CM -> value
            HeightUnit.M -> value * 100f
            HeightUnit.IN -> value * 2.54f
            HeightUnit.FT -> value * 30.48f
            HeightUnit.FT_IN -> value
        }
        return formatCmToDisplay(cmValue, toUnit)
    }

    fun formatCmToDisplay(cm: Float, unit: HeightUnit): Pair<String, String?> {
        return when (unit) {
            HeightUnit.CM -> Pair(DecimalInputValidator.formatDecimal(cm, 1), null)
            HeightUnit.M -> Pair(DecimalInputValidator.formatDecimal(cm / 100f, 2), null)
            HeightUnit.IN -> Pair(DecimalInputValidator.formatDecimal(cm / 2.54f, 1), null)
            HeightUnit.FT -> Pair(DecimalInputValidator.formatDecimal(cm / 30.48f, 2), null)
            HeightUnit.FT_IN -> {
                val (feet, inches) = UnitConversionEngine.cmToFtIn(cm)
                Pair(feet.toString(), inches.toString())
            }
        }
    }

    fun parseHeightToCm(text: String, unit: HeightUnit, inchesText: String? = null): Float? {
        return when (unit) {
            HeightUnit.FT_IN -> {
                val feet = text.toIntOrNull() ?: 0
                val inches = inchesText?.toIntOrNull() ?: 0
                UnitConversionEngine.ftInToCm(feet, inches)
            }
            else -> {
                val value = DecimalInputValidator.parseDecimal(text) ?: return null
                UnitConversionEngine.convertHeight(value, unit, HeightUnit.CM)
            }
        }
    }

    // --- INSTANT CIRCUMFERENCE CONVERSION HELPERS ---
    fun convertCircumferenceDisplayString(
        currentText: String,
        fromUnit: CircumferenceUnit,
        toUnit: CircumferenceUnit
    ): String {
        val value = DecimalInputValidator.parseDecimal(currentText) ?: return ""
        val converted = UnitConversionEngine.convertCircumference(value, fromUnit, toUnit)
        return DecimalInputValidator.formatDecimal(converted, 1)
    }

    fun parseCircumferenceToCm(text: String, unit: CircumferenceUnit): Float? {
        val value = DecimalInputValidator.parseDecimal(text) ?: return null
        return UnitConversionEngine.convertCircumference(value, unit, CircumferenceUnit.CM)
    }

    fun formatCircumferenceToDisplay(cm: Float, unit: CircumferenceUnit): String {
        val converted = UnitConversionEngine.convertCircumference(cm, CircumferenceUnit.CM, unit)
        return DecimalInputValidator.formatDecimal(converted, 1)
    }

    // --- INSTANT WATER CONVERSION HELPERS ---
    fun convertWaterDisplayString(
        currentText: String,
        fromUnit: WaterUnit,
        toUnit: WaterUnit
    ): String {
        val value = DecimalInputValidator.parseDecimal(currentText) ?: return ""
        // convert fromUnit to ML
        val ml = when (fromUnit) {
            WaterUnit.ML -> value
            WaterUnit.L -> value * 1000f
            WaterUnit.FL_OZ -> value * 29.5735f
            WaterUnit.CUP -> value * 240f
            WaterUnit.GLASS -> value * 250f
            WaterUnit.BOTTLE -> value * 750f
        }
        val converted = UnitConversionEngine.convertWater(ml, toUnit)
        val decimals = if (toUnit == WaterUnit.ML) 0 else 1
        return DecimalInputValidator.formatDecimal(converted, decimals)
    }

    fun parseWaterToMl(text: String, unit: WaterUnit): Float? {
        val value = DecimalInputValidator.parseDecimal(text) ?: return null
        return when (unit) {
            WaterUnit.ML -> value
            WaterUnit.L -> value * 1000f
            WaterUnit.FL_OZ -> value * 29.5735f
            WaterUnit.CUP -> value * 240f
            WaterUnit.GLASS -> value * 250f
            WaterUnit.BOTTLE -> value * 750f
        }
    }

    fun formatWaterToDisplay(ml: Float, unit: WaterUnit): String {
        val converted = UnitConversionEngine.convertWater(ml, unit)
        val decimals = if (unit == WaterUnit.ML) 0 else 1
        return DecimalInputValidator.formatDecimal(converted, decimals)
    }
}
