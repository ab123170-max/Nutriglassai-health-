package com.example.data.local

import com.example.data.model.FastingScheduleEntity

object FastingPreloadedData {
    val defaultSchedules = listOf(
        FastingScheduleEntity(
            protocolId = "12_12",
            protocolName = "12:12 Circadian Fast",
            fastingHours = 12,
            eatingHours = 12,
            difficulty = "Beginner",
            safetyInfo = "Very safe and gentle; aligns with natural circadian rhythms.",
            isRecommended = true
        ),
        FastingScheduleEntity(
            protocolId = "13_11",
            protocolName = "13:11 Balance Fast",
            fastingHours = 13,
            eatingHours = 11,
            difficulty = "Beginner",
            safetyInfo = "Balanced approach for sustainable daily metabolic rest.",
            isRecommended = false
        ),
        FastingScheduleEntity(
            protocolId = "14_10",
            protocolName = "14:10 Gentle Fast",
            fastingHours = 14,
            eatingHours = 10,
            difficulty = "Beginner",
            safetyInfo = "Great stepping stone towards moderate metabolic flexibility.",
            isRecommended = false
        ),
        FastingScheduleEntity(
            protocolId = "16_8",
            protocolName = "16:8 LeanGains Fast",
            fastingHours = 16,
            eatingHours = 8,
            difficulty = "Intermediate",
            safetyInfo = "Most popular protocol; supports fat oxidation and insulin sensitivity.",
            isRecommended = true
        ),
        FastingScheduleEntity(
            protocolId = "18_6",
            protocolName = "18:6 Deep Fast",
            fastingHours = 18,
            eatingHours = 6,
            difficulty = "Advanced",
            safetyInfo = "Enhanced autophagy and fat burning; ensure adequate nutrient intake.",
            isRecommended = false
        ),
        FastingScheduleEntity(
            protocolId = "20_4",
            protocolName = "20:4 Warrior Fast",
            fastingHours = 20,
            eatingHours = 4,
            difficulty = "Advanced",
            safetyInfo = "Requires careful meal planning to avoid under-eating during the 4-hour window.",
            isRecommended = false
        ),
        FastingScheduleEntity(
            protocolId = "omad",
            protocolName = "OMAD (One Meal A Day)",
            fastingHours = 23,
            eatingHours = 1,
            difficulty = "Expert",
            safetyInfo = "High metabolic stress; not recommended for beginners or individuals with medical conditions.",
            isRecommended = false
        ),
        FastingScheduleEntity(
            protocolId = "5_2",
            protocolName = "5:2 Weekly Fast",
            fastingHours = 24,
            eatingHours = 24,
            difficulty = "Intermediate",
            safetyInfo = "Eat normally 5 days, consume ~500 kcal on 2 non-consecutive days.",
            isRecommended = false
        ),
        FastingScheduleEntity(
            protocolId = "alternate_day",
            protocolName = "Alternate-Day Fasting",
            fastingHours = 36,
            eatingHours = 12,
            difficulty = "Expert",
            safetyInfo = "Strict protocol requiring medical clearance and careful electrolyte monitoring.",
            isRecommended = false
        )
    )
}
