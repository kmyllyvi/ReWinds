package home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.DebugMenu
import components.PlaceButton
import core.DatabaseExportImport
import core.GeoSearchResult
import core.Navigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeView(vm: HomeViewModel = koinViewModel(), navigator: Navigator) {
    val uiState by vm.uiState.collectAsState()
    val searchText by vm.searchText.collectAsState()
    val databaseExportImport: DatabaseExportImport = koinInject()

    var showDebugMenu by remember { mutableStateOf(false) }
    var debugMessage by remember { mutableStateOf("") }

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
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SearchWithSuggestions(
            searchText = searchText,
            onSearchTextChange = vm::onSearchTextChange,
            isSearching = uiState.isSearching,
            suggestions = uiState.searchResults,
            onSuggestionSelected = vm::onSearchResultSelected
        )

        PlaceSelector(
            places = uiState.placeDisplayData,
            onPlaceSelected = vm::onSavedPlaceSelected,
            onDeleteClicked = vm::onDeleteRequest
        )

        // Debug menu (for development)
        if (showDebugMenu) {
            DebugMenu(
                databaseExportImport = databaseExportImport,
                onExportResult = { message ->
                    debugMessage = message
                },
                onImportResult = { message ->
                    debugMessage = message
                },
                onBackupListResult = { backups ->
                    debugMessage = if (backups.isEmpty()) {
                        "No backups found"
                    } else {
                        "Found ${backups.size} backups:\n${backups.joinToString("\n")}"
                    }
                }
            )
        }

        // Debug toggle button
        Button(
            onClick = { showDebugMenu = !showDebugMenu },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(if (showDebugMenu) "Hide Debug Menu" else "Show Debug Menu")
        }

        // Show debug messages
        if (debugMessage.isNotEmpty()) {
            Text(
                text = debugMessage,
                modifier = Modifier.padding(8.dp),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
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
private fun SearchWithSuggestions(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    isSearching: Boolean,
    suggestions: List<GeoSearchResult>,
    onSuggestionSelected: (GeoSearchResult) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search for a place") },
            // leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
            singleLine = true
        )

        AnimatedVisibility(visible = isSearching) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        AnimatedVisibility(visible = suggestions.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(suggestions, key = { it.id }) {
                    ListItem(
                        headlineContent = { Text(it.name) },
                        supportingContent = { Text("${it.region ?: ""}, ${it.country ?: ""}") },
                        modifier = Modifier.clickable { onSuggestionSelected(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceSelector(
    places: List<PlaceDisplayData>,
    onPlaceSelected: (String) -> Unit,
    onDeleteClicked: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(places) {
            PlaceButton(
                text = it.name,
                onClick = onPlaceSelected,
                onDelete = onDeleteClicked,
                dayCount = it.dayCount
            )
        }
    }
}
