package place

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.AppHeader
import core.LocalAppStrings
import core.Navigator
import core.PlaceSummaryRoute
import core.TestTags
import org.koin.compose.viewmodel.koinViewModel
import place.components.initialYear
import ui.components.IsobarBackground
import ui.theme.rewinds

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
fun PlaceSummaryView(
    route: PlaceSummaryRoute,
    onBackClick: () -> Unit,
    navigator: Navigator,
    vm: PlaceSummaryViewModel = koinViewModel(key = route.placeName) { org.koin.core.parameter.parametersOf(route) }
) {
    val uiState by vm.uiState.collectAsState()
    val showMapModal by vm.showStationMap.collectAsState()
    val stationMapSummary by vm.stationMapSummary.collectAsState()
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

    // IMPORTANT: pageBg on the root Box — KIM-270 missed this and shipped a white background.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.rewinds.pageBg)
    ) {
        // Decorative isobar texture — lowest layer, below all content.
        IsobarBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = currentPlaceName,
                onBackClick = {
                    vm.refreshData()
                    onBackClick()
                },
                rightContent = {
                    // Map entry point — non-accent icon button, opens the station map sheet.
                    IconButton(
                        onClick = { vm.openStationMap() },
                        modifier = Modifier.testTag(TestTags.PLACE_STATION_MAP_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Map,
                            contentDescription = strings.openMapButton,
                            tint = MaterialTheme.rewinds.textPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            // "Ask AI about this place" — Chat tab resolves to (or creates)
                            // a session tagged with this place. initialMessage deep-link is
                            // still deferred (KIM-267).
                            navigator.navigateToChat(placeId = currentPlaceName)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = strings.chatButton,
                            tint = MaterialTheme.rewinds.accentBlue
                        )
                    }
                }
            )

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
                summary = stationMapSummary,
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

// ── Year selector strip ───────────────────────────────────────────────────────

/**
 * Horizontal scrollable year strip. The selected year is highlighted with
 * [ui.theme.ReWindsColors.accentBlue] text plus a pill underline; the rest use
 * [ui.theme.ReWindsColors.textSecondary]. Selection and scroll offset live in the
 * ViewModel, not in composable [remember] state.
 */
@Composable
private fun YearSelectorStrip(
    availableYears: List<Int>,
    selectedYear: Int?,
    initialScrollOffset: Int,
    onYearSelected: (Int) -> Unit,
    onScrollOffsetChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemScrollOffset = initialScrollOffset)

    // Persist the scroll offset back to the ViewModel so it survives navigation.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }.collect(onScrollOffsetChanged)
    }

    LazyRow(
        state = listState,
        modifier = modifier.testTag(TestTags.PLACE_YEAR_SELECTOR),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(availableYears) { year ->
            YearTab(
                year = year,
                isSelected = year == selectedYear,
                onClick = { onYearSelected(year) }
            )
        }
    }
}

@Composable
private fun YearTab(year: Int, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag(TestTags.PLACE_YEAR_TAB)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(
            text = year.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.rewinds.accentBlue else MaterialTheme.rewinds.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Pill indicator under the selected year only.
        Box(
            modifier = Modifier
                .height(3.dp)
                .width(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isSelected) MaterialTheme.rewinds.accentBlue else Color.Transparent
                )
        )
    }
}

// ── Month grid ────────────────────────────────────────────────────────────────

