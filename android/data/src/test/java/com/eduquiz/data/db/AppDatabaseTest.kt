package com.eduquiz.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eduquiz.domain.exam.ExamOrigin
import com.eduquiz.domain.exam.ExamStatus
import com.eduquiz.domain.pack.ExplanationStatus
import com.eduquiz.domain.pack.PackStatus
import com.eduquiz.domain.profile.SyncState
import com.eduquiz.data.db.UserProfileEntity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var packDao: PackDao
    private lateinit var contentDao: ContentDao
    private lateinit var profileDao: ProfileDao
    private lateinit var examDao: ExamDao

    @BeforeTest
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        packDao = db.packDao()
        contentDao = db.contentDao()
        profileDao = db.profileDao()
        examDao = db.examDao()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertPack_andReadActive() = runBlocking {
        val pack = PackEntity(
            packId = "pack-1",
            weekLabel = "Semana 1",
            status = PackStatus.ACTIVE,
            publishedAt = 123L,
            downloadedAt = 456L
        )

        packDao.insert(pack)

        val active = packDao.observeByStatus(PackStatus.ACTIVE).first()

        assertNotNull(active)
        assertEquals(pack.packId, active.packId)
        assertEquals(PackStatus.ACTIVE, active.status)
    }

    @Test
    fun createAttempt_answer_finish_andRead() = runBlocking {
        val pack = PackEntity(
            packId = "pack-2",
            weekLabel = "Semana 2",
            status = PackStatus.DOWNLOADED,
            publishedAt = 1L,
            downloadedAt = 2L
        )
        val profile = UserProfileEntity(
            uid = "uid-1",
            displayName = "Ana",
            photoUrl = null,
            schoolId = "school-1",
            classroomId = "class-1",
            coins = 0,
            selectedCosmeticId = "default",
            updatedAtLocal = 1L,
            syncState = SyncState.PENDING
        )
        val text = TextEntity(
            textId = "text-1",
            packId = pack.packId,
            title = "Titulo",
            body = "Cuerpo",
            subject = "LECTURA"
        )
        val question = QuestionEntity(
            questionId = "question-1",
            packId = pack.packId,
            textId = text.textId,
            prompt = "Pregunta?",
            correctOptionId = "A",
            difficulty = 1,
            explanationText = null,
            explanationStatus = ExplanationStatus.NONE
        )

        packDao.insert(pack)
        profileDao.upsertProfile(profile)
        contentDao.insertTexts(listOf(text))
        contentDao.insertQuestions(listOf(question))

        val attempt = ExamAttemptEntity(
            attemptId = "attempt-1",
            uid = "uid-1",
            packId = pack.packId,
            subject = null,
            startedAtLocal = 1L,
            finishedAtLocal = null,
            durationMs = 1_200_000L,
            status = ExamStatus.IN_PROGRESS,
            scoreRaw = 0,
            scoreValidated = null,
            origin = ExamOrigin.OFFLINE,
            syncState = SyncState.PENDING
        )
        val answer = ExamAnswerEntity(
            attemptId = attempt.attemptId,
            questionId = question.questionId,
            selectedOptionId = "A",
            isCorrect = true,
            timeSpentMs = 1000L
        )

        examDao.insertAttempt(attempt)
        examDao.upsertAnswer(answer)
        examDao.finishAttempt(
            attemptId = attempt.attemptId,
            finishedAtLocal = 2L,
            status = ExamStatus.COMPLETED,
            scoreRaw = 10,
            scoreValidated = 10,
            syncState = SyncState.PENDING
        )

        val attempts = examDao.getAttempts(attempt.uid)
        assertEquals(1, attempts.size)
        assertEquals(ExamStatus.COMPLETED, attempts.first().status)
        assertEquals(10, attempts.first().scoreRaw)

        val answers = examDao.getAnswers(attempt.attemptId)
        assertEquals(1, answers.size)
        assertEquals(answer.selectedOptionId, answers.first().selectedOptionId)
        assertEquals(answer.isCorrect, answers.first().isCorrect)
    }
}
