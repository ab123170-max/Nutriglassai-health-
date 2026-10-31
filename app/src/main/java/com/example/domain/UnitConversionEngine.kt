package com.example.domain

import com.example.data.model.FoodItem
import java.util.Locale
import kotlin.math.roundToInt

enum class WeightUnit(val symbol: String, val displayName: String, val nepaliName: String) {
    KG("kg", "Kilograms (kg)", "किलोग्राम"),
    G("g", "Grams (g)", "ग्राम"),
    LB("lb", "Pounds (lb)", "पाउन्ड"),
    ST("st", "Stones (st)", "स्टोन");

    companion object {
        fun fromSymbol(symbol: String): WeightUnit =
            entries.find { it.symbol.equals(symbol, ignoreCase = true) } ?: KG
    }
}

enum class HeightUnit(val symbol: String, val displayName: String, val nepaliName: String) {
    CM("cm", "Centimetres (cm)", "सेन्टिमिटर"),
    M("m", "Metres (m)", "मिटर"),
    FT_IN("ft_in", "Feet & Inches (ft in)", "फिट र इन्च"),
    IN("in", "Inches (in)", "इन्च"),
    FT("ft", "Feet (ft)", "फिट");

    companion object {
        fun fromSymbol(symbol: String): HeightUnit =
            entries.find { it.symbol.equals(symbol, ignoreCase = true) } ?: CM
    }
}

enum class CircumferenceUnit(val symbol: String, val displayName: String, val nepaliName: String) {
    CM("cm", "Centimetres (cm)", "सेन्टिमिटर"),
    IN("in", "Inches (in)", "इन्च");

    companion object {
        fun fromSymbol(symbol: String): CircumferenceUnit =
            entries.find { it.symbol.equals(symbol, ignoreCase = true) } ?: CM
    }
}

enum class WaterUnit(val symbol: String, val displayName: String, val nepaliName: String) {
    ML("ml", "Millilitres (ml)", "मिलिलिटर"),
    L("L", "Litres (L)", "लिटर"),
    FL_OZ("fl_oz", "Fluid Ounces (fl oz)", "फ्लुइड आउन्स"),
    CUP("cup", "Cups (~240 ml)", "कप"),
    GLASS("glass", "Glasses (~250 ml)", "ग्लास"),
    BOTTLE("bottle", "Bottles (~750 ml)", "बोतल");

    companion object {
        fun fromSymbol(symbol: String): WaterUnit =
            entries.find { it.symbol.equals(symbol, ignoreCase = true) } ?: ML
    }
}

enum class DistanceUnit(val symbol: String, val displayName: String, val nepaliName: String) {
    KM("km", "Kilometres (km)", "किलोमिटर"),
    MI("mi", "Miles (mi)", "माइल"),
    M("m", "Metres (m)", "मिटर");

    companion object {
        fun fromSymbol(symbol: String): DistanceUnit =
            entries.find { it.symbol.equals(symbol, ignoreCase = true) } ?: KM
    }
}

enum class SpeedUnit(val symbol: String, val displayName: String, val nepaliName: String) {
    KM_H("km/h", "Kilometres per hour (km/h)", "किमि/घण्टा"),
    MPH("mph", "Miles per hour (mph)", "माइल/घण्टा");

    companion object {
        fun fromSymbol(symbol: String): SpeedUnit =
            entries.find { it.symbol.equals(symbol, ignoreCase = true) } ?: KM_H
    }
}

enum class ExerciseWeightUnit(val symbol: String, val displayName: String, val nepaliName: String) {
    KG("kg", "Kilograms (kg)", "किलोग्राम"),
    LB("lb", "Pounds (lb)", "पाउन्ड");

    companion object {
        fun fromSymbol(symbol: String): ExerciseWeightUnit =
            entries.find { it.symbol.equals(symbol, ignoreCase = true) } ?: KG
    }
}

