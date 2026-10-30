package com.example.data.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class USDAFood(
    val fdcId: Long,
    val name: String,
    val dataType: String,
    val calories: Float?,
    val protein: Float?,
    val carbs: Float?,
    val fat: Float?,
    val fiber: Float?,
    val sugars: Float?,
    val sodiumMg: Float?
)

class USDAFoodRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("usda_food_cache", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()
    private val endpoint = "https://nutriglass-seven.vercel.app/api/usda"

    suspend fun search(query: String): Result<List<USDAFood>> = withContext(Dispatchers.IO) {
        runCatching {
            require(query.isNotBlank()) { "Enter a food name." }
            val key = "search_" + query.trim().lowercase()
            val requestBody = JSONObject()
                .put("action", "search")
                .put("query", query.trim())
                .put("pageSize", 15)
                .toString()
            val request = Request.Builder().url(endpoint)
                .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException(parseError(body, response.code))
            val root = JSONObject(body)
            if (!root.optBoolean("ok")) throw IllegalStateException(root.optString("error", "USDA search failed"))
            val array = root.optJSONArray("foods") ?: org.json.JSONArray()
            val results = (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                val n = item.optJSONObject("nutrientsPer100g") ?: JSONObject()
                USDAFood(
                    fdcId = item.optLong("fdcId"),
                    name = item.optString("description"),
                    dataType = item.optString("dataType"),
                    calories = n.nullableFloat("energyKcal"),
                    protein = n.nullableFloat("proteinG"),
                    carbs = n.nullableFloat("carbohydratesG"),
                    fat = n.nullableFloat("fatG"),
                    fiber = n.nullableFloat("fiberG"),
                    sugars = n.nullableFloat("sugarsG"),
                    sodiumMg = n.nullableFloat("sodiumMg")
                )
            }
            prefs.edit().putString(key, body).apply()
            results
        }.recoverCatching { error ->
            val cached = prefs.getString("search_" + query.trim().lowercase(), null)
            if (cached != null) {
                val root = JSONObject(cached)
                val array = root.optJSONArray("foods") ?: org.json.JSONArray()
                return@recoverCatching (0 until array.length()).mapNotNull { i ->
                    val item = array.optJSONObject(i) ?: return@mapNotNull null
                    val n = item.optJSONObject("nutrientsPer100g") ?: JSONObject()
                    USDAFood(item.optLong("fdcId"), item.optString("description"), item.optString("dataType"),
                        n.nullableFloat("energyKcal"), n.nullableFloat("proteinG"), n.nullableFloat("carbohydratesG"),
                        n.nullableFloat("fatG"), n.nullableFloat("fiberG"), n.nullableFloat("sugarsG"), n.nullableFloat("sodiumMg"))
                }
            }
            throw error
        }
    }

    private fun JSONObject.nullableFloat(name: String): Float? =
        if (!has(name) || isNull(name)) null else optDouble(name).toFloat()

    private fun parseError(body: String, code: Int): String =
        runCatching { JSONObject(body).optString("error").ifBlank { "USDA request failed ($code)." } }
            .getOrDefault("USDA request failed ($code).")
}
