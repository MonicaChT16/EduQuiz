package com.eduquiz.data.repository

import com.eduquiz.domain.profile.DailyStreak
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.profile.SyncState
import com.eduquiz.domain.streak.StreakService
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class StreakServiceImpl @Inject constructor(
    private val profileRepository: ProfileRepository
) : StreakService {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private const val STREAK_BONUS_THRESHOLD = 3
        private const val STREAK_BONUS_COINS = 50
    }

    override suspend fun updateStreak(uid: String): DailyStreak {
        val today = LocalDate.now().format(DATE_FORMATTER)
        val existingStreak = try {
            profileRepository.observeDailyStreak(uid).firstOrNull()
        } catch (e: Exception) {
            android.util.Log.e("StreakService", "Error getting streak for $uid", e)
            null
        }
        
        val updatedStreak = when {
            existingStreak == null -> {
                // Primera vez que entra
                DailyStreak(
                    uid = uid,
                    currentStreak = 1,
                    lastLoginDate = today,
                    updatedAtLocal = System.currentTimeMillis(),
                    syncState = SyncState.PENDING
                )
            }
            existingStreak.lastLoginDate == today -> {
                // Mismo día, no cambia
                existingStreak
            }
            isNextDay(existingStreak.lastLoginDate, today) -> {
                // Día siguiente, incrementar racha
                val newStreak = existingStreak.currentStreak + 1
                DailyStreak(
                    uid = uid,
                    currentStreak = newStreak,
                    lastLoginDate = today,
                    updatedAtLocal = System.currentTimeMillis(),
                    syncState = SyncState.PENDING
                )
            }
            else -> {
                // Saltó días, reset a 1
                DailyStreak(
                    uid = uid,
                    currentStreak = 1,
                    lastLoginDate = today,
                    updatedAtLocal = System.currentTimeMillis(),
                    syncState = SyncState.PENDING
                )
            }
        }

        try {
            profileRepository.saveDailyStreak(updatedStreak)

            // Si alcanzó 3 días, otorgar bonus
            if (updatedStreak.currentStreak == STREAK_BONUS_THRESHOLD && existingStreak?.currentStreak != STREAK_BONUS_THRESHOLD) {
                try {
                    profileRepository.addCoins(
                        uid = uid,
                        delta = STREAK_BONUS_COINS,
                        reason = "streak_bonus",
                        updatedAtLocal = System.currentTimeMillis(),
                        syncState = SyncState.PENDING
                    )
                } catch (e: Exception) {
                    android.util.Log.e("StreakService", "Error adding coins for streak bonus", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StreakService", "Error saving streak for $uid", e)
            // Retornar el streak actualizado aunque falle la persistencia
        }

        return updatedStreak
    }

    private fun isNextDay(lastDate: String, today: String): Boolean {
        return try {
            val last = LocalDate.parse(lastDate, DATE_FORMATTER)
            val current = LocalDate.parse(today, DATE_FORMATTER)
            current == last.plusDays(1)
        } catch (e: Exception) {
            false
        }
    }
}