enum class FoodUnit(
    val symbol: String,
    val displayName: String,
    val nepaliName: String,
    val isVolume: Boolean = false,
    val isDiscrete: Boolean = false
) {
    G("g", "Gram (g)", "ग्राम"),
    KG("kg", "Kilogram (kg)", "किलोग्राम"),
    MG("mg", "Milligram (mg)", "मिलिग्राम"),
    OZ("oz", "Ounce (oz)", "आउन्स"),
    LB("lb", "Pound (lb)", "पाउन्ड"),
    ML("ml", "Millilitre (ml)", "मिलिलिटर", isVolume = true),
    L("L", "Litre (L)", "लिटर", isVolume = true),
    FL_OZ("fl_oz", "US fluid ounce (fl oz)", "फ्लुइड आउन्स", isVolume = true),
    CUP("cup", "Cup", "कप", isVolume = true),
    TBSP("tbsp", "Tablespoon (tbsp)", "ठूलो चम्चा", isVolume = true),
    TSP("tsp", "Teaspoon (tsp)", "सानो चम्चा", isVolume = true),
    SERVING("serving", "Serving", "भाग / सर्भिङ"),
    PORTION("portion", "Portion", "अंश"),
    PIECE("piece", "Piece", "टुक्रा", isDiscrete = true),
    SLICE("slice", "Slice", "स्लाइस", isDiscrete = true),
    BOWL("bowl", "Bowl", "कचौरा"),
    PLATE("plate", "Plate", "थाल"),
    GLASS("glass", "Glass", "ग्लास", isVolume = true),
    BOTTLE("bottle", "Bottle", "बोतल", isVolume = true),
    CUP_OF_RICE("cup_of_rice", "Cup of rice", "भातको कप"),
    KATORI("katori", "Katori", "कटोरी"),
    ROTI("roti", "Roti", "रोटी", isDiscrete = true),
    CHAPATI("chapati", "Chapati", "चपाती", isDiscrete = true),
    MOMO_PIECE("momo_piece", "Momo piece", "मोमो पिस", isDiscrete = true),
    IDLI_PIECE("idli_piece", "Idli piece", "इडली पिस", isDiscrete = true),
    EGG("egg", "Egg", "अण्डा", isDiscrete = true),
    CUSTOM("custom", "Custom quantity", "कस्टम परिमाण");

    companion object {
        fun fromSymbol(symbol: String): FoodUnit {
            return entries.find {
                it.symbol.equals(symbol, ignoreCase = true) ||
                it.displayName.equals(symbol, ignoreCase = true)
            } ?: G
        }
    }
}

data class CalculatedNutrition(
    val normalizedGrams: Float,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val sugar: Float,
    val sodium: Float
)

object UnitConversionEngine {

    // --- WEIGHT CONVERSIONS (Base: Kilograms) ---
    fun kgToLb(kg: Float): Float = kg * 2.20462262f
    fun lbToKg(lb: Float): Float = lb / 2.20462262f
    fun kgToSt(kg: Float): Float = kg * 0.15747304f
    fun stToKg(st: Float): Float = st / 0.15747304f
    fun kgToG(kg: Float): Float = kg * 1000f
    fun gToKg(g: Float): Float = g / 1000f

    /**
     * Converts a weight value between any two supported WeightUnits.
     */
    fun convertWeight(value: Float, from: WeightUnit, to: WeightUnit): Float {
        if (from == to) return value
        val inKg = when (from) {
            WeightUnit.KG -> value
            WeightUnit.G -> gToKg(value)
            WeightUnit.LB -> lbToKg(value)
            WeightUnit.ST -> stToKg(value)
        }
        return when (to) {
            WeightUnit.KG -> inKg
            WeightUnit.G -> kgToG(inKg)
            WeightUnit.LB -> kgToLb(inKg)
            WeightUnit.ST -> kgToSt(inKg)
        }
    }

    fun formatWeight(weightInKg: Float, unit: WeightUnit): String {
        val converted = convertWeight(weightInKg, WeightUnit.KG, unit)
        return when (unit) {
            WeightUnit.KG -> String.format(Locale.US, "%.1f kg", converted)
            WeightUnit.G -> String.format(Locale.US, "%.0f g", converted)
            WeightUnit.LB -> String.format(Locale.US, "%.1f lb", converted)
            WeightUnit.ST -> String.format(Locale.US, "%.1f st", converted)
        }
    }

