package home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.AppHeader
import core.GeoSearchResult
import core.LocalAppStrings
import core.Navigator
import ui.components.IsobarBackground
import ui.theme.ReWindsColors
import ui.theme.rewinds
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeView(vm: HomeViewModel = koinViewModel(), navigator: Navigator) {
    val uiState by vm.uiState.collectAsState()
    val searchText by vm.searchText.collectAsState()
    val focusManager = LocalFocusManager.current
    val strings = LocalAppStrings.current

    LaunchedEffect(Unit) {
        vm.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.ToPlaceSummary -> {
                    navigator.navigateToPlaceSummary(event.placeName)
                }
            }
        }
    }

    // Refresh key state whenever this screen becomes active (e.g. returning from Settings).
    LaunchedEffect(Unit) {
        vm.refreshWeatherKeyState()
    }

    // Show error dialog if there is an error
    uiState.error?.let {
        ErrorDialog(
            error = it,
            onDismiss = vm::onErrorDismissed
        )
    }

    // Show VC key error dialog
    uiState.vcKeyError?.let { keyError ->
        VcKeyErrorDialog(
            errorType = keyError,
            onGoToSettings = {
                vm.onVcKeyErrorDismissed()
                navigator.navigateToSettings()
            },
            onDismiss = vm::onVcKeyErrorDismissed
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Decorative isobar background — rendered first so it sits below all content.
        IsobarBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            AppHeader(
                title = strings.appTitle,
                showLogo = true,
                titleSizeSp = 26,
                rightContent = {
                    IconButton(onClick = { navigator.navigateToSettings() }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = strings.settingsIconDesc,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { /* TODO: add-place action */ }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add place",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )

            // Non-dismissible VC key nudge banner
            AnimatedVisibility(
                visible = !uiState.isWeatherKeyConfigured,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                VcKeyNudgeBanner(onSetUpNow = { navigator.navigateToSettings() })
            }

            // Scrollable content — tap anywhere on the list area to dismiss keyboard
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
                // Search bar
                item {
                    PlacesSearchBar(
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

                // Alert banners — rendered when ViewModel exposes them, invisible otherwise
                if (uiState.alertBanners.isNotEmpty()) {
                    items(uiState.alertBanners) { banner ->
                        AlertBannerRow(banner = banner)
                    }
                }

                // Places list
                if (uiState.placeDisplayData.isNotEmpty()) {
                    items(uiState.placeDisplayData, key = { it.name }) { place ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    vm.onDeleteRequest(place.name)
                                }
                                false // always snap back — dialog handles actual deletion
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(13.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White)
                                }
                            }
                        ) {
                            PlaceRow(
                                place = place,
                                onClick = { vm.onSavedPlaceSelected(place.name) }
                            )
                        }
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
}

// ── Alert banners ────────────────────────────────────────────────────────────

/**
 * A single alert banner row. Amber for warnings, red for errors.
 * Background and border colours are derived from the token with opacity to keep
 * it consistent with the Midnight Blue palette spec.
 */
@Composable
private fun AlertBannerRow(banner: AlertBanner) {
    val baseColor = if (banner.isError) MaterialTheme.rewinds.error else MaterialTheme.rewinds.attention
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(
                color = baseColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = baseColor.copy(alpha = 0.28f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = baseColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = banner.message,
            style = MaterialTheme.typography.bodySmall,
            color = baseColor
        )
    }
}

// ── Place row ─────────────────────────────────────────────────────────────────

/**
 * A single place row: status dot, name, subtitle, chevron.
 * Status dot colour comes from [PlaceDisplayData.status] — never computed here.
 */
@Composable
private fun PlaceRow(place: PlaceDisplayData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .background(
                color = MaterialTheme.rewinds.surface.copy(alpha = 0.75f),
                shape = RoundedCornerShape(13.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status dot — colour is purely determined by VM state
        StatusDot(status = place.status)
        Spacer(modifier = Modifier.width(12.dp))

        // Name + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.rewinds.textPrimary
            )
            Text(
                text = place.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.rewinds.textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Chevron
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.rewinds.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** 6 dp filled circle whose colour maps to the [PlaceStatus] enum values. */
@Composable
private fun StatusDot(status: PlaceStatus) {
    val dotColor = when (status) {
        PlaceStatus.NORMAL  -> MaterialTheme.rewinds.accentBlue
        PlaceStatus.WARNING -> MaterialTheme.rewinds.attention
        PlaceStatus.ERROR   -> MaterialTheme.rewinds.error
    }
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(dotColor)
    )
}

// ── Search bar ───────────────────────────────────────────────────────────────

/**
 * Themed search bar using [ReWindsColors.surface] background, [ReWindsColors.border] outline,
 * and [ReWindsColors.textTertiary] placeholder — matching the design spec.
 */
@Composable
private fun PlacesSearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    isSearching: Boolean,
    suggestions: List<GeoSearchResult>,
    onSuggestionSelected: (GeoSearchResult) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        TextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.rewinds.border,
                    shape = RoundedCornerShape(10.dp)
                )
                .clip(RoundedCornerShape(10.dp)),
            placeholder = {
                Text(
                    strings.searchPlaceholder,
                    color = MaterialTheme.rewinds.textTertiary
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.rewinds.textTertiary
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = MaterialTheme.rewinds.surface,
                unfocusedContainerColor = MaterialTheme.rewinds.surface,
                focusedTextColor        = MaterialTheme.rewinds.textPrimary,
                unfocusedTextColor      = MaterialTheme.rewinds.textPrimary,
                cursorColor             = MaterialTheme.rewinds.accentBlue,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        AnimatedVisibility(visible = isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.rewinds.accentBlue)
            }
        }

        AnimatedVisibility(visible = suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(
                        color = MaterialTheme.rewinds.surface,
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
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
private fun SuggestionCell(name: String, region: String, country: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.rewinds.textPrimary
        )
        Text(
            text = "$region, $country",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.rewinds.textSecondary
        )
    }
}

// ── Banners & dialogs ────────────────────────────────────────────────────────

/**
 * Persistent nudge banner shown every launch until a Visual Crossing API key is configured.
 * Non-dismissible — it disappears only once the user saves a valid key in Settings.
 * Has its own background because it is a self-contained, boxed component (not a page element).
 */
@Composable
private fun VcKeyNudgeBanner(onSetUpNow: () -> Unit) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.vcKeyNudgeTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = strings.vcKeyNudgeBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        TextButton(onClick = onSetUpNow) {
            Text(
                text = strings.vcKeyNudgeAction,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VcKeyErrorDialog(
    errorType: VcKeyErrorType,
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val title   = if (errorType == VcKeyErrorType.MISSING) strings.vcKeyMissingTitle   else strings.vcKeyInvalidTitle
    val message = if (errorType == VcKeyErrorType.MISSING) strings.vcKeyMissingMessage else strings.vcKeyInvalidMessage
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = strings.warningIconDesc,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title   = { Text(title) },
        text    = { Text(message) },
        confirmButton = {
            Button(onClick = onGoToSettings) { Text(strings.goToSettings) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.dismiss) }
        }
    )
}

@Composable
private fun ErrorDialog(error: String, onDismiss: () -> Unit) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text(strings.apiError) },
        text    = { Text(error) },
        confirmButton = {
            Button(onClick = onDismiss) { Text(strings.ok) }
        }
    )
}

@Composable
private fun DeleteConfirmationDialog(placeName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text(strings.deletePlaceTitle) },
        text    = { Text(strings.deletePlaceMessage(placeName)) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(strings.delete) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

// ── Debug tools (dev-only section) ──────────────────────────────────────────

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
            "Debug Tools",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Text("Export Database")
        }

        Button(onClick = onListBackups, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("List Backups")
        }

        Text(
            "Import Database",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )

        TextField(
            value = importFilePath,
            onValueChange = onImportFilePathChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("File path") },
            singleLine = true
        )

        Button(onClick = onImport, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
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
