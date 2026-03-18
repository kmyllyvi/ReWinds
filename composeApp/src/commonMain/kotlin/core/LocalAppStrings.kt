package core

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal that provides the current AppStrings instance.
 * Defaults to English. Wrap your app content with CompositionLocalProvider to supply strings.
 */
val LocalAppStrings = compositionLocalOf { AppStrings.English }
