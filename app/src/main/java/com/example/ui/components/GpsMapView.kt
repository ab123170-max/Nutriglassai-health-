package com.example.ui.components

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.HealthTeal
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

data class GpsCoordinate(val lat: Double, val lon: Double)

/**
 * Native Google Maps surface for the Activity tracker.
 * The route is rendered only from coordinates supplied by the GPS tracker;
 * this composable never invents locations or distance.
 */
@Composable
fun GpsMapView(
    coordinates: List<GpsCoordinate>,
    currentLat: Double? = null,
    currentLon: Double? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasLocationPermission =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val tulsipur = LatLng(28.1296, 82.2964)
    val current = if (currentLat != null && currentLon != null) {
        LatLng(currentLat, currentLon)
    } else {
        coordinates.lastOrNull()?.let { LatLng(it.lat, it.lon) }
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(current ?: tulsipur, 15f)
    }

    LaunchedEffect(currentLat, currentLon) {
        if (current != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(current, 17f),
                durationMs = 650
            )
        }
    }

    val routePoints = coordinates.map { LatLng(it.lat, it.lon) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(270.dp)
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = MapType.NORMAL,
                    isMyLocationEnabled = hasLocationPermission
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = hasLocationPermission,
                    compassEnabled = true,
                    mapToolbarEnabled = false
                )
            ) {
                if (routePoints.size >= 2) {
                    Polyline(
                        points = routePoints,
                        color = HealthTeal,
                        width = 10f,
                        geodesic = true,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap()
                    )
                }

                // Fallback marker when only approximate permission is unavailable
                // to the native My Location layer, while a GPS fix is already known.
                if (!hasLocationPermission && current != null) {
                    Marker(
                        state = MarkerState(position = current),
                        title = "Last GPS position"
                    )
                }
            }
        }
    }
}
