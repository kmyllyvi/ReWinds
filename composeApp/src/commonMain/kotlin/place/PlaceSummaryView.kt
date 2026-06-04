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
import androidx.compose.material.icons.automirrored.filled.Chat
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
import core.LocalAppStrings
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
    val showMapModal by vm.showStationMap.collectAsState()
    val isRefreshingStations by vm.isRefreshingStations.collectAsState()
    val currentPlaceName = vm.placeName
    val strings = LocalAppStrings.current

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
                    onClick = { navigator.navigateToChat(initialMessage = "Chat about $currentPlaceName") }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Chat about $currentPlaceName",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(
                    onClick = { vm.openStationMap() },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = strings.infoIconDesc,
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
                stations = success.stations,
                isRefreshingStations = isRefreshingStations,
                stationsError = success.stationsError,
                onRefreshStations = { vm.refreshStations() },
                onDismiss = { vm.closeStationMap() }
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
    val strings = LocalAppStrings.current
    if (showDialog && month != null && year != null) {
        val monthName = getMonthFullName(month)
        val dialogText = if (missingDaysCount > 0) {
            strings.missingDaysMessage(monthName, year, missingDaysCount)
        } else {
            strings.notDownloadedMessage(monthName, year)
        }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(strings.downloadFullMonth) },
            text = { Text(dialogText) },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text(strings.download)
                }
            },
            dismissButton = {
                Button(onClick = onDismissRequest) {
                    Text(strings.cancel)
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
    val availableYears = remember { (2020..place.components.initialYear).toList().sortedDescending() }

    LazyColumn(modifier = modifier) {
        currentPlaceDescription?.let {
            item {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
        }

        // Year selector scrolls with content
        item {
            YearSelector(
                availableYears = availableYears,
                selectedYear = selectedYear,
                onYearSelected = { year -> onYearSelected(year) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Months grid
        if (selectedYear != null && selectedYear != Int.MIN_VALUE) {
            item {
                Text(
                    "$selectedYear",
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
                            year = selectedYear,
                            missingDaysCount = monthCompletionStatusMap[month] ?: 0,
                            temperature = monthAverageTemps[month],
                            modifier = Modifier.weight(1f),
                            onMonthSelected = {
                                onMonthSelected(month)
                                viewModel.onShowMonth(selectedYear, month)
                            },
                            onPromptForMissingDays = {
                                onPromptForMissingDays(selectedYear, month, monthCompletionStatusMap[month] ?: 0)
                            }
                        )
                    }
                    if (monthPair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
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
    val strings = LocalAppStrings.current
    val monthName = getMonthShortName(month)

    // Calculate total days in month and present days
    val totalDaysInMonth = getDaysInMonth(month, year)
    val presentDaysCount = totalDaysInMonth - missingDaysCount

    // Determine data status
    val isFullyLoaded = missingDaysCount == 0
    val isPartiallyLoaded = missingDaysCount > 0 && presentDaysCount > 0
    val hasNoData = presentDaysCount == 0

    // Background tint reflects data completeness: accent / attention / muted surface.
    val backgroundColor = when {
        isFullyLoaded    -> MaterialTheme.colorScheme.primaryContainer
        isPartiallyLoaded -> MaterialTheme.colorScheme.tertiaryContainer
        else             -> MaterialTheme.colorScheme.surfaceVariant
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
                val tempStr = "${kotlin.math.round(temperature * 10) / 10.0}"
                Text(strings.tempDisplay(tempStr), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(2.dp))
                // TODO: Show kiteable days count when available
                Text("⭐ X days", style = MaterialTheme.typography.bodySmall)
            } else {
                Text(
                    if (hasNoData) strings.noStoredDays else strings.daysFraction(presentDaysCount, totalDaysInMonth),
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
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(strings.loadingSummary)
        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun ErrorStateView(
    modifier: Modifier = Modifier,
    errorState: WeatherSummaryUiState.Error
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.errorLabel,
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
    val strings = LocalAppStrings.current
    return when (month) {
        1 -> strings.monthShortJan
        2 -> strings.monthShortFeb
        3 -> strings.monthShortMar
        4 -> strings.monthShortApr
        5 -> strings.monthShortMay
        6 -> strings.monthShortJun
        7 -> strings.monthShortJul
        8 -> strings.monthShortAug
        9 -> strings.monthShortSep
        10 -> strings.monthShortOct
        11 -> strings.monthShortNov
        12 -> strings.monthShortDec
        else -> strings.monthFallbackNumber(month)
    }
}

@Composable
private fun getMonthFullName(month: Int): String {
    val strings = LocalAppStrings.current
    return when (month) {
        1 -> strings.monthJanuary
        2 -> strings.monthFebruary
        3 -> strings.monthMarch
        4 -> strings.monthApril
        5 -> strings.monthMay
        6 -> strings.monthJune
        7 -> strings.monthJuly
        8 -> strings.monthAugust
        9 -> strings.monthSeptember
        10 -> strings.monthOctober
        11 -> strings.monthNovember
        12 -> strings.monthDecember
        else -> strings.monthFallbackNumber(month)
    }
}
