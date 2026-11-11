package com.example.podcast.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.podcast.data.model.Episode
import com.example.podcast.ui.components.EpisodeRow
import com.example.podcast.ui.components.PodcastCard
import com.example.podcast.ui.components.ProgressRing

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPodcastClick: (String) -> Unit,
    onEpisodeClick: (String) -> Unit
) {
    val continueListening by viewModel.continueListening.collectAsState()
    val recentEpisodes by viewModel.recentEpisodes.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val subscribedPodcasts by viewModel.subscribedPodcasts.collectAsState()
    val downloadProgressMap by viewModel.downloadProgressMap.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // App Header
        item {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)) {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Pick up right where you left off",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Continue Listening Row
        if (continueListening.isNotEmpty()) {
            item {
                Text(
                    text = "Continue Listening",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(continueListening) { episode ->
                        val progress = if (episode.duration > 0) {
                            (episode.playbackPosition.toFloat() / episode.duration.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        val podcast = subscribedPodcasts.firstOrNull { it.id == episode.podcastId }

                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { viewModel.playEpisode(episode, continueListening) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ProgressRing(
                                progress = progress,
                                size = 88.dp,
                                strokeWidth = 4.dp
                            ) {
                                AsyncImage(
                                    model = podcast?.artworkUrl ?: "",
                                    contentDescription = episode.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Text(
                                text = episode.title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Subscribed Podcasts Horizontal Shelf
        if (subscribedPodcasts.isNotEmpty()) {
            item {
                Text(
                    text = "Your Subscriptions",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(subscribedPodcasts) { podcast ->
                        PodcastCard(
                            podcast = podcast,
                            onClick = { onPodcastClick(podcast.id) },
                            onPlayClick = { onPodcastClick(podcast.id) },
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // New Episodes
        item {
            Text(
                text = "New Episodes",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        if (recentEpisodes.isEmpty()) {
            item {
                Text(
                    text = "Subscribe to podcasts in Discover to see new episodes here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        } else {
            items(recentEpisodes) { episode ->
                EpisodeRow(
                    episode = episode,
                    downloadProgress = downloadProgressMap[episode.id],
                    onEpisodeClick = { onEpisodeClick(episode.id) },
                    onPlayClick = { viewModel.playEpisode(episode, recentEpisodes) },
                    onDownloadClick = { viewModel.toggleDownload(episode) }
                )
            }
        }
    }
}
