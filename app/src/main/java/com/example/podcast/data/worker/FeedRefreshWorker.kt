package com.example.podcast.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.podcast.data.downloader.DownloadManager
import com.example.podcast.data.local.EpisodeDao
import com.example.podcast.data.local.PodcastDao
import com.example.podcast.data.remote.api.RssFeedParser
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class FeedRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val rssFeedParser: RssFeedParser,
    private val downloadManager: DownloadManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val podcasts = podcastDao.getSubscribedPodcastsSync()
            for (podcast in podcasts) {
                try {
                    val parsed = rssFeedParser.fetchAndParseFeed(podcast.feedUrl)
                    podcastDao.insertPodcast(podcast.copy(lastRefreshedAt = System.currentTimeMillis()))
                    episodeDao.insertEpisodes(parsed.episodes)

                    // Auto-download new episodes if enabled
                    if (podcast.autoDownloadNew && parsed.episodes.isNotEmpty()) {
                        val latestEpisodes = parsed.episodes.take(podcast.autoDownloadLastN)
                        for (ep in latestEpisodes) {
                            if (!ep.isDownloaded) {
                                downloadManager.startDownload(ep)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Continue refreshing other podcasts
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
