package com.example.podcast.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.podcast.data.datastore.SettingsDataStore
import com.example.podcast.data.downloader.DownloadManager
import com.example.podcast.data.local.EpisodeDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
class DownloadRetentionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val episodeDao: EpisodeDao,
    private val downloadManager: DownloadManager,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val settings = settingsDataStore.settingsFlow.first()
            val retentionMs = settings.retentionDays * 24L * 60L * 60L * 1000L
            val threshold = System.currentTimeMillis() - retentionMs

            val staleEpisodes = episodeDao.getStaleDownloads(threshold)
            for (episode in staleEpisodes) {
                downloadManager.deleteDownloadedFile(episode.id)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
