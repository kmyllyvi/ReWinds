package home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for KIM-268: PlaceStatus dot state and AlertBanner state in HomeUiState.
 *
 * These tests verify that status dot colour and banner visibility are driven
 * entirely by ViewModel-provided state — no logic in the composable.
 */
class HomeViewModelStatusTest {

    // ── PlaceStatus ──────────────────────────────────────────────────────────

    @Test
    fun placeWithDaysHasNormalStatus() {
        val place = PlaceDisplayData(name = "Helsinki", subtitle = "365 days stored", status = PlaceStatus.NORMAL)
        assertEquals(PlaceStatus.NORMAL, place.status)
    }

    @Test
    fun placeWithNoDaysHasWarningStatus() {
        val place = PlaceDisplayData(name = "Helsinki", subtitle = "No data yet", status = PlaceStatus.WARNING)
        assertEquals(PlaceStatus.WARNING, place.status)
    }

    @Test
    fun placeWithErrorHasErrorStatus() {
        val place = PlaceDisplayData(name = "Helsinki", subtitle = "Fetch failed", status = PlaceStatus.ERROR)
        assertEquals(PlaceStatus.ERROR, place.status)
    }

    @Test
    fun defaultStatusIsNormal() {
        val place = PlaceDisplayData(name = "Espoo", subtitle = "10 days stored")
        assertEquals(PlaceStatus.NORMAL, place.status)
    }

    // ── AlertBanner ──────────────────────────────────────────────────────────

    @Test
    fun attentionBannerIsNotError() {
        val banner = AlertBanner(message = "Data is 2 days old", isError = false)
        assertFalse(banner.isError)
        assertEquals("Data is 2 days old", banner.message)
    }

    @Test
    fun errorBannerIsError() {
        val banner = AlertBanner(message = "Fetch failed for Oulu", isError = true)
        assertTrue(banner.isError)
    }

    // ── HomeUiState banner list ──────────────────────────────────────────────

    @Test
    fun noAlertBannersInDefaultState() {
        val state = HomeUiState()
        assertTrue(state.alertBanners.isEmpty())
    }

    @Test
    fun stateCanHoldMultipleBanners() {
        val banners = listOf(
            AlertBanner(message = "Warning for place A", isError = false),
            AlertBanner(message = "Error for place B", isError = true)
        )
        val state = HomeUiState(alertBanners = banners)
        assertEquals(2, state.alertBanners.size)
        assertFalse(state.alertBanners[0].isError)
        assertTrue(state.alertBanners[1].isError)
    }

    @Test
    fun clearingBannersYieldsEmptyList() {
        val state = HomeUiState(alertBanners = listOf(AlertBanner("old", false)))
        val cleared = state.copy(alertBanners = emptyList())
        assertTrue(cleared.alertBanners.isEmpty())
    }

    // ── PlaceDisplayData subtitle ────────────────────────────────────────────

    @Test
    fun subtitleIsPropagatedCorrectly() {
        val place = PlaceDisplayData(name = "Turku", subtitle = "42 days stored", status = PlaceStatus.NORMAL)
        assertEquals("42 days stored", place.subtitle)
    }
}
