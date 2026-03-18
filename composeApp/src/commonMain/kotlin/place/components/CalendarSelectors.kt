package place.components

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import components.monthFullyLoadedColor
import components.monthNotLoadedColor
import components.monthPartiallyLoadedColor
import core.LocalAppStrings
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


// Basic helper, replace with kotlinx-datetime for accuracy
private fun getApproxDaysInMonth(month: Int, year: Int): Int {
    return when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28 // Basic leap year
        4, 6, 9, 11 -> 30
        else -> 31
    }
}


// Get current year dynamically - works across all platforms (iOS, Android)
@OptIn(kotlin.time.ExperimentalTime::class)
val initialYear: Int
    get() = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .year
val initialYearStr: String = initialYear.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YearDropdownSelector(
    selectedYear: Int?,
    onYearSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }
    // Fixed list of years (excluding current year since it's shown separately as default)
    val yearsToDisplay = remember { (2020..initialYear - 1).toList().sortedDescending() }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            strings.selectYear,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedYear?.toString() ?: initialYearStr,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                DropdownMenuItem(
                    text = { Text(initialYearStr) },
                    onClick = {
                        onYearSelected(null)
                        expanded = false
                    }
                )
                yearsToDisplay.forEach { year ->
                    DropdownMenuItem(
                        text = { Text(year.toString()) },
                        onClick = {
                            onYearSelected(year)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun MonthSelector(
    selectedMonth: Int?, // null means "All Months"
    onMonthSelected: (Int?) -> Unit, // Allow selecting "All Months" by passing null
    monthCompletionStatus: Map<Int, Int>, // Map of month (1-12) to missingDaysCount (0 = fully loaded, > 0 = missing days)
    currentSelectedYear: Int?, // To know which year to check for completeness for prompting
    onPromptForMissingDays: (Int, Int, Int) -> Unit, // (year, month, missingDaysCount)
    onDownloadedMonthSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(modifier = modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        Text(
            strings.selectMonth,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val months = (1..12).toList()
        val rows = months.chunked(4)

        rows.forEach { monthRow ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                monthRow.forEach { month ->
                    val missingDaysCount =
                        monthCompletionStatus[month] ?: 0 // Default to 0 (fully loaded) if not in map
                    val isFullyLoaded = missingDaysCount == 0

                    // Calculate if month is partially loaded (some data exists but not all)
                    val totalDaysInMonth = getApproxDaysInMonth(month, currentSelectedYear ?: 2025)
                    val loadedDays = totalDaysInMonth - missingDaysCount
                    val isPartiallyLoaded = loadedDays > 0 && missingDaysCount > 0

                    val monthItemOnClick = {
                        onMonthSelected(month) // Select the month visually first
                        if (currentSelectedYear != null) {
                            // If this month has any missing days => prompt to download
                            if (isFullyLoaded) {
                                // Already fully downloaded month selected
                                onDownloadedMonthSelected(currentSelectedYear, month)
                            } else {
                                // Month is partially or not yet downloaded
                                onPromptForMissingDays(currentSelectedYear, month, missingDaysCount)
                            }
                        }
                    }
                    MonthItem(
                        month = month,
                        isSelected = month == selectedMonth,
                        hasData = isFullyLoaded, // Pass fully loaded status (true only if 0 missing days)
                        isPartiallyLoaded = isPartiallyLoaded, // Show partial state only if some data is loaded
                        onClick = monthItemOnClick
                    )
                }
            }
        }
    }
}


@Composable
internal fun YearItem(
    year: Int,
    isSelected: Boolean,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onClick(year) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(text = year.toString())
    }
}

@Composable
internal fun MonthItem(
    month: Int, // 1 for January, 12 for December
    isSelected: Boolean,
    hasData: Boolean, // This now means "isFullyLoaded"
    onClick: () -> Unit,
    isPartiallyLoaded: Boolean = false, // New parameter for partial loading
    modifier: Modifier = Modifier
) {
    val monthNames = remember {
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
    }
    val monthName = monthNames.getOrElse(month - 1) { "N/A" }

    // Updated color logic:
    // Selected takes precedence.
    // If not selected: Dark green if fully loaded, Yellow if partially loaded, Gray if not loaded.
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        hasData -> monthFullyLoadedColor // Fully loaded (dark green)
        isPartiallyLoaded -> monthPartiallyLoadedColor // Partially loaded (yellow)
        else -> monthNotLoadedColor // Not loaded (gray)
    }
    val contentColor = Color.White


    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(text = monthName)
    }
}

@Composable
internal fun MonthSelectorWithTemperature(
    selectedMonth: Int?, // null means "All Months"
    onMonthSelected: (Int?) -> Unit,
    monthCompletionStatus: Map<Int, Int>, // Map of month (1-12) to missingDaysCount
    monthAverageTemps: Map<Int, Double?>, // Map of month to average temperature
    currentSelectedYear: Int?,
    onPromptForMissingDays: (Int, Int, Int) -> Unit,
    onDownloadedMonthSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            strings.selectMonth,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(12) { index ->
                val month = index + 1
                val missingDaysCount = monthCompletionStatus[month] ?: 0
                val isFullyLoaded = missingDaysCount == 0
                val totalDaysInMonth = getApproxDaysInMonth(month, currentSelectedYear ?: 2025)
                val loadedDays = totalDaysInMonth - missingDaysCount
                val isPartiallyLoaded = loadedDays > 0 && missingDaysCount > 0
                val avgTemp = monthAverageTemps[month]

                val monthItemOnClick = {
                    onMonthSelected(month)
                    if (currentSelectedYear != null) {
                        if (isFullyLoaded) {
                            onDownloadedMonthSelected(currentSelectedYear, month)
                        } else {
                            onPromptForMissingDays(currentSelectedYear, month, missingDaysCount)
                        }
                    }
                }

                MonthItemWithTemperature(
                    month = month,
                    monthName = getMonthFullName(month),
                    isSelected = month == selectedMonth,
                    hasData = isFullyLoaded,
                    isPartiallyLoaded = isPartiallyLoaded,
                    avgTemp = avgTemp,
                    onClick = monthItemOnClick
                )
            }
        }
    }
}

@Composable
internal fun MonthItemWithTemperature(
    month: Int,
    monthName: String,
    isSelected: Boolean,
    hasData: Boolean,
    isPartiallyLoaded: Boolean = false,
    avgTemp: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        hasData -> monthFullyLoadedColor // Fully loaded (dark green)
        isPartiallyLoaded -> monthPartiallyLoadedColor // Partially loaded (yellow)
        else -> monthNotLoadedColor // Not loaded (gray)
    }
    val contentColor = Color.White

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthName,
                style = MaterialTheme.typography.bodyMedium
            )

            avgTemp?.let {
                Text(
                    text = "${(it * 10).toInt() / 10.0}\u00B0",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Kept for reference or if a different year selection style is desired later
@Composable
internal fun YearSelector(
    availableYears: List<Int>,
    selectedYear: Int?,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    if (availableYears.isEmpty()) {
        Text(strings.noDataAvailable, modifier = modifier.padding(8.dp))
        return
    }
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            strings.selectYear,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(availableYears.sortedDescending()) { year -> // Show most recent years first
                YearItem(
                    year = year,
                    isSelected = year == selectedYear,
                    onClick = onYearSelected
                )
            }
        }
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
