package ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import core.LocalAppStrings
import core.Navigator
import core.TestTags
import org.koin.compose.viewmodel.koinViewModel
import components.AppHeader
import ui.components.IsobarBackground
import ui.theme.rewinds

@Composable
fun ChatView(
    initialMessage: String? = null,
    placeId: String? = null,
    /** Invoked after the one-shot [placeId] has been resolved, so the host can drop it. */
    onPlaceIdConsumed: () -> Unit = {},
    vm: ChatViewModel = koinViewModel(),
    navigator: Navigator
) {
    val uiState by vm.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val strings = LocalAppStrings.current

    // Tracks whether this composable instance was ever entered with a place context, so the
    // placeId-consume transition (non-null -> null, see ChatStackOps.consumePlaceContext)
    // doesn't immediately re-trigger ensureGeneralChat() and revert the place chat it just
    // opened (KIM-297).
    var hadPlaceId by remember { mutableStateOf(false) }

    // "Ask AI about this place" — resolve to (or create) the session tagged with this place,
    // then signal the host to clear the one-shot placeId so revisits behave normally.
    LaunchedEffect(placeId) {
        if (!placeId.isNullOrEmpty()) {
            hadPlaceId = true
            vm.openPlaceChat(placeId)
            onPlaceIdConsumed()
        } else if (!hadPlaceId) {
            // Plain entry to the Chat tab (no place context, and none was just consumed):
            // make sure we're showing the general session, not a stale place-tagged one.
            vm.ensureGeneralChat()
        }
    }

    // Pre-fill input with initial message (e.g. "Chat about Helsinki")
    LaunchedEffect(initialMessage) {
        if (!initialMessage.isNullOrEmpty()) {
            vm.onInputTextChange(initialMessage)
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.rewinds.pageBg)
    ) {
        // Decorative isobar background — rendered first so it sits below all content.
        IsobarBackground()

        // Note: keyboard (IME) inset is applied by the Scaffold content host in Router, which
        // collapses it with the tab-bar inset to avoid double-counting (KIM-298). Adding
        // imePadding() here would re-introduce that gap on iOS.
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
        // Header at top of column - messages start below it.
        // Right action: opens the session switcher (chat screen only).
        AppHeader(
            title = strings.chatTitle,
            onBackClick = { navigator.navigateBack() },
            rightContent = {
                IconButton(
                    onClick = vm::openSessionSwitcher,
                    modifier = Modifier.testTag(TestTags.CHAT_SESSION_SWITCHER_BUTTON)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = strings.sessionSwitcherIconDesc,
                        tint = MaterialTheme.rewinds.textPrimary
                    )
                }
            }
        )

        // Static place tag: shown only when this chat is tagged to a place. Informational
        // only — switching chats is the session switcher's job (KIM-286), not this pill.
        uiState.currentPlaceTag?.let { placeTag ->
            PlaceTagPill(placeName = placeTag)
        }

        // Messages area with keyboard dismissal on click
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag(TestTags.CHAT_MESSAGE_LIST)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(
                    indication = null,
                    interactionSource = MutableInteractionSource()
                ) {
                    focusManager.clearFocus()
                },
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            reverseLayout = false
        ) {
            items(
                items = uiState.messages,
                key = { message -> message.id }
            ) { message ->
                ChatMessageBubble(message)
            }

            // Loading indicator
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Error message display
        AnimatedVisibility(visible = uiState.error != null) {
            uiState.error?.let { errorMessage ->
                ErrorMessageBox(
                    error = errorMessage,
                    onDismiss = vm::onErrorDismissed
                )
            }
        }

        // Input area
        ChatInputArea(
            inputText = uiState.inputText,
            onInputChange = vm::onInputTextChange,
            onSendClick = { vm.sendMessage(uiState.inputText) },
            isSendEnabled = uiState.isSendEnabled
        )
        }
    }

    // Session switcher bottom sheet — state owned by the ViewModel.
    if (uiState.isSessionSwitcherOpen) {
        ChatSessionSwitcher(
            sessions = uiState.sessions,
            activeSessionId = uiState.activeSessionId,
            onSessionClick = vm::switchToSession,
            onNewChatClick = vm::startNewChat,
            onDismiss = vm::closeSessionSwitcher
        )
    }

    // API Key Missing Dialog — with "Go to Settings" primary action
    if (uiState.showApiKeyMissingDialog) {
        AlertDialog(
            onDismissRequest = { vm.onApiKeyDialogDismissed() },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = strings.warningIconDesc,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(strings.apiKeyNotConfigured) },
            text = {
                Text(strings.apiKeyNotConfiguredMessage)
            },
            confirmButton = {
                Button(onClick = {
                    vm.onApiKeyDialogDismissed()
                    navigator.navigateToSettings()
                }) {
                    Text(strings.goToSettings)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.onApiKeyDialogDismissed() }) {
                    Text(strings.dismiss)
                }
            }
        )
    }

    // API Key Invalid Dialog — shown when Anthropic returns 401/403
    if (uiState.showApiKeyInvalidError) {
        AlertDialog(
            onDismissRequest = { vm.onApiKeyInvalidErrorDismissed() },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = strings.warningIconDesc,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(strings.claudeKeyInvalidTitle) },
            text = { Text(strings.claudeKeyInvalidMessage) },
            confirmButton = {
                Button(onClick = {
                    vm.onApiKeyInvalidErrorDismissed()
                    navigator.navigateToSettings()
                }) {
                    Text(strings.goToSettings)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.onApiKeyInvalidErrorDismissed() }) {
                    Text(strings.dismiss)
                }
            }
        )
    }
}

