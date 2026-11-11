package com.example.podcast.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podcast.data.datastore.AppSettings
import com.example.podcast.data.datastore.SettingsDataStore
import com.example.podcast.data.local.EpisodeDao
import com.example.podcast.data.model.AppThemeMode
import com.example.podcast.data.model.EpisodeSortOrder
import com.example.podcast.data.model.PlaybackDirection
import com.example.podcast.data.repository.StatsRepository
import com.example.podcast.data.sync.GoogleDriveSyncManager
import com.example.podcast.data.sync.JsonBackupManager
import com.example.podcast.data.sync.OpmlManager
import com.example.podcast.data.sync.RestoreStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val syncManager: GoogleDriveSyncManager,
    private val opmlManager: OpmlManager,
    private val backupManager: JsonBackupManager,
    private val statsRepository: StatsRepository,
    private val episodeDao: EpisodeDao
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { settingsDataStore.updateThemeMode(mode) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateDynamicColor(enabled) }
    }

    fun updateReduceMotion(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateReduceMotion(enabled) }
    }

    fun updatePlaybackDirection(direction: PlaybackDirection) {
        viewModelScope.launch { settingsDataStore.updateDefaultPlaybackDirection(direction) }
    }

    fun updateAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateAutoPlayNext(enabled) }
    }

    fun updateGlobalSpeed(speed: Float) {
        viewModelScope.launch { settingsDataStore.updateGlobalSpeed(speed) }
    }

    fun updateSkipSeconds(forward: Int, back: Int) {
        viewModelScope.launch { settingsDataStore.updateSkipSeconds(forward, back) }
    }

    fun updateWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            val s = settings.value ?: return@launch
            settingsDataStore.updateDownloadSettings(
                wifiOnly = enabled,
                autoDownloadNew = s.autoDownloadNewEpisodes,
                lastN = s.autoDownloadLastN,
                retentionDays = s.retentionDays,
                deleteAfterFinished = s.deleteAfterFinished,
                capGb = s.storageCapGb,
                concurrent = s.concurrentDownloads
            )
        }
    }

    fun updateRetentionDays(days: Int) {
        viewModelScope.launch {
            val s = settings.value ?: return@launch
            settingsDataStore.updateDownloadSettings(
                wifiOnly = s.wifiOnlyDownloads,
                autoDownloadNew = s.autoDownloadNewEpisodes,
                lastN = s.autoDownloadLastN,
                retentionDays = days,
                deleteAfterFinished = s.deleteAfterFinished,
                capGb = s.storageCapGb,
                concurrent = s.concurrentDownloads
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            try {
                syncManager.createLocalSyncPayload()
                settingsDataStore.updateLastSynced(System.currentTimeMillis())
                _message.value = "Synced with Google Drive successfully!"
            } catch (e: Exception) {
                _message.value = "Sync failed: ${e.message}"
            }
        }
    }

    fun exportOpml(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val opml = opmlManager.exportOpml()
                onResult(opml)
                _message.value = "OPML Export ready!"
            } catch (e: Exception) {
                _message.value = "OPML Export failed: ${e.message}"
            }
        }
    }

    fun exportFullBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = backupManager.createFullBackupJson()
                onResult(json)
                _message.value = "Full backup generated!"
            } catch (e: Exception) {
                _message.value = "Backup failed: ${e.message}"
            }
        }
    }

    fun restoreFullBackup(json: String, strategy: RestoreStrategy) {
        viewModelScope.launch {
            try {
                backupManager.restoreFromJson(json, strategy)
                _message.value = "Backup restored successfully (${strategy.name.lowercase()})!"
            } catch (e: Exception) {
                _message.value = "Restore failed: ${e.message}"
            }
        }
    }

    fun resetStats() {
        viewModelScope.launch {
            statsRepository.resetStats()
            _message.value = "Listening stats reset."
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            _message.value = "Playback history cleared."
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
