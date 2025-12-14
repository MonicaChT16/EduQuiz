package com.eduquiz.domain.profile

data class UserStats(
    val totalXp: Long = 0L,
    val totalScore: Int = 0,
    val totalAttempts: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val totalQuestions: Int = 0
) {
    val efficiency: Float
        get() = if (totalQuestions > 0) {
            (totalCorrectAnswers.toFloat() / totalQuestions) * 100f
        } else {
            0f
        }
    
    val incorrectAnswers: Int
        get() = totalQuestions - totalCorrectAnswers
}
