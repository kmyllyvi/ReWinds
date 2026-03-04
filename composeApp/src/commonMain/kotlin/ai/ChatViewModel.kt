package ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.Log
import core.isAnthropicApiKeyConfigured
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
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
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val showApiKeyMissingDialog: Boolean = false,
    val pendingDataFetch: PendingDataFetch? = null
)

/**
 * ViewModel for the chat interface.
 * Manages conversation state and delegates to AiRepository for AI logic.
 */
class ChatViewModel(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    /**
     * The current UI state.
     */
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

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

        viewModelScope.launch(Dispatchers.IO) {
            try {
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

                Log.d("ChatViewModel: received response in ${result.totalTurns} turn(s), tools called: ${result.toolCallsMade}")
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
     * Clears the conversation and starts a new chat session.
     */
    fun clearChat() {
        aiRepository.clearHistory()
        _uiState.update { ChatUiState() }
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

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Send confirmation to AI to proceed with fetch and query
                val message = "Proceed with fetching weather data for ${pending.location} from ${pending.startDate} to ${pending.endDate} and then answer my original question."
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

                Log.d("ChatViewModel: Data fetch confirmed and query executed in ${result.totalTurns} turn(s)")
            } catch (e: Exception) {
                Log.e("ChatViewModel: error during data fetch confirmation", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error fetching data: ${e.message}",
                        pendingDataFetch = null
                    )
                }
            }
        }
    }
}
