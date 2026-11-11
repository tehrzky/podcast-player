package com.example.podcast.data.model

enum class EpisodeSortOrder(val displayName: String) {
    OLDEST_FIRST("Oldest → Newest"),
    NEWEST_FIRST("Newest → Oldest"),
    EPISODE_ASC("Episode # (Ascending)"),
    EPISODE_DESC("Episode # (Descending)"),
    TITLE_ASC("Title (A–Z)"),
    SHORTEST_FIRST("Shortest First"),
    LONGEST_FIRST("Longest First"),
    UNPLAYED_FIRST("Unplayed First"),
    DOWNLOADED_FIRST("Downloaded First")
}

enum class PlaybackDirection(val displayName: String) {
    UP("Up (Oldest First / Binge)"),
    DOWN("Down (Newest First)")
}

enum class QueueSource {
    MANUAL,
    AUTO
}

enum class EpisodeType {
    FULL,
    TRAILER,
    BONUS
}

enum class DownloadStatus {
    NOT_DOWNLOADED,
    QUEUED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    OLED_BLACK
}

enum class LibraryViewMode {
    LIST,
    COMPACT,
    GRID_2,
    GRID_3,
    GRID_4
}
