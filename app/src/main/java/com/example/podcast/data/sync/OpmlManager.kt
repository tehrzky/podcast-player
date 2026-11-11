package com.example.podcast.data.sync

import android.util.Xml
import com.example.podcast.data.local.PodcastDao
import com.example.podcast.data.model.Podcast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

data class OpmlOutline(
    val title: String,
    val xmlUrl: String,
    val htmlUrl: String? = null
)

@Singleton
class OpmlManager @Inject constructor(
    private val podcastDao: PodcastDao
) {
    suspend fun exportOpml(): String = withContext(Dispatchers.IO) {
        val podcasts = podcastDao.getSubscribedPodcastsSync()
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<opml version=\"2.0\">\n")
        sb.append("  <head>\n")
        sb.append("    <title>Podcast Player Subscriptions</title>\n")
        sb.append("    <dateCreated>${System.currentTimeMillis()}</dateCreated>\n")
        sb.append("  </head>\n")
        sb.append("  <body>\n")
        sb.append("    <outline text=\"feeds\" title=\"Feeds\">\n")
        for (p in podcasts) {
            val safeTitle = escapeXml(p.title)
            val safeUrl = escapeXml(p.feedUrl)
            sb.append("      <outline type=\"rss\" text=\"$safeTitle\" title=\"$safeTitle\" xmlUrl=\"$safeUrl\" />\n")
        }
        sb.append("    </outline>\n")
        sb.append("  </body>\n")
        sb.append("</opml>")
        sb.toString()
    }

    suspend fun parseOpml(inputStream: InputStream): List<OpmlOutline> = withContext(Dispatchers.IO) {
        val outlines = mutableListOf<OpmlOutline>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals("outline", ignoreCase = true)) {
                val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                if (!xmlUrl.isNullOrBlank()) {
                    val title = parser.getAttributeValue(null, "title")
                        ?: parser.getAttributeValue(null, "text")
                        ?: xmlUrl
                    val htmlUrl = parser.getAttributeValue(null, "htmlUrl")
                    outlines.add(OpmlOutline(title = title, xmlUrl = xmlUrl, htmlUrl = htmlUrl))
                }
            }
            eventType = parser.next()
        }
        outlines
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
