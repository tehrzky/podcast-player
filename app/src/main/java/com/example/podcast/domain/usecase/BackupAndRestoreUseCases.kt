package com.example.podcast.domain.usecase

import com.example.podcast.data.sync.JsonBackupManager
import com.example.podcast.data.sync.RestoreStrategy
import javax.inject.Inject

class BackupNowUseCase @Inject constructor(
    private val backupManager: JsonBackupManager
) {
    suspend fun exportJson(): String {
        return backupManager.createFullBackupJson()
    }
}

class RestoreNowUseCase @Inject constructor(
    private val backupManager: JsonBackupManager
) {
    suspend fun restore(json: String, strategy: RestoreStrategy) {
        backupManager.restoreFromJson(json, strategy)
    }
}
