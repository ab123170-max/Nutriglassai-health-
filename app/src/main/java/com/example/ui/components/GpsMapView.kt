package com.example.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.HealthNavy

data class GpsCoordinate(val lat: Double, val lon: Double)

@Composable
fun GpsMapView(
    coordinates: List<GpsCoordinate>,
    currentLat: Double? = null,
    currentLon: Double? = null,
    modifier: Modifier = Modifier
) {
    val centerLat = currentLat ?: coordinates.lastOrNull()?.lat ?: 28.1296 // Default Tulsipur/Nepal center
    val centerLon = currentLon ?: coordinates.lastOrNull()?.lon ?: 82.2964

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // CartoDB Voyager / OpenStreetMap Interactive WebView
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadDataWithBaseURL(
                            "https://unpkg.com",
                            generateCartoDbLeafletHtml(centerLat, centerLon, coordinates),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL(
                        "https://unpkg.com",
                        generateCartoDbLeafletHtml(centerLat, centerLon, coordinates),
                        "text/html",
                        "UTF-8",
                        null
                    )
                },
                modifier = Modifier.fillMaxSize()
            )

            // Map Status Overlay Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = HealthNavy.copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(CalorieGreen, shape = RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (coordinates.isNotEmpty()) "GPS Path Active (${coordinates.size} pts)" else "Live Route Map",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun generateCartoDbLeafletHtml(
    centerLat: Double,
    centerLon: Double,
    coordinates: List<GpsCoordinate>
): String {
    val polylineJson = coordinates.joinToString(",", "[", "]") { "[${it.lat}, ${it.lon}]" }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body, html, #workoutMap { margin: 0; padding: 0; width: 100%; height: 100%; background: #f0f2f5; }
                .leaflet-control-attribution { display: none !important; }
            </style>
        </head>
        <body>
            <div id="workoutMap" style="width: 100%; height: 100%; border-radius: 12px;"></div>
            <script>
                var map = L.map('workoutMap', { zoomControl: false }).setView([$centerLat, $centerLon], 16);

                L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                    maxZoom: 19,
                    attribution: '&copy; OpenStreetMap &copy; CARTO'
                }).addTo(map);

                var coords = $polylineJson;
                if (coords.length > 0) {
                    var polyline = L.polyline(coords, { color: '#10b981', weight: 5 }).addTo(map);
                    map.fitBounds(polyline.getBounds(), { padding: [25, 25] });
                }

                var userMarker = L.marker([$centerLat, $centerLon]).addTo(map);
                userMarker.bindPopup("You are here").openPopup();
            </script>
        </body>
        </html>
    """.trimIndent()
}
