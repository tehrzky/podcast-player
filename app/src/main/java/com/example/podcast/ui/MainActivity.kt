package com.example.podcast.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.podcast.data.datastore.SettingsDataStore
import com.example.podcast.data.model.AppThemeMode
import com.example.podcast.data.model.PlaybackConflict
import com.example.podcast.data.sync.GoogleDriveSyncManager
import com.example.podcast.playback.PlaybackController
import com.example.podcast.ui.components.ConflictResolutionDialog
import com.example.podcast.ui.navigation.AppNavigation
import com.example.podcast.ui.theme.PodcastTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playbackController: PlaybackController

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var syncManager: GoogleDriveSyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by settingsDataStore.settingsFlow.collectAsState(initial = null)
            val themeMode = settings?.themeMode ?: AppThemeMode.SYSTEM
            val dynamicColor = settings?.dynamicColor ?: true
            val seedColor = settings?.accentColorSeed ?: 0xFF6650a4

            var activeConflict by remember { mutableStateOf<PlaybackConflict?>(null) }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                syncManager.conflictFlow.collectLatest { conflict ->
                    activeConflict = conflict
                }
            }

            PodcastTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                accentSeedColor = seedColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(playbackController = playbackController)

                    // Position conflict resolution modal dialog
                    activeConflict?.let { conflict ->
                        ConflictResolutionDialog(
                            conflict = conflict,
                            onResolveThisDevice = {
                                lifecycleScope.launch {
                                    syncManager.resolveConflict(conflict.episodeId, conflict.localPositionMs)
                                    activeConflict = null
                                }
                            },
                            onResolveOtherDevice = {
                                lifecycleScope.launch {
                                    syncManager.resolveConflict(conflict.episodeId, conflict.remotePositionMs)
                                    activeConflict = null
                                }
                            },
                            onResolveFurthestAlong = {
                                lifecycleScope.launch {
                                    val furthest = Math.max(conflict.localPositionMs, conflict.remotePositionMs)
                                    syncManager.resolveConflict(conflict.episodeId, furthest)
                                    activeConflict = null
                                }
                            },
                            onDismiss = { activeConflict = null }
                        )
                    }
                }
            }
        }
    }
}
