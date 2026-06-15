package uitest

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import core.FakeNavigator
import core.GeoSearchResult
import core.NavigationCall
import core.TestTags
import home.HomeView
import home.HomeViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.theme.ReWindsTheme

/**
 * Compose semantic UI tests for the Home journeys (KIM-293):
 *  - J2: add a place via search
 *  - J3: open a saved place's summary
 *
 * Both drive the real [HomeView] with a [HomeViewModel] built from in-memory fakes (no network /
 * database). Selectors are [TestTags] constants; outgoing navigation is asserted via [FakeNavigator].
 * See `composeApp/src/androidInstrumentedTest/README.md` for the pattern.
 */
@RunWith(AndroidJUnit4::class)
class HomeJourneyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val helsinki = GeoSearchResult(
        id = 1,
        name = "Helsinki",
        latitude = 60.17,
        longitude = 24.94,
        country = "Finland",
        region = "Uusimaa"
    )

    private fun homeViewModel(
        searchResults: List<GeoSearchResult> = emptyList(),
        initialPlaces: List<String> = emptyList()
    ): Pair<HomeViewModel, FakeWeatherRepository> {
        val repo = FakeWeatherRepository(searchResults = searchResults, initialPlaces = initialPlaces)
        val vm = HomeViewModel(
            weatherRepository = repo,
            databaseExportImport = core.DatabaseExportImport(),
            database = FakeDatabase()
        )
        return vm to repo
    }

    @Test
    fun j2_typingQueryAndTappingSuggestion_addsPlaceRow() {
        val (vm, _) = homeViewModel(searchResults = listOf(helsinki))
        val navigator = FakeNavigator()

        composeTestRule.setContent {
            ReWindsTheme { HomeView(vm = vm, navigator = navigator) }
        }

        // No saved places yet → no PlaceRow on screen.
        composeTestRule.onAllNodesWithTag(TestTags.HOME_PLACE_ROW).assertCountEquals(0)

        composeTestRule.onNodeWithTag(TestTags.HOME_SEARCH_FIELD).performTextInput("Helsinki")

        // Search is debounced (500ms) then async; wait for the suggestion to resolve.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(TestTags.HOME_SEARCH_SUGGESTION)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodesWithTag(TestTags.HOME_SEARCH_SUGGESTION).onFirst().performClick()

        // Selecting the suggestion adds the place, which renders as a PlaceRow.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(TestTags.HOME_PLACE_ROW)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithTag(TestTags.HOME_PLACE_ROW).assertCountEquals(1)
    }

    @Test
    fun j3_tappingPlaceRow_navigatesToPlaceSummary() {
        // Seed a saved place so a PlaceRow is present from the start.
        val (vm, _) = homeViewModel(initialPlaces = listOf("Helsinki"))
        val navigator = FakeNavigator()

        composeTestRule.setContent {
            ReWindsTheme { HomeView(vm = vm, navigator = navigator) }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(TestTags.HOME_PLACE_ROW)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodesWithTag(TestTags.HOME_PLACE_ROW).onFirst().performClick()

        // HomeView routes saved-place taps through the ViewModel's navigation event,
        // which calls navigator.navigateToPlaceSummary(...).
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            navigator.calls.isNotEmpty()
        }
        composeTestRule.runOnIdle {
            assert(navigator.lastCall == NavigationCall.PlaceSummary("Helsinki")) {
                "Expected navigation to PlaceSummary(Helsinki) but was ${navigator.lastCall}"
            }
        }
    }
}
