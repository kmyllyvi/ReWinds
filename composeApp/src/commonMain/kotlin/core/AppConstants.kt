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
     * Base system prompt for Claude in the chat assistant role.
     * Guides the AI's behaviour and focus area. The per-place downloaded-data summary is
     * appended at send time by [buildAnthropicSystemPrompt] (KIM-321).
     */
    private val ANTHROPIC_SYSTEM_PROMPT_BASE = """
        You are a wind sports assistant for the ReWinds app. You help kitesurfers and windsurfers
        analyse historical weather data for their saved locations. Be concise and focus on
        wind-relevant insights. When asked about conditions, always consider wind speed,
        gusts, and sustained wind together.

        Fetching weather data that is not already downloaded makes a paid API call, so it always
        requires explicit user confirmation. Before promising data you do not have, check the
        downloaded-data summary below.
    """.trimIndent()

    /**
     * Assembles the full system prompt at send time from the live downloaded-months state
     * (KIM-321). [downloadedMonthsByPlace] maps each saved place to the set of `YYYY-MM`
     * months already in the database; [monthFormatter] renders those as full month names
     * (e.g. "October 2025") for the user-facing summary.
     *
     * Pure string construction — no I/O — so the caller passes a current snapshot.
     */
    fun buildAnthropicSystemPrompt(
        downloadedMonthsByPlace: Map<String, Set<String>>,
        monthFormatter: (String) -> String
    ): String {
        if (downloadedMonthsByPlace.isEmpty()) {
            return ANTHROPIC_SYSTEM_PROMPT_BASE +
                "\n\nDownloaded data: none yet. Any weather query will require fetching new data."
        }

        val lines = downloadedMonthsByPlace.entries.joinToString("\n") { (place, months) ->
            if (months.isEmpty()) {
                "$place: no months downloaded"
            } else {
                val names = months.sorted().joinToString(", ") { monthFormatter(it) }
                "$place: $names"
            }
        }
        return ANTHROPIC_SYSTEM_PROMPT_BASE +
            "\n\nDownloaded data already available (no fetch needed for these months):\n$lines"
    }

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
