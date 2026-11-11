package com.example.podcast.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ListeningStat")
data class ListeningStat(
    @PrimaryKey
    val date: String, // YYYY-MM-DD
    val secondsListened: Long = 0L,
    val episodesStarted: Int = 0,
    val episodesFinished: Int = 0
)
