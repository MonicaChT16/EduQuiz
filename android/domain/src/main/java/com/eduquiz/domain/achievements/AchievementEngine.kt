package com.eduquiz.domain.achievements

import com.eduquiz.domain.profile.Achievement
import com.eduquiz.domain.profile.ProfileRepository

/**
 * Motor de logros que evalúa condiciones y desbloquea logros.
 */
interface AchievementEngine {
    /**
     * Evalúa condiciones basadas en eventos y desbloquea logros si se cumplen.
     * @param uid ID del usuario
     * @param event Tipo de evento que disparó la evaluación
     * @param eventData Datos adicionales del evento
     */
    suspend fun evaluateAndUnlock(uid: String, event: AchievementEvent, eventData: Map<String, Any> = emptyMap())
}

/**
 * Eventos que pueden disparar la evaluación de logros.
 */
sealed class AchievementEvent {
    object Login : AchievementEvent()
    object ExamCompleted : AchievementEvent()
    data class StreakUpdated(val currentStreak: Int) : AchievementEvent()
}















