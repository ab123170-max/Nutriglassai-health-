package com.example.vision.offline

import com.example.data.local.FoodDao
import com.example.data.model.FoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface NutritionDatabaseService {
    suspend fun searchFood(query: String): List<FoodItem>
    suspend fun findFoodByExactName(name: String): FoodItem?
    suspend fun getAllNepaliFoods(): List<FoodItem>
    suspend fun exportDatabaseToJson(): String
    suspend fun importFoodsFromJson(jsonString: String): Int
}

class StandardNutritionDatabaseService(
    private val foodDao: FoodDao
) : NutritionDatabaseService {

    override suspend fun searchFood(query: String): List<FoodItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val flowList = foodDao.searchFoods(query)
        flowList.first()
    }

    override suspend fun findFoodByExactName(name: String): FoodItem? = withContext(Dispatchers.IO) {
        foodDao.getFoodByName(name.trim())
    }

    override suspend fun getAllNepaliFoods(): List<FoodItem> = withContext(Dispatchers.IO) {
        val all = foodDao.getAllFoods().first()
        all.filter { it.isNepaliDish }
    }

    override suspend fun exportDatabaseToJson(): String = withContext(Dispatchers.IO) {
        val all = foodDao.getAllFoods().first()
        val root = JSONObject()
        root.put("version", 1)
        root.put("totalRecords", all.size)

        val array = JSONArray()
        for (item in all) {
            val obj = JSONObject().apply {
                put("name", item.name)
                put("nepaliName", item.nepaliName)
                put("category", item.category)
                put("caloriesPer100g", item.caloriesPer100g.toDouble())
                put("proteinPer100g", item.proteinPer100g.toDouble())
                put("carbsPer100g", item.carbsPer100g.toDouble())
                put("fatPer100g", item.fatPer100g.toDouble())
                put("fiberPer100g", item.fibrePer100g.toDouble())
                put("sodiumPer100g", item.sodiumPer100g.toDouble())
                put("isNepaliDish", item.isNepaliDish)
            }
            array.put(obj)
        }
        root.put("foods", array)
        root.toString(2)
    }

    override suspend fun importFoodsFromJson(jsonString: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)
        val array = root.getJSONArray("foods")
        val itemsToInsert = mutableListOf<FoodItem>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            itemsToInsert.add(
                FoodItem(
                    name = obj.getString("name"),
                    nepaliName = obj.optString("nepaliName", ""),
                    category = obj.optString("category", "General"),
                    caloriesPer100g = obj.getDouble("caloriesPer100g").toFloat(),
                    proteinPer100g = obj.getDouble("proteinPer100g").toFloat(),
                    carbsPer100g = obj.getDouble("carbsPer100g").toFloat(),
                    fatPer100g = obj.getDouble("fatPer100g").toFloat(),
                    fibrePer100g = obj.optDouble("fiberPer100g", 2.0).toFloat(),
                    sodiumPer100g = obj.optDouble("sodiumPer100g", 50.0).toFloat(),
                    isNepaliDish = obj.optBoolean("isNepaliDish", false),
                    calories = obj.getDouble("caloriesPer100g").toInt()
                )
            )
        }

        foodDao.insertFoods(itemsToInsert)
        itemsToInsert.size
    }
}
