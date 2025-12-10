package com.eduquiz.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eduquiz.data.db.AppDatabase
import com.eduquiz.data.db.ProfileDao
import com.eduquiz.data.db.UserProfileEntity
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.profile.ProfileRepositoryImpl
import com.eduquiz.domain.profile.SyncState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StreakServiceTest {

    private lateinit var database: AppDatabase
    private lateinit var profileDao: ProfileDao
    private lateinit var profileRepository: ProfileRepository
    private lateinit var streakService: StreakServiceImpl

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        profileDao = database.profileDao()
        profileRepository = ProfileRepositoryImpl(
            profileDao,
            database.storeDao(),
            database.achievementsDao()
        )
        streakService = StreakServiceImpl(profileRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun updateStreak_primeraVez_creaStreakDe1() = runTest {
        val uid = "user1"
        
        val streak = streakService.updateStreak(uid)
        
        assertEquals(1, streak.currentStreak)
        assertEquals(LocalDate.now().format(DATE_FORMATTER), streak.lastLoginDate)
        
        val saved = profileRepository.observeDailyStreak(uid).firstOrNull()
        assertNotNull(saved)
        assertEquals(1, saved.currentStreak)
    }

    @Test
    fun updateStreak_mismoDia_noCambia() = runTest {
        val uid = "user1"
        val today = LocalDate.now().format(DATE_FORMATTER)
        
        // Primera vez
        val firstStreak = streakService.updateStreak(uid)
        assertEquals(1, firstStreak.currentStreak)
        
        // Mismo día
        val secondStreak = streakService.updateStreak(uid)
        assertEquals(1, secondStreak.currentStreak)
        assertEquals(firstStreak.lastLoginDate, secondStreak.lastLoginDate)
    }

    @Test
    fun updateStreak_diaSiguiente_incrementaStreak() = runTest {
        val uid = "user1"
        val yesterday = LocalDate.now().minusDays(1).format(DATE_FORMATTER)
        
        // Crear perfil
        profileDao.upsertProfile(
            UserProfileEntity(
                uid = uid,
                displayName = "Test",
                photoUrl = null,
                schoolId = "school1",
                classroomId = "class1",
                coins = 0,
                selectedCosmeticId = null,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )
        
        // Crear streak inicial de ayer
        profileRepository.saveDailyStreak(
            com.eduquiz.domain.profile.DailyStreak(
                uid = uid,
                currentStreak = 1,
                lastLoginDate = yesterday,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )
        
        // Actualizar hoy (día siguiente)
        val updatedStreak = streakService.updateStreak(uid)
        
        assertEquals(2, updatedStreak.currentStreak)
        assertEquals(LocalDate.now().format(DATE_FORMATTER), updatedStreak.lastLoginDate)
    }

    @Test
    fun updateStreak_saltoDias_resetA1() = runTest {
        val uid = "user1"
        val twoDaysAgo = LocalDate.now().minusDays(2).format(DATE_FORMATTER)
        
        // Crear perfil
        profileDao.upsertProfile(
            UserProfileEntity(
                uid = uid,
                displayName = "Test",
                photoUrl = null,
                schoolId = "school1",
                classroomId = "class1",
                coins = 0,
                selectedCosmeticId = null,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )
        
        // Crear streak de hace 2 días
        profileRepository.saveDailyStreak(
            com.eduquiz.domain.profile.DailyStreak(
                uid = uid,
                currentStreak = 3,
                lastLoginDate = twoDaysAgo,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )
        
        // Actualizar hoy (saltó días)
        val updatedStreak = streakService.updateStreak(uid)
        
        assertEquals(1, updatedStreak.currentStreak) // Reset a 1
        assertEquals(LocalDate.now().format(DATE_FORMATTER), updatedStreak.lastLoginDate)
    }

    @Test
    fun updateStreak_alcanza3Dias_otorgaBonus() = runTest {
        val uid = "user1"
        val yesterday = LocalDate.now().minusDays(1).format(DATE_FORMATTER)
        
        // Crear perfil con 0 coins
        profileDao.upsertProfile(
            UserProfileEntity(
                uid = uid,
                displayName = "Test",
                photoUrl = null,
                schoolId = "school1",
                classroomId = "class1",
                coins = 0,
                selectedCosmeticId = null,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )
        
        // Crear streak de 2 días (ayer)
        profileRepository.saveDailyStreak(
            com.eduquiz.domain.profile.DailyStreak(
                uid = uid,
                currentStreak = 2,
                lastLoginDate = yesterday,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )
        
        // Actualizar hoy (alcanza 3 días)
        val updatedStreak = streakService.updateStreak(uid)
        
        assertEquals(3, updatedStreak.currentStreak)
        
        // Verificar que se otorgaron 50 coins de bonus
        val profile = profileRepository.observeProfile(uid).firstOrNull()
        assertNotNull(profile)
        assertEquals(50, profile.coins) // Bonus de 50 coins
    }
}










