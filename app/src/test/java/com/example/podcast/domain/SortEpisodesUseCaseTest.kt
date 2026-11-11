package com.example.podcast.domain

import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.EpisodeSortOrder
import com.example.podcast.domain.usecase.SortEpisodesUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class SortEpisodesUseCaseTest {

    private val sortEpisodesUseCase = SortEpisodesUseCase()

    @Test
    fun `test OLDEST_FIRST orders by publishedDate ascending`() {
        val ep1 = createEpisode("ep1", pubDate = 100L)
        val ep2 = createEpisode("ep2", pubDate = 200L)
        val ep3 = createEpisode("ep3", pubDate = 300L)

        val sorted = sortEpisodesUseCase.sort(listOf(ep3, ep1, ep2), EpisodeSortOrder.OLDEST_FIRST)
        assertEquals(listOf("ep1", "ep2", "ep3"), sorted.map { it.id })
    }

    @Test
    fun `test NEWEST_FIRST orders by publishedDate descending`() {
        val ep1 = createEpisode("ep1", pubDate = 100L)
        val ep2 = createEpisode("ep2", pubDate = 200L)
        val ep3 = createEpisode("ep3", pubDate = 300L)

        val sorted = sortEpisodesUseCase.sort(listOf(ep1, ep3, ep2), EpisodeSortOrder.NEWEST_FIRST)
        assertEquals(listOf("ep3", "ep2", "ep1"), sorted.map { it.id })
    }

    @Test
    fun `test EPISODE_ASC sorts by episode number with null fallback to pubDate`() {
        val epSpecial = createEpisode("special", pubDate = 50L, epNum = null)
        val ep1 = createEpisode("ep1", pubDate = 100L, epNum = 1)
        val ep2 = createEpisode("ep2", pubDate = 200L, epNum = 2)

        val sorted = sortEpisodesUseCase.sort(listOf(ep2, epSpecial, ep1), EpisodeSortOrder.EPISODE_ASC)
        assertEquals(listOf("ep1", "ep2", "special"), sorted.map { it.id })
    }

    @Test
    fun `test TITLE_ASC sorts alphabetically`() {
        val epA = createEpisode("epA", title = "Apple")
        val epB = createEpisode("epB", title = "Banana")
        val epC = createEpisode("epC", title = "Cherry")

        val sorted = sortEpisodesUseCase.sort(listOf(epC, epA, epB), EpisodeSortOrder.TITLE_ASC)
        assertEquals(listOf("epA", "epB", "epC"), sorted.map { it.id })
    }

    @Test
    fun `test UNPLAYED_FIRST puts unfinished episodes first`() {
        val finished = createEpisode("fin", isFinished = true, pubDate = 300L)
        val unplayed = createEpisode("unplay", isFinished = false, pubDate = 200L)

        val sorted = sortEpisodesUseCase.sort(listOf(finished, unplayed), EpisodeSortOrder.UNPLAYED_FIRST)
        assertEquals(listOf("unplay", "fin"), sorted.map { it.id })
    }

    private fun createEpisode(
        id: String,
        title: String = "Title",
        pubDate: Long = 1000L,
        epNum: Int? = null,
        isFinished: Boolean = false,
        isDownloaded: Boolean = false
    ): Episode {
        return Episode(
            id = id,
            podcastId = "pod1",
            title = title,
            description = "Desc",
            audioUrl = "http://test.mp3",
            publishedDate = pubDate,
            episodeNumber = epNum,
            isFinished = isFinished,
            isDownloaded = isDownloaded
        )
    }
}
