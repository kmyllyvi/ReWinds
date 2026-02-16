package components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import core.DatabaseExportImport
import kotlinx.coroutines.launch

/**
 * Debug menu for developer tools including database export/import
 */
@Composable
fun DebugMenu(
    databaseExportImport: DatabaseExportImport,
    onExportResult: (String) -> Unit = {},
    onImportResult: (String) -> Unit = {},
    onBackupListResult: (List<String>) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "🛠️ Debug Tools",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Export Database Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        val result = databaseExportImport.exportDatabase()
                        result.onSuccess { message ->
                            onExportResult("✅ Export: $message")
                        }
                        result.onFailure { exception ->
                            onExportResult("❌ Export failed: ${exception.message}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("📤 Export Database")
            }

            // List Backups Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        val result = databaseExportImport.listBackups()
                        result.onSuccess { backups ->
                            onBackupListResult(backups)
                        }
                        result.onFailure { exception ->
                            onBackupListResult(emptyList())
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("📋 List Backups")
            }

            // Import Database Button (iOS only)
            Button(
                onClick = {
                    coroutineScope.launch {
                        // This will need UI integration (file picker)
                        // For now, just a placeholder
                        onImportResult("📥 File picker needed to select import file")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("📥 Import Database")
            }
        }
    }
}
