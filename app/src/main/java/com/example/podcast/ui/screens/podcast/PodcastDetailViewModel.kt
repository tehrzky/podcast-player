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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val podcast: StateFlow<Podcast?> = podcastRepository.getPodcastById(podcastId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentSort = MutableStateFlow<EpisodeSortOrder?>(null)
    val currentSort: StateFlow<EpisodeSortOrder?> = _currentSort.asStateFlow()

    val episodes: StateFlow<List<Episode>> = combine(podcast.filterNotNull(), _currentSort) { pod, overrideSort ->
        val effectiveSort = overrideSort
            ?: pod.defaultSortOrder
            ?: settingsDataStore.settingsFlow.first().defaultEpisodeSort
        effectiveSort
    }.flatMapLatest { sort ->
        episodeRepository.getEpisodesSorted(podcastId, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadProgressMap: StateFlow<Map<String, DownloadProgress>> = downloadManager.downloadProgressMap

    /**
     * Called from the UI via LaunchedEffect(podcastId).
     * Ensures the podcast is in Room and, if it has no episodes cached yet,
     * fetches its feed so the episode list populates.
     */
    fun loadPodcast(id: String) {
        if (id.isBlank()) {
            _errorMessage.value = "Invalid podcast ID"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val existing = podcastRepository.getPodcastByIdSync(id)
                if (existing == null) {
                    _errorMessage.value = "Podcast not found. Try searching again."
                    return@launch
                }

                // Fetch feed if we have no episodes cached for this podcast
                val cachedEpisodes = episodes.value
                if (cachedEpisodes.isEmpty()) {
                    podcastRepository.fetchAndPersistFeed(existing.feedUrl)
                }

                val refreshed = podcastRepository.getPodcastByIdSync(id)
                _currentSort.value = refreshed?.defaultSortOrder
            } catch (e: Exception) {
                android.util.Log.e("PodcastDetailVM", "Failed to load podcast $id", e)
                _errorMessage.value = "Failed to load feed: ${e.message ?: "unknown error"}"
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
            _isLoading.value = true
            try {
                podcastRepository.refreshFeed(currentPod.id, currentPod.feedUrl)
            } finally {
                _isLoading.value = false
            }
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
