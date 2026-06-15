package settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import components.AppHeader
import core.ApiKeyManager
import core.Language
import core.LocalAppStrings
import core.Navigator
import core.TestTags
import core.WeatherApiKeyManager
import core.deleteApiKeyPlatform
import core.deleteWeatherApiKeyPlatform
import core.filterSummary
import core.saveApiKeyPlatform
import core.saveWeatherApiKeyPlatform
import org.koin.compose.viewmodel.koinViewModel
import settings.components.SettingsDestructiveRow
import settings.components.SettingsGroup
import settings.components.SettingsKeyRow
import settings.components.SettingsToggleRow
import settings.components.SettingsValueRow
import ui.components.IsobarBackground
import ui.theme.rewinds

/** Identifies which editor/dialog is currently open. Pure UI navigation state. */
private enum class SettingsDialog { NONE, ANTHROPIC_KEY, WEATHER_KEY, DAYS_OF_INTEREST }

@Composable
fun SettingsView(
    navigator: Navigator,
    vm: SettingsViewModel = koinViewModel()
) {
    val scrollState = rememberScrollState()
    val strings = LocalAppStrings.current

    val language by vm.languageState.collectAsState()
    val units by vm.unitsState.collectAsState()
    val windSpeedUnit by vm.windSpeedUnitState.collectAsState()
    val anthropicConfigured by vm.anthropicKeyConfigured.collectAsState()
    val weatherConfigured by vm.visualCrossingKeyConfigured.collectAsState()
    val autoRefresh by vm.autoRefreshEnabled.collectAsState()
    val wifiOnly by vm.wifiOnlyEnabled.collectAsState()

    var openDialog by remember { mutableStateOf(SettingsDialog.NONE) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.rewinds.pageBg)
    ) {
        // Decorative isobar texture — lowest layer, below all content.
        IsobarBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(title = strings.settingsTitle)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ── General ─────────────────────────────────────────────────
                SettingsGroup(
                    label = strings.settingsGroupGeneral,
                    rows = listOf(
                        {
                            SettingsValueRow(
                                label = strings.languageTitle,
                                value = language.displayName,
                                modifier = Modifier.testTag(TestTags.SETTINGS_LANGUAGE_ROW),
                                onClick = {
                                    // Single toggle between the two supported languages.
                                    vm.setLanguage(
                                        if (language == Language.ENGLISH) Language.GERMAN
                                        else Language.ENGLISH
                                    )
                                }
                            )
                        },
                        {
                            SettingsValueRow(
                                label = strings.settingsRowUnits,
                                value = units.displayName,
                                modifier = Modifier.testTag(TestTags.SETTINGS_UNITS_ROW),
                                onClick = {
                                    vm.setUnits(
                                        if (units == UnitSystem.METRIC) UnitSystem.IMPERIAL
                                        else UnitSystem.METRIC
                                    )
                                }
                            )
                        },
                        {
                            SettingsValueRow(
                                label = strings.settingsRowWindSpeed,
                                value = windSpeedUnit.displayName,
                                modifier = Modifier.testTag(TestTags.SETTINGS_WIND_SPEED_ROW),
                                onClick = {
                                    // Cycle through the supported wind units.
                                    val next = when (windSpeedUnit) {
                                        WindSpeedUnit.KMH -> WindSpeedUnit.KNOTS
                                        WindSpeedUnit.KNOTS -> WindSpeedUnit.MPH
                                        WindSpeedUnit.MPH -> WindSpeedUnit.KMH
                                    }
                                    vm.setWindSpeedUnit(next)
                                }
                            )
                        }
                    )
                )

                // ── API Keys ────────────────────────────────────────────────
                SettingsGroup(
                    label = strings.settingsGroupApiKeys,
                    rows = listOf(
                        {
                            SettingsKeyRow(
                                title = strings.anthropicKeyTitle,
                                subLabel = strings.settingsAnthropicSubLabel,
                                configured = anthropicConfigured,
                                configuredChipText = strings.settingsKeyConfiguredChip,
                                notSetChipText = strings.settingsKeyNotSetChip,
                                modifier = Modifier.testTag(TestTags.SETTINGS_ANTHROPIC_KEY_ROW),
                                onClick = { openDialog = SettingsDialog.ANTHROPIC_KEY }
                            )
                        },
                        {
                            SettingsKeyRow(
                                title = strings.visualCrossingKeyTitle,
                                subLabel = strings.settingsVisualCrossingSubLabel,
                                configured = weatherConfigured,
                                configuredChipText = strings.settingsKeyConfiguredChip,
                                notSetChipText = strings.settingsKeyNotSetChip,
                                modifier = Modifier.testTag(TestTags.SETTINGS_WEATHER_KEY_ROW),
                                onClick = { openDialog = SettingsDialog.WEATHER_KEY }
                            )
                        }
                    )
                )

                // ── Data ────────────────────────────────────────────────────
                SettingsGroup(
                    label = strings.settingsGroupData,
                    rows = listOf(
                        {
                            SettingsToggleRow(
                                label = strings.settingsRowAutoRefresh,
                                checked = autoRefresh,
                                onCheckedChange = { vm.setAutoRefreshEnabled(it) }
                            )
                        },
                        {
                            SettingsToggleRow(
                                label = strings.settingsRowWifiOnly,
                                checked = wifiOnly,
                                onCheckedChange = { vm.setWifiOnlyEnabled(it) }
                            )
                        },
                        {
                            SettingsValueRow(
                                label = strings.daysOfInterestTitle,
                                onClick = { openDialog = SettingsDialog.DAYS_OF_INTEREST }
                            )
                        },
                        {
                            SettingsValueRow(
                                label = strings.settingsRowExportData,
                                onClick = { /* Routed to the existing export flow elsewhere. */ }
                            )
                        }
                    )
                )

                // ── About ───────────────────────────────────────────────────
                SettingsGroup(
                    label = strings.settingsGroupAbout,
                    rows = listOf(
                        {
                            SettingsValueRow(
                                label = strings.settingsRowVersion,
                                value = strings.settingsAppVersion,
                                showChevron = false
                            )
                        },
                        {
                            SettingsDestructiveRow(
                                label = strings.settingsRowDeleteAllData,
                                onClick = { /* Routed to the existing data-management flow elsewhere. */ }
                            )
                        }
                    )
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    when (openDialog) {
        SettingsDialog.ANTHROPIC_KEY -> ApiKeyDialog(
            title = strings.anthropicKeyTitle,
            description = strings.anthropicKeyDescription,
            urlHint = strings.anthropicApiUrl,
            configured = anthropicConfigured,
            onSave = { key ->
                saveApiKeyPlatform(key)
                ApiKeyManager.setApiKey(key)
                vm.refreshKeyStatus()
            },
            onDelete = {
                deleteApiKeyPlatform()
                ApiKeyManager.setApiKey("")
                vm.refreshKeyStatus()
            },
            onDismiss = { openDialog = SettingsDialog.NONE }
        )
        SettingsDialog.WEATHER_KEY -> ApiKeyDialog(
            title = strings.visualCrossingKeyTitle,
            description = strings.visualCrossingKeyDescription,
            urlHint = strings.visualCrossingApiUrl,
            configured = weatherConfigured,
            onSave = { key ->
                saveWeatherApiKeyPlatform(key)
                WeatherApiKeyManager.setApiKey(key)
                vm.refreshKeyStatus()
            },
            onDelete = {
                deleteWeatherApiKeyPlatform()
                WeatherApiKeyManager.setApiKey("")
                vm.refreshKeyStatus()
            },
            onDismiss = { openDialog = SettingsDialog.NONE }
        )
        SettingsDialog.DAYS_OF_INTEREST -> DaysOfInterestDialog(
            vm = vm,
            onDismiss = { openDialog = SettingsDialog.NONE }
        )
        SettingsDialog.NONE -> Unit
    }
}

/**
 * Editor dialog for a single API key: shows the description, a secure entry field,
 * and Save / Delete actions. Persistence is delegated to the supplied callbacks so
 * this composable stays free of platform logic.
 */
@Composable
private fun ApiKeyDialog(
    title: String,
    description: String,
    urlHint: String,
    configured: Boolean,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var key by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.rewinds.textSecondary
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.SETTINGS_API_KEY_FIELD),
                    placeholder = { Text(strings.apiKeyPlaceholder) },
                    label = { Text(strings.apiKeyFieldLabel) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = false,
                    maxLines = 3
                )
                Text(
                    text = urlHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.rewinds.textTertiary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (key.isNotBlank()) {
                        onSave(key)
                        onDismiss()
                    }
                },
                enabled = key.isNotBlank(),
                modifier = Modifier.testTag(TestTags.SETTINGS_API_KEY_SAVE_BUTTON)
            ) {
                Text(strings.saveKey)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    enabled = configured,
                    modifier = Modifier.testTag(TestTags.SETTINGS_API_KEY_DELETE_BUTTON)
                ) {
                    Text(strings.delete)
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(TestTags.SETTINGS_API_KEY_CANCEL_BUTTON)
                ) {
                    Text(strings.cancel)
                }
            }
        }
    )
}

