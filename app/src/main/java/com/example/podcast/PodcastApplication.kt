package com.example.podcast

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.podcast.data.downloader.DownloadForegroundService
import com.example.podcast.data.worker.DownloadRetentionWorker
import com.example.podcast.data.worker.FeedRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class PodcastApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleBackgroundWorkers()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val playbackChannel = NotificationChannel(
                "podcast_playback_channel",
                getString(R.string.playback_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.playback_notification_channel_desc)
            }

            val downloadChannel = NotificationChannel(
                DownloadForegroundService.CHANNEL_ID,
                getString(R.string.download_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.download_notification_channel_desc)
            }

            notificationManager.createNotificationChannel(playbackChannel)
            notificationManager.createNotificationChannel(downloadChannel)
        }
    }

    private fun scheduleBackgroundWorkers() {
        val workManager = WorkManager.getInstance(this)

        // 1. Periodic Feed Refresh (every 6 hours)
        val feedRefreshRequest = PeriodicWorkRequestBuilder<FeedRefreshWorker>(6, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "FeedRefreshWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            feedRefreshRequest
        )

        // 2. Daily Download Retention Worker (every 24 hours)
        val retentionRequest = PeriodicWorkRequestBuilder<DownloadRetentionWorker>(24, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "DownloadRetentionWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            retentionRequest
        )
    }
}