@Composable
private fun MonthGrid(
    monthCells: List<MonthCellInfo>,
    downloadingMonth: Int?,
    onMonthClick: (MonthCellInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.testTag(TestTags.PLACE_MONTH_GRID),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(monthCells.chunked(2)) { rowCells ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCells.forEach { cell ->
                    MonthCell(
                        cell = cell,
                        isDownloading = cell.month == downloadingMonth,
                        onClick = { onMonthClick(cell) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowCells.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * A single month grid cell. Appearance is a pure function of [MonthCellInfo.state]:
 * - FULL: solid [ui.theme.ReWindsColors.surface] card, month in textPrimary, day count in textSecondary
 * - PARTIAL: same card at reduced opacity, plus a "Partial" label
 * - NO_DATA: outline-only card (border token), month in textTertiary
 *
 * The state itself is computed in the ViewModel, never here.
 */
@Composable
private fun MonthCell(
    cell: MonthCellInfo,
    isDownloading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val monthName = getMonthShortName(cell.month)
    val shape = RoundedCornerShape(16.dp)

    val base = modifier
        .testTag(TestTags.PLACE_MONTH_CELL)
        .height(96.dp)
        .clip(shape)
        // Ignore taps while the month is downloading to avoid re-triggering the request.
        .clickable(enabled = !isDownloading, onClick = onClick)

    val styled = when (cell.state) {
        MonthCellState.FULL ->
            base.background(MaterialTheme.rewinds.surface)
        MonthCellState.PARTIAL ->
            base.background(MaterialTheme.rewinds.surface.copy(alpha = 0.5f))
        MonthCellState.NO_DATA ->
            base.border(1.dp, MaterialTheme.rewinds.border, shape)
    }

    val titleColor = when (cell.state) {
        MonthCellState.NO_DATA -> MaterialTheme.rewinds.textTertiary
        else -> MaterialTheme.rewinds.textPrimary
    }

    Box(modifier = styled.padding(14.dp)) {
        Column {
            Text(
                text = monthName,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isDownloading) {
                MonthCellDownloadingIndicator()
                return@Column
            }
            when (cell.state) {
                MonthCellState.NO_DATA -> {
                    Text(
                        text = strings.noStoredDays,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.rewinds.textTertiary
                    )
                }
                MonthCellState.FULL -> {
                    Text(
                        text = strings.monthDownloadedLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.rewinds.textSecondary
                    )
                }
                MonthCellState.PARTIAL -> {
                    Text(
                        text = strings.daysFraction(cell.presentDaysCount, cell.totalDaysInMonth),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.rewinds.textSecondary
                    )
                    Text(
                        text = strings.monthPartialLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.rewinds.attention,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Per-cell loading row shown inside a [MonthCell] while that month is being downloaded
 * on demand (KIM-332). A small spinner plus the shared "Downloading…" label, styled to
 * match the muted-feedback approach used by the other loading states.
 */
@Composable
private fun MonthCellDownloadingIndicator() {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier.testTag(TestTags.PLACE_MONTH_CELL_DOWNLOADING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.rewinds.accentBlue
        )
        Text(
            text = strings.downloading,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.rewinds.textSecondary
        )
    }
}

// ── Success state ─────────────────────────────────────────────────────────────

@Composable
private fun SuccessStateView(
    modifier: Modifier = Modifier,
    successState: WeatherSummaryUiState.Success,
    viewModel: PlaceSummaryViewModel
) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val yearScrollOffset by viewModel.yearScrollOffset.collectAsState()

    var showMissingDaysDialog by remember { mutableStateOf(false) }
    var yearToDownloadForDialog by remember { mutableStateOf<Int?>(null) }
    var monthToDownloadForDialog by remember { mutableStateOf<Int?>(null) }
    var missingDaysCountForDialog by remember { mutableStateOf(0) }

    val availableYears = remember { (2020..initialYear).toList().sortedDescending() }

    // Cell states come straight from the ViewModel; the grid only renders them.
    val monthCells = remember(selectedYear, successState.storedDays) {
        viewModel.calculateMonthCellStates(selectedYear, successState.storedDays)
    }

    LaunchedEffect(Unit) {
        if (selectedYear == Int.MIN_VALUE) { // Sentinel for first load
            viewModel.setSelectedYear(initialYear)
        }
    }

    Column(modifier = modifier) {
        successState.currentPlaceDescription?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.rewinds.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        YearSelectorStrip(
            availableYears = availableYears,
            selectedYear = selectedYear,
            initialScrollOffset = yearScrollOffset,
            onYearSelected = { viewModel.setSelectedYear(it) },
            onScrollOffsetChanged = { viewModel.setYearScrollOffset(it) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        MonthGrid(
            monthCells = monthCells,
            downloadingMonth = successState.downloadingMonth,
            onMonthClick = { cell ->
                val year = selectedYear
                if (year == null || year == Int.MIN_VALUE) return@MonthGrid
                if (cell.state == MonthCellState.NO_DATA) {
                    yearToDownloadForDialog = year
                    monthToDownloadForDialog = cell.month
                    missingDaysCountForDialog = cell.totalDaysInMonth - cell.presentDaysCount
                    showMissingDaysDialog = true
                } else {
                    viewModel.onShowMonth(year, cell.month)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    DownloadMissingDaysDialog(
        showDialog = showMissingDaysDialog,
        month = monthToDownloadForDialog,
        year = yearToDownloadForDialog,
        missingDaysCount = missingDaysCountForDialog,
        onDismissRequest = { showMissingDaysDialog = false },
        onConfirm = {
            val year = yearToDownloadForDialog
            val month = monthToDownloadForDialog
            if (year != null && month != null) {
                viewModel.onDownloadFullMonth(year, month)
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
        Text(strings.loadingSummary, color = MaterialTheme.rewinds.textSecondary)
        CircularProgressIndicator(
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.rewinds.accentBlue
        )
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
            color = MaterialTheme.rewinds.error
        )
        Text(
            text = errorState.message,
            color = MaterialTheme.rewinds.error
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
