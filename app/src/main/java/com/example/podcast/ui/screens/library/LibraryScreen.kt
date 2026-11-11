package com.example.podcast.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.podcast.data.model.Episode
import com.example.podcast.data.model.LibraryViewMode
import com.example.podcast.data.model.Podcast
import com.example.podcast.ui.components.EpisodeRow
import com.example.podcast.ui.components.PodcastCard
import com.example.podcast.ui.components.SortChip
import com.example.podcast.ui.components.rememberFormattedDate
import com.example.podcast.ui.components.rememberFormattedDuration

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onPodcastClick: (String) -> Unit,
    onEpisodeClick: (String) -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val subscribedCount by viewModel.subscribedCount.collectAsState()
    val podcasts by viewModel.podcasts.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val downloads by viewModel.downloadedEpisodes.collectAsState()
    val currentSort by viewModel.selectedEpisodeSort.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val downloadProgressMap by viewModel.downloadProgressMap.collectAsState()

    val currentViewMode = settings?.libraryViewMode ?: LibraryViewMode.GRID_2

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Podcasts", "Episodes", "Downloads")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp)
    ) {
        // Stats Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Listening Stats",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(label = "All-Time", value = "${String.format("%.1f", stats?.totalHoursAllTime ?: 0f)} hrs")
                    StatItem(label = "This Week", value = "${String.format("%.1f", stats?.totalHoursThisWeek ?: 0f)} hrs")
                    StatItem(label = "Started", value = "${stats?.totalStarted ?: 0}")
                    StatItem(label = "Finished", value = "${stats?.totalFinished ?: 0}")
                }
            }
        }

        // Tabs
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = if (index == 0) "$title ($subscribedCount)" else if (index == 2) "$title (${downloads.size})" else title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                )
            }
        }

        // Subheader Controls (Sort Chip + View Mode Toggle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedTabIndex == 1) {
                SortChip(
                    currentSort = currentSort,
                    onSortSelected = { viewModel.setEpisodeSort(it) }
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (selectedTabIndex == 0) {
                Row {
                    IconButton(onClick = { viewModel.setViewMode(LibraryViewMode.GRID_2) }) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "2 Columns",
                            tint = if (currentViewMode == LibraryViewMode.GRID_2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.setViewMode(LibraryViewMode.LIST) }) {
                        Icon(
                            imageVector = Icons.Default.ViewList,
                            contentDescription = "List",
                            tint = if (currentViewMode == LibraryViewMode.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        when (selectedTabIndex) {
            0 -> PodcastsTabContent(
                podcasts = podcasts,
                viewMode = currentViewMode,
                onPodcastClick = onPodcastClick
            )
            1 -> EpisodesTabContent(
                episodes = episodes,
                downloadProgressMap = downloadProgressMap,
                onEpisodeClick = onEpisodeClick,
                onPlay = { ep -> viewModel.playEpisode(ep, episodes) },
                onToggleDownload = { ep -> viewModel.toggleDownload(ep) }
            )
            2 -> DownloadsTabContent(
                downloads = downloads,
                onPlay = { ep -> viewModel.playEpisode(ep, downloads) },
                onDelete = { ep -> viewModel.deleteDownload(ep.id) },
                onEpisodeClick = onEpisodeClick
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun PodcastsTabContent(
    podcasts: List<Podcast>,
    viewMode: LibraryViewMode,
    onPodcastClick: (String) -> Unit
) {
    if (podcasts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No subscribed podcasts yet.\nExplore in Discover to build your library!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    val columns = when (viewMode) {
        LibraryViewMode.GRID_3 -> 3
        LibraryViewMode.GRID_4 -> 4
        LibraryViewMode.GRID_2 -> 2
        else -> 1
    }

    if (columns > 1) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(podcasts) { podcast ->
                PodcastCard(
                    podcast = podcast,
                    onClick = { onPodcastClick(podcast.id) },
                    onPlayClick = { onPodcastClick(podcast.id) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(podcasts) { podcast ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPodcastClick(podcast.id) },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = podcast.artworkUrl,
                            contentDescription = podcast.title,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = podcast.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = podcast.author,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodesTabContent(
    episodes: List<Episode>,
    downloadProgressMap: Map<String, com.example.podcast.data.downloader.DownloadProgress>,
    onEpisodeClick: (String) -> Unit,
    onPlay: (Episode) -> Unit,
    onToggleDownload: (Episode) -> Unit
) {
    if (episodes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No episodes match your selection.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(episodes) { episode ->
            EpisodeRow(
                episode = episode,
                downloadProgress = downloadProgressMap[episode.id],
                onEpisodeClick = { onEpisodeClick(episode.id) },
                onPlayClick = { onPlay(episode) },
                onDownloadClick = { onToggleDownload(episode) }
            )
        }
    }
}

@Composable
private fun DownloadsTabContent(
    downloads: List<Episode>,
    onPlay: (Episode) -> Unit,
    onDelete: (Episode) -> Unit,
    onEpisodeClick: (String) -> Unit
) {
    if (downloads.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No downloaded episodes.\nTap download on any episode for offline binging!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(downloads) { episode ->
            val durationStr = rememberFormattedDuration(episode.duration)
            val lastAccessStr = rememberFormattedDate(episode.downloadLastAccessedAt ?: 0L)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPlay(episode) },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = episode.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Text(
                                text = durationStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (lastAccessStr.isNotBlank()) {
                                Text(
                                    text = " • Last accessed $lastAccessStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(onClick = { onDelete(episode) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Download",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
