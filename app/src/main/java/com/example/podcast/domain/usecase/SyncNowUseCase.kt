package com.example.podcast.domain.usecase

import com.example.podcast.data.sync.GoogleDriveSyncManager
import javax.inject.Inject

class SyncNowUseCase @Inject constructor(
    private val syncManager: GoogleDriveSyncManager
) {
    suspend operator fun invoke() {
        val payload = syncManager.createLocalSyncPayload()
        // Local payload created, would be uploaded to Drive AppDataFolder via Drive API
    }
}
