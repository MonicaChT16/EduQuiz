package com.eduquiz.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class SyncScheduler(private val context: Context) {
    fun schedule(tag: String) {
        val request = PeriodicWorkRequestBuilder<PlaceholderWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

class PlaceholderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = Result.success()
}
