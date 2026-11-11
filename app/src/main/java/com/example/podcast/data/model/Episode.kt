package com.example.podcast.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Episode",
    foreignKeys = [
        ForeignKey(
            entity = Podcast::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["podcastId"]),
        Index(value = ["publishedDate"]),
        Index(value = ["isDownloaded"]),
        Index(value = ["isFinished"])
    ]
)
data class Episode(
    @PrimaryKey
    val id: String,
    val podcastId: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val duration: Long = 0L, // in seconds or milliseconds
    val publishedDate: Long = 0L, // epoch millis
    val episodeNumber: Int? = null,
    val seasonNumber: Int? = null,
    val episodeType: EpisodeType = EpisodeType.FULL,
    val transcriptUrl: String? = null,
    val transcriptType: String? = null,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val downloadLastAccessedAt: Long? = null,
    val playbackPosition: Long = 0L, // in milliseconds
    val isFinished: Boolean = false,
    val isStarted: Boolean = false,
    val lastPlayedAt: Long = 0L
)
