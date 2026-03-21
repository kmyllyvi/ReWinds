package ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import core.LocalAppStrings
import core.Navigator
import org.koin.compose.viewmodel.koinViewModel
import components.AppHeader

@Composable
fun ChatView(initialMessage: String? = null, vm: ChatViewModel = koinViewModel(), navigator: Navigator) {
    val uiState by vm.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val strings = LocalAppStrings.current

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Header at top of column - messages start below it
        AppHeader(
            title = strings.chatTitle,
            onBackClick = { navigator.navigateBack() }
        )

        // Messages area with keyboard dismissal on click
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
            isLoading = uiState.isLoading
        )
    }

    // API Key Missing Dialog
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
                Button(onClick = { vm.onApiKeyDialogDismissed() }) {
                    Text(strings.ok)
                }
            }
        )
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    color = if (isUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.content,
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
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
    isLoading: Boolean
) {
    val strings = LocalAppStrings.current
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f),
            placeholder = { Text(strings.messagePlaceholder) },
            singleLine = false,
            maxLines = 3,
            enabled = !isLoading
        )

        IconButton(
            onClick = {
                onSendClick()
                // Dismiss keyboard after sending
                focusManager.clearFocus()
            },
            enabled = inputText.trim().isNotEmpty() && !isLoading,
            modifier = Modifier
                .padding(bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = strings.sendMessageDesc,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