    // --- HEIGHT CONVERSIONS (Base: Centimetres) ---
    fun cmToM(cm: Float): Float = cm / 100f
    fun mToCm(m: Float): Float = m * 100f
    fun cmToIn(cm: Float): Float = cm / 2.54f
    fun inToCm(inch: Float): Float = inch * 2.54f
    fun cmToFt(cm: Float): Float = cm / 30.48f
    fun ftToCm(ft: Float): Float = ft * 30.48f

    fun cmToFtIn(cm: Float): Pair<Int, Int> {
        val totalInches = cmToIn(cm)
        val feet = (totalInches / 12f).toInt()
        val remainingInches = (totalInches % 12f).roundToInt()
        return if (remainingInches == 12) {
            (feet + 1) to 0
        } else {
            feet to remainingInches
        }
    }

    fun ftInToCm(feet: Int, inches: Int): Float {
        val totalInches = (feet * 12) + inches
        return inToCm(totalInches.toFloat())
    }

    fun convertHeight(value: Float, from: HeightUnit, to: HeightUnit): Float {
        if (from == to) return value
        val inCm = when (from) {
            HeightUnit.CM -> value
            HeightUnit.M -> mToCm(value)
            HeightUnit.IN -> inToCm(value)
            HeightUnit.FT -> ftToCm(value)
            HeightUnit.FT_IN -> value
        }
        return when (to) {
            HeightUnit.CM -> inCm
            HeightUnit.M -> cmToM(inCm)
            HeightUnit.IN -> cmToIn(inCm)
            HeightUnit.FT -> cmToFt(inCm)
            HeightUnit.FT_IN -> inCm
        }
    }

    fun formatHeight(heightInCm: Float, unit: HeightUnit): String {
        return when (unit) {
            HeightUnit.CM -> String.format(Locale.US, "%.1f cm", heightInCm)
            HeightUnit.M -> String.format(Locale.US, "%.2f m", cmToM(heightInCm))
            HeightUnit.IN -> String.format(Locale.US, "%.1f in", cmToIn(heightInCm))
            HeightUnit.FT -> String.format(Locale.US, "%.2f ft", cmToFt(heightInCm))
            HeightUnit.FT_IN -> {
                val (ft, inch) = cmToFtIn(heightInCm)
                "$ft' $inch\""
            }
        }
    }

    // --- CIRCUMFERENCE CONVERSIONS (Base: Centimetres) ---
    fun convertCircumference(value: Float, from: CircumferenceUnit, to: CircumferenceUnit): Float {
        if (from == to) return value
        val inCm = when (from) {
            CircumferenceUnit.CM -> value
            CircumferenceUnit.IN -> inToCm(value)
        }
        return when (to) {
            CircumferenceUnit.CM -> inCm
            CircumferenceUnit.IN -> cmToIn(inCm)
        }
    }

    fun formatCircumference(valueCm: Float, unit: CircumferenceUnit): String {
        val converted = convertCircumference(valueCm, CircumferenceUnit.CM, unit)
        return when (unit) {
            CircumferenceUnit.CM -> String.format(Locale.US, "%.1f cm", converted)
            CircumferenceUnit.IN -> String.format(Locale.US, "%.1f in", converted)
        }
    }

    // --- WATER CONVERSIONS (Base: Millilitres) ---
    fun mlToL(ml: Float): Float = ml / 1000f
    fun lToMl(l: Float): Float = l * 1000f
    fun mlToFlOz(ml: Float): Float = ml / 29.5735f
    fun flOzToMl(flOz: Float): Float = flOz * 29.5735f

    fun convertWater(valueMl: Float, to: WaterUnit, glassSizeMl: Int = 250, bottleSizeMl: Int = 750): Float {
        return when (to) {
            WaterUnit.ML -> valueMl
            WaterUnit.L -> mlToL(valueMl)
            WaterUnit.FL_OZ -> mlToFlOz(valueMl)
            WaterUnit.GLASS -> valueMl / glassSizeMl.toFloat()
            WaterUnit.CUP -> valueMl / 240f
            WaterUnit.BOTTLE -> valueMl / bottleSizeMl.toFloat()
        }
    }

