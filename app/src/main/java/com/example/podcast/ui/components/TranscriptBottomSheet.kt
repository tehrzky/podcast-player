package com.example.podcast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.podcast.data.model.TranscriptSegment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptBottomSheet(
    sheetState: SheetState,
    segments: List<TranscriptSegment>,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        val listState = rememberLazyListState()

        // Find active segment
        val activeIndex = segments.indexOfFirst {
            currentPositionMs >= it.startMs && (it.endMs <= 0 || currentPositionMs <= it.endMs)
        }

        // Auto-scroll to active line
        LaunchedEffect(activeIndex) {
            if (activeIndex >= 0) {
                listState.animateScrollToItem(activeIndex)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Episode Transcript",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (segments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transcript available for this episode.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(segments) { index, segment ->
                        val isActive = index == activeIndex
                        val timestampStr = formatTimestamp(segment.startMs)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSeekTo(segment.startMs) },
                            color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = timestampStr,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.width(54.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = segment.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = if (isActive) 15.sp else 14.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatTimestamp(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
