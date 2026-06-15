package ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import components.AppHeader
import core.TestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented layout test for KIM-127:
 * Verifies that chat messages are not obscured by the chat header.
 *
 * The bug: AppHeader was overlaid via Box, causing messages to scroll under it.
 * The fix: AppHeader is now the first item in a Column, so messages start below it.
 *
 * KIM-293: migrated from text selectors (`onNodeWithText`) to [TestTags] constants
 * (`onNodeWithTag`). Kept as the reference example for the Compose-UI-test pattern —
 * see `composeApp/src/androidInstrumentedTest/README.md`.
 */
@RunWith(AndroidJUnit4::class)
class ChatLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatHeader_isDisplayedAboveMessages() {
        composeTestRule.setContent {
            // Mirrors the fixed layout: AppHeader first in Column, then messages
            Column(modifier = Modifier.fillMaxSize()) {
                AppHeader(
                    title = "Chat",
                    onBackClick = {}
                )
                ChatMessageBubble(
                    message = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = "Let's talk about the weather!"
                    )
                )
            }
        }

        // Both nodes are visible, selected by stable testTags rather than display text.
        composeTestRule.onNodeWithTag(TestTags.APP_HEADER_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.CHAT_MESSAGE_BUBBLE).assertIsDisplayed()

        // Header top-y must be above message top-y (no overlap)
        val headerBounds = composeTestRule.onNodeWithTag(TestTags.APP_HEADER_TITLE).getUnclippedBoundsInRoot()
        val messageBounds = composeTestRule.onNodeWithTag(TestTags.CHAT_MESSAGE_BUBBLE).getUnclippedBoundsInRoot()

        assert(headerBounds.top < messageBounds.top) {
            "Header top (${headerBounds.top}) should be above message top (${messageBounds.top})"
        }
        assert(headerBounds.bottom <= messageBounds.top) {
            "Header bottom (${headerBounds.bottom}) overlaps message top (${messageBounds.top}) — messages are under the header"
        }
    }

    @Test
    fun chatHeader_isFullyVisibleAndNotClipped() {
        composeTestRule.setContent {
            Column(modifier = Modifier.fillMaxSize()) {
                AppHeader(title = "Chat", onBackClick = {})
                ChatMessageBubble(
                    message = ChatMessage(role = MessageRole.ASSISTANT, content = "Hello")
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.APP_HEADER_TITLE).assertIsDisplayed()

        val bounds = composeTestRule.onNodeWithTag(TestTags.APP_HEADER_TITLE).getUnclippedBoundsInRoot()
        assert(bounds.top.value >= 0f) {
            "Header should not be positioned off-screen (top=${bounds.top})"
        }
    }
}
