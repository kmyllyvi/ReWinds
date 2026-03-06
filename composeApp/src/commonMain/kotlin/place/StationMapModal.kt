package place

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState

@Composable
fun StationMapModal(
    lat: Double,
    lon: Double,
    placeName: String,
    onDismiss: () -> Unit
) {
    // Create the HTML for the map
    val htmlContent = generateMapHtml(lat, lon, placeName)
    
    // Create a safe data URI for iOS
    val safeHtmlUri = createSafeDataUri(htmlContent)
    
    // Create WebView state
    val webViewState = rememberWebViewState(url = safeHtmlUri)
    
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // WebView with the map
            WebView(
                state = webViewState,
                modifier = Modifier.fillMaxSize()
            )
            
            // Close button in the top-right corner
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

private fun generateMapHtml(lat: Double, lon: Double, placeName: String): String {
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
                    L.marker([$lat, $lon]).addTo(map).bindPopup('$placeName').openPopup();
                })();
            </script>
        </body>
        </html>
    """.trimIndent()
}

// Create a data URI that works on iOS by using semicolon-separated base64 encoding
private fun createSafeDataUri(html: String): String {
    // For iOS compatibility, use simple base64 encoding without special characters
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
