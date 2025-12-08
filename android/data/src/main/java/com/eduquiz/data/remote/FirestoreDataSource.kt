package com.eduquiz.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun fetchCollection(name: String): List<Map<String, Any?>> {
        val snapshot = firestore.collection(name).get().await()
        return snapshot.documents.map { it.data ?: emptyMap() }
    }
}
