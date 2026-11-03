package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONArray

/** Foreground GPS session that keeps recording while the app is backgrounded. */
class LocationTrackingService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastAcceptedLocation: Location? = null
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach(::acceptLocation)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSession()
            ACTION_PAUSE -> pauseSession()
            ACTION_RESUME -> resumeSession()
            ACTION_START -> {
                if (intent.getBooleanExtra(EXTRA_RESET, false)) resetSession()
                startAsForeground("Recording your GPS route")
                prefs.edit().putBoolean(KEY_ACTIVE, true).putBoolean(KEY_PAUSED, false)
                    .putLong(KEY_STARTED_AT, System.currentTimeMillis()).apply()
                requestLocationUpdates()
            }
            else -> {
                if (prefs.getBoolean(KEY_ACTIVE, false)) {
                    startAsForeground("Restoring your GPS route")
                    if (!prefs.getBoolean(KEY_PAUSED, false)) {
                        if (prefs.getLong(KEY_STARTED_AT, 0L) == 0L) {
                            prefs.edit().putLong(KEY_STARTED_AT, System.currentTimeMillis()).apply()
                        }
                        requestLocationUpdates()
                    }
                } else stopSelf()
            }
        }
        return START_STICKY
    }

    private fun resetSession() {
        lastAcceptedLocation = null
        prefs.edit()
            .putFloat(KEY_DISTANCE, 0f)
            .putString(KEY_ROUTE, "[]")
            .putLong(KEY_ELAPSED_BASE, 0L)
            .putLong(KEY_STARTED_AT, 0L)
            .putBoolean(KEY_ACTIVE, true)
            .putBoolean(KEY_PAUSED, false)
            .apply()
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .setMinUpdateDistanceMeters(3f)
            .build()
        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            updateNotification("GPS permission is missing. Reopen Nutriglass AI to enable location.")
            stopSession()
        }
    }

    private fun acceptLocation(location: Location) {
        if (prefs.getBoolean(KEY_PAUSED, false) || !prefs.getBoolean(KEY_ACTIVE, false)) return
        if (!location.hasAccuracy() || location.accuracy > 25f) return
        val previous = lastAcceptedLocation
        if (previous == null) {
            lastAcceptedLocation = location
            appendRoutePoint(location)
            return
        }
        val segment = previous.distanceTo(location)
        val seconds = (location.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1_000_000_000.0
        val speed = if (seconds > 0.0) segment / seconds else Double.POSITIVE_INFINITY
        if (segment >= 3f && segment <= 100f && seconds > 0.0 && speed <= 8.0) {
            prefs.edit().putFloat(KEY_DISTANCE, prefs.getFloat(KEY_DISTANCE, 0f) + segment).apply()
            lastAcceptedLocation = location
            appendRoutePoint(location)
            updateNotification(String.format(java.util.Locale.US, "Recording walk · %.2f km", prefs.getFloat(KEY_DISTANCE, 0f) / 1000f))
        } else if (segment < 3f || segment > 100f || speed > 8.0) {
            lastAcceptedLocation = location
        }
    }

    private fun appendRoutePoint(location: Location) {
        val route = try { JSONArray(prefs.getString(KEY_ROUTE, "[]")) } catch (_: Exception) { JSONArray() }
        route.put(JSONArray().put(location.latitude).put(location.longitude))
        while (route.length() > 1200) route.remove(0)
        prefs.edit().putString(KEY_ROUTE, route.toString()).apply()
    }

    private fun pauseSession() {
        if (!prefs.getBoolean(KEY_ACTIVE, false)) return
        val started = prefs.getLong(KEY_STARTED_AT, 0L)
        val base = prefs.getLong(KEY_ELAPSED_BASE, 0L)
        val elapsed = if (started > 0L) base + ((System.currentTimeMillis() - started) / 1000L) else base
        prefs.edit().putLong(KEY_ELAPSED_BASE, elapsed).putLong(KEY_STARTED_AT, 0L).putBoolean(KEY_PAUSED, true).apply()
        lastAcceptedLocation = null
        fusedLocationClient.removeLocationUpdates(locationCallback)
        updateNotification("Walk paused")
    }

    private fun resumeSession() {
        if (!prefs.getBoolean(KEY_ACTIVE, false)) return
        prefs.edit().putBoolean(KEY_PAUSED, false).putLong(KEY_STARTED_AT, System.currentTimeMillis()).apply()
        lastAcceptedLocation = null
        startAsForeground("Resuming GPS route")
        requestLocationUpdates()
    }

    private fun stopSession() {
        if (prefs.getBoolean(KEY_ACTIVE, false)) pauseSession()
        prefs.edit().putBoolean(KEY_ACTIVE, false).putBoolean(KEY_PAUSED, false).apply()
        if (::fusedLocationClient.isInitialized) fusedLocationClient.removeLocationUpdates(locationCallback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE)
        else stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Walking GPS tracker", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun startAsForeground(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Nutriglass AI · Walking tracker")
            .setContentText(message)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Nutriglass AI · Walking tracker")
            .setContentText(message)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        if (::fusedLocationClient.isInitialized) fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.service.LOCATION_START"
        const val ACTION_PAUSE = "com.example.service.LOCATION_PAUSE"
        const val ACTION_RESUME = "com.example.service.LOCATION_RESUME"
        const val ACTION_STOP = "com.example.service.LOCATION_STOP"
        const val EXTRA_RESET = "reset_session"
        const val PREFS = "nutriglass_live_gps"
        const val KEY_DISTANCE = "distance_meters"
        const val KEY_ROUTE = "route_json"
        const val KEY_ACTIVE = "active"
        const val KEY_PAUSED = "paused"
        const val KEY_ELAPSED_BASE = "elapsed_base_seconds"
        const val KEY_STARTED_AT = "started_at_millis"
        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIFICATION_ID = 4102
    }
}
