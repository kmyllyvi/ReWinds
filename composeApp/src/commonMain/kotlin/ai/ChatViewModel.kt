package ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.Log
import core.isAnthropicApiKeyConfigured
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Represents different roles for messages in the chat.
 */
enum class MessageRole {
    USER, ASSISTANT
}

/**
 * Generates a unique ID string for messages.
 */
private fun generateMessageId(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..32).map { chars.random() }.joinToString("")
}

/**
 * A single message in the chat UI.
 */
data class ChatMessage(
    val id: String = generateMessageId(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = 0L // Set when created
)

/**
 * A selectable context chip shown above the message list. [placeName] is null
 * for the "All places" chip; a non-null value targets a specific saved place.
 */
data class ContextChip(
    val placeName: String?,
    val isSelected: Boolean
)

/**
 * Details about a pending data fetch that requires user permission.
 */
data class PendingDataFetch(
    val location: String,
    val startDate: String,
    val endDate: String,
    val metrics: List<String>
)

/**
 * UI state for the chat interface.
 *
 * [showApiKeyMissingDialog] — Anthropic key is absent before sending.
 * [showApiKeyInvalidError] — Anthropic API returned 401/403 (key invalid/expired).
 * [error] — generic (non-auth) failure; shown as a dismissible banner without a Settings CTA.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val showApiKeyMissingDialog: Boolean = false,
    val showApiKeyInvalidError: Boolean = false,
    val pendingDataFetch: PendingDataFetch? = null,
    /** Context chips above the message list. The "All places" chip is always present. */
    val contextChips: List<ContextChip> = listOf(ContextChip(placeName = null, isSelected = true)),
    /** Whether the session switcher (bottom sheet) is currently open. */
    val isSessionSwitcherOpen: Boolean = false,
    /**
     * Saved chat sessions for the switcher, newest activity first. Mirrors
     * [ChatRepository.listSessions] ordering — never re-sorted in the View.
     */
    val sessions: List<ChatSessionSummary> = emptyList(),
    /** Id of the session currently shown in the chat view; highlighted in the switcher. */
    val activeSessionId: Long? = null
) {
    /** Send is enabled only when there is non-blank input and no request in flight. */
    val isSendEnabled: Boolean
        get() = inputText.isNotBlank() && !isLoading
}

/**
 * ViewModel for the chat interface.
 * Manages conversation state and delegates to AiRepository for AI logic.
 */
