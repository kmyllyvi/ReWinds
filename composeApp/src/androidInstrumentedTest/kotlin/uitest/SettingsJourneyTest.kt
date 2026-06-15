package uitest

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.AnthropicClient
import core.ApiKeyChecker
import core.ApiKeyManager
import core.AppSettingsStore
import core.AppStrings
import core.FakeNavigator
import core.TestTags
import core.WeatherApiKeyManager
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import settings.SettingsViewModel
import settings.SettingsView
import ui.theme.ReWindsTheme

/**
 * Compose semantic UI test for the Settings journey J10 (KIM-293):
 * configure the Anthropic API key and see the row's status chip flip to "Configured".
 *
 * The "Configured" state is driven by [SettingsViewModel.anthropicKeyConfigured], which reads the
 * injectable [ApiKeyChecker]. Here the checker reflects [ApiKeyManager] — the same in-memory store
 * the Save button writes to — so the journey is deterministic and independent of the platform
 * key store / `BuildConfig`.
 */
@RunWith(AndroidJUnit4::class)
class SettingsJourneyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val strings = AppStrings.English

    /** Minimal in-memory settings store; J10 touches no persisted setting. */
    private class FakeSettingsStore : AppSettingsStore {
        private val map = mutableMapOf<String, String>()
        override fun getString(key: String): String? = map[key]
        override fun setString(key: String, value: String) { map[key] = value }
    }

    // Both key managers are global singletons; reset them so the row chips start from a known
    // state and the test is order-independent.
    @Before
    fun clearKeys() {
        ApiKeyManager.setApiKey("")
        WeatherApiKeyManager.setApiKey("")
    }

    @After
    fun resetKeys() {
        ApiKeyManager.setApiKey("")
        WeatherApiKeyManager.setApiKey("")
    }

    @Test
    fun j10_savingAnthropicKey_flipsRowChipToConfigured() {
        val vm = SettingsViewModel(
            settingsRepo = FakeSettingsStore(),
            anthropicClient = AnthropicClient(apiKey = "test", enableLogs = false),
            // Reflects the in-memory store the ApiKeyDialog writes to on Save.
            apiKeyChecker = ApiKeyChecker { ApiKeyManager.hasValidKey() }
        )

        composeTestRule.setContent {
            ReWindsTheme { SettingsView(navigator = FakeNavigator(), vm = vm) }
        }

        // Matchers scoped to the Anthropic row, so the assertion is unaffected by the sibling
        // Visual Crossing key row's chip. The row is a clickable node that merges its descendants'
        // text, so the chip label is matched as a substring of the row node's own text.
        val anthropicRowNotSet = hasTestTag(TestTags.SETTINGS_ANTHROPIC_KEY_ROW)
            .and(hasText(strings.settingsKeyNotSetChip, substring = true))
        val anthropicRowConfigured = hasTestTag(TestTags.SETTINGS_ANTHROPIC_KEY_ROW)
            .and(hasText(strings.settingsKeyConfiguredChip, substring = true))

        // Initially no key → the Anthropic row shows the "Not set" chip (and not "Configured").
        // assertExists, not assertIsDisplayed: the API Keys group may sit below the fold on first
        // render; J10 verifies the chip *state*, not its scroll position.
        composeTestRule.onNode(anthropicRowNotSet).assertExists()
        composeTestRule.onAllNodes(anthropicRowConfigured).assertCountEquals(0)

        // Open the Anthropic key dialog, enter a valid key, and save.
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_ANTHROPIC_KEY_ROW).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(TestTags.SETTINGS_API_KEY_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_API_KEY_FIELD)
            .performTextInput("sk-ant-test-valid-key")
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_API_KEY_SAVE_BUTTON).performClick()

        // After save the Anthropic row's chip flips to "Configured" and no longer shows "Not set".
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(anthropicRowConfigured).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(anthropicRowConfigured).assertExists()
        composeTestRule.onAllNodes(anthropicRowNotSet).assertCountEquals(0)
    }
}
