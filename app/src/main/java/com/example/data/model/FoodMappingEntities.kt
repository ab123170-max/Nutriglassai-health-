package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_ontology")
data class FoodOntologyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val standardName: String, // e.g. Cooked white rice
    val nepaliName: String,
    val englishName: String,
    val regionalSynonyms: String, // Comma separated
    val category: String,
    val preparationMethod: String,
    val isRaw: Boolean,
    val usdaId: String?,
    val regionalDbId: String?
)

@Entity(tableName = "detected_food")
data class DetectedFoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionTimestamp: Long,
    val foodName: String,
    val confidence: Float,
    val estimatedWeightGrams: Float,
    val boundingBox: String, // JSON string
    val isVerified: Boolean = false,
    val mappingId: Long? = null // Reference to food_ontology
)
