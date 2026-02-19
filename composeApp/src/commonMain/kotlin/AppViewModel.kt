import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppViewModel : ViewModel() {
    private val _showContent = MutableStateFlow(true)
    val showContent: StateFlow<Boolean> = _showContent

    fun setShowContent(show: Boolean) {
        _showContent.value = show
    }
}
