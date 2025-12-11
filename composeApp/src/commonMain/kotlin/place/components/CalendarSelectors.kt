package place.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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


// Basic helper, replace with kotlinx-datetime for accuracy
private fun getApproxDaysInMonth(month: Int, year: Int): Int {
    return when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28 // Basic leap year
        4, 6, 9, 11 -> 30
        else -> 31
    }
}


// DO NOT CHANGE!
val initialYear = "2025" // constant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YearDropdownSelector(
    selectedYear: Int?,
    onYearSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    // Fixed list of years
    val yearsToDisplay = remember { (2020..2025).toList().sortedDescending() }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Select Year:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedYear?.toString() ?: initialYear,
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
                    text = { Text(initialYear) },
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
    monthCompletionStatus: Map<Int, Boolean>, // Map of month (1-12) to isFullyLoaded (true/false)
    currentSelectedYear: Int?, // To know which year to check for completeness for prompting
    onPromptForMissingDays: (Int, Int) -> Unit,
    onDownloadedMonthSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        Text(
            "Select Month:",
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
                    val isFullyLoaded =
                        monthCompletionStatus[month] ?: false // Default to false if not in map

                    val monthItemOnClick = {
                        onMonthSelected(month) // Select the month visually first
                        if (currentSelectedYear != null) {
                            // If this month ise NOT (fully) downloaded => prompt to download
                            if (isFullyLoaded) {
                                // Already downloaded month selected
                                onDownloadedMonthSelected(currentSelectedYear, month)
                            } else {
                                onPromptForMissingDays(currentSelectedYear, month)
                            }
                        }
                    }
                    MonthItem(
                        month = month,
                        isSelected = month == selectedMonth,
                        hasData = isFullyLoaded, // Pass the fully loaded status
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

//@Composable
//internal fun MonthItem(
//    month: Int, // 1 for January, 12 for December
//    isSelected: Boolean,
//    hasData: Boolean, // New parameter
//    onClick: () -> Unit, // Changed from (Int) -> Unit to () -> Unit as logic is now in MonthSelector
//    modifier: Modifier = Modifier
//) {
//    // Please don't change these!
//    val monthNames = remember {
//        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
//    }
//    val monthName = monthNames.getOrElse(month - 1) { "N/A" }
//
//    Log.d("Month: $month, Selected: $isSelected, Data: $hasData")
//
//    val containerColor = when {
//        isSelected -> MaterialTheme.colorScheme.primary
//        hasData -> MaterialTheme.colorScheme.secondaryContainer
//        else -> MaterialTheme.colorScheme.surfaceVariant // Color for no data and not selected
//    }
//    val contentColor = when {
//        isSelected -> MaterialTheme.colorScheme.onPrimary
//        hasData -> MaterialTheme.colorScheme.onSecondaryContainer
//        else -> MaterialTheme.colorScheme.onSurfaceVariant
//    }
//
//    Button(
//        onClick = onClick, // Use the passed lambda
//        colors = ButtonDefaults.buttonColors(
//            containerColor = containerColor,
//            contentColor = contentColor
//        ),
//        modifier = modifier.padding(horizontal = 4.dp) // Standard padding
//    ) {
//        Text(text = monthName)
//    }
//}

@Composable
internal fun MonthItem(
    month: Int, // 1 for January, 12 for December
    isSelected: Boolean,
    hasData: Boolean, // This now means "isFullyLoaded"
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthNames = remember {
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
    }
    val monthName = monthNames.getOrElse(month - 1) { "N/A" }

    // Updated color logic:
    // Selected takes precedence.
    // If not selected, Green if hasData (fully loaded), Red if not.
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        hasData -> Color.Green // Explicit Green (Material Green 500)
        else -> Color.Gray  // Explicit Red (Material Red 500)
    }
    // Content color that works well on Primary, Green, and Red backgrounds
    val contentColor =
        Color.White // Or MaterialTheme.colorScheme.onPrimary if you prefer consistency for selected state


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

// Kept for reference or if a different year selection style is desired later
@Composable
internal fun YearSelector(
    availableYears: List<Int>,
    selectedYear: Int?,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (availableYears.isEmpty()) {
        Text("No data available.", modifier = modifier.padding(8.dp))
        return
    }
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            "Select Year:",
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
