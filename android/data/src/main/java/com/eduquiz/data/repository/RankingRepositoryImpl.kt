package com.eduquiz.data.repository

import com.eduquiz.domain.ranking.LeaderboardEntry
import com.eduquiz.domain.ranking.RankingRepository
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class RankingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : RankingRepository {

    override fun observeClassroomLeaderboard(
        schoolId: String,
        classroomId: String
    ): Flow<List<LeaderboardEntry>> = callbackFlow {
        val leaderboardRef = firestore
            .collection("schools")
            .document(schoolId)
            .collection("classrooms")
            .document(classroomId)
            .collection("leaderboard")
            .document("current")

        val registration = leaderboardRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val top = snapshot?.get("top") as? List<*>
            val entries = top
                ?.mapNotNull { item ->
                    (item as? Map<*, *>)?.let { map ->
                        val uid = map["uid"] as? String ?: return@let null
                        val displayName = map["displayName"] as? String ?: ""
                        val photoUrl = map["photoUrl"] as? String
                        // Usar totalScore o totalXp o xp (compatibilidad)
                        val totalScore = (map["totalScore"] as? Number)?.toInt() 
                            ?: (map["totalXp"] as? Number)?.toInt() 
                            ?: (map["xp"] as? Number)?.toInt() 
                            ?: 0
                        // Usar averageAccuracy (nuevo nombre) o accuracy (compatibilidad)
                        val accuracy = (map["averageAccuracy"] as? Number)?.toFloat() 
                            ?: (map["accuracy"] as? Number)?.toFloat() 
                            ?: 0f
                        // Usar totalAttempts (nuevo nombre) o examsCompleted (compatibilidad)
                        val examsCompleted = (map["totalAttempts"] as? Number)?.toInt() 
                            ?: (map["examsCompleted"] as? Number)?.toInt() 
                            ?: 0
                        LeaderboardEntry(
                            uid = uid,
                            displayName = displayName,
                            photoUrl = photoUrl,
                            totalScore = totalScore,
                            accuracy = accuracy,
                            examsCompleted = examsCompleted
                        )
                    }
                }
                ?: emptyList()

            trySend(entries)
        }

        awaitClose { registration.remove() }
    }

    override fun observeSchoolLeaderboard(schoolCode: String): Flow<List<LeaderboardEntry>> = callbackFlow {
        val usersRef = firestore
            .collection("users")
            .whereEqualTo("schoolCode", schoolCode) // Usar schoolCode (camelCase) como en FirestoreSyncService
            .orderBy("totalScore", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)

        val registration = usersRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val entries = snapshot?.documents
                ?.mapNotNull { doc ->
                    val uid = doc.id
                    val displayName = doc.getString("displayName") ?: ""
                    val photoUrl = doc.getString("photoUrl")
                    // Usar totalScore o totalXp o xp (compatibilidad)
                    val totalScore = doc.getLong("totalScore")?.toInt() 
                        ?: doc.getLong("totalXp")?.toInt() 
                        ?: doc.getLong("xp")?.toInt() 
                        ?: 0
                    // Usar averageAccuracy (nuevo nombre) o accuracy (compatibilidad)
                    val accuracy = doc.getDouble("averageAccuracy")?.toFloat() 
                        ?: doc.getDouble("accuracy")?.toFloat() 
                        ?: 0f
                    // Usar totalAttempts (nuevo nombre) o examsCompleted (compatibilidad)
                    val examsCompleted = doc.getLong("totalAttempts")?.toInt() 
                        ?: doc.getLong("examsCompleted")?.toInt() 
                        ?: 0
                    
                    LeaderboardEntry(
                        uid = uid,
                        displayName = displayName,
                        photoUrl = photoUrl,
                        totalScore = totalScore,
                        accuracy = accuracy,
                        examsCompleted = examsCompleted
                    )
                }
                ?: emptyList()

            trySend(entries)
        }

        awaitClose { registration.remove() }
    }

    override fun observeNationalLeaderboard(): Flow<List<LeaderboardEntry>> = callbackFlow {
        val usersRef = firestore
            .collection("users")
            .orderBy("totalScore", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100) // Top 100 nacional

        val registration = usersRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val entries = snapshot?.documents
                ?.mapNotNull { doc ->
                    val uid = doc.id
                    val displayName = doc.getString("displayName") ?: ""
                    val photoUrl = doc.getString("photoUrl")
                    // Usar totalScore o totalXp o xp (compatibilidad)
                    val totalScore = doc.getLong("totalScore")?.toInt() 
                        ?: doc.getLong("totalXp")?.toInt() 
                        ?: doc.getLong("xp")?.toInt() 
                        ?: 0
                    // Usar averageAccuracy (nuevo nombre) o accuracy (compatibilidad)
                    val accuracy = doc.getDouble("averageAccuracy")?.toFloat() 
                        ?: doc.getDouble("accuracy")?.toFloat() 
                        ?: 0f
                    // Usar totalAttempts (nuevo nombre) o examsCompleted (compatibilidad)
                    val examsCompleted = doc.getLong("totalAttempts")?.toInt() 
                        ?: doc.getLong("examsCompleted")?.toInt() 
                        ?: 0
                    
                    LeaderboardEntry(
                        uid = uid,
                        displayName = displayName,
                        photoUrl = photoUrl,
                        totalScore = totalScore,
                        accuracy = accuracy,
                        examsCompleted = examsCompleted
                    )
                }
                ?: emptyList()

            trySend(entries)
        }

        awaitClose { registration.remove() }
    }
}


