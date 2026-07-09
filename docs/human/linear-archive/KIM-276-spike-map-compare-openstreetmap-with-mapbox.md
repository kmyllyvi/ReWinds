# KIM-276: Spike: Map. compare OpenStreetMap with mapbox

**Status:** Done · **Priority:** Low · **Labels:** _none_
**Created:** 2026-06-06T11:17:24.449Z · **Completed:** 2026-06-10T06:17:07.189Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-276/spike-map-compare-openstreetmap-with-mapbox

## Description

nothing specific against OSM but just to check if mapbox offers better UX or something

[https://www.mapbox.com/](<https://www.mapbox.com/>)

---

## Spike Findings

**Current state**: The app already uses OSM — specifically Leaflet.js embedded in a `compose-webview-multiplatform` WebView, with CartoDB Dark Matter raster tiles. Custom SVG teardrop markers for the saved place and nearby weather stations are already working. This is the OSM side of the comparison.

**Mapbox assessment**: Mapbox has no first-class Compose Multiplatform SDK. On both iOS and Android it would require either platform-specific native SDK wrappers (considerable glue code in iosMain/androidMain) or the same WebView approach already in use — just with Mapbox GL JS instead of Leaflet. The visual quality of Mapbox vector tiles is genuinely better than CartoDB raster tiles (smoother at all zoom levels, crisper labels), and Mapbox's dark style ("Mapbox Dark") is polished. However, it requires a developer-side Mapbox access token baked into the app, which adds a registration dependency even before users bring their own keys. The free tier (50k mobile map loads/month) is adequate at MVP scale but introduces ongoing account management that OSM avoids entirely.

**Recommendation**: Stay with the current OSM/Leaflet/WebView approach for the MVP milestone. The existing CartoDB Dark Matter tiles match the app's Midnight Blue design theme well, require no token or registration, and are already working on both platforms. The WebView abstraction means both platforms use identical code with zero native SDK overhead. The one meaningful gap Mapbox would close — vector tile crispness — is not worth the added dependency and token management at this stage. If map UX becomes a differentiator post-MVP (e.g. offline maps, custom wind overlay layers, richer interactivity), revisit MapLibre GL JS as the OSM-based vector alternative to Mapbox before considering Mapbox itself.

## Comments

_No comments._
