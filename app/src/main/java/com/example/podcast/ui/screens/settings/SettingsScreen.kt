package com.example.podcast.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.podcast.data.model.AppThemeMode
import com.example.podcast.data.model.PlaybackDirection
import com.example.podcast.data.sync.RestoreStrategy
import com.example.podcast.ui.components.RestoreOptionDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val settings by viewModel.settings.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var showBackupTextDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val s = settings

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Preferences, sync & playback customization",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- PLAYBACK ---
        item {
            SettingsCategoryHeader(title = "Playback", icon = Icons.Default.PlayCircle)
        }

        item {
            SettingsClickableItem(
                title = "Default Playback Direction",
                subtitle = s?.defaultPlaybackDirection?.displayName ?: "Up (Oldest First / Binge)",
                onClick = {
                    val next = if (s?.defaultPlaybackDirection == PlaybackDirection.UP) PlaybackDirection.DOWN else PlaybackDirection.UP
                    viewModel.updatePlaybackDirection(next)
                }
            )
        }

        item {
            SettingsSwitchItem(
                title = "Auto-play next episode",
                subtitle = "Queue binging automatically after finishing an episode",
                checked = s?.autoPlayNext ?: true,
                onCheckedChange = { viewModel.updateAutoPlayNext(it) }
            )
        }

        item {
            SettingsClickableItem(
                title = "Global Playback Speed",
                subtitle = "${s?.globalSpeed ?: 1.0f}x (Per-podcast override wins)",
                onClick = {
                    val newSpeed = if ((s?.globalSpeed ?: 1.0f) >= 2.0f) 1.0f else (s?.globalSpeed ?: 1.0f) + 0.2f
                    viewModel.updateGlobalSpeed(newSpeed)
                }
            )
        }

        // --- APPEARANCE ---
        item {
            SettingsCategoryHeader(title = "Appearance", icon = Icons.Default.ColorLens)
        }

        item {
            SettingsClickableItem(
                title = "Theme",
                subtitle = when (s?.themeMode) {
                    AppThemeMode.OLED_BLACK -> "OLED Black (Pure #000000)"
                    AppThemeMode.DARK -> "Dark"
                    AppThemeMode.LIGHT -> "Light"
                    else -> "System Default"
                },
                onClick = {
                    val nextTheme = when (s?.themeMode) {
                        AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
                        AppThemeMode.LIGHT -> AppThemeMode.DARK
                        AppThemeMode.DARK -> AppThemeMode.OLED_BLACK
                        else -> AppThemeMode.SYSTEM
                    }
                    viewModel.updateThemeMode(nextTheme)
                }
            )
        }

        item {
            SettingsSwitchItem(
                title = "Dynamic Color (Material You)",
                subtitle = "Generate theme scheme from system wallpaper",
                checked = s?.dynamicColor ?: true,
                onCheckedChange = { viewModel.updateDynamicColor(it) }
            )
        }

        item {
            SettingsSwitchItem(
                title = "Reduce Motion",
                subtitle = "Swap spring animations for fades",
                checked = s?.reduceMotion ?: false,
                onCheckedChange = { viewModel.updateReduceMotion(it) }
            )
        }

        // --- DOWNLOADS & STORAGE ---
        item {
            SettingsCategoryHeader(title = "Downloads & Retention", icon = Icons.Default.Download)
        }

        item {
            SettingsSwitchItem(
                title = "Download over Wi-Fi only",
                subtitle = "Prevent mobile data consumption for downloads",
                checked = s?.wifiOnlyDownloads ?: true,
                onCheckedChange = { viewModel.updateWifiOnly(it) }
            )
        }

        item {
            SettingsClickableItem(
                title = "Retention Cleanup Window",
                subtitle = "Auto-delete if untouched for ${s?.retentionDays ?: 30} days (resets on play/seek)",
                onClick = {
                    val nextDays = when (s?.retentionDays) {
                        7 -> 14
                        14 -> 30
                        30 -> 60
                        60 -> 90
                        else -> 7
                    }
                    viewModel.updateRetentionDays(nextDays)
                }
            )
        }

        // --- SYNC & BACKUP ---
        item {
            SettingsCategoryHeader(title = "Sync & Cloud Backup", icon = Icons.Default.CloudSync)
        }

        item {
            SettingsClickableItem(
                title = "Sync with Google Drive",
                subtitle = if (s?.lastSyncedTimestamp ?: 0L > 0L) {
                    "Last synced: " + SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(s?.lastSyncedTimestamp ?: 0L))
                } else "Tap to sync subscriptions and positions now",
                onClick = { viewModel.syncNow() }
            )
        }

        item {
            SettingsClickableItem(
                title = "Export OPML 2.0",
                subtitle = "Export subscriptions for other podcast apps",
                onClick = {
                    viewModel.exportOpml { opml ->
                        showBackupTextDialog = opml
                    }
                }
            )
        }

        item {
            SettingsClickableItem(
                title = "Full JSON Backup",
                subtitle = "Generate complete state backup JSON",
                onClick = {
                    viewModel.exportFullBackup { json ->
                        showBackupTextDialog = json
                    }
                }
            )
        }

        // --- DATA & RESET ---
        item {
            SettingsCategoryHeader(title = "Data & Maintenance", icon = Icons.Default.Storage)
        }

        item {
            SettingsClickableItem(
                title = "Reset Listening Stats",
                subtitle = "Clear all-time and weekly listening rollups",
                onClick = { viewModel.resetStats() }
            )
        }

        item {
            SettingsClickableItem(
                title = "Clear Playback History",
                subtitle = "Remove recently played timestamps",
                onClick = { viewModel.clearHistory() }
            )
        }
    }

    // Backup text preview dialog
    showBackupTextDialog?.let { text ->
        AlertDialog(
            onDismissRequest = { showBackupTextDialog = null },
            title = { Text("Exported Data") },
            text = {
                Text(
                    text = text.take(600) + if (text.length > 600) "\n\n... (full output saved)" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showBackupTextDialog = null }) {
                    Text("Done")
                }
            }
        )
    }

    if (showRestoreDialog && pendingRestoreJson != null) {
        RestoreOptionDialog(
            onConfirm = { strategy ->
                viewModel.restoreFullBackup(pendingRestoreJson!!, strategy)
                showRestoreDialog = false
                pendingRestoreJson = null
            },
            onDismiss = {
                showRestoreDialog = false
                pendingRestoreJson = null
            }
        )
    }
}

@Composable
private fun SettingsCategoryHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsClickableItem(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsSwitchItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
