package com.eduquiz.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eduquiz.data.db.AppDatabase
import com.eduquiz.data.remote.FirestoreSyncService
import com.eduquiz.domain.profile.SyncState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker que sincroniza datos pendientes con Firestore.
 * 
 * Requisitos:
 * - Network connected constraint
 * - Sincroniza examAttempts PENDING
 * - Sincroniza perfil PENDING
 * - Marca SYNCED si ok, FAILED si no
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val database: AppDatabase,
    private val syncService: FirestoreSyncService
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "com.eduquiz.data.sync.SyncWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting sync work")

            val examDao = database.examDao()
            val profileDao = database.profileDao()

            // 1. Sincronizar intentos PENDING
            val pendingAttempts = examDao.getAttemptsBySyncState(SyncState.PENDING)
            Log.d(TAG, "Found ${pendingAttempts.size} pending exam attempts")

            var attemptsSynced = 0
            var attemptsFailed = 0

            for (attempt in pendingAttempts) {
                val answers = examDao.getAnswers(attempt.attemptId)
                val success = syncService.syncExamAttempt(attempt, answers)

                if (success) {
                    examDao.updateSyncState(attempt.attemptId, SyncState.SYNCED)
                    attemptsSynced++
                    Log.d(TAG, "Synced exam attempt ${attempt.attemptId}")
                } else {
                    examDao.updateSyncState(attempt.attemptId, SyncState.FAILED)
                    attemptsFailed++
                    Log.w(TAG, "Failed to sync exam attempt ${attempt.attemptId}")
                }
            }

            // 2. Sincronizar perfil PENDING
            val pendingProfiles = profileDao.getProfilesBySyncState(SyncState.PENDING)
            Log.d(TAG, "Found ${pendingProfiles.size} pending profiles")

            var profilesSynced = 0
            var profilesFailed = 0

            for (profile in pendingProfiles) {
                val success = syncService.syncUserProfile(profile)

                if (success) {
                    profileDao.updateProfileSyncState(profile.uid, SyncState.SYNCED)
                    profilesSynced++
                    Log.d(TAG, "Synced user profile ${profile.uid}")
                } else {
                    profileDao.updateProfileSyncState(profile.uid, SyncState.FAILED)
                    profilesFailed++
                    Log.w(TAG, "Failed to sync user profile ${profile.uid}")
                }
            }

            Log.d(
                TAG,
                "Sync completed: $attemptsSynced attempts synced, $attemptsFailed failed; " +
                        "$profilesSynced profiles synced, $profilesFailed failed"
            )

            // Retornar success si al menos algo se sincronizó, o si no había nada pendiente
            if (attemptsSynced > 0 || profilesSynced > 0 || (pendingAttempts.isEmpty() && pendingProfiles.isEmpty())) {
                Result.success()
            } else {
                // Si había pendientes pero todos fallaron, retry
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in sync work", e)
            Result.retry()
        }
    }

}