    fun formatWater(valueMl: Float, to: WaterUnit, glassSizeMl: Int = 250, bottleSizeMl: Int = 750): String {
        val converted = convertWater(valueMl, to, glassSizeMl, bottleSizeMl)
        return when (to) {
            WaterUnit.ML -> String.format(Locale.US, "%.0f ml", converted)
            WaterUnit.L -> String.format(Locale.US, "%.2f L", converted)
            WaterUnit.FL_OZ -> String.format(Locale.US, "%.1f fl oz", converted)
            WaterUnit.GLASS -> String.format(Locale.US, "%.1f glasses", converted)
            WaterUnit.CUP -> String.format(Locale.US, "%.1f cups", converted)
            WaterUnit.BOTTLE -> String.format(Locale.US, "%.1f bottles", converted)
        }
    }

    // --- DISTANCE CONVERSIONS (Base: Kilometres) ---
    fun kmToMiles(km: Float): Float = km * 0.621371f
    fun milesToKm(miles: Float): Float = miles / 0.621371f
    fun kmToMeters(km: Float): Float = km * 1000f
    fun metersToKm(meters: Float): Float = meters / 1000f

    fun convertDistance(valueKm: Float, to: DistanceUnit): Float {
        return when (to) {
            DistanceUnit.KM -> valueKm
            DistanceUnit.MI -> kmToMiles(valueKm)
            DistanceUnit.M -> kmToMeters(valueKm)
        }
    }

    fun formatDistance(distanceKm: Float, to: DistanceUnit): String {
        val converted = convertDistance(distanceKm, to)
        return when (to) {
            DistanceUnit.KM -> String.format(Locale.US, "%.2f km", converted)
            DistanceUnit.MI -> String.format(Locale.US, "%.2f mi", converted)
            DistanceUnit.M -> String.format(Locale.US, "%.0f m", converted)
        }
    }

    // --- SPEED CONVERSIONS (Base: km/h) ---
    fun kmhToMph(kmh: Float): Float = kmh * 0.621371f
    fun mphToKmh(mph: Float): Float = mph / 0.621371f

    fun convertSpeed(value: Float, from: SpeedUnit, to: SpeedUnit): Float {
        if (from == to) return value
        val inKmh = when (from) {
            SpeedUnit.KM_H -> value
            SpeedUnit.MPH -> mphToKmh(value)
        }
        return when (to) {
            SpeedUnit.KM_H -> inKmh
            SpeedUnit.MPH -> kmhToMph(inKmh)
        }
    }

    fun formatSpeed(speedKmh: Float, to: SpeedUnit): String {
        val converted = convertSpeed(speedKmh, SpeedUnit.KM_H, to)
        return when (to) {
            SpeedUnit.KM_H -> String.format(Locale.US, "%.1f km/h", converted)
            SpeedUnit.MPH -> String.format(Locale.US, "%.1f mph", converted)
        }
    }

    // --- EXERCISE WEIGHT CONVERSIONS (Base: kg) ---
    fun convertExerciseWeight(value: Float, from: ExerciseWeightUnit, to: ExerciseWeightUnit): Float {
        if (from == to) return value
        val inKg = when (from) {
            ExerciseWeightUnit.KG -> value
            ExerciseWeightUnit.LB -> lbToKg(value)
        }
        return when (to) {
            ExerciseWeightUnit.KG -> inKg
            ExerciseWeightUnit.LB -> kgToLb(inKg)
        }
    }

    fun formatExerciseWeight(weightKg: Float, to: ExerciseWeightUnit): String {
        val converted = convertExerciseWeight(weightKg, ExerciseWeightUnit.KG, to)
        return when (to) {
            ExerciseWeightUnit.KG -> String.format(Locale.US, "%.1f kg", converted)
            ExerciseWeightUnit.LB -> String.format(Locale.US, "%.1f lb", converted)
        }
    }

    // --- FOOD DENSITY & PORTION CONVERSIONS ---
    /**
     * Look up ingredient-specific physical density in g/ml.
     * Never assumes 1 ml = 1 gram for all foods.
     */
    fun getIngredientDensity(foodName: String): Float {
        val lower = foodName.lowercase(Locale.ROOT)
        return when {
            lower.contains("oil") || lower.contains("ghee") || lower.contains("tel") -> 0.92f // Cooking oil / mustard oil / ghee
            lower.contains("honey") || lower.contains("mahad") || lower.contains("syrup") -> 1.42f
            lower.contains("milk") || lower.contains("doodh") || lower.contains("chiya") || lower.contains("tea") -> 1.03f
            lower.contains("curd") || lower.contains("dahi") || lower.contains("yogurt") -> 1.06f
            lower.contains("dal") || lower.contains("soup") || lower.contains("gravy") -> 1.05f
            lower.contains("juice") || lower.contains("ras") -> 1.04f
            lower.contains("sauce") || lower.contains("ketchup") -> 1.15f
            lower.contains("water") || lower.contains("pani") -> 1.0f
            else -> 1.0f
        }
    }

