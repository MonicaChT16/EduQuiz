package com.eduquiz.feature.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.exam.ExamAnswer
import com.eduquiz.domain.exam.ExamAttempt
import com.eduquiz.domain.exam.ExamOrigin
import com.eduquiz.domain.exam.ExamRepository
import com.eduquiz.domain.exam.ExamStatus
import com.eduquiz.feature.exam.ExamResult
import com.eduquiz.feature.exam.QuestionReview
import com.eduquiz.domain.achievements.AchievementEngine
import com.eduquiz.domain.achievements.AchievementEvent
import com.eduquiz.domain.pack.Pack
import com.eduquiz.domain.pack.PackRepository
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.profile.SyncState
import com.eduquiz.domain.sync.SyncRepository
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
    private val profileRepository: ProfileRepository,
    private val achievementEngine: AchievementEngine,
    private val timeProvider: TimeProvider,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExamUiState(stage = ExamStage.Loading))
    val state: StateFlow<ExamUiState> = _state.asStateFlow()

    private val _resultState = MutableStateFlow<ExamResult?>(null)
    val resultState: StateFlow<ExamResult?> = _resultState.asStateFlow()

    private val _reviewData = MutableStateFlow<List<QuestionReview>>(emptyList())
    val reviewData: StateFlow<List<QuestionReview>> = _reviewData.asStateFlow()

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
            val startedAtLocal = timeProvider.currentTimeMillis()
            val duration = DEFAULT_EXAM_DURATION_MS
            
            // Usar el nuevo método startAttempt que crea el intento en Room
            val attemptId = examRepository.startAttempt(
                uid = uid,
                packId = pack.packId,
                startedAtLocal = startedAtLocal,
                durationMs = duration
            )
            
            // Crear el objeto attempt para uso local
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
        // El tiempo gastado es el tiempo desde que se mostró la pregunta hasta ahora
        // Si el usuario cambia de opción, el tiempo se actualiza al tiempo total transcurrido
        val timeSpent = elapsedOnQuestion

        // Usar el nuevo método submitAnswer que calcula isCorrect automáticamente
        viewModelScope.launch {
            examRepository.submitAnswer(
                attemptId = attemptId,
                questionId = question.question.questionId,
                optionId = optionId,
                timeSpentMs = timeSpent
            )
            
            // Actualizar estado local
            val savedAnswer = examRepository.getAnswersForAttempt(attemptId)
                .find { it.questionId == question.question.questionId }
            
            if (savedAnswer != null) {
                answers[question.question.questionId] = savedAnswer
                _state.update { 
                    it.copy(answers = it.answers + (question.question.questionId to optionId)) 
                }
            }
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
            // Si no hay pack activo, buscar packs disponibles
            val availablePack = runCatching { packRepository.fetchCurrentPackMeta() }.getOrNull()
            _state.update {
                it.copy(
                    stage = ExamStage.Start,
                    pack = null,
                    availablePack = availablePack,
                    questions = emptyList(),
                    isBusy = false,
                    errorMessage = if (availablePack == null) {
                        "No hay packs disponibles. Intenta refrescar."
                    } else {
                        "Descarga un pack para iniciar el simulacro."
                    }
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

        // Buscar packs disponibles en paralelo (para mostrar si hay actualizaciones)
        val availablePack = runCatching { packRepository.fetchCurrentPackMeta() }.getOrNull()

        if (inProgressAttempt != null) {
            resumeAttempt(inProgressAttempt, pack, questions)
        } else {
            _state.update {
                it.copy(
                    stage = ExamStage.Start,
                    pack = pack,
                    availablePack = availablePack,
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

        // Calcular score desde las respuestas guardadas
        val scoreFromSaved = savedAnswers.count { it.isCorrect }

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
                correctCount = scoreFromSaved,
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
        val uid = userId ?: return
        timerJob?.cancel()

        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            
            // Usar el nuevo método finishAttempt que calcula scoreRaw automáticamente
            examRepository.finishAttempt(
                attemptId = currentAttempt.attemptId,
                finishedAtLocal = timeProvider.currentTimeMillis(),
                status = status
            )
            
            // Calcular y otorgar EduCoins
            if (status != ExamStatus.CANCELLED_CHEAT) {
                calculateAndAwardCoins(uid, currentAttempt.attemptId)
                
                // Evaluar logros relacionados con completar examen
                achievementEngine.evaluateAndUnlock(
                    uid = uid,
                    event = AchievementEvent.ExamCompleted
                )
            }
            
            // Encolar sincronización inmediata
            syncRepository.enqueueSyncNow()
            
            // Cargar resultado desde Room
            loadResult(currentAttempt.attemptId)
            
            _state.update {
                it.copy(
                    stage = ExamStage.Finished,
                    finishedStatus = status,
                    remainingMs = 0L,
                    isBusy = false
                )
            }
        }
    }

    private suspend fun calculateAndAwardCoins(uid: String, attemptId: String) {
        val answers = examRepository.getAnswersForAttempt(attemptId)
        if (answers.isEmpty()) return

        val updatedAtLocal = timeProvider.currentTimeMillis()
        var totalCoins = 0
        var totalXp = 0L

        // 1. Base: coins por respuestas correctas (10 coins por correcta)
        val correctAnswers = answers.count { it.isCorrect }
        val baseCoins = correctAnswers * 10
        if (baseCoins > 0) {
            profileRepository.addCoins(uid, baseCoins, "correct_answer", updatedAtLocal, SyncState.PENDING)
            // XP se gana igual que coins
            profileRepository.addXp(uid, baseCoins.toLong(), updatedAtLocal, SyncState.PENDING)
            totalCoins += baseCoins
            totalXp += baseCoins
        }

        // 2. Bonus por velocidad: respuestas < 60 segundos (5 coins extra por cada una)
        val speedBonusThreshold = 60_000L // 60 segundos en ms
        val fastAnswers = answers.count { it.isCorrect && it.timeSpentMs < speedBonusThreshold }
        val speedBonus = fastAnswers * 5
        if (speedBonus > 0) {
            profileRepository.addCoins(uid, speedBonus, "speed_bonus", updatedAtLocal, SyncState.PENDING)
            // XP se gana igual que coins
            profileRepository.addXp(uid, speedBonus.toLong(), updatedAtLocal, SyncState.PENDING)
            totalCoins += speedBonus
            totalXp += speedBonus
        }

        // 3. Bonus de racha se otorga automáticamente en StreakService cuando se alcanza 3 días
        // El bonus se otorga al iniciar sesión, no al completar examen
    }

    fun loadResult(attemptId: String) {
        viewModelScope.launch {
            val finishedAttempt = examRepository.getAttempts(userId ?: "")
                .find { it.attemptId == attemptId }
            
            if (finishedAttempt != null) {
                val answers = examRepository.getAnswersForAttempt(attemptId)
                _resultState.value = ExamResult(
                    attemptId = attemptId,
                    status = finishedAttempt.status,
                    scoreRaw = finishedAttempt.scoreRaw,
                    answeredCount = answers.size,
                    finishedAtLocal = finishedAttempt.finishedAtLocal,
                    totalQuestions = _state.value.totalQuestions
                )
                
                // Cargar datos de revisión
                loadReviewData(attemptId, finishedAttempt.packId, answers)
            }
        }
    }

    private suspend fun loadReviewData(attemptId: String, packId: String, answers: List<ExamAnswer>) {
        try {
            val pack = packRepository.getPackById(packId) ?: return
            val questions = packRepository.getQuestionsForPack(packId)
            val texts = packRepository.getTextsForPack(packId)
            val textsMap = texts.associateBy { it.textId }
            val answersMap = answers.associateBy { it.questionId }
            
            val reviewItems = questions.mapNotNull { question ->
                val text = textsMap[question.textId] ?: return@mapNotNull null
                val options = packRepository.getOptionsForQuestion(question.questionId)
                val userAnswer = answersMap[question.questionId]
                
                QuestionReview(
                    question = question,
                    text = text,
                    options = options,
                    userAnswer = userAnswer,
                    correctOptionId = question.correctOptionId
                )
            }
            
            _reviewData.value = reviewItems
        } catch (e: Exception) {
            android.util.Log.e("ExamViewModel", "Error loading review data", e)
        }
    }

    fun refreshAvailablePack() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPack = true, errorMessage = null) }
            val availablePack = runCatching { packRepository.fetchCurrentPackMeta() }
                .getOrElse { throwable ->
                    _state.update {
                        it.copy(
                            isLoadingPack = false,
                            errorMessage = throwable.localizedMessage ?: "Error al buscar packs disponibles."
                        )
                    }
                    null
                }
            _state.update {
                it.copy(
                    availablePack = availablePack,
                    isLoadingPack = false,
                    errorMessage = if (availablePack == null) {
                        "No hay packs disponibles en este momento."
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun downloadPack() {
        val packId = _state.value.availablePack?.packId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDownloading = true, errorMessage = null) }
            runCatching { packRepository.downloadPack(packId) }
                .onSuccess { downloadedPack ->
                    // Recargar el estado inicial para actualizar con el pack descargado
                    loadInitialState()
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isDownloading = false,
                            errorMessage = throwable.localizedMessage ?: "No se pudo descargar el pack."
                        )
                    }
                }
        }
    }

    private fun calculateScore(): Int = answers.values.count { it.isCorrect }

    private fun remainingFromAttempt(attempt: ExamAttempt): Long {
        val elapsedWall = (timeProvider.currentTimeMillis() - attempt.startedAtLocal).coerceAtLeast(0L)
        val nowElapsed = timeProvider.elapsedRealtime()
        // Ajustar startedElapsed para que el cálculo del timer sea correcto
        // Usamos elapsedWall limitado a durationMs para evitar valores negativos
        val elapsedClamped = elapsedWall.coerceAtMost(attempt.durationMs)
        startedElapsed = nowElapsed - elapsedClamped
        val remaining = attempt.durationMs - elapsedWall
        return remaining.coerceAtLeast(0L)
    }

    private fun currentLockRemaining(now: Long = timeProvider.elapsedRealtime()): Long {
        val unlockAt = optionsUnlockAt ?: return 0L
        return (unlockAt - now).coerceAtLeast(0L)
    }
}
