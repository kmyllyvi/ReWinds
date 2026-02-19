import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppViewModelTest {

    @Test
    fun showContent_initiallyTrue() {
        val viewModel = AppViewModel()
        val initialValue = viewModel.showContent.value
        assertTrue(initialValue, "showContent should be true initially")
    }

    @Test
    fun setShowContent_updatesStateFlow() {
        val viewModel = AppViewModel()

        // Initial state should be true
        assertEquals(true, viewModel.showContent.value)

        // Set to false
        viewModel.setShowContent(false)
        assertEquals(false, viewModel.showContent.value)

        // Set back to true
        viewModel.setShowContent(true)
        assertEquals(true, viewModel.showContent.value)
    }

    @Test
    fun setShowContent_false_makesContentHidden() {
        val viewModel = AppViewModel()
        viewModel.setShowContent(false)
        assertFalse(viewModel.showContent.value)
    }

    @Test
    fun setShowContent_true_makesContentVisible() {
        val viewModel = AppViewModel()

        // First hide it
        viewModel.setShowContent(false)
        assertFalse(viewModel.showContent.value)

        // Then show it
        viewModel.setShowContent(true)
        assertTrue(viewModel.showContent.value)
    }
}
