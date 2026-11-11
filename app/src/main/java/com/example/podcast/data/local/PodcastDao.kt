package com.example.podcast.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.podcast.data.model.EpisodeSortOrder
import com.example.podcast.data.model.PlaybackDirection
import com.example.podcast.data.model.Podcast
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {

    @Query("SELECT * FROM Podcast WHERE subscribed = 1 ORDER BY title ASC")
    fun getSubscribedPodcasts(): Flow<List<Podcast>>

    @Query("SELECT * FROM Podcast WHERE subscribed = 1 ORDER BY addedAt DESC")
    fun getRecentlyAddedPodcasts(): Flow<List<Podcast>>

    @Query("SELECT * FROM Podcast WHERE subscribed = 1")
    suspend fun getSubscribedPodcastsSync(): List<Podcast>

    @Query("SELECT * FROM Podcast WHERE id = :id")
    fun getPodcastById(id: String): Flow<Podcast?>

    @Query("SELECT * FROM Podcast WHERE id = :id")
    suspend fun getPodcastByIdSync(id: String): Podcast?

    @Query("SELECT * FROM Podcast")
    fun getAllPodcasts(): Flow<List<Podcast>>

    @Query("SELECT COUNT(*) FROM Podcast WHERE subscribed = 1")
    fun getSubscribedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPodcast(podcast: Podcast)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPodcasts(podcasts: List<Podcast>)

    @Update
    suspend fun updatePodcast(podcast: Podcast)

    @Query("UPDATE Podcast SET subscribed = :subscribed, addedAt = :addedAt WHERE id = :id")
    suspend fun updateSubscription(id: String, subscribed: Boolean, addedAt: Long)

    @Query("UPDATE Podcast SET defaultSortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: EpisodeSortOrder?)

    @Query("UPDATE Podcast SET defaultPlaybackDirection = :direction WHERE id = :id")
    suspend fun updatePlaybackDirection(id: String, direction: PlaybackDirection?)

    @Query("UPDATE Podcast SET playbackSpeedOverride = :speed WHERE id = :id")
    suspend fun updateSpeedOverride(id: String, speed: Float?)

    @Query("UPDATE Podcast SET lastRefreshedAt = :timestamp WHERE id = :id")
    suspend fun updateLastRefreshed(id: String, timestamp: Long)

    @Query("DELETE FROM Podcast WHERE id = :id")
    suspend fun deletePodcast(id: String)

    @Query("DELETE FROM Podcast")
    suspend fun clearAll()
}
