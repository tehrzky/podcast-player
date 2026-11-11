package com.example.podcast.data.sync

import com.example.podcast.data.local.AppDatabase
import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.Podcast
import com.example.podcast.data.model.QueueItem
import com.example.podcast.data.model.QueueSource
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class RestoreStrategy {
    MERGE,
    REPLACE
}

@Singleton
class JsonBackupManager @Inject constructor(
    private val database: AppDatabase,
    private val syncManager: GoogleDriveSyncManager
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(SyncPayload::class.java)

    suspend fun createFullBackupJson(): String = withContext(Dispatchers.IO) {
        val payload = syncManager.createLocalSyncPayload()
        adapter.indent("  ").toJson(payload)
    }

    suspend fun restoreFromJson(json: String, strategy: RestoreStrategy) = withContext(Dispatchers.IO) {
        val payload = adapter.fromJson(json) ?: throw IllegalArgumentException("Invalid backup JSON format")

        if (strategy == RestoreStrategy.REPLACE) {
            database.queueDao().clearAll()
            database.episodeDao().clearAll()
            database.podcastDao().clearAll()
            database.listeningStatsDao().clearAll()
        }

        // Restore Podcasts
        for (sub in payload.subscriptions) {
            val existing = database.podcastDao().getPodcastByIdSync(sub.id)
            if (existing == null || strategy == RestoreStrategy.REPLACE) {
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
            }
        }

        // Restore Queue
        if (strategy == RestoreStrategy.REPLACE) {
            val queueItems = payload.queue.map {
                QueueItem(
                    episodeId = it.episodeId,
                    position = it.position,
                    source = try { QueueSource.valueOf(it.source) } catch (e: Exception) { QueueSource.AUTO }
                )
            }
            database.queueDao().insertQueueItems(queueItems)
        }
    }
}
