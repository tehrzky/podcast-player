package com.example.podcast.data.sync

import android.content.Context
import com.example.podcast.data.datastore.SettingsDataStore
import com.example.podcast.data.local.AppDatabase
import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.ListeningStat
import com.example.podcast.data.model.PlaybackConflict
import com.example.podcast.data.model.Podcast
import com.example.podcast.data.model.QueueItem
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class SyncPayload(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val subscriptions: List<SyncPodcastDto> = emptyList(),
    val episodeStates: List<SyncEpisodeStateDto> = emptyList(),
    val queue: List<SyncQueueItemDto> = emptyList(),
    val stats: List<SyncStatDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SyncPodcastDto(
    val id: String,
    val title: String,
    val author: String,
    val artworkUrl: String,
    val feedUrl: String,
    val subscribed: Boolean,
    val addedAt: Long,
    val defaultSortOrder: String?,
    val defaultPlaybackDirection: String?,
    val playbackSpeedOverride: Float?
)

@JsonClass(generateAdapter = true)
data class SyncEpisodeStateDto(
    val episodeId: String,
    val podcastId: String,
    val playbackPosition: Long,
    val isFinished: Boolean,
    val isStarted: Boolean,
    val lastPlayedAt: Long,
    val isDownloadedIntent: Boolean
)

@JsonClass(generateAdapter = true)
data class SyncQueueItemDto(
    val episodeId: String,
    val position: Int,
    val source: String
)

@JsonClass(generateAdapter = true)
data class SyncStatDto(
    val date: String,
    val secondsListened: Long,
    val episodesStarted: Int,
    val episodesFinished: Int
)

@Singleton
class GoogleDriveSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(SyncPayload::class.java)

    private val _conflictFlow = MutableSharedFlow<PlaybackConflict>(replay = 1)
    val conflictFlow: SharedFlow<PlaybackConflict> = _conflictFlow.asSharedFlow()

    suspend fun createLocalSyncPayload(): SyncPayload = withContext(Dispatchers.IO) {
        val podcasts = database.podcastDao().getSubscribedPodcastsSync().map {
            SyncPodcastDto(
                id = it.id,
                title = it.title,
                author = it.author,
                artworkUrl = it.artworkUrl,
                feedUrl = it.feedUrl,
                subscribed = it.subscribed,
                addedAt = it.addedAt,
                defaultSortOrder = it.defaultSortOrder?.name,
                defaultPlaybackDirection = it.defaultPlaybackDirection?.name,
                playbackSpeedOverride = it.playbackSpeedOverride
            )
        }

        val queue = database.queueDao().getQueueItemsSync().map {
            SyncQueueItemDto(
                episodeId = it.episodeId,
                position = it.position,
                source = it.source.name
            )
        }

        // We only need to sync episodes that have user interaction
        val episodes = database.episodeDao().getEpisodesSortedSync("", "NEWEST_FIRST")
            .filter { it.lastPlayedAt > 0 || it.isFinished || it.isStarted || it.isDownloaded }
            .map {
                SyncEpisodeStateDto(
                    episodeId = it.id,
                    podcastId = it.podcastId,
                    playbackPosition = it.playbackPosition,
                    isFinished = it.isFinished,
                    isStarted = it.isStarted,
                    lastPlayedAt = it.lastPlayedAt,
                    isDownloadedIntent = it.isDownloaded
                )
            }

        SyncPayload(
            version = 1,
            timestamp = System.currentTimeMillis(),
            subscriptions = podcasts,
            episodeStates = episodes,
            queue = queue
        )
    }

    suspend fun applyRemotePayload(remoteJson: String) = withContext(Dispatchers.IO) {
        val payload = adapter.fromJson(remoteJson) ?: return@withContext
        val now = System.currentTimeMillis()
        val oneDayAgo = now - 24 * 60 * 60 * 1000L

        // 1. Process Subscriptions (last write wins)
        for (sub in payload.subscriptions) {
            val existing = database.podcastDao().getPodcastByIdSync(sub.id)
            if (existing == null) {
                database.podcastDao().insertPodcast(
                    Podcast(
                        id = sub.id,
                        title = sub.title,
                        author = sub.author,
                        artworkUrl = sub.artworkUrl,
                        feedUrl = sub.feedUrl,
                        description = "",
                        subscribed = sub.subscribed,
                        addedAt = sub.addedAt,
                        playbackSpeedOverride = sub.playbackSpeedOverride
                    )
                )
            } else if (sub.addedAt > existing.addedAt) {
                database.podcastDao().updateSubscription(sub.id, sub.subscribed, sub.addedAt)
            }
        }

        // 2. Process Episode States & Detect Conflicts
        for (epState in payload.episodeStates) {
            val local = database.episodeDao().getEpisodeByIdSync(epState.episodeId)
            if (local == null) continue

            val posDiff = Math.abs(local.playbackPosition - epState.playbackPosition)
            val bothRecentlyTouched = local.lastPlayedAt > oneDayAgo && epState.lastPlayedAt > oneDayAgo

            if (posDiff > 30_000L && bothRecentlyTouched) {
                // Conflict detected!
                val podcast = database.podcastDao().getPodcastByIdSync(local.podcastId)
                _conflictFlow.emit(
                    PlaybackConflict(
                        episodeId = local.id,
                        episodeTitle = local.title,
                        podcastTitle = podcast?.title ?: "Podcast",
                        localPositionMs = local.playbackPosition,
                        remotePositionMs = epState.playbackPosition,
                        localTimestamp = local.lastPlayedAt,
                        remoteTimestamp = epState.lastPlayedAt
                    )
                )
            } else {
                // Last write wins
                if (epState.lastPlayedAt > local.lastPlayedAt) {
                    database.episodeDao().updatePlaybackPosition(local.id, epState.playbackPosition, epState.lastPlayedAt)
                    if (epState.isFinished) database.episodeDao().markEpisodeFinished(local.id, true)
                }
            }
        }

        settingsDataStore.updateLastSynced(System.currentTimeMillis())
    }

    suspend fun resolveConflict(episodeId: String, chosenPositionMs: Long) = withContext(Dispatchers.IO) {
        database.episodeDao().updatePlaybackPosition(episodeId, chosenPositionMs, System.currentTimeMillis())
    }
}
