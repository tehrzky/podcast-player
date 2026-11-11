package com.example.podcast.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.podcast.data.datastore.SettingsDataStore
import com.example.podcast.data.local.EpisodeDao
import com.example.podcast.data.local.PodcastDao
import com.example.podcast.data.repository.StatsRepository
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var episodeDao: EpisodeDao

    @Inject
    lateinit var podcastDao: PodcastDao

    @Inject
    lateinit var statsRepository: StatsRepository

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var positionSaveJob: Job? = null

    private var lastRecordedSecondTimestamp = 0L

    // Headphone disconnect receiver
    private val noisyAudioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                scope.launch {
                    val settings = settingsDataStore.settingsFlow.first()
                    if (settings.pauseOnHeadphoneDisconnect) {
                        exoPlayer.pause()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // true = handle audio focus automatically
            .setHandleAudioBecomingNoisy(true)
            .build()

        val callback = object : MediaSession.Callback {
            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    ACTION_SET_SPEED -> {
                        val speed = args.getFloat("speed", 1.0f)
                        setPlaybackSpeed(speed)
                    }
                    ACTION_SKIP_FORWARD -> {
                        val seconds = args.getInt("seconds", 30)
                        exoPlayer.seekTo(exoPlayer.currentPosition + seconds * 1000L)
                    }
                    ACTION_SKIP_BACK -> {
                        val seconds = args.getInt("seconds", 10)
                        exoPlayer.seekTo(Math.max(0L, exoPlayer.currentPosition - seconds * 1000L))
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(callback)
            .build()

        registerReceiver(noisyAudioReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

        setupPlayerListener()
        startPeriodicPositionSaving()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                val episodeId = mediaItem?.mediaId ?: return
                scope.launch(Dispatchers.IO) {
                    val ep = episodeDao.getEpisodeByIdSync(episodeId)
                    if (ep != null) {
                        // Apply speed: per-podcast override wins over global
                        val podcast = podcastDao.getPodcastByIdSync(ep.podcastId)
                        val globalSpeed = settingsDataStore.settingsFlow.first().globalSpeed
                        val speedToUse = podcast?.playbackSpeedOverride ?: globalSpeed
                        withContext(Dispatchers.Main) {
                            exoPlayer.playbackParameters = PlaybackParameters(speedToUse)
                        }

                        // Restore playback position
                        if (ep.playbackPosition > 0 && !ep.isFinished) {
                            withContext(Dispatchers.Main) {
                                exoPlayer.seekTo(ep.playbackPosition)
                            }
                        }

                        // Mark started if not already
                        if (!ep.isStarted) {
                            episodeDao.updatePlaybackPosition(ep.id, ep.playbackPosition, System.currentTimeMillis())
                            statsRepository.recordEpisodeStarted()
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    handleEpisodeFinished()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (!isPlaying) {
                    saveCurrentPosition()
                }
            }
        })
    }

    private fun startPeriodicPositionSaving() {
        positionSaveJob = scope.launch {
            while (isActive) {
                delay(10_000L) // every 10 seconds
                if (exoPlayer.isPlaying) {
                    saveCurrentPosition()
                    recordListeningStats(10L)
                }
            }
        }
    }

    private fun saveCurrentPosition() {
        val currentMediaId = exoPlayer.currentMediaItem?.mediaId ?: return
        val currentPos = exoPlayer.currentPosition
        val duration = exoPlayer.duration

        scope.launch(Dispatchers.IO) {
            episodeDao.updatePlaybackPosition(currentMediaId, currentPos, System.currentTimeMillis())

            // Check if played > 90%
            if (duration > 0 && currentPos.toFloat() / duration.toFloat() >= 0.9f) {
                episodeDao.markEpisodeFinished(currentMediaId, true)
                statsRepository.recordEpisodeFinished()
            }
        }
    }

    private fun handleEpisodeFinished() {
        val currentMediaId = exoPlayer.currentMediaItem?.mediaId ?: return
        scope.launch(Dispatchers.IO) {
            episodeDao.markEpisodeFinished(currentMediaId, true)
            statsRepository.recordEpisodeFinished()
        }
    }

    private fun recordListeningStats(seconds: Long) {
        scope.launch(Dispatchers.IO) {
            statsRepository.recordListeningSeconds(seconds)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        positionSaveJob?.cancel()
        serviceJob.cancel()
        try {
            unregisterReceiver(noisyAudioReceiver)
        } catch (e: Exception) {}
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_SET_SPEED = "com.example.podcast.SET_SPEED"
        const val ACTION_SKIP_FORWARD = "com.example.podcast.SKIP_FORWARD"
        const val ACTION_SKIP_BACK = "com.example.podcast.SKIP_BACK"
    }
}
