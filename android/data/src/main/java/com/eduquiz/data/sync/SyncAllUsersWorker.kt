package com.eduquiz.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eduquiz.domain.sync.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker que sincroniza TODOS los usuarios de la app a Firestore.
 * 
 * Útil para:
 * - Migrar usuarios existentes a Firestore
 * - Forzar una actualización masiva de todos los perfiles
 * - Sincronizar usuarios que no se han sincronizado antes
 */
@HiltWorker
class SyncAllUsersWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncAllUsersWorker"
        const val WORK_NAME = "com.eduquiz.data.sync.SyncAllUsersWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting sync of all users to Firestore")
            
            val result = syncRepository.syncAllUsers()
            
            Log.d(
                TAG,
                "Sync completed: ${result.syncedUsers} synced, ${result.failedUsers} failed, " +
                        "${result.skippedUsers} skipped out of ${result.totalUsers} total users"
            )
            
            if (result.syncedUsers > 0 || result.failedUsers == 0) {
                Result.success()
            } else {
                // Si todos fallaron, retry
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in sync all users work", e)
            Result.retry()
        }
    }
}

