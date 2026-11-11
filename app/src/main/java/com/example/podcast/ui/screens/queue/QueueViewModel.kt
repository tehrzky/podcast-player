package com.example.podcast.ui.screens.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podcast.data.local.QueueItemWithEpisode
import com.example.podcast.data.model.QueueItem
import com.example.podcast.data.model.QueueSource
import com.example.podcast.data.repository.QueueRepository
import com.example.podcast.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val queueRepository: QueueRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    val queueItems: StateFlow<List<QueueItemWithEpisode>> = queueRepository.getQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentEpisode = playbackController.currentEpisode

    fun playEpisodeAt(episodeId: String) {
        viewModelScope.launch {
            val allIds = queueItems.value.mapNotNull { it.episode?.id }
            playbackController.submitQueueAndPlay(allIds, episodeId)
        }
    }

    fun removeItem(episodeId: String) {
        viewModelScope.launch {
            queueRepository.removeFromQueue(episodeId)
        }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val currentList = queueItems.value.map { it.queueItem }.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            viewModelScope.launch {
                queueRepository.reorderQueue(currentList)
            }
        }
    }

    fun clearAutoItems() {
        viewModelScope.launch {
            queueRepository.replaceAutoQueue(emptyList())
        }
    }
}
