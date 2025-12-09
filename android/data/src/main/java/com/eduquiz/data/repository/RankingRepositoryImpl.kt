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
    private val firestore: FirebaseFirestore,
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
                        val totalScore = (map["totalScore"] as? Number)?.toInt() ?: 0
                        LeaderboardEntry(
                            uid = uid,
                            displayName = displayName,
                            photoUrl = photoUrl,
                            totalScore = totalScore
                        )
                    }
                }
                ?: emptyList()

            trySend(entries)
        }

        awaitClose { registration.remove() }
    }
}


