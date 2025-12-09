package com.eduquiz.data.remote

import com.eduquiz.data.db.ExamAnswerEntity
import com.eduquiz.data.db.ExamAttemptEntity
import com.eduquiz.data.db.UserProfileEntity
import com.eduquiz.domain.profile.SyncState
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
    private val firestore: FirebaseFirestore
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
     * Sincroniza el perfil del usuario a Firestore.
     * Ruta: users/{uid}
     * 
     * Regla: última escritura gana - comparar updatedAtLocal antes de escribir.
     * 
     * @return true si la sincronización fue exitosa, false en caso contrario
     */
    suspend fun syncUserProfile(profile: UserProfileEntity): Boolean {
        return try {
            val profileRef = firestore.collection("users").document(profile.uid)

            // Leer el perfil remoto para comparar timestamps
            val remoteSnapshot = profileRef.get().await()
            val remoteUpdatedAt = if (remoteSnapshot.exists()) {
                remoteSnapshot.getLong("updatedAtLocal") ?: 0L
            } else {
                0L // Si no existe, el local es más reciente
            }

            // Solo escribir si el local es más reciente o igual (última escritura gana)
            if (profile.updatedAtLocal >= remoteUpdatedAt) {
                val profileData = mapOf(
                    "uid" to profile.uid,
                    "displayName" to profile.displayName,
                    "photoUrl" to profile.photoUrl,
                    "schoolId" to profile.schoolId,
                    "classroomId" to profile.classroomId,
                    "coins" to profile.coins,
                    "xp" to profile.xp,
                    "selectedCosmeticId" to profile.selectedCosmeticId,
                    "updatedAtLocal" to profile.updatedAtLocal,
                    "syncState" to SyncState.SYNCED,
                    "lastSyncedAt" to System.currentTimeMillis()
                )
                profileRef.set(profileData, SetOptions.merge()).await()
                true
            } else {
                // El remoto es más reciente, no sobrescribir
                android.util.Log.d("FirestoreSyncService", "Remote profile is newer (remote: $remoteUpdatedAt, local: ${profile.updatedAtLocal}), skipping sync for ${profile.uid}")
                // Aún así marcamos como SYNCED porque el remoto ya tiene la versión más reciente
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSyncService", "Error syncing user profile ${profile.uid}", e)
            false
        }
    }
}

