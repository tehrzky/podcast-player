package com.example.podcast.di

import com.example.podcast.BuildConfig
import com.example.podcast.data.remote.api.ITunesSearchApi
import com.example.podcast.data.remote.api.PodcastIndexApi
import com.example.podcast.data.remote.api.PodcastIndexAuthInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val authInterceptor = PodcastIndexAuthInterceptor(
            apiKeyProvider = { BuildConfig.PODCASTINDEX_KEY },
            apiSecretProvider = { BuildConfig.PODCASTINDEX_SECRET }
        )

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun providePodcastIndexApi(okHttpClient: OkHttpClient, moshi: Moshi): PodcastIndexApi {
        return Retrofit.Builder()
            .baseUrl("https://api.podcastindex.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PodcastIndexApi::class.java)
    }

    @Provides
    @Singleton
    fun provideITunesSearchApi(okHttpClient: OkHttpClient, moshi: Moshi): ITunesSearchApi {
        return Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ITunesSearchApi::class.java)
    }
}
