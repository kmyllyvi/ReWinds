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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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

@Composable
fun SettingsView(navigator: Navigator) {
    val scrollState = rememberScrollState()
    val strings = LocalAppStrings.current
    val language by LanguageManager.currentLanguage.collectAsState()
    var anthropicApiKey by remember { mutableStateOf("") }
    var weatherApiKey by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf("") }

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
