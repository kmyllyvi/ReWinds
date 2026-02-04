package place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import core.Screen
import io.github.aakira.napier.Napier
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import place.components.MonthSelector
import place.components.MonthSelectorWithTemperature
import place.components.YearDropdownSelector
import kotlin.time.ExperimentalTime

// Define this outside or in a shared file if MonthSelector needs it directly
// For now, keeping it local to SuccessStateView and MonthSelector will use the map
data class MonthCompletionInfo(
    val presentDaysCount: Int,
    val totalDaysInMonth: Int,
    val isFullyLoaded: Boolean
)

// Assuming DayWeatherSummary is defined in your model or another file in the 'place' package
// e.g., data class DayWeatherSummary(val date: String?, ...)
// Assuming PlaceSummaryViewModel is defined similarly
// e.g., class PlaceSummaryViewModel(...) : ViewModel()
// Assuming WeatherSummaryUiState is defined similarly
// e.g., sealed interface WeatherSummaryUiState { object Loading; data class Success(...); data class Error(...); }


// Helper to parse year from "YYYY-MM-DD" string or return null if invalid
internal fun parseYear(dateString: String?): Int? {
    return dateString?.split("-")?.firstOrNull()?.toIntOrNull()
}

// Helper to parse month from "YYYY-MM-DD" string or return null if invalid
internal fun parseMonth(dateString: String?): Int? {
    return dateString?.split("-")?.getOrNull(1)?.toIntOrNull()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSummaryView(onBackClick: () -> Unit, navController: NavController, vm: PlaceSummaryViewModel = koinViewModel()) {
    val uiState by vm.uiState.collectAsState()
    val currentPlaceName = vm.placeName // Access it directly

    LaunchedEffect(Unit) {
        vm.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.ToMonthlySummary -> {
                    navController.navigate(
                        Screen.MonthlyStatistics.createRoute(
                            event.placeName,
                            event.year,
                            event.month
                        )
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentPlaceName,
                        style = MaterialTheme.typography.titleMedium, // Smaller font
                        maxLines = 2,                                // Max 2 lines
                        overflow = TextOverflow.Ellipsis             // Ellipsize if too long
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Standard back icon
                            contentDescription = "Back" // For accessibility
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                is WeatherSummaryUiState.Loading -> {
                    LoadingStateView(modifier = Modifier.fillMaxSize())
                }
                is WeatherSummaryUiState.Success -> {
                    SuccessStateView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp), // Apply horizontal padding here
                        successState = state,
                        viewModel = vm
                    )
                }
                is WeatherSummaryUiState.Error -> {
                    ErrorStateView(
                        modifier = Modifier.fillMaxSize(),
                        errorState = state
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadMissingDaysDialog(
    showDialog: Boolean,
    dialogText: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Download full month?") },
            text = { Text(dialogText) },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("Download")
                }
            },
            dismissButton = {
                Button(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PlaceDetailsContent(
    modifier: Modifier = Modifier,
    currentPlaceDescription: String?,
    selectedYear: Int?,
    onYearSelected: (Int?) -> Unit,
    selectedMonth: Int?,
    onMonthSelected: (Int?) -> Unit,
    monthCompletionStatusMap: Map<Int, Int>, // Map of month to missing days count
    monthAverageTemps: Map<Int, Double?>, // Map of month to average temperature
    onPromptForMissingDays: (Int, Int, Int) -> Unit, // (year, month, missingDaysCount)
    viewModel: PlaceSummaryViewModel
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        currentPlaceDescription?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        YearDropdownSelector(
            selectedYear = selectedYear,
            onYearSelected = onYearSelected,
            modifier = Modifier.fillMaxWidth()
        )

        MonthSelectorWithTemperature(
            selectedMonth = selectedMonth,
            onMonthSelected = onMonthSelected,
            monthCompletionStatus = monthCompletionStatusMap,
            monthAverageTemps = monthAverageTemps,
            currentSelectedYear = selectedYear,
            onPromptForMissingDays = onPromptForMissingDays,
            modifier = Modifier.fillMaxWidth(),
            onDownloadedMonthSelected = { year, month ->
                viewModel.onShowMonth(year, month)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // The StoredDaysList is now removed from here
        // The content for the selected month will be shown in the MonthlySummaryView
    }
}

// private helper function to check month completion status (downloaded or not)
private fun calculateMonthCompletionStatusMap(
    selectedYear: Int?,
    storedDays: List<DayWeatherSummary> // Make sure DayWeatherSummary is the correct type
): Map<Int, MonthCompletionInfo> {
    if (selectedYear == null || selectedYear == Int.MIN_VALUE) {
        return emptyMap()
    }
    return (1..12).associateWith { monthIndex ->
        val firstDayOfMonth = LocalDate(selectedYear, monthIndex, 1)
        val totalDaysInMonth = firstDayOfMonth.plus(1, DateTimeUnit.MONTH)
            .minus(1, DateTimeUnit.DAY).dayOfMonth

        val presentDaysCount = storedDays.count { daySummary ->
            val (dYear, dMonth) = try {
                val dateStr = daySummary.date
                if (dateStr != null) {
                    val parts = dateStr.split('-')
                    if (parts.size >= 2) {
                        val year = parts[0].toIntOrNull()
                        val month = parts[1].toIntOrNull()
                        if (year != null && month != null) {
                            Pair(year, month)
                        } else {
                            Pair(-1, -1)
                        }
                    } else {
                        Pair(-1, -1)
                    }
                } else {
                    Pair(-1, -1)
                }
            } catch (e: Exception) {
                Napier.w("Error parsing date: ${daySummary.date}", e, tag = "PlaceSummaryView")
                Pair(-1, -1)
            }
            dYear == selectedYear && dMonth == monthIndex
        }
        MonthCompletionInfo(
            presentDaysCount = presentDaysCount,
            totalDaysInMonth = totalDaysInMonth,
            isFullyLoaded = presentDaysCount >= totalDaysInMonth
        )
    }
}

// Helper function to calculate missing days count from MonthCompletionInfo map
private fun calculateMissingDaysMap(
    detailedMap: Map<Int, MonthCompletionInfo>
): Map<Int, Int> {
    return detailedMap.mapValues { (_, info) ->
        maxOf(0, info.totalDaysInMonth - info.presentDaysCount)
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun SuccessStateView(
    modifier: Modifier = Modifier,
    successState: WeatherSummaryUiState.Success,
    viewModel: PlaceSummaryViewModel
) {
    // Get selected year from ViewModel (persists across navigation)
    val selectedYear by viewModel.selectedYear.collectAsState()
    var selectedMonth by remember { mutableStateOf<Int?>(null) }

    var showMissingDaysDialog by remember { mutableStateOf(false) }
    var yearToDownloadForDialog by remember { mutableStateOf<Int?>(null) }
    var monthToDownloadForDialog by remember { mutableStateOf<Int?>(null) }
    var missingDaysTextForDialog by remember { mutableStateOf("") }

    // This map holds MonthCompletionInfo (detailed)
    val detailedMonthCompletionStatusMap = remember(selectedYear, successState.storedDays) {
        calculateMonthCompletionStatusMap(selectedYear, successState.storedDays)
    }

    // Calculate missing days count for each month (0 = fully loaded, > 0 = missing days)
    val missingDaysMap = remember(detailedMonthCompletionStatusMap) {
        calculateMissingDaysMap(detailedMonthCompletionStatusMap)
    }

    // Get average temperature for each month from ViewModel
    val monthlyAverageTemps by viewModel.monthlyAverageTemps.collectAsState()

    LaunchedEffect(Unit) {
        if (selectedYear == Int.MIN_VALUE) { // Using a sentinel for first load
            viewModel.setSelectedYear(place.components.initialYear)
        }
    }

    LaunchedEffect(selectedYear) {
        selectedMonth = null // Reset month when year changes
        // Update month temperatures for the selected year
        if (selectedYear != Int.MIN_VALUE) {
            viewModel.updateMonthTemperaturesForYear(selectedYear)
        }
    }

    PlaceDetailsContent(
        modifier = modifier, // Pass modifier from SuccessStateView
        currentPlaceDescription = successState.currentPlaceDescription,
        selectedYear = selectedYear,
        onYearSelected = { year -> viewModel.setSelectedYear(year) },
        selectedMonth = selectedMonth,
        onMonthSelected = { month -> selectedMonth = month },
        monthCompletionStatusMap = missingDaysMap,
        monthAverageTemps = monthlyAverageTemps,
        onPromptForMissingDays = { yearArg, monthArg, missingDaysArg ->
            yearToDownloadForDialog = yearArg
            monthToDownloadForDialog = monthArg
            val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
            val monthName = monthNames.getOrElse(monthArg - 1) { "Month $monthArg" }
            missingDaysTextForDialog = if (missingDaysArg > 0) {
                "$monthName $yearArg is missing $missingDaysArg day${if (missingDaysArg > 1) "s" else ""}. Download missing data?"
            } else {
                "$monthName $yearArg is not yet downloaded. Download now?"
            }
            showMissingDaysDialog = true
        },
        viewModel = viewModel
    )

    DownloadMissingDaysDialog(
        showDialog = showMissingDaysDialog,
        dialogText = missingDaysTextForDialog,
        onDismissRequest = { showMissingDaysDialog = false },
        onConfirm = {
            if (yearToDownloadForDialog != null && monthToDownloadForDialog != null) {
                viewModel.onDownloadFullMonth( // Assuming this is the correct VM method
                    yearToDownloadForDialog!!,
                    monthToDownloadForDialog!!
                )
            }
            showMissingDaysDialog = false
        }
    )
}


@Composable
fun LoadingStateView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Loading weather summary...")
        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun ErrorStateView(
    modifier: Modifier = Modifier,
    errorState: WeatherSummaryUiState.Error
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error:",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = errorState.message, // Assuming Error state has a message
            color = MaterialTheme.colorScheme.error
        )
    }
}
