package com.example.podcast.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.ListeningStat
import com.example.podcast.data.model.Podcast
import com.example.podcast.data.model.QueueItem
import com.example.podcast.data.model.TranscriptSegment

@Database(
    entities = [
        Podcast::class,
        Episode::class,
        QueueItem::class,
        ListeningStat::class,
        TranscriptSegment::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun queueDao(): QueueDao
    abstract fun listeningStatsDao(): ListeningStatsDao
    abstract fun transcriptDao(): TranscriptDao
}