/**
 * Editor dialog for the natural-language Days of Interest filter. Parsing is performed
 * by the ViewModel; this composable only renders state and forwards user input.
 */
@Composable
private fun DaysOfInterestDialog(
    vm: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val doiState by vm.doiState.collectAsState()
    val currentFilter by vm.currentFilter.collectAsState()
    var criteria by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.daysOfInterestTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.daysOfInterestDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.rewinds.textSecondary
                )

                currentFilter?.let { filter ->
                    Text(
                        text = strings.daysOfInterestCurrent(filter.naturalLanguageCriteria),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.rewinds.accentBlue
                    )
                    Text(
                        text = filter.filterSummary(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.rewinds.textTertiary
                    )
                }

                OutlinedTextField(
                    value = criteria,
                    onValueChange = {
                        criteria = it
                        if (doiState !is DaysOfInterestUiState.Idle) vm.resetDoiState()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(strings.daysOfInterestPlaceholder) },
                    label = { Text(strings.daysOfInterestTitle) },
                    singleLine = false,
                    maxLines = 4
                )

                when (val state = doiState) {
                    is DaysOfInterestUiState.Parsing -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        Text(
                            text = strings.daysOfInterestParsing,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.rewinds.textSecondary
                        )
                    }
                    is DaysOfInterestUiState.Error -> Text(
                        text = strings.daysOfInterestError(state.message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.rewinds.error
                    )
                    else -> Unit
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.saveFilter(criteria) },
                enabled = criteria.isNotBlank() && doiState !is DaysOfInterestUiState.Parsing
            ) {
                Text(strings.daysOfInterestSave)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
