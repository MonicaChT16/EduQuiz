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
import com.eduquiz.domain.exam.ExamStatus
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
    
    // Prevenir múltiples llamadas simultáneas a syncUserProfileNow
    private val syncingUsers = mutableSetOf<String>()

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
            val examDao = database.examDao()
            
            // IMPORTANTE: Primero sincronizar intentos pendientes para que las métricas incluyan el examen recién completado
            Log.d("SyncRepository", "📝 Step 1: Syncing pending exam attempts for $uid")
            
            // Verificar que el uid no esté vacío
            if (uid.isBlank()) {
                Log.e("SyncRepository", "❌ ERROR: uid is blank!")
                return false
            }
            
            // Obtener TODOS los intentos del usuario primero para verificar
            Log.d("SyncRepository", "📊 Querying local DB for attempts with uid: $uid")
            val allUserAttempts = examDao.getAttempts(uid)
            Log.d("SyncRepository", "📊 All attempts for user $uid: ${allUserAttempts.size} total")
            
            if (allUserAttempts.isEmpty()) {
                Log.w("SyncRepository", "⚠️ WARNING: No attempts found in local DB for user $uid")
                Log.w("SyncRepository", "   This might mean:")
                Log.w("SyncRepository", "   1. The exam was not saved to Room")
                Log.w("SyncRepository", "   2. The uid used to save is different from $uid")
                Log.w("SyncRepository", "   3. The attempts were deleted")
            } else {
                allUserAttempts.forEach { attempt ->
                    Log.d("SyncRepository", "   - ${attempt.attemptId}: uid=${attempt.uid}, status=${attempt.status}, scoreRaw=${attempt.scoreRaw}, syncState=${attempt.syncState}, finishedAt=${attempt.finishedAtLocal}")
                }
            }
            
            // Obtener intentos completados del usuario (no solo PENDING, sino todos los completados)
            val completedAttempts = allUserAttempts.filter { 
                it.status == ExamStatus.COMPLETED || it.status == ExamStatus.AUTO_SUBMIT 
            }
            Log.d("SyncRepository", "📊 Found ${completedAttempts.size} completed attempts for user $uid")
            
            val pendingAttempts = examDao.getAttemptsBySyncState(SyncState.PENDING)
            Log.d("SyncRepository", "Found ${pendingAttempts.size} pending exam attempts (all users)")
            
            var syncedCount = 0
            var failedCount = 0
            var skippedCount = 0
            
            for (attempt in pendingAttempts) {
                if (attempt.uid == uid) {
                    // Verificar que el intento esté completado
                    if (attempt.status != ExamStatus.COMPLETED && attempt.status != ExamStatus.AUTO_SUBMIT) {
                        Log.w("SyncRepository", "⚠️ Skipping attempt ${attempt.attemptId} - status is ${attempt.status}, not COMPLETED/AUTO_SUBMIT")
                        skippedCount++
                        continue
                    }
                    
                    Log.d("SyncRepository", "   Processing attempt ${attempt.attemptId} (status=${attempt.status}, scoreRaw=${attempt.scoreRaw})")
                    val answers = examDao.getAnswers(attempt.attemptId)
                    Log.d("SyncRepository", "   Attempt has ${answers.size} answers, ${answers.count { it.isCorrect }} correct")
                    
                    if (answers.isEmpty()) {
                        Log.w("SyncRepository", "⚠️ Attempt ${attempt.attemptId} has no answers, skipping sync")
                        skippedCount++
                        continue
                    }
                    
                    val success = syncService.syncExamAttempt(attempt, answers)
                    if (success) {
                        examDao.updateSyncState(attempt.attemptId, SyncState.SYNCED)
                        syncedCount++
                        Log.d("SyncRepository", "✅ Synced exam attempt ${attempt.attemptId}")
                    } else {
                        failedCount++
                        Log.w("SyncRepository", "⚠️ Failed to sync exam attempt ${attempt.attemptId}")
                    }
                }
            }
            
            Log.d("SyncRepository", "📊 Step 1 Summary: $syncedCount synced, $failedCount failed, $skippedCount skipped for user $uid")
            Log.d("SyncRepository", "📊 Total completed attempts for user: ${completedAttempts.size} (these will be used for metrics calculation)")
            
            // IMPORTANTE: Esperar un poco más para asegurar que:
            // 1. Los intentos se hayan guardado completamente en Room
            // 2. Los intentos sincronizados se hayan actualizado en Room
            // 3. Cualquier operación pendiente de Room haya terminado
            kotlinx.coroutines.delay(800) // Aumentado a 800ms para dar más tiempo
            
            // Re-verificar los intentos después del delay
            val attemptsAfterDelay = examDao.getAttempts(uid)
            Log.d("SyncRepository", "📊 Attempts after delay: ${attemptsAfterDelay.size} total")
            val completedAfterDelay = attemptsAfterDelay.filter { 
                it.status == ExamStatus.COMPLETED || it.status == ExamStatus.AUTO_SUBMIT 
            }
            Log.d("SyncRepository", "📊 Completed attempts after delay: ${completedAfterDelay.size}")
            
            // Obtener el perfil actual usando getAllProfiles para evitar problemas con Flow
            // Esperar un poco más para asegurar que las actualizaciones de coins/XP se hayan completado
            kotlinx.coroutines.delay(300)
            
            val allProfiles = profileDao.getAllProfiles()
            val profileEntity = allProfiles.find { it.uid == uid } ?: run {
                Log.w("SyncRepository", "❌ Profile not found for $uid")
                return false
            }
            
            Log.d("SyncRepository", "📊 Current profile state BEFORE update - syncState: ${profileEntity.syncState}, xp: ${profileEntity.xp}, coins: ${profileEntity.coins}, updatedAtLocal: ${profileEntity.updatedAtLocal}")
            
            // Si el perfil ya está en PENDING, no necesitamos actualizar el timestamp
            // Solo asegurarnos de que esté en PENDING
            val updatedAtLocal = if (profileEntity.syncState == SyncState.PENDING && profileEntity.updatedAtLocal > 0) {
                // Ya está PENDING, usar el timestamp existente o actualizarlo ligeramente
                kotlin.math.max(profileEntity.updatedAtLocal, System.currentTimeMillis() - 1000)
            } else {
                // No está PENDING, forzar actualización
                System.currentTimeMillis()
            }
            
            // Crear una copia del perfil con el timestamp actualizado y estado PENDING
            // IMPORTANTE: Preservar los valores de xp y coins que ya fueron actualizados por addCoins/addXp
            val updatedProfile = profileEntity.copy(
                updatedAtLocal = updatedAtLocal,
                syncState = SyncState.PENDING
            )
            profileDao.upsertProfile(updatedProfile)
            
            Log.d("SyncRepository", "✅ Updated profile in DB with new timestamp: $updatedAtLocal, syncState: PENDING")
            Log.d("SyncRepository", "   Profile values - xp: ${updatedProfile.xp}, coins: ${updatedProfile.coins}")
            
            // Pequeña espera para asegurar que la base de datos se actualizó
            kotlinx.coroutines.delay(300)
            
            // Re-verificar el perfil después del delay para asegurarnos de tener los datos más recientes
            val finalProfile = profileDao.getAllProfiles().find { it.uid == uid } ?: updatedProfile
            Log.d("SyncRepository", "📋 Final profile to sync - syncState: ${finalProfile.syncState}, xp: ${finalProfile.xp}, coins: ${finalProfile.coins}, updatedAtLocal: ${finalProfile.updatedAtLocal}")
            
            // Verificar que el perfil tiene valores válidos
            if (finalProfile.xp == 0L && finalProfile.coins == 0) {
                Log.w("SyncRepository", "⚠️ WARNING: Profile has 0 XP and 0 coins - this might indicate coins/XP were not added correctly")
            }
            
            // Sincronizar inmediatamente (esto recalculará las métricas incluyendo los intentos recién sincronizados)
            // IMPORTANTE: Pasar los intentos y respuestas que ya encontramos para evitar race conditions
            Log.d("SyncRepository", "🚀 Calling syncService.syncUserProfile for $uid")
            Log.d("SyncRepository", "📊 Profile data before sync - xp: ${finalProfile.xp}, coins: ${finalProfile.coins}, syncState: ${finalProfile.syncState}")
            Log.d("SyncRepository", "📊 Passing ${completedAfterDelay.size} completed attempts to syncService to avoid race conditions")
            
            // Convertir entidades a domain objects para pasarlas a syncService
            val attemptsToPass = completedAfterDelay.map { it.toDomain() }
            
            // Obtener las respuestas para cada intento y crear un mapa
            // IMPORTANTE: Esperar un poco más para asegurar que las respuestas estén guardadas
            kotlinx.coroutines.delay(500) // Delay adicional para asegurar que las respuestas estén disponibles
            
            val answersMap = mutableMapOf<String, List<com.eduquiz.domain.exam.ExamAnswer>>()
            completedAfterDelay.forEach { attempt ->
                Log.d("SyncRepository", "   Fetching answers for attempt ${attempt.attemptId} (status=${attempt.status}, scoreRaw=${attempt.scoreRaw})")
                val answers = examDao.getAnswers(attempt.attemptId)
                Log.d("SyncRepository", "   Attempt ${attempt.attemptId}: Found ${answers.size} answers in DB")
                
                if (answers.isEmpty()) {
                    Log.w("SyncRepository", "   ⚠️ WARNING: Attempt ${attempt.attemptId} has NO answers in DB but scoreRaw=${attempt.scoreRaw}")
                    Log.w("SyncRepository", "   This might indicate answers were not saved or there's a timing issue")
                } else {
                    Log.d("SyncRepository", "   Attempt ${attempt.attemptId}: ${answers.count { it.isCorrect }} correct out of ${answers.size} total")
                    answersMap[attempt.attemptId] = answers.map { it.toDomain() }
                }
            }
            Log.d("SyncRepository", "📊 Prepared answers map with ${answersMap.size} entries (total answers: ${answersMap.values.sumOf { it.size }})")
            
            if (answersMap.isEmpty() && completedAfterDelay.isNotEmpty()) {
                Log.e("SyncRepository", "❌ ERROR: No answers found for any completed attempts!")
                Log.e("SyncRepository", "   This will cause metrics to show 0 correct answers and 0 questions")
                Log.e("SyncRepository", "   Attempt IDs: ${completedAfterDelay.map { it.attemptId }}")
            }
            
            // PASO 1: Calcular métricas desde Room ANTES de sincronizar
            // IMPORTANTE: Calcular desde TODOS los intentos completados directamente desde Room
            Log.d("SyncRepository", "📊 STEP 1: Calculating metrics from Room BEFORE syncing to Firestore")
            var totalAttempts = completedAfterDelay.size
            var totalCorrect = 0
            var totalQuestions = 0
            
            // Buscar respuestas directamente desde Room para TODOS los intentos completados
            Log.d("SyncRepository", "📊 Fetching answers from Room for ${completedAfterDelay.size} completed attempts")
            completedAfterDelay.forEach { attempt ->
                val answers = examDao.getAnswers(attempt.attemptId)
                totalQuestions += answers.size
                totalCorrect += answers.count { it.isCorrect }
                Log.d("SyncRepository", "   Attempt ${attempt.attemptId}: ${answers.size} answers, ${answers.count { it.isCorrect }} correct, scoreRaw=${attempt.scoreRaw}")
                
                // Si no hay respuestas pero scoreRaw > 0, hay un problema
                if (answers.isEmpty() && attempt.scoreRaw > 0) {
                    Log.e("SyncRepository", "   ❌ ERROR: Attempt ${attempt.attemptId} has scoreRaw=${attempt.scoreRaw} but NO answers in Room!")
                }
            }
            
            val averageAccuracy = if (totalQuestions > 0) {
                (totalCorrect.toFloat() / totalQuestions.toFloat()) * 100f
            } else {
                0f
            }
            
            Log.d("SyncRepository", "📊 Calculated metrics from Room: attempts=$totalAttempts, correct=$totalCorrect, questions=$totalQuestions, accuracy=$averageAccuracy%")
            
            // PASO 2: Guardar métricas en Room PRIMERO
            val metricsUpdatedAt = System.currentTimeMillis()
            Log.d("SyncRepository", "📊 STEP 2: Saving metrics to Room: attempts=$totalAttempts, correct=$totalCorrect, questions=$totalQuestions, accuracy=$averageAccuracy%")
            try {
                profileDao.updateRankingMetrics(
                    uid = uid,
                    totalAttempts = totalAttempts,
                    totalCorrectAnswers = totalCorrect,
                    totalQuestions = totalQuestions,
                    averageAccuracy = averageAccuracy,
                    updatedAtLocal = metricsUpdatedAt,
                    syncState = SyncState.PENDING // Marcar como PENDING para forzar sincronización
                )
                
                // Verificar que se guardó correctamente
                kotlinx.coroutines.delay(200) // Delay para asegurar que se guardó
                val updatedProfile = profileDao.getAllProfiles().find { it.uid == uid }
                if (updatedProfile != null) {
                    Log.d("SyncRepository", "✅ Metrics saved in Room - Verified:")
                    Log.d("SyncRepository", "   - totalAttempts: ${updatedProfile.totalAttempts} (expected: $totalAttempts)")
                    Log.d("SyncRepository", "   - totalCorrectAnswers: ${updatedProfile.totalCorrectAnswers} (expected: $totalCorrect)")
                    Log.d("SyncRepository", "   - totalQuestions: ${updatedProfile.totalQuestions} (expected: $totalQuestions)")
                    Log.d("SyncRepository", "   - averageAccuracy: ${updatedProfile.averageAccuracy} (expected: $averageAccuracy)")
                    
                    if (updatedProfile.totalAttempts != totalAttempts || 
                        updatedProfile.totalCorrectAnswers != totalCorrect ||
                        updatedProfile.totalQuestions != totalQuestions ||
                        Math.abs(updatedProfile.averageAccuracy - averageAccuracy) > 0.01f) {
                        Log.e("SyncRepository", "❌ ERROR: Metrics in Room don't match expected values!")
                    } else {
                        Log.d("SyncRepository", "✅ All metrics saved correctly in Room")
                    }
                } else {
                    Log.e("SyncRepository", "❌ ERROR: Could not find profile after updating metrics!")
                }
            } catch (e: Exception) {
                Log.e("SyncRepository", "❌ ERROR updating ranking metrics in Room", e)
                e.printStackTrace()
            }
            
            // PASO 3: Sincronizar a Firestore usando las métricas de Room
            // Leer el perfil actualizado de Room para pasar las métricas correctas
            val profileWithMetrics = profileDao.getAllProfiles().find { it.uid == uid } ?: finalProfile
            Log.d("SyncRepository", "📊 STEP 3: Syncing to Firestore using metrics from Room")
            Log.d("SyncRepository", "   Room metrics: attempts=${profileWithMetrics.totalAttempts}, correct=${profileWithMetrics.totalCorrectAnswers}, questions=${profileWithMetrics.totalQuestions}, accuracy=${profileWithMetrics.averageAccuracy}")
            
            val success = syncService.syncUserProfile(profileWithMetrics, null, null) // Pasar null para que use las métricas de Room
            
            Log.d("SyncRepository", "📤 Sync result for $uid: $success")
            
            if (!success) {
                Log.e("SyncRepository", "❌❌❌ SYNC FAILED for $uid - check FirestoreSyncService logs above for details")
                Log.e("SyncRepository", "This means metrics were NOT updated in Firestore")
            } else {
                Log.d("SyncRepository", "✅✅✅ SYNC SUCCESS for $uid - metrics should be updated in Firestore")
            }
            
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
        } finally {
            // Siempre remover el uid del set, incluso si hay error
            syncingUsers.remove(uid)
        }
    }
}
