package com.example.podcast.domain.usecase

import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.PlaybackDirection
import com.example.podcast.data.model.QueueItem
import com.example.podcast.data.model.QueueSource
import com.example.podcast.data.repository.QueueRepository
import javax.inject.Inject

class BuildQueueUseCase @Inject constructor(
    private val queueRepository: QueueRepository
) {
    /**
     * Executes the Queue Builder Contract:
     * - Read currently displayed, sorted episode list
     * - Clear existing AUTO items, preserve MANUAL items
     * - Append tapped episode + subsequent episodes according to playback direction
     * - Flattened list = MANUAL items + AUTO items
     */
    suspend fun buildQueueOnEpisodePlay(
        tappedEpisode: Episode,
        displayedEpisodes: List<Episode>,
        playbackDirection: PlaybackDirection
    ): List<String> {
        val tappedIndex = displayedEpisodes.indexOfFirst { it.id == tappedEpisode.id }
        if (tappedIndex == -1) {
            // Fallback if tapped episode is not in list
            queueRepository.replaceAutoQueue(listOf(tappedEpisode.id))
            return listOf(tappedEpisode.id)
        }

        val autoEpisodes = mutableListOf<Episode>()
        when (playbackDirection) {
            PlaybackDirection.DOWN -> {
                // Walk list downward from tapped episode (tappedIndex .. displayedEpisodes.lastIndex)
                for (i in tappedIndex until displayedEpisodes.size) {
                    autoEpisodes.add(displayedEpisodes[i])
                }
            }
            PlaybackDirection.UP -> {
                // Walk list upward from tapped episode (tappedIndex downTo 0)
                for (i in tappedIndex downTo 0) {
                    autoEpisodes.add(displayedEpisodes[i])
                }
            }
        }

        val autoEpisodeIds = autoEpisodes.map { it.id }
        queueRepository.replaceAutoQueue(autoEpisodeIds)

        // Return the full flattened queue episode IDs (MANUAL first, then AUTO)
        val currentQueue = queueRepository.getQueueSync()
        return currentQueue.mapNotNull { it.episode?.id }
    }
}
