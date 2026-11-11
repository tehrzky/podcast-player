package com.example.podcast.di

import android.content.Context
import androidx.room.Room
import com.example.podcast.data.local.AppDatabase
import com.example.podcast.data.local.EpisodeDao
import com.example.podcast.data.local.ListeningStatsDao
import com.example.podcast.data.local.PodcastDao
import com.example.podcast.data.local.QueueDao
import com.example.podcast.data.local.TranscriptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "podcast_player.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun providePodcastDao(database: AppDatabase): PodcastDao = database.podcastDao()

    @Provides
    fun provideEpisodeDao(database: AppDatabase): EpisodeDao = database.episodeDao()

    @Provides
    fun provideQueueDao(database: AppDatabase): QueueDao = database.queueDao()

    @Provides
    fun provideListeningStatsDao(database: AppDatabase): ListeningStatsDao = database.listeningStatsDao()

    @Provides
    fun provideTranscriptDao(database: AppDatabase): TranscriptDao = database.transcriptDao()
}