/**
 * Static, non-interactive pill naming the place this chat is tagged to. Display-only:
 * a place-tagged chat shows exactly this place; switching chats is the session
 * switcher's job (KIM-286), so there is nothing to tap here.
 */
@Composable
private fun PlaceTagPill(placeName: String) {
    Row(
        modifier = Modifier
            .testTag(TestTags.CHAT_PLACE_TAG_PILL)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.rewinds.surfaceRaised)
            .border(
                width = 1.dp,
                color = MaterialTheme.rewinds.border,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.rewinds.accentBlue,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = placeName,
            color = MaterialTheme.rewinds.accentBlue,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.CHAT_MESSAGE_BUBBLE)
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    color = if (isUser)
                        MaterialTheme.rewinds.surfaceRaised
                    else
                        MaterialTheme.rewinds.surface,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Text(
                text = message.content,
                color = MaterialTheme.rewinds.textPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ErrorMessageBox(error: String, onDismiss: () -> Unit) {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text(strings.dismiss)
            }
        }
    }
}

@Composable
fun ChatInputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isSendEnabled: Boolean
) {
    val strings = LocalAppStrings.current
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.rewinds.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .testTag(TestTags.CHAT_INPUT_FIELD),
            placeholder = {
                Text(
                    strings.messagePlaceholder,
                    color = MaterialTheme.rewinds.textTertiary
                )
            },
            singleLine = false,
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.rewinds.border,
                unfocusedBorderColor = MaterialTheme.rewinds.border,
                focusedTextColor = MaterialTheme.rewinds.textPrimary,
                unfocusedTextColor = MaterialTheme.rewinds.textPrimary,
                cursorColor = MaterialTheme.rewinds.accentBlue
            )
        )

        IconButton(
            onClick = {
                onSendClick()
                // Dismiss keyboard after sending
                focusManager.clearFocus()
            },
            enabled = isSendEnabled,
            modifier = Modifier
                .padding(bottom = 4.dp)
                .testTag(TestTags.CHAT_SEND_BUTTON)
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = strings.sendMessageDesc,
                tint = if (isSendEnabled)
                    MaterialTheme.rewinds.accentBlue
                else
                    MaterialTheme.rewinds.textTertiary
            )
        }
    }
}
