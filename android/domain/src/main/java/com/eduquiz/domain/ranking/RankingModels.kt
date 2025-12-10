package com.eduquiz.domain.ranking

data class LeaderboardEntry(
    val uid: String,
    val displayName: String,
    val photoUrl: String?,
    val totalScore: Int, // Puntaje Total (XP)
    val accuracy: Float, // Precisión Promedio (%): 0.0 a 100.0
    val examsCompleted: Int // Exámenes Completados
)


