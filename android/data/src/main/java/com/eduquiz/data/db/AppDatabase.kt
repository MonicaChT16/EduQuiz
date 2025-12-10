package com.eduquiz.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import com.eduquiz.domain.exam.ExamStatus
import com.eduquiz.domain.pack.PackStatus
import com.eduquiz.domain.profile.SyncState
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pack_entity")
data class PackEntity(
    @PrimaryKey val packId: String,
    val weekLabel: String,
    val status: String,
    val publishedAt: Long,
    val downloadedAt: Long,
)

@Entity(
    tableName = "text_entity",
    foreignKeys = [
        ForeignKey(
            entity = PackEntity::class,
            parentColumns = ["packId"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("packId")]
)
data class TextEntity(
    @PrimaryKey val textId: String,
    val packId: String,
    val title: String,
    val body: String,
    val subject: String,
)

@Entity(
    tableName = "question_entity",
    foreignKeys = [
        ForeignKey(
            entity = PackEntity::class,
            parentColumns = ["packId"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TextEntity::class,
            parentColumns = ["textId"],
            childColumns = ["textId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("packId"), Index("textId")]
)
data class QuestionEntity(
    @PrimaryKey val questionId: String,
    val packId: String,
    val textId: String,
    val prompt: String,
    val correctOptionId: String,
    val difficulty: Int,
    val explanationText: String?,
    val explanationStatus: String,
)

@Entity(
    tableName = "option_entity",
    primaryKeys = ["questionId", "optionId"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["questionId"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("questionId")]
)
data class OptionEntity(
    val questionId: String,
    val optionId: String,
    val text: String,
)

@Entity(tableName = "user_profile_entity")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val photoUrl: String?,
    val schoolId: String,
    val classroomId: String,
    val ugelCode: String? = null, // Código UGEL ingresado por el usuario
    val coins: Int,
    val xp: Long = 0L, // Puntos de experiencia (acumulativo, nunca disminuye)
    val selectedCosmeticId: String?, // Nullable porque puede no tener cosmético equipado
    val updatedAtLocal: Long,
    val syncState: String,
)

@Entity(
    tableName = "inventory_entity",
    primaryKeys = ["uid", "cosmeticId"],
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["uid"],
            childColumns = ["uid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("uid")]
)
data class InventoryEntity(
    val uid: String,
    val cosmeticId: String,
    val purchasedAt: Long,
)

@Entity(
    tableName = "achievement_entity",
    primaryKeys = ["uid", "achievementId"],
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["uid"],
            childColumns = ["uid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("uid")]
)
data class AchievementEntity(
    val uid: String,
    val achievementId: String,
    val unlockedAt: Long,
)

@Entity(
    tableName = "daily_streak_entity",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["uid"],
            childColumns = ["uid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("uid")]
)
data class DailyStreakEntity(
    @PrimaryKey val uid: String,
    val currentStreak: Int,
    val lastLoginDate: String,
    val updatedAtLocal: Long,
    val syncState: String,
)

@Entity(
    tableName = "exam_attempt_entity",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["uid"],
            childColumns = ["uid"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PackEntity::class,
            parentColumns = ["packId"],
            childColumns = ["packId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("uid"), Index("packId")]
)
data class ExamAttemptEntity(
    @PrimaryKey val attemptId: String,
    val uid: String,
    val packId: String,
    val subject: String?,
    val startedAtLocal: Long,
    val finishedAtLocal: Long?,
    val durationMs: Long,
    val status: String,
    val scoreRaw: Int,
    val scoreValidated: Int?,
    val origin: String,
    val syncState: String,
)

@Entity(
    tableName = "exam_answer_entity",
    primaryKeys = ["attemptId", "questionId"],
    foreignKeys = [
        ForeignKey(
            entity = ExamAttemptEntity::class,
            parentColumns = ["attemptId"],
            childColumns = ["attemptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["questionId"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("attemptId"), Index("questionId")]
)
data class ExamAnswerEntity(
    val attemptId: String,
    val questionId: String,
    val selectedOptionId: String,
    val isCorrect: Boolean,
    val timeSpentMs: Long,
)

@Dao
interface PackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pack: PackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(packs: List<PackEntity>)

    @Query("SELECT * FROM pack_entity WHERE packId = :packId LIMIT 1")
    suspend fun findById(packId: String): PackEntity?

    @Query("UPDATE pack_entity SET status = :status WHERE packId = :packId")
    suspend fun updateStatus(packId: String, status: String)

    @Query("UPDATE pack_entity SET status = :newStatus WHERE status = :currentStatus")
    suspend fun updateStatusForCurrentStatus(currentStatus: String, newStatus: String)

    @Query(
        """
        UPDATE pack_entity 
        SET status = :newStatus 
        WHERE packId != :packId AND status = :currentStatus
        """
    )
    suspend fun updateStatusForOthers(packId: String, currentStatus: String, newStatus: String)

    @Transaction
    suspend fun markAsActive(packId: String) {
        updateStatusForCurrentStatus(PackStatus.ACTIVE, PackStatus.DOWNLOADED)
        updateStatus(packId, PackStatus.ACTIVE)
    }

    @Query("SELECT * FROM pack_entity WHERE status = :status LIMIT 1")
    fun observeByStatus(status: String = PackStatus.ACTIVE): Flow<PackEntity?>
}

@Dao
interface ContentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTexts(texts: List<TextEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<OptionEntity>)

    @Query("SELECT * FROM text_entity WHERE packId = :packId")
    suspend fun getTextsByPack(packId: String): List<TextEntity>

    @Query("SELECT * FROM question_entity WHERE textId = :textId")
    suspend fun getQuestionsByText(textId: String): List<QuestionEntity>

    @Query("SELECT * FROM question_entity WHERE packId = :packId")
    suspend fun getQuestionsByPack(packId: String): List<QuestionEntity>

    @Query("""
        SELECT q.* FROM question_entity q
        INNER JOIN text_entity t ON q.textId = t.textId
        WHERE q.packId = :packId AND t.subject = :subject
    """)
    suspend fun getQuestionsByPackAndSubject(packId: String, subject: String): List<QuestionEntity>

    @Query("SELECT * FROM option_entity WHERE questionId = :questionId")
    suspend fun getOptionsByQuestion(questionId: String): List<OptionEntity>
}

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: UserProfileEntity)

    @Query("SELECT * FROM user_profile_entity WHERE uid = :uid LIMIT 1")
    fun observeProfile(uid: String): Flow<UserProfileEntity?>

    @Query(
        """
        UPDATE user_profile_entity 
        SET coins = coins + :delta, updatedAtLocal = :updatedAtLocal, syncState = :syncState 
        WHERE uid = :uid
        """
    )
    suspend fun updateCoins(uid: String, delta: Int, updatedAtLocal: Long, syncState: String)

    @Query(
        """
        UPDATE user_profile_entity 
        SET xp = xp + :delta, updatedAtLocal = :updatedAtLocal, syncState = :syncState 
        WHERE uid = :uid
        """
    )
    suspend fun updateXp(uid: String, delta: Long, updatedAtLocal: Long, syncState: String)

    @Query(
        """
        UPDATE user_profile_entity 
        SET selectedCosmeticId = :cosmeticId, updatedAtLocal = :updatedAtLocal, syncState = :syncState 
        WHERE uid = :uid
        """
    )
    suspend fun updateSelectedCosmetic(uid: String, cosmeticId: String?, updatedAtLocal: Long, syncState: String)

    @Query(
        """
        UPDATE user_profile_entity 
        SET photoUrl = :photoUrl, updatedAtLocal = :updatedAtLocal, syncState = :syncState 
        WHERE uid = :uid
        """
    )
    suspend fun updatePhotoUrl(uid: String, photoUrl: String?, updatedAtLocal: Long, syncState: String)

    @Query(
        """
        UPDATE user_profile_entity 
        SET ugelCode = :ugelCode, updatedAtLocal = :updatedAtLocal, syncState = :syncState 
        WHERE uid = :uid
        """
    )
    suspend fun updateUgelCode(uid: String, ugelCode: String?, updatedAtLocal: Long, syncState: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyStreak(entity: DailyStreakEntity)

    @Query("SELECT * FROM daily_streak_entity WHERE uid = :uid LIMIT 1")
    fun observeDailyStreak(uid: String): Flow<DailyStreakEntity?>

    @Query("SELECT * FROM user_profile_entity WHERE syncState = :syncState")
    suspend fun getProfilesBySyncState(syncState: String): List<UserProfileEntity>
    
    @Query("SELECT * FROM user_profile_entity")
    suspend fun getAllProfiles(): List<UserProfileEntity>

    @Query(
        """
        UPDATE user_profile_entity 
        SET syncState = :syncState 
        WHERE uid = :uid
        """
    )
    suspend fun updateProfileSyncState(uid: String, syncState: String)
}

@Dao
interface StoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM inventory_entity WHERE uid = :uid AND cosmeticId = :cosmeticId)")
    suspend fun hasInventoryItem(uid: String, cosmeticId: String): Boolean

    @Query("SELECT * FROM inventory_entity WHERE uid = :uid")
    suspend fun getInventory(uid: String): List<InventoryEntity>
}

@Dao
interface AchievementsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(item: AchievementEntity)

    @Query("SELECT * FROM achievement_entity WHERE uid = :uid")
    suspend fun getAchievements(uid: String): List<AchievementEntity>
}

@Dao
interface ExamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: ExamAttemptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnswer(answer: ExamAnswerEntity)

    @Query(
        """
        UPDATE exam_attempt_entity 
        SET finishedAtLocal = :finishedAtLocal, status = :status, scoreRaw = :scoreRaw, 
            scoreValidated = :scoreValidated, syncState = :syncState
        WHERE attemptId = :attemptId
        """
    )
    suspend fun finishAttempt(
        attemptId: String,
        finishedAtLocal: Long,
        status: String = ExamStatus.COMPLETED,
        scoreRaw: Int,
        scoreValidated: Int?,
        syncState: String = SyncState.PENDING,
    )

    @Query("SELECT * FROM exam_attempt_entity WHERE uid = :uid ORDER BY startedAtLocal DESC")
    suspend fun getAttempts(uid: String): List<ExamAttemptEntity>

    @Query("SELECT * FROM exam_attempt_entity WHERE attemptId = :attemptId LIMIT 1")
    suspend fun getAttemptById(attemptId: String): ExamAttemptEntity?

    @Query("SELECT * FROM exam_attempt_entity WHERE uid = :uid ORDER BY startedAtLocal DESC")
    fun observeAttempts(uid: String): Flow<List<ExamAttemptEntity>>

    @Query("SELECT * FROM exam_answer_entity WHERE attemptId = :attemptId")
    suspend fun getAnswers(attemptId: String): List<ExamAnswerEntity>

    @Query("SELECT correctOptionId FROM question_entity WHERE questionId = :questionId LIMIT 1")
    suspend fun getCorrectOptionId(questionId: String): String?

    @Query("SELECT * FROM exam_attempt_entity WHERE syncState = :syncState")
    suspend fun getAttemptsBySyncState(syncState: String): List<ExamAttemptEntity>

    @Query(
        """
        UPDATE exam_attempt_entity 
        SET syncState = :syncState 
        WHERE attemptId = :attemptId
        """
    )
    suspend fun updateSyncState(attemptId: String, syncState: String)
}

@Database(
    entities = [
        PackEntity::class,
        TextEntity::class,
        QuestionEntity::class,
        OptionEntity::class,
        UserProfileEntity::class,
        InventoryEntity::class,
        AchievementEntity::class,
        DailyStreakEntity::class,
        ExamAttemptEntity::class,
        ExamAnswerEntity::class,
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packDao(): PackDao
    abstract fun contentDao(): ContentDao
    abstract fun profileDao(): ProfileDao
    abstract fun storeDao(): StoreDao
    abstract fun achievementsDao(): AchievementsDao
    abstract fun examDao(): ExamDao

    companion object {
        const val NAME = "eduquiz.db"
        val MIGRATIONS: Array<Migration> = arrayOf(
            Migration(1, 2) { database ->
                // Agregar campo XP a user_profile_entity
                database.execSQL("ALTER TABLE user_profile_entity ADD COLUMN xp INTEGER NOT NULL DEFAULT 0")
            },
            Migration(2, 3) { database ->
                // Agregar campo subject a exam_attempt_entity (nullable para compatibilidad con intentos antiguos)
                database.execSQL("ALTER TABLE exam_attempt_entity ADD COLUMN subject TEXT")
            },
            Migration(3, 4) { database ->
                // Agregar campo ugelCode a user_profile_entity (nullable para compatibilidad con perfiles antiguos)
                database.execSQL("ALTER TABLE user_profile_entity ADD COLUMN ugelCode TEXT")
            }
        )
    }
}
