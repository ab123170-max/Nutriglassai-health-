package com.example.domain

import com.example.data.model.FoodItem
import java.util.Locale
import kotlin.math.roundToInt

enum class WeightUnit(val symbol: String, val displayName: String) {
    KG("kg", "Kilograms (kg)"),
    G("g", "Grams (g)"),
    LB("lb", "Pounds (lb)"),
    ST("st", "Stones (st)");

    companion object {
        fun fromSymbol(symbol: String): WeightUnit =
            entries.find { it.symbol.equals(symbol, ignoreCase = true) } ?: KG
    }
}

enum class HeightUnit(val symbol: String, val displayName: String) {
    CM("cm", "Centimetres (cm)"),
    M("m", "Metres (m)"),
    FT_IN("ft_in", "Feet and Inches (ft in)"),
    IN("in", "Inches (in)");

    companion object {
        fun fromSymbol(symbol: String): HeightUnit =
            entries.find { it.symbol.equals(symbol, ignoreCase = true) } ?: CM
    }
}

enum class WaterUnit(val symbol: String, val displayName: String) {
    ML("ml", "Millilitres (ml)"),
    L("L", "Litres (L)"),
    FL_OZ("fl_oz", "US Fluid Ounces (fl oz)"),
    GLASS("glass", "Glasses"),
    CUP("cup", "Cups"),
    BOTTLE("bottle", "Bottles");

    companion object {
        fun fromSymbol(symbol: String): WaterUnit =
            entries.find { it.symbol.equals(symbol, ignoreCase = true) } ?: ML
    }
}

enum class DistanceUnit(val symbol: String, val displayName: String) {
    KM("km", "Kilometres (km)"),
    MI("mi", "Miles (mi)"),
    M("m", "Metres (m)");

    companion object {
        fun fromSymbol(symbol: String): DistanceUnit =
            entries.find { it.symbol.equals(symbol, ignoreCase = true) } ?: KM
    }
}

