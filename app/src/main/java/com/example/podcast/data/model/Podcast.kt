package com.example.podcast.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Podcast")
data class Podcast(
    @PrimaryKey
    val id: String, // Feed URL hash or GUID
    val title: String,
    val author: String,
    val artworkUrl: String,
    val feedUrl: String,
    val description: String,
    val categories: List<String> = emptyList(),
    val subscribed: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val lastRefreshedAt: Long = 0L,
    val defaultSortOrder: EpisodeSortOrder? = null,
    val defaultPlaybackDirection: PlaybackDirection? = null,
    val autoDownloadNew: Boolean = false,
    val autoDownloadLastN: Int = 3,
    val playbackSpeedOverride: Float? = null
)
