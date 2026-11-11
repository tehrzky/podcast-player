package com.example.podcast.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.podcast.data.sync.RestoreStrategy

@Composable
fun RestoreOptionDialog(
    onConfirm: (RestoreStrategy) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Backup") },
        text = {
            Text(
                "Choose restore mode:\n\n" +
                "• Merge: Add missing podcasts and queue items while preserving your existing local data.\n\n" +
                "• Replace: Wipe existing local database and restore everything exactly as backed up."
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(RestoreStrategy.MERGE) }) {
                Text("Merge (Recommended)")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onConfirm(RestoreStrategy.REPLACE) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Replace (Wipe)")
            }
        }
    )
}
