package com.example.data.network

import android.graphics.Bitmap
import com.example.BuildConfig
import com.example.data.model.RecognizedFoodItem
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class NutritionLabelOcrResult(
    val rawText: String,
    val caloriesKcal: Float? = null,
    val proteinGrams: Float? = null,
    val carbohydratesGrams: Float? = null,
    val fatGrams: Float? = null,
    val fiberGrams: Float? = null,
    val sugarGrams: Float? = null,
    val sodiumMilligrams: Float? = null,
    val servingSizeText: String? = null
)

data class UsdaNutritionMatch(
    val fdcId: Long,
    val description: String,
    val dataType: String,
    val caloriesKcalPer100g: Float?,
    val proteinGramsPer100g: Float?,
    val carbsGramsPer100g: Float?,
    val fatGramsPer100g: Float?,
    val fiberGramsPer100g: Float?,
    val sugarGramsPer100g: Float?,
    val sodiumMgPer100g: Float?
)

/** ML Kit OCR and USDA FoodData Central cross-checking for scanned food records. */
object FoodVerificationService {
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** Call this with a clear photo of a packaged-food Nutrition Facts panel. */
    suspend fun readNutritionLabel(bitmap: Bitmap): NutritionLabelOcrResult =
        withContext(Dispatchers.IO) {
            require(!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0) {
                "Please select a valid, clear nutrition-label photo."
            }
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0))).text
                parseNutritionLabel(text)
            } finally {
                recognizer.close()
            }
        }

    /** Extract common label values; absent/unclear values stay null, never guessed. */
    fun parseNutritionLabel(text: String): NutritionLabelOcrResult {
        fun value(vararg labels: String): Float? {
            for (label in labels) {
                val pattern = Regex(
                    "(?i)\\b" + Regex.escape(label) +
                        "\\s*[:\\-]?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(kcal|calories?|g|mg)?"
                )
                val match = pattern.find(text) ?: continue
                val number = match.groupValues[1].toFloatOrNull() ?: continue
                val unit = match.groupValues.getOrNull(2).orEmpty().lowercase(Locale.US)
                return when {
                    label.equals("sodium", true) && unit == "g" -> number * 1000f
                    label.equals("calories", true) || label.equals("energy", true) ->
                        if (unit == "kj") number / 4.184f else number
                    else -> number
                }
            }
            return null
        }
        val serving = Regex("(?i)(serving size|per serving)\\s*[:\\-]?\\s*([^\\n\\r]+)")
            .find(text)?.groupValues?.getOrNull(2)?.trim()
        return NutritionLabelOcrResult(
            rawText = text,
            caloriesKcal = value("calories", "energy"),
            proteinGrams = value("protein"),
            carbohydratesGrams = value("total carbohydrate", "carbohydrates", "carbs"),
            fatGrams = value("total fat", "fat"),
            fiberGrams = value("dietary fiber", "fiber"),
            sugarGrams = value("total sugars", "sugars", "sugar"),
            sodiumMilligrams = value("sodium"),
            servingSizeText = serving
        )
    }

    suspend fun searchUsdaFood(query: String): UsdaNutritionMatch? = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().take(100)
        if (cleanQuery.length < 2) return@withContext null
        val key = try { BuildConfig.USDA_API_KEY.trim() } catch (_: Exception) { "" }
        if (key.isBlank() || key.equals("YOUR_USDA_API_KEY", true) ||
            key.equals("BUILD_WITHOUT_API_KEY", true)) return@withContext null

        val body = JSONObject()
            .put("query", cleanQuery)
            .put("pageSize", 8)
            .put("requireAllWords", false)
            .toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("https://api.nal.usda.gov/fdc/v1/foods/search?api_key=$key")
            .post(body)
            .build()
        val response = http.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) return@withContext null
            val foods = JSONObject(it.body?.string().orEmpty()).optJSONArray("foods")
                ?: return@withContext null
            if (foods.length() == 0) return@withContext null
            val candidates = (0 until foods.length()).map { i -> foods.getJSONObject(i) }
            val chosen = candidates.firstOrNull {
                it.optString("dataType").contains("Foundation", true) ||
                    it.optString("dataType").contains("SR Legacy", true)
            } ?: candidates.first()
            val nutrients = chosen.optJSONArray("foodNutrients")
            fun nutrient(vararg ids: Int): Float? {
                if (nutrients == null) return null
                for (i in 0 until nutrients.length()) {
                    val n = nutrients.optJSONObject(i) ?: continue
                    if (ids.contains(n.optInt("nutrientId", -1))) {
                        val amount = n.optDouble("value", Double.NaN)
                        if (!amount.isNaN()) return amount.toFloat()
                    }
                }
                return null
            }
            UsdaNutritionMatch(
                fdcId = chosen.optLong("fdcId"),
                description = chosen.optString("description", cleanQuery),
                dataType = chosen.optString("dataType", "USDA FoodData Central"),
                caloriesKcalPer100g = nutrient(1008, 2047, 2048),
                proteinGramsPer100g = nutrient(1003),
                carbsGramsPer100g = nutrient(1005),
                fatGramsPer100g = nutrient(1004),
                fiberGramsPer100g = nutrient(1079),
                sugarGramsPer100g = nutrient(2000, 1063),
                sodiumMgPer100g = nutrient(1093)
            )
        }
    }

    /** Replace AI nutrient estimates only when USDA supplies the corresponding value. */
    suspend fun verifyWithUsda(items: List<RecognizedFoodItem>): List<RecognizedFoodItem> =
        withContext(Dispatchers.IO) {
            items.map { item ->
                val match = try { searchUsdaFood(item.foodName) } catch (_: Exception) { null }
                    ?: return@map item
                val grams = item.estimatedGrams.coerceIn(1f, 5000f)
                val scale = grams / 100f
                item.copy(
                    calories = match.caloriesKcalPer100g?.let { (it * scale).toInt() } ?: item.calories,
                    protein = match.proteinGramsPer100g?.times(scale) ?: item.protein,
                    carbs = match.carbsGramsPer100g?.times(scale) ?: item.carbs,
                    fat = match.fatGramsPer100g?.times(scale) ?: item.fat,
                    fiber = match.fiberGramsPer100g?.times(scale) ?: item.fiber,
                    sugar = match.sugarGramsPer100g?.times(scale) ?: item.sugar,
                    sodium = match.sodiumMgPer100g?.times(scale) ?: item.sodium,
                    uncertaintyNotes = listOf(
                        item.uncertaintyNotes,
                        "USDA FoodData Central match: ${match.description} (FDC ${match.fdcId}); scaled to estimated ${grams.toInt()} g."
                    ).filter { it.isNotBlank() }.joinToString(" ")
                )
            }
        }
}
