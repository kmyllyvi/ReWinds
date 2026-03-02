package ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.Log
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
 * UI state for the chat interface.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = ""
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

                // Add assistant message to UI
                val assistantMessage = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = result.responseText
                )

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
     * Clears the conversation and starts a new chat session.
     */
    fun clearChat() {
        aiRepository.clearHistory()
        _uiState.update { ChatUiState() }
        Log.d("ChatViewModel: chat cleared")
    }
}
