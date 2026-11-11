package com.example.podcast.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.http.GET
import retrofit2.http.Query
import java.security.MessageDigest

@JsonClass(generateAdapter = true)
data class PodcastIndexSearchResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "feeds") val feeds: List<PodcastIndexFeedDto> = emptyList(),
    @Json(name = "count") val count: Int = 0
)

@JsonClass(generateAdapter = true)
data class PodcastIndexFeedDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "title") val title: String = "",
    @Json(name = "url") val url: String = "",
    @Json(name = "originalUrl") val originalUrl: String? = null,
    @Json(name = "description") val description: String = "",
    @Json(name = "author") val author: String = "",
    @Json(name = "image") val image: String = "",
    @Json(name = "artwork") val artwork: String = "",
    @Json(name = "categories") val categories: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class PodcastIndexEpisodeResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "items") val items: List<PodcastIndexEpisodeDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PodcastIndexEpisodeDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "title") val title: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "enclosureUrl") val enclosureUrl: String = "",
    @Json(name = "duration") val duration: Long? = 0L,
    @Json(name = "datePublished") val datePublished: Long? = 0L,
    @Json(name = "episode") val episode: Int? = null,
    @Json(name = "season") val season: Int? = null,
    @Json(name = "transcriptUrl") val transcriptUrl: String? = null
)

interface PodcastIndexApi {
    @GET("api/1.0/search/byterm")
    suspend fun searchByTerm(
        @Query("q") query: String,
        @Query("max") max: Int = 30
    ): PodcastIndexSearchResponse

    @GET("api/1.0/podcasts/byfeedurl")
    suspend fun getByFeedUrl(
        @Query("url") url: String
    ): PodcastIndexSearchResponse

    @GET("api/1.0/episodes/byfeedid")
    suspend fun getEpisodesByFeedId(
        @Query("id") feedId: Long
    ): PodcastIndexEpisodeResponse
}

class PodcastIndexAuthInterceptor(
    private val apiKeyProvider: () -> String,
    private val apiSecretProvider: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = apiKeyProvider()
        val apiSecret = apiSecretProvider()

        if (apiKey.isBlank() || apiSecret.isBlank()) {
            return chain.proceed(chain.request())
        }

        val unixTime = (System.currentTimeMillis() / 1000).toString()
        val dataToHash = apiKey + apiSecret + unixTime
        val sha1Hash = MessageDigest.getInstance("SHA-1")
            .digest(dataToHash.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "PodcastPlayer/1.0")
            .addHeader("X-Auth-Key", apiKey)
            .addHeader("X-Auth-Date", unixTime)
            .addHeader("Authorization", sha1Hash)
            .build()

        return chain.proceed(request)
    }
}
