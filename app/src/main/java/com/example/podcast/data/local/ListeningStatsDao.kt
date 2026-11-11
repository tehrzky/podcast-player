package com.example.podcast.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.podcast.data.model.ListeningStat
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningStatsDao {

    @Query("SELECT * FROM ListeningStat WHERE date = :date")
    suspend fun getStatForDate(date: String): ListeningStat?

    @Query("SELECT * FROM ListeningStat ORDER BY date DESC")
    fun getAllStats(): Flow<List<ListeningStat>>

    @Query("SELECT * FROM ListeningStat WHERE date >= :startDate ORDER BY date ASC")
    fun getStatsSince(startDate: String): Flow<List<ListeningStat>>

    @Query("SELECT COALESCE(SUM(secondsListened), 0) FROM ListeningStat")
    fun getTotalSecondsAllTime(): Flow<Long>

    @Query("SELECT COALESCE(SUM(secondsListened), 0) FROM ListeningStat WHERE date >= :startDate")
    fun getSecondsSince(startDate: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(episodesStarted), 0) FROM ListeningStat")
    fun getTotalEpisodesStarted(): Flow<Int>

    @Query("SELECT COALESCE(SUM(episodesFinished), 0) FROM ListeningStat")
    fun getTotalEpisodesFinished(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stat: ListeningStat)

    @Transaction
    suspend fun addListeningTime(date: String, seconds: Long) {
        val current = getStatForDate(date) ?: ListeningStat(date = date)
        insertOrUpdate(current.copy(secondsListened = current.secondsListened + seconds))
    }

    @Transaction
    suspend fun recordEpisodeStarted(date: String) {
        val current = getStatForDate(date) ?: ListeningStat(date = date)
        insertOrUpdate(current.copy(episodesStarted = current.episodesStarted + 1))
    }

    @Transaction
    suspend fun recordEpisodeFinished(date: String) {
        val current = getStatForDate(date) ?: ListeningStat(date = date)
        insertOrUpdate(current.copy(episodesFinished = current.episodesFinished + 1))
    }

    @Query("DELETE FROM ListeningStat")
    suspend fun clearAll()
}
