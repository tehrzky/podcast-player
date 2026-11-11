package com.example.podcast.data.repository

import com.example.podcast.data.local.TranscriptDao
import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.TranscriptSegment
import com.example.podcast.data.remote.api.RssFeedParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptRepository @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val rssFeedParser: RssFeedParser,
    private val okHttpClient: OkHttpClient
) {
    fun getTranscript(episodeId: String): Flow<List<TranscriptSegment>> {
        return transcriptDao.getTranscriptForEpisode(episodeId)
    }

    suspend fun hasTranscript(episodeId: String): Boolean = withContext(Dispatchers.IO) {
        transcriptDao.getSegmentCount(episodeId) > 0
    }

    suspend fun loadTranscriptIfAvailable(episode: Episode): Boolean = withContext(Dispatchers.IO) {
        if (transcriptDao.getSegmentCount(episode.id) > 0) {
            return@withContext true
        }

        val transcriptUrl = episode.transcriptUrl ?: return@withContext false
        try {
            val request = Request.Builder().url(transcriptUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val bodyString = response.body?.string() ?: return@withContext false
                val segments = rssFeedParser.parseTranscript(bodyString, episode.id)
                if (segments.isNotEmpty()) {
                    transcriptDao.insertSegments(segments)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            // Ignore error
        }
        false
    }
}
