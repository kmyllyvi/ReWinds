package home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.PlaceButton
import core.GeoSearchResult
import core.isAndroid
import core.Navigator
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeView(vm: HomeViewModel = koinViewModel(), navigator: Navigator) {
    val uiState by vm.uiState.collectAsState()
    val searchText by vm.searchText.collectAsState()

    LaunchedEffect(Unit) {
        vm.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.ToPlaceSummary -> {
                    navigator.navigateToPlaceSummary(event.placeName)
                }
            }
        }
    }

    // Show error dialog if there is an error
    uiState.error?.let {
        ErrorDialog(
            error = it,
            onDismiss = vm::onErrorDismissed
        )
    }

    // Show delete confirmation dialog
    if (uiState.showDeleteConfirmation) {
        DeleteConfirmationDialog(
            placeName = uiState.placeToDelete ?: "",
            onConfirm = vm::onDeleteConfirmed,
            onDismiss = vm::onDeleteCancelled
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with title and settings icon
        HomeHeader(onSettingsClick = { vm.toggleDebugMenu() }, showDebug = uiState.showDebugMenu)

        // Scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Search section
            item {
                SearchWithSuggestions(
                    searchText = searchText,
                    onSearchTextChange = vm::onSearchTextChange,
                    isSearching = uiState.isSearching,
                    suggestions = uiState.searchResults,
                    onSuggestionSelected = vm::onSearchResultSelected
                )
            }

            // Places list
            if (uiState.placeDisplayData.isNotEmpty()) {
                item {
                    Text(
                        "Saved Locations",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(uiState.placeDisplayData) {
                    PlaceCell(
                        text = it.name,
                        dayCount = it.dayCount,
                        onClick = { vm.onSavedPlaceSelected(it.name) },
                        onDelete = { vm.onDeleteRequest(it.name) }
                    )
                }
            }

            // Debug menu
            if (uiState.showDebugMenu) {
                item {
                    DebugMenuSection(
                        onExport = { vm.exportDatabase() },
                        onListBackups = { vm.listBackups() },
                        importFilePath = uiState.importFilePath,
                        onImportFilePathChange = { vm.onImportFilePathChange(it) },
                        onImport = { vm.importDatabase() },
                        debugMessage = uiState.debugMessage
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorDialog(error: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API Error") },
        text = { Text(error) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun DeleteConfirmationDialog(placeName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Place") },
        text = { Text("Are you sure you want to delete '$placeName'? This action cannot be undone.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun HomeHeader(onSettingsClick: () -> Unit, showDebug: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Locations",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SearchWithSuggestions(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    isSearching: Boolean,
    suggestions: List<GeoSearchResult>,
    onSuggestionSelected: (GeoSearchResult) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Search field with iOS-style rounded corners
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ),
            label = { Text("Search locations") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        AnimatedVisibility(visible = isSearching) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        AnimatedVisibility(visible = suggestions.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                suggestions.forEach { suggestion ->
                    SuggestionCell(
                        name = suggestion.name,
                        region = suggestion.region ?: "",
                        country = suggestion.country ?: "",
                        onClick = { onSuggestionSelected(suggestion) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionCell(
    name: String,
    region: String,
    country: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "$region, $country",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlaceCell(
    text: String,
    dayCount: Int?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📍 $text",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (dayCount != null && dayCount > 0) {
                    Text(
                        text = "$dayCount ${if (dayCount == 1) "saved day" else "saved days"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Text(
                text = ">",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        Divider(
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun DebugMenuSection(
    onExport: () -> Unit,
    onListBackups: () -> Unit,
    importFilePath: String,
    onImportFilePathChange: (String) -> Unit,
    onImport: () -> Unit,
    debugMessage: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            "🛠️ Debug Tools",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📤 Export Database")
        }

        Button(
            onClick = onListBackups,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("📋 List Backups")
        }

        Text(
            "📥 Import Database",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = importFilePath,
            onValueChange = onImportFilePathChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("File path") },
            singleLine = true
        )

        Button(
            onClick = onImport,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Import")
        }

        if (debugMessage.isNotEmpty()) {
            Text(
                text = debugMessage,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
