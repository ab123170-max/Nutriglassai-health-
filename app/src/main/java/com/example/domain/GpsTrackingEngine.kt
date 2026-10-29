package com.example.domain

object GpsTrackingEngine {

    const val STATIONARY_THRESHOLD_SECONDS = 5
    const val MIN_WALKING_SPEED_MPS = 0.4f // ~1.44 km/h
    const val MAX_ACCURACY_THRESHOLD_METERS = 25f

    /**
     * Haversine formula to calculate distance between two GPS coordinates in kilometers
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of the Earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c // Distance in km
    }

    /**
     * Check if GPS signal accuracy is usable (accuracy <= 50m).
     * If accuracy > 50m, GPS is inaccurate and step-fallback mode should activate.
     */
    fun isAccuracyUsable(accuracyMeters: Float): Boolean {
        return accuracyMeters > 0f && accuracyMeters <= 50f
    }

    /**
     * Strict accuracy check for filtering noisy points (accuracy <= 25m).
     */
    fun isHighAccuracyUsable(accuracyMeters: Float): Boolean {
        return accuracyMeters > 0f && accuracyMeters <= MAX_ACCURACY_THRESHOLD_METERS
    }

    /**
     * Ignore micro-drifts under 4 meters
     */
    fun isSignificantMovementMeters(distanceMeters: Float): Boolean {
        return distanceMeters >= 4.0f
    }

    /**
     * Estimated calories burned: ~65 kcal per km walked for an average adult
     */
    fun calculateCaloriesBurned(distanceKm: Float): Int {
        return (distanceKm * 65f).toInt()
    }

    /**
     * Format pace (minutes per km) as mins'secs" /km
     */
    fun formatPace(secondsElapsed: Long, distanceKm: Float): String {
        if (distanceKm <= 0.05f) return "--'--\" /km"
        val paceMinutesPerKm = (secondsElapsed / 60f) / distanceKm
        val mins = paceMinutesPerKm.toInt()
        val secs = ((paceMinutesPerKm - mins) * 60f).toInt().coerceIn(0, 59)
        return String.format("%d'%02d\" /km", mins, secs)
    }

    /**
     * Format duration timer MM:SS or HH:MM:SS
     */
    fun formatTimerDisplay(secondsElapsed: Long): String {
        val hours = secondsElapsed / 3600
        val mins = (secondsElapsed % 3600) / 60
        val secs = secondsElapsed % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    /**
     * Fallback distance calculation using step count or time-based standard walking pace (4.5 km/h)
     */
    fun calculateFallbackDistanceKm(
        secondsElapsed: Long,
        fallbackStepCount: Int,
        speedKmPerHour: Float = 4.5f
    ): Float {
        return if (fallbackStepCount > 0) {
            (fallbackStepCount * 0.75f) / 1000f // Average stride length ~0.75m per step
        } else {
            (secondsElapsed / 3600f) * speedKmPerHour // Standard 4.5 km/h walk speed
        }
    }
}

/**
 * State machine managing auto-pause and resume detection for GPS sessions
 */
class GpsAutoPauseSession {
    var isTracking: Boolean = false
        private set
    var isAutoPaused: Boolean = false
        private set
    var totalDistanceKm: Double = 0.0
        private set
    var lastCoords: Pair<Double, Double>? = null
        private set
    var stationarySecondsElapsed: Int = 0
        private set

    fun startSession() {
        isTracking = true
        isAutoPaused = false
        totalDistanceKm = 0.0
        lastCoords = null
        stationarySecondsElapsed = 0
    }

    fun stopSession() {
        isTracking = false
        isAutoPaused = false
        stationarySecondsElapsed = 0
    }

    /**
     * Process incoming GPS coordinate and speed telemetry.
     * Returns true if session state changed (e.g. paused or resumed) or distance updated.
     */
    fun processGpsUpdate(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        speedMps: Float?
    ): Boolean {
        if (!isTracking) return false

        // Skip noisy points with accuracy > 25m
        if (!GpsTrackingEngine.isHighAccuracyUsable(accuracyMeters)) {
            return false
        }

        val currentSpeed = speedMps?.coerceAtLeast(0f) ?: 0f
        val currentCoords = Pair(latitude, longitude)

        // --- AUTO-PAUSE & RESUME DETECTION ---
        if (currentSpeed < GpsTrackingEngine.MIN_WALKING_SPEED_MPS) {
            // User appears to have stopped moving
            stationarySecondsElapsed++
            if (!isAutoPaused && stationarySecondsElapsed >= GpsTrackingEngine.STATIONARY_THRESHOLD_SECONDS) {
                isAutoPaused = true
                return true // Triggered auto-pause
            }
        } else {
            // User is moving again (speed >= 0.4 m/s)
            stationarySecondsElapsed = 0
            if (isAutoPaused) {
                isAutoPaused = false
                // Resume session
            }
        }

        // --- DISTANCE ACCUMULATION ---
        if (!isAutoPaused) {
            val last = lastCoords
            if (last != null) {
                val distKm = GpsTrackingEngine.calculateDistanceKm(
                    last.first, last.second, currentCoords.first, currentCoords.second
                )
                val distMeters = distKm * 1000.0
                if (GpsTrackingEngine.isSignificantMovementMeters(distMeters.toFloat())) {
                    totalDistanceKm += distKm
                    lastCoords = currentCoords
                    return true
                }
            } else {
                lastCoords = currentCoords
                return true
            }
        }

        return false
    }

    fun manualPause() {
        isAutoPaused = true
    }

    fun manualResume() {
        isAutoPaused = false
        stationarySecondsElapsed = 0
    }
}
