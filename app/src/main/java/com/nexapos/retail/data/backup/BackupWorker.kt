package com.nexapos.retail.data.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.IOException

/**
 * Daily unattended backup. No-ops if the owner hasn't picked a destination folder
 * yet. Scheduled from [com.nexapos.retail.PosApplication].
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    @Suppress("SwallowedException", "TooGenericExceptionCaught") // mapped to Result.retry/failure; no signal to lose
    override suspend fun doWork(): Result {
        val folder = BackupPrefs.folderUri(applicationContext) ?: return Result.success()
        return try {
            BackupManager.backupNow(applicationContext, Uri.parse(folder))
            Result.success()
        } catch (e: IOException) {
            Result.retry()
        } catch (e: RuntimeException) {
            // e.g. the folder permission was revoked; nothing to retry until reconfigured.
            Result.failure()
        }
    }
}
