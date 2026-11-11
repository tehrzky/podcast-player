package com.example.podcast.domain.usecase

import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.EpisodeSortOrder
import javax.inject.Inject

class SortEpisodesUseCase @Inject constructor() {

    fun sort(episodes: List<Episode>, sortOrder: EpisodeSortOrder): List<Episode> {
        return when (sortOrder) {
            EpisodeSortOrder.OLDEST_FIRST -> {
                episodes.sortedWith(compareBy<Episode> { it.publishedDate }.thenBy { it.id })
            }
            EpisodeSortOrder.NEWEST_FIRST -> {
                episodes.sortedWith(compareByDescending<Episode> { it.publishedDate }.thenBy { it.id })
            }
            EpisodeSortOrder.EPISODE_ASC -> {
                episodes.sortedWith(
                    compareBy<Episode> { it.episodeNumber ?: Int.MAX_VALUE }
                        .thenBy { it.publishedDate }
                )
            }
            EpisodeSortOrder.EPISODE_DESC -> {
                episodes.sortedWith(
                    compareByDescending<Episode> { it.episodeNumber ?: Int.MIN_VALUE }
                        .thenByDescending { it.publishedDate }
                )
            }
            EpisodeSortOrder.TITLE_ASC -> {
                episodes.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            }
            EpisodeSortOrder.SHORTEST_FIRST -> {
                episodes.sortedBy { it.duration }
            }
            EpisodeSortOrder.LONGEST_FIRST -> {
                episodes.sortedByDescending { it.duration }
            }
            EpisodeSortOrder.UNPLAYED_FIRST -> {
                episodes.sortedWith(
                    compareBy<Episode> { it.isFinished }
                        .thenByDescending { it.publishedDate }
                )
            }
            EpisodeSortOrder.DOWNLOADED_FIRST -> {
                episodes.sortedWith(
                    compareByDescending<Episode> { it.isDownloaded }
                        .thenByDescending { it.publishedDate }
                )
            }
        }
    }
}
