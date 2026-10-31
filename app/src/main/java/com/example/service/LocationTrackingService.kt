package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.data.local.AppDatabase
import com.example.data.repository.HealthRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val repository = HealthRepository(AppDatabase.getDatabase(this))

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // TODO: Implement actual tracking and accuracy filtering
                    // repository.logLocation(location)
                }
            }
        }
        startForegroundService()
    }

    private fun startForegroundService() {
        val channelId = "tracking_channel"
        val channel = NotificationChannel(channelId, "Tracking", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("NutriGlass Tracking")
            .setContentText("Recording activity...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
            
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, android.os.Looper.getMainLooper())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
