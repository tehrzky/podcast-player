package com.example.podcast.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.podcast.data.model.AppThemeMode
import com.example.podcast.data.model.EpisodeSortOrder
import com.example.podcast.data.model.LibraryViewMode
import com.example.podcast.data.model.PlaybackDirection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "podcast_settings")

data class AppSettings(
    // Playback
    val globalSpeed: Float = 1.0f,
    val skipForwardSeconds: Int = 30,
    val skipBackSeconds: Int = 10,
    val autoPlayNext: Boolean = true,
    val continueAcrossPodcasts: Boolean = true,
    val defaultPlaybackDirection: PlaybackDirection = PlaybackDirection.UP,
    val skipIntroSeconds: Int = 0,
    val skipOutroSeconds: Int = 0,
    val silenceTrim: Boolean = false,
    val volumeBoost: Boolean = false,
    val resumeOnHeadphoneReconnect: Boolean = false,
    val pauseOnHeadphoneDisconnect: Boolean = true,

    // Downloads
    val wifiOnlyDownloads: Boolean = true,
    val autoDownloadNewEpisodes: Boolean = false,
    val autoDownloadLastN: Int = 3,
    val retentionDays: Int = 30,
    val deleteAfterFinished: Boolean = false,
    val storageCapGb: Int = 5,
    val concurrentDownloads: Int = 2,
    val downloadLocation: String = "App Internal Storage",

    // Sync
    val googleAccountEmail: String? = null,
    val autoSyncEnabled: Boolean = true,
    val lastSyncedTimestamp: Long = 0L,

    // Backup
    val autoBackupEnabled: Boolean = false,
    val autoBackupFrequency: String = "daily",
    val backupDestination: String = "drive",

    // Notifications
    val newEpisodeNotifications: Boolean = true,
    val playbackNotificationStyle: String = "standard",

    // Appearance
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accentColorSeed: Long = 0xFF6750A4, // Material purple seed
    val textSizeMultiplier: Float = 1.0f,
    val densityCompact: Boolean = false,
    val reduceMotion: Boolean = false,

    // Library
    val defaultEpisodeSort: EpisodeSortOrder = EpisodeSortOrder.OLDEST_FIRST,
    val defaultShowSort: String = "TITLE_ASC",
    val libraryViewMode: LibraryViewMode = LibraryViewMode.GRID_2,
    val startTab: String = "home",
    val hideFinishedEpisodes: Boolean = false,

    // Advanced
    val feedRefreshIntervalHours: Int = 6,
    val customUserAgent: String = "PodcastPlayer/1.0 (Android)",
    val markAsPlayedThresholdPercent: Int = 90,
    val episodeNumberParsing: String = "auto"
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val GLOBAL_SPEED = floatPreferencesKey("global_speed")
        val SKIP_FORWARD_SEC = intPreferencesKey("skip_forward_sec")
        val SKIP_BACK_SEC = intPreferencesKey("skip_back_sec")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val CONTINUE_ACROSS_PODCASTS = booleanPreferencesKey("continue_across_podcasts")
        val PLAYBACK_DIRECTION = stringPreferencesKey("playback_direction")
        val SKIP_INTRO_SEC = intPreferencesKey("skip_intro_sec")
        val SKIP_OUTRO_SEC = intPreferencesKey("skip_outro_sec")
        val SILENCE_TRIM = booleanPreferencesKey("silence_trim")
        val VOLUME_BOOST = booleanPreferencesKey("volume_boost")
        val RESUME_HEADPHONE_RECONNECT = booleanPreferencesKey("resume_headphone_reconnect")
        val PAUSE_HEADPHONE_DISCONNECT = booleanPreferencesKey("pause_headphone_disconnect")

        val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
        val AUTO_DOWNLOAD_NEW = booleanPreferencesKey("auto_download_new")
        val AUTO_DOWNLOAD_LAST_N = intPreferencesKey("auto_download_last_n")
        val RETENTION_DAYS = intPreferencesKey("retention_days")
        val DELETE_AFTER_FINISHED = booleanPreferencesKey("delete_after_finished")
        val STORAGE_CAP_GB = intPreferencesKey("storage_cap_gb")
        val CONCURRENT_DOWNLOADS = intPreferencesKey("concurrent_downloads")

        val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val LAST_SYNCED_TIMESTAMP = longPreferencesKey("last_synced_timestamp")

        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FREQUENCY = stringPreferencesKey("auto_backup_frequency")
        val BACKUP_DESTINATION = stringPreferencesKey("backup_destination")

        val NEW_EPISODE_NOTIFICATIONS = booleanPreferencesKey("new_episode_notifications")
        val NOTIFICATION_STYLE = stringPreferencesKey("notification_style")

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT_COLOR_SEED = longPreferencesKey("accent_color_seed")
        val TEXT_SIZE_MULTIPLIER = floatPreferencesKey("text_size_multiplier")
        val DENSITY_COMPACT = booleanPreferencesKey("density_compact")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")

        val DEFAULT_EPISODE_SORT = stringPreferencesKey("default_episode_sort")
        val DEFAULT_SHOW_SORT = stringPreferencesKey("default_show_sort")
        val LIBRARY_VIEW_MODE = stringPreferencesKey("library_view_mode")
        val START_TAB = stringPreferencesKey("start_tab")
        val HIDE_FINISHED_EPISODES = booleanPreferencesKey("hide_finished_episodes")

        val REFRESH_INTERVAL_HOURS = intPreferencesKey("refresh_interval_hours")
        val CUSTOM_USER_AGENT = stringPreferencesKey("custom_user_agent")
        val MARK_AS_PLAYED_THRESHOLD = intPreferencesKey("mark_as_played_threshold")
        val EPISODE_NUMBER_PARSING = stringPreferencesKey("episode_number_parsing")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { pref ->
        AppSettings(
            globalSpeed = pref[Keys.GLOBAL_SPEED] ?: 1.0f,
            skipForwardSeconds = pref[Keys.SKIP_FORWARD_SEC] ?: 30,
            skipBackSeconds = pref[Keys.SKIP_BACK_SEC] ?: 10,
            autoPlayNext = pref[Keys.AUTO_PLAY_NEXT] ?: true,
            continueAcrossPodcasts = pref[Keys.CONTINUE_ACROSS_PODCASTS] ?: true,
            defaultPlaybackDirection = pref[Keys.PLAYBACK_DIRECTION]?.let {
                try { PlaybackDirection.valueOf(it) } catch (e: Exception) { PlaybackDirection.UP }
            } ?: PlaybackDirection.UP,
            skipIntroSeconds = pref[Keys.SKIP_INTRO_SEC] ?: 0,
            skipOutroSeconds = pref[Keys.SKIP_OUTRO_SEC] ?: 0,
            silenceTrim = pref[Keys.SILENCE_TRIM] ?: false,
            volumeBoost = pref[Keys.VOLUME_BOOST] ?: false,
            resumeOnHeadphoneReconnect = pref[Keys.RESUME_HEADPHONE_RECONNECT] ?: false,
            pauseOnHeadphoneDisconnect = pref[Keys.PAUSE_HEADPHONE_DISCONNECT] ?: true,

            wifiOnlyDownloads = pref[Keys.WIFI_ONLY_DOWNLOADS] ?: true,
            autoDownloadNewEpisodes = pref[Keys.AUTO_DOWNLOAD_NEW] ?: false,
            autoDownloadLastN = pref[Keys.AUTO_DOWNLOAD_LAST_N] ?: 3,
            retentionDays = pref[Keys.RETENTION_DAYS] ?: 30,
            deleteAfterFinished = pref[Keys.DELETE_AFTER_FINISHED] ?: false,
            storageCapGb = pref[Keys.STORAGE_CAP_GB] ?: 5,
            concurrentDownloads = pref[Keys.CONCURRENT_DOWNLOADS] ?: 2,

            googleAccountEmail = pref[Keys.GOOGLE_ACCOUNT_EMAIL],
            autoSyncEnabled = pref[Keys.AUTO_SYNC_ENABLED] ?: true,
            lastSyncedTimestamp = pref[Keys.LAST_SYNCED_TIMESTAMP] ?: 0L,

            autoBackupEnabled = pref[Keys.AUTO_BACKUP_ENABLED] ?: false,
            autoBackupFrequency = pref[Keys.AUTO_BACKUP_FREQUENCY] ?: "daily",
            backupDestination = pref[Keys.BACKUP_DESTINATION] ?: "drive",

            newEpisodeNotifications = pref[Keys.NEW_EPISODE_NOTIFICATIONS] ?: true,
            playbackNotificationStyle = pref[Keys.NOTIFICATION_STYLE] ?: "standard",

            themeMode = pref[Keys.THEME_MODE]?.let {
                try { AppThemeMode.valueOf(it) } catch (e: Exception) { AppThemeMode.SYSTEM }
            } ?: AppThemeMode.SYSTEM,
            dynamicColor = pref[Keys.DYNAMIC_COLOR] ?: true,
            accentColorSeed = pref[Keys.ACCENT_COLOR_SEED] ?: 0xFF6750A4,
            textSizeMultiplier = pref[Keys.TEXT_SIZE_MULTIPLIER] ?: 1.0f,
            densityCompact = pref[Keys.DENSITY_COMPACT] ?: false,
            reduceMotion = pref[Keys.REDUCE_MOTION] ?: false,

            defaultEpisodeSort = pref[Keys.DEFAULT_EPISODE_SORT]?.let {
                try { EpisodeSortOrder.valueOf(it) } catch (e: Exception) { EpisodeSortOrder.OLDEST_FIRST }
            } ?: EpisodeSortOrder.OLDEST_FIRST,
            defaultShowSort = pref[Keys.DEFAULT_SHOW_SORT] ?: "TITLE_ASC",
            libraryViewMode = pref[Keys.LIBRARY_VIEW_MODE]?.let {
                try { LibraryViewMode.valueOf(it) } catch (e: Exception) { LibraryViewMode.GRID_2 }
            } ?: LibraryViewMode.GRID_2,
            startTab = pref[Keys.START_TAB] ?: "home",
            hideFinishedEpisodes = pref[Keys.HIDE_FINISHED_EPISODES] ?: false,

            feedRefreshIntervalHours = pref[Keys.REFRESH_INTERVAL_HOURS] ?: 6,
            customUserAgent = pref[Keys.CUSTOM_USER_AGENT] ?: "PodcastPlayer/1.0 (Android)",
            markAsPlayedThresholdPercent = pref[Keys.MARK_AS_PLAYED_THRESHOLD] ?: 90,
            episodeNumberParsing = pref[Keys.EPISODE_NUMBER_PARSING] ?: "auto"
        )
    }

    suspend fun updateGlobalSpeed(speed: Float) = context.dataStore.edit { it[Keys.GLOBAL_SPEED] = speed }
    suspend fun updateSkipSeconds(forward: Int, back: Int) = context.dataStore.edit {
        it[Keys.SKIP_FORWARD_SEC] = forward
        it[Keys.SKIP_BACK_SEC] = back
    }
    suspend fun updateAutoPlayNext(enabled: Boolean) = context.dataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    suspend fun updateContinueAcrossPodcasts(enabled: Boolean) = context.dataStore.edit { it[Keys.CONTINUE_ACROSS_PODCASTS] = enabled }
    suspend fun updateDefaultPlaybackDirection(direction: PlaybackDirection) = context.dataStore.edit { it[Keys.PLAYBACK_DIRECTION] = direction.name }
    suspend fun updateSilenceTrim(enabled: Boolean) = context.dataStore.edit { it[Keys.SILENCE_TRIM] = enabled }
    suspend fun updateVolumeBoost(enabled: Boolean) = context.dataStore.edit { it[Keys.VOLUME_BOOST] = enabled }
    suspend fun updateHeadphoneSettings(resumeOnReconnect: Boolean, pauseOnDisconnect: Boolean) = context.dataStore.edit {
        it[Keys.RESUME_HEADPHONE_RECONNECT] = resumeOnReconnect
        it[Keys.PAUSE_HEADPHONE_DISCONNECT] = pauseOnDisconnect
    }

    suspend fun updateDownloadSettings(
        wifiOnly: Boolean,
        autoDownloadNew: Boolean,
        lastN: Int,
        retentionDays: Int,
        deleteAfterFinished: Boolean,
        capGb: Int,
        concurrent: Int
    ) = context.dataStore.edit {
        it[Keys.WIFI_ONLY_DOWNLOADS] = wifiOnly
        it[Keys.AUTO_DOWNLOAD_NEW] = autoDownloadNew
        it[Keys.AUTO_DOWNLOAD_LAST_N] = lastN
        it[Keys.RETENTION_DAYS] = retentionDays
        it[Keys.DELETE_AFTER_FINISHED] = deleteAfterFinished
        it[Keys.STORAGE_CAP_GB] = capGb
        it[Keys.CONCURRENT_DOWNLOADS] = concurrent
    }

    suspend fun updateGoogleAccount(email: String?) = context.dataStore.edit {
        if (email != null) it[Keys.GOOGLE_ACCOUNT_EMAIL] = email else it.remove(Keys.GOOGLE_ACCOUNT_EMAIL)
    }
    suspend fun updateAutoSync(enabled: Boolean) = context.dataStore.edit { it[Keys.AUTO_SYNC_ENABLED] = enabled }
    suspend fun updateLastSynced(timestamp: Long) = context.dataStore.edit { it[Keys.LAST_SYNCED_TIMESTAMP] = timestamp }

    suspend fun updateAutoBackup(enabled: Boolean, frequency: String, destination: String) = context.dataStore.edit {
        it[Keys.AUTO_BACKUP_ENABLED] = enabled
        it[Keys.AUTO_BACKUP_FREQUENCY] = frequency
        it[Keys.BACKUP_DESTINATION] = destination
    }

    suspend fun updateThemeMode(mode: AppThemeMode) = context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun updateDynamicColor(enabled: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun updateAccentColorSeed(seed: Long) = context.dataStore.edit { it[Keys.ACCENT_COLOR_SEED] = seed }
    suspend fun updateReduceMotion(enabled: Boolean) = context.dataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
    suspend fun updateDensityCompact(compact: Boolean) = context.dataStore.edit { it[Keys.DENSITY_COMPACT] = compact }

    suspend fun updateDefaultEpisodeSort(sort: EpisodeSortOrder) = context.dataStore.edit { it[Keys.DEFAULT_EPISODE_SORT] = sort.name }
    suspend fun updateLibraryViewMode(mode: LibraryViewMode) = context.dataStore.edit { it[Keys.LIBRARY_VIEW_MODE] = mode.name }
    suspend fun updateHideFinishedEpisodes(hide: Boolean) = context.dataStore.edit { it[Keys.HIDE_FINISHED_EPISODES] = hide }
}
