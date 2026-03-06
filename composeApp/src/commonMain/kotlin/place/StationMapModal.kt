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
    
    // Use simple URL encoding for HTML content (works cross-platform)
    val htmlUri = "data:text/html," + htmlContent.urlEncode()
    
    // Create WebView state with the data URL
    val webViewState = rememberWebViewState(url = htmlUri)
    
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
    return """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width, initial-scale=1.0"><link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/><script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"><\/script><style>body,html,#map{margin:0;padding:0;height:100%;width:100%;}<\/style><\/head><body><div id="map"><\/div><script>var map = L.map('map').setView([$lat, $lon], 13);L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {attribution: '© OpenStreetMap contributors',maxZoom: 19}).addTo(map);L.marker([$lat, $lon]).addTo(map).bindPopup('$placeName').openPopup();<\/script><\/body><\/html>"""
}

// Simple URL encoding that works cross-platform
private fun String.urlEncode(): String {
    val chars = StringBuilder()
    for (c in this) {
        when {
            c == ' ' -> chars.append("%20")
            c == '"' -> chars.append("%22")
            c == '\'' -> chars.append("%27")
            c == '\n' -> chars.append("%0A")
            c == '\r' -> chars.append("%0D")
            c == '\t' -> chars.append("%09")
            c == '(' -> chars.append("%28")
            c == ')' -> chars.append("%29")
            c == '<' -> chars.append("%3C")
            c == '>' -> chars.append("%3E")
            c == '#' -> chars.append("%23")
            c == '$' -> chars.append("%24")
            c == '%' -> chars.append("%25")
            c == '&' -> chars.append("%26")
            c == '=' -> chars.append("%3D")
            c == '?' -> chars.append("%3F")
            c == '@' -> chars.append("%40")
            c == '{' -> chars.append("%7B")
            c == '}' -> chars.append("%7D")
            c == '[' -> chars.append("%5B")
            c == ']' -> chars.append("%5D")
            c == '/' -> chars.append("%2F")
            c == ':' -> chars.append("%3A")
            c == ';' -> chars.append("%3B")
            c == ',' -> chars.append("%2C")
            c == '+' -> chars.append("%2B")
            c == '\\' -> chars.append("%5C")
            c == '`' -> chars.append("%60")
            c == '~' -> chars.append("%7E")
            c == '^' -> chars.append("%5E")
            else -> chars.append(c)
        }
    }
    return chars.toString()
}
