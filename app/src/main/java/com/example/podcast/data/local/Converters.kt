package com.example.podcast.data.local

import androidx.room.TypeConverter
import com.example.podcast.data.model.DownloadStatus
import com.example.podcast.data.model.EpisodeSortOrder
import com.example.podcast.data.model.EpisodeType
import com.example.podcast.data.model.PlaybackDirection
import com.example.podcast.data.model.QueueSource

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(";;;") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(";;;").filter { it.isNotBlank() }
    }

    @TypeConverter
    fun fromEpisodeSortOrder(value: EpisodeSortOrder?): String? = value?.name

    @TypeConverter
    fun toEpisodeSortOrder(value: String?): EpisodeSortOrder? {
        return value?.let {
            try {
                EpisodeSortOrder.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun fromPlaybackDirection(value: PlaybackDirection?): String? = value?.name

    @TypeConverter
    fun toPlaybackDirection(value: String?): PlaybackDirection? {
        return value?.let {
            try {
                PlaybackDirection.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun fromQueueSource(value: QueueSource): String = value.name

    @TypeConverter
    fun toQueueSource(value: String): QueueSource {
        return try {
            QueueSource.valueOf(value)
        } catch (e: Exception) {
            QueueSource.AUTO
        }
    }

    @TypeConverter
    fun fromEpisodeType(value: EpisodeType): String = value.name

    @TypeConverter
    fun toEpisodeType(value: String): EpisodeType {
        return try {
            EpisodeType.valueOf(value)
        } catch (e: Exception) {
            EpisodeType.FULL
        }
    }

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus {
        return try {
            DownloadStatus.valueOf(value)
        } catch (e: Exception) {
            DownloadStatus.NOT_DOWNLOADED
        }
    }
}
