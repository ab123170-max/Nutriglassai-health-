package com.example.ui.components

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import org.json.JSONArray

data class GpsCoordinate(val lat: Double, val lon: Double)

/** OpenStreetMap route preview. Does not require a Google Maps API key. */
@Composable
fun GpsMapView(
    coordinates: List<GpsCoordinate>,
    currentLat: Double? = null,
    currentLon: Double? = null,
    modifier: Modifier = Modifier
) {
    val points = JSONArray().apply {
        coordinates.forEach { put(JSONArray().put(it.lat).put(it.lon)) }
    }
    val centerLat = currentLat ?: coordinates.lastOrNull()?.lat ?: 28.1296
    val centerLon = currentLon ?: coordinates.lastOrNull()?.lon ?: 82.2964
    val html = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <style>html,body,#map{height:100%;width:100%;margin:0;padding:0}</style></head>
        <body><div id="map"></div><script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <script>
          const map=L.map('map').setView([$centerLat,$centerLon],16);
          L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{
            maxZoom:19, attribution:'&copy; OpenStreetMap contributors'
          }).addTo(map);
          let routeLayer=null, markers=[];
          window.updateRoute=function(pts){
            if(routeLayer){map.removeLayer(routeLayer);routeLayer=null;}
            markers.forEach(m=>map.removeLayer(m)); markers=[];
            if(!pts || !pts.length) return;
            routeLayer=L.polyline(pts,{color:'#1473EA',weight:5}).addTo(map);
            markers.push(L.circleMarker(pts[0],{radius:6,color:'#16A34A'}).addTo(map).bindPopup('Start'));
            markers.push(L.circleMarker(pts[pts.length-1],{radius:7,color:'#1473EA'}).addTo(map).bindPopup('Latest GPS fix'));
            if(pts.length>1) map.fitBounds(routeLayer.getBounds().pad(0.18));
          };
          window.updateRoute($points);
        </script></body></html>
    """.trimIndent()
    AndroidView(
        modifier = modifier.fillMaxWidth().height(270.dp).clip(RoundedCornerShape(16.dp)),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.evaluateJavascript("window.updateRoute && window.updateRoute($points);", null)
        }
    )
}
