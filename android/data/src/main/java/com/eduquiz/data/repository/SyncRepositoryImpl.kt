package com.eduquiz.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.eduquiz.data.sync.SyncWorker
import com.eduquiz.domain.sync.SyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SyncRepository {

    companion object {
        private const val PERIODIC_SYNC_TAG = "periodic_sync"
        private const val PERIODIC_SYNC_INTERVAL_HOURS = 4L
    }

    override suspend fun enqueueSyncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag(SyncWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "immediate_sync",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }

    override fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            PERIODIC_SYNC_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_SYNC_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_SYNC_TAG,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
    }
}
