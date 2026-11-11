package com.example.podcast.data.repository

import com.example.podcast.data.local.QueueDao
import com.example.podcast.data.local.QueueItemWithEpisode
import com.example.podcast.data.model.QueueItem
import com.example.podcast.data.model.QueueSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueRepository @Inject constructor(
    private val queueDao: QueueDao
) {
    fun getQueue(): Flow<List<QueueItemWithEpisode>> = queueDao.getQueueWithEpisodes()
    suspend fun getQueueSync(): List<QueueItemWithEpisode> = queueDao.getQueueWithEpisodesSync()
    suspend fun getQueueItemsSync(): List<QueueItem> = queueDao.getQueueItemsSync()

    suspend fun addPlayNext(episodeId: String) = withContext(Dispatchers.IO) {
        val currentItems = queueDao.getQueueItemsSync().toMutableList()
        val manualItems = currentItems.filter { it.source == QueueSource.MANUAL }
        val insertIndex = manualItems.size // Place right after current manual items

        val newItem = QueueItem(
            episodeId = episodeId,
            position = insertIndex,
            source = QueueSource.MANUAL
        )

        currentItems.add(insertIndex, newItem)
        reindexAndSave(currentItems)
    }

    suspend fun addToQueue(episodeId: String) = withContext(Dispatchers.IO) {
        val currentItems = queueDao.getQueueItemsSync().toMutableList()
        val manualItems = currentItems.filter { it.source == QueueSource.MANUAL }
        val insertIndex = manualItems.size // End of manual block

        val newItem = QueueItem(
            episodeId = episodeId,
            position = insertIndex,
            source = QueueSource.MANUAL
        )

        currentItems.add(insertIndex, newItem)
        reindexAndSave(currentItems)
    }

    suspend fun reorderQueue(newOrder: List<QueueItem>) = withContext(Dispatchers.IO) {
        reindexAndSave(newOrder)
    }

    suspend fun removeFromQueue(episodeId: String) = withContext(Dispatchers.IO) {
        queueDao.deleteItemByEpisodeId(episodeId)
        val remaining = queueDao.getQueueItemsSync()
        reindexAndSave(remaining)
    }

    suspend fun replaceAutoQueue(newAutoEpisodeIds: List<String>) = withContext(Dispatchers.IO) {
        val manualItems = queueDao.getManualItemsSync()
        var pos = manualItems.size
        val autoItems = newAutoEpisodeIds.map { epId ->
            QueueItem(
                episodeId = epId,
                position = pos++,
                source = QueueSource.AUTO
            )
        }
        val combined = manualItems + autoItems
        queueDao.updateAllPositions(combined)
    }

    private suspend fun reindexAndSave(items: List<QueueItem>) {
        val reindexed = items.mapIndexed { index, item -> item.copy(position = index) }
        queueDao.updateAllPositions(reindexed)
    }
}
