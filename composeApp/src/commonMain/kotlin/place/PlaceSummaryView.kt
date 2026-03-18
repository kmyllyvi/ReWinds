package place

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
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
import core.Navigator
import core.PlaceSummaryRoute
import io.github.aakira.napier.Napier
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import place.components.YearSelector
import components.AppHeader
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import rewinds.composeapp.generated.resources.Res
import rewinds.composeapp.generated.resources.*

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

// Helper to get days in month (accounting for leap years)
internal fun getDaysInMonth(month: Int, year: Int): Int {
    return when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28 // Leap year
        4, 6, 9, 11 -> 30
        else -> 31
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSummaryView(
    route: PlaceSummaryRoute,
    onBackClick: () -> Unit,
    navigator: Navigator,
    vm: PlaceSummaryViewModel = koinViewModel(key = route.placeName) { org.koin.core.parameter.parametersOf(route) }
) {
    val uiState by vm.uiState.collectAsState()
    val currentPlaceName = vm.placeName // Access it directly
    var showMapModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.ToMonthlySummary -> {
                    navigator.navigateToMonthlyStatistics(
                        event.placeName,
                        event.year,
                        event.month
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Header
        AppHeader(
            title = currentPlaceName,
            onBackClick = {
                vm.refreshData()
                onBackClick()
            },
            rightContent = {
                IconButton(
                    onClick = { showMapModal = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = stringResource(Res.string.info_icon_desc),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )

        // Content
        when (val state = uiState) {
            is WeatherSummaryUiState.Loading -> {
                LoadingStateView(modifier = Modifier.fillMaxSize())
            }
            is WeatherSummaryUiState.Success -> {
                SuccessStateView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
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

    // Show map modal when latitude/longitude are available
    if (showMapModal && uiState is WeatherSummaryUiState.Success) {
        val success = uiState as WeatherSummaryUiState.Success
        if (success.latitude != null && success.longitude != null) {
            StationMapModal(
                lat = success.latitude,
                lon = success.longitude,
                placeName = currentPlaceName,
                onDismiss = { showMapModal = false }
            )
        }
    }
}

@Composable
private fun DownloadMissingDaysDialog(
    showDialog: Boolean,
    month: Int?,
    year: Int?,
    missingDaysCount: Int,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog && month != null && year != null) {
        val monthName = getMonthFullName(month)
        val dialogText = if (missingDaysCount > 0) {
            stringResource(Res.string.missing_days_message, monthName, year, missingDaysCount)
        } else {
            stringResource(Res.string.not_downloaded_message, monthName, year)
        }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(Res.string.download_full_month)) },
            text = { Text(dialogText) },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text(stringResource(Res.string.download))
                }
            },
            dismissButton = {
                Button(onClick = onDismissRequest) {
                    Text(stringResource(Res.string.cancel))
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
        modifier = modifier
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

        // Year selector - horizontal scrolling list
        val availableYears = remember { (2020..place.components.initialYear).toList().sortedDescending() }
        YearSelector(
            availableYears = availableYears,
            selectedYear = selectedYear,
            onYearSelected = { year -> onYearSelected(year) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Months grid
        if (selectedYear != null && selectedYear != Int.MIN_VALUE) {
            MonthsGridLayout(
                year = selectedYear,
                monthCompletionStatus = monthCompletionStatusMap,
                monthAverageTemps = monthAverageTemps,
                onMonthSelected = { month ->
                    onMonthSelected(month)
                    viewModel.onShowMonth(selectedYear, month)
                },
                onPromptForMissingDays = onPromptForMissingDays
            )
        }
    }
}

@Composable
private fun MonthsGridLayout(
    year: Int,
    monthCompletionStatus: Map<Int, Int>,
    monthAverageTemps: Map<Int, Double?>,
    onMonthSelected: (Int) -> Unit,
    onPromptForMissingDays: (Int, Int, Int) -> Unit
) {
    LazyColumn {
        item {
            Text(
                "$year",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items((1..12).chunked(2)) { monthPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                monthPair.forEach { month ->
                    MonthCardForGrid(
                        month = month,
                        year = year,
                        missingDaysCount = monthCompletionStatus[month] ?: 0,
                        temperature = monthAverageTemps[month],
                        modifier = Modifier.weight(1f),
                        onMonthSelected = { onMonthSelected(month) },
                        onPromptForMissingDays = { onPromptForMissingDays(year, month, monthCompletionStatus[month] ?: 0) }
                    )
                }
                // Add spacer if odd number of months
                if (monthPair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MonthCardForGrid(
    month: Int,
    year: Int,
    missingDaysCount: Int,
    temperature: Double?,
    modifier: Modifier = Modifier,
    onMonthSelected: () -> Unit,
    onPromptForMissingDays: () -> Unit
) {
    val monthName = getMonthShortName(month)

    // Calculate total days in month and present days
    val totalDaysInMonth = getDaysInMonth(month, year)
    val presentDaysCount = totalDaysInMonth - missingDaysCount

    // Determine data status
    val isFullyLoaded = missingDaysCount == 0
    val isPartiallyLoaded = missingDaysCount > 0 && presentDaysCount > 0
    val hasNoData = presentDaysCount == 0

    // Color based on completion status
    val backgroundColor = when {
        isFullyLoaded -> Color(0xFFe2f2ce) // Light green - fully loaded
        isPartiallyLoaded -> Color(0xFFf5e6cc) // Light tan/orange - partially loaded
        else -> Color(0xFFF0F0F0) // Light gray - no data
    }

    Box(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable {
                if (hasNoData) {
                    // No data at all - prompt to download
                    onPromptForMissingDays()
                } else {
                    // Has some data (full or partial) - show the data
                    onMonthSelected()
                }
            }
            .padding(16.dp)
    ) {
        Column {
            Text(
                "$monthName $year",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (isFullyLoaded && temperature != null) {
                val tempStr = kotlin.math.round(temperature * 10) / 10.0
                Text(stringResource(Res.string.temp_display, tempStr), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(2.dp))
                // TODO: Show kiteable days count when available
                Text("⭐ X days", style = MaterialTheme.typography.bodySmall)
            } else {
                Text(
                    if (hasNoData) stringResource(Res.string.no_stored_days) else stringResource(Res.string.days_fraction, presentDaysCount, totalDaysInMonth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
    var missingDaysCountForDialog by remember { mutableStateOf(0) }

    // This map holds MonthCompletionInfo (detailed)
    val detailedMonthCompletionStatusMap = remember(selectedYear, successState.storedDays) {
        viewModel.calculateMonthCompletionStatusMap(selectedYear, successState.storedDays)
    }

    // Calculate missing days count for each month (0 = fully loaded, > 0 = missing days)
    val missingDaysMap = remember(detailedMonthCompletionStatusMap) {
        viewModel.calculateMissingDaysMap(detailedMonthCompletionStatusMap)
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
            missingDaysCountForDialog = missingDaysArg
            showMissingDaysDialog = true
        },
        viewModel = viewModel
    )

    DownloadMissingDaysDialog(
        showDialog = showMissingDaysDialog,
        month = monthToDownloadForDialog,
        year = yearToDownloadForDialog,
        missingDaysCount = missingDaysCountForDialog,
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
        Text(stringResource(Res.string.loading_summary))
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
            text = stringResource(Res.string.error_label),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = errorState.message, // Assuming Error state has a message
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun getMonthShortName(month: Int): String {
    return when (month) {
        1 -> stringResource(Res.string.month_short_jan)
        2 -> stringResource(Res.string.month_short_feb)
        3 -> stringResource(Res.string.month_short_mar)
        4 -> stringResource(Res.string.month_short_apr)
        5 -> stringResource(Res.string.month_short_may)
        6 -> stringResource(Res.string.month_short_jun)
        7 -> stringResource(Res.string.month_short_jul)
        8 -> stringResource(Res.string.month_short_aug)
        9 -> stringResource(Res.string.month_short_sep)
        10 -> stringResource(Res.string.month_short_oct)
        11 -> stringResource(Res.string.month_short_nov)
        12 -> stringResource(Res.string.month_short_dec)
        else -> stringResource(Res.string.month_fallback_number, month)
    }
}

@Composable
private fun getMonthFullName(month: Int): String {
    return when (month) {
        1 -> stringResource(Res.string.month_january)
        2 -> stringResource(Res.string.month_february)
        3 -> stringResource(Res.string.month_march)
        4 -> stringResource(Res.string.month_april)
        5 -> stringResource(Res.string.month_may)
        6 -> stringResource(Res.string.month_june)
        7 -> stringResource(Res.string.month_july)
        8 -> stringResource(Res.string.month_august)
        9 -> stringResource(Res.string.month_september)
        10 -> stringResource(Res.string.month_october)
        11 -> stringResource(Res.string.month_november)
        12 -> stringResource(Res.string.month_december)
        else -> stringResource(Res.string.month_fallback_number, month)
    }
}
