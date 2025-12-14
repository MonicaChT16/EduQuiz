# Guía Completa: Base de Datos Room - EduQuiz

## 📋 Índice
1. [Resumen de la Base de Datos](#resumen)
2. [Pasos para Crear la Base de Datos](#pasos)
3. [Estructura Completa de Tablas](#estructura)
4. [Configuración de Dependencias](#dependencias)
5. [Entidades (Entities)](#entidades)
6. [DAOs (Data Access Objects)](#daos)
7. [Módulo de Inyección de Dependencias](#inyeccion)
8. [Migraciones](#migraciones)
9. [Uso en el Código](#uso)

---

## 📊 Resumen de la Base de Datos {#resumen}

La base de datos Room de EduQuiz está completamente implementada y contiene:

- **10 Entidades** (tablas)
- **6 DAOs** (interfaces de acceso a datos)
- **Versión actual**: 2
- **Nombre de la base de datos**: `eduquiz.db`
- **Ubicación**: `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`

---

## 🚀 Pasos para Crear la Base de Datos {#pasos}

### Paso 1: Agregar Dependencias de Room

En `android/data/build.gradle.kts`:

```kotlin
dependencies {
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
}
```

Y en `android/gradle/libs.versions.toml`:

```toml
[versions]
room = "2.7.0-alpha10"

[libraries]
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
```

### Paso 2: Configurar KAPT para Room

En `android/data/build.gradle.kts`:

```kotlin
kapt {
    correctErrorTypes = true
    useBuildCache = true
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}
```

### Paso 3: Crear las Entidades (Entities)

Cada entidad representa una tabla en la base de datos. Ver sección [Entidades](#entidades).

### Paso 4: Crear los DAOs

Cada DAO define las operaciones de base de datos. Ver sección [DAOs](#daos).

### Paso 5: Crear la Clase Database

La clase `AppDatabase` extiende `RoomDatabase` y define todas las entidades y DAOs.

### Paso 6: Configurar Inyección de Dependencias

Crear el módulo Hilt para proporcionar la instancia de la base de datos. Ver sección [Inyección](#inyeccion).

---

## 🗄️ Estructura Completa de Tablas {#estructura}

### 1. **pack_entity** - Packs de Exámenes
- `packId` (PK): String
- `weekLabel`: String
- `status`: String (ACTIVE/DOWNLOADED/PUBLISHED)
- `publishedAt`: Long
- `downloadedAt`: Long

### 2. **text_entity** - Textos de Lectura
- `textId` (PK): String
- `packId` (FK): String → pack_entity
- `title`: String
- `body`: String
- `subject`: String (LECTURA/MATEMATICA/CIENCIAS)

### 3. **question_entity** - Preguntas
- `questionId` (PK): String
- `packId` (FK): String → pack_entity
- `textId` (FK): String → text_entity
- `prompt`: String
- `correctOptionId`: String
- `difficulty`: Int
- `explanationText`: String? (nullable)
- `explanationStatus`: String (NONE/GENERATED/APPROVED)

### 4. **option_entity** - Opciones de Respuesta
- `questionId` (PK parte): String → question_entity
- `optionId` (PK parte): String
- `text`: String

### 5. **user_profile_entity** - Perfil de Usuario
- `uid` (PK): String
- `displayName`: String
- `photoUrl`: String? (nullable)
- `schoolId`: String
- `classroomId`: String
- `coins`: Int
- `xp`: Long (puntos de experiencia)
- `selectedCosmeticId`: String? (nullable)
- `updatedAtLocal`: Long
- `syncState`: String (PENDING/SYNCED)

### 6. **inventory_entity** - Inventario de Cosméticos
- `uid` (PK parte): String → user_profile_entity
- `cosmeticId` (PK parte): String
- `purchasedAt`: Long

### 7. **achievement_entity** - Logros Desbloqueados
- `uid` (PK parte): String → user_profile_entity
- `achievementId` (PK parte): String
- `unlockedAt`: Long

### 8. **daily_streak_entity** - Racha Diaria
- `uid` (PK): String → user_profile_entity
- `currentStreak`: Int
- `lastLoginDate`: String (YYYY-MM-DD)
- `updatedAtLocal`: Long
- `syncState`: String

### 9. **exam_attempt_entity** - Intentos de Examen
- `attemptId` (PK): String
- `uid` (FK): String → user_profile_entity
- `packId` (FK): String → pack_entity
- `startedAtLocal`: Long
- `finishedAtLocal`: Long? (nullable)
- `durationMs`: Long
- `status`: String (IN_PROGRESS/COMPLETED/AUTO_SUBMIT/CANCELLED_CHEAT)
- `scoreRaw`: Int
- `scoreValidated`: Int? (nullable)
- `origin`: String (OFFLINE/ONLINE)
- `syncState`: String (PENDING/SYNCED/FAILED)

### 10. **exam_answer_entity** - Respuestas de Examen
- `attemptId` (PK parte): String → exam_attempt_entity
- `questionId` (PK parte): String → question_entity
- `selectedOptionId`: String
- `isCorrect`: Boolean
- `timeSpentMs`: Long

---

## 📦 Configuración de Dependencias {#dependencias}

### Versiones Utilizadas

```toml
[versions]
room = "2.7.0-alpha10"
kotlin = "2.0.21"
hilt = "2.52"
```

### Dependencias en build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

dependencies {
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    
    // Hilt para inyección de dependencias
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

---

## 🏗️ Entidades (Entities) {#entidades}

### Ubicación
`android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`

### Ejemplo de Entidad Completa

```kotlin
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
    val startedAtLocal: Long,
    val finishedAtLocal: Long?,
    val durationMs: Long,
    val status: String,
    val scoreRaw: Int,
    val scoreValidated: Int?,
    val origin: String,
    val syncState: String,
)
```

### Todas las Entidades

1. `PackEntity` - Packs de exámenes
2. `TextEntity` - Textos de lectura
3. `QuestionEntity` - Preguntas
4. `OptionEntity` - Opciones de respuesta
5. `UserProfileEntity` - Perfil de usuario
6. `InventoryEntity` - Inventario
7. `AchievementEntity` - Logros
8. `DailyStreakEntity` - Racha diaria
9. `ExamAttemptEntity` - Intentos de examen
10. `ExamAnswerEntity` - Respuestas

---

## 🔌 DAOs (Data Access Objects) {#daos}

### Ubicación
`android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`

### DAOs Implementados

#### 1. **PackDao** - Operaciones con Packs
- `insert(pack: PackEntity)`
- `insertAll(packs: List<PackEntity>)`
- `findById(packId: String): PackEntity?`
- `updateStatus(packId: String, status: String)`
- `observeByStatus(status: String): Flow<PackEntity?>`
- `markAsActive(packId: String)` (transacción)

#### 2. **ContentDao** - Operaciones con Contenido
- `insertTexts(texts: List<TextEntity>)`
- `insertQuestions(questions: List<QuestionEntity>)`
- `insertOptions(options: List<OptionEntity>)`
- `getTextsByPack(packId: String): List<TextEntity>`
- `getQuestionsByPack(packId: String): List<QuestionEntity>`
- `getOptionsByQuestion(questionId: String): List<OptionEntity>`

#### 3. **ProfileDao** - Operaciones con Perfil
- `upsertProfile(entity: UserProfileEntity)`
- `observeProfile(uid: String): Flow<UserProfileEntity?>`
- `updateCoins(uid: String, delta: Int, updatedAtLocal: Long, syncState: String)`
- `updateXp(uid: String, delta: Long, updatedAtLocal: Long, syncState: String)`
- `updateSelectedCosmetic(...)`
- `updatePhotoUrl(...)`
- `upsertDailyStreak(entity: DailyStreakEntity)`
- `observeDailyStreak(uid: String): Flow<DailyStreakEntity?>`

#### 4. **StoreDao** - Operaciones con Tienda
- `insertInventoryItem(item: InventoryEntity)`
- `hasInventoryItem(uid: String, cosmeticId: String): Boolean`
- `getInventory(uid: String): List<InventoryEntity>`

#### 5. **AchievementsDao** - Operaciones con Logros
- `insertAchievement(item: AchievementEntity)`
- `getAchievements(uid: String): List<AchievementEntity>`

#### 6. **ExamDao** - Operaciones con Exámenes
- `insertAttempt(attempt: ExamAttemptEntity)`
- `upsertAnswer(answer: ExamAnswerEntity)`
- `finishAttempt(...)` - Finaliza un intento y calcula score
- `getAttempts(uid: String): List<ExamAttemptEntity>`
- `getAttemptById(attemptId: String): ExamAttemptEntity?`
- `observeAttempts(uid: String): Flow<List<ExamAttemptEntity>>`
- `getAnswers(attemptId: String): List<ExamAnswerEntity>`
- `getCorrectOptionId(questionId: String): String?`
- `getAttemptsBySyncState(syncState: String): List<ExamAttemptEntity>`
- `updateSyncState(attemptId: String, syncState: String)`

---

## 💉 Módulo de Inyección de Dependencias {#inyeccion}

### Ubicación
`android/data/src/main/java/com/eduquiz/data/di/DatabaseModule.kt`

### Configuración

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.NAME
        )
            .addMigrations(*AppDatabase.MIGRATIONS)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun providePackDao(db: AppDatabase): PackDao = db.packDao()
    
    @Provides
    fun provideContentDao(db: AppDatabase): ContentDao = db.contentDao()
    
    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()
    
    @Provides
    fun provideStoreDao(db: AppDatabase): StoreDao = db.storeDao()
    
    @Provides
    fun provideAchievementsDao(db: AppDatabase): AchievementsDao = db.achievementsDao()
    
    @Provides
    fun provideExamDao(db: AppDatabase): ExamDao = db.examDao()
}
```

---

## 🔄 Migraciones {#migraciones}

### Versión Actual: 2

### Migración 1 → 2

Agregó el campo `xp` (puntos de experiencia) a `user_profile_entity`:

```kotlin
Migration(1, 2) { database ->
    database.execSQL("ALTER TABLE user_profile_entity ADD COLUMN xp INTEGER NOT NULL DEFAULT 0")
}
```

### Cómo Agregar una Nueva Migración

1. Incrementar la versión en `@Database(version = X)`
2. Agregar la migración en `AppDatabase.MIGRATIONS`:

```kotlin
@Database(
    entities = [...],
    version = 3, // Incrementar versión
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        val MIGRATIONS: Array<Migration> = arrayOf(
            Migration(1, 2) { database ->
                // Migración anterior
            },
            Migration(2, 3) { database ->
                // Nueva migración
                database.execSQL("ALTER TABLE ...")
            }
        )
    }
}
```

---

## 💻 Uso en el Código {#uso}

### Inyectar un DAO

```kotlin
@HiltViewModel
class ExamViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    // El DAO se inyecta automáticamente a través del repositorio
) : ViewModel() {
    // ...
}
```

### Ejemplo de Uso del Repositorio

```kotlin
// Iniciar un intento
val attemptId = examRepository.startAttempt(
    uid = "user123",
    packId = "pack-1",
    startedAtLocal = System.currentTimeMillis(),
    durationMs = 20 * 60 * 1000L // 20 minutos
)

// Enviar una respuesta
examRepository.submitAnswer(
    attemptId = attemptId,
    questionId = "q1",
    optionId = "opt-A",
    timeSpentMs = 5000L
)

// Finalizar el intento
examRepository.finishAttempt(
    attemptId = attemptId,
    finishedAtLocal = System.currentTimeMillis(),
    status = ExamStatus.COMPLETED
)

// Obtener respuestas
val answers = examRepository.getAnswersForAttempt(attemptId)
```

### Mappers (Conversión Entity ↔ Domain)

Ubicación: `android/data/src/main/java/com/eduquiz/data/repository/DbMappers.kt`

Los mappers convierten entre:
- **Entity** (capa de datos) ↔ **Domain Model** (capa de dominio)

Ejemplo:
```kotlin
// Entity → Domain
val domainAttempt = examAttemptEntity.toDomain()

// Domain → Entity
val entity = examAttempt.toEntity()
```

---

## ✅ Verificación de la Base de Datos

### Estado Actual

✅ **Base de datos creada y configurada**
✅ **10 entidades definidas**
✅ **6 DAOs implementados**
✅ **Migraciones configuradas**
✅ **Inyección de dependencias configurada**
✅ **Mappers implementados**
✅ **Repositorios funcionando**

### Archivos Principales

1. **AppDatabase.kt** - Definición de la base de datos
   - `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`

2. **DatabaseModule.kt** - Configuración de Hilt
   - `android/data/src/main/java/com/eduquiz/data/di/DatabaseModule.kt`

3. **DbMappers.kt** - Conversión Entity ↔ Domain
   - `android/data/src/main/java/com/eduquiz/data/repository/DbMappers.kt`

4. **Repositorios** - Implementación de lógica de negocio
   - `android/data/src/main/java/com/eduquiz/data/repository/ExamRepositoryImpl.kt`
   - Otros repositorios...

---

## 📝 Notas Importantes

1. **Exportación de Schema**: La base de datos tiene `exportSchema = true`, lo que genera archivos JSON en `android/data/schemas/` para documentación.

2. **Fallback Destructivo**: Actualmente configurado con `fallbackToDestructiveMigration(dropAllTables = true)`. En producción, esto debería cambiarse para usar solo migraciones.

3. **Foreign Keys**: Todas las relaciones están definidas con claves foráneas y acciones CASCADE o NO_ACTION según corresponda.

4. **Índices**: Se han agregado índices en campos frecuentemente consultados para mejorar el rendimiento.

5. **SyncState**: Todas las entidades que se sincronizan con Firebase tienen un campo `syncState` para rastrear el estado de sincronización.

---

## 🔧 Comandos Útiles

### Ver el Schema de la Base de Datos

Los schemas se generan automáticamente en:
```
android/data/schemas/com.eduquiz.data.db.AppDatabase/
```

### Limpiar y Reconstruir

```bash
./gradlew clean build
```

### Verificar Compilación

```bash
./gradlew :data:compileDebugKotlin
```

---

## 📚 Recursos Adicionales

- [Documentación oficial de Room](https://developer.android.com/training/data-storage/room)
- [Guía de migraciones de Room](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Hilt para inyección de dependencias](https://developer.android.com/training/dependency-injection/hilt-android)

---

**Última actualización**: Base de datos versión 2
**Estado**: ✅ Completamente funcional













