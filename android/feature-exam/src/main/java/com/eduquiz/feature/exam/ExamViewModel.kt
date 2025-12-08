package com.eduquiz.feature.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.exam.ExamAnswer
import com.eduquiz.domain.exam.ExamAttempt
import com.eduquiz.domain.exam.ExamOrigin
import com.eduquiz.domain.exam.ExamRepository
import com.eduquiz.domain.exam.ExamStatus
import com.eduquiz.domain.pack.Pack
import com.eduquiz.domain.pack.PackRepository
import com.eduquiz.domain.profile.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val packRepository: PackRepository,
    private val examRepository: ExamRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(ExamUiState(stage = ExamStage.Loading))
    val state: StateFlow<ExamUiState> = _state.asStateFlow()

    private val answers = mutableMapOf<String, ExamAnswer>()
    private var attempt: ExamAttempt? = null
    private var userId: String? = null
    private var startedElapsed: Long? = null
    private var optionsUnlockAt: Long? = null
    private var questionShownAt: Long = 0L
    private var timerJob: Job? = null

    fun initialize(uid: String) {
        if (userId != null) return
        userId = uid
        viewModelScope.launch {
            loadInitialState()
        }
    }

    fun startExam() {
        if (_state.value.stage == ExamStage.InProgress) return
        val uid = userId ?: return
        val pack = _state.value.pack ?: return
        if (_state.value.questions.isEmpty()) {
            _state.update { it.copy(errorMessage = "No hay preguntas disponibles para este pack.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, errorMessage = null) }
            val attemptId = "attempt-${UUID.randomUUID()}"
            val startedAtLocal = timeProvider.currentTimeMillis()
            val duration = DEFAULT_EXAM_DURATION_MS
            attempt = ExamAttempt(
                attemptId = attemptId,
                uid = uid,
                packId = pack.packId,
                startedAtLocal = startedAtLocal,
                finishedAtLocal = null,
                durationMs = duration,
                status = ExamStatus.IN_PROGRESS,
                scoreRaw = 0,
                scoreValidated = null,
                origin = ExamOrigin.OFFLINE,
                syncState = SyncState.PENDING
            )
            examRepository.createAttempt(requireNotNull(attempt))
            answers.clear()
            startedElapsed = timeProvider.elapsedRealtime()
            lockForCurrentQuestion()
            _state.update {
                it.copy(
                    stage = ExamStage.InProgress,
                    attemptId = attemptId,
                    currentIndex = 0,
                    answers = emptyMap(),
                    durationMs = duration,
                    remainingMs = duration,
                    lockRemainingMs = OPTION_LOCK_MS,
                    showWarningDialog = false,
                    leaveCount = 0,
                    finishedStatus = null,
                    correctCount = 0,
                    isBusy = false
                )
            }
            startTimer()
        }
    }

    fun submitManually() {
        if (_state.value.stage != ExamStage.InProgress) return
        finishExam(ExamStatus.COMPLETED)
    }

    fun onLeaveApp() {
        if (_state.value.stage != ExamStage.InProgress) return
        val updatedLeaveCount = _state.value.leaveCount + 1
        _state.update {
            it.copy(
                leaveCount = updatedLeaveCount,
                showWarningDialog = updatedLeaveCount == 1
            )
        }
        if (updatedLeaveCount >= 2) {
            finishExam(ExamStatus.CANCELLED_CHEAT)
        }
    }

    fun dismissWarning() {
        _state.update { it.copy(showWarningDialog = false) }
    }

    fun selectOption(optionId: String) {
        if (_state.value.stage != ExamStage.InProgress) return
        if (currentLockRemaining() > 0) return
        val attemptId = attempt?.attemptId ?: return
        val question = currentQuestion() ?: return

        val now = timeProvider.elapsedRealtime()
        val elapsedOnQuestion = (now - questionShownAt).coerceAtLeast(0L)
        val existingAnswer = answers[question.question.questionId]
        val timeSpent = max(existingAnswer?.timeSpentMs ?: 0L, elapsedOnQuestion)
        val isCorrect = optionId == question.question.correctOptionId

        val answer = ExamAnswer(
            attemptId = attemptId,
            questionId = question.question.questionId,
            selectedOptionId = optionId,
            isCorrect = isCorrect,
            timeSpentMs = timeSpent
        )
        answers[question.question.questionId] = answer
        _state.update { it.copy(answers = it.answers + (question.question.questionId to optionId)) }

        viewModelScope.launch {
            examRepository.upsertAnswer(answer)
        }
    }

    fun goToNextQuestion() {
        val total = _state.value.totalQuestions
        if (total == 0) return
        val nextIndex = (_state.value.currentIndex + 1).coerceAtMost(total - 1)
        if (nextIndex != _state.value.currentIndex) {
            setCurrentIndex(nextIndex)
        }
    }

    fun goToPreviousQuestion() {
        val total = _state.value.totalQuestions
        if (total == 0) return
        val prevIndex = (_state.value.currentIndex - 1).coerceAtLeast(0)
        if (prevIndex != _state.value.currentIndex) {
            setCurrentIndex(prevIndex)
        }
    }

    private suspend fun loadInitialState() {
        _state.update { it.copy(stage = ExamStage.Loading, isBusy = true, errorMessage = null) }
        val pack = packRepository.observeActivePack().firstOrNull()
        if (pack == null) {
            _state.update {
                it.copy(
                    stage = ExamStage.Start,
                    pack = null,
                    questions = emptyList(),
                    isBusy = false,
                    errorMessage = "Descarga un pack para iniciar el simulacro."
                )
            }
            return
        }

        val questions = runCatching { prepareQuestions(pack.packId) }
            .getOrElse { throwable ->
                _state.update {
                    it.copy(
                        stage = ExamStage.Start,
                        pack = pack,
                        questions = emptyList(),
                        isBusy = false,
                        errorMessage = throwable.localizedMessage
                            ?: "No se pudieron cargar las preguntas."
                    )
                }
                return
            }

        val inProgressAttempt = userId?.let { uid ->
            examRepository.getAttempts(uid)
                .firstOrNull { attempt -> attempt.status == ExamStatus.IN_PROGRESS && attempt.packId == pack.packId }
        }

        if (inProgressAttempt != null) {
            resumeAttempt(inProgressAttempt, pack, questions)
        } else {
            _state.update {
                it.copy(
                    stage = ExamStage.Start,
                    pack = pack,
                    questions = questions,
                    durationMs = DEFAULT_EXAM_DURATION_MS,
                    remainingMs = DEFAULT_EXAM_DURATION_MS,
                    isBusy = false
                )
            }
        }
    }

    private suspend fun resumeAttempt(
        existing: ExamAttempt,
        pack: Pack,
        questions: List<ExamContent>
    ) {
        attempt = existing
        val remaining = remainingFromAttempt(existing)
        val savedAnswers = examRepository.getAnswersForAttempt(existing.attemptId)
        answers.clear()
        savedAnswers.forEach { answers[it.questionId] = it }
        val firstUnanswered = questions.indexOfFirst { it.question.questionId !in answers.keys }
        val nextIndex = if (firstUnanswered >= 0) firstUnanswered else 0

        lockForCurrentQuestion()
        _state.update {
            it.copy(
                stage = ExamStage.InProgress,
                pack = pack,
                attemptId = existing.attemptId,
                questions = questions,
                currentIndex = nextIndex,
                answers = answers.mapValues { entry -> entry.value.selectedOptionId },
                durationMs = existing.durationMs,
                remainingMs = remaining.coerceAtLeast(0L),
                lockRemainingMs = OPTION_LOCK_MS,
                finishedStatus = null,
                correctCount = calculateScore(),
                isBusy = false
            )
        }

        if (remaining <= 0L) {
            finishExam(ExamStatus.AUTO_SUBMIT)
        } else {
            startTimer()
        }
    }

    private suspend fun prepareQuestions(packId: String): List<ExamContent> {
        val texts = packRepository.getTextsForPack(packId).associateBy { it.textId }
        val questions = packRepository.getQuestionsForPack(packId).sortedBy { it.questionId }
        if (questions.isEmpty()) error("El pack no tiene preguntas almacenadas.")

        return questions.map { question ->
            val text = texts[question.textId]
                ?: error("Falta el texto ${question.textId} para la pregunta.")
            val options = packRepository.getOptionsForQuestion(question.questionId)
                .sortedBy { it.optionId }
            ExamContent(question, text, options)
        }
    }

    private fun setCurrentIndex(newIndex: Int) {
        lockForCurrentQuestion()
        _state.update {
            it.copy(
                currentIndex = newIndex,
                lockRemainingMs = OPTION_LOCK_MS,
                showWarningDialog = false
            )
        }
    }

    private fun lockForCurrentQuestion() {
        val now = timeProvider.elapsedRealtime()
        optionsUnlockAt = now + OPTION_LOCK_MS
        questionShownAt = now
    }

    private fun currentQuestion(): ExamContent? =
        _state.value.questions.getOrNull(_state.value.currentIndex)

    private fun startTimer() {
        timerJob?.cancel()
        val duration = attempt?.durationMs ?: _state.value.durationMs
        val start = startedElapsed ?: timeProvider.elapsedRealtime().also { startedElapsed = it }

        timerJob = viewModelScope.launch {
            while (isActive) {
                val now = timeProvider.elapsedRealtime()
                val elapsed = now - start
                val remaining = (duration - elapsed).coerceAtLeast(0L)
                val lockRemaining = currentLockRemaining(now)
                _state.update {
                    it.copy(
                        remainingMs = remaining,
                        lockRemainingMs = lockRemaining
                    )
                }
                if (remaining <= 0L) {
                    handleAutoSubmit()
                    break
                }
                delay(250L)
            }
        }
    }

    private fun handleAutoSubmit() {
        finishExam(ExamStatus.AUTO_SUBMIT)
    }

    private fun finishExam(status: String) {
        if (_state.value.stage == ExamStage.Finished) return
        val currentAttempt = attempt ?: return
        timerJob?.cancel()

        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            val score = calculateScore()
            examRepository.finishAttempt(
                attemptId = currentAttempt.attemptId,
                finishedAtLocal = timeProvider.currentTimeMillis(),
                status = status,
                scoreRaw = score,
                scoreValidated = score,
                syncState = SyncState.PENDING
            )
            _state.update {
                it.copy(
                    stage = ExamStage.Finished,
                    finishedStatus = status,
                    correctCount = score,
                    remainingMs = 0L,
                    isBusy = false
                )
            }
        }
    }

    private fun calculateScore(): Int = answers.values.count { it.isCorrect }

    private fun remainingFromAttempt(attempt: ExamAttempt): Long {
        val elapsedWall = (timeProvider.currentTimeMillis() - attempt.startedAtLocal).coerceAtLeast(0L)
        val nowElapsed = timeProvider.elapsedRealtime()
        startedElapsed = nowElapsed - elapsedWall
        return attempt.durationMs - elapsedWall
    }

    private fun currentLockRemaining(now: Long = timeProvider.elapsedRealtime()): Long {
        val unlockAt = optionsUnlockAt ?: return 0L
        return (unlockAt - now).coerceAtLeast(0L)
    }
}
