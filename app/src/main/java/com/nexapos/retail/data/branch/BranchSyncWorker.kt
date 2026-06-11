package com.nexapos.retail.data.branch

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nexapos.retail.PosApplication
import java.util.concurrent.TimeUnit

/**
 * Periodic background full-sync (~every 6 h, only on a network and when the
 * battery isn't low). Pushes this branch's latest summary, stock and today's
 * sales so head office / peers stay reasonably fresh even if the till is idle.
 * A no-op when the add-on is off (branchSync is then [NoopBranchSync]).
 */
class BranchSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as PosApplication).container
        return when (container.branchSync.syncNow()) {
            is SyncResult.Ok -> Result.success()
            is SyncResult.Failed -> Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "nexapos-branch-sync"
        private const val INTERVAL_HOURS = 6L

        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<BranchSyncWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(true)
                            .build(),
                    )
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
