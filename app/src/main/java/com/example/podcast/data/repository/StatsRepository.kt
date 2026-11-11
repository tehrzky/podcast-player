package com.example.podcast.data.repository

import com.example.podcast.data.local.ListeningStatsDao
import com.example.podcast.data.model.ListeningStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class AggregatedStats(
    val totalHoursAllTime: Float,
    val totalHoursThisWeek: Float,
    val totalHoursThisMonth: Float,
    val totalStarted: Int,
    val totalFinished: Int
)

@Singleton
class StatsRepository @Inject constructor(
    private val statsDao: ListeningStatsDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun getTodayDate(): String = dateFormat.format(System.currentTimeMillis())

    private fun getDateDaysAgo(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return dateFormat.format(cal.time)
    }

    private fun getMonthStartDate(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return dateFormat.format(cal.time)
    }

    fun getAggregatedStats(): Flow<AggregatedStats> {
        val allTimeFlow = statsDao.getTotalSecondsAllTime()
        val weekFlow = statsDao.getSecondsSince(getDateDaysAgo(7))
        val monthFlow = statsDao.getSecondsSince(getMonthStartDate())
        val startedFlow = statsDao.getTotalEpisodesStarted()
        val finishedFlow = statsDao.getTotalEpisodesFinished()

        return combine(allTimeFlow, weekFlow, monthFlow, startedFlow, finishedFlow) { allTime, week, month, started, finished ->
            AggregatedStats(
                totalHoursAllTime = allTime / 3600f,
                totalHoursThisWeek = week / 3600f,
                totalHoursThisMonth = month / 3600f,
                totalStarted = started,
                totalFinished = finished
            )
        }
    }

    suspend fun recordListeningSeconds(seconds: Long) = withContext(Dispatchers.IO) {
        if (seconds > 0) {
            statsDao.addListeningTime(getTodayDate(), seconds)
        }
    }

    suspend fun recordEpisodeStarted() = withContext(Dispatchers.IO) {
        statsDao.recordEpisodeStarted(getTodayDate())
    }

    suspend fun recordEpisodeFinished() = withContext(Dispatchers.IO) {
        statsDao.recordEpisodeFinished(getTodayDate())
    }

    suspend fun resetStats() = withContext(Dispatchers.IO) {
        statsDao.clearAll()
    }
}
