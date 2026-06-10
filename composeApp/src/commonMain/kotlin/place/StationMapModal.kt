package place

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import core.LocalAppStrings
import core.Log
import ui.theme.rewinds

/**
 * Dark-themed station map presented as a bottom-sheet modal.
 *
 * The sheet covers ~88 % of the screen with the host tab bar still visible (dimmed) behind the
 * backdrop. The handle row is the only top chrome — there is no nav-header. Markers and tiles are
 * rendered inside a Leaflet WebView; all Compose chrome (vignette, info panel) sits above it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationMapModal(
    lat: Double,
    lon: Double,
    placeName: String,
    stations: List<StationDisplayData>,
    summary: StationMapSummary,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val htmlContent = generateMapHtml(lat, lon, placeName, stations, strings.noStationData, strings.unknownStation)
    val safeHtmlUri = createSafeDataUri(htmlContent)
    val webViewState = rememberWebViewState(url = safeHtmlUri)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        Log.d("=== STATION MAP MODAL OPENED ===")
        Log.d("Place: $placeName, stations: ${stations.size}")
        Log.d("Latitude: $lat, Longitude: $lon")
        Log.d("=====================================")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.rewinds.surface,
        // The handle row provides our own chrome, so suppress the default M3 drag handle.
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.88f)) {
            SheetHandleRow(onClose = onDismiss, closeDesc = strings.closeMap)
            SheetTitleRow(placeName = placeName, summary = summary)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                WebView(
                    state = webViewState,
                    modifier = Modifier.fillMaxSize()
                )
                TileVignette()
                StationInfoPanel(
                    summary = summary,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/** Top chrome of the sheet: centred drag handle with a close button at the trailing edge. */
@Composable
private fun SheetHandleRow(onClose: () -> Unit, closeDesc: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.rewinds.accentBlue.copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.rewinds.accentBlue.copy(alpha = 0.08f))
                .border(1.dp, MaterialTheme.rewinds.accentBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = closeDesc,
                tint = MaterialTheme.rewinds.textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** Place name plus the "N weather stations · closest X km" subtitle. */
@Composable
private fun SheetTitleRow(placeName: String, summary: StationMapSummary) {
    val strings = LocalAppStrings.current
    val subtitle = summary.closestDistanceKm
        ?.let { strings.mapSheetSubtitle(summary.stationCount, it) }
        ?: strings.mapSheetSubtitleNoDistance(summary.stationCount)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = placeName,
            color = MaterialTheme.rewinds.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = MaterialTheme.rewinds.textMuted,
            fontSize = 10.sp
        )
    }
}

/**
 * Four-edge pageBg → transparent gradient over the tile area. Anchors the info panel and darkens
 * the tile edges. Non-interactive so map gestures pass through.
 */
@Composable
private fun TileVignette() {
    val edge = MaterialTheme.rewinds.pageBg
    Box(modifier = Modifier.fillMaxSize()) {
        // Top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.18f)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(edge.copy(alpha = 0.72f), edge.copy(alpha = 0f))))
        )
        // Bottom — deeper to seat the info panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.22f)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(edge.copy(alpha = 0f), edge.copy(alpha = 0.82f))))
        )
        // Left
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.14f)
                .align(Alignment.CenterStart)
                .background(Brush.horizontalGradient(listOf(edge.copy(alpha = 0.55f), edge.copy(alpha = 0f))))
        )
        // Right
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.14f)
                .align(Alignment.CenterEnd)
                .background(Brush.horizontalGradient(listOf(edge.copy(alpha = 0f), edge.copy(alpha = 0.55f))))
        )
    }
}

/**
 * Floating panel above the tiles showing station count and closest distance.
 * Solid surface fill (never semi-transparent) so it stays legible over any map content.
 */
