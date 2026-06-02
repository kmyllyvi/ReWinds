package place

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import core.LocalAppStrings
import core.Log

@Composable
fun StationMapModal(
    lat: Double,
    lon: Double,
    placeName: String,
    stations: List<StationDisplayData>,
    isRefreshingStations: Boolean,
    stationsError: String?,
    onRefreshStations: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val htmlContent = generateMapHtml(lat, lon, placeName, stations, strings.noStationData, strings.unknownStation)
    val safeHtmlUri = createSafeDataUri(htmlContent)
    val webViewState = rememberWebViewState(url = safeHtmlUri)

    LaunchedEffect(Unit) {
        Log.d("=== STATION MAP MODAL OPENED ===")
        Log.d("Place: $placeName, stations: ${stations.size}")
        Log.d("Latitude: $lat, Longitude: $lon")
        Log.d("=====================================")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Error banner — shown when a refresh returned an error; keeps the map markers intact
            if (stationsError != null) {
                Text(
                    text = strings.stationRefreshError(stationsError),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                WebView(
                    state = webViewState,
                    modifier = Modifier.fillMaxSize()
                )

                // Refresh button — top-start corner so it doesn't overlap the close button
                IconButton(
                    onClick = onRefreshStations,
                    enabled = !isRefreshingStations,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                ) {
                    if (isRefreshingStations) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSecondary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.padding(4.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = strings.refreshStationsDesc,
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }

                // Close button — top-end corner
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * Generates the Leaflet map HTML with:
 *  - A blue default marker at the saved-place centre
 *  - Orange markers for each weather station (visually distinct from the place marker)
 *  - A "no station data" note overlaid on the map when the station list is empty
 */
private fun generateMapHtml(
    lat: Double,
    lon: Double,
    placeName: String,
    stations: List<StationDisplayData>,
    noStationDataText: String,
    unknownStationText: String
): String {
    val stationMarkersJs = buildStationMarkersJs(stations, unknownStationText)
    val noDataOverlay = if (stations.isEmpty()) buildNoDataOverlayJs(noStationDataText) else ""

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                * { margin: 0; padding: 0; }
                html, body, #map { width: 100%; height: 100%; }
                body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; }
                #no-data-overlay {
                    position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
                    background: rgba(255,255,255,0.85); border-radius: 8px;
                    padding: 12px 20px; font-size: 14px; color: #555;
                    pointer-events: none; z-index: 1000; white-space: nowrap;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                (function() {
                    var map = L.map('map').setView([$lat, $lon], 13);
                    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '© OpenStreetMap',
                        maxZoom: 19
                    }).addTo(map);

                    // Saved-place centre: default blue marker
                    L.marker([$lat, $lon]).addTo(map).bindPopup('$placeName');

                    $stationMarkersJs
                    $noDataOverlay
                })();
            </script>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Builds JS that adds one orange marker per station.
 * Orange is achieved via a small inline SVG data-URL icon so we need no external assets.
 */
private fun buildStationMarkersJs(stations: List<StationDisplayData>, unknownStationText: String): String {
    if (stations.isEmpty()) return ""

    // Orange marker SVG (matches Leaflet's default shape/size, distinct colour)
    val orangeIconJs = """
        var orangeIcon = L.icon({
            iconUrl: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
                '<svg xmlns="http://www.w3.org/2000/svg" width="25" height="41" viewBox="0 0 25 41">' +
                '<path d="M12.5 0C5.596 0 0 5.596 0 12.5c0 9.375 12.5 28.5 12.5 28.5S25 21.875 25 12.5C25 5.596 19.404 0 12.5 0z" fill="#FF7A00" stroke="#CC5500" stroke-width="1"/>' +
                '<circle cx="12.5" cy="12.5" r="5" fill="white"/>' +
                '</svg>'
            ),
            iconSize: [25, 41],
            iconAnchor: [12, 41],
            popupAnchor: [1, -34]
        });
    """.trimIndent()

    val markerStatements = stations.joinToString("\n") { station ->
        val displayName = station.name?.replace("'", "\\'") ?: unknownStationText
        "L.marker([${station.latitude}, ${station.longitude}], { icon: orangeIcon }).addTo(map).bindPopup('$displayName');"
    }

    return "$orangeIconJs\n$markerStatements"
}

private fun buildNoDataOverlayJs(message: String): String {
    val escaped = message.replace("'", "\\'")
    return """
        var overlay = document.createElement('div');
        overlay.id = 'no-data-overlay';
        overlay.textContent = '$escaped';
        document.getElementById('map').appendChild(overlay);
    """.trimIndent()
}

// Create a data URI that works on iOS by using base64 encoding
private fun createSafeDataUri(html: String): String {
    val base64Html = htmlToBase64(html)
    return "data:text/html;base64,$base64Html"
}

// Simple base64 encoder using only standard characters
private fun htmlToBase64(html: String): String {
    val base64Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val bytes = html.map { it.code.toByte() }.toByteArray()
    val result = StringBuilder()
    var i = 0

    while (i < bytes.size) {
        val b1 = bytes[i++].toInt() and 0xFF
        val b2 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else 0
        val b3 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else 0

        val hasSecond = i - 1 < bytes.size
        val hasThird = i < bytes.size

        val c1 = b1 shr 2
        val c2 = ((b1 and 0x3) shl 4) or (b2 shr 4)
        val c3 = ((b2 and 0xF) shl 2) or (b3 shr 6)
        val c4 = b3 and 0x3F

        result.append(base64Alphabet[c1])
        result.append(base64Alphabet[c2])
        result.append(if (hasSecond) base64Alphabet[c3] else '=')
        result.append(if (hasThird) base64Alphabet[c4] else '=')
    }

    return result.toString()
}