enum class FoodUnit(val symbol: String, val displayName: String, val isVolume: Boolean = false, val isDiscrete: Boolean = false) {
    G("g", "Gram (g)"),
    KG("kg", "Kilogram (kg)"),
    MG("mg", "Milligram (mg)"),
    OZ("oz", "Ounce (oz)"),
    LB("lb", "Pound (lb)"),
    ML("ml", "Millilitre (ml)", isVolume = true),
    L("L", "Litre (L)", isVolume = true),
    FL_OZ("fl_oz", "US fluid ounce (fl oz)", isVolume = true),
    CUP("cup", "Cup", isVolume = true),
    TBSP("tbsp", "Tablespoon (tbsp)", isVolume = true),
    TSP("tsp", "Teaspoon (tsp)", isVolume = true),
    PIECE("piece", "Piece", isDiscrete = true),
    SERVING("serving", "Serving"),
    PORTION("portion", "Portion"),
    SLICE("slice", "Slice", isDiscrete = true),
    BOWL("bowl", "Bowl"),
    PLATE("plate", "Plate"),
    GLASS("glass", "Glass", isVolume = true),
    BOTTLE("bottle", "Bottle", isVolume = true),
    CUP_OF_RICE("cup_of_rice", "Cup of rice"),
    KATORI("katori", "Katori"),
    ROTI("roti", "Roti", isDiscrete = true),
    CHAPATI("chapati", "Chapati", isDiscrete = true),
    MOMO_PIECE("momo_piece", "Momo piece", isDiscrete = true),
    IDLI_PIECE("idli_piece", "Idli piece", isDiscrete = true),
    EGG("egg", "Egg", isDiscrete = true),
    CUSTOM("custom", "Custom quantity");

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
    fun kgToLb(kg: Float): Float = kg * 2.20462f
    fun lbToKg(lb: Float): Float = lb / 2.20462f
    fun kgToSt(kg: Float): Float = kg * 0.157473f
    fun stToKg(st: Float): Float = st / 0.157473f
    fun kgToG(kg: Float): Float = kg * 1000f
    fun gToKg(g: Float): Float = g / 1000f

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
            HeightUnit.FT_IN -> value // assumes passed as total cm if single float
        }
        return when (to) {
            HeightUnit.CM -> inCm
            HeightUnit.M -> cmToM(inCm)
            HeightUnit.IN -> cmToIn(inCm)
            HeightUnit.FT_IN -> inCm
        }
    }

    fun formatHeight(heightInCm: Float, unit: HeightUnit): String {
        return when (unit) {
            HeightUnit.CM -> String.format(Locale.US, "%.0f cm", heightInCm)
            HeightUnit.M -> String.format(Locale.US, "%.2f m", cmToM(heightInCm))
            HeightUnit.IN -> String.format(Locale.US, "%.1f in", cmToIn(heightInCm))
            HeightUnit.FT_IN -> {
                val (ft, inch) = cmToFtIn(heightInCm)
                "$ft' $inch\""
            }
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

    // --- FOOD-SPECIFIC PORTION CONVERSIONS & DENSITY TABLE ---
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

        return when (unit) {
            FoodUnit.G -> quantity
            FoodUnit.KG -> quantity * 1000f
            FoodUnit.MG -> quantity / 1000f
            FoodUnit.OZ -> quantity * 28.3495f
            FoodUnit.LB -> quantity * 453.592f

            FoodUnit.ML -> {
                // Density adjustment: Milk/Juice ~1.03g/ml, Cooking Oil ~0.92g/ml, Water ~1.0g/ml
                val density = when {
                    nameLower.contains("oil") || nameLower.contains("ghee") -> 0.92f
                    nameLower.contains("milk") || nameLower.contains("chiya") || nameLower.contains("tea") -> 1.03f
                    nameLower.contains("honey") || nameLower.contains("syrup") -> 1.42f
                    else -> 1.0f
                }
                quantity * density
            }
            FoodUnit.L -> convertFoodToGrams(foodName, quantity * 1000f, FoodUnit.ML, customServingGrams)
            FoodUnit.FL_OZ -> convertFoodToGrams(foodName, quantity * 29.5735f, FoodUnit.ML, customServingGrams)

            FoodUnit.TBSP -> {
                // 1 tbsp = 15 ml
                convertFoodToGrams(foodName, quantity * 15f, FoodUnit.ML, customServingGrams)
            }
            FoodUnit.TSP -> {
                // 1 tsp = 5 ml
                convertFoodToGrams(foodName, quantity * 5f, FoodUnit.ML, customServingGrams)
            }

            FoodUnit.CUP -> {
                // Food specific cup densities:
                when {
                    nameLower.contains("rice") || nameLower.contains("bhat") -> quantity * 160f // Cooked rice cup ~160g
                    nameLower.contains("flour") || nameLower.contains("atta") || nameLower.contains("maida") -> quantity * 125f
                    nameLower.contains("chiura") -> quantity * 80f
                    nameLower.contains("dal") || nameLower.contains("curry") -> quantity * 200f
                    nameLower.contains("sugar") -> quantity * 200f
                    nameLower.contains("milk") || nameLower.contains("water") -> quantity * 240f
                    else -> quantity * 200f
                }
            }

            FoodUnit.CUP_OF_RICE -> quantity * 160f

            FoodUnit.KATORI -> {
                // Traditional South Asian small steel bowl ~150g
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
                    nameLower.contains("momo") -> quantity * 250f // 1 plate ~ 10 pcs
                    nameLower.contains("chowmein") -> quantity * 250f
                    nameLower.contains("panipuri") -> quantity * 150f
                    nameLower.contains("chatpate") -> quantity * 120f
                    else -> quantity * 300f
                }
            }

            FoodUnit.GLASS -> {
                when {
                    nameLower.contains("milk") || nameLower.contains("doodh") -> quantity * 250f
                    nameLower.contains("chiya") || nameLower.contains("tea") -> quantity * 150f
                    nameLower.contains("juice") -> quantity * 240f
                    else -> quantity * 250f
                }
            }

            FoodUnit.BOTTLE -> quantity * 750f

            FoodUnit.ROTI, FoodUnit.CHAPATI -> quantity * 40f // Standard roti ~ 40g

            FoodUnit.MOMO_PIECE -> quantity * 25f // Single momo piece ~ 25g

            FoodUnit.IDLI_PIECE -> quantity * 45f // Single idli piece ~ 45g

            FoodUnit.EGG -> quantity * 50f // Standard egg ~ 50g

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
     * Calculates nutrition dynamically based on per-100g base values or per-serving values.
     */
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
        val totalGrams = convertFoodToGrams(foodName, quantity, unit, defaultServingGrams)
        val factor = totalGrams / 100f

        return CalculatedNutrition(
            normalizedGrams = (totalGrams * 10f).roundToInt() / 10f,
            calories = (caloriesPer100g * factor).roundToInt(),
            protein = ((proteinPer100g * factor) * 10f).roundToInt() / 10f,
            carbs = ((carbsPer100g * factor) * 10f).roundToInt() / 10f,
            fat = ((fatPer100g * factor) * 10f).roundToInt() / 10f,
            fiber = ((fiberPer100g * factor) * 10f).roundToInt() / 10f,
            sugar = ((sugarPer100g * factor) * 10f).roundToInt() / 10f,
            sodium = ((sodiumPer100g * factor) * 10f).roundToInt() / 10f
        )
    }
}
