package components

import androidx.compose.ui.graphics.Color
import ui.theme.ReWindsColors

// Month status indicator colours used by CalendarSelectors.
// Mapped to palette-coherent values; actual meaning (loaded/partial/missing)
// stays the same — only the hue is updated to fit the Midnight Blue theme.
val monthFullyLoadedColor     = ReWindsColors.accentBlue       // fully loaded
val monthPartiallyLoadedColor = ReWindsColors.attention        // partially loaded
val monthNotLoadedColor       = ReWindsColors.textTertiary     // no data
