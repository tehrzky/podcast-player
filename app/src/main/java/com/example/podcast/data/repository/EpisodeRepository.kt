package com.example.podcast.data.repository

import com.example.podcast.data.local.EpisodeDao
import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.EpisodeSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeRepository @Inject constructor(
    private val episodeDao: EpisodeDao
) {
    fun getEpisodesSorted(podcastId: String, sortOrder: EpisodeSortOrder): Flow<List<Episode>> {
        return episodeDao.getEpisodesSorted(podcastId, sortOrder.name)
    }

    suspend fun getEpisodesSortedSync(podcastId: String, sortOrder: EpisodeSortOrder): List<Episode> {
        return episodeDao.getEpisodesSortedSync(podcastId, sortOrder.name)
    }

    fun getAllEpisodesSorted(sortOrder: EpisodeSortOrder): Flow<List<Episode>> {
        return episodeDao.getAllEpisodesSorted(sortOrder.name)
    }

    fun getSubscribedEpisodesSorted(sortOrder: EpisodeSortOrder, limit: Int = 50): Flow<List<Episode>> {
        return episodeDao.getSubscribedEpisodesSorted(sortOrder.name, limit)
    }

    fun getEpisodeById(id: String): Flow<Episode?> = episodeDao.getEpisodeById(id)
    suspend fun getEpisodeByIdSync(id: String): Episode? = episodeDao.getEpisodeByIdSync(id)

    fun getContinueListening(): Flow<List<Episode>> = episodeDao.getContinueListeningEpisodes()
    fun getRecentlyPlayed(limit: Int = 20): Flow<List<Episode>> = episodeDao.getRecentlyPlayedEpisodes(limit)
    fun getDownloadedEpisodes(): Flow<List<Episode>> = episodeDao.getDownloadedEpisodes()

    suspend fun updatePlaybackPosition(id: String, positionMs: Long) = withContext(Dispatchers.IO) {
        episodeDao.updatePlaybackPosition(id, positionMs, System.currentTimeMillis())
    }

    suspend fun markFinished(id: String, finished: Boolean) = withContext(Dispatchers.IO) {
        episodeDao.markEpisodeFinished(id, finished)
    }

    suspend fun updateDownloadLastAccessed(id: String) = withContext(Dispatchers.IO) {
        episodeDao.updateDownloadLastAccessed(id, System.currentTimeMillis())
    }
}
