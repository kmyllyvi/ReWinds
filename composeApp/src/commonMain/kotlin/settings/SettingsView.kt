package settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import core.ApiKeyManager
import core.Language
import core.LanguageManager
import core.LocalAppStrings
import core.WeatherApiKeyManager
import core.Navigator
import core.deleteApiKeyPlatform
import core.deleteWeatherApiKeyPlatform
import core.isAnthropicApiKeyConfigured
import core.saveApiKeyPlatform
import core.saveWeatherApiKeyPlatform
import components.AppHeader
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsView(
    navigator: Navigator,
    vm: SettingsViewModel = koinViewModel()
) {
    val scrollState = rememberScrollState()
    val strings = LocalAppStrings.current
    val language by LanguageManager.currentLanguage.collectAsState()
    val focusManager = LocalFocusManager.current
    var anthropicApiKey by remember { mutableStateOf("") }
    var weatherApiKey by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf("") }

    val doiState by vm.doiState.collectAsState()
    val currentFilter by vm.currentFilter.collectAsState()
    var doiCriteria by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AppHeader(
            title = strings.settingsTitle,
            onBackClick = { navigator.navigateBack() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .clickable(
                    indication = null,
                    interactionSource = MutableInteractionSource()
                ) {
                    focusManager.clearFocus()
                }
                .padding(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language Section
            Text(
                text = strings.languageTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { LanguageManager.setLanguage(Language.ENGLISH) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (language == Language.ENGLISH)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (language == Language.ENGLISH)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("EN")
                }
                Button(
                    onClick = { LanguageManager.setLanguage(Language.GERMAN) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (language == Language.GERMAN)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (language == Language.GERMAN)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("DE")
                }
            }

            // Anthropic API Key Section
            Text(
                text = strings.anthropicKeyTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = strings.anthropicKeyDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isAnthropicApiKeyConfigured()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = strings.apiKeyConfigured,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = strings.apiKeyNotConfiguredStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (showSuccessMessage == "anthropic") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = strings.apiKeySaved,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = anthropicApiKey,
                onValueChange = { anthropicApiKey = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(strings.apiKeyPlaceholder) },
                label = { Text(strings.apiKeyFieldLabel) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = false,
                maxLines = 3
            )

            Text(
                text = strings.anthropicApiUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { showDeleteConfirm = "anthropic" },
                    modifier = Modifier.weight(1f),
                    enabled = isAnthropicApiKeyConfigured()
                ) {
                    Text(strings.delete)
                }

                Button(
                    onClick = {
                        if (anthropicApiKey.isNotBlank()) {
                            saveApiKeyPlatform(anthropicApiKey)
                            ApiKeyManager.setApiKey(anthropicApiKey)
                            showSuccessMessage = "anthropic"
                            anthropicApiKey = ""
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(strings.saveKey)
                }
            }

            // Visual Crossing API Key Section
            Text(
                text = strings.visualCrossingKeyTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = strings.visualCrossingKeyDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (WeatherApiKeyManager.hasValidKey()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = strings.apiKeyConfigured,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = strings.apiKeyNotConfiguredStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (showSuccessMessage == "weather") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = strings.apiKeySaved,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = weatherApiKey,
                onValueChange = { weatherApiKey = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(strings.apiKeyPlaceholder) },
                label = { Text(strings.apiKeyFieldLabel) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = false,
                maxLines = 3
            )

            Text(
                text = strings.visualCrossingApiUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { showDeleteConfirm = "weather" },
                    modifier = Modifier.weight(1f),
                    enabled = WeatherApiKeyManager.hasValidKey()
                ) {
                    Text(strings.delete)
                }

                Button(
                    onClick = {
                        if (weatherApiKey.isNotBlank()) {
                            saveWeatherApiKeyPlatform(weatherApiKey)
                            WeatherApiKeyManager.setApiKey(weatherApiKey)
                            showSuccessMessage = "weather"
                            weatherApiKey = ""
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(strings.saveKey)
                }
            }

            // Days of Interest Section
            Text(
                text = strings.daysOfInterestTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = strings.daysOfInterestDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            currentFilter?.let { filter ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = strings.daysOfInterestCurrent(filter.naturalLanguageCriteria),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            when (val state = doiState) {
                is DaysOfInterestUiState.Parsing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        Text(
                            text = strings.daysOfInterestParsing,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is DaysOfInterestUiState.Success -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = strings.daysOfInterestCurrent(state.filter.naturalLanguageCriteria),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                is DaysOfInterestUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = strings.daysOfInterestError(state.message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                is DaysOfInterestUiState.Idle -> Unit
            }

            OutlinedTextField(
                value = doiCriteria,
                onValueChange = {
                    doiCriteria = it
                    if (doiState !is DaysOfInterestUiState.Idle) vm.resetDoiState()
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(strings.daysOfInterestPlaceholder) },
                label = { Text(strings.daysOfInterestTitle) },
                singleLine = false,
                maxLines = 4
            )

            Button(
                onClick = {
                    vm.saveFilter(doiCriteria)
                    doiCriteria = ""
                    focusManager.clearFocus()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = doiCriteria.isNotBlank() && doiState !is DaysOfInterestUiState.Parsing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(strings.daysOfInterestSave)
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm.isNotBlank()) {
        val (keyType, onConfirmDelete) = when (showDeleteConfirm) {
            "anthropic" -> strings.anthropicKeyTitle to {
                deleteApiKeyPlatform()
                ApiKeyManager.setApiKey("")
            }
            "weather" -> strings.visualCrossingKeyTitle to {
                deleteWeatherApiKeyPlatform()
                WeatherApiKeyManager.setApiKey("")
            }
            else -> "" to {}
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = "" },
            title = { Text(strings.deleteKeyTitle(keyType)) },
            text = { Text(strings.deleteKeyMessage(keyType)) },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmDelete()
                        showDeleteConfirm = ""
                        showSuccessMessage = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = "" }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
