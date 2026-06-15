package uitest

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.ChatMessage
import ai.ChatView
import ai.ChatViewModel
import ai.MessageRole
import core.ApiKeyChecker
import core.AppStrings
import core.FakeNavigator
import core.NavigationCall
import core.TestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.theme.ReWindsTheme

/**
 * Compose semantic UI tests for the Chat journeys (KIM-293):
 *  - J6: chat send / receive
 *  - J7: chat blocked when no Anthropic key is configured
 *  - J8: switch chat sessions
 *
 * Each drives the real [ChatView] with a [ChatViewModel] built from in-memory fakes — a canned
 * [FakeAiConversationRepository] (no Anthropic network) and a seedable [FakeChatRepository]. The
 * key gate is the injectable [ApiKeyChecker], so the J7 "key missing" branch is deterministic and
 * independent of the developer's local key configuration.
 */
@RunWith(AndroidJUnit4::class)
class ChatJourneyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val strings = AppStrings.English

    private fun chatViewModel(
        chatRepo: FakeChatRepository = FakeChatRepository(),
        keyConfigured: Boolean = true,
        cannedReply: String = "It will be windy in Helsinki."
    ): ChatViewModel = ChatViewModel(
        aiRepository = FakeAiConversationRepository(cannedReply),
        weatherRepository = FakeWeatherRepository(),
        chatRepository = chatRepo,
        apiKeyChecker = ApiKeyChecker { keyConfigured }
    )

    @Test
    fun j6_sendingMessage_showsUserThenAssistantBubble() {
        val vm = chatViewModel(keyConfigured = true, cannedReply = "It will be windy in Helsinki.")

        composeTestRule.setContent {
            ReWindsTheme { ChatView(vm = vm, navigator = FakeNavigator()) }
        }

        composeTestRule.onNodeWithTag(TestTags.CHAT_INPUT_FIELD)
            .performTextInput("What's the wind forecast for Helsinki?")
        composeTestRule.onNodeWithTag(TestTags.CHAT_SEND_BUTTON).performClick()

        // User bubble appears immediately; the assistant's canned reply follows asynchronously.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("What's the wind forecast for Helsinki?")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("It will be windy in Helsinki.")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("What's the wind forecast for Helsinki?").assertIsDisplayed()
        composeTestRule.onNodeWithText("It will be windy in Helsinki.").assertIsDisplayed()
    }

    @Test
    fun j7_sendingWithoutKey_showsDialog_andGoToSettingsNavigates() {
        val vm = chatViewModel(keyConfigured = false)
        val navigator = FakeNavigator()

        composeTestRule.setContent {
            ReWindsTheme { ChatView(vm = vm, navigator = navigator) }
        }

        composeTestRule.onNodeWithTag(TestTags.CHAT_INPUT_FIELD)
            .performTextInput("Will it rain tomorrow?")
        composeTestRule.onNodeWithTag(TestTags.CHAT_SEND_BUTTON).performClick()

        // The "API Key Not Configured" dialog blocks the send.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(strings.apiKeyNotConfigured)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(strings.apiKeyNotConfigured).assertIsDisplayed()

        // Tapping "Go to Settings" dismisses the dialog and routes to Settings.
        composeTestRule.onNodeWithText(strings.goToSettings).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { navigator.calls.isNotEmpty() }
        composeTestRule.runOnIdle {
            assert(navigator.lastCall == NavigationCall.Settings) {
                "Expected navigation to Settings but was ${navigator.lastCall}"
            }
        }
    }

    @Test
    fun j8_switchingSession_loadsSelectedSessionMessages() {
        // Two seeded sessions with distinct messages. Session 2 is the most recent → active on load.
        val chatRepo = FakeChatRepository().apply {
            seed(
                id = 1L,
                title = "Helsinki winds",
                messages = listOf(ChatMessage(role = MessageRole.ASSISTANT, content = "Session one greeting")),
                ts = 100L
            )
            seed(
                id = 2L,
                title = "Tarifa kite report",
                messages = listOf(ChatMessage(role = MessageRole.ASSISTANT, content = "Session two greeting")),
                ts = 200L
            )
        }
        val vm = chatViewModel(chatRepo = chatRepo)

        composeTestRule.setContent {
            ReWindsTheme { ChatView(vm = vm, navigator = FakeNavigator()) }
        }

        // The active (most recent) session 2's message is shown first.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Session two greeting")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Open the switcher and pick the non-active session (session 1).
        composeTestRule.onNodeWithTag(TestTags.CHAT_SESSION_SWITCHER_BUTTON).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(TestTags.CHAT_SESSION_LIST_ITEM)
                .fetchSemanticsNodes().size >= 2
        }
        composeTestRule.onNodeWithText("Helsinki winds").performClick()

        // The message list now shows session 1's message instead of session 2's.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Session one greeting")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Session one greeting").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Session two greeting").assertCountEquals(0)
    }
}
