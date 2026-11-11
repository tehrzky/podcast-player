package com.example.podcast.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.QueueItem
import com.example.podcast.data.model.QueueSource
import kotlinx.coroutines.flow.Flow

data class QueueItemWithEpisode(
    @Embedded val queueItem: QueueItem,
    @Relation(
        parentColumn = "episodeId",
        entityColumn = "id"
    )
    val episode: Episode?
)

@Dao
interface QueueDao {

    @Transaction
    @Query("SELECT * FROM QueueItem ORDER BY position ASC")
    fun getQueueWithEpisodes(): Flow<List<QueueItemWithEpisode>>

    @Transaction
    @Query("SELECT * FROM QueueItem ORDER BY position ASC")
    suspend fun getQueueWithEpisodesSync(): List<QueueItemWithEpisode>

    @Query("SELECT * FROM QueueItem ORDER BY position ASC")
    suspend fun getQueueItemsSync(): List<QueueItem>

    @Query("SELECT * FROM QueueItem WHERE source = 'MANUAL' ORDER BY position ASC")
    suspend fun getManualItemsSync(): List<QueueItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<QueueItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: QueueItem): Long

    @Query("DELETE FROM QueueItem WHERE source = 'AUTO'")
    suspend fun clearAutoItems()

    @Query("DELETE FROM QueueItem WHERE episodeId = :episodeId")
    suspend fun deleteItemByEpisodeId(episodeId: String)

    @Query("DELETE FROM QueueItem WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM QueueItem")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAutoQueue(newAutoItems: List<QueueItem>) {
        clearAutoItems()
        insertQueueItems(newAutoItems)
    }

    @Transaction
    suspend fun updateAllPositions(items: List<QueueItem>) {
        clearAll()
        insertQueueItems(items)
    }
}
