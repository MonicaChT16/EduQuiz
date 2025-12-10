package com.eduquiz.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eduquiz.data.db.AppDatabase
import com.eduquiz.data.db.AchievementsDao
import com.eduquiz.data.db.ContentDao
import com.eduquiz.data.db.ExamDao
import com.eduquiz.data.db.ProfileDao
import com.eduquiz.data.db.UserProfileEntity
import com.eduquiz.domain.achievements.AchievementEvent
import com.eduquiz.domain.exam.ExamRepository
import com.eduquiz.data.repository.ExamRepositoryImpl
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.data.repository.ProfileRepositoryImpl
import com.eduquiz.domain.profile.SyncState
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementEngineTest {

    private lateinit var database: AppDatabase
    private lateinit var profileDao: ProfileDao
    private lateinit var examDao: ExamDao
    private lateinit var contentDao: ContentDao
    private lateinit var achievementsDao: AchievementsDao
    private lateinit var profileRepository: ProfileRepository
    private lateinit var examRepository: ExamRepository
    private lateinit var achievementEngine: AchievementEngineImpl

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        profileDao = database.profileDao()
        examDao = database.examDao()
        contentDao = database.contentDao()
        achievementsDao = database.achievementsDao()
        
        profileRepository = ProfileRepositoryImpl(
            profileDao,
            database.storeDao(),
            achievementsDao
        )
        examRepository = ExamRepositoryImpl(examDao, contentDao)
        achievementEngine = AchievementEngineImpl(profileRepository, examRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun evaluateAndUnlock_firstExam_unlocksFirstExamAchievement() = runTest {
        val uid = "user1"
        
        // Crear perfil
        profileDao.upsertProfile(
            UserProfileEntity(
                uid = uid,
                displayName = "Test",
                photoUrl = null,
                schoolId = "school1",
                classroomId = "class1",
                ugelCode = null,
                coins = 0,
                xp = 0L,
                selectedCosmeticId = null,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )
        
        // Simular completar primer examen
        achievementEngine.evaluateAndUnlock(uid, AchievementEvent.ExamCompleted)
        
        // Verificar que se desbloqueó el logro
        val achievements = profileRepository.getAchievements(uid)
        assertTrue(achievements.any { it.achievementId == "first_exam" })
    }

    @Test
    fun evaluateAndUnlock_streak3Days_unlocksStreakAchievement() = runTest {
        val uid = "user1"
        
        // Crear perfil
        profileDao.upsertProfile(
            UserProfileEntity(
                uid = uid,
                displayName = "Test",
                photoUrl = null,
                schoolId = "school1",
                classroomId = "class1",
                ugelCode = null,
                coins = 0,
                xp = 0L,
                selectedCosmeticId = null,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )
        
        // Simular alcanzar 3 días de racha
        achievementEngine.evaluateAndUnlock(
            uid,
            AchievementEvent.StreakUpdated(3)
        )
        
        // Verificar que se desbloqueó el logro
        val achievements = profileRepository.getAchievements(uid)
        assertTrue(achievements.any { it.achievementId == "streak_3_days" })
    }

    @Test
    fun evaluateAndUnlock_idempotente_noDuplicaLogros() = runTest {
        val uid = "user1"
        
        // Crear perfil
        profileDao.upsertProfile(
            UserProfileEntity(
                uid = uid,
                displayName = "Test",
                photoUrl = null,
                schoolId = "school1",
                classroomId = "class1",
                ugelCode = null,
                coins = 0,
                xp = 0L,
                selectedCosmeticId = null,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )
        
        // Evaluar primera vez
        achievementEngine.evaluateAndUnlock(uid, AchievementEvent.ExamCompleted)
        val firstCount = profileRepository.getAchievements(uid).size
        
        // Evaluar segunda vez (debería ser idempotente)
        achievementEngine.evaluateAndUnlock(uid, AchievementEvent.ExamCompleted)
        val secondCount = profileRepository.getAchievements(uid).size
        
        // No debería duplicarse
        assertEquals(firstCount, secondCount)
    }

    @Test
    fun evaluateAndUnlock_10CorrectAnswers_unlocksCorrectAnswersAchievement() = runTest {
        val uid = "user1"
        val packId = "pack1"
        
        // Crear perfil
        profileDao.upsertProfile(
            UserProfileEntity(
                uid = uid,
                displayName = "Test",
                photoUrl = null,
                schoolId = "school1",
                classroomId = "class1",
                ugelCode = null,
                coins = 0,
                xp = 0L,
                selectedCosmeticId = null,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )
        
        // Crear pack y preguntas para el test
        insertPackAndQuestions(packId, 10) // 10 preguntas
        
        // Crear intento y responder 10 correctas
        val attemptId = examRepository.startAttempt(uid, packId, System.currentTimeMillis(), 1000L)
        for (i in 1..10) {
            examRepository.submitAnswer(attemptId, "q$i", "optA", 1000L)
        }
        examRepository.finishAttempt(attemptId, System.currentTimeMillis(), "COMPLETED")
        
        // Evaluar logros
        achievementEngine.evaluateAndUnlock(uid, AchievementEvent.ExamCompleted)
        
        // Verificar que se desbloqueó el logro de 10 respuestas correctas
        val achievements = profileRepository.getAchievements(uid)
        assertTrue(achievements.any { it.achievementId == "correct_answers_10" })
    }

    private suspend fun insertPackAndQuestions(packId: String, count: Int) {
        database.packDao().insert(
            com.eduquiz.data.db.PackEntity(packId, "Week", "ACTIVE", 0L, 0L)
        )
        contentDao.insertTexts(listOf(
            com.eduquiz.data.db.TextEntity("text1", packId, "Title", "Body", "Subject")
        ))
        val questions = (1..count).map { i ->
            com.eduquiz.data.db.QuestionEntity(
                questionId = "q$i",
                packId = packId,
                textId = "text1",
                prompt = "Question $i",
                correctOptionId = "optA",
                difficulty = 1,
                explanationText = null,
                explanationStatus = "NONE"
            )
        }
        contentDao.insertQuestions(questions)
        questions.forEach { question ->
            contentDao.insertOptions(listOf(
                com.eduquiz.data.db.OptionEntity(question.questionId, "optA", "Correct"),
                com.eduquiz.data.db.OptionEntity(question.questionId, "optB", "Wrong")
            ))
        }
    }
}

