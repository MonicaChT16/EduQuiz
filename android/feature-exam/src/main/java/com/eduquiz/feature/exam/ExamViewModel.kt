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
    private var currentSubject: String? = null

    fun initialize(uid: String) {
        android.util.Log.d("ExamViewModel", "initialize called with uid: $uid")
        if (userId != null) {
            android.util.Log.d("ExamViewModel", "Already initialized with userId: $userId")
            return
        }
        userId = uid
        android.util.Log.d("ExamViewModel", "Setting userId to: $uid")
        viewModelScope.launch {
            android.util.Log.d("ExamViewModel", "Starting loadInitialState")
            loadInitialState()
        }
    }

    fun startExam(subject: String? = null) {
        if (_state.value.stage == ExamStage.InProgress) {
            android.util.Log.w("ExamViewModel", "startExam: Exam already in progress")
            return
        }
        
        viewModelScope.launch {
            android.util.Log.d("ExamViewModel", "startExam: Starting with subject=$subject")
            _state.update { it.copy(isBusy = true, errorMessage = null) }
            
            // Validar que tenemos userId
            val uid = userId
            if (uid == null) {
                android.util.Log.e("ExamViewModel", "startExam: userId is null")
                _state.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = "Error: Usuario no identificado. Por favor, cierra sesión y vuelve a iniciar sesión."
                    )
                }
                return@launch
            }
            
            // Validar que tenemos pack - si no hay, intentar cargarlo de nuevo
            var pack = _state.value.pack
            if (pack == null) {
                android.util.Log.w("ExamViewModel", "startExam: pack is null, trying to load from database")
                pack = packRepository.getActivePack()
                if (pack == null) {
                    android.util.Log.e("ExamViewModel", "startExam: No pack found in database")
                    _state.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = "No hay pack activo. Por favor, descarga un pack primero."
                        )
                    }
                    return@launch
                }
                // Actualizar el estado con el pack encontrado
                _state.update { it.copy(pack = pack) }
            }
            
            // Guardar la materia actual
            currentSubject = subject
            
            android.util.Log.d("ExamViewModel", "startExam: packId=${pack.packId}, subject=$subject")
            
            // Si se especifica una materia, cargar solo preguntas de esa materia (máximo 10)
            val questions = if (subject != null) {
                runCatching { 
                    android.util.Log.d("ExamViewModel", "Loading questions for subject: $subject")
                    prepareQuestions(pack.packId, subject) 
                }.getOrElse { throwable ->
                    android.util.Log.e("ExamViewModel", "Error preparing questions for subject $subject", throwable)
                    _state.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = throwable.localizedMessage ?: "No hay preguntas disponibles para ${com.eduquiz.domain.pack.Subject.getDisplayName(subject)}. Verifica que el pack tenga contenido para esta materia."
                        )
                    }
                    return@launch
                }
            } else {
                // Si no hay materia específica, usar las preguntas ya cargadas o cargar todas
                if (_state.value.questions.isEmpty()) {
                    runCatching {
                        prepareQuestions(pack.packId)
                    }.getOrElse { throwable ->
                        android.util.Log.e("ExamViewModel", "Error preparing questions", throwable)
                        _state.update {
                            it.copy(
                                isBusy = false,
                                errorMessage = throwable.localizedMessage ?: "No se pudieron cargar las preguntas."
                            )
                        }
                        return@launch
                    }
                } else {
                    _state.value.questions
                }
            }
            
            android.util.Log.d("ExamViewModel", "Loaded ${questions.size} questions")
            
            if (questions.isEmpty()) {
                android.util.Log.w("ExamViewModel", "No questions found for packId=${pack.packId}, subject=$subject")
                _state.update { 
                    it.copy(
                        isBusy = false,
                        errorMessage = if (subject != null) {
                            "No hay preguntas disponibles para ${com.eduquiz.domain.pack.Subject.getDisplayName(subject)} en este pack. Intenta con otra materia."
                        } else {
                            "No hay preguntas disponibles para este pack."
                        }
                    ) 
                }
                return@launch
            }
            
            _state.update { it.copy(questions = questions) }
            
            startExamInternal()
        }
    }
    
    private fun startExamInternal() {
        val uid = userId ?: return
        val pack = _state.value.pack ?: return
        val questions = _state.value.questions
        if (questions.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, errorMessage = null) }
            val startedAtLocal = timeProvider.currentTimeMillis()
            val duration = DEFAULT_EXAM_DURATION_MS
            
            // Usar el nuevo método startAttempt que crea el intento en Room
            val attemptId = examRepository.startAttempt(
                uid = uid,
                packId = pack.packId,
                subject = currentSubject,
                startedAtLocal = startedAtLocal,
                durationMs = duration
            )
            
            // Crear el objeto attempt para uso local
            attempt = ExamAttempt(
                attemptId = attemptId,
                uid = uid,
                packId = pack.packId,
                subject = currentSubject,
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
        android.util.Log.d("ExamViewModel", "loadInitialState: Starting")
        _state.update { it.copy(stage = ExamStage.Loading, isBusy = true, errorMessage = null) }
        
        android.util.Log.d("ExamViewModel", "loadInitialState: Getting active pack from database")
        var pack = packRepository.getActivePack()
        android.util.Log.d("ExamViewModel", "loadInitialState: Active pack = ${pack?.packId ?: "null"}")
        
        if (pack == null) {
            // Si no hay pack activo, buscar packs disponibles y descargar automáticamente
            val availablePack = runCatching { packRepository.fetchCurrentPackMeta() }.getOrNull()
            
            if (availablePack != null) {
                // Descargar automáticamente el pack disponible
                _state.update { it.copy(isBusy = true, errorMessage = "Descargando pack...") }
                try {
                    android.util.Log.d("ExamViewModel", "Auto-downloading pack: ${availablePack.packId}")
                    pack = packRepository.downloadPack(availablePack.packId)
                    android.util.Log.d("ExamViewModel", "Pack downloaded successfully: ${pack.packId}")
                    // Continuar con la carga normal ahora que tenemos el pack
                } catch (e: Exception) {
                    android.util.Log.e("ExamViewModel", "Error auto-downloading pack", e)
                    _state.update {
                        it.copy(
                            stage = ExamStage.Start,
                            pack = null,
                            availablePack = availablePack,
                            questions = emptyList(),
                            isBusy = false,
                            errorMessage = "Error al descargar el pack. Intenta nuevamente."
                        )
                    }
                    return
                }
            } else {
                // No hay pack disponible
                _state.update {
                    it.copy(
                        stage = ExamStage.Start,
                        pack = null,
                        availablePack = null,
                        questions = emptyList(),
                        isBusy = false,
                        errorMessage = "No hay packs disponibles. Intenta refrescar."
                    )
                }
                return
            }
        }

        android.util.Log.d("ExamViewModel", "loadInitialState: Preparing questions for pack ${pack.packId}")
        val questions = runCatching { prepareQuestions(pack.packId) }
            .getOrElse { throwable ->
                android.util.Log.e("ExamViewModel", "loadInitialState: Error preparing questions", throwable)
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
        
        android.util.Log.d("ExamViewModel", "loadInitialState: Prepared ${questions.size} questions")

        val inProgressAttempt = userId?.let { uid ->
            android.util.Log.d("ExamViewModel", "loadInitialState: Checking for in-progress attempts for uid: $uid")
            examRepository.getAttempts(uid)
                .firstOrNull { attempt -> attempt.status == ExamStatus.IN_PROGRESS && attempt.packId == pack.packId }
        }
        
        android.util.Log.d("ExamViewModel", "loadInitialState: In-progress attempt = ${inProgressAttempt?.attemptId ?: "null"}")

        // Buscar packs disponibles en paralelo (para mostrar si hay actualizaciones)
        val availablePack = runCatching { packRepository.fetchCurrentPackMeta() }.getOrNull()
        android.util.Log.d("ExamViewModel", "loadInitialState: Available pack = ${availablePack?.packId ?: "null"}")

        if (inProgressAttempt != null) {
            android.util.Log.d("ExamViewModel", "loadInitialState: Resuming attempt ${inProgressAttempt.attemptId}")
            resumeAttempt(inProgressAttempt, pack, questions)
        } else {
            android.util.Log.d("ExamViewModel", "loadInitialState: Setting stage to Start with ${questions.size} questions")
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
            val updatedState = _state.value
            android.util.Log.d("ExamViewModel", "loadInitialState: State updated, stage=${updatedState.stage}, pack=${updatedState.pack?.packId}, questions=${updatedState.questions.size}")
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

    private suspend fun prepareQuestions(packId: String, subject: String? = null): List<ExamContent> {
        android.util.Log.d("ExamViewModel", "prepareQuestions: packId=$packId, subject=$subject")
        
        val texts = packRepository.getTextsForPack(packId).associateBy { it.textId }
        android.util.Log.d("ExamViewModel", "Found ${texts.size} texts for pack $packId")
        
        // Log textos por materia para debugging
        if (subject != null) {
            val textsForSubject = texts.values.filter { it.subject == subject }
            android.util.Log.d("ExamViewModel", "Found ${textsForSubject.size} texts for subject $subject")
            textsForSubject.forEach { text ->
                android.util.Log.d("ExamViewModel", "Text ${text.textId}: subject=${text.subject}, title=${text.title}")
            }
        }
        
        val questions = if (subject != null) {
            // Limitar a 10 preguntas por materia para pruebas PISA
            val allQuestions = packRepository.getQuestionsForPackBySubject(packId, subject)
            android.util.Log.d("ExamViewModel", "Found ${allQuestions.size} questions for subject $subject")
            allQuestions
                .sortedBy { it.questionId }
                .take(10)
        } else {
            val allQuestions = packRepository.getQuestionsForPack(packId)
            android.util.Log.d("ExamViewModel", "Found ${allQuestions.size} questions for pack (no subject filter)")
            allQuestions.sortedBy { it.questionId }
        }
        
        if (questions.isEmpty()) {
            val errorMsg = "El pack no tiene preguntas almacenadas${if (subject != null) " para ${com.eduquiz.domain.pack.Subject.getDisplayName(subject)}" else ""}."
            android.util.Log.e("ExamViewModel", errorMsg)
            android.util.Log.e("ExamViewModel", "PackId: $packId, Subject: $subject, Total texts: ${texts.size}")
            error(errorMsg)
        }

        val result = questions.mapNotNull { question ->
            val text = texts[question.textId]
            if (text == null) {
                android.util.Log.e("ExamViewModel", "Missing text ${question.textId} for question ${question.questionId}")
                null
            } else {
                val options = packRepository.getOptionsForQuestion(question.questionId)
                if (options.isEmpty()) {
                    android.util.Log.w("ExamViewModel", "Question ${question.questionId} has no options")
                }
                ExamContent(question, text, options)
            }
        }
        
        android.util.Log.d("ExamViewModel", "Prepared ${result.size} exam contents (from ${questions.size} questions)")
        return result
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
            // Obtener la materia del intento para filtrar solo las preguntas de ese examen
            val attempt = examRepository.getAttempts(userId ?: "")
                .find { it.attemptId == attemptId }
            val subject = attempt?.subject
            
            // Solo cargar preguntas de la materia del examen (no todas las del pack)
            val questions = if (subject != null) {
                packRepository.getQuestionsForPackBySubject(packId, subject)
                    .sortedBy { it.questionId }
                    .take(10) // Limitar a 10 como en el examen
            } else {
                // Si no hay materia, cargar todas (compatibilidad con intentos antiguos)
                packRepository.getQuestionsForPack(packId)
            }
            
            val texts = packRepository.getTextsForPack(packId)
            val textsMap = texts.associateBy { it.textId }
            val answersMap = answers.associateBy { it.questionId }
            
            // Solo incluir preguntas que tienen respuesta (del examen dado)
            val reviewItems = questions
                .filter { question -> answersMap.containsKey(question.questionId) }
                .mapNotNull { question ->
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
                    android.util.Log.e("ExamViewModel", "Error fetching pack meta", throwable)
                    val errorMsg = when {
                        throwable.message?.contains("Missing or insufficient permissions") == true -> 
                            "Error de permisos en Firestore. Verifica las reglas de seguridad."
                        throwable.message?.contains("network") == true || throwable.message?.contains("Network") == true -> 
                            "Error de conexión. Verifica tu conexión a internet."
                        throwable.message?.contains("google-services.json") == true || throwable.message?.contains("FirebaseApp") == true -> 
                            "Error de configuración de Firebase. Verifica google-services.json"
                        else -> throwable.localizedMessage ?: "Error al buscar packs disponibles: ${throwable.javaClass.simpleName}"
                    }
                    _state.update {
                        it.copy(
                            isLoadingPack = false,
                            errorMessage = errorMsg
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
