package com.eduquiz.domain.ranking

data class LeaderboardEntry(
    val uid: String,
    val displayName: String,
    val photoUrl: String?,
    val totalScore: Int, // Puntaje Total (XP)
    val accuracy: Float, // Precisión Promedio (%): 0.0 a 100.0
    val examsCompleted: Int, // Exámenes Completados
    val selectedCosmeticId: String? = null // Cosmético equipado
)

/**
 * Resultado de una operación de ranking que puede contener datos o un error.
 */
sealed class RankingResult<out T> {
    data class Success<T>(val data: T) : RankingResult<T>()
    data class Error(val error: RankingError) : RankingResult<Nothing>()
}


