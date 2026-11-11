package com.example.podcast.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.podcast.data.model.Episode
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Query("""
      SELECT * FROM Episode WHERE podcastId = :podcastId
      ORDER BY
        CASE :sortOrder
          WHEN 'OLDEST_FIRST' THEN publishedDate
          WHEN 'NEWEST_FIRST' THEN -publishedDate
          WHEN 'EPISODE_ASC' THEN COALESCE(episodeNumber, 999999)
          WHEN 'EPISODE_DESC' THEN -COALESCE(episodeNumber, 999999)
          WHEN 'TITLE_ASC' THEN title
          WHEN 'UNPLAYED_FIRST' THEN isFinished
          WHEN 'DOWNLOADED_FIRST' THEN -isDownloaded
        END ASC
    """)
    fun getEpisodesSorted(podcastId: String, sortOrder: String): Flow<List<Episode>>

    @Query("""
      SELECT * FROM Episode WHERE podcastId = :podcastId
      ORDER BY
        CASE :sortOrder
          WHEN 'OLDEST_FIRST' THEN publishedDate
          WHEN 'NEWEST_FIRST' THEN -publishedDate
          WHEN 'EPISODE_ASC' THEN COALESCE(episodeNumber, 999999)
          WHEN 'EPISODE_DESC' THEN -COALESCE(episodeNumber, 999999)
          WHEN 'TITLE_ASC' THEN title
          WHEN 'UNPLAYED_FIRST' THEN isFinished
          WHEN 'DOWNLOADED_FIRST' THEN -isDownloaded
        END ASC
    """)
    suspend fun getEpisodesSortedSync(podcastId: String, sortOrder: String): List<Episode>

    @Query("""
      SELECT * FROM Episode
      ORDER BY
        CASE :sortOrder
          WHEN 'OLDEST_FIRST' THEN publishedDate
          WHEN 'NEWEST_FIRST' THEN -publishedDate
          WHEN 'EPISODE_ASC' THEN COALESCE(episodeNumber, 999999)
          WHEN 'EPISODE_DESC' THEN -COALESCE(episodeNumber, 999999)
          WHEN 'TITLE_ASC' THEN title
          WHEN 'UNPLAYED_FIRST' THEN isFinished
          WHEN 'DOWNLOADED_FIRST' THEN -isDownloaded
        END ASC
    """)
    fun getAllEpisodesSorted(sortOrder: String): Flow<List<Episode>>

    @Query("""
      SELECT e.* FROM Episode e
      INNER JOIN Podcast p ON e.podcastId = p.id
      WHERE p.subscribed = 1
      ORDER BY
        CASE :sortOrder
          WHEN 'OLDEST_FIRST' THEN e.publishedDate
          WHEN 'NEWEST_FIRST' THEN -e.publishedDate
          WHEN 'EPISODE_ASC' THEN COALESCE(e.episodeNumber, 999999)
          WHEN 'EPISODE_DESC' THEN -COALESCE(e.episodeNumber, 999999)
          WHEN 'TITLE_ASC' THEN e.title
          WHEN 'UNPLAYED_FIRST' THEN e.isFinished
          WHEN 'DOWNLOADED_FIRST' THEN -e.isDownloaded
        END ASC
      LIMIT :limit
    """)
    fun getSubscribedEpisodesSorted(sortOrder: String, limit: Int = 50): Flow<List<Episode>>

    @Query("SELECT * FROM Episode WHERE id = :id")
    fun getEpisodeById(id: String): Flow<Episode?>

    @Query("SELECT * FROM Episode WHERE id = :id")
    suspend fun getEpisodeByIdSync(id: String): Episode?

    @Query("SELECT * FROM Episode WHERE isStarted = 1 AND isFinished = 0 ORDER BY lastPlayedAt DESC")
    fun getContinueListeningEpisodes(): Flow<List<Episode>>

    @Query("SELECT * FROM Episode WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentlyPlayedEpisodes(limit: Int = 20): Flow<List<Episode>>

    @Query("SELECT * FROM Episode WHERE isDownloaded = 1 ORDER BY downloadLastAccessedAt DESC")
    fun getDownloadedEpisodes(): Flow<List<Episode>>

    @Query("SELECT * FROM Episode WHERE isDownloaded = 1 AND downloadLastAccessedAt < :staleThreshold")
    suspend fun getStaleDownloads(staleThreshold: Long): List<Episode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<Episode>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: Episode)

    @Update
    suspend fun updateEpisode(episode: Episode)

    @Query("UPDATE Episode SET playbackPosition = :position, lastPlayedAt = :timestamp, isStarted = 1 WHERE id = :id")
    suspend fun updatePlaybackPosition(id: String, position: Long, timestamp: Long)

    @Query("UPDATE Episode SET isFinished = :finished WHERE id = :id")
    suspend fun markEpisodeFinished(id: String, finished: Boolean)

    @Query("UPDATE Episode SET downloadLastAccessedAt = :timestamp WHERE id = :id")
    suspend fun updateDownloadLastAccessed(id: String, timestamp: Long)

    @Query("UPDATE Episode SET isDownloaded = :isDownloaded, localFilePath = :localPath, downloadStatus = :status WHERE id = :id")
    suspend fun updateDownloadState(id: String, isDownloaded: Boolean, localPath: String?, status: String)

    @Query("DELETE FROM Episode WHERE podcastId = :podcastId")
    suspend fun deleteEpisodesByPodcast(podcastId: String)

    @Query("DELETE FROM Episode")
    suspend fun clearAll()
}
