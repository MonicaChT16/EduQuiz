package com.eduquiz.data.remote

import com.eduquiz.data.db.ExamAnswerEntity
import com.eduquiz.data.db.ExamAttemptEntity
import com.eduquiz.data.db.UserProfileEntity
import com.eduquiz.domain.exam.ExamRepository
import com.eduquiz.domain.exam.ExamStatus
import com.eduquiz.domain.profile.SyncState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para sincronizar datos locales con Firestore.
 * 
 * Reglas de sincronización:
 * - examAttempts: merge/unión (nunca borrar) - usar merge: true
 * - perfil: última escritura gana - usar transacción o comparación de timestamps
 */
@Singleton
class FirestoreSyncService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val examRepository: ExamRepository,
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Sincroniza un intento de examen y sus respuestas a Firestore.
     * Ruta: users/{uid}/examAttempts/{attemptId}
     * 
     * @return true si la sincronización fue exitosa, false en caso contrario
     */
    suspend fun syncExamAttempt(
        attempt: ExamAttemptEntity,
        answers: List<ExamAnswerEntity>
    ): Boolean {
        return try {
            val attemptRef = firestore
                .collection("users")
                .document(attempt.uid)
                .collection("examAttempts")
                .document(attempt.attemptId)

            // Construir payload del intento
            val attemptData = mapOf(
                "attemptId" to attempt.attemptId,
                "uid" to attempt.uid,
                "packId" to attempt.packId,
                "subject" to (attempt.subject ?: ""),
                "startedAtLocal" to attempt.startedAtLocal,
                "finishedAtLocal" to attempt.finishedAtLocal,
                "durationMs" to attempt.durationMs,
                "status" to attempt.status,
                "scoreRaw" to attempt.scoreRaw,
                "scoreValidated" to attempt.scoreValidated,
                "origin" to attempt.origin,
                "syncState" to SyncState.SYNCED,
                "lastSyncedAt" to System.currentTimeMillis()
            )

            // Construir payload de respuestas
            val answersData = answers.map { answer ->
                mapOf(
                    "questionId" to answer.questionId,
                    "selectedOptionId" to answer.selectedOptionId,
                    "isCorrect" to answer.isCorrect,
                    "timeSpentMs" to answer.timeSpentMs
                )
            }

            // Usar batch write para atomicidad
            val batch = firestore.batch()
            
            // Escribir intento con merge: true (nunca borrar, solo actualizar)
            batch.set(attemptRef, attemptData, SetOptions.merge())

            // Escribir respuestas como subcolección
            answers.forEach { answer ->
                val answerRef = attemptRef
                    .collection("answers")
                    .document(answer.questionId)
                batch.set(answerRef, mapOf(
                    "questionId" to answer.questionId,
                    "selectedOptionId" to answer.selectedOptionId,
                    "isCorrect" to answer.isCorrect,
                    "timeSpentMs" to answer.timeSpentMs
                ), SetOptions.merge())
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSyncService", "❌ Error syncing exam attempt ${attempt.attemptId} (${attempt.packId})", e)
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                android.util.Log.e("FirestoreSyncService", "   ⚠️ PERMISSION_DENIED: Check Firestore security rules for users/{uid}/examAttempts")
                android.util.Log.e("FirestoreSyncService", "   The attempt will remain in PENDING state and will be retried later")
            }
            false
        }
    }

    /**
     * Calcula las estadísticas de ranking del usuario.
     * Primero intenta desde datos locales, si no hay datos o hay muy pocos, intenta desde Firestore.
     * Retorna: (accuracy, totalAttempts, totalCorrectAnswers, totalQuestions)
     * 
     * @param uid ID del usuario
     * @param providedAttempts Intentos ya obtenidos (opcional) - si se proporcionan, se usan estos en lugar de buscar de nuevo
     * @param providedAnswersMap Mapa de attemptId a respuestas (opcional) - si se proporciona, se usan estas respuestas en lugar de buscarlas de nuevo
     */
    private suspend fun calculateRankingStats(
        uid: String, 
        providedAttempts: List<com.eduquiz.domain.exam.ExamAttempt>? = null,
        providedAnswersMap: Map<String, List<com.eduquiz.domain.exam.ExamAnswer>>? = null
    ): RankingStats {
        return try {
            android.util.Log.d("FirestoreSyncService", "📊 Calculating ranking stats for $uid")
            
            // Verificar que el uid no esté vacío
            if (uid.isBlank()) {
                android.util.Log.e("FirestoreSyncService", "❌ ERROR: uid is blank!")
                return RankingStats(0f, 0, 0, 0)
            }
            
            // 1. Intentar calcular desde datos locales primero
            // IMPORTANTE: Si se proporcionaron intentos, usarlos directamente para evitar race conditions
            val localAttempts = if (providedAttempts != null) {
                android.util.Log.d("FirestoreSyncService", "📊 Using provided attempts: ${providedAttempts.size} total")
                providedAttempts
            } else {
                // IMPORTANTE: Buscar TODOS los intentos completados, no importa su syncState
                // porque necesitamos incluirlos en las métricas incluso si aún no están sincronizados
                android.util.Log.d("FirestoreSyncService", "📊 Querying local DB for attempts with uid: $uid")
                examRepository.getAttempts(uid)
            }
            android.util.Log.d("FirestoreSyncService", "📊 Found ${localAttempts.size} total attempts in local DB for $uid")
            
            // Listar todos los intentos con sus detalles
            localAttempts.forEach { attempt ->
                android.util.Log.d("FirestoreSyncService", "   - ${attempt.attemptId}: status=${attempt.status}, scoreRaw=${attempt.scoreRaw}, syncState=${attempt.syncState}, finishedAt=${attempt.finishedAtLocal}")
            }
            
            val localCompletedAttempts = localAttempts.filter { 
                it.status == ExamStatus.COMPLETED || it.status == ExamStatus.AUTO_SUBMIT 
            }
            
            android.util.Log.d("FirestoreSyncService", "📊 Found ${localCompletedAttempts.size} completed attempts (from ${localAttempts.size} total)")
            
            // Si no hay intentos completados, mostrar advertencia
            if (localCompletedAttempts.isEmpty() && localAttempts.isNotEmpty()) {
                android.util.Log.w("FirestoreSyncService", "⚠️ WARNING: User has ${localAttempts.size} attempts but NONE are completed!")
                android.util.Log.w("FirestoreSyncService", "   Attempt statuses: ${localAttempts.map { it.status }.distinct()}")
            }
            
            var totalAttempts = localCompletedAttempts.size
            var totalCorrect = 0
            var totalQuestions = 0
            
            localCompletedAttempts.forEach { attempt ->
                // Si se proporcionaron respuestas, usarlas; de lo contrario, buscarlas
                val answers = if (providedAnswersMap != null && providedAnswersMap.containsKey(attempt.attemptId)) {
                    android.util.Log.d("FirestoreSyncService", "Using provided answers for attempt ${attempt.attemptId}")
                    providedAnswersMap[attempt.attemptId] ?: emptyList()
                } else {
                    android.util.Log.d("FirestoreSyncService", "Fetching answers from DB for attempt ${attempt.attemptId}")
                    examRepository.getAnswersForAttempt(attempt.attemptId)
                }
                totalQuestions += answers.size
                val correctCount = answers.count { it.isCorrect }
                totalCorrect += correctCount
                android.util.Log.d("FirestoreSyncService", "Local attempt ${attempt.attemptId} (status=${attempt.status}, packId=${attempt.packId}): ${answers.size} answers, $correctCount correct, scoreRaw=${attempt.scoreRaw}")
            }
            
            android.util.Log.d("FirestoreSyncService", "📊 Local stats calculated: attempts=$totalAttempts, correct=$totalCorrect, questions=$totalQuestions")
            
            // Si no hay intentos completados, log de advertencia con más detalles
            if (totalAttempts == 0) {
                android.util.Log.w("FirestoreSyncService", "⚠️ WARNING: No completed attempts found for user $uid")
                android.util.Log.w("FirestoreSyncService", "   Total attempts in DB: ${localAttempts.size}")
                android.util.Log.w("FirestoreSyncService", "   Attempt statuses: ${localAttempts.map { "${it.attemptId}=${it.status}" }}")
                // Listar todos los intentos con sus detalles
                localAttempts.forEach { attempt ->
                    android.util.Log.w("FirestoreSyncService", "   - ${attempt.attemptId}: status=${attempt.status}, packId=${attempt.packId}, scoreRaw=${attempt.scoreRaw}, finishedAt=${attempt.finishedAtLocal}")
                }
            } else {
                android.util.Log.d("FirestoreSyncService", "✅ Found $totalAttempts completed attempts with $totalCorrect correct answers out of $totalQuestions total questions")
            }
            
            // 2. Si no hay suficientes datos locales, intentar obtener desde Firestore
            // Esto es importante cuando se reinstala la app y los datos locales se perdieron
            // PERO: Si se proporcionaron intentos pero no se encontraron respuestas, NO buscar en Firestore
            // porque significa que las respuestas aún no están guardadas (problema de timing)
            if (totalAttempts == 0 && providedAttempts == null) {
                android.util.Log.d("FirestoreSyncService", "No local attempts found, fetching from Firestore...")
                try {
                    // Usar dos consultas separadas porque whereIn con 2 valores puede no requerir índice compuesto
                    // y es más compatible con Firestore
                    val attemptsCompleted = firestore
                        .collection("users")
                        .document(uid)
                        .collection("examAttempts")
                        .whereEqualTo("status", ExamStatus.COMPLETED)
                        .get()
                        .await()
                    
                    val attemptsAutoSubmit = firestore
                        .collection("users")
                        .document(uid)
                        .collection("examAttempts")
                        .whereEqualTo("status", ExamStatus.AUTO_SUBMIT)
                        .get()
                        .await()
                    
                    // Combinar ambas consultas
                    val allFirestoreAttempts = attemptsCompleted.documents + attemptsAutoSubmit.documents
                    
                    android.util.Log.d("FirestoreSyncService", "Found ${allFirestoreAttempts.size} completed attempts in Firestore")
                    
                    allFirestoreAttempts.forEach { attemptDoc ->
                        totalAttempts++
                        
                        // Obtener respuestas de este intento desde Firestore
                        val answersRef = attemptDoc.reference.collection("answers").get().await()
                        val correctCount = answersRef.documents.count { answerDoc ->
                            answerDoc.getBoolean("isCorrect") == true
                        }
                        
                        totalQuestions += answersRef.documents.size
                        totalCorrect += correctCount
                        
                        android.util.Log.d("FirestoreSyncService", "Firestore attempt ${attemptDoc.id}: ${answersRef.documents.size} answers, $correctCount correct")
                    }
                    
                    if (totalAttempts > 0) {
                        android.util.Log.d("FirestoreSyncService", "✅ Successfully retrieved stats from Firestore")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FirestoreSyncService", "⚠️ Could not fetch attempts from Firestore (user might not have any): ${e.message}")
                    // No es un error crítico, simplemente continuar con 0
                }
            }
            
            val accuracy = if (totalQuestions > 0) {
                (totalCorrect.toFloat() / totalQuestions.toFloat()) * 100f
            } else {
                0f
            }
            
            android.util.Log.d("FirestoreSyncService", "📊 Stats calculated - accuracy: $accuracy%, attempts: $totalAttempts, correct: $totalCorrect/$totalQuestions")
            
            RankingStats(
                accuracy = accuracy,
                totalAttempts = totalAttempts,
                totalCorrectAnswers = totalCorrect,
                totalQuestions = totalQuestions
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSyncService", "❌ Error calculating ranking stats for $uid", e)
            android.util.Log.e("FirestoreSyncService", "Error details: ${e.message}")
            e.printStackTrace()
            RankingStats(0f, 0, 0, 0)
        }
    }
    
    /**
     * Datos de estadísticas de ranking calculadas.
     */
    private data class RankingStats(
        val accuracy: Float,
        val totalAttempts: Int,
        val totalCorrectAnswers: Int,
        val totalQuestions: Int
    )

    /**
     * Sincroniza el perfil del usuario a Firestore.
     * Ruta: users/{uid}
     * 
     * Regla: última escritura gana - comparar updatedAtLocal antes de escribir.
     * 
     * Incluye todos los campos del diseño:
     * - Datos básicos: uid, displayName, email, photoUrl
     * - Datos de colegio: schoolCode (del ugelCode), ugelCode
     * - Métricas de ranking: totalXp, averageAccuracy, totalAttempts, totalCorrectAnswers, totalQuestions
     * - Otros: coins, selectedCosmeticId, timestamps
     * 
     * @param profile Perfil del usuario a sincronizar
     * @param providedAttempts Intentos ya obtenidos (opcional) - si se proporcionan, se usan estos para calcular métricas en lugar de buscar de nuevo
     * @return true si la sincronización fue exitosa, false en caso contrario
     */
    suspend fun syncUserProfile(
        profile: UserProfileEntity, 
        providedAttempts: List<com.eduquiz.domain.exam.ExamAttempt>? = null,
        providedAnswersMap: Map<String, List<com.eduquiz.domain.exam.ExamAnswer>>? = null
    ): Boolean {
        return try {
            android.util.Log.d("FirestoreSyncService", "Starting sync for user profile: ${profile.uid}")
            val profileRef = firestore.collection("users").document(profile.uid)

            // Leer el perfil remoto para obtener datos existentes (email, etc.)
            android.util.Log.d("FirestoreSyncService", "Reading remote profile for ${profile.uid}")
            val remoteSnapshot = profileRef.get().await()
            
            // Comparar updatedAtLocal: última escritura gana
            // Si el perfil local tiene syncState PENDING, siempre sincronizar (forzar actualización)
            val remoteUpdatedAtLocal = if (remoteSnapshot.exists()) {
                remoteSnapshot.getLong("updatedAtLocal") ?: 0L
            } else {
                0L // Si no existe, el local es más reciente
            }
            
            val remoteLastSyncedAt = if (remoteSnapshot.exists()) {
                remoteSnapshot.getLong("lastSyncedAt") ?: 0L
            } else {
                0L
            }
            
            // Sincronizar si:
            // 1. El perfil está PENDING (forzar sincronización)
            // 2. El perfil local es más reciente que el remoto
            // 3. El documento no existe en remoto
            val shouldSync = profile.syncState == SyncState.PENDING || 
                           profile.updatedAtLocal > remoteUpdatedAtLocal ||
                           !remoteSnapshot.exists()
            
            android.util.Log.d("FirestoreSyncService", "📊 Sync decision for ${profile.uid}:")
            android.util.Log.d("FirestoreSyncService", "   - Remote updatedAtLocal: $remoteUpdatedAtLocal")
            android.util.Log.d("FirestoreSyncService", "   - Remote lastSyncedAt: $remoteLastSyncedAt")
            android.util.Log.d("FirestoreSyncService", "   - Local updatedAtLocal: ${profile.updatedAtLocal}")
            android.util.Log.d("FirestoreSyncService", "   - Profile syncState: ${profile.syncState}")
            android.util.Log.d("FirestoreSyncService", "   - Should sync: $shouldSync (PENDING=${profile.syncState == SyncState.PENDING}, newer=${profile.updatedAtLocal > remoteUpdatedAtLocal}, notExists=${!remoteSnapshot.exists()})")

            // Sincronizar si el perfil está PENDING, es más reciente, o no existe en remoto
            if (shouldSync) {
                android.util.Log.d("FirestoreSyncService", "Local is newer or equal, proceeding with sync")
                
                // IMPORTANTE: Usar las métricas de Room en lugar de calcularlas
                // Las métricas ya fueron calculadas y guardadas en Room antes de llamar a syncUserProfile
                var stats = RankingStats(
                    accuracy = profile.averageAccuracy,
                    totalAttempts = profile.totalAttempts,
                    totalCorrectAnswers = profile.totalCorrectAnswers,
                    totalQuestions = profile.totalQuestions
                )
                android.util.Log.d("FirestoreSyncService", "📊 Using metrics from Room (not calculating):")
                android.util.Log.d("FirestoreSyncService", "   - totalAttempts: ${stats.totalAttempts}")
                android.util.Log.d("FirestoreSyncService", "   - totalCorrectAnswers: ${stats.totalCorrectAnswers}")
                android.util.Log.d("FirestoreSyncService", "   - totalQuestions: ${stats.totalQuestions}")
                android.util.Log.d("FirestoreSyncService", "   - averageAccuracy: ${stats.accuracy}%")
                
                // PROTECCIÓN: No sobrescribir valores válidos en Firestore con 0 de Room
                // Si Firestore tiene valores válidos (no 0) y Room tiene 0, mantener los valores de Firestore
                if (remoteSnapshot.exists()) {
                    val remoteTotalAttempts = remoteSnapshot.getLong("totalAttempts") ?: 0L
                    val remoteTotalCorrect = remoteSnapshot.getLong("totalCorrectAnswers") ?: 0L
                    val remoteTotalQuestions = remoteSnapshot.getLong("totalQuestions") ?: 0L
                    val remoteAccuracy = remoteSnapshot.getDouble("averageAccuracy") ?: 0.0
                    
                    // Si Room tiene 0 pero Firestore tiene valores válidos, usar los de Firestore
                    if (stats.totalAttempts == 0 && remoteTotalAttempts > 0) {
                        android.util.Log.w("FirestoreSyncService", "⚠️ WARNING: Room has 0 attempts but Firestore has $remoteTotalAttempts")
                        android.util.Log.w("FirestoreSyncService", "   Using Firestore values to avoid overwriting valid data with 0")
                        android.util.Log.w("FirestoreSyncService", "   This might indicate Room metrics were not saved correctly")
                        
                        // Usar los valores de Firestore en lugar de los de Room
                        stats = RankingStats(
                            accuracy = remoteAccuracy.toFloat(),
                            totalAttempts = remoteTotalAttempts.toInt(),
                            totalCorrectAnswers = remoteTotalCorrect.toInt(),
                            totalQuestions = remoteTotalQuestions.toInt()
                        )
                        android.util.Log.d("FirestoreSyncService", "📊 Using metrics from Firestore instead of Room:")
                        android.util.Log.d("FirestoreSyncService", "   - totalAttempts: ${stats.totalAttempts}")
                        android.util.Log.d("FirestoreSyncService", "   - totalCorrectAnswers: ${stats.totalCorrectAnswers}")
                        android.util.Log.d("FirestoreSyncService", "   - totalQuestions: ${stats.totalQuestions}")
                        android.util.Log.d("FirestoreSyncService", "   - averageAccuracy: ${stats.accuracy}%")
                    }
                }
                
                // Obtener email: primero del documento remoto si existe, luego del usuario actual si coincide
                val userEmail = if (remoteSnapshot.exists()) {
                    remoteSnapshot.getString("email")?.takeIf { it.isNotBlank() }
                } else {
                    null
                } ?: run {
                    // Si no existe en remoto, intentar obtener del usuario actual si coincide
                    val currentUser = firebaseAuth.currentUser
                    if (currentUser?.uid == profile.uid) {
                        currentUser.email ?: ""
                    } else {
                        "" // Si no coincide, dejar vacío
                    }
                }
                android.util.Log.d("FirestoreSyncService", "User email: ${if (userEmail.isBlank()) "NOT AVAILABLE" else userEmail}")
                
                // schoolCode: usar ugelCode como código de colegio/UGEL (ingresado manualmente por el usuario)
                // Si ugelCode está vacío o null, schoolCode también estará vacío
                val schoolCode = profile.ugelCode?.takeIf { it.isNotBlank() } ?: ""
                
                val profileData = mapOf(
                    // Datos básicos del usuario
                    "uid" to profile.uid,
                    "displayName" to profile.displayName,
                    "email" to userEmail,
                    "photoUrl" to profile.photoUrl,
                    
                    // Datos de colegio/UGEL
                    "schoolCode" to schoolCode,  // Código de colegio/UGEL (ingresado manualmente por el usuario)
                    "ugelCode" to profile.ugelCode,  // Guardar también el código UGEL original (compatibilidad)
                    
                    // Monedas y XP
                    "coins" to profile.coins,
                    
                    // Métricas de ranking (pre-calculadas para consultas rápidas)
                    "totalXp" to profile.xp.toLong(),  // XP total acumulado (único campo de XP)
                    "totalScore" to profile.xp.toLong(),  // Alias de totalXp para compatibilidad con ranking
                    "averageAccuracy" to stats.accuracy,  // Promedio de aciertos (%)
                    "totalAttempts" to stats.totalAttempts,  // Total de exámenes completados
                    "totalCorrectAnswers" to stats.totalCorrectAnswers,  // Total de respuestas correctas
                    "totalQuestions" to stats.totalQuestions,  // Total de preguntas respondidas
                    
                    // Otros campos
                    "selectedCosmeticId" to profile.selectedCosmeticId,
                    "updatedAtLocal" to profile.updatedAtLocal,  // Timestamp local para comparación (última escritura gana)
                    "lastSyncedAt" to System.currentTimeMillis()  // Timestamp de última sincronización
                )
                
                android.util.Log.d("FirestoreSyncService", "📤 Writing profile data to Firestore:")
                android.util.Log.d("FirestoreSyncService", "  - uid: ${profileData["uid"]}")
                android.util.Log.d("FirestoreSyncService", "  - totalXp: ${profileData["totalXp"]}")
                android.util.Log.d("FirestoreSyncService", "  - averageAccuracy: ${profileData["averageAccuracy"]}")
                android.util.Log.d("FirestoreSyncService", "  - totalAttempts: ${profileData["totalAttempts"]}")
                android.util.Log.d("FirestoreSyncService", "  - totalCorrectAnswers: ${profileData["totalCorrectAnswers"]}")
                android.util.Log.d("FirestoreSyncService", "  - totalQuestions: ${profileData["totalQuestions"]}")
                android.util.Log.d("FirestoreSyncService", "  - coins: ${profileData["coins"]}")
                android.util.Log.d("FirestoreSyncService", "  - updatedAtLocal: ${profileData["updatedAtLocal"]}")
                
                try {
                    profileRef.set(profileData, SetOptions.merge()).await()
                    android.util.Log.d("FirestoreSyncService", "✅ Write operation completed")
                    
                    // Verificar que se escribió correctamente
                    val verifySnapshot = profileRef.get().await()
                    if (verifySnapshot.exists()) {
                        val writtenTotalXp = verifySnapshot.getLong("totalXp") ?: 0L
                        val writtenAttempts = verifySnapshot.getLong("totalAttempts") ?: 0L
                        val writtenAccuracy = verifySnapshot.getDouble("averageAccuracy") ?: 0.0
                        val writtenCorrect = verifySnapshot.getLong("totalCorrectAnswers") ?: 0L
                        val writtenQuestions = verifySnapshot.getLong("totalQuestions") ?: 0L
                        
                        android.util.Log.d("FirestoreSyncService", "✅ Verified write:")
                        android.util.Log.d("FirestoreSyncService", "   - totalXp: $writtenTotalXp (expected: ${profileData["totalXp"]})")
                        android.util.Log.d("FirestoreSyncService", "   - totalAttempts: $writtenAttempts (expected: ${profileData["totalAttempts"]})")
                        android.util.Log.d("FirestoreSyncService", "   - averageAccuracy: $writtenAccuracy (expected: ${profileData["averageAccuracy"]})")
                        android.util.Log.d("FirestoreSyncService", "   - totalCorrectAnswers: $writtenCorrect (expected: ${profileData["totalCorrectAnswers"]})")
                        android.util.Log.d("FirestoreSyncService", "   - totalQuestions: $writtenQuestions (expected: ${profileData["totalQuestions"]})")
                        
                        // Verificar si hay discrepancias (comparar correctamente los tipos)
                        val expectedTotalXp = (profileData["totalXp"] as? Long ?: 0L)
                        val expectedAttempts = (profileData["totalAttempts"] as? Int ?: 0).toLong()
                        val expectedAccuracy = (profileData["averageAccuracy"] as? Float ?: 0f).toDouble()
                        
                        val hasDiscrepancy = writtenTotalXp != expectedTotalXp ||
                            writtenAttempts != expectedAttempts ||
                            Math.abs(writtenAccuracy - expectedAccuracy) > 0.01
                        
                        if (hasDiscrepancy) {
                            android.util.Log.e("FirestoreSyncService", "❌ DISCREPANCY DETECTED: Written values don't match expected values!")
                            android.util.Log.e("FirestoreSyncService", "   totalXp: written=$writtenTotalXp, expected=$expectedTotalXp")
                            android.util.Log.e("FirestoreSyncService", "   totalAttempts: written=$writtenAttempts, expected=$expectedAttempts")
                            android.util.Log.e("FirestoreSyncService", "   averageAccuracy: written=$writtenAccuracy, expected=$expectedAccuracy")
                        } else {
                            android.util.Log.d("FirestoreSyncService", "✅ All values match correctly")
                        }
                    } else {
                        android.util.Log.e("FirestoreSyncService", "❌ ERROR: Document doesn't exist after write!")
                    }
                    
                    android.util.Log.d("FirestoreSyncService", "✅✅✅ Successfully synced user profile ${profile.uid} to Firestore")
                    true
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreSyncService", "❌ ERROR writing to Firestore", e)
                    android.util.Log.e("FirestoreSyncService", "Error type: ${e.javaClass.simpleName}")
                    android.util.Log.e("FirestoreSyncService", "Error message: ${e.message}")
                    e.printStackTrace()
                    false
                }
            } else {
                // El remoto es más reciente y el perfil no está PENDING, no sobrescribir
                android.util.Log.w("FirestoreSyncService", "⚠️ Remote profile is newer and local is not PENDING, skipping sync for ${profile.uid}")
                android.util.Log.w("FirestoreSyncService", "⚠️ Remote updatedAtLocal: $remoteUpdatedAtLocal, Local updatedAtLocal: ${profile.updatedAtLocal}")
                android.util.Log.d("FirestoreSyncService", "Note: Use fetchProfileFromFirestore() to update local profile from remote")
                // Si el perfil está PENDING, forzar sincronización de todos modos
                if (profile.syncState == SyncState.PENDING) {
                    android.util.Log.d("FirestoreSyncService", "⚠️ But profile is PENDING, forcing sync anyway")
                    // Usar las métricas de Room, no calcularlas
                    val stats = RankingStats(
                        accuracy = profile.averageAccuracy,
                        totalAttempts = profile.totalAttempts,
                        totalCorrectAnswers = profile.totalCorrectAnswers,
                        totalQuestions = profile.totalQuestions
                    )
                    android.util.Log.d("FirestoreSyncService", "📊 Using metrics from Room (PENDING override):")
                    android.util.Log.d("FirestoreSyncService", "   - totalAttempts: ${stats.totalAttempts}")
                    android.util.Log.d("FirestoreSyncService", "   - totalCorrectAnswers: ${stats.totalCorrectAnswers}")
                    android.util.Log.d("FirestoreSyncService", "   - totalQuestions: ${stats.totalQuestions}")
                    android.util.Log.d("FirestoreSyncService", "   - averageAccuracy: ${stats.accuracy}%")
                    val schoolCode = profile.ugelCode?.takeIf { it.isNotBlank() } ?: ""
                    val userEmail = if (remoteSnapshot.exists()) {
                        remoteSnapshot.getString("email")?.takeIf { it.isNotBlank() }
                    } else {
                        null
                    } ?: run {
                        val currentUser = firebaseAuth.currentUser
                        if (currentUser?.uid == profile.uid) {
                            currentUser.email ?: ""
                        } else {
                            ""
                        }
                    }
                    
                    val profileData = mapOf(
                        "uid" to profile.uid,
                        "displayName" to profile.displayName,
                        "email" to userEmail,
                        "photoUrl" to profile.photoUrl,
                        "schoolCode" to schoolCode,
                        "ugelCode" to profile.ugelCode,
                        "coins" to profile.coins,
                        "totalXp" to profile.xp.toLong(),
                        "totalScore" to profile.xp.toLong(),  // Alias de totalXp para compatibilidad
                        "averageAccuracy" to stats.accuracy,
                        "totalAttempts" to stats.totalAttempts,
                        "totalCorrectAnswers" to stats.totalCorrectAnswers,
                        "totalQuestions" to stats.totalQuestions,
                        "selectedCosmeticId" to profile.selectedCosmeticId,
                        "updatedAtLocal" to profile.updatedAtLocal,  // Asegurar que esté presente
                        "lastSyncedAt" to System.currentTimeMillis()
                    )
                    
                    profileRef.set(profileData, SetOptions.merge()).await()
                    android.util.Log.d("FirestoreSyncService", "✅ Forced sync completed for PENDING profile ${profile.uid}")
                    true
                } else {
                    // No está PENDING y remoto es más reciente, no sincronizar
                    true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSyncService", "❌ Error syncing user profile ${profile.uid}", e)
            android.util.Log.e("FirestoreSyncService", "Error message: ${e.message}")
            android.util.Log.e("FirestoreSyncService", "Error cause: ${e.cause?.message}")
            e.printStackTrace()
            false
        }
    }
}

