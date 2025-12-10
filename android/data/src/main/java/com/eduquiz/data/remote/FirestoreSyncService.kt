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
            android.util.Log.e("FirestoreSyncService", "Error syncing exam attempt ${attempt.attemptId}", e)
            false
        }
    }

    /**
     * Calcula las estadísticas de ranking del usuario.
     * Retorna: (accuracy, totalAttempts, totalCorrectAnswers, totalQuestions)
     */
    private suspend fun calculateRankingStats(uid: String): RankingStats {
        return try {
            val attempts = examRepository.getAttempts(uid)
            val completedAttempts = attempts.filter { 
                it.status == ExamStatus.COMPLETED || it.status == ExamStatus.AUTO_SUBMIT 
            }
            val totalAttempts = completedAttempts.size
            
            var totalCorrect = 0
            var totalQuestions = 0
            
            completedAttempts.forEach { attempt ->
                val answers = examRepository.getAnswersForAttempt(attempt.attemptId)
                totalQuestions += answers.size
                totalCorrect += answers.count { it.isCorrect }
            }
            
            val accuracy = if (totalQuestions > 0) {
                (totalCorrect.toFloat() / totalQuestions.toFloat()) * 100f
            } else {
                0f
            }
            
            RankingStats(
                accuracy = accuracy,
                totalAttempts = totalAttempts,
                totalCorrectAnswers = totalCorrect,
                totalQuestions = totalQuestions
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSyncService", "Error calculating ranking stats for $uid", e)
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
     * - Datos de colegio: schoolCode (del schoolId), schoolId, classroomId
     * - Métricas de ranking: totalXp, averageAccuracy, totalAttempts, totalCorrectAnswers, totalQuestions
     * - Otros: coins, selectedCosmeticId, timestamps
     * 
     * @return true si la sincronización fue exitosa, false en caso contrario
     */
    suspend fun syncUserProfile(profile: UserProfileEntity): Boolean {
        return try {
            android.util.Log.d("FirestoreSyncService", "Starting sync for user profile: ${profile.uid}")
            val profileRef = firestore.collection("users").document(profile.uid)

            // Leer el perfil remoto para comparar timestamps
            android.util.Log.d("FirestoreSyncService", "Reading remote profile for ${profile.uid}")
            val remoteSnapshot = profileRef.get().await()
            val remoteUpdatedAt = if (remoteSnapshot.exists()) {
                remoteSnapshot.getLong("updatedAtLocal") ?: 0L
            } else {
                0L // Si no existe, el local es más reciente
            }
            android.util.Log.d("FirestoreSyncService", "Remote updatedAt: $remoteUpdatedAt, Local updatedAt: ${profile.updatedAtLocal}")

            // Solo escribir si el local es más reciente o igual (última escritura gana)
            if (profile.updatedAtLocal >= remoteUpdatedAt) {
                android.util.Log.d("FirestoreSyncService", "Local is newer or equal, proceeding with sync")
                
                // Calcular estadísticas de ranking
                android.util.Log.d("FirestoreSyncService", "Calculating ranking stats for ${profile.uid}")
                val stats = calculateRankingStats(profile.uid)
                android.util.Log.d("FirestoreSyncService", "Stats calculated: accuracy=${stats.accuracy}, attempts=${stats.totalAttempts}, correct=${stats.totalCorrectAnswers}, questions=${stats.totalQuestions}")
                
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
                
                // schoolCode: usar schoolId como código de colegio (el usuario lo ingresa manualmente)
                // Si schoolId está vacío, schoolCode también estará vacío
                val schoolCode = profile.schoolId.takeIf { it.isNotBlank() } ?: ""
                
                val profileData = mapOf(
                    // Datos básicos del usuario
                    "uid" to profile.uid,
                    "displayName" to profile.displayName,
                    "email" to userEmail,
                    "photoUrl" to profile.photoUrl,
                    
                    // Datos de colegio
                    "schoolCode" to schoolCode,  // Código de colegio/UGEL (ingresado manualmente)
                    "schoolId" to profile.schoolId,
                    "classroomId" to profile.classroomId,
                    
                    // Monedas y XP
                    "coins" to profile.coins,
                    "xp" to profile.xp,
                    
                    // Métricas de ranking (pre-calculadas para consultas rápidas)
                    "totalXp" to profile.xp.toLong(),  // XP total acumulado
                    "totalScore" to profile.xp.toInt(), // Compatibilidad con código existente
                    "averageAccuracy" to stats.accuracy,  // Promedio de aciertos (%)
                    "totalAttempts" to stats.totalAttempts,  // Total de exámenes completados
                    "totalCorrectAnswers" to stats.totalCorrectAnswers,  // Total de respuestas correctas
                    "totalQuestions" to stats.totalQuestions,  // Total de preguntas respondidas
                    
                    // Otros campos
                    "selectedCosmeticId" to profile.selectedCosmeticId,
                    "updatedAtLocal" to profile.updatedAtLocal,
                    "syncState" to SyncState.SYNCED,
                    "lastSyncedAt" to System.currentTimeMillis()
                )
                
                android.util.Log.d("FirestoreSyncService", "Writing profile data to Firestore: ${profileData.keys.joinToString()}")
                profileRef.set(profileData, SetOptions.merge()).await()
                android.util.Log.d("FirestoreSyncService", "✅ Successfully synced user profile ${profile.uid} to Firestore")
                true
            } else {
                // El remoto es más reciente, no sobrescribir
                android.util.Log.d("FirestoreSyncService", "Remote profile is newer (remote: $remoteUpdatedAt, local: ${profile.updatedAtLocal}), skipping sync for ${profile.uid}")
                // Aún así marcamos como SYNCED porque el remoto ya tiene la versión más reciente
                true
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

