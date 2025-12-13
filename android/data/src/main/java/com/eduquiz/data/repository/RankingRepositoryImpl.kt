package com.eduquiz.data.repository

import com.eduquiz.domain.ranking.LeaderboardEntry
import com.eduquiz.domain.ranking.RankingError
import com.eduquiz.domain.ranking.RankingRepository
import com.eduquiz.domain.ranking.RankingResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class RankingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : RankingRepository {

    /**
     * Convierte un error de Firestore a RankingError.
     */
    private fun mapFirestoreError(error: Exception): RankingError {
        return when (error) {
            is FirebaseFirestoreException -> {
                when (error.code) {
                    FirebaseFirestoreException.Code.NOT_FOUND -> 
                        RankingError.EmptyRanking
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> 
                        RankingError.PermissionDenied()
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> 
                        RankingError.NetworkError()
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION -> {
                        // Error de índice faltante
                        if (error.message?.contains("index") == true) {
                            RankingError.IndexMissing()
                        } else {
                            RankingError.FirestoreError(error.code.toString(), error.message ?: "Error de Firestore")
                        }
                    }
                    else -> 
                        RankingError.FirestoreError(error.code.toString(), error.message ?: "Error desconocido de Firestore")
                }
            }
            else -> RankingError.UnknownError(error.message ?: "Error desconocido")
        }
    }

    /**
     * Convierte un DocumentSnapshot a LeaderboardEntry.
     */
    private fun DocumentSnapshot.toLeaderboardEntry(): LeaderboardEntry? {
        val uid = id
        val displayName = getString("displayName") ?: ""
        val photoUrl = getString("photoUrl")
        
        // Usar totalScore o totalXp o xp (compatibilidad)
        val totalScore = getLong("totalScore")?.toInt() 
            ?: getLong("totalXp")?.toInt() 
            ?: getLong("xp")?.toInt() 
            ?: 0
        
        // Usar averageAccuracy (nuevo nombre) o accuracy (compatibilidad)
        val accuracy = getDouble("averageAccuracy")?.toFloat() 
            ?: getDouble("accuracy")?.toFloat() 
            ?: 0f
        
        // Usar totalAttempts (nuevo nombre) o examsCompleted (compatibilidad)
        val examsCompleted = getLong("totalAttempts")?.toInt() 
            ?: getLong("examsCompleted")?.toInt() 
            ?: 0
        
        return LeaderboardEntry(
            uid = uid,
            displayName = displayName,
            photoUrl = photoUrl,
            totalScore = totalScore,
            accuracy = accuracy,
            examsCompleted = examsCompleted
        )
    }

    /**
     * Convierte un Map a LeaderboardEntry (para estructura de aula).
     */
    private fun Map<*, *>.toLeaderboardEntry(): LeaderboardEntry? {
        val uid = get("uid") as? String ?: return null
        val displayName = get("displayName") as? String ?: ""
        val photoUrl = get("photoUrl") as? String
        
        val totalScore = (get("totalScore") as? Number)?.toInt() 
            ?: (get("totalXp") as? Number)?.toInt() 
            ?: (get("xp") as? Number)?.toInt() 
            ?: 0
        
        val accuracy = (get("averageAccuracy") as? Number)?.toFloat() 
            ?: (get("accuracy") as? Number)?.toFloat() 
            ?: 0f
        
        val examsCompleted = (get("totalAttempts") as? Number)?.toInt() 
            ?: (get("examsCompleted") as? Number)?.toInt() 
            ?: 0
        
        return LeaderboardEntry(
            uid = uid,
            displayName = displayName,
            photoUrl = photoUrl,
            totalScore = totalScore,
            accuracy = accuracy,
            examsCompleted = examsCompleted
        )
    }

    override fun observeClassroomLeaderboard(
        schoolId: String,
        classroomId: String
    ): Flow<RankingResult<List<LeaderboardEntry>>> = callbackFlow {
        val leaderboardRef = firestore
            .collection("schools")
            .document(schoolId)
            .collection("classrooms")
            .document(classroomId)
            .collection("leaderboard")
            .document("current")

        val registration = leaderboardRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(RankingResult.Error(mapFirestoreError(error)))
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                trySend(RankingResult.Success(emptyList()))
                return@addSnapshotListener
            }

            val top = snapshot.get("top") as? List<*>
            val entries = top
                ?.mapNotNull { item ->
                    (item as? Map<*, *>)?.toLeaderboardEntry()
                }
                ?: emptyList()

            if (entries.isEmpty()) {
                trySend(RankingResult.Success(emptyList()))
            } else {
                trySend(RankingResult.Success(entries))
            }
        }

        awaitClose { registration.remove() }
    }

    override fun observeSchoolLeaderboard(schoolCode: String): Flow<RankingResult<List<LeaderboardEntry>>> = callbackFlow {
        val usersRef = firestore
            .collection("users")
            .whereEqualTo("schoolCode", schoolCode)
            .orderBy("totalScore", Query.Direction.DESCENDING)
            .limit(100)

        val registration = usersRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(RankingResult.Error(mapFirestoreError(error)))
                return@addSnapshotListener
            }

            val entries = snapshot?.documents
                ?.mapNotNull { it.toLeaderboardEntry() }
                ?: emptyList()

            if (entries.isEmpty()) {
                trySend(RankingResult.Success(emptyList()))
            } else {
                trySend(RankingResult.Success(entries))
            }
        }

        awaitClose { registration.remove() }
    }

    override fun observeNationalLeaderboard(): Flow<RankingResult<List<LeaderboardEntry>>> = callbackFlow {
        val usersRef = firestore
            .collection("users")
            .orderBy("totalScore", Query.Direction.DESCENDING)
            .limit(100)

        val registration = usersRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(RankingResult.Error(mapFirestoreError(error)))
                return@addSnapshotListener
            }

            val entries = snapshot?.documents
                ?.mapNotNull { it.toLeaderboardEntry() }
                ?: emptyList()

            if (entries.isEmpty()) {
                trySend(RankingResult.Success(emptyList()))
            } else {
                trySend(RankingResult.Success(entries))
            }
        }

        awaitClose { registration.remove() }
    }

    override suspend fun loadMoreSchoolLeaderboard(
        schoolCode: String,
        lastScore: Int
    ): RankingResult<List<LeaderboardEntry>> {
        return try {
            // Para paginación, usamos whereLessThan con el último score
            // Esto requiere un índice compuesto: schoolCode (Asc) + totalScore (Desc)
            val query = firestore
                .collection("users")
                .whereEqualTo("schoolCode", schoolCode)
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .whereLessThan("totalScore", lastScore)
                .limit(100)

            val snapshot = query.get().await()
            val entries = snapshot.documents.mapNotNull { it.toLeaderboardEntry() }
            
            RankingResult.Success(entries)
        } catch (e: Exception) {
            // Si falla por índice faltante, retornar error específico
            if (e.message?.contains("index") == true) {
                RankingResult.Error(RankingError.IndexMissing())
            } else {
                RankingResult.Error(mapFirestoreError(e))
            }
        }
    }

    override suspend fun loadMoreNationalLeaderboard(
        lastScore: Int
    ): RankingResult<List<LeaderboardEntry>> {
        return try {
            // Para ranking nacional, simplemente usamos whereLessThan con el último score
            val query = firestore
                .collection("users")
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .whereLessThan("totalScore", lastScore)
                .limit(100)

            val snapshot = query.get().await()
            val entries = snapshot.documents.mapNotNull { it.toLeaderboardEntry() }
            
            RankingResult.Success(entries)
        } catch (e: Exception) {
            RankingResult.Error(mapFirestoreError(e))
        }
    }

    override suspend fun calculateUserPosition(
        uid: String,
        schoolCode: String?
    ): RankingResult<Int> {
        return try {
            // Primero obtener el score del usuario
            val userDoc = firestore.collection("users").document(uid).get().await()
            if (!userDoc.exists()) {
                return RankingResult.Error(RankingError.UnknownError("Usuario no encontrado"))
            }

            val userScore = userDoc.getLong("totalScore")?.toInt() 
                ?: userDoc.getLong("totalXp")?.toInt() 
                ?: userDoc.getLong("xp")?.toInt() 
                ?: 0

            // Contar cuántos usuarios tienen más score
            val query = if (schoolCode != null) {
                firestore
                    .collection("users")
                    .whereEqualTo("schoolCode", schoolCode)
                    .whereGreaterThan("totalScore", userScore)
            } else {
                firestore
                    .collection("users")
                    .whereGreaterThan("totalScore", userScore)
            }

            val countSnapshot = query.get().await()
            val position = countSnapshot.size() + 1

            RankingResult.Success(position)
        } catch (e: Exception) {
            RankingResult.Error(mapFirestoreError(e))
        }
    }
}
