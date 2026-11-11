package com.example.podcast.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Queue : Screen("queue", "Queue", Icons.Default.QueueMusic)
    object Discover : Screen("discover", "Discover", Icons.Default.Explore)
    object Library : Screen("library", "Library", Icons.Default.VideoLibrary)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    object PodcastDetail : Screen("podcast/{podcastId}", "Podcast Detail", Icons.Default.Home) {
        fun createRoute(podcastId: String) = "podcast/${Uri.encode(podcastId)}"
    }

    object Player : Screen("player", "Player", Icons.Default.Home)
}
