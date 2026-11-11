package com.example.podcast.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podcast.data.datastore.SettingsDataStore
import com.example.podcast.data.downloader.DownloadManager
import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.EpisodeSortOrder
import com.example.podcast.data.model.LibraryViewMode
import com.example.podcast.data.model.Podcast
import com.example.podcast.data.repository.EpisodeRepository
import com.example.podcast.data.repository.PodcastRepository
import com.example.podcast.data.repository.StatsRepository
import com.example.podcast.domain.usecase.BuildQueueUseCase
import com.example.podcast.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository,
    private val statsRepository: StatsRepository,
    private val settingsDataStore: SettingsDataStore,
    private val buildQueueUseCase: BuildQueueUseCase,
    private val playbackController: PlaybackController,
    private val downloadManager: DownloadManager
) : ViewModel() {

    val stats = statsRepository.getAggregatedStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subscribedCount = podcastRepository.getSubscribedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val podcasts = podcastRepository.getSubscribedPodcasts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedEpisodeSort = MutableStateFlow(EpisodeSortOrder.OLDEST_FIRST)
    val selectedEpisodeSort: StateFlow<EpisodeSortOrder> = _selectedEpisodeSort.asStateFlow()

    val episodes = _selectedEpisodeSort.flatMapLatest { sort ->
        episodeRepository.getSubscribedEpisodesSorted(sort, 100)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedEpisodes = episodeRepository.getDownloadedEpisodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val downloadProgressMap = downloadManager.downloadProgressMap

    init {
        viewModelScope.launch {
            val initialSort = settingsDataStore.settingsFlow.first().defaultEpisodeSort
            _selectedEpisodeSort.value = initialSort
        }
    }

    fun setEpisodeSort(order: EpisodeSortOrder) {
        _selectedEpisodeSort.value = order
    }

    fun setViewMode(mode: LibraryViewMode) {
        viewModelScope.launch {
            settingsDataStore.updateLibraryViewMode(mode)
        }
    }

    fun playEpisode(episode: Episode, contextList: List<Episode>) {
        viewModelScope.launch {
            val podcast = podcastRepository.getPodcastByIdSync(episode.podcastId)
            val currentSettings = settingsDataStore.settingsFlow.first()
            val direction = podcast?.defaultPlaybackDirection ?: currentSettings.defaultPlaybackDirection

            val queue = buildQueueUseCase.buildQueueOnEpisodePlay(
                tappedEpisode = episode,
                displayedEpisodes = contextList,
                playbackDirection = direction
            )
            playbackController.submitQueueAndPlay(queue, episode.id)
            episodeRepository.updateDownloadLastAccessed(episode.id)
        }
    }

    fun toggleDownload(episode: Episode) {
        if (episode.isDownloaded) {
            downloadManager.deleteDownloadedFile(episode.id)
        } else {
            downloadManager.startDownload(episode)
        }
    }

    fun deleteDownload(episodeId: String) {
        downloadManager.deleteDownloadedFile(episodeId)
    }

    fun unsubscribePodcast(podcast: Podcast) {
        viewModelScope.launch {
            podcastRepository.toggleSubscription(podcast)
        }
    }
}
