package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.FoodOntologyEntity
import com.example.data.model.DetectedFoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodMappingDao {

    @Query("SELECT * FROM food_ontology WHERE standardName LIKE :name OR nepaliName LIKE :name")
    suspend fun searchFood(name: String): List<FoodOntologyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodEntry(food: FoodOntologyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logDetectedFood(detectedFood: DetectedFoodEntity)

    @Query("SELECT * FROM detected_food WHERE sessionTimestamp = :timestamp")
    fun getDetectedFoodsForSession(timestamp: Long): Flow<List<DetectedFoodEntity>>
}
