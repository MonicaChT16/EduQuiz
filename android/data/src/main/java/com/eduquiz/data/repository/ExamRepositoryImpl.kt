package com.eduquiz.data.repository

import com.eduquiz.data.db.ExamDao
import com.eduquiz.domain.exam.ExamAnswer
import com.eduquiz.domain.exam.ExamAttempt
import com.eduquiz.domain.exam.ExamRepository
import javax.inject.Inject

class ExamRepositoryImpl @Inject constructor(
    private val examDao: ExamDao,
) : ExamRepository {

    override suspend fun createAttempt(attempt: ExamAttempt) {
        examDao.insertAttempt(attempt.toEntity())
    }

    override suspend fun upsertAnswer(answer: ExamAnswer) {
        examDao.upsertAnswer(answer.toEntity())
    }

    override suspend fun finishAttempt(
        attemptId: String,
        finishedAtLocal: Long,
        status: String,
        scoreRaw: Int,
        scoreValidated: Int?,
        syncState: String
    ) {
        examDao.finishAttempt(
            attemptId = attemptId,
            finishedAtLocal = finishedAtLocal,
            status = status,
            scoreRaw = scoreRaw,
            scoreValidated = scoreValidated,
            syncState = syncState
        )
    }

    override suspend fun getAttempts(uid: String): List<ExamAttempt> =
        examDao.getAttempts(uid).map { it.toDomain() }

    override suspend fun getAnswersForAttempt(attemptId: String): List<ExamAnswer> =
        examDao.getAnswers(attemptId).map { it.toDomain() }
}
