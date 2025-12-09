package com.eduquiz.data.repository

import com.eduquiz.data.db.ContentDao
import com.eduquiz.data.db.ExamDao
import com.eduquiz.domain.exam.ExamAnswer
import com.eduquiz.domain.exam.ExamAttempt
import com.eduquiz.domain.exam.ExamOrigin
import com.eduquiz.domain.exam.ExamRepository
import com.eduquiz.domain.exam.ExamStatus
import com.eduquiz.domain.profile.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class ExamRepositoryImpl @Inject constructor(
    private val examDao: ExamDao,
    private val contentDao: ContentDao,
) : ExamRepository {

    override suspend fun startAttempt(
        uid: String,
        packId: String,
        startedAtLocal: Long,
        durationMs: Long
    ): String {
        val attemptId = "attempt-${UUID.randomUUID()}"
        val attempt = ExamAttempt(
            attemptId = attemptId,
            uid = uid,
            packId = packId,
            startedAtLocal = startedAtLocal,
            finishedAtLocal = null,
            durationMs = durationMs,
            status = ExamStatus.IN_PROGRESS,
            scoreRaw = 0,
            scoreValidated = null,
            origin = ExamOrigin.OFFLINE,
            syncState = SyncState.PENDING
        )
        examDao.insertAttempt(attempt.toEntity())
        return attemptId
    }

    override suspend fun submitAnswer(
        attemptId: String,
        questionId: String,
        optionId: String,
        timeSpentMs: Long
    ) {
        // Obtener correctOptionId desde question_entity
        val correctOptionId = examDao.getCorrectOptionId(questionId)
            ?: throw IllegalStateException("Question $questionId not found")
        val isCorrect = optionId == correctOptionId

        val answer = ExamAnswer(
            attemptId = attemptId,
            questionId = questionId,
            selectedOptionId = optionId,
            isCorrect = isCorrect,
            timeSpentMs = timeSpentMs
        )
        examDao.upsertAnswer(answer.toEntity())
    }

    override suspend fun finishAttempt(
        attemptId: String,
        finishedAtLocal: Long,
        status: String
    ) {
        // Calcular scoreRaw desde las respuestas guardadas
        val answers = examDao.getAnswers(attemptId)
        val scoreRaw = answers.count { it.isCorrect }

        // Obtener el intento para verificar que existe
        val attempt = examDao.getAttemptById(attemptId)
            ?: throw IllegalStateException("Attempt $attemptId not found")

        examDao.finishAttempt(
            attemptId = attemptId,
            finishedAtLocal = finishedAtLocal,
            status = status,
            scoreRaw = scoreRaw,
            scoreValidated = scoreRaw, // Por ahora, scoreValidated = scoreRaw
            syncState = SyncState.PENDING
        )
    }

    override fun observeAttempts(uid: String): Flow<List<ExamAttempt>> =
        examDao.observeAttempts(uid).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAttempts(uid: String): List<ExamAttempt> =
        examDao.getAttempts(uid).map { it.toDomain() }

    override suspend fun getAnswersForAttempt(attemptId: String): List<ExamAnswer> =
        examDao.getAnswers(attemptId).map { it.toDomain() }

    // Métodos legacy - mantener para compatibilidad
    override suspend fun createAttempt(attempt: ExamAttempt) {
        examDao.insertAttempt(attempt.toEntity())
    }

    override suspend fun upsertAnswer(answer: ExamAnswer) {
        examDao.upsertAnswer(answer.toEntity())
    }
}
