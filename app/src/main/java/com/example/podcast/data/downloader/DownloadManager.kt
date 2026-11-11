package com.example.podcast.data.downloader

import android.content.Context
import com.example.podcast.data.datastore.SettingsDataStore
import com.example.podcast.data.local.EpisodeDao
import com.example.podcast.data.model.DownloadStatus
import com.example.podcast.data.model.Episode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val episodeId: String,
    val progressPercent: Int,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val status: DownloadStatus
)

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val episodeDao: EpisodeDao,
    private val settingsDataStore: SettingsDataStore,
    private val okHttpClient: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _downloadProgressMap = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, DownloadProgress>> = _downloadProgressMap.asStateFlow()

    private val activeJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    fun startDownload(episode: Episode) {
        val currentStatus = _downloadProgressMap.value[episode.id]?.status ?: episode.downloadStatus
        if (currentStatus == DownloadStatus.DOWNLOADING || currentStatus == DownloadStatus.QUEUED) {
            return
        }

        updateProgress(episode.id, 0, 0L, 0L, DownloadStatus.QUEUED)
        scope.launch {
            episodeDao.updateDownloadState(episode.id, false, null, DownloadStatus.QUEUED.name)
        }

        val job = scope.launch {
            executeDownload(episode)
        }
        activeJobs[episode.id] = job
    }

    private suspend fun executeDownload(episode: Episode) = withContext(Dispatchers.IO) {
        val downloadDir = File(context.filesDir, "podcasts/${episode.podcastId}").apply { mkdirs() }
        val fileName = "${episode.id.replace(Regex("[^a-zA-Z0-9.-]"), "_")}.mp3"
        val destinationFile = File(downloadDir, fileName)

        updateProgress(episode.id, 0, 0L, 0L, DownloadStatus.DOWNLOADING)
        episodeDao.updateDownloadState(episode.id, false, null, DownloadStatus.DOWNLOADING.name)

        try {
            val request = Request.Builder().url(episode.audioUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("Download HTTP ${response.code}")
                val body = response.body ?: throw IllegalStateException("Empty response body")
                val totalBytes = body.contentLength()
                var bytesRead = 0L

                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(destinationFile)
                val buffer = ByteArray(8192)
                var read: Int

                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    bytesRead += read
                    val percent = if (totalBytes > 0) ((bytesRead * 100) / totalBytes).toInt() else 0
                    updateProgress(episode.id, percent, bytesRead, totalBytes, DownloadStatus.DOWNLOADING)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                val now = System.currentTimeMillis()
                episodeDao.updateDownloadState(
                    id = episode.id,
                    isDownloaded = true,
                    localPath = destinationFile.absolutePath,
                    status = DownloadStatus.DOWNLOADED.name
                )
                episodeDao.updateDownloadLastAccessed(episode.id, now)
                updateProgress(episode.id, 100, bytesRead, totalBytes, DownloadStatus.DOWNLOADED)
            }
        } catch (e: Exception) {
            if (destinationFile.exists()) destinationFile.delete()
            episodeDao.updateDownloadState(episode.id, false, null, DownloadStatus.FAILED.name)
            updateProgress(episode.id, 0, 0L, 0L, DownloadStatus.FAILED)
        } finally {
            activeJobs.remove(episode.id)
        }
    }

    fun cancelDownload(episodeId: String) {
        activeJobs[episodeId]?.cancel()
        activeJobs.remove(episodeId)
        scope.launch {
            val ep = episodeDao.getEpisodeByIdSync(episodeId)
            if (ep?.localFilePath != null) {
                val file = File(ep.localFilePath)
                if (file.exists()) file.delete()
            }
            episodeDao.updateDownloadState(episodeId, false, null, DownloadStatus.NOT_DOWNLOADED.name)
            _downloadProgressMap.value = _downloadProgressMap.value - episodeId
        }
    }

    fun deleteDownloadedFile(episodeId: String) {
        scope.launch {
            val ep = episodeDao.getEpisodeByIdSync(episodeId)
            if (ep?.localFilePath != null) {
                val file = File(ep.localFilePath)
                if (file.exists()) file.delete()
            }
            episodeDao.updateDownloadState(episodeId, false, null, DownloadStatus.NOT_DOWNLOADED.name)
            _downloadProgressMap.value = _downloadProgressMap.value - episodeId
        }
    }

    fun recordAccess(episodeId: String) {
        scope.launch {
            episodeDao.updateDownloadLastAccessed(episodeId, System.currentTimeMillis())
        }
    }

    private fun updateProgress(id: String, percent: Int, bytes: Long, total: Long, status: DownloadStatus) {
        val current = _downloadProgressMap.value.toMutableMap()
        current[id] = DownloadProgress(id, percent, bytes, total, status)
        _downloadProgressMap.value = current
    }
}
