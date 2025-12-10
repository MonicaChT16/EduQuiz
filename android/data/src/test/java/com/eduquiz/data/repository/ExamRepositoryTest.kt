package com.eduquiz.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eduquiz.data.db.AppDatabase
import com.eduquiz.data.db.ContentDao
import com.eduquiz.data.db.ExamDao
import com.eduquiz.data.db.ExamAnswerEntity
import com.eduquiz.data.db.ExamAttemptEntity
import com.eduquiz.data.db.OptionEntity
import com.eduquiz.data.db.PackDao
import com.eduquiz.data.db.PackEntity
import com.eduquiz.data.db.QuestionEntity
import com.eduquiz.data.db.TextEntity
import com.eduquiz.domain.exam.ExamStatus
import com.eduquiz.domain.profile.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExamRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var examDao: ExamDao
    private lateinit var contentDao: ContentDao
    private lateinit var packDao: PackDao
    private lateinit var repository: ExamRepositoryImpl

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        examDao = database.examDao()
        contentDao = database.contentDao()
        packDao = database.packDao()
        repository = ExamRepositoryImpl(examDao, contentDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun startAttempt_createsAttemptInProgress() = runTest {
        val uid = "user-1"
        val packId = "pack-1"
        val startedAt = System.currentTimeMillis()
        val duration = 20 * 60 * 1000L

        // Setup pack
        packDao.insert(PackEntity(packId, "Semana 1", "ACTIVE", 0L, 0L))

        val attemptId = repository.startAttempt(uid, packId, null, startedAt, duration)

        assertNotNull(attemptId)
        val attempts = examDao.getAttempts(uid)
        assertEquals(1, attempts.size)
        val attempt = attempts[0]
        assertEquals(ExamStatus.IN_PROGRESS, attempt.status)
        assertEquals(packId, attempt.packId)
        assertEquals(uid, attempt.uid)
        assertEquals(0, attempt.scoreRaw)
        assertEquals(SyncState.PENDING, attempt.syncState)
    }

    @Test
    fun submitAnswer_upsertsAnswerWithCorrectCalculation() = runTest {
        val uid = "user-1"
        val packId = "pack-1"
        val questionId = "q-1"
        val correctOptionId = "A"
        val wrongOptionId = "B"

        // Setup pack and question
        packDao.insert(PackEntity(packId, "Semana 1", "ACTIVE", 0L, 0L))
        contentDao.insertQuestions(
            listOf(
                QuestionEntity(
                    questionId = questionId,
                    packId = packId,
                    textId = "text-1",
                    prompt = "Pregunta?",
                    correctOptionId = correctOptionId,
                    difficulty = 1,
                    explanationText = null,
                    explanationStatus = "NONE"
                )
            )
        )

        val attemptId = repository.startAttempt(uid, packId, null, System.currentTimeMillis(), 20 * 60 * 1000L)

        // Submit correct answer
        repository.submitAnswer(attemptId, questionId, correctOptionId, 5000L)

        val answers = examDao.getAnswers(attemptId)
        assertEquals(1, answers.size)
        val answer = answers[0]
        assertTrue(answer.isCorrect)
        assertEquals(correctOptionId, answer.selectedOptionId)
        assertEquals(5000L, answer.timeSpentMs)

        // Submit wrong answer (should update)
        repository.submitAnswer(attemptId, questionId, wrongOptionId, 10000L)

        val updatedAnswers = examDao.getAnswers(attemptId)
        assertEquals(1, updatedAnswers.size)
        val updatedAnswer = updatedAnswers[0]
        assertTrue(!updatedAnswer.isCorrect)
        assertEquals(wrongOptionId, updatedAnswer.selectedOptionId)
        assertEquals(10000L, updatedAnswer.timeSpentMs)
    }

    @Test
    fun finishAttempt_calculatesScoreFromAnswers() = runTest {
        val uid = "user-1"
        val packId = "pack-1"
        val question1Id = "q-1"
        val question2Id = "q-2"
        val correctOption1 = "A"
        val correctOption2 = "B"

        // Setup pack and questions
        packDao.insert(PackEntity(packId, "Semana 1", "ACTIVE", 0L, 0L))
        contentDao.insertQuestions(
            listOf(
                QuestionEntity(question1Id, packId, "text-1", "Q1?", correctOption1, 1, null, "NONE"),
                QuestionEntity(question2Id, packId, "text-1", "Q2?", correctOption2, 1, null, "NONE")
            )
        )

        val attemptId = repository.startAttempt(uid, packId, null, System.currentTimeMillis(), 20 * 60 * 1000L)

        // Submit 2 correct answers
        repository.submitAnswer(attemptId, question1Id, correctOption1, 5000L)
        repository.submitAnswer(attemptId, question2Id, correctOption2, 3000L)

        val finishedAt = System.currentTimeMillis()
        repository.finishAttempt(attemptId, finishedAt, ExamStatus.COMPLETED)

        val attempts = examDao.getAttempts(uid)
        val finishedAttempt = attempts.find { it.attemptId == attemptId }
        assertNotNull(finishedAttempt)
        assertEquals(ExamStatus.COMPLETED, finishedAttempt.status)
        assertEquals(2, finishedAttempt.scoreRaw) // 2 respuestas correctas
        assertEquals(finishedAt, finishedAttempt.finishedAtLocal)
        assertEquals(SyncState.PENDING, finishedAttempt.syncState)
    }

    @Test
    fun finishAttempt_cancelledCheat_calculatesScoreFromAnswered() = runTest {
        val uid = "user-1"
        val packId = "pack-1"
        val question1Id = "q-1"
        val correctOption1 = "A"

        // Setup
        packDao.insert(PackEntity(packId, "Semana 1", "ACTIVE", 0L, 0L))
        contentDao.insertQuestions(
            listOf(
                QuestionEntity(question1Id, packId, "text-1", "Q1?", correctOption1, 1, null, "NONE")
            )
        )

        val attemptId = repository.startAttempt(uid, packId, null, System.currentTimeMillis(), 20 * 60 * 1000L)

        // Responder 1 pregunta antes de cancelar
        repository.submitAnswer(attemptId, question1Id, correctOption1, 5000L)

        // Cancelar por trampa
        repository.finishAttempt(attemptId, System.currentTimeMillis(), ExamStatus.CANCELLED_CHEAT)

        val attempts = examDao.getAttempts(uid)
        val cancelledAttempt = attempts.find { it.attemptId == attemptId }
        assertNotNull(cancelledAttempt)
        assertEquals(ExamStatus.CANCELLED_CHEAT, cancelledAttempt.status)
        assertEquals(1, cancelledAttempt.scoreRaw) // Score calculado desde respuestas guardadas
    }

    @Test
    fun observeAttempts_returnsFlowOrderedByDate() = runTest {
        val uid = "user-1"
        val packId = "pack-1"

        packDao.insert(PackEntity(packId, "Semana 1", "ACTIVE", 0L, 0L))

        val attempt1Id = repository.startAttempt(uid, packId, null, 1000L, 20 * 60 * 1000L)
        val attempt2Id = repository.startAttempt(uid, packId, null, 2000L, 20 * 60 * 1000L)

        repository.finishAttempt(attempt1Id, 5000L, ExamStatus.COMPLETED)
        repository.finishAttempt(attempt2Id, 6000L, ExamStatus.COMPLETED)

        val attempts = repository.observeAttempts(uid).first()

        assertEquals(2, attempts.size)
        // Debe estar ordenado descendente (más reciente primero)
        assertTrue(attempts[0].startedAtLocal >= attempts[1].startedAtLocal)
    }
}

