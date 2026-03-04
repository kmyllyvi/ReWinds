package core

/**
 * Application-wide constants.
 * Centralized configuration values for easy modification and visibility.
 */
object AppConstants {
    // ============================================
    // Anthropic AI Configuration
    // ============================================
    /**
     * Claude model to use for the AI chat feature.
     * claude-haiku-4-5-20241022 (fastest & cheapest - $1 input / $5 output per 1M tokens)
     *
     * See: https://docs.anthropic.com/en/docs/about/models/latest-models
     */
    const val ANTHROPIC_MODEL = "claude-haiku-4-5"

    /**
     * Anthropic API version header.
     * See: https://docs.anthropic.com/en/api/messages
     */
    const val ANTHROPIC_API_VERSION = "2023-06-01"

    /**
     * Anthropic API base URL.
     */
    const val ANTHROPIC_API_BASE_URL = "https://api.anthropic.com/v1/"

    /**
     * Maximum tokens per response from Claude.
     * Balances response length with token cost.
     * Typical conversation response: 200-500 tokens
     */
    const val ANTHROPIC_MAX_TOKENS = 1024

    /**
     * Maximum number of tool-call turns in a single conversation.
     * Prevents infinite loops if Claude keeps calling tools.
     * Typical conversation: 2-5 turns
     */
    const val ANTHROPIC_MAX_TURNS = 20

    /**
     * System prompt for Claude in the chat assistant role.
     * Guides the AI's behavior and focus area.
     */
    val ANTHROPIC_SYSTEM_PROMPT = """
        You are a wind sports assistant for the ReWinds app. You help kitesurfers and windsurfers
        analyse historical weather data for their saved locations. Be concise and focus on
        wind-relevant insights. When asked about conditions, always consider wind speed,
        gusts, and sustained wind together.
    """.trimIndent()

    // ============================================
    // Visual Crossing API Configuration
    // ============================================
    /**
     * Visual Crossing weather API base URL.
     */
    const val VISUAL_CROSSING_BASE_URL = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline"

    // ============================================
    // Network Configuration
    // ============================================
    /**
     * HTTP request timeout in milliseconds.
     */
    const val HTTP_REQUEST_TIMEOUT_MS = 30000L

    // ============================================
    // Database Configuration
    // ============================================
    /**
     * SQLDelight database filename.
     */
    const val DATABASE_NAME = "app.db"

    // ============================================
    // Feature Flags (for future use)
    // ============================================
    /**
     * Enable/disable streaming responses in chat.
     * TODO: Implement streaming UI in Phase D
     */
    const val FEATURE_STREAMING_CHAT = false

    /**
     * Enable/disable persistent chat history.
     * TODO: Implement chat persistence in Phase D
     */
    const val FEATURE_CHAT_PERSISTENCE = false
}
