# Diagrama de la Base de Datos - EduQuiz

## 📊 Diagrama de Relaciones

```
┌─────────────────────────────────────────────────────────────────┐
│                    BASE DE DATOS: eduquiz.db                    │
│                         Versión: 2                              │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────┐
│   pack_entity       │
├─────────────────────┤
│ packId (PK)         │◄─────┐
│ weekLabel           │       │
│ status              │       │
│ publishedAt         │       │
│ downloadedAt        │       │
└─────────────────────┘       │
                              │
                              │ FK: packId
                              │
        ┌─────────────────────┴─────────────────────┐
        │                                           │
        │                                           │
┌───────▼────────┐                        ┌────────▼────────┐
│ text_entity    │                        │ question_entity │
├───────────────┤                        ├─────────────────┤
│ textId (PK)    │                        │ questionId (PK) │
│ packId (FK)    │                        │ packId (FK)     │
│ title          │                        │ textId (FK)     │◄──┐
│ body           │                        │ prompt          │   │
│ subject        │                        │ correctOptionId │   │
└────────────────┘                        │ difficulty      │   │
                                           │ explanationText │   │
                                           │ explanationStatus│   │
                                           └─────────────────┘   │
                                                                 │
                                                                 │ FK: questionId
                                                                 │
                                           ┌─────────────────────┘
                                           │
                                  ┌────────▼────────┐
                                  │ option_entity   │
                                  ├─────────────────┤
                                  │ questionId (PK) │
                                  │ optionId (PK)   │
                                  │ text            │
                                  └─────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    PERFIL Y USUARIO                             │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────────┐
│ user_profile_entity  │
├──────────────────────┤
│ uid (PK)             │◄─────┐
│ displayName          │       │
│ photoUrl             │       │
│ schoolId             │       │
│ classroomId          │       │
│ coins                │       │
│ xp                   │       │
│ selectedCosmeticId   │       │
│ updatedAtLocal       │       │
│ syncState            │       │
└──────────────────────┘       │
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        │ FK: uid              │ FK: uid              │ FK: uid
        │                      │                      │
┌───────▼────────┐    ┌────────▼────────┐    ┌───────▼──────────┐
│inventory_entity│    │achievement_entity│    │daily_streak_entity│
├────────────────┤    ├─────────────────┤    ├──────────────────┤
│ uid (PK)       │    │ uid (PK)        │    │ uid (PK)         │
│ cosmeticId (PK)│    │ achievementId (PK)│ │ currentStreak    │
│ purchasedAt    │    │ unlockedAt      │    │ lastLoginDate    │
└────────────────┘    └─────────────────┘    │ updatedAtLocal   │
                                             │ syncState        │
                                             └──────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    EXÁMENES Y RESPUESTAS                        │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────────┐
│ exam_attempt_entity  │
├──────────────────────┤
│ attemptId (PK)       │◄─────┐
│ uid (FK)             │       │
│ packId (FK)          │       │
│ startedAtLocal       │       │
│ finishedAtLocal      │       │
│ durationMs           │       │
│ status               │       │
│ scoreRaw             │       │
│ scoreValidated       │       │
│ origin               │       │
│ syncState            │       │
└──────────────────────┘       │
                               │
                               │ FK: attemptId
                               │
                  ┌────────────▼────────────┐
                  │ exam_answer_entity      │
                  ├─────────────────────────┤
                  │ attemptId (PK)          │
                  │ questionId (PK)         │
                  │ selectedOptionId        │
                  │ isCorrect               │
                  │ timeSpentMs             │
                  └─────────────────────────┘
```

## 🔗 Relaciones Detalladas

### 1. Packs y Contenido
```
pack_entity (1) ──< (N) text_entity
pack_entity (1) ──< (N) question_entity
text_entity (1) ──< (N) question_entity
question_entity (1) ──< (N) option_entity
```

