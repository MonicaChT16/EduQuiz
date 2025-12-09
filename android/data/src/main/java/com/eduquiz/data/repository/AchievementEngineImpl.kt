package com.eduquiz.data.repository

import com.eduquiz.domain.achievements.AchievementCondition
import com.eduquiz.domain.achievements.AchievementDefinition
import com.eduquiz.domain.achievements.AchievementEngine
import com.eduquiz.domain.achievements.AchievementEvent
import com.eduquiz.domain.exam.ExamRepository
import com.eduquiz.domain.profile.Achievement
import com.eduquiz.domain.profile.ProfileRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Catálogo de logros disponibles.
 */
object AchievementCatalog {
    val ACHIEVEMENTS = listOf(
        AchievementDefinition(
            achievementId = "first_exam",
            name = "Primer Simulacro",
            description = "Completa tu primer simulacro",
            condition = AchievementCondition.FirstExamCompleted
        ),
        AchievementDefinition(
            achievementId = "streak_3_days",
            name = "3 Días de Racha",
            description = "Entra a la app 3 días seguidos",
            condition = AchievementCondition.StreakReached(3)
        ),
        AchievementDefinition(
            achievementId = "correct_answers_10",
            name = "10 Respuestas Correctas",
            description = "Acumula 10 respuestas correctas",
            condition = AchievementCondition.CorrectAnswersAccumulated(10)
        )
    )
}

class AchievementEngineImpl @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val examRepository: ExamRepository
) : AchievementEngine {

    override suspend fun evaluateAndUnlock(uid: String, event: AchievementEvent, eventData: Map<String, Any>) {
        try {
            val unlockedAchievements = profileRepository.getAchievements(uid)
            val unlockedIds = unlockedAchievements.map { it.achievementId }.toSet()

        for (achievementDef in AchievementCatalog.ACHIEVEMENTS) {
            // Si ya está desbloqueado, saltar (idempotente)
            if (achievementDef.achievementId in unlockedIds) {
                continue
            }

            val shouldUnlock = when (val condition = achievementDef.condition) {
                is AchievementCondition.FirstExamCompleted -> {
                    event == AchievementEvent.ExamCompleted
                }
                is AchievementCondition.StreakReached -> {
                    if (event is AchievementEvent.StreakUpdated) {
                        event.currentStreak >= condition.days
                    } else {
                        false
                    }
                }
                is AchievementCondition.CorrectAnswersAccumulated -> {
                    if (event == AchievementEvent.ExamCompleted) {
                        val totalCorrect = getTotalCorrectAnswers(uid)
                        totalCorrect >= condition.count
                    } else {
                        false
                    }
                }
            }

            if (shouldUnlock) {
                try {
                    unlockAchievement(uid, achievementDef.achievementId)
                } catch (e: Exception) {
                    android.util.Log.e("AchievementEngine", "Error unlocking achievement ${achievementDef.achievementId}", e)
                }
            }
        }
        } catch (e: Exception) {
            android.util.Log.e("AchievementEngine", "Error evaluating achievements for $uid", e)
        }
    }

    private suspend fun getTotalCorrectAnswers(uid: String): Int {
        return try {
            val attempts = examRepository.getAttempts(uid)
            attempts.sumOf { attempt ->
                try {
                    examRepository.getAnswersForAttempt(attempt.attemptId)
                        .count { it.isCorrect }
                } catch (e: Exception) {
                    android.util.Log.e("AchievementEngine", "Error getting answers for attempt ${attempt.attemptId}", e)
                    0
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AchievementEngine", "Error getting attempts for $uid", e)
            0
        }
    }

    private suspend fun unlockAchievement(uid: String, achievementId: String) {
        val achievement = Achievement(
            uid = uid,
            achievementId = achievementId,
            unlockedAt = System.currentTimeMillis()
        )
        profileRepository.unlockAchievement(achievement)
    }
}

