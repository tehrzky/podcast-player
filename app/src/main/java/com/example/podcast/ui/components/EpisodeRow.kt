package com.example.podcast.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.podcast.data.downloader.DownloadProgress
import com.example.podcast.data.model.DownloadStatus
import com.example.podcast.data.model.Episode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EpisodeRow(
    episode: Episode,
    onEpisodeClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloadProgress: DownloadProgress? = null,
    isPlaying: Boolean = false
) {
    val dateStr = rememberFormattedDate(episode.publishedDate)
    val durationStr = rememberFormattedDuration(episode.duration)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEpisodeClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    episode.episodeNumber?.let { num ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "EP $num",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dateStr.isNotBlank()) {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (durationStr.isNotBlank()) {
                        Text(
                            text = durationStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (episode.isFinished) {
                        Text(
                            text = "• Finished",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Download Action
            val status = downloadProgress?.status ?: episode.downloadStatus
            when (status) {
                DownloadStatus.DOWNLOADING -> {
                    val progressFloat = (downloadProgress?.progressPercent ?: 0) / 100f
                    CircularProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                DownloadStatus.DOWNLOADED -> {
                    IconButton(onClick = onDownloadClick) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                DownloadStatus.QUEUED -> {
                    IconButton(onClick = onDownloadClick) {
                        Icon(
                            imageVector = Icons.Default.Downloading,
                            contentDescription = "Queued",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                else -> {
                    IconButton(onClick = onDownloadClick) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Play Button
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun rememberFormattedDate(timeMillis: Long): String {
    if (timeMillis <= 0L) return ""
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

@Composable
fun rememberFormattedDuration(durationMs: Long): String {
    if (durationMs <= 0L) return ""
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    val remainingSeconds = totalSeconds % 60
    return if (hours > 0) {
        "${hours}h ${remainingMinutes}m"
    } else {
        "${remainingMinutes}m ${remainingSeconds}s"
    }
}
