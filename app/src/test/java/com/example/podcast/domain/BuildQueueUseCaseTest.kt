package com.example.podcast.domain

import com.example.podcast.data.local.QueueItemWithEpisode
import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.PlaybackDirection
import com.example.podcast.data.model.QueueItem
import com.example.podcast.data.model.QueueSource
import com.example.podcast.data.repository.QueueRepository
import com.example.podcast.domain.usecase.BuildQueueUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BuildQueueUseCaseTest {

    private val queueRepository: QueueRepository = mockk(relaxed = true)
    private lateinit var buildQueueUseCase: BuildQueueUseCase

    @Before
    fun setup() {
        buildQueueUseCase = BuildQueueUseCase(queueRepository)
    }

    @Test
    fun `when tapping episode with direction DOWN, walks list downward and preserves manual items`() = runTest {
        val ep1 = createTestEpisode("ep1", 1000L, 1)
        val ep2 = createTestEpisode("ep2", 2000L, 2)
        val ep3 = createTestEpisode("ep3", 3000L, 3)
        val ep4 = createTestEpisode("ep4", 4000L, 4)
        val displayedList = listOf(ep1, ep2, ep3, ep4)

        // Mock current queue containing a manual item
        val manualQueueItem = QueueItem(id = 1L, episodeId = "manual_ep", position = 0, source = QueueSource.MANUAL)
        val manualEpisode = createTestEpisode("manual_ep", 500L, null)

        coEvery { queueRepository.getQueueSync() } returns listOf(
            QueueItemWithEpisode(manualQueueItem, manualEpisode),
            QueueItemWithEpisode(QueueItem(id = 2L, episodeId = "ep2", position = 1, source = QueueSource.AUTO), ep2),
            QueueItemWithEpisode(QueueItem(id = 3L, episodeId = "ep3", position = 2, source = QueueSource.AUTO), ep3),
            QueueItemWithEpisode(QueueItem(id = 4L, episodeId = "ep4", position = 3, source = QueueSource.AUTO), ep4)
        )

        val result = buildQueueUseCase.buildQueueOnEpisodePlay(
            tappedEpisode = ep2,
            displayedEpisodes = displayedList,
            playbackDirection = PlaybackDirection.DOWN
        )

        // DOWN direction walks from ep2 downward: ep2, ep3, ep4
        coVerify {
            queueRepository.replaceAutoQueue(listOf("ep2", "ep3", "ep4"))
        }

        // Result flattened: manual first, then auto
        assertEquals(listOf("manual_ep", "ep2", "ep3", "ep4"), result)
    }

    @Test
    fun `when tapping episode with direction UP, walks list upward for oldest-first binging`() = runTest {
        val ep1 = createTestEpisode("ep1", 1000L, 1)
        val ep2 = createTestEpisode("ep2", 2000L, 2)
        val ep3 = createTestEpisode("ep3", 3000L, 3)
        val displayedList = listOf(ep1, ep2, ep3)

        coEvery { queueRepository.getQueueSync() } returns listOf(
            QueueItemWithEpisode(QueueItem(id = 1L, episodeId = "ep2", position = 0, source = QueueSource.AUTO), ep2),
            QueueItemWithEpisode(QueueItem(id = 2L, episodeId = "ep1", position = 1, source = QueueSource.AUTO), ep1)
        )

        val result = buildQueueUseCase.buildQueueOnEpisodePlay(
            tappedEpisode = ep2,
            displayedEpisodes = displayedList,
            playbackDirection = PlaybackDirection.UP
        )

        // UP direction walks from ep2 upward: ep2, ep1
        coVerify {
            queueRepository.replaceAutoQueue(listOf("ep2", "ep1"))
        }

        assertEquals(listOf("ep2", "ep1"), result)
    }

    private fun createTestEpisode(id: String, pubDate: Long, epNum: Int?): Episode {
        return Episode(
            id = id,
            podcastId = "test_pod",
            title = "Episode $id",
            description = "Desc",
            audioUrl = "http://test.mp3",
            publishedDate = pubDate,
            episodeNumber = epNum
        )
    }
}
