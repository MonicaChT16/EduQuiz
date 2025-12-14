# 📊 Reporte de Base de Datos Room

## ✅ Estado General

La base de datos Room está **correctamente configurada** y lista para usar.

---

## 📋 Estructura de la Base de Datos

### Versión Actual: **2**

### Entidades (10 total)

#### 1. **PackEntity** (`pack_entity`)
- **Propósito**: Almacena información de los packs descargados
- **Campos**:
  - `packId` (PK): String
  - `weekLabel`: String
  - `status`: String (ACTIVE, DOWNLOADED, ARCHIVED)
  - `publishedAt`: Long
  - `downloadedAt`: Long

#### 2. **TextEntity** (`text_entity`)
- **Propósito**: Almacena textos de lectura asociados a packs
- **Campos**:
  - `textId` (PK): String
  - `packId`: String (FK → PackEntity)
  - `title`: String
  - `body`: String
  - `subject`: String
- **Índices**: `packId`

#### 3. **QuestionEntity** (`question_entity`)
- **Propósito**: Almacena preguntas asociadas a textos y packs
- **Campos**:
  - `questionId` (PK): String
  - `packId`: String (FK → PackEntity)
  - `textId`: String (FK → TextEntity)
  - `prompt`: String
  - `correctOptionId`: String
  - `difficulty`: Int
  - `explanationText`: String?
  - `explanationStatus`: String
- **Índices**: `packId`, `textId`

#### 4. **OptionEntity** (`option_entity`)
- **Propósito**: Almacena opciones de respuesta para preguntas
- **Campos**:
  - `questionId` (PK, FK → QuestionEntity): String
  - `optionId` (PK): String
  - `text`: String
- **Índices**: `questionId`

#### 5. **UserProfileEntity** (`user_profile_entity`)
- **Propósito**: Almacena perfil del usuario
- **Campos**:
  - `uid` (PK): String
  - `displayName`: String
  - `photoUrl`: String?
  - `schoolId`: String
  - `classroomId`: String
  - `coins`: Int
  - `xp`: Long (agregado en migración 1→2)
  - `selectedCosmeticId`: String?
  - `updatedAtLocal`: Long
  - `syncState`: String

#### 6. **InventoryEntity** (`inventory_entity`)
- **Propósito**: Almacena inventario de cosméticos del usuario
- **Campos**:
  - `uid` (PK, FK → UserProfileEntity): String
  - `cosmeticId` (PK): String
  - `purchasedAt`: Long
- **Índices**: `uid`

#### 7. **AchievementEntity** (`achievement_entity`)
- **Propósito**: Almacena logros desbloqueados por el usuario
- **Campos**:
  - `uid` (PK, FK → UserProfileEntity): String
  - `achievementId` (PK): String
  - `unlockedAt`: Long
- **Índices**: `uid`

#### 8. **DailyStreakEntity** (`daily_streak_entity`)
- **Propósito**: Almacena racha diaria del usuario
- **Campos**:
  - `uid` (PK, FK → UserProfileEntity): String
  - `currentStreak`: Int
  - `lastLoginDate`: String
  - `updatedAtLocal`: Long
  - `syncState`: String
- **Índices**: `uid`

#### 9. **ExamAttemptEntity** (`exam_attempt_entity`)
- **Propósito**: Almacena intentos de examen del usuario
- **Campos**:
  - `attemptId` (PK): String
  - `uid` (FK → UserProfileEntity): String
  - `packId` (FK → PackEntity): String
  - `startedAtLocal`: Long
  - `finishedAtLocal`: Long?
  - `durationMs`: Long
  - `status`: String
  - `scoreRaw`: Int
  - `scoreValidated`: Int?
  - `origin`: String
  - `syncState`: String
- **Índices**: `uid`, `packId`

#### 10. **ExamAnswerEntity** (`exam_answer_entity`)
- **Propósito**: Almacena respuestas individuales de cada intento
- **Campos**:
  - `attemptId` (PK, FK → ExamAttemptEntity): String
  - `questionId` (PK, FK → QuestionEntity): String
  - `selectedOptionId`: String
  - `isCorrect`: Boolean
  - `timeSpentMs`: Long
- **Índices**: `attemptId`, `questionId`

---

## 🔧 DAOs (Data Access Objects)

### 1. **PackDao**
- `insert(pack)`: Insertar pack
- `insertAll(packs)`: Insertar múltiples packs
- `findById(packId)`: Buscar pack por ID
- `updateStatus(packId, status)`: Actualizar estado
- `markAsActive(packId)`: Marcar como activo
- `observeByStatus(status)`: Observar pack por estado