class ChatViewModel(
    private val aiRepository: AiRepository,
    private val weatherRepository: core.WeatherRepository,
    private val chatRepository: ChatRepository,
    /** Background dispatcher for repository I/O. Injectable so tests can substitute a TestDispatcher. */
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    /**
     * Session the VM is currently reading from / writing to. Resolved on launch via
     * [ChatSessionLogic.resolveActiveSessionId] (defaults to the most recent session)
     * and updated by [switchToSession]. Replaces the old "always load the latest" rule.
     */
    private var currentSessionId: Long? = null

    private val _uiState = MutableStateFlow(ChatUiState(
        messages = listOf(
            ChatMessage(
                role = MessageRole.ASSISTANT,
                content = "Let's talk about the weather!"
            )
        )
    ))
    /**
     * The current UI state.
     */
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            loadActiveSession(requestedId = null)
        }
        loadContextChips()
    }

    /**
     * Resolves and loads the active session. With [requestedId] null (app launch) the
     * most recent session is used; a session is auto-created when none exist, so the
     * original single-chat launch behaviour is preserved.
     */
    private suspend fun loadActiveSession(requestedId: Long?) {
        val sessionIds = chatRepository.listSessions().map { it.id }
        val resolvedId = ChatSessionLogic.resolveActiveSessionId(requestedId, sessionIds)
            ?: chatRepository.createSession()

        currentSessionId = resolvedId
        val savedMessages = chatRepository.loadUiMessages(resolvedId)
        if (savedMessages.isNotEmpty()) {
            val history = chatRepository.loadConversationHistory(resolvedId)
            aiRepository.loadHistory(history)
            _uiState.update { it.copy(messages = savedMessages, activeSessionId = resolvedId) }
            Log.d("ChatViewModel: loaded ${savedMessages.size} messages from session $resolvedId")
        } else {
            _uiState.update { it.copy(activeSessionId = resolvedId) }
            Log.d("ChatViewModel: session $resolvedId has no messages, starting fresh")
        }
    }

    /**
     * Opens the session switcher, refreshing the session list from the repository so the
     * sheet always reflects current titles/timestamps. List order is the repository's
     * (newest first) — never re-sorted here.
     */
    fun openSessionSwitcher() {
        viewModelScope.launch(ioDispatcher) {
            val sessions = runCatching { chatRepository.listSessions() }.getOrDefault(emptyList())
            _uiState.update { it.copy(sessions = sessions, isSessionSwitcherOpen = true) }
            Log.d("ChatViewModel: opened session switcher (${sessions.size} sessions)")
        }
    }

    /** Closes the session switcher without changing the active session. */
    fun closeSessionSwitcher() {
        _uiState.update { it.copy(isSessionSwitcherOpen = false) }
    }

    /**
     * Creates a fresh session, makes it active, and closes the switcher. The new chat
     * starts empty with the standard greeting; the previous session stays persisted.
     */
    fun startNewChat() {
        viewModelScope.launch(ioDispatcher) {
            val newId = chatRepository.createSession()
            currentSessionId = newId
            aiRepository.clearHistory()
            _uiState.update {
                it.copy(
                    messages = listOf(
                        ChatMessage(
                            role = MessageRole.ASSISTANT,
                            content = "Let's talk about the weather!"
                        )
                    ),
                    activeSessionId = newId,
                    isSessionSwitcherOpen = false,
                    error = null,
                    pendingDataFetch = null
                )
            }
            Log.d("ChatViewModel: started new chat (session $newId)")
        }
    }

    /**
     * Switches the active session to [sessionId] and rebuilds UI + AI history from its
     * stored messages, then closes the switcher. No-op when the session no longer exists
     * (the switcher stays open so the user can pick another).
     */
    fun switchToSession(sessionId: Long) {
        viewModelScope.launch(ioDispatcher) {
            val messages = chatRepository.switchToSession(sessionId)
            if (messages == null) {
                Log.d("ChatViewModel: switch ignored, session $sessionId not found")
                return@launch
            }
            currentSessionId = sessionId
            val history = chatRepository.loadConversationHistory(sessionId)
            aiRepository.loadHistory(history)
            _uiState.update {
                it.copy(
                    messages = messages,
                    activeSessionId = sessionId,
                    isSessionSwitcherOpen = false
                )
            }
            Log.d("ChatViewModel: switched to session $sessionId (${messages.size} messages)")
        }
    }

    /**
     * Resolves the "Ask AI about this place" entry point: switches to the existing chat
     * session tagged with [placeId], or creates a new one tagged with that place when none
     * exists. Either way the place's session becomes active. Idempotent — re-entering for a
     * place that is already active reloads the same session.
     */
    fun openPlaceChat(placeId: String) {
        viewModelScope.launch(ioDispatcher) {
            val sessions = runCatching { chatRepository.listSessions() }.getOrDefault(emptyList())
            val existingId = ChatSessionLogic.resolveSessionForPlace(placeId, sessions)
            val targetId = existingId ?: chatRepository.createSession(placeId = placeId)

            val messages = chatRepository.switchToSession(targetId) ?: emptyList()
            currentSessionId = targetId
            aiRepository.loadHistory(chatRepository.loadConversationHistory(targetId))

            val resolvedMessages = messages.ifEmpty {
                listOf(
                    ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = "Let's talk about the weather!"
                    )
                )
            }
            _uiState.update {
                it.copy(
                    messages = resolvedMessages,
                    activeSessionId = targetId,
                    isSessionSwitcherOpen = false,
                    error = null,
                    pendingDataFetch = null
                )
            }
            Log.d("ChatViewModel: opened place chat for '$placeId' (session $targetId, existing=${existingId != null})")
        }
    }

    /**
     * Builds the context-chip row. "All places" is always first and starts selected; a
     * per-place chip is added only for places that have at least one tagged chat session
     * (selecting a chip for a place with no chats would filter to an empty list).
     */
    private fun loadContextChips() {
        viewModelScope.launch(ioDispatcher) {
            val placeNames = runCatching { weatherRepository.getSavedPlaceNames() }
                .getOrDefault(emptyList())
            val taggedPlaceIds = runCatching { chatRepository.listSessions().map { it.placeId } }
                .getOrDefault(emptyList())
            val placesWithChats = ChatSessionLogic.placesWithSessions(placeNames, taggedPlaceIds)
            val chips = buildList {
                add(ContextChip(placeName = null, isSelected = true))
                placesWithChats.forEach { add(ContextChip(placeName = it, isSelected = false)) }
            }
            _uiState.update { it.copy(contextChips = chips) }
        }
    }

    /**
     * Selects a context chip by place name (null == "All places").
     * Selection is mutually exclusive and lives in UI state, not local composable state.
     */
    fun selectContextChip(placeName: String?) {
        _uiState.update { state ->
            state.copy(
                contextChips = state.contextChips.map { chip ->
                    chip.copy(isSelected = chip.placeName == placeName)
                }
            )
        }
    }

    private suspend fun persistMessage(message: ChatMessage) {
        val sessionId = currentSessionId ?: run {
            Log.d("ChatViewModel: session not ready, skipping persist")
            return
        }
        chatRepository.saveMessage(sessionId, message)
    }

    /**
     * Called when the user sends a message.
     * @param userInput The user's message text.
     */
    fun sendMessage(userInput: String) {
        val trimmedInput = userInput.trim()
        if (trimmedInput.isBlank()) {
            Log.d("ChatViewModel: ignoring blank user input")
            return
        }

        // Check if user is confirming a pending data fetch
        val currentState = _uiState.value
        if (currentState.pendingDataFetch != null) {
            val isConfirmation = trimmedInput.lowercase() in listOf("yes", "ok", "proceed", "confirm", "y")
            if (isConfirmation) {
                Log.d("ChatViewModel: User confirmed data fetch for ${currentState.pendingDataFetch.location}")
                confirmPendingDataFetch(trimmedInput)
                return
            } else {
                // User rejected the fetch
                Log.d("ChatViewModel: User rejected data fetch")
                val userMessage = ChatMessage(
                    role = MessageRole.USER,
                    content = trimmedInput
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + userMessage,
                        inputText = "",
                        pendingDataFetch = null,
                        error = "Data fetch cancelled. You can ask another question or try a different approach."
                    )
                }
                return
            }
        }

        // Check if API key is configured
        if (!isAnthropicApiKeyConfigured()) {
            Log.d("ChatViewModel: API key not configured, showing dialog")
            _uiState.update { it.copy(showApiKeyMissingDialog = true) }
            return
        }

        Log.d("ChatViewModel: user sent message: '$trimmedInput'")

        // Add user message to UI immediately
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = trimmedInput
        )
        _uiState.update { it.copy(messages = it.messages + userMessage, inputText = "") }

        // Start loading state
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch(ioDispatcher) {
            try {
                // Persist user message
                persistMessage(userMessage)

                // Send to AI repository
                val result = aiRepository.sendMessage(trimmedInput)

                // Check if the response indicates we're waiting for data fetch permission
                val responseText = result.responseText
                val hasPermissionKeywords = responseText.contains("fetch", ignoreCase = true) &&
                    (responseText.contains("API call", ignoreCase = true) ||
                     responseText.contains("yes or ok", ignoreCase = true) ||
                     responseText.contains("permission", ignoreCase = true))

                // Add assistant message to UI
                val assistantMessage = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = result.responseText
                )

                // If we're waiting for permission, extract the details and set pending fetch
                if (hasPermissionKeywords && currentState.pendingDataFetch == null) {
                    // Try to extract location and dates from the response
                    val locationRegex = Regex("""for\s+(\w+)\s+from""", RegexOption.IGNORE_CASE)
                    val dateRangeRegex = Regex("""from\s+(\d{4}-\d{2}-\d{2})\s+to\s+(\d{4}-\d{2}-\d{2})""", RegexOption.IGNORE_CASE)

                    val locationMatch = locationRegex.find(responseText)
                    val dateMatch = dateRangeRegex.find(responseText)

                    if (locationMatch != null && dateMatch != null) {
                        val location = locationMatch.groupValues.getOrNull(1) ?: ""
                        val startDate = dateMatch.groupValues.getOrNull(1) ?: ""
                        val endDate = dateMatch.groupValues.getOrNull(2) ?: ""

                        if (location.isNotEmpty() && startDate.isNotEmpty() && endDate.isNotEmpty()) {
                            setPendingDataFetch(location, startDate, endDate, emptyList())
                            Log.d("ChatViewModel: Set pending data fetch for $location from $startDate to $endDate")
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isLoading = false
                    )
                }

                // Persist assistant message
                persistMessage(assistantMessage)

                Log.d("ChatViewModel: received response in ${result.totalTurns} turn(s), tools called: ${result.toolCallsMade}")
            } catch (e: AnthropicException) {
                if (e.httpStatus == 401 || e.httpStatus == 403) {
                    Log.e("ChatViewModel: Anthropic auth error (${e.httpStatus})", e)
                    _uiState.update { it.copy(isLoading = false, showApiKeyInvalidError = true) }
                } else {
                    Log.e("ChatViewModel: Anthropic API error", e)
                    _uiState.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel: error sending message", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Called when the input text field changes.
     * @param text The new input text.
     */
    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * Called when the error is dismissed.
     */
    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Called when the API key missing dialog is dismissed.
     */
    fun onApiKeyDialogDismissed() {
        _uiState.update { it.copy(showApiKeyMissingDialog = false) }
    }

    /**
     * Called when the "API key invalid" error banner is dismissed.
     */
    fun onApiKeyInvalidErrorDismissed() {
        _uiState.update { it.copy(showApiKeyInvalidError = false) }
    }

    /**
     * Clears the conversation and starts a new chat session.
     */
    fun clearChat() {
        aiRepository.clearHistory()
        viewModelScope.launch(ioDispatcher) {
            currentSessionId?.let { chatRepository.clearSession(it) }
        }
        _uiState.update {
            ChatUiState(
                messages = listOf(
                    ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = "Let's talk about the weather!"
                    )
                )
            )
        }
        Log.d("ChatViewModel: chat cleared")
    }

    /**
     * Called when the AI requests permission to fetch data.
     * Stores the pending fetch and updates UI to show the permission message.
     */
    internal fun setPendingDataFetch(location: String, startDate: String, endDate: String, metrics: List<String>) {
        val pending = PendingDataFetch(location, startDate, endDate, metrics)
        _uiState.update { it.copy(pendingDataFetch = pending) }
        Log.d("ChatViewModel: Pending data fetch set for $location from $startDate to $endDate")
    }

    /**
     * Called when user confirms a pending data fetch.
     * Triggers the data fetch and then re-sends the original query.
     */
    private fun confirmPendingDataFetch(confirmationInput: String) {
        val currentState = _uiState.value
        val pending = currentState.pendingDataFetch ?: return

        Log.d("ChatViewModel: Confirming data fetch for ${pending.location}")

        // Add user confirmation to messages
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = confirmationInput
        )
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                // Persist user confirmation message
                persistMessage(userMessage)

                // STEP 1: Actually fetch the data from the API before asking Claude to retry
                Log.d("ChatViewModel: Fetching data for ${pending.location} from ${pending.startDate} to ${pending.endDate}")

                try {
                    weatherRepository.getDaysRange(
                        pending.location,
                        pending.startDate,
                        pending.endDate
                    )
                    Log.d("ChatViewModel: Data fetch completed successfully")
                } catch (e: Exception) {
                    Log.e("ChatViewModel: Failed to fetch data", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to fetch weather data: ${e.message}",
                            pendingDataFetch = null
                        )
                    }
                    return@launch
                }

                // STEP 2: Now that data is fetched, ask Claude to retry the original query
                val message = "I've fetched the weather data. Now please answer my original question about ${pending.location} from ${pending.startDate} to ${pending.endDate}."
                val result = aiRepository.sendMessage(message)

                // Add assistant response
                val assistantMessage = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = result.responseText
                )

                _uiState.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isLoading = false,
                        pendingDataFetch = null  // Clear pending fetch
                    )
                }

                // Persist assistant response
                persistMessage(assistantMessage)

                Log.d("ChatViewModel: Data fetch confirmed and query executed in ${result.totalTurns} turn(s)")
            } catch (e: AnthropicException) {
                if (e.httpStatus == 401 || e.httpStatus == 403) {
                    Log.e("ChatViewModel: Anthropic auth error during data fetch confirmation (${e.httpStatus})", e)
                    _uiState.update { it.copy(isLoading = false, showApiKeyInvalidError = true, pendingDataFetch = null) }
                } else {
                    Log.e("ChatViewModel: Anthropic API error during data fetch confirmation", e)
                    _uiState.update { it.copy(isLoading = false, error = "Error: ${e.message}", pendingDataFetch = null) }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel: error during data fetch confirmation", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error: ${e.message}",
                        pendingDataFetch = null
                    )
                }
            }
        }
    }
}
