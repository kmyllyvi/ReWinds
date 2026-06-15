package ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import core.LocalAppStrings
import core.TestTags
import kotlin.time.Clock
import ui.theme.rewinds

/**
 * Bottom-sheet session switcher for the chat screen. Purely presentational: it renders the
 * session list from [ChatUiState] and forwards taps to the ViewModel — no state or ordering
 * logic lives here (MV*). The list order is the repository's (newest first); never re-sorted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSessionSwitcher(
    sessions: List<ChatSessionSummary>,
    activeSessionId: Long?,
    onSessionClick: (Long) -> Unit,
    onNewChatClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Single "now" reference so every relative label in this render is consistent.
    val now = Clock.System.now().toEpochMilliseconds()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.rewinds.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = strings.sessionSwitcherTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.rewinds.textPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            NewChatRow(label = strings.sessionSwitcherNewChat, onClick = onNewChatClick)

            if (sessions.isEmpty()) {
                Text(
                    text = strings.sessionSwitcherEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.rewinds.textTertiary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .testTag(TestTags.CHAT_SESSION_LIST)
                        .padding(top = 8.dp)
                ) {
                    items(items = sessions, key = { it.id }) { session ->
                        SessionRow(
                            session = session,
                            isActive = session.id == activeSessionId,
                            relativeTime = ChatSessionLogic.relativeTimeLabel(session.lastMessageTimestamp, now),
                            messageCountLabel = strings.sessionMessageCount(session.messageCount),
                            activeDesc = strings.sessionSwitcherActiveDesc,
                            onClick = { onSessionClick(session.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewChatRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.CHAT_NEW_SESSION_BUTTON)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.rewinds.surfaceRaised)
            .border(
                width = 1.dp,
                color = MaterialTheme.rewinds.accentBlue,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.rewinds.accentBlue
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.rewinds.accentBlue
        )
    }
}

@Composable
private fun SessionRow(
    session: ChatSessionSummary,
    isActive: Boolean,
    relativeTime: String,
    messageCountLabel: String,
    activeDesc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.CHAT_SESSION_LIST_ITEM)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.rewinds.surfaceRaised)
            .border(
                width = 1.dp,
                color = if (isActive) MaterialTheme.rewinds.accentBlue else MaterialTheme.rewinds.border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.rewinds.textPrimary,
                maxLines = 1
            )
            Text(
                text = "$relativeTime · $messageCountLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.rewinds.textTertiary,
                maxLines = 1
            )
        }
        if (isActive) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = activeDesc,
                tint = MaterialTheme.rewinds.accentBlue
            )
        }
    }
}