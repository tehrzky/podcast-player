package com.example.podcast.ui.screens.home

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val episodeRepository: EpisodeRepository,
    private val podcastRepository: PodcastRepository,
    private val buildQueueUseCase: BuildQueueUseCase,
    private val playbackController: PlaybackController,
    private val downloadManager: DownloadManager,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val continueListening = episodeRepository.getContinueListening()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentEpisodes = episodeRepository.getSubscribedEpisodesSorted(EpisodeSortOrder.NEWEST_FIRST, 30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed = episodeRepository.getRecentlyPlayed(15)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscribedPodcasts = podcastRepository.getSubscribedPodcasts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadProgressMap: StateFlow<Map<String, DownloadProgress>> = downloadManager.downloadProgressMap

    fun playEpisode(episode: Episode, contextList: List<Episode>) {
        viewModelScope.launch {
            val podcast = podcastRepository.getPodcastByIdSync(episode.podcastId)
            val settings = settingsDataStore.settingsFlow.first()
            val direction = podcast?.defaultPlaybackDirection ?: settings.defaultPlaybackDirection

            val queue = buildQueueUseCase.buildQueueOnEpisodePlay(
                tappedEpisode = episode,
                displayedEpisodes = contextList,
                playbackDirection = direction
            )
            playbackController.submitQueueAndPlay(queue, episode.id)
        }
    }

    fun toggleDownload(episode: Episode) {
        if (episode.isDownloaded) {
            downloadManager.deleteDownloadedFile(episode.id)
        } else {
            downloadManager.startDownload(episode)
        }
    }
}
