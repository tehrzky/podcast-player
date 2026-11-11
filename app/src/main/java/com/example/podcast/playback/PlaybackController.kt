package com.example.podcast.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.podcast.data.datastore.SettingsDataStore
import com.example.podcast.data.local.EpisodeDao
import com.example.podcast.data.local.PodcastDao
import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.Podcast
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
    private val settingsDataStore: SettingsDataStore,
    private val queueManager: QueueManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentEpisode = MutableStateFlow<Episode?>(null)
    val currentEpisode: StateFlow<Episode?> = _currentEpisode.asStateFlow()

    private val _currentPodcast = MutableStateFlow<Podcast?>(null)
    val currentPodcast: StateFlow<Podcast?> = _currentPodcast.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepTimerRemainingSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Int?> = _sleepTimerRemainingSeconds.asStateFlow()
    private var sleepTimerJob: Job? = null

    init {
        initializeController()
        startTrackingPlayback()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            setupControllerListener()
        }, MoreExecutors.directExecutor())
    }

    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentMedia(mediaItem)
            }

            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                _playbackSpeed.value = playbackParameters.speed
            }
        })
        updateCurrentMedia(mediaController?.currentMediaItem)
    }

    private fun updateCurrentMedia(mediaItem: MediaItem?) {
        val episodeId = mediaItem?.mediaId
        if (episodeId == null) {
            _currentEpisode.value = null
            _currentPodcast.value = null
            return
        }
        scope.launch(Dispatchers.IO) {
            val ep = episodeDao.getEpisodeByIdSync(episodeId)
            _currentEpisode.value = ep
            if (ep != null) {
                val pod = podcastDao.getPodcastByIdSync(ep.podcastId)
                _currentPodcast.value = pod
            }
        }
    }

    private fun startTrackingPlayback() {
        scope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    _isPlaying.value = controller.isPlaying
                    _currentPosition.value = controller.currentPosition
                    _duration.value = if (controller.duration > 0) controller.duration else 0L
                }
                delay(500L)
            }
        }
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun playPause() {
        if (_isPlaying.value) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun skipForward() {
        scope.launch {
            val forwardSec = settingsDataStore.settingsFlow.first().skipForwardSeconds
            val newPos = (_currentPosition.value + forwardSec * 1000L).coerceAtMost(_duration.value)
            seekTo(newPos)
        }
    }

    fun skipBack() {
        scope.launch {
            val backSec = settingsDataStore.settingsFlow.first().skipBackSeconds
            val newPos = (_currentPosition.value - backSec * 1000L).coerceAtLeast(0L)
            seekTo(newPos)
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        val bundle = Bundle().apply { putFloat("speed", speed) }
        val command = SessionCommand(PlaybackService.ACTION_SET_SPEED, bundle)
        mediaController?.sendCustomCommand(command, bundle)
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _sleepTimerRemainingSeconds.value = null
            return
        }

        _sleepTimerRemainingSeconds.value = minutes * 60
        sleepTimerJob = scope.launch {
            while ((_sleepTimerRemainingSeconds.value ?: 0) > 0) {
                delay(1000L)
                val current = _sleepTimerRemainingSeconds.value ?: 0
                _sleepTimerRemainingSeconds.value = current - 1
            }
            pause()
            _sleepTimerRemainingSeconds.value = null
        }
    }

    suspend fun submitQueueAndPlay(episodeIds: List<String>, startEpisodeId: String) {
        val mediaItems = queueManager.getMediaItemsFromQueue()
        val startIndex = mediaItems.indexOfFirst { it.mediaId == startEpisodeId }.coerceAtLeast(0)

        mediaController?.let { controller ->
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()
        }
    }

    fun playNextMediaItem() {
        if (mediaController?.hasNextMediaItem() == true) {
            mediaController?.seekToNextMediaItem()
        }
    }
}
