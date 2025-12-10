package com.eduquiz.domain.achievements

/**
 * Definición de un logro.
 */
data class AchievementDefinition(
    val achievementId: String,
    val name: String,
    val description: String,
    val condition: AchievementCondition
)

/**
 * Condiciones para desbloquear logros.
 */
sealed class AchievementCondition {
    /**
     * Se desbloquea al completar el primer simulacro.
     */
    object FirstExamCompleted : AchievementCondition()

    /**
     * Se desbloquea al alcanzar una racha de días específica.
     */
    data class StreakReached(val days: Int) : AchievementCondition()

    /**
     * Se desbloquea al acumular un número de respuestas correctas.
     */
    data class CorrectAnswersAccumulated(val count: Int) : AchievementCondition()
}








