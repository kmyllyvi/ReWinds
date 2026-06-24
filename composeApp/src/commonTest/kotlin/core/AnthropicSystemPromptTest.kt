package core

import core.utils.formatMonthName
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * KIM-321: the system prompt is assembled at send time from the downloaded-months snapshot.
 * Covers zero places, one place with months, and two places.
 */
class AnthropicSystemPromptTest {

    @Test
    fun zeroPlaces_statesNoneDownloaded() {
        val prompt = AppConstants.buildAnthropicSystemPrompt(emptyMap(), ::formatMonthName)
        assertContains(prompt, "wind sports assistant")
        assertContains(prompt, "none yet")
    }

    @Test
    fun onePlace_listsMonthsAsFullNames() {
        val prompt = AppConstants.buildAnthropicSystemPrompt(
            mapOf("Helsinki" to setOf("2025-10", "2025-11", "2025-12")),
            ::formatMonthName
        )
        assertContains(prompt, "Helsinki: October 2025, November 2025, December 2025")
        // YYYY-MM grouping must not leak into user-facing copy.
        assertFalse(prompt.contains("2025-10"))
    }

    @Test
    fun twoPlaces_listEachOnItsOwnLine() {
        val prompt = AppConstants.buildAnthropicSystemPrompt(
            mapOf(
                "Tarifa" to setOf("2025-06"),
                "Maui" to setOf("2024-01", "2024-02")
            ),
            ::formatMonthName
        )
        assertContains(prompt, "Tarifa: June 2025")
        assertContains(prompt, "Maui: January 2024, February 2024")
    }

    @Test
    fun placeWithNoMonths_isLabelledNotEmpty() {
        val prompt = AppConstants.buildAnthropicSystemPrompt(
            mapOf("Tarifa" to emptySet()),
            ::formatMonthName
        )
        assertContains(prompt, "Tarifa: no months downloaded")
    }
}
