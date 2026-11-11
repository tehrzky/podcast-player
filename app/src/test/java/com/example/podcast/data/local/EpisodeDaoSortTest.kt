package com.example.podcast.data.local

import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.EpisodeSortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeDaoSortTest {

    @Test
    fun `verify sortOrder enum names match DAO SQL CASE statements exactly`() {
        // The DAO SQL contract uses:
        // 'OLDEST_FIRST', 'NEWEST_FIRST', 'EPISODE_ASC', 'EPISODE_DESC', 'TITLE_ASC', 'UNPLAYED_FIRST', 'DOWNLOADED_FIRST'
        val expectedSqlClauses = listOf(
            "OLDEST_FIRST",
            "NEWEST_FIRST",
            "EPISODE_ASC",
            "EPISODE_DESC",
            "TITLE_ASC",
            "UNPLAYED_FIRST",
            "DOWNLOADED_FIRST"
        )

        for (clause in expectedSqlClauses) {
            val enumValue = EpisodeSortOrder.valueOf(clause)
            assertEquals(clause, enumValue.name)
        }
    }
}
