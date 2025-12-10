package com.eduquiz.feature.exam

import com.eduquiz.domain.exam.ExamAnswer
import com.eduquiz.domain.exam.ExamAttempt
import com.eduquiz.domain.exam.ExamRepository
import com.eduquiz.domain.exam.ExamStatus
import com.eduquiz.domain.pack.Option
import com.eduquiz.domain.pack.Pack
import com.eduquiz.domain.pack.PackMeta
import com.eduquiz.domain.pack.PackRepository
import com.eduquiz.domain.pack.PackStatus
import com.eduquiz.domain.pack.Question
import com.eduquiz.domain.pack.TextContent
import com.eduquiz.domain.profile.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.test.BeforeTest
import kotlin.test.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExamViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var packRepository: FakePackRepository
    private lateinit var examRepository: FakeExamRepository
    private lateinit var timeProvider: FakeTimeProvider

    @BeforeTest
    fun setup() {
        packRepository = FakePackRepository()
        examRepository = FakeExamRepository()
        timeProvider = FakeTimeProvider()
    }

    @Test
    fun remaining_isCalculatedFromElapsedTime_andAutoSubmits() = runTest(dispatcherRule.testDispatcher) {
        val viewModel = ExamViewModel(packRepository, examRepository, timeProvider)

        viewModel.initialize("uid-1")
        dispatcherRule.advanceUntilIdle()
        viewModel.startExam()
        dispatcherRule.advanceUntilIdle()

        assertEquals(DEFAULT_EXAM_DURATION_MS, viewModel.state.value.remainingMs)

        timeProvider.advanceBy(1_000L)
        dispatcherRule.advanceUntilIdle()
        assertEquals(DEFAULT_EXAM_DURATION_MS - 1_000L, viewModel.state.value.remainingMs)

        timeProvider.advanceBy(DEFAULT_EXAM_DURATION_MS)
        dispatcherRule.advanceUntilIdle()

        assertEquals(ExamStage.Finished, viewModel.state.value.stage)
        assertEquals(ExamStatus.AUTO_SUBMIT, viewModel.state.value.finishedStatus)
    }

    @Test
    fun options_areLockedForFiveSeconds() = runTest(dispatcherRule.testDispatcher) {
        val viewModel = ExamViewModel(packRepository, examRepository, timeProvider)

        viewModel.initialize("uid-2")
        dispatcherRule.advanceUntilIdle()
        viewModel.startExam()
        dispatcherRule.advanceUntilIdle()

        assertTrue(viewModel.state.value.areOptionsLocked)

        timeProvider.advanceBy(5_200L)
        dispatcherRule.advanceBy(5_200L)

        assertFalse(viewModel.state.value.areOptionsLocked)
    }
}

private class FakeTimeProvider(
    var elapsed: Long = 0L,
    var current: Long = 0L
) : TimeProvider {
    override fun elapsedRealtime(): Long = elapsed
    override fun currentTimeMillis(): Long = current

    fun advanceBy(millis: Long) {
        elapsed += millis
        current += millis
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        kotlinx.coroutines.test.resetMain()
    }

    fun advanceUntilIdle() {
        testDispatcher.scheduler.advanceUntilIdle()
    }

    fun advanceBy(millis: Long) {
        testDispatcher.scheduler.advanceTimeBy(millis)
    }
}

private class FakePackRepository : PackRepository {
    private val pack = Pack(
        packId = "pack-1",
        weekLabel = "Semana 1",
        status = PackStatus.ACTIVE,
        publishedAt = 0L,
        downloadedAt = 0L
    )
    private val text = TextContent(
        textId = "text-1",
        packId = pack.packId,
        title = "Titulo",
        body = "Cuerpo del texto",
        subject = "LECTURA"
    )
    private val question = Question(
        questionId = "question-1",
        packId = pack.packId,
        textId = text.textId,
        prompt = "Pregunta?",
        correctOptionId = "A",
        difficulty = 1,
        explanationText = null,
        explanationStatus = "NONE"
    )
    private val options = listOf(
        Option(questionId = question.questionId, optionId = "A", text = "Opcion A"),
        Option(questionId = question.questionId, optionId = "B", text = "Opcion B")
    )

    override suspend fun fetchCurrentPackMeta(): PackMeta? = null

    override suspend fun downloadPack(packId: String): Pack = pack

    override suspend fun getPackById(packId: String): Pack? = pack

    override suspend fun insertPack(pack: Pack) = Unit

    override suspend fun insertTexts(texts: List<TextContent>) = Unit

    override suspend fun insertQuestions(questions: List<Question>) = Unit

    override suspend fun insertOptions(options: List<Option>) = Unit

    override suspend fun setActivePack(packId: String) = Unit

    override suspend fun updatePackStatus(packId: String, status: String) = Unit

    override fun observeActivePack(): Flow<Pack?> = flowOf(pack)

    override suspend fun getTextsForPack(packId: String): List<TextContent> = listOf(text)

    override suspend fun getQuestionsForText(textId: String): List<Question> =
        if (textId == text.textId) listOf(question) else emptyList()

    override suspend fun getQuestionsForPack(packId: String): List<Question> = listOf(question)

    override suspend fun getQuestionsForPackBySubject(packId: String, subject: String): List<Question> =
        if (text.subject == subject) listOf(question) else emptyList()

    override suspend fun getOptionsForQuestion(questionId: String): List<Option> = options
}

private class FakeExamRepository : ExamRepository {
    val attempts = mutableListOf<ExamAttempt>()
    val answers = mutableListOf<ExamAnswer>()
    var finishedStatus: String? = null
    private var attemptCounter = 0

    override suspend fun startAttempt(
        uid: String,
        packId: String,
        subject: String?,
        startedAtLocal: Long,
        durationMs: Long
    ): String {
        val attemptId = "attempt-${++attemptCounter}"
        val attempt = ExamAttempt(
            attemptId = attemptId,
            uid = uid,
            packId = packId,
            subject = subject,
            startedAtLocal = startedAtLocal,
            finishedAtLocal = null,
            durationMs = durationMs,
            status = ExamStatus.IN_PROGRESS,
            scoreRaw = 0,
            scoreValidated = null,
            origin = "OFFLINE",
            syncState = "PENDING"
        )
        attempts.add(attempt)
        return attemptId
    }

    override suspend fun createAttempt(attempt: ExamAttempt) {
        attempts.add(attempt)
    }

    override suspend fun upsertAnswer(answer: ExamAnswer) {
        answers.removeAll { it.attemptId == answer.attemptId && it.questionId == answer.questionId }
        answers.add(answer)
    }

    override suspend fun finishAttempt(
        attemptId: String,
        finishedAtLocal: Long,
        status: String
    ) {
        finishedStatus = status
        val attempt = attempts.find { it.attemptId == attemptId }
        if (attempt != null) {
            val updatedAttempt = attempt.copy(
                finishedAtLocal = finishedAtLocal,
                status = status,
                scoreRaw = answers.count { it.attemptId == attemptId && it.isCorrect }
            )
            attempts.remove(attempt)
            attempts.add(updatedAttempt)
        }
    }

    override suspend fun getAttempts(uid: String): List<ExamAttempt> =
        attempts.filter { it.uid == uid }

    override suspend fun getAnswersForAttempt(attemptId: String): List<ExamAnswer> =
        answers.filter { it.attemptId == attemptId }
}
