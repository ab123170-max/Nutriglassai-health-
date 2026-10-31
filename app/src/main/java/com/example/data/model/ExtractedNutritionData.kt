package com.example.data.model

data class ExtractedNutritionData(
    val foodName: String?,
    val calories: Int?,
    val protein: Float?,
    val carbs: Float?,
    val fat: Float?,
    val sugar: Float?,
    val sodium: Float?,
    val servingSize: String?,
    val ingredients: List<String> = emptyList()
)
