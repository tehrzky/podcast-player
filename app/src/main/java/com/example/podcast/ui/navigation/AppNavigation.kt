package com.example.podcast.ui.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.podcast.playback.PlaybackController
import com.example.podcast.ui.components.MiniPlayer
import com.example.podcast.ui.screens.discover.DiscoverScreen
import com.example.podcast.ui.screens.discover.DiscoverViewModel
import com.example.podcast.ui.screens.home.HomeScreen
import com.example.podcast.ui.screens.home.HomeViewModel
import com.example.podcast.ui.screens.library.LibraryScreen
import com.example.podcast.ui.screens.library.LibraryViewModel
import com.example.podcast.ui.screens.player.PlayerScreen
import com.example.podcast.ui.screens.player.PlayerViewModel
import com.example.podcast.ui.screens.podcast.PodcastDetailScreen
import com.example.podcast.ui.screens.podcast.PodcastDetailViewModel
import com.example.podcast.ui.screens.queue.QueueScreen
import com.example.podcast.ui.screens.queue.QueueViewModel
import com.example.podcast.ui.screens.settings.SettingsScreen
import com.example.podcast.ui.screens.settings.SettingsViewModel

@Composable
fun AppNavigation(
    playbackController: PlaybackController,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val currentEpisode by playbackController.currentEpisode.collectAsState()
    val currentPodcast by playbackController.currentPodcast.collectAsState()
    val isPlaying by playbackController.isPlaying.collectAsState()
    val currentPosition by playbackController.currentPosition.collectAsState()
    val duration by playbackController.duration.collectAsState()

    val bottomNavScreens = listOf(
        Screen.Home,
        Screen.Queue,
        Screen.Discover,
        Screen.Library,
        Screen.Settings
    )

    val showBottomBar = currentDestination != Screen.Player.route &&
            currentDestination?.startsWith("podcast/") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    val viewModel: HomeViewModel = hiltViewModel()
                    HomeScreen(
                        viewModel = viewModel,
                        onPodcastClick = { id -> navController.navigate(Screen.PodcastDetail.createRoute(id)) },
                        onEpisodeClick = { _ -> navController.navigate(Screen.Player.route) }
                    )
                }

                composable(Screen.Queue.route) {
                    val viewModel: QueueViewModel = hiltViewModel()
                    QueueScreen(
                        viewModel = viewModel,
                        onEpisodeClick = { _ -> navController.navigate(Screen.Player.route) }
                    )
                }

                composable(Screen.Discover.route) {
                    val viewModel: DiscoverViewModel = hiltViewModel()
                    DiscoverScreen(
                        viewModel = viewModel,
                        onPodcastClick = { id -> navController.navigate(Screen.PodcastDetail.createRoute(id)) }
                    )
                }

                composable(Screen.Library.route) {
                    val viewModel: LibraryViewModel = hiltViewModel()
                    LibraryScreen(
                        viewModel = viewModel,
                        onPodcastClick = { id -> navController.navigate(Screen.PodcastDetail.createRoute(id)) },
                        onEpisodeClick = { _ -> navController.navigate(Screen.Player.route) }
                    )
                }

                composable(Screen.Settings.route) {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(viewModel = viewModel)
                }

                // *** FIXED: capture backStackEntry, decode argument, pass to ViewModel ***
                composable(
                    route = Screen.PodcastDetail.route,
                    arguments = listOf(navArgument("podcastId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val rawId = backStackEntry.arguments?.getString("podcastId").orEmpty()
                    val podcastId = Uri.decode(rawId)
                    Log.d("PodcastNav", "PodcastDetail received id: $podcastId")

                    val viewModel: PodcastDetailViewModel = hiltViewModel()
                    PodcastDetailScreen(
                        viewModel = viewModel,
                        podcastId = podcastId,
                        onBackClick = { navController.popBackStack() },
                        onEpisodeClick = { _ -> navController.navigate(Screen.Player.route) }
                    )
                }

                composable(Screen.Player.route) {
                    val viewModel: PlayerViewModel = hiltViewModel()
                    PlayerScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            AnimatedVisibility(
                visible = currentEpisode != null && currentDestination != Screen.Player.route,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                MiniPlayer(
                    episode = currentEpisode,
                    podcast = currentPodcast,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onPlayPauseClick = { playbackController.playPause() },
                    onSkipForwardClick = { playbackController.skipForward() },
                    onClick = { navController.navigate(Screen.Player.route) }
                )
            }
        }
    }
}
