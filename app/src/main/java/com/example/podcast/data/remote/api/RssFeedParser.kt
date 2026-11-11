package com.example.podcast.data.remote.api

import android.util.Xml
import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.EpisodeType
import com.example.podcast.data.model.Podcast
import com.example.podcast.data.model.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedFeed(
    val podcast: Podcast,
    val episodes: List<Episode>
)

@Singleton
class RssFeedParser @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val dateFormats = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    )

    suspend fun fetchAndParseFeed(feedUrl: String): ParsedFeed = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(feedUrl)
            .addHeader("User-Agent", "PodcastPlayer/1.0 (Android; RSS Client)")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code} fetching $feedUrl")
            val stream = response.body?.byteStream() ?: throw IllegalStateException("Empty body for $feedUrl")
            parse(stream, feedUrl)
        }
    }

    fun parse(inputStream: InputStream, feedUrl: String): ParsedFeed {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var podcastTitle = ""
        var podcastAuthor = ""
        var podcastDescription = ""
        var podcastArtwork = ""
        val categories = mutableListOf<String>()

        val episodes = mutableListOf<Episode>()
        val podcastId = feedUrl.hashCode().toString()

        var eventType = parser.eventType
        var insideChannel = false
        var insideItem = false

        // Temporary item vars
        var itemTitle = ""
        var itemDescription = ""
        var itemAudioUrl = ""
        var itemDuration = 0L
        var itemPubDate = 0L
        var itemEpisodeNum: Int? = null
        var itemSeasonNum: Int? = null
        var itemType = EpisodeType.FULL
        var itemTranscriptUrl: String? = null
        var itemTranscriptType: String? = null
        var itemGuid = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when {
                        name.equals("channel", ignoreCase = true) -> insideChannel = true
                        name.equals("item", ignoreCase = true) -> {
                            insideItem = true
                            itemTitle = ""
                            itemDescription = ""
                            itemAudioUrl = ""
                            itemDuration = 0L
                            itemPubDate = 0L
                            itemEpisodeNum = null
                            itemSeasonNum = null
                            itemType = EpisodeType.FULL
                            itemTranscriptUrl = null
                            itemTranscriptType = null
                            itemGuid = ""
                        }
                        insideItem -> {
                            when {
                                name.equals("title", ignoreCase = true) -> itemTitle = parser.nextText().trim()
                                name.equals("description", ignoreCase = true) || name.equals("content:encoded", ignoreCase = true) -> {
                                    if (itemDescription.isBlank()) itemDescription = parser.nextText().trim()
                                }
                                name.equals("guid", ignoreCase = true) -> itemGuid = parser.nextText().trim()
                                name.equals("enclosure", ignoreCase = true) -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    if (!url.isNullOrBlank()) itemAudioUrl = url
                                }
                                name.equals("pubDate", ignoreCase = true) -> {
                                    val rawDate = parser.nextText().trim()
                                    itemPubDate = parseDate(rawDate)
                                }
                                name.equals("itunes:duration", ignoreCase = true) -> {
                                    val rawDuration = parser.nextText().trim()
                                    itemDuration = parseDuration(rawDuration)
                                }
                                name.equals("itunes:episode", ignoreCase = true) -> {
                                    itemEpisodeNum = parser.nextText().trim().toIntOrNull()
                                }
                                name.equals("itunes:season", ignoreCase = true) -> {
                                    itemSeasonNum = parser.nextText().trim().toIntOrNull()
                                }
                                name.equals("itunes:episodeType", ignoreCase = true) -> {
                                    val rawType = parser.nextText().trim()
                                    itemType = when (rawType.lowercase()) {
                                        "trailer" -> EpisodeType.TRAILER
                                        "bonus" -> EpisodeType.BONUS
                                        else -> EpisodeType.FULL
                                    }
                                }
                                name.equals("podcast:transcript", ignoreCase = true) -> {
                                    itemTranscriptUrl = parser.getAttributeValue(null, "url")
                                    itemTranscriptType = parser.getAttributeValue(null, "type")
                                }
                            }
                        }
                        insideChannel && !insideItem -> {
                            when {
                                name.equals("title", ignoreCase = true) -> podcastTitle = parser.nextText().trim()
                                name.equals("description", ignoreCase = true) -> podcastDescription = parser.nextText().trim()
                                name.equals("itunes:author", ignoreCase = true) -> podcastAuthor = parser.nextText().trim()
                                name.equals("itunes:image", ignoreCase = true) -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    if (!href.isNullOrBlank()) podcastArtwork = href
                                }
                                name.equals("itunes:category", ignoreCase = true) -> {
                                    val text = parser.getAttributeValue(null, "text")
                                    if (!text.isNullOrBlank()) categories.add(text)
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name.equals("item", ignoreCase = true) && insideItem) {
                        insideItem = false
                        if (itemAudioUrl.isNotBlank()) {
                            val epId = if (itemGuid.isNotBlank()) {
                                "$podcastId-${itemGuid.hashCode()}"
                            } else {
                                "$podcastId-${itemAudioUrl.hashCode()}"
                            }
                            episodes.add(
                                Episode(
                                    id = epId,
                                    podcastId = podcastId,
                                    title = if (itemTitle.isNotBlank()) itemTitle else "Episode ${episodes.size + 1}",
                                    description = itemDescription,
                                    audioUrl = itemAudioUrl,
                                    duration = itemDuration,
                                    publishedDate = itemPubDate,
                                    episodeNumber = itemEpisodeNum,
                                    seasonNumber = itemSeasonNum,
                                    episodeType = itemType,
                                    transcriptUrl = itemTranscriptUrl,
                                    transcriptType = itemTranscriptType
                                )
                            )
                        }
                    } else if (name.equals("channel", ignoreCase = true)) {
                        insideChannel = false
                    }
                }
            }
            eventType = parser.next()
        }

        val podcast = Podcast(
            id = podcastId,
            title = if (podcastTitle.isNotBlank()) podcastTitle else "Untitled Podcast",
            author = if (podcastAuthor.isNotBlank()) podcastAuthor else "Unknown Author",
            artworkUrl = podcastArtwork,
            feedUrl = feedUrl,
            description = podcastDescription,
            categories = categories,
            lastRefreshedAt = System.currentTimeMillis()
        )

        return ParsedFeed(podcast, episodes)
    }

    private fun parseDate(dateStr: String): Long {
        for (format in dateFormats) {
            try {
                val parsed = format.parse(dateStr)
                if (parsed != null) return parsed.time
            } catch (e: Exception) {
                // try next format
            }
        }
        return System.currentTimeMillis()
    }

    private fun parseDuration(durationStr: String): Long {
        return try {
            if (durationStr.contains(":")) {
                val parts = durationStr.split(":")
                if (parts.size == 3) {
                    val h = parts[0].toLong()
                    val m = parts[1].toLong()
                    val s = parts[2].toDouble().toLong()
                    (h * 3600 + m * 60 + s) * 1000L
                } else if (parts.size == 2) {
                    val m = parts[0].toLong()
                    val s = parts[1].toDouble().toLong()
                    (m * 60 + s) * 1000L
                } else 0L
            } else {
                durationStr.toLong() * 1000L
            }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Parse WebVTT or SRT transcript into TranscriptSegments
     */
    fun parseTranscript(content: String, episodeId: String): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()
        val lines = content.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.contains("-->")) {
                val times = line.split("-->")
                if (times.size == 2) {
                    val startMs = parseTimestampToMs(times[0].trim())
                    val endMs = parseTimestampToMs(times[1].trim().split(" ")[0])
                    val textBuilder = StringBuilder()
                    i++
                    while (i < lines.size && lines[i].trim().isNotBlank() && !lines[i].contains("-->")) {
                        textBuilder.append(lines[i].trim()).append(" ")
                        i++
                    }
                    val text = textBuilder.toString().trim()
                    if (text.isNotBlank()) {
                        segments.add(TranscriptSegment(episodeId = episodeId, startMs = startMs, endMs = endMs, text = text))
                    }
                    continue
                }
            }
            i++
        }
        return segments
    }

    private fun parseTimestampToMs(ts: String): Long {
        return try {
            val normalized = ts.replace(",", ".")
            val parts = normalized.split(":")
            if (parts.size == 3) {
                val h = parts[0].toLong()
                val m = parts[1].toLong()
                val s = parts[2].toDouble()
                ((h * 3600 + m * 60 + s) * 1000).toLong()
            } else if (parts.size == 2) {
                val m = parts[0].toLong()
                val s = parts[1].toDouble()
                ((m * 60 + s) * 1000).toLong()
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }
}
