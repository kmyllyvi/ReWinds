package home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.AppHeader
import core.GeoSearchResult
import core.LocalAppStrings
import core.Navigator
import core.TestTags
import ui.components.ArchiveBoxIcon
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

    // Archive confirmation modal — nothing is archived until this is confirmed.
    uiState.archiveConfirmationPlace?.let { placeName ->
        ArchiveConfirmationDialog(
            placeName = placeName,
            onConfirm = vm::onArchiveConfirmed,
            onDismiss = vm::onArchiveCancelled
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
                titleSizeSp = 22,
                titleFontWeight = FontWeight.Light
            )

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

                // Skeleton placeholders while the first load is in flight and we have
                // nothing to show yet — gives immediate feedback instead of a blank list.
                if (uiState.isLoading && uiState.placeDisplayData.isEmpty()) {
                    items(3) {
                        PlaceRowSkeleton()
                    }
                }

                // Empty state — no places and not loading. Minimal single row (KIM-365).
                if (!uiState.isLoading && uiState.placeDisplayData.isEmpty()) {
                    item {
                        EmptyPlacesRow()
                    }
                }

                // Places list
                if (uiState.placeDisplayData.isNotEmpty()) {
                    items(uiState.placeDisplayData, key = { it.name }) { place ->
                        PlaceRow(
                            place = place,
                            onClick = { vm.onSavedPlaceSelected(place.name) },
                            onLongPress = { vm.onPlaceLongPressed(place.name) },
                            menuExpanded = uiState.archiveMenuPlace == place.name,
                            onMenuDismiss = vm::onArchiveMenuDismissed,
                            onArchive = vm::onArchiveRequested
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
 * Tap opens the place; long-press opens the archive action menu (KIM-365).
 * Status dot colour comes from [PlaceDisplayData.status] — never computed here.
 * Menu visibility is driven entirely by [menuExpanded] (VM state), not local state.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaceRow(
    place: PlaceDisplayData,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    menuExpanded: Boolean,
    onMenuDismiss: () -> Unit,
    onArchive: () -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.HOME_PLACE_ROW)
                .padding(horizontal = 12.dp, vertical = 5.dp)
                .background(
                    color = MaterialTheme.rewinds.surface.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(13.dp)
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongPress)
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

        // Long-press action menu — single "Archive" item, never "Delete".
        PlaceActionMenu(
            expanded = menuExpanded,
            onDismiss = onMenuDismiss,
            onArchive = onArchive
        )
    }
}

/**
 * Dropdown action menu shown on long-press of a place row. Contains exactly one
 * item — "Archive" — with a dedicated archive-box icon distinct from the trash icon.
 */
@Composable
private fun PlaceActionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onArchive: () -> Unit
) {
    val strings = LocalAppStrings.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.HOME_PLACE_ACTION_MENU)
    ) {
        DropdownMenuItem(
            text = { Text(strings.archivePlaceAction) },
            onClick = onArchive,
            leadingIcon = {
                Icon(
                    imageVector = ArchiveBoxIcon,
                    contentDescription = strings.archivePlaceIconDesc,
                    tint = MaterialTheme.rewinds.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.testTag(TestTags.HOME_ARCHIVE_ACTION)
        )
    }
}

/**
 * Minimal empty-state row shown when there are no saved places (KIM-365).
 * A single row — icon + one line + a search hint — not a full onboarding screen.
 */
@Composable
private fun EmptyPlacesRow() {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.HOME_EMPTY_STATE)
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .background(
                color = MaterialTheme.rewinds.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(13.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ArchiveBoxIcon,
            contentDescription = null,
            tint = MaterialTheme.rewinds.textTertiary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.noPlacesTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.rewinds.textPrimary
            )
            Text(
                text = strings.noPlacesHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.rewinds.textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Muted placeholder shaped like a [PlaceRow], shown while the places list loads.
 * Two grey bars stand in for the name and subtitle. No shimmer — a static muted block
 * is enough feedback and keeps the loading state cheap.
 */
@Composable
private fun PlaceRowSkeleton() {
    val placeholderColor = MaterialTheme.rewinds.textTertiary.copy(alpha = 0.18f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .background(
                color = MaterialTheme.rewinds.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(13.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(placeholderColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholderColor)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.32f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholderColor)
            )
        }
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
    Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 4.dp)) {
        TextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.HOME_SEARCH_FIELD)
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
            .testTag(TestTags.HOME_SEARCH_SUGGESTION)
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

// ── Dialogs ──────────────────────────────────────────────────────────────────

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
private fun ArchiveConfirmationDialog(
    placeName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = ArchiveBoxIcon,
                contentDescription = strings.archivePlaceIconDesc,
                tint = MaterialTheme.rewinds.accentBlue
            )
        },
        title = { Text(strings.archivePlaceTitle) },
        text = { Text(strings.archivePlaceMessage(placeName)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.HOME_ARCHIVE_CONFIRM)
            ) { Text(strings.archivePlaceConfirm) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
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
