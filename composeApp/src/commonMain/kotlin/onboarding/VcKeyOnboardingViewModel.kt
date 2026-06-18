package onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.WeatherApiKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Gate state for the first-run Visual Crossing key onboarding (KIM-309).
 *
 * Owns the single source of truth for whether a valid VC key is configured by forwarding
 * [WeatherApiKeyManager.hasValidKeyFlow]. While [isWeatherKeyConfigured] is `false` the app must
 * show the blocking onboarding screen and never expose the functional Home/tab surface — see
 * ARCHITECTURE-RULES (all gating logic lives here, the composable only collects state).
 *
 * The gate cannot be bypassed: it clears only when a valid key is saved, and because it observes
 * the key manager reactively it clears in the same session the moment the key is stored.
 * [showKeyEntry] drives the in-gate navigation to the existing Settings VC key entry; it is not a
 * dismiss/skip affordance — there is no path from it back to a functional app state.
 */
class VcKeyOnboardingViewModel : ViewModel() {

    /** True once a valid VC key is present. When false, the hard gate is active. */
    val isWeatherKeyConfigured: StateFlow<Boolean> =
        WeatherApiKeyManager.hasValidKeyFlow.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            WeatherApiKeyManager.hasValidKey()
        )

    private val _showKeyEntry = MutableStateFlow(false)
    /** True while the user is on the Settings key-entry surface reached from the gate CTA. */
    val showKeyEntry: StateFlow<Boolean> = _showKeyEntry.asStateFlow()

    /** "Configure now" CTA — route into the existing Settings VC key entry. */
    fun onConfigureNowClicked() {
        _showKeyEntry.value = true
    }
}
