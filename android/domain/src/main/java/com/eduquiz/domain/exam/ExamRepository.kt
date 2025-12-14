package com.eduquiz.domain.exam

import kotlinx.coroutines.flow.Flow

interface ExamRepository {
    /**
     * Crea un nuevo intento de examen y lo guarda en Room con status IN_PROGRESS.
     * @param subject materia del examen (MATEMATICA, COMPRENSION_LECTORA, CIENCIAS)
     * @return attemptId generado
     */
    suspend fun startAttempt(
        uid: String,
        packId: String,
        subject: String?,
        startedAtLocal: Long,
        durationMs: Long
    ): String

    /**
     * Guarda o actualiza una respuesta. Calcula isCorrect comparando con correctOptionId.
     * @param timeSpentMs tiempo transcurrido en la pregunta
     */
    suspend fun submitAnswer(
        attemptId: String,
        questionId: String,
        optionId: String,
        timeSpentMs: Long
    )

    /**
     * Finaliza un intento calculando scoreRaw automáticamente desde las respuestas guardadas.
     * @param status COMPLETED, AUTO_SUBMIT, o CANCELLED_CHEAT
     */
    suspend fun finishAttempt(
        attemptId: String,
        finishedAtLocal: Long,
        status: String
    )

    /**
     * Obtiene todos los intentos de un usuario ordenados por fecha descendente.
     */
    fun observeAttempts(uid: String): Flow<List<ExamAttempt>>

    suspend fun getAttempts(uid: String): List<ExamAttempt>
    
    /**
     * Obtiene un intento específico por su ID.
     */
    suspend fun getAttemptById(attemptId: String): ExamAttempt?
    
    suspend fun getAnswersForAttempt(attemptId: String): List<ExamAnswer>

    // Métodos legacy - mantener para compatibilidad
    suspend fun createAttempt(attempt: ExamAttempt)
    suspend fun upsertAnswer(answer: ExamAnswer)
}