@Composable
private fun StationInfoPanel(summary: StationMapSummary, modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.rewinds.surface)
            .border(1.dp, MaterialTheme.rewinds.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.rewinds.accentBlue.copy(alpha = 0.10f))
                .border(1.dp, MaterialTheme.rewinds.accentBlue.copy(alpha = 0.20f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = strings.mapStationIconDesc,
                tint = MaterialTheme.rewinds.accentBlue,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (summary.stationCount > 0) {
                Text(
                    text = strings.stationsNearby(summary.stationCount),
                    color = MaterialTheme.rewinds.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                summary.closestDistanceKm?.let { km ->
                    Text(
                        text = strings.closestStationAway(km),
                        color = MaterialTheme.rewinds.textSecondary,
                        fontSize = 11.sp
                    )
                }
            } else {
                Text(
                    text = strings.noStationsNearby,
                    color = MaterialTheme.rewinds.textSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Generates the Leaflet map HTML with:
 *  - CartoDB Dark Matter raster tiles (dark schematic style)
 *  - An accent-blue teardrop pin at the saved-place centre
 *  - Lower-opacity teardrop pins for each nearby weather station
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
    val placeMarkerSvg = teardropSvg(fill = PLACE_PIN_FILL, stroke = MARKER_STROKE, width = 28, height = 36)

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
                body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; background: $PAGE_BG_HEX; }
                .leaflet-container { background: $PAGE_BG_HEX; }
                .leaflet-tile { filter: brightness(1.55) saturate(0.9); }
                #no-data-overlay {
                    position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
                    background: rgba(13,27,46,0.92); border: 1px solid $BORDER_HEX; border-radius: 8px;
                    padding: 12px 20px; font-size: 14px; color: $TEXT_SECONDARY_HEX;
                    pointer-events: none; z-index: 1000; white-space: nowrap;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                (function() {
                    var map = L.map('map', { zoomControl: false, attributionControl: false }).setView([$lat, $lon], 13);
                    L.tileLayer('https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                        attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors © <a href="https://carto.com/attributions">CARTO</a>',
                        maxZoom: 20
                    }).addTo(map);

                    var placeIcon = L.icon({
                        iconUrl: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent($placeMarkerSvg),
                        iconSize: [28, 36],
                        iconAnchor: [14, 36],
                        popupAnchor: [0, -30]
                    });
                    L.marker([$lat, $lon], { icon: placeIcon }).addTo(map);

                    $stationMarkersJs
                    $noDataOverlay
                })();
            </script>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Builds JS that adds one lower-opacity teardrop marker per station. Each marker is an inline SVG
 * data-URL icon so no external assets are needed.
 */
private fun buildStationMarkersJs(stations: List<StationDisplayData>, unknownStationText: String): String {
    if (stations.isEmpty()) return ""

    val stationSvg = teardropSvg(fill = STATION_PIN_FILL, stroke = MARKER_STROKE, width = 20, height = 26)
    val iconJs = """
        var stationIcon = L.icon({
            iconUrl: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent($stationSvg),
            iconSize: [20, 26],
            iconAnchor: [10, 26],
            popupAnchor: [0, -22]
        });
    """.trimIndent()

    val markerStatements = stations.joinToString("\n") { station ->
        val displayName = station.name?.replace("'", "\\'") ?: unknownStationText
        "L.marker([${station.latitude}, ${station.longitude}], { icon: stationIcon }).addTo(map).bindPopup('$displayName');"
    }

    return "$iconJs\n$markerStatements"
}

/**
 * Returns a single-quoted JS string literal containing a teardrop marker SVG
 * (circular head + pointed tail) with the given fill and stroke.
 */
private fun teardropSvg(fill: String, stroke: String, width: Int, height: Int): String {
    val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$width\" height=\"$height\" viewBox=\"0 0 28 36\">" +
        "<path d=\"M14 1C7.4 1 2 6.4 2 13c0 9 12 22 12 22s12-13 12-22C26 6.4 20.6 1 14 1z\" " +
        "fill=\"$fill\" stroke=\"$stroke\" stroke-width=\"2\"/>" +
        "</svg>"
    return "'$svg'"
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

// Marker colours mirror the Midnight Blue tokens (kept as hex strings for inline SVG).
private const val PLACE_PIN_FILL = "#8ECFF0"      // accentBlue
private const val STATION_PIN_FILL = "rgba(122,184,216,0.55)"  // textSecondary @ ~55 %
private const val MARKER_STROKE = "#030810"       // pageBg
private const val PAGE_BG_HEX = "#030810"
private const val BORDER_HEX = "#1A3050"
private const val TEXT_SECONDARY_HEX = "#7AB8D8"

// Create a data URI that works on iOS by using base64 encoding
private fun createSafeDataUri(html: String): String {
    val base64Html = htmlToBase64(html)
    return "data:text/html;base64,$base64Html"
}

// Simple base64 encoder using only standard characters.
// Tracks bytes-read-per-triplet explicitly to avoid off-by-one padding errors
// that would corrupt the trailing byte(s) when bytes.size % 3 != 2.
private fun htmlToBase64(html: String): String {
    val base64Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val bytes = html.map { it.code.toByte() }.toByteArray()
    val result = StringBuilder()
    var i = 0

    while (i < bytes.size) {
        val b1 = bytes[i++].toInt() and 0xFF
        val hasB2 = i < bytes.size
        val b2 = if (hasB2) bytes[i++].toInt() and 0xFF else 0
        val hasB3 = i < bytes.size
        val b3 = if (hasB3) bytes[i++].toInt() and 0xFF else 0

        val c1 = b1 shr 2
        val c2 = ((b1 and 0x3) shl 4) or (b2 shr 4)
        val c3 = ((b2 and 0xF) shl 2) or (b3 shr 6)
        val c4 = b3 and 0x3F

        result.append(base64Alphabet[c1])
        result.append(base64Alphabet[c2])
        result.append(if (hasB2) base64Alphabet[c3] else '=')
        result.append(if (hasB3) base64Alphabet[c4] else '=')
    }

    return result.toString()
}
