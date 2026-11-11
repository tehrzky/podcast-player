package com.example.podcast.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.podcast.data.datastore.SettingsDataStore
import com.example.podcast.data.sync.JsonBackupManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val jsonBackupManager: JsonBackupManager,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val settings = settingsDataStore.settingsFlow.first()
            if (!settings.autoBackupEnabled) return@withContext Result.success()

            val backupJson = jsonBackupManager.createFullBackupJson()
            val backupFile = File(context.filesDir, "backups/auto_backup_${System.currentTimeMillis()}.json")
            backupFile.parentFile?.mkdirs()
            backupFile.writeText(backupJson)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
