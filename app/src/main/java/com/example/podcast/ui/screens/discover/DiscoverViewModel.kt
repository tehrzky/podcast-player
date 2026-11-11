package com.example.podcast.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podcast.data.model.Podcast
import com.example.podcast.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Podcast>>(emptyList())
    val searchResults: StateFlow<List<Podcast>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(400L) // debounce
            _isSearching.value = true
            _errorMessage.value = null
            try {
                val results = podcastRepository.searchPodcasts(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _errorMessage.value = "Failed to search podcasts. Check network connection."
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addByRssUrl(url: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val podcast = podcastRepository.addAndSubscribeFeed(url)
                onSuccess(podcast.id)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load RSS feed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }
}
