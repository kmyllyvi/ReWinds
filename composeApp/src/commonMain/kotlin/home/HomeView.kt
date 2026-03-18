package home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.PlaceButton
import components.AppHeader
import core.GeoSearchResult
import core.isAndroid
import core.Navigator
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import rewinds.composeapp.generated.resources.Res
import rewinds.composeapp.generated.resources.*

@Composable
fun HomeView(vm: HomeViewModel = koinViewModel(), navigator: Navigator) {
    val uiState by vm.uiState.collectAsState()
    val searchText by vm.searchText.collectAsState()
    val focusManager = LocalFocusManager.current

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
    ) {
        // Header with title and action buttons
        AppHeader(
            title = stringResource(Res.string.app_title),
            showLogo = true,
            rightContent = {
                IconButton(onClick = { navigator.navigateToSettings() }) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = stringResource(Res.string.settings_icon_desc),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = { navigator.navigateToChat() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Filled.Chat,
                        contentDescription = stringResource(Res.string.chat_icon_desc),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        )

        // Scrollable content with keyboard dismissal on click
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(
                    indication = null,
                    interactionSource = MutableInteractionSource()
                ) {
                    focusManager.clearFocus()
                },
                contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Search section
            item {
                SearchWithSuggestions(
                    searchText = searchText,
                    onSearchTextChange = vm::onSearchTextChange,
                    isSearching = uiState.isSearching,
                    suggestions = uiState.searchResults,
                    onSuggestionSelected = {
                        vm.onSearchResultSelected(it)
                        focusManager.clearFocus()
                    }
                )
            }

            // Places list
            if (uiState.placeDisplayData.isNotEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.saved_locations),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
        title = { Text(stringResource(Res.string.api_error)) },
        text = { Text(error) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.ok))
            }
        }
    )
}

@Composable
private fun DeleteConfirmationDialog(placeName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.delete_place_title)) },
        text = { Text(stringResource(Res.string.delete_place_message, placeName)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(Res.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
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
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
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
            placeholder = { Text(stringResource(Res.string.search_placeholder)) },
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📍 $text",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (dayCount != null && dayCount > 0) {
                    Text(
                        text = pluralStringResource(Res.plurals.days_stored, dayCount, dayCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
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
            .padding(horizontal = 12.dp, vertical = 16.dp)
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
