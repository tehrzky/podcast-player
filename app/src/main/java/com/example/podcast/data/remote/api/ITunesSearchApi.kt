package com.example.podcast.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class ITunesSearchResponse(
    @Json(name = "resultCount") val resultCount: Int = 0,
    @Json(name = "results") val results: List<ITunesPodcastDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ITunesPodcastDto(
    @Json(name = "collectionId") val collectionId: Long = 0L,
    @Json(name = "collectionName") val collectionName: String? = null,
    @Json(name = "artistName") val artistName: String? = null,
    @Json(name = "feedUrl") val feedUrl: String? = null,
    @Json(name = "artworkUrl600") val artworkUrl600: String? = null,
    @Json(name = "artworkUrl100") val artworkUrl100: String? = null,
    @Json(name = "primaryGenreName") val primaryGenreName: String? = null,
    @Json(name = "genres") val genres: List<String>? = null
)

interface ITunesSearchApi {
    @GET("search")
    suspend fun searchPodcasts(
        @Query("term") term: String,
        @Query("media") media: String = "podcast",
        @Query("limit") limit: Int = 25
    ): ITunesSearchResponse
}
