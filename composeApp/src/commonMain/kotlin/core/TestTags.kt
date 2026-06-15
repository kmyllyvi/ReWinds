package core

/**
 * Single source of truth for Compose `Modifier.testTag(...)` identifiers.
 *
 * These constants are shared between Compose semantic UI tests and Maestro `id:` selectors
 * so the two never drift. Never inline a raw string at a call site — always reference a
 * constant here. Values are kept stable: renaming one breaks every test that asserts on it.
 *
 * Naming convention: `SCREEN_ELEMENT` in UPPER_SNAKE_CASE, value in lower_snake_case.
 * List/grid item tags are intentionally non-indexed; tests select by surrounding content.
 */
object TestTags {

    // ── Home ───────────────────────────────────────────────────────────────────
    const val HOME_SEARCH_FIELD = "home_search_field"
    const val HOME_PLACE_ROW = "home_place_row"
    const val HOME_SEARCH_SUGGESTION = "home_search_suggestion"
    const val HOME_VC_KEY_NUDGE_ACTION = "home_vc_key_nudge_action"

    // ── Tab bar ─────────────────────────────────────────────────────────────────
    const val TAB_PLACES = "tab_places"
    const val TAB_CHAT = "tab_chat"
    const val TAB_SETTINGS = "tab_settings"

    // ── Chat ───────────────────────────────────────────────────────────────────
    const val CHAT_MESSAGE_LIST = "chat_message_list"
    const val CHAT_INPUT_FIELD = "chat_input_field"
    const val CHAT_SEND_BUTTON = "chat_send_button"
    const val CHAT_SESSION_SWITCHER_BUTTON = "chat_session_switcher_button"
    const val CHAT_CONTEXT_CHIP_ROW = "chat_context_chip_row"
    const val CHAT_CONTEXT_CHIP = "chat_context_chip"

    // ── Chat session switcher ─────────────────────────────────────────────────────
    const val CHAT_SESSION_LIST = "chat_session_list"
    const val CHAT_SESSION_LIST_ITEM = "chat_session_list_item"
    const val CHAT_NEW_SESSION_BUTTON = "chat_new_session_button"

    // ── Settings ─────────────────────────────────────────────────────────────────
    const val SETTINGS_LANGUAGE_ROW = "settings_language_row"
    const val SETTINGS_UNITS_ROW = "settings_units_row"
    const val SETTINGS_WIND_SPEED_ROW = "settings_wind_speed_row"
    const val SETTINGS_ANTHROPIC_KEY_ROW = "settings_anthropic_key_row"
    const val SETTINGS_WEATHER_KEY_ROW = "settings_weather_key_row"
    const val SETTINGS_API_KEY_FIELD = "settings_api_key_field"
    const val SETTINGS_API_KEY_SAVE_BUTTON = "settings_api_key_save_button"
    const val SETTINGS_API_KEY_DELETE_BUTTON = "settings_api_key_delete_button"
    const val SETTINGS_API_KEY_CANCEL_BUTTON = "settings_api_key_cancel_button"

    // ── Place summary ─────────────────────────────────────────────────────────────
    const val PLACE_STATION_MAP_BUTTON = "place_station_map_button"
    const val PLACE_YEAR_SELECTOR = "place_year_selector"
    const val PLACE_YEAR_TAB = "place_year_tab"
    const val PLACE_MONTH_GRID = "place_month_grid"
    const val PLACE_MONTH_CELL = "place_month_cell"

    // ── Monthly statistics ──────────────────────────────────────────────────────
    const val MONTH_PREVIOUS_BUTTON = "month_previous_button"
    const val MONTH_NEXT_BUTTON = "month_next_button"
    const val MONTH_STAT_CARD_GRID = "month_stat_card_grid"
    const val MONTH_DAY_LIST = "month_day_list"
}
