package com.eduquiz.domain.streak

import com.eduquiz.domain.profile.DailyStreak
import com.eduquiz.domain.profile.ProfileRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Servicio para manejar la lógica de racha diaria.
 */
interface StreakService {
    /**
     * Actualiza la racha del usuario basándose en la fecha de hoy.
     * @param uid ID del usuario
     * @return El DailyStreak actualizado
     */
    suspend fun updateStreak(uid: String): DailyStreak
}















