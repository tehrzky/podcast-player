package com.example.podcast.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.podcast.data.local.EpisodeDao
import com.example.podcast.data.local.PodcastDao
import com.example.podcast.data.local.QueueItemWithEpisode
import com.example.podcast.data.model.Episode
import com.example.podcast.data.repository.QueueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueManager @Inject constructor(
    private val queueRepository: QueueRepository,
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao
) {
    suspend fun getMediaItemsFromQueue(): List<MediaItem> = withContext(Dispatchers.IO) {
        val queueWithEpisodes = queueRepository.getQueueSync()
        queueWithEpisodes.mapNotNull { item ->
            val episode = item.episode ?: return@mapNotNull null
            val podcast = podcastDao.getPodcastByIdSync(episode.podcastId)

            // If downloaded, play local file; otherwise stream audioUrl
            val uri = if (episode.isDownloaded && !episode.localFilePath.isNullOrBlank()) {
                "file://${episode.localFilePath}"
            } else {
                episode.audioUrl
            }

            MediaItem.Builder()
                .setMediaId(episode.id)
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(episode.title)
                        .setArtist(podcast?.title ?: "")
                        .setAlbumArtist(podcast?.author ?: "")
                        .setArtworkUri(podcast?.artworkUrl?.let { android.net.Uri.parse(it) })
                        .build()
                )
                .build()
        }
    }
}
