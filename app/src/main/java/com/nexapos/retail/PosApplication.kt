package com.nexapos.retail

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nexapos.retail.data.backup.BackupManager
import com.nexapos.retail.data.backup.BackupWorker
import com.nexapos.retail.di.AppContainer
import net.sqlcipher.database.SQLiteDatabase
import java.util.concurrent.TimeUnit

class PosApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Apply any backup staged for restore before the database is opened.
        BackupManager.applyPendingRestore(this)
        // SQLCipher native libraries must be loaded before the encrypted DB is opened.
        SQLiteDatabase.loadLibs(this)
        container = AppContainer(this)
        scheduleDailyBackup()
    }

    private fun scheduleDailyBackup() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily-backup",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS).build(),
        )
    }
}
