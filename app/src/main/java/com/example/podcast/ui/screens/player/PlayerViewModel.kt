package com.example.podcast.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podcast.data.downloader.DownloadManager
import com.example.podcast.data.model.TranscriptSegment
import com.example.podcast.data.repository.EpisodeRepository
import com.example.podcast.data.repository.PodcastRepository
import com.example.podcast.data.repository.TranscriptRepository
import com.example.podcast.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val transcriptRepository: TranscriptRepository,
    private val episodeRepository: EpisodeRepository,
    private val podcastRepository: PodcastRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    val currentEpisode = playbackController.currentEpisode
    val currentPodcast = playbackController.currentPodcast
    val isPlaying = playbackController.isPlaying
    val currentPosition = playbackController.currentPosition
    val duration = playbackController.duration
    val playbackSpeed = playbackController.playbackSpeed
    val sleepTimerSeconds = playbackController.sleepTimerRemainingSeconds

    private val _transcriptSegments = MutableStateFlow<List<TranscriptSegment>>(emptyList())
    val transcriptSegments: StateFlow<List<TranscriptSegment>> = _transcriptSegments.asStateFlow()

    private val _hasTranscript = MutableStateFlow(false)
    val hasTranscript: StateFlow<Boolean> = _hasTranscript.asStateFlow()

    init {
        viewModelScope.launch {
            currentEpisode.collectLatest { episode ->
                if (episode != null) {
                    val available = transcriptRepository.loadTranscriptIfAvailable(episode)
                    _hasTranscript.value = available
                    if (available) {
                        transcriptRepository.getTranscript(episode.id).collectLatest {
                            _transcriptSegments.value = it
                        }
                    } else {
                        _transcriptSegments.value = emptyList()
                    }
                } else {
                    _hasTranscript.value = false
                    _transcriptSegments.value = emptyList()
                }
            }
        }
    }

    fun playPause() = playbackController.playPause()
    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)
    fun skipForward() = playbackController.skipForward()
    fun skipBack() = playbackController.skipBack()
    fun setSpeed(speed: Float) = playbackController.setSpeed(speed)
    fun setSleepTimer(minutes: Int?) = playbackController.setSleepTimer(minutes)

    fun toggleDownload() {
        val ep = currentEpisode.value ?: return
        if (ep.isDownloaded) {
            downloadManager.deleteDownloadedFile(ep.id)
        } else {
            downloadManager.startDownload(ep)
        }
    }
}
