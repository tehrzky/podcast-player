package com.example.podcast.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "TranscriptSegment",
    indices = [
        Index(value = ["episodeId"]),
        Index(value = ["startMs"])
    ]
)
data class TranscriptSegment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val episodeId: String,
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class PlaybackConflict(
    val episodeId: String,
    val episodeTitle: String,
    val podcastTitle: String,
    val localPositionMs: Long,
    val remotePositionMs: Long,
    val localTimestamp: Long,
    val remoteTimestamp: Long
)