    /**
     * Converts any food quantity and unit into normalized weight in Grams.
     * Respects food-specific densities and authentic serving definitions.
     */
    fun convertFoodToGrams(
        foodName: String,
        quantity: Float,
        unit: FoodUnit,
        customServingGrams: Float = 100f
    ): Float {
        val nameLower = foodName.lowercase(Locale.ROOT)
        val density = getIngredientDensity(foodName)

        return when (unit) {
            FoodUnit.G -> quantity
            FoodUnit.KG -> quantity * 1000f
            FoodUnit.MG -> quantity / 1000f
            FoodUnit.OZ -> quantity * 28.3495f
            FoodUnit.LB -> quantity * 453.592f

            FoodUnit.ML -> quantity * density
            FoodUnit.L -> quantity * 1000f * density
            FoodUnit.FL_OZ -> quantity * 29.5735f * density

            FoodUnit.TBSP -> {
                // 1 tablespoon = 15 ml
                quantity * 15f * density
            }
            FoodUnit.TSP -> {
                // 1 teaspoon = 5 ml
                quantity * 5f * density
            }

            FoodUnit.CUP -> {
                when {
                    nameLower.contains("rice") || nameLower.contains("bhat") -> quantity * 160f // Cooked rice cup ~160g
                    nameLower.contains("flour") || nameLower.contains("atta") || nameLower.contains("maida") -> quantity * 125f
                    nameLower.contains("chiura") -> quantity * 80f
                    nameLower.contains("sugar") -> quantity * 200f
                    nameLower.contains("dal") || nameLower.contains("curry") -> quantity * 210f
                    nameLower.contains("milk") || nameLower.contains("water") -> quantity * 240f * density
                    nameLower.contains("oil") || nameLower.contains("ghee") -> quantity * 220f * density
                    else -> quantity * 240f * density
                }
            }

            FoodUnit.CUP_OF_RICE -> quantity * 160f

            FoodUnit.KATORI -> {
                when {
                    nameLower.contains("dal") -> quantity * 150f
                    nameLower.contains("rice") || nameLower.contains("bhat") -> quantity * 130f
                    nameLower.contains("curd") || nameLower.contains("dahi") -> quantity * 150f
                    nameLower.contains("sabzi") || nameLower.contains("tarkari") -> quantity * 120f
                    else -> quantity * 150f
                }
            }

            FoodUnit.BOWL -> {
                when {
                    nameLower.contains("dal") -> quantity * 200f
                    nameLower.contains("soup") || nameLower.contains("gundruk") -> quantity * 220f
                    nameLower.contains("rice") || nameLower.contains("bhat") -> quantity * 180f
                    nameLower.contains("thukpa") || nameLower.contains("noodles") -> quantity * 350f
                    nameLower.contains("dhido") -> quantity * 200f
                    else -> quantity * 220f
                }
            }

            FoodUnit.PLATE -> {
                when {
                    nameLower.contains("dal bhat") -> quantity * 450f
                    nameLower.contains("momo") -> quantity * 250f
                    nameLower.contains("chowmein") -> quantity * 250f
                    nameLower.contains("panipuri") -> quantity * 150f
                    nameLower.contains("chatpate") -> quantity * 120f
                    else -> quantity * 300f
                }
            }

            FoodUnit.GLASS -> {
                when {
                    nameLower.contains("milk") || nameLower.contains("doodh") -> quantity * 250f * 1.03f
                    nameLower.contains("chiya") || nameLower.contains("tea") -> quantity * 150f * 1.03f
                    nameLower.contains("juice") -> quantity * 240f * 1.04f
                    else -> quantity * 250f * density
                }
            }

            FoodUnit.BOTTLE -> quantity * 750f * density

            FoodUnit.ROTI, FoodUnit.CHAPATI -> quantity * 40f
            FoodUnit.MOMO_PIECE -> quantity * 25f
            FoodUnit.IDLI_PIECE -> quantity * 45f
            FoodUnit.EGG -> quantity * 50f

            FoodUnit.SLICE -> {
                when {
                    nameLower.contains("bread") || nameLower.contains("toast") -> quantity * 30f
                    nameLower.contains("cheese") -> quantity * 20f
                    nameLower.contains("cake") -> quantity * 60f
                    else -> quantity * 30f
                }
            }

            FoodUnit.PIECE -> {
                when {
                    nameLower.contains("samosa") -> quantity * 80f
                    nameLower.contains("sel roti") -> quantity * 60f
                    nameLower.contains("egg") -> quantity * 50f
                    nameLower.contains("apple") || nameLower.contains("banana") || nameLower.contains("orange") -> quantity * 150f
                    nameLower.contains("momo") -> quantity * 25f
                    nameLower.contains("biscuit") || nameLower.contains("cookie") -> quantity * 15f
                    else -> quantity * customServingGrams
                }
            }

            FoodUnit.SERVING, FoodUnit.PORTION, FoodUnit.CUSTOM -> {
                quantity * customServingGrams
            }
        }
    }

