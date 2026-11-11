package com.example.podcast.ui.screens.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podcast.data.datastore.SettingsDataStore
import com.example.podcast.data.downloader.DownloadManager
import com.example.podcast.data.downloader.DownloadProgress
import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.EpisodeSortOrder
import com.example.podcast.data.model.Podcast
import com.example.podcast.data.repository.EpisodeRepository
import com.example.podcast.data.repository.PodcastRepository
import com.example.podcast.domain.usecase.BuildQueueUseCase
import com.example.podcast.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PodcastDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository,
    private val buildQueueUseCase: BuildQueueUseCase,
    private val playbackController: PlaybackController,
    private val downloadManager: DownloadManager,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val podcastId: String = savedStateHandle.get<String>("podcastId").orEmpty()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val podcast: StateFlow<Podcast?> = podcastRepository.getPodcastById(podcastId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentSort = MutableStateFlow<EpisodeSortOrder?>(null)
    val currentSort: StateFlow<EpisodeSortOrder?> = _currentSort.asStateFlow()

    val episodes: StateFlow<List<Episode>> = combine(podcast.filterNotNull(), _currentSort) { pod, overrideSort ->
        val effectiveSort = overrideSort ?: pod.defaultSortOrder ?: settingsDataStore.settingsFlow.first().defaultEpisodeSort
        effectiveSort
    }.flatMapLatest { sort ->
        episodeRepository.getEpisodesSorted(podcastId, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadProgressMap: StateFlow<Map<String, DownloadProgress>> = downloadManager.downloadProgressMap

    /**
     * Ensure the podcast is persisted in Room before we try to read it.
     * Called from the UI via LaunchedEffect(podcastId).
     * - If it already exists locally: just load sort order.
     * - If it's from search (not in Room yet): fetch its feed and insert.
     */
    fun loadPodcast(id: String) {
        if (id.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Check if it exists in local DB
                val existing = podcastRepository.getPodcastByIdSync(id)

                if (existing == null) {
                    // Not in Room → try to fetch from feed URL (id IS the feed URL for search results)
                    // This uses the repository's fetch-and-persist path.
                    podcastRepository.subscribeAndFetch(id)
                }

                // After ensuring it exists, seed sort state
                val pod = podcastRepository.getPodcastByIdSync(id)
                pod?.let {
                    _currentSort.value = it.defaultSortOrder
                }
            } catch (e: Exception) {
                android.util.Log.e("PodcastDetailVM", "Failed to load podcast $id", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSortOrder(sortOrder: EpisodeSortOrder) {
        _currentSort.value = sortOrder
        viewModelScope.launch {
            podcastRepository.updateSortOrder(podcastId, sortOrder)
        }
    }

    fun toggleSubscription() {
        val currentPod = podcast.value ?: return
        viewModelScope.launch {
            podcastRepository.toggleSubscription(currentPod)
        }
    }

    fun refresh() {
        val currentPod = podcast.value ?: return
        viewModelScope.launch {
            podcastRepository.refreshFeed(currentPod.id, currentPod.feedUrl)
        }
    }

    fun downloadAll() {
        val epList = episodes.value
        for (ep in epList) {
            if (!ep.isDownloaded) {
                downloadManager.startDownload(ep)
            }
        }
    }

    fun playEpisode(episode: Episode) {
        viewModelScope.launch {
            val pod = podcast.value
            val settings = settingsDataStore.settingsFlow.first()
            val direction = pod?.defaultPlaybackDirection ?: settings.defaultPlaybackDirection

            val queue = buildQueueUseCase.buildQueueOnEpisodePlay(
                tappedEpisode = episode,
                displayedEpisodes = episodes.value,
                playbackDirection = direction
            )
            playbackController.submitQueueAndPlay(queue, episode.id)
            episodeRepository.updateDownloadLastAccessed(episode.id)
        }
    }

    fun playLatest() {
        val firstEp = episodes.value.firstOrNull() ?: return
        playEpisode(firstEp)
    }

    fun toggleDownload(episode: Episode) {
        if (episode.isDownloaded) {
            downloadManager.deleteDownloadedFile(episode.id)
        } else {
            downloadManager.startDownload(episode)
        }
    }
}
