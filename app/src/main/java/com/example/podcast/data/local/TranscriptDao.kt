package com.example.podcast.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.podcast.data.model.TranscriptSegment
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {

    @Query("SELECT * FROM TranscriptSegment WHERE episodeId = :episodeId ORDER BY startMs ASC")
    fun getTranscriptForEpisode(episodeId: String): Flow<List<TranscriptSegment>>

    @Query("SELECT * FROM TranscriptSegment WHERE episodeId = :episodeId ORDER BY startMs ASC")
    suspend fun getTranscriptSync(episodeId: String): List<TranscriptSegment>

    @Query("SELECT COUNT(*) FROM TranscriptSegment WHERE episodeId = :episodeId")
    suspend fun getSegmentCount(episodeId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<TranscriptSegment>)

    @Query("DELETE FROM TranscriptSegment WHERE episodeId = :episodeId")
    suspend fun deleteTranscriptForEpisode(episodeId: String)

    @Query("DELETE FROM TranscriptSegment")
    suspend fun clearAll()
}
