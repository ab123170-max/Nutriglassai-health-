package com.example.data.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.util.LruCache
import com.example.BuildConfig
import com.example.data.local.PreloadedData
import com.example.data.model.FoodScanRecord
import com.example.data.model.MultiFoodScanResult
import com.example.data.model.RecognizedFoodItem
import com.example.domain.ImagePreparationModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class GeminiErrorType {
    API_KEY_MISSING,
    API_KEY_INVALID,
    MODEL_UNAVAILABLE,
    QUOTA_EXCEEDED,
    NETWORK_ERROR,
    INVALID_PAYLOAD,
    INVALID_RESPONSE,
    BLURRY_OR_NO_FOOD
}

class GeminiAnalysisException(
    override val message: String,
    val errorType: GeminiErrorType,
    val httpCode: Int = 0,
    cause: Throwable? = null
) : Exception(message, cause)

object GeminiFoodService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    // In-memory cache to avoid repeatedly uploading and analyzing the same image (up to 20 meals)
    private val scanResultCache = LruCache<String, MultiFoodScanResult>(20)

    /**
     * Clear the in-memory scan cache
     */
    fun clearCache() {
        scanResultCache.evictAll()
    }

    /**
     * Multi-food AI analysis identifying multiple foods, portions, preparation, allergens, and ingredients.
     * Uses ImagePreparationModule for validation, orientation correction, downsampling, and JPEG verification.
     */
    suspend fun analyzeFoodImageMulti(
        bitmap: Bitmap,
        imageUriString: String?
    ): MultiFoodScanResult = withContext(Dispatchers.IO) {
        // 1. Prepare image using robust ImagePreparationModule
        val prepared = try {
            ImagePreparationModule.prepareBitmapForAi(bitmap)
        } catch (e: Exception) {
            Log.e("FoodScanner", "Image preparation failed: ${e.message}", e)
            throw GeminiAnalysisException(
                message = "Invalid image payload: ${e.message}. Please take a clear photo of your meal.",
                errorType = GeminiErrorType.INVALID_PAYLOAD,
                cause = e
            )
        }

        val imageHash = computeHash(prepared.jpegBytes)
        Log.d("FoodScanner", "Sending image file size: ${prepared.byteCount} bytes")

        // Check if exact same image is already in memory cache
        scanResultCache.get(imageHash)?.let { cachedResult ->
            return@withContext cachedResult.copy(imageUri = imageUriString)
        }

        val prompt = buildAnalysisPrompt()
        val requestJson = buildGeminiRequestBody(prompt, prepared.base64Data, prepared.mimeType)

        val hasDirectKey = GeminiConfig.hasDirectApiKey()
        val directKey = getApiKey()

        var lastException: GeminiAnalysisException? = null

        // Channel A: Attempt via Secure Vercel Backend Proxy (hosts GEMINI_API_KEY server-side)
        try {
            Log.d("FoodScanner", "Attempting food analysis via backend proxy (${GeminiConfig.PROXY_URL})...")
            val proxyResult = executeProxyCall(requestJson, imageUriString, prepared.byteCount, prepared.mimeType)
            scanResultCache.put(imageHash, proxyResult)
            return@withContext proxyResult
        } catch (e: GeminiAnalysisException) {
            Log.w("FoodScanner", "Proxy attempt returned [${e.errorType}] (HTTP ${e.httpCode}): ${e.message}")
            lastException = e
            // If proxy returned an error and user did NOT provide a local API key, rethrow immediately
            if (!hasDirectKey) {
                throw e
            }
        } catch (e: Exception) {
            Log.w("FoodScanner", "Proxy network error: ${e.message}")
            if (!hasDirectKey) {
                throw GeminiAnalysisException(
                    message = "Network connection failed communicating with AI service. Please check your internet connection.",
                    errorType = GeminiErrorType.NETWORK_ERROR,
                    cause = e
                )
            }
        }

        // Channel B: Direct Google Generative AI Fallback (if user configured a valid direct key in AI Studio Secrets)
        if (hasDirectKey) {
            val candidateModels = listOf(GeminiConfig.activeModel, GeminiConfig.FALLBACK_MODEL, GeminiConfig.BACKUP_MODEL).distinct()
            for (model in candidateModels) {
                try {
                    Log.d("FoodScanner", "Attempting direct Google Generative AI with model $model...")
                    val result = executeDirectCall(model, directKey, requestJson, imageUriString, prepared.byteCount, prepared.mimeType)
                    scanResultCache.put(imageHash, result)
                    GeminiConfig.activeModel = model
                    return@withContext result
                } catch (e: GeminiAnalysisException) {
                    lastException = e
                    if (e.errorType == GeminiErrorType.MODEL_UNAVAILABLE && model != candidateModels.last()) {
                        continue
                    }
                    throw e
                } catch (e: IOException) {
                    throw GeminiAnalysisException(
                        message = "Network timeout communicating directly with Gemini API. Check your internet connection.",
                        errorType = GeminiErrorType.NETWORK_ERROR,
                        cause = e
                    )
                }
            }
        }

        throw lastException ?: GeminiAnalysisException(
            message = "AI service could not analyze this photo. Check connection and try again.",
            errorType = GeminiErrorType.INVALID_RESPONSE
        )
    }

    /**
     * Builds request JSON following supported Gemini generateContent structure:
     * {
     *   "contents": [
     *     {
     *       "parts": [
     *         { "text": "..." },
     *         { "inline_data": { "mime_type": "image/jpeg", "data": "..." } }
     *       ]
     *     }
     *   ]
     * }
     */
    fun buildGeminiRequestBody(prompt: String, base64Image: String, mimeType: String = "image/jpeg"): JSONObject {
        val cleanBase64 = ImagePreparationModule.cleanBase64Data(base64Image)
        return JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        // Provide both inline_data and inlineData for universal server/proxy compatibility
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", mimeType)
                                put("data", cleanBase64)
                            })
                            put("inlineData", JSONObject().apply {
                                put("mimeType", mimeType)
                                put("data", cleanBase64)
                            })
                        })
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)

            // Enforce camelCase responseMimeType for Google REST API standard
            val generationConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            }
            put("generationConfig", generationConfig)
        }
    }

    private fun executeProxyCall(
        requestJson: JSONObject,
        imageUriString: String?,
        imageByteCount: Int,
        mimeType: String
    ): MultiFoodScanResult {
        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(GeminiConfig.PROXY_URL)
            .post(requestBody)
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw GeminiAnalysisException(
                message = "Network timeout communicating with AI proxy. Check your internet connection.",
                errorType = GeminiErrorType.NETWORK_ERROR,
                cause = e
            )
        }

        val responseBody = response.body?.string() ?: ""
        val modelHeader = response.header("X-Nutriglass-Model") ?: "backend-proxy"

        if (!response.isSuccessful) {
            handleHttpError(response.code, responseBody, GeminiConfig.PROXY_URL, imageByteCount, mimeType)
        }

        return parseAndValidateGeminiResponse(responseBody, modelHeader, imageUriString)
    }

    private fun executeDirectCall(
        model: String,
        apiKey: String,
        requestJson: JSONObject,
        imageUriString: String?,
        imageByteCount: Int,
        mimeType: String
    ): MultiFoodScanResult {
        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val url = GeminiConfig.getGenerateContentUrl(model, apiKey)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw GeminiAnalysisException(
                message = "Network timeout communicating with Gemini API. Check your internet connection.",
                errorType = GeminiErrorType.NETWORK_ERROR,
                cause = e
            )
        }

        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            handleHttpError(response.code, responseBody, "Google Generative AI ($model)", imageByteCount, mimeType)
        }

        return parseAndValidateGeminiResponse(responseBody, model, imageUriString)
    }

    private fun handleHttpError(
        code: Int,
        responseBody: String,
        endpoint: String,
        imageByteCount: Int,
        mimeType: String
    ): Nothing {
        val parsedMessage = parseErrorMessage(responseBody)

        // Safe diagnostic logging (never log API keys or full base64 strings)
        Log.w(
            "FoodScanner",
            "HTTP $code error from $endpoint. Message: '$parsedMessage'. Image bytes: $imageByteCount, MIME: $mimeType, Stage: API_CALL"
        )

        when (code) {
            400 -> {
                throw GeminiAnalysisException(
                    message = "Request validation error (HTTP 400): $parsedMessage",
                    errorType = GeminiErrorType.INVALID_PAYLOAD,
                    httpCode = 400
                )
            }
            401, 403 -> {
                throw GeminiAnalysisException(
                    message = "Gemini API authorization rejected (HTTP $code): $parsedMessage",
                    errorType = GeminiErrorType.API_KEY_INVALID,
                    httpCode = code
                )
            }
            404 -> {
                throw GeminiAnalysisException(
                    message = "Gemini model is unsupported or not found (HTTP 404): $parsedMessage",
                    errorType = GeminiErrorType.MODEL_UNAVAILABLE,
                    httpCode = 404
                )
            }
            413 -> {
                throw GeminiAnalysisException(
                    message = "Image payload is too large for AI service (HTTP 413). Please take a closer, single-dish photo.",
                    errorType = GeminiErrorType.INVALID_PAYLOAD,
                    httpCode = 413
                )
            }
            429 -> {
                throw GeminiAnalysisException(
                    message = "Gemini API rate limit or quota exceeded (HTTP 429). Please wait a few moments before retrying.",
                    errorType = GeminiErrorType.QUOTA_EXCEEDED,
                    httpCode = 429
                )
            }
            503 -> {
                throw GeminiAnalysisException(
                    message = "AI service temporarily unavailable (HTTP 503): $parsedMessage",
                    errorType = GeminiErrorType.INVALID_RESPONSE,
                    httpCode = 503
                )
            }
            else -> {
                throw GeminiAnalysisException(
                    message = "AI service error (HTTP $code): $parsedMessage",
                    errorType = GeminiErrorType.INVALID_RESPONSE,
                    httpCode = code
                )
            }
        }
    }

    private fun parseAndValidateGeminiResponse(
        responseBody: String,
        modelUsed: String,
        imageUriString: String?
    ): MultiFoodScanResult {
        try {
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                throw GeminiAnalysisException(
                    message = "AI response contained no candidate results. The photo may be blurry or obscured.",
                    errorType = GeminiErrorType.BLURRY_OR_NO_FOOD
                )
            }

            val candidate = candidates.getJSONObject(0)
            val finishReason = candidate.optString("finishReason", "")
            if (finishReason == "SAFETY") {
                throw GeminiAnalysisException(
                    message = "Photo could not be processed due to content safety flags.",
                    errorType = GeminiErrorType.INVALID_PAYLOAD
                )
            }

            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                throw GeminiAnalysisException(
                    message = "Empty content parts in AI response.",
                    errorType = GeminiErrorType.INVALID_RESPONSE
                )
            }

            var rawText = parts.getJSONObject(0).optString("text", "").trim()
            if (rawText.startsWith("```json")) {
                rawText = rawText.removePrefix("```json").removeSuffix("```").trim()
            } else if (rawText.startsWith("```")) {
                rawText = rawText.removePrefix("```").removeSuffix("```").trim()
            }

            Log.d("FoodScanner", "API Response received: $rawText")

            val parsed = try {
                JSONObject(rawText)
            } catch (e: Exception) {
                Log.e("FoodScanner", "JSON Parsing failed!", e)
                null // Make sure this doesn't fallback to a hardcoded Dal Bhat object!
            }

            if (parsed == null) {
                throw GeminiAnalysisException(
                    message = "Could not parse structured nutrition data from AI response: ${parseErrorMessage(rawText)}",
                    errorType = GeminiErrorType.INVALID_RESPONSE
                )
            }

            val mealTitle = parsed.optString("meal_title", parsed.optString("mealTitle", "Identified Meal"))
            val uncertaintyNotes = parsed.optString(
                "uncertainty_notes",
                parsed.optString("uncertaintyNotes", "Portions estimated visually from photo. Cooking oils and sodium may vary.")
            )
            val disclaimer = parsed.optString(
                "disclaimer",
                "Nutritional values are AI-estimated approximations and should not replace clinical medical advice. Please confirm portion sizes before logging."
            )

            val jsonItems = parsed.optJSONArray("items") ?: JSONArray()
            val itemsList = mutableListOf<RecognizedFoodItem>()

            for (i in 0 until jsonItems.length()) {
                val itemObj = jsonItems.getJSONObject(i)

                val foodName = itemObj.optString("food_name", itemObj.optString("foodName", "Food Item")).trim()
                val nepaliName = itemObj.optString("nepali_name", itemObj.optString("nepaliName", "")).trim()
                val prepMethod = itemObj.optString("preparation_method", itemObj.optString("preparationMethod", "Standard")).trim()
                val quantity = itemObj.optDouble("serving_size", itemObj.optDouble("quantity", 1.0)).toFloat().coerceAtLeast(0.1f)
                val unit = itemObj.optString("serving_unit", itemObj.optString("unit", "serving")).trim()
                val estGrams = itemObj.optDouble("estimated_weight_grams", itemObj.optDouble("estimatedGrams", 100.0)).toFloat().coerceAtLeast(1f)

                val calories = itemObj.optInt("calories", 0).coerceAtLeast(0)
                val protein = itemObj.optDouble("protein_grams", itemObj.optDouble("protein", 0.0)).toFloat().coerceAtLeast(0f)
                val carbs = itemObj.optDouble("carbohydrates_grams", itemObj.optDouble("carbs", 0.0)).toFloat().coerceAtLeast(0f)
                val fat = itemObj.optDouble("fat_grams", itemObj.optDouble("fat", 0.0)).toFloat().coerceAtLeast(0f)
                val fiber = itemObj.optDouble("fiber_grams", itemObj.optDouble("fiber", 0.0)).toFloat().coerceAtLeast(0f)
                val sugar = itemObj.optDouble("sugar_grams", itemObj.optDouble("sugar", 0.0)).toFloat().coerceAtLeast(0f)
                val sodium = itemObj.optDouble("sodium_milligrams", itemObj.optDouble("sodium", 0.0)).toFloat().coerceAtLeast(0f)

                val rawConf = itemObj.optDouble("confidence", 0.85).toFloat()
                val confidence = rawConf.coerceIn(0.1f, 1.0f)
                val requiresConfirm = itemObj.optBoolean("requires_portion_confirmation", confidence < 0.80f)

                val ingArray = itemObj.optJSONArray("ingredients") ?: JSONArray()
                val ingredients = (0 until ingArray.length()).mapNotNull { ingArray.optString(it).takeIf { s -> s.isNotBlank() } }

                val allergensArray = itemObj.optJSONArray("allergens") ?: JSONArray()
                val allergens = (0 until allergensArray.length()).mapNotNull { allergensArray.optString(it).takeIf { s -> s.isNotBlank() } }

                val altArray = itemObj.optJSONArray("healthier_alternatives")
                    ?: itemObj.optJSONArray("alternatives")
                    ?: JSONArray()
                val alternatives = (0 until altArray.length()).mapNotNull { altArray.optString(it).takeIf { s -> s.isNotBlank() } }

                val likelyOils = itemObj.optString("likely_oils_or_sauces", itemObj.optString("likelyOilsOrSauces", ""))

                itemsList.add(
                    RecognizedFoodItem(
                        id = UUID.randomUUID().toString(),
                        foodName = foodName,
                        nepaliName = nepaliName,
                        preparationMethod = prepMethod,
                        quantity = quantity,
                        unit = unit,
                        estimatedGrams = estGrams,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        fiber = fiber,
                        sugar = sugar,
                        sodium = sodium,
                        confidence = confidence,
                        uncertaintyNotes = itemObj.optString("uncertainty_notes", ""),
                        ingredients = ingredients,
                        likelyOilsOrSauces = likelyOils,
                        allergens = allergens,
                        alternatives = alternatives,
                        requiresPortionConfirmation = requiresConfirm
                    )
                )
            }

            if (itemsList.isEmpty()) {
                throw GeminiAnalysisException(
                    message = "No recognizable food items could be identified in this image. Please take a closer photo with good lighting.",
                    errorType = GeminiErrorType.BLURRY_OR_NO_FOOD
                )
            }

            val totalCals = itemsList.sumOf { it.calories }
            val totalP = itemsList.map { it.protein }.sum()
            val totalC = itemsList.map { it.carbs }.sum()
            val totalF = itemsList.map { it.fat }.sum()
            val totalFib = itemsList.map { it.fiber }.sum()
            val totalSug = itemsList.map { it.sugar }.sum()
            val totalSod = itemsList.map { it.sodium }.sum()
            val avgConf = (itemsList.map { it.confidence }.average()).toFloat()

            val formattedTitle = if (mealTitle.isBlank() || mealTitle == "Identified Meal") {
                com.example.domain.NutritionEngine.renderMealHeader(itemsList.map { it.foodName })
            } else mealTitle

            return MultiFoodScanResult(
                mealTitle = formattedTitle,
                items = itemsList,
                totalCalories = totalCals,
                totalProtein = totalP,
                totalCarbs = totalC,
                totalFat = totalF,
                totalFiber = totalFib,
                totalSugar = totalSug,
                totalSodium = totalSod,
                overallConfidence = avgConf,
                uncertaintyNotes = uncertaintyNotes,
                disclaimer = disclaimer,
                imageUri = imageUriString,
                modelUsed = modelUsed,
                isOfflineFallback = false
            )
        } catch (e: GeminiAnalysisException) {
            throw e
        } catch (e: Exception) {
            throw GeminiAnalysisException(
                message = "Failed to parse structured nutrition data from AI response: ${e.message}",
                errorType = GeminiErrorType.INVALID_RESPONSE,
                cause = e
            )
        }
    }

    private fun buildAnalysisPrompt(): String {
        return """
            You are a professional nutrition vision AI specializing in multi-food meal identification, South Asian / Nepali dishes, and global foods.
            Identify all distinct food items present in this photo (e.g. separate rice, dal, chicken curry, sauteed vegetables, roti, salad).
            
            Return ONLY a valid JSON object matching this exact schema:
            {
              "meal_title": "Descriptive meal name (e.g. Nepali Dal Bhat Thali with Chicken Curry)",
              "uncertainty_notes": "Clear explanation of visual estimation assumptions (e.g. Hidden butter/oil in dal estimated at 1 tsp, portion estimated visually from bowl size).",
              "disclaimer": "Nutritional values are AI-estimated approximations and should not replace clinical medical advice. Please confirm portion sizes before logging.",
              "items": [
                {
                  "food_name": "Food item in English (e.g. Steamed Basmati Rice)",
                  "nepali_name": "Devanagari or romanized Nepali name if applicable (e.g. उसिनेको भात)",
                  "preparation_method": "Cooking style (e.g. Steamed, Pan-fried, Deep-fried, Boiled, Curried, Raw)",
                  "serving_size": 1.0,
                  "serving_unit": "Standard unit: g, bowl, plate, cup, katori, roti, piece, tbsp, glass, serving",
                  "estimated_weight_grams": 160.0,
                  "calories": 208,
                  "protein_grams": 4.4,
                  "carbohydrates_grams": 45.0,
                  "fat_grams": 0.5,
                  "fiber_grams": 1.0,
                  "sugar_grams": 0.1,
                  "sodium_milligrams": 2.0,
                  "confidence": 0.92,
                  "uncertainty_notes": "Visually estimated portion based on container aspect ratio.",
                  "ingredients": ["Basmati rice", "Water"],
                  "likely_oils_or_sauces": "None / Mustard oil / Ghee",
                  "allergens": ["Gluten", "Dairy", "Nuts", "Soy", "Mustard", "Egg", "Fish"],
                  "healthier_alternatives": ["Brown Rice", "Quinoa", "Cauliflower Rice"],
                  "requires_portion_confirmation": false
                }
              ]
            }
        """.trimIndent()
    }

    fun parseErrorMessage(errorBody: String): String {
        return try {
            val json = JSONObject(errorBody)
            if (json.has("error")) {
                val errObj = json.optJSONObject("error")
                errObj?.optString("message") ?: json.optString("error")
            } else if (json.has("message")) {
                json.optString("message")
            } else {
                "Service returned HTTP error"
            }
        } catch (_: Exception) {
            if (errorBody.length < 200 && errorBody.isNotBlank()) errorBody.trim() else "Unknown service error"
        }
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun computeHash(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Backward-compatible single record analyzer
     */
    suspend fun analyzeFoodImage(
        bitmap: Bitmap,
        imageUriString: String?
    ): FoodScanRecord = withContext(Dispatchers.IO) {
        val multiResult = analyzeFoodImageMulti(bitmap, imageUriString)
        val itemsDesc = multiResult.items.joinToString(", ") { "${it.foodName} (${it.estimatedGrams.toInt()}g)" }
        val allAllergens = multiResult.items.flatMap { it.allergens }.distinct().joinToString(", ")
        val allAlts = multiResult.items.flatMap { it.alternatives }.distinct().take(3).joinToString(", ")

        FoodScanRecord(
            foodName = multiResult.mealTitle,
            nepaliName = multiResult.items.firstOrNull()?.nepaliName ?: "",
            portion = "${multiResult.items.size} components",
            estimatedCalories = multiResult.totalCalories,
            calorieRange = "±${(multiResult.totalCalories * 0.12f).toInt()} kcal",
            protein = multiResult.totalProtein,
            carbs = multiResult.totalCarbs,
            fat = multiResult.totalFat,
            fiber = multiResult.totalFiber,
            sugar = multiResult.totalSugar,
            sodium = multiResult.totalSodium,
            confidence = multiResult.overallConfidence,
            uncertaintyNotes = multiResult.uncertaintyNotes,
            preparationMethod = multiResult.items.firstOrNull()?.preparationMethod ?: "Standard",
            itemsSummary = itemsDesc,
            allergens = allAllergens,
            healthierAlternatives = allAlts,
            imageUri = imageUriString,
            scannedAt = System.currentTimeMillis()
        )
    }

    /**
     * Authentic local multi-food fallback from verified Nepali nutrition database
     */
    fun generateLocalMultiEstimate(imageUriString: String?): MultiFoodScanResult {
        val rice = PreloadedData.nepaliFoodItems.find { it.name.contains("Rice", true) }
            ?: PreloadedData.nepaliFoodItems[1]
        val dal = PreloadedData.nepaliFoodItems.find { it.name.contains("Yellow Lentil", true) }
            ?: PreloadedData.nepaliFoodItems[6]
        val saag = PreloadedData.nepaliFoodItems.find { it.name.contains("Saag", true) }
            ?: PreloadedData.nepaliFoodItems[5]

        val item1 = RecognizedFoodItem(
            id = UUID.randomUUID().toString(),
            foodName = "Steamed Rice (Bhat)",
            nepaliName = "उसिनेको भात",
            preparationMethod = "Steamed boiled white rice",
            quantity = 1.0f,
            unit = "bowl",
            estimatedGrams = 180f,
            calories = rice.calories,
            protein = rice.protein,
            carbs = rice.carbs,
            fat = rice.fat,
            fiber = rice.fiber,
            sugar = rice.sugar,
            sodium = 5f,
            confidence = 0.91f,
            uncertaintyNotes = "Assumed standard moisture content.",
            ingredients = listOf("Basmati Rice", "Water"),
            likelyOilsOrSauces = "None",
            allergens = emptyList(),
            alternatives = listOf("Brown Rice", "Chiura"),
            requiresPortionConfirmation = false
        )

        val item2 = RecognizedFoodItem(
            id = UUID.randomUUID().toString(),
            foodName = "Yellow Lentil Dal (Musuro)",
            nepaliName = "मुसुरोको दाल",
            preparationMethod = "Boiled yellow lentils with cumin & turmeric tarka",
            quantity = 1.0f,
            unit = "katori",
            estimatedGrams = 150f,
            calories = (dal.calories * 0.85f).toInt(),
            protein = dal.protein * 0.85f,
            carbs = dal.carbs * 0.85f,
            fat = dal.fat * 0.85f,
            fiber = dal.fiber * 0.85f,
            sugar = dal.sugar * 0.85f,
            sodium = 190f,
            confidence = 0.89f,
            uncertaintyNotes = "Sodium estimated from standard home seasoning.",
            ingredients = listOf("Musuro lentils", "Cumin", "Turmeric", "Garlic"),
            likelyOilsOrSauces = "Mustard oil tarka",
            allergens = listOf("Mustard"),
            alternatives = listOf("Black Dal (Maas)", "Chana Dal"),
            requiresPortionConfirmation = false
        )

        val item3 = RecognizedFoodItem(
            id = UUID.randomUUID().toString(),
            foodName = "Rayo ko Saag (Mustard Greens)",
            nepaliName = "रायोको साग",
            preparationMethod = "Pan sauteed with fenugreek & mild mustard oil",
            quantity = 1.0f,
            unit = "serving",
            estimatedGrams = 120f,
            calories = saag.calories,
            protein = saag.protein,
            carbs = saag.carbs,
            fat = saag.fat,
            fiber = saag.fiber,
            sugar = saag.sugar,
            sodium = 140f,
            confidence = 0.87f,
            uncertaintyNotes = "Oil uptake estimated at 1 tsp.",
            ingredients = listOf("Fresh mustard greens", "Fenugreek seeds", "Chili"),
            likelyOilsOrSauces = "Light mustard oil",
            allergens = listOf("Mustard"),
            alternatives = listOf("Spinach", "Gundruk"),
            requiresPortionConfirmation = true
        )

        val items = listOf(item1, item2, item3)
        val totalCals = items.sumOf { it.calories }
        val totalP = items.map { it.protein }.sum()
        val totalC = items.map { it.carbs }.sum()
        val totalF = items.map { it.fat }.sum()
        val totalFib = items.map { it.fiber }.sum()
        val totalSug = items.map { it.sugar }.sum()
        val totalSod = items.map { it.sodium }.sum()

        return MultiFoodScanResult(
            mealTitle = "Nepali Dal Bhat Tarkari Thali",
            items = items,
            totalCalories = totalCals,
            totalProtein = totalP,
            totalCarbs = totalC,
            totalFat = totalF,
            totalFiber = totalFib,
            totalSugar = totalSug,
            totalSodium = totalSod,
            overallConfidence = 0.89f,
            uncertaintyNotes = "Offline local profile loaded. Values based on verified Nepali nutritional composition tables. Confirm portions before logging.",
            disclaimer = "Nutritional values are approximations and should not replace clinical medical advice. Please confirm portion sizes before logging.",
            imageUri = imageUriString,
            modelUsed = "offline-local-db",
            isOfflineFallback = true
        )
    }

    /**
     * Ask AI Health Coach for advice based on user stats and logged data
     */
    suspend fun getHealthCoachGuidance(
        userGoal: String,
        caloriesConsumedToday: Int,
        targetCalories: Int,
        waterConsumedMl: Int,
        stepsTaken: Int
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        if (!GeminiConfig.hasDirectApiKey()) {
            return@withContext generateLocalHealthAdvice(userGoal, caloriesConsumedToday, targetCalories, waterConsumedMl, stepsTaken)
        }

        try {
            val prompt = """
                You are NutriGlass AI Health Coach, an empathetic, evidence-based nutrition and lifestyle coach specializing in healthy living and South Asian/Nepali balanced nutrition.
                User profile stats:
                - Goal: $userGoal
                - Calories consumed today: $caloriesConsumedToday kcal (Daily Target: $targetCalories kcal)
                - Water intake: $waterConsumedMl ml
                - Daily steps: $stepsTaken steps
                
                Provide a short (3-4 bullet points), practical, encouraging health guidance note.
                Recommend accessible, wholesome foods (e.g. lentils/dal, saag, beans, whole grains).
                Include an explicit reminder: "Disclaimer: Nutritional guidance is for informational and educational purposes. Consult a physician or registered dietitian for clinical advice."
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val candidateModels = listOf(GeminiConfig.activeModel, GeminiConfig.FALLBACK_MODEL).distinct()

            for (model in candidateModels) {
                val url = GeminiConfig.getGenerateContentUrl(model, apiKey)
                try {
                    val response = client.newCall(Request.Builder().url(url).post(requestBody).build()).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        val json = JSONObject(responseBody)
                        val text = json.optJSONArray("candidates")?.optJSONObject(0)
                            ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                        if (!text.isNullOrBlank()) {
                            return@withContext text
                        }
                    }
                } catch (_: Exception) {
                    // Try next candidate model
                }
            }

            generateLocalHealthAdvice(userGoal, caloriesConsumedToday, targetCalories, waterConsumedMl, stepsTaken)
        } catch (_: Exception) {
            generateLocalHealthAdvice(userGoal, caloriesConsumedToday, targetCalories, waterConsumedMl, stepsTaken)
        }
    }

    private fun generateLocalHealthAdvice(
        goal: String,
        cals: Int,
        targetCals: Int,
        waterMl: Int,
        steps: Int
    ): String {
        val calDiff = targetCals - cals
        val calTip = if (calDiff > 300) {
            "You have approximately $calDiff kcal remaining today. A nourishing bowl of yellow dal (musuro) or mixed sprouted kwatee with steamed greens will supply optimal protein and micronutrients without excess refined sugars."
        } else if (calDiff in 0..300) {
            "Great job maintaining balanced energy balance! You are right on target for your $goal goal."
        } else {
            "You are slightly above your base target (+${-calDiff} kcal). Consider a gentle 20-minute evening walk to support digestive health and insulin sensitivity."
        }

        val hydraTip = if (waterMl < 1500) {
            "Your hydration is currently at ${waterMl}ml. Sip an additional glass of water or herbal tea before bedtime to support cellular hydration."
        } else {
            "Excellent hydration levels today (${waterMl}ml). Consistent water intake optimizes metabolic rate and satiety."
        }

        val stepTip = if (steps < 5000) {
            "You have logged $steps steps so far. Even a quick 10-minute stroll can boost blood circulation and cognitive focus."
        } else {
            "Terrific active movement today with $steps recorded steps!"
        }

        return """
            • Nutrition: $calTip
            • Hydration: $hydraTip
            • Activity: $stepTip
            • Nepali Food Tip: Combine lentils with fermented gundruk or rayo ko saag for natural iron, fiber, and probiotic digestive benefits.
            
            Disclaimer: AI nutritional estimates and coaching tips are for wellness tracking and educational purposes. Always consult a physician or registered dietitian for medical diagnosis or clinical diet planning.
        """.trimIndent()
    }
}