### 2. Usuario y Perfil
```
user_profile_entity (1) ──< (N) inventory_entity
user_profile_entity (1) ──< (N) achievement_entity
user_profile_entity (1) ──< (1) daily_streak_entity
user_profile_entity (1) ──< (N) exam_attempt_entity
```

### 3. Exámenes
```
exam_attempt_entity (1) ──< (N) exam_answer_entity
pack_entity (1) ──< (N) exam_attempt_entity
question_entity (1) ──< (N) exam_answer_entity
```

## 📋 Tabla de Entidades

| Entidad | Tabla | Clave Primaria | Claves Foráneas |
|---------|-------|----------------|-----------------|
| PackEntity | pack_entity | packId | - |
| TextEntity | text_entity | textId | packId → pack_entity |
| QuestionEntity | question_entity | questionId | packId, textId |
| OptionEntity | option_entity | (questionId, optionId) | questionId |
| UserProfileEntity | user_profile_entity | uid | - |
| InventoryEntity | inventory_entity | (uid, cosmeticId) | uid |
| AchievementEntity | achievement_entity | (uid, achievementId) | uid |
| DailyStreakEntity | daily_streak_entity | uid | uid |
| ExamAttemptEntity | exam_attempt_entity | attemptId | uid, packId |
| ExamAnswerEntity | exam_answer_entity | (attemptId, questionId) | attemptId, questionId |

## 🔑 Índices

### pack_entity
- Ninguno (PK es suficiente)

### text_entity
- `packId` (para búsquedas por pack)

### question_entity
- `packId` (para búsquedas por pack)
- `textId` (para búsquedas por texto)

### option_entity
- `questionId` (para búsquedas por pregunta)

### user_profile_entity
- Ninguno (PK es suficiente)

### inventory_entity
- `uid` (para búsquedas por usuario)

### achievement_entity
- `uid` (para búsquedas por usuario)

### daily_streak_entity
- `uid` (para búsquedas por usuario)

### exam_attempt_entity
- `uid` (para búsquedas por usuario)
- `packId` (para búsquedas por pack)

### exam_answer_entity
- `attemptId` (para búsquedas por intento)
- `questionId` (para búsquedas por pregunta)

## 🎯 Acciones de Foreign Key

| Relación | Acción onDelete |
|----------|-----------------|
| text_entity → pack_entity | CASCADE |
| question_entity → pack_entity | CASCADE |
| question_entity → text_entity | CASCADE |
| option_entity → question_entity | CASCADE |
| inventory_entity → user_profile_entity | CASCADE |
| achievement_entity → user_profile_entity | CASCADE |
| daily_streak_entity → user_profile_entity | CASCADE |
| exam_attempt_entity → user_profile_entity | CASCADE |
| exam_attempt_entity → pack_entity | NO_ACTION |
| exam_answer_entity → exam_attempt_entity | CASCADE |
| exam_answer_entity → question_entity | NO_ACTION |

## 📊 Estadísticas de la Base de Datos

- **Total de tablas**: 10
- **Total de relaciones**: 11
- **Total de índices**: 8
- **Campos nullable**: 4 (photoUrl, selectedCosmeticId, finishedAtLocal, scoreValidated)
- **Claves primarias compuestas**: 3 (option_entity, inventory_entity, achievement_entity, exam_answer_entity)

## 🔄 Flujo de Datos Típico

### Crear un Examen
```
1. Usuario selecciona pack → pack_entity
2. Se cargan preguntas → question_entity, option_entity
3. Se crea intento → exam_attempt_entity
4. Usuario responde → exam_answer_entity
5. Se finaliza intento → exam_attempt_entity (actualizado)
6. Se calculan coins/XP → user_profile_entity (actualizado)
```

### Sincronización
```
1. Entidades con syncState = PENDING
2. Worker sincroniza con Firestore
3. syncState = SYNCED
```

---

**Nota**: Este diagrama representa la estructura actual de la base de datos Room versión 2.









