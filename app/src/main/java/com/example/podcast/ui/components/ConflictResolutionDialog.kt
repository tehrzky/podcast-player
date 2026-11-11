package com.example.podcast.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.podcast.data.model.PlaybackConflict

@Composable
fun ConflictResolutionDialog(
    conflict: PlaybackConflict,
    onResolveThisDevice: () -> Unit,
    onResolveOtherDevice: () -> Unit,
    onResolveFurthestAlong: () -> Unit,
    onDismiss: () -> Unit
) {
    val localTimeStr = formatSecondsToTime(conflict.localPositionMs / 1000)
    val remoteTimeStr = formatSecondsToTime(conflict.remotePositionMs / 1000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Position Conflict") },
        text = {
            Text(
                "Resume \"${conflict.episodeTitle}\" from $localTimeStr (this device) or $remoteTimeStr (other device)?"
            )
        },
        confirmButton = {
            Button(onClick = onResolveFurthestAlong) {
                Text("Furthest Along")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onResolveThisDevice) {
                Text("This Device ($localTimeStr)")
            }
        }
    )
}

private fun formatSecondsToTime(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d", m, s)
}