### 2. **ContentDao**
- `insertTexts(texts)`: Insertar textos
- `insertQuestions(questions)`: Insertar preguntas
- `insertOptions(options)`: Insertar opciones
- `getTextsByPack(packId)`: Obtener textos de un pack
- `getQuestionsByText(textId)`: Obtener preguntas de un texto
- `getQuestionsByPack(packId)`: Obtener preguntas de un pack
- `getOptionsByQuestion(questionId)`: Obtener opciones de una pregunta

### 3. **ProfileDao**
- `upsertProfile(entity)`: Insertar/actualizar perfil
- `observeProfile(uid)`: Observar perfil
- `updateCoins(uid, delta, ...)`: Actualizar monedas
- `updateXp(uid, delta, ...)`: Actualizar experiencia
- `updateSelectedCosmetic(...)`: Actualizar cosmético seleccionado
- `updatePhotoUrl(...)`: Actualizar foto de perfil
- `upsertDailyStreak(entity)`: Insertar/actualizar racha diaria
- `observeDailyStreak(uid)`: Observar racha diaria

### 4. **StoreDao**
- `insertInventoryItem(item)`: Insertar item en inventario
- `hasInventoryItem(uid, cosmeticId)`: Verificar si tiene item
- `getInventory(uid)`: Obtener inventario completo

### 5. **AchievementsDao**
- `insertAchievement(item)`: Insertar logro
- `getAchievements(uid)`: Obtener logros del usuario

### 6. **ExamDao**
- `insertAttempt(attempt)`: Insertar intento
- `upsertAnswer(answer)`: Insertar/actualizar respuesta
- `finishAttempt(...)`: Finalizar intento
- `getAttempts(uid)`: Obtener intentos del usuario
- `getAttemptById(attemptId)`: Obtener intento por ID
- `observeAttempts(uid)`: Observar intentos
- `getAnswers(attemptId)`: Obtener respuestas de un intento
- `getCorrectOptionId(questionId)`: Obtener opción correcta

---

## 🔄 Migraciones

### Migración 1 → 2
- **Cambio**: Agregar campo `xp` a `user_profile_entity`
- **SQL**: `ALTER TABLE user_profile_entity ADD COLUMN xp INTEGER NOT NULL DEFAULT 0`

---

## ⚙️ Configuración

### Gradle (`android/data/build.gradle.kts`)
- ✅ Plugin `kotlin-kapt` habilitado
- ✅ `room.schemaLocation` configurado: `$projectDir/schemas`
- ✅ Dependencias Room:
  - `androidx.room:room-runtime:2.7.0-alpha10`
  - `androidx.room:room-ktx:2.7.0-alpha10`
  - `androidx.room:room-compiler:2.7.0-alpha10` (kapt)

### Versiones (`android/gradle/libs.versions.toml`)
- ✅ Room version: `2.7.0-alpha10`

### DatabaseModule (`android/data/src/main/java/com/eduquiz/data/di/DatabaseModule.kt`)
- ✅ Base de datos configurada como Singleton
- ✅ Migraciones aplicadas
- ✅ Fallback a migración destructiva habilitado
- ✅ Todos los DAOs proporcionados

---

## 📁 Esquemas Generados

Los esquemas de Room están generados en:
```
android/data/schemas/com.eduquiz.data.db.AppDatabase/
  - 1.json (versión 1)
  - 2.json (versión 2)
```

---

## ✅ Verificación

### Checklist de Configuración

- [x] **Entidades definidas**: 10 entidades
- [x] **DAOs implementados**: 6 DAOs
- [x] **Versión de base de datos**: 2
- [x] **Migraciones configuradas**: 1 migración (1→2)
- [x] **Schema export habilitado**: `exportSchema = true`
- [x] **kapt configurado**: `room.schemaLocation` configurado
- [x] **Dependencias Room**: Todas las 3 dependencias agregadas
- [x] **DatabaseModule**: Configurado con Hilt
- [x] **Esquemas generados**: Versiones 1 y 2

---

## 🚀 Estado Final

**✅ La base de datos Room está completamente configurada y lista para usar.**

Todos los componentes están en su lugar:
- Entidades definidas correctamente
- DAOs implementados
- Migraciones configuradas
- Dependencias agregadas
- Esquemas generados
- Módulo de inyección de dependencias configurado

---

## 📝 Notas

1. **Fallback Destructivo**: La base de datos está configurada con `fallbackToDestructiveMigration(dropAllTables = true)`. Esto significa que si hay un error en una migración, se eliminarán todas las tablas y se recrearán. Esto es útil para desarrollo, pero en producción deberías manejar las migraciones más cuidadosamente.

2. **Schema Export**: Los esquemas se exportan automáticamente a `android/data/schemas/` cuando compilas el proyecto.

3. **Versión Actual**: La base de datos está en la versión 2. Si necesitas hacer cambios, incrementa la versión y agrega una nueva migración.

---

**Última actualización**: Diciembre 2025











