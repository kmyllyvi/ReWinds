package place

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import place.components.StoredDaysList
// ... inside MonthlySummaryView.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlySummaryView(
    placeName: String,
    year: Int,
    month: Int,
    onBackClick: () -> Unit,
    vm: MonthlySummaryViewModel = koinViewModel {
        parametersOf(placeName, year, month)
    }
) {
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$placeName - $year/$month") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is WeatherSummaryUiState.Loading -> LoadingStateView()
                is WeatherSummaryUiState.Success -> { // Correction is here
                    StoredDaysList(storedDays = state.storedDays)
                }
                is WeatherSummaryUiState.Error -> ErrorStateView(errorState = state)
            }
        }
    }
}
