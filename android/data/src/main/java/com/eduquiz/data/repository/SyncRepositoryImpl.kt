package com.eduquiz.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.eduquiz.data.db.AppDatabase
import com.eduquiz.data.remote.FirestoreSyncService
import com.eduquiz.data.sync.PackUpdateWorker
import com.eduquiz.data.sync.SyncAllUsersWorker
import com.eduquiz.data.sync.SyncWorker
import com.eduquiz.domain.profile.SyncState
import com.eduquiz.domain.sync.SyncAllUsersResult
import com.eduquiz.domain.sync.SyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val syncService: FirestoreSyncService
) : SyncRepository {

    companion object {
        private const val PERIODIC_SYNC_TAG = "periodic_sync"
        private const val PERIODIC_SYNC_INTERVAL_HOURS = 4L
        private const val PERIODIC_PACK_UPDATE_TAG = "periodic_pack_update"
        private const val PERIODIC_PACK_UPDATE_INTERVAL_HOURS = 6L // Verificar cada 6 horas
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

    /**
     * Programa la verificación y actualización automática de packs.
     * Se ejecuta periódicamente cuando hay conexión a internet.
     */
    override fun schedulePackUpdate() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<PackUpdateWorker>(
            PERIODIC_PACK_UPDATE_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_PACK_UPDATE_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_PACK_UPDATE_TAG,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
    }

    /**
     * Ejecuta una verificación inmediata de packs disponibles.
     * Útil cuando la app se inicia o cuando se detecta conexión a internet.
     */
    override fun checkPackUpdateNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val packUpdateRequest = OneTimeWorkRequestBuilder<PackUpdateWorker>()
            .setConstraints(constraints)
            .addTag(PackUpdateWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "immediate_pack_update",
                ExistingWorkPolicy.REPLACE,
                packUpdateRequest
            )
    }
    
    override suspend fun syncAllUsers(): SyncAllUsersResult {
        return try {
            Log.d("SyncRepository", "Starting sync of all users to Firestore")
            
            val profileDao = database.profileDao()
            val allProfiles = profileDao.getAllProfiles()
            
            Log.d("SyncRepository", "Found ${allProfiles.size} users to sync")
            
            var syncedCount = 0
            var failedCount = 0
            var skippedCount = 0
            
            for (profile in allProfiles) {
                try {
                    // Marcar como PENDING para forzar sincronización
                    profileDao.updateProfileSyncState(profile.uid, SyncState.PENDING)
                    
                    // Intentar sincronizar
                    val success = syncService.syncUserProfile(profile)
                    
                    if (success) {
                        profileDao.updateProfileSyncState(profile.uid, SyncState.SYNCED)
                        syncedCount++
                        Log.d("SyncRepository", "✅ Synced user: ${profile.uid} (${profile.displayName})")
                    } else {
                        profileDao.updateProfileSyncState(profile.uid, SyncState.FAILED)
                        failedCount++
                        Log.w("SyncRepository", "❌ Failed to sync user: ${profile.uid}")
                    }
                } catch (e: Exception) {
                    profileDao.updateProfileSyncState(profile.uid, SyncState.FAILED)
                    failedCount++
                    Log.e("SyncRepository", "Error syncing user ${profile.uid}", e)
                }
                
                // Pequeña pausa para no sobrecargar Firestore
                kotlinx.coroutines.delay(100)
            }
            
            val result = SyncAllUsersResult(
                totalUsers = allProfiles.size,
                syncedUsers = syncedCount,
                failedUsers = failedCount,
                skippedUsers = skippedCount
            )
            
            Log.d(
                "SyncRepository",
                "Sync completed: ${result.syncedUsers} synced, ${result.failedUsers} failed out of ${result.totalUsers} total users"
            )
            
            result
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error in syncAllUsers", e)
            SyncAllUsersResult(0, 0, 0, 0)
        }
    }
    
    override fun enqueueSyncAllUsers() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncAllRequest = OneTimeWorkRequestBuilder<SyncAllUsersWorker>()
            .setConstraints(constraints)
            .addTag(SyncAllUsersWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "sync_all_users",
                ExistingWorkPolicy.REPLACE,
                syncAllRequest
            )
        
        Log.d("SyncRepository", "Enqueued sync all users work")
    }
    
    override suspend fun syncUserProfileNow(uid: String): Boolean {
        return try {
            Log.d("SyncRepository", "🔄 Syncing user profile immediately: $uid")
            
            val profileDao = database.profileDao()
            
            // Obtener el perfil actual usando getAllProfiles para evitar problemas con Flow
            val allProfiles = profileDao.getAllProfiles()
            val profileEntity = allProfiles.find { it.uid == uid } ?: run {
                Log.w("SyncRepository", "❌ Profile not found for $uid")
                return false
            }
            
            Log.d("SyncRepository", "📊 Current profile state - syncState: ${profileEntity.syncState}, xp: ${profileEntity.xp}, updatedAtLocal: ${profileEntity.updatedAtLocal}")
            
            // Actualizar updatedAtLocal para forzar sincronización (asegurar que el local sea más reciente)
            val updatedAtLocal = System.currentTimeMillis()
            
            // Crear una copia del perfil con el timestamp actualizado y estado PENDING
            val updatedProfile = profileEntity.copy(
                updatedAtLocal = updatedAtLocal,
                syncState = SyncState.PENDING
            )
            profileDao.upsertProfile(updatedProfile)
            
            Log.d("SyncRepository", "✅ Updated profile in DB with new timestamp: $updatedAtLocal, syncState: PENDING")
            
            // Pequeña espera para asegurar que la base de datos se actualizó
            kotlinx.coroutines.delay(200)
            
            // Re-verificar el perfil después del delay para asegurarnos de tener los datos más recientes
            val finalProfile = profileDao.getAllProfiles().find { it.uid == uid } ?: updatedProfile
            Log.d("SyncRepository", "📋 Final profile to sync - syncState: ${finalProfile.syncState}, xp: ${finalProfile.xp}, updatedAtLocal: ${finalProfile.updatedAtLocal}")
            
            // Sincronizar inmediatamente
            Log.d("SyncRepository", "🚀 Calling syncService.syncUserProfile for $uid")
            val success = syncService.syncUserProfile(finalProfile)
            
            Log.d("SyncRepository", "📤 Sync result for $uid: $success")
            
            if (success) {
                profileDao.updateProfileSyncState(uid, SyncState.SYNCED)
                Log.d("SyncRepository", "✅✅ Successfully synced user profile: $uid - syncState updated to SYNCED")
            } else {
                profileDao.updateProfileSyncState(uid, SyncState.FAILED)
                Log.w("SyncRepository", "❌❌ Failed to sync user profile: $uid - syncState updated to FAILED - check FirestoreSyncService logs for details")
            }
            
            success
        } catch (e: Exception) {
            Log.e("SyncRepository", "💥 Exception syncing user profile $uid", e)
            Log.e("SyncRepository", "Exception type: ${e.javaClass.simpleName}")
            Log.e("SyncRepository", "Exception message: ${e.message}")
            e.printStackTrace()
            val profileDao = database.profileDao()
            profileDao.updateProfileSyncState(uid, SyncState.FAILED)
            false
        }
    }
}
