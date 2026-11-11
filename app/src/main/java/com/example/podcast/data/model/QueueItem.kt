package com.example.podcast.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "QueueItem",
    foreignKeys = [
        ForeignKey(
            entity = Episode::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["episodeId"]),
        Index(value = ["position"])
    ]
)
data class QueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val episodeId: String,
    val position: Int,
    val source: QueueSource = QueueSource.AUTO,
    val addedAt: Long = System.currentTimeMillis()
)
