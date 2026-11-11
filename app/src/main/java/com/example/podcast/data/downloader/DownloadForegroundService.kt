package com.example.podcast.data.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.podcast.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadForegroundService : Service() {

    @Inject
    lateinit var downloadManager: DownloadManager

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Podcast Downloads Active", 0))

        scope.launch {
            downloadManager.downloadProgressMap.collectLatest { progressMap ->
                val activeDownloads = progressMap.values.filter { it.status == com.example.podcast.data.model.DownloadStatus.DOWNLOADING }
                if (activeDownloads.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    val avgProgress = activeDownloads.map { it.progressPercent }.average().toInt()
                    val notification = createNotification("Downloading ${activeDownloads.size} episode(s)...", avgProgress)
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of podcast episode downloads"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, progress: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()

    companion object {
        const val CHANNEL_ID = "podcast_download_channel"
        const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
