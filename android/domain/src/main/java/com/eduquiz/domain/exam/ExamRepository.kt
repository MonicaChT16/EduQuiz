package com.eduquiz.domain.exam

interface ExamRepository {
    suspend fun createAttempt(attempt: ExamAttempt)
    suspend fun upsertAnswer(answer: ExamAnswer)
    suspend fun finishAttempt(
        attemptId: String,
        finishedAtLocal: Long,
        status: String,
        scoreRaw: Int,
        scoreValidated: Int?,
        syncState: String,
    )

    suspend fun getAttempts(uid: String): List<ExamAttempt>
    suspend fun getAnswersForAttempt(attemptId: String): List<ExamAnswer>
}
