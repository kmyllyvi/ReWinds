package onboarding

import androidx.lifecycle.ViewModel
import core.AppSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the "has the user seen the welcome guide" flag and the first-run visibility of the
 * welcome screen (KIM-334).
 *
 * On first-ever launch [showFirstRunWelcome] is `true`, so [core.Navigation] shows the welcome
 * screen ahead of the existing VC-key gate. Dismissing it ([onFirstRunDismissed]) persists the
 * seen flag under [SEEN_KEY] and clears the flag for this session — the gate then proceeds as
 * usual. Subsequent launches read the persisted flag and skip the screen.
 *
 * The same content is revisitable from Settings; that path does NOT go through this ViewModel's
 * first-run flag (it navigates to a route), so re-opening the guide never re-triggers first-run
 * logic. All logic lives here; the View only collects state and calls methods (MV*).
 */
class WelcomeViewModel(
    private val settingsRepo: AppSettingsStore
) : ViewModel() {

    private val _showFirstRunWelcome = MutableStateFlow(!hasSeenWelcome())
    /** True while the first-run welcome screen should block the launch flow. */
    val showFirstRunWelcome: StateFlow<Boolean> = _showFirstRunWelcome.asStateFlow()

    /** "Get Started" on the first-run screen: persist the seen flag and dismiss. */
    fun onFirstRunDismissed() {
        settingsRepo.setString(SEEN_KEY, "true")
        _showFirstRunWelcome.value = false
    }

    private fun hasSeenWelcome(): Boolean = settingsRepo.getString(SEEN_KEY) == "true"

    companion object {
        const val SEEN_KEY = "has_seen_onboarding_v1"
    }
}
