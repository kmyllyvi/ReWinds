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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.MonthlyStatisticsRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MonthlyStatisticsView(
    placeName: String,
    year: Int,
    month: Int, // Unused - kept for compatibility, shows months grid instead
    onBackClick: () -> Unit,
    onMonthClick: (Int, Int) -> Unit = { _, _ -> }, // year, month
    vm: MonthlyStatisticsViewModel = koinViewModel {
        parametersOf(MonthlyStatisticsRoute(placeName, year, month))
    }
) {
    // For now, fetch some data to work with
    val statistics by vm.statistics.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("Months data", style = MaterialTheme.typography.labelSmall)
                    Text(placeName, style = MaterialTheme.typography.titleMedium)
                }
            }
            IconButton(
                onClick = {},
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Months Grid
        if (statistics == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show months grid grouped by year
                // For now, show a sample month card structure
                items(1) {
                    MonthsGrid(
                        placeName = placeName,
                        onMonthClick = onMonthClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthsGrid(
    placeName: String,
    onMonthClick: (Int, Int) -> Unit
) {
    // TODO: Get actual months data from viewmodel
    // For now, show sample layout

    Column {
        Text("2026", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MonthCard(
                monthName = "Jan",
                dayCount = 26,
                temperature = "1.5°C",
                kiteableDays = 3,
                hasData = true,
                modifier = Modifier.weight(1f),
                onClick = { onMonthClick(2026, 1) }
            )
            MonthCard(
                monthName = "Feb",
                dayCount = 26,
                temperature = null,
                kiteableDays = null,
                hasData = false,
                status = "current",
                modifier = Modifier.weight(1f),
                onClick = { }
            )
        }
    }
}

@Composable
private fun MonthCard(
    monthName: String,
    dayCount: Int,
    temperature: String?,
    kiteableDays: Int?,
    hasData: Boolean,
    status: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = if (hasData) {
        Color(0xFFe2f2ce) // Light green
    } else {
        Color(0xFFF0F0F0) // Light gray
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(enabled = hasData) { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Text(
                "$monthName $dayCount",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (hasData && temperature != null && kiteableDays != null) {
                Text("Temp: $temperature", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐", modifier = Modifier.padding(end = 4.dp))
                    Text("$kiteableDays days", style = MaterialTheme.typography.bodySmall)
                }
            } else if (!hasData) {
                Text(
                    status ?: "Nothing here yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
