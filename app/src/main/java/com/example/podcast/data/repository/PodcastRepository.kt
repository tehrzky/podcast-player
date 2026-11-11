package com.example.podcast.data.repository

import com.example.podcast.data.local.EpisodeDao
import com.example.podcast.data.local.PodcastDao
import com.example.podcast.data.model.EpisodeSortOrder
import com.example.podcast.data.model.PlaybackDirection
import com.example.podcast.data.model.Podcast
import com.example.podcast.data.remote.api.ITunesSearchApi
import com.example.podcast.data.remote.api.PodcastIndexApi
import com.example.podcast.data.remote.api.RssFeedParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastRepository @Inject constructor(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val podcastIndexApi: PodcastIndexApi,
    private val iTunesSearchApi: ITunesSearchApi,
    private val rssFeedParser: RssFeedParser
) {
    fun getSubscribedPodcasts(): Flow<List<Podcast>> = podcastDao.getSubscribedPodcasts()
    fun getRecentlyAddedPodcasts(): Flow<List<Podcast>> = podcastDao.getRecentlyAddedPodcasts()
    fun getPodcastById(id: String): Flow<Podcast?> = podcastDao.getPodcastById(id)
    suspend fun getPodcastByIdSync(id: String): Podcast? = podcastDao.getPodcastByIdSync(id)
    fun getSubscribedCount(): Flow<Int> = podcastDao.getSubscribedCount()

    suspend fun searchPodcasts(query: String): List<Podcast> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Podcast>()
        val seenFeeds = mutableSetOf<String>()

        // 1. Try Podcast Index first
        try {
            val piResponse = podcastIndexApi.searchByTerm(query)
            for (feed in piResponse.feeds) {
                if (feed.url.isNotBlank() && seenFeeds.add(feed.url)) {
                    val catList = feed.categories?.values?.toList() ?: emptyList()
                    results.add(
                        Podcast(
                            id = feed.url.hashCode().toString(),
                            title = feed.title,
                            author = feed.author,
                            artworkUrl = if (feed.artwork.isNotBlank()) feed.artwork else feed.image,
                            feedUrl = feed.url,
                            description = feed.description,
                            categories = catList
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback to iTunes
        }

        // 2. iTunes fallback / merge
        if (results.size < 10) {
            try {
                val itunesResponse = iTunesSearchApi.searchPodcasts(query)
                for (item in itunesResponse.results) {
                    val feedUrl = item.feedUrl
                    if (!feedUrl.isNullOrBlank() && seenFeeds.add(feedUrl)) {
                        results.add(
                            Podcast(
                                id = feedUrl.hashCode().toString(),
                                title = item.collectionName ?: "Podcast",
                                author = item.artistName ?: "Unknown",
                                artworkUrl = item.artworkUrl600 ?: item.artworkUrl100 ?: "",
                                feedUrl = feedUrl,
                                description = "",
                                categories = item.genres ?: listOfNotNull(item.primaryGenreName)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Return whatever we have
            }
        }

        // Persist search results to Room (insert-only, never overwrite existing rows)
        // so PodcastDetailScreen can find them via getPodcastById(id).
        if (results.isNotEmpty()) {
            val now = System.currentTimeMillis()
            results.forEach { p ->
                val existing = podcastDao.getPodcastByIdSync(p.id)
                if (existing == null) {
                    podcastDao.insertPodcast(
                        p.copy(
                            subscribed = false,
                            addedAt = now,
                            lastRefreshedAt = 0L
                        )
                    )
                }
            }
        }

        results
    }

    suspend fun addAndSubscribeFeed(feedUrl: String): Podcast = withContext(Dispatchers.IO) {
        val parsed = rssFeedParser.fetchAndParseFeed(feedUrl)
        val now = System.currentTimeMillis()
        val podcastToSave = parsed.podcast.copy(
            subscribed = true,
            addedAt = now,
            lastRefreshedAt = now
        )
        podcastDao.insertPodcast(podcastToSave)
        episodeDao.insertEpisodes(parsed.episodes)
        podcastToSave
    }

    /**
     * Fetch and persist a feed WITHOUT changing subscription state.
     * Used by PodcastDetailScreen when it opens a search result that has
     * zero episodes cached (feed not yet fetched).
     */
    suspend fun fetchAndPersistFeed(feedUrl: String): Podcast = withContext(Dispatchers.IO) {
        val parsed = rssFeedParser.fetchAndParseFeed(feedUrl)
        val now = System.currentTimeMillis()
        val existing = podcastDao.getPodcastByIdSync(parsed.podcast.id)
        val podcastToSave = parsed.podcast.copy(
            subscribed = existing?.subscribed ?: false,
            addedAt = existing?.addedAt ?: now,
            lastRefreshedAt = now,
            defaultSortOrder = existing?.defaultSortOrder,
            defaultPlaybackDirection = existing?.defaultPlaybackDirection,
            playbackSpeedOverride = existing?.playbackSpeedOverride,
            autoDownloadNew = existing?.autoDownloadNew ?: false,
            autoDownloadLastN = existing?.autoDownloadLastN ?: 0
        )
        podcastDao.insertPodcast(podcastToSave)
        episodeDao.insertEpisodes(parsed.episodes)
        podcastToSave
    }

    suspend fun toggleSubscription(podcast: Podcast) = withContext(Dispatchers.IO) {
        val newSubscribed = !podcast.subscribed
        podcastDao.updateSubscription(podcast.id, newSubscribed, System.currentTimeMillis())
        if (newSubscribed) {
            refreshFeed(podcast.id, podcast.feedUrl)
        }
    }

    suspend fun refreshFeed(podcastId: String, feedUrl: String) = withContext(Dispatchers.IO) {
        try {
            val parsed = rssFeedParser.fetchAndParseFeed(feedUrl)
            podcastDao.updateLastRefreshed(podcastId, System.currentTimeMillis())
            episodeDao.insertEpisodes(parsed.episodes)
        } catch (e: Exception) {
            // Log or ignore
        }
    }

    suspend fun updateSortOrder(podcastId: String, sortOrder: EpisodeSortOrder?) = withContext(Dispatchers.IO) {
        podcastDao.updateSortOrder(podcastId, sortOrder)
    }

    suspend fun updatePlaybackDirection(podcastId: String, direction: PlaybackDirection?) = withContext(Dispatchers.IO) {
        podcastDao.updatePlaybackDirection(podcastId, direction)
    }

    suspend fun updateSpeedOverride(podcastId: String, speed: Float?) = withContext(Dispatchers.IO) {
        podcastDao.updateSpeedOverride(podcastId, speed)
    }
}
