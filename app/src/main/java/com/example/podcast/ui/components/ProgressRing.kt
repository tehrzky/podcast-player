package com.example.podcast.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ProgressRing(
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 3.dp,
    ringColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            // Draw background ring track
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokePx)
            )
            // Draw progress arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = (progress.coerceIn(0f, 1f)) * 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .size(size - strokeWidth * 2 - 4.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