    /**
     * Calculates macronutrients and calories for a given quantity and unit of a food item.
     */
    fun calculatePortionNutrition(
        food: FoodItem,
        quantity: Float,
        unit: FoodUnit
    ): CalculatedNutrition {
        val safeGramsPerServing = if (food.gramsPerServing > 0f) food.gramsPerServing else 100f
        val normalizedGrams = convertFoodToGrams(
            foodName = food.name,
            quantity = quantity,
            unit = unit,
            customServingGrams = safeGramsPerServing
        )

        val calPer100g = food.getPer100gCalories()
        val proPer100g = food.getPer100gProtein()
        val carbPer100g = food.getPer100gCarbs()
        val fatPer100g = food.getPer100gFat()
        val fibPer100g = food.getPer100gFiber()
        val sugPer100g = food.getPer100gSugar()
        val sodPer100g = food.sodiumPer100g

        val factor = normalizedGrams / 100f

        return CalculatedNutrition(
            normalizedGrams = normalizedGrams,
            calories = (calPer100g * factor).roundToInt(),
            protein = (proPer100g * factor * 10f).roundToInt() / 10f,
            carbs = (carbPer100g * factor * 10f).roundToInt() / 10f,
            fat = (fatPer100g * factor * 10f).roundToInt() / 10f,
            fiber = (fibPer100g * factor * 10f).roundToInt() / 10f,
            sugar = (sugPer100g * factor * 10f).roundToInt() / 10f,
            sodium = (sodPer100g * factor * 10f).roundToInt() / 10f
        )
    }

    fun calculateNutrition(
        caloriesPer100g: Float,
        proteinPer100g: Float,
        carbsPer100g: Float,
        fatPer100g: Float,
        fiberPer100g: Float,
        sugarPer100g: Float,
        sodiumPer100g: Float,
        foodName: String,
        quantity: Float,
        unit: FoodUnit,
        defaultServingGrams: Float = 100f
    ): CalculatedNutrition {
        val safeGramsPerServing = if (defaultServingGrams > 0f) defaultServingGrams else 100f
        val normalizedGrams = convertFoodToGrams(
            foodName = foodName,
            quantity = quantity,
            unit = unit,
            customServingGrams = safeGramsPerServing
        )
        val factor = normalizedGrams / 100f
        return CalculatedNutrition(
            normalizedGrams = normalizedGrams,
            calories = (caloriesPer100g * factor).roundToInt(),
            protein = (proteinPer100g * factor * 10f).roundToInt() / 10f,
            carbs = (carbsPer100g * factor * 10f).roundToInt() / 10f,
            fat = (fatPer100g * factor * 10f).roundToInt() / 10f,
            fiber = (fiberPer100g * factor * 10f).roundToInt() / 10f,
            sugar = (sugarPer100g * factor * 10f).roundToInt() / 10f,
            sodium = (sodiumPer100g * factor * 10f).roundToInt() / 10f
        )
    }
}

