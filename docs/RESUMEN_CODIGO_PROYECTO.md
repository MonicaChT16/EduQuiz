# 📊 Resumen Ejecutivo del Código - EduQuiz

## 🎯 Visión General del Proyecto

**EduQuiz** es una plataforma de simulacros tipo PISA para Android que permite a los estudiantes:
- Descargar packs semanales de preguntas
- Realizar exámenes offline
- Ver rankings y logros
- Sincronizar resultados con Firestore

---

## 🏗️ Arquitectura del Proyecto

### Estructura de Módulos

```
android/
├── app/                    # Módulo principal de la aplicación
├── core/                   # Utilidades compartidas
├── data/                   # Capa de datos (Room + Firestore)
├── domain/                 # Lógica de negocio
└── feature-*/              # Módulos de características
    ├── feature-auth/       # Autenticación
    ├── feature-exam/       # Exámenes
    ├── feature-pack/       # Gestión de packs
    ├── feature-profile/    # Perfil de usuario
    ├── feature-ranking/    # Rankings
    └── feature-store/      # Tienda de cosméticos
```

### Patrón Arquitectónico

**Clean Architecture** con separación de capas:
- **Data Layer**: Room (local) + Firestore (remoto)
- **Domain Layer**: Casos de uso y modelos de negocio
- **Presentation Layer**: ViewModels y UI (Compose)

---

## 🗄️ Base de Datos

### Base de Datos Local (Room)

**Archivo**: `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`

**Versión**: 6

**Entidades** (11 tablas):

1. **PackEntity** - Packs semanales
   - `packId`, `weekLabel`, `status`, `publishedAt`, `downloadedAt`

2. **TextEntity** - Textos de lectura
   - `textId`, `packId`, `title`, `body`, `subject`
   - Índices: `packId`, `(packId, subject)`

3. **QuestionEntity** - Preguntas
   - `questionId`, `packId`, `textId`, `prompt`, `correctOptionId`, `difficulty`
   - Índices: `packId`, `textId`, `(packId, textId)`

4. **OptionEntity** - Opciones de respuesta
   - `questionId`, `optionId`, `text`
   - Clave primaria compuesta: `(questionId, optionId)`

5. **UserProfileEntity** - Perfiles de usuario
   - `uid`, `displayName`, `photoUrl`, `ugelCode`, `coins`, `xp`, `selectedCosmeticId`

6. **InventoryEntity** - Inventario de cosméticos
   - `uid`, `cosmeticId`, `purchasedAt`

7. **AchievementEntity** - Logros desbloqueados
   - `uid`, `achievementId`, `unlockedAt`

8. **DailyStreakEntity** - Racha diaria
   - `uid`, `currentStreak`, `lastLoginDate`

9. **ExamAttemptEntity** - Intentos de examen
   - `attemptId`, `uid`, `packId`, `subject`, `startedAtLocal`, `finishedAtLocal`, `status`, `scoreRaw`

10. **ExamAnswerEntity** - Respuestas de exámenes
    - `attemptId`, `questionId`, `selectedOptionId`, `isCorrect`, `timeSpentMs`

11. **OnboardingPreferencesEntity** - Preferencias de onboarding
    - `id`, `hasCompletedOnboarding`

**DAOs** (7 interfaces):

- `PackDao` - Gestión de packs
- `ContentDao` - Textos, preguntas y opciones
- `ProfileDao` - Perfiles y rachas
- `StoreDao` - Inventario
- `AchievementsDao` - Logros
- `ExamDao` - Intentos y respuestas
- `OnboardingDao` - Preferencias de onboarding

**Migraciones**:
- `1→2`: Agregar campo `xp` a `user_profile_entity`
- `2→3`: Agregar `subject` a `exam_attempt_entity` y crear `onboarding_preferences_entity`
- `3→4`: Agregar `ugelCode` a `user_profile_entity`
- `4→5`: Eliminar `schoolId` y `classroomId` de `user_profile_entity`
- `5→6`: Agregar índices compuestos para optimización

### Base de Datos Remota (Firestore)

**Proyecto**: `eduquiz-e2829`

**Colecciones principales**:

1. **`packs`** - Packs publicados
   ```json
   {
     "packId": "pack_2025_w01",
     "weekLabel": "2025-W01",
     "status": "PUBLISHED",
     "publishedAt": 1234567890,
     "textIds": ["txt_2025_w01_001", ...],
     "questionIds": ["q_2025_w01_0001", ...]
   }
   ```

2. **`texts`** - Textos de lectura
   ```json
   {
     "textId": "txt_2025_w01_001",
     "packId": "pack_2025_w01",
     "title": "Título del texto",
     "body": "Contenido...",
     "subject": "COMPRENSION_LECTORA"
   }
   ```

3. **`questions`** - Preguntas con opciones
   ```json
   {
     "questionId": "q_2025_w01_0001",
     "textId": "txt_2025_w01_001",
     "packId": "pack_2025_w01",
     "prompt": "¿Pregunta?",
     "correctOptionId": "B",
     "difficulty": 2,
     "options": [
       { "optionId": "A", "text": "Opción A" },
       { "optionId": "B", "text": "Opción B" },
       ...
     ]
   }
   ```

4. **`users/{uid}`** - Perfiles de usuario
   ```json
   {
     "uid": "user123",
     "displayName": "Nombre",
     "email": "email@example.com",
     "coins": 100,
     "xp": 500,
     "totalXp": 500,
     "averageAccuracy": 85.5,
     "totalAttempts": 10,
     ...
   }
   ```

5. **`users/{uid}/examAttempts/{attemptId}`** - Intentos de examen
   ```json
   {
     "attemptId": "attempt123",
     "uid": "user123",
     "packId": "pack_2025_w01",
     "status": "COMPLETED",
     "scoreRaw": 5,
     ...
   }
   ```

6. **`users/{uid}/examAttempts/{attemptId}/answers/{questionId}`** - Respuestas
   ```json
   {
     "questionId": "q_2025_w01_0001",
     "selectedOptionId": "B",
     "isCorrect": true,
     "timeSpentMs": 5000
   }
   ```

---

## 🔄 Flujo de Sincronización

### Descarga de Packs (Firestore → Room)

1. App busca packs con `status = "PUBLISHED"` en Firestore
2. Usuario selecciona un pack para descargar
3. App descarga:
   - Pack metadata
   - Textos relacionados (`textIds`)
   - Preguntas relacionadas (`questionIds`)
   - Opciones de cada pregunta
4. Todo se guarda en Room con transacción atómica
5. Pack se marca como `ACTIVE` en Room

### Sincronización de Resultados (Room → Firestore)

**Servicio**: `FirestoreSyncService`

**Datos sincronizados**:

1. **Perfiles de usuario** (`syncUserProfile`)
   - Ruta: `users/{uid}`
   - Regla: Última escritura gana (comparación de `updatedAtLocal`)
   - Incluye: Datos básicos, métricas de ranking, coins, XP

2. **Intentos de examen** (`syncExamAttempt`)
   - Ruta: `users/{uid}/examAttempts/{attemptId}`
   - Regla: Merge (nunca borrar, solo actualizar)
   - Incluye: Metadata del intento + respuestas como subcolección

**Workers** (sincronización automática):

- `SyncAllUsersWorker` - Sincroniza todos los usuarios periódicamente
- `PackUpdateWorker` - Verifica nuevos packs disponibles

---

## 📦 Módulos Principales

### 1. Data Module (`android/data/`)

**Responsabilidades**:
- Definición de entidades Room
- Implementación de DAOs
- Repositorios (implementación de interfaces del domain)
- Sincronización con Firestore
- Mappers (Entity ↔ Domain)

**Archivos clave**:
- `AppDatabase.kt` - Definición de BD y entidades
- `DatabaseModule.kt` - Configuración Hilt
- `FirestoreSyncService.kt` - Sincronización
- `PackRemoteDataSource.kt` - Descarga de packs desde Firestore
- `DbMappers.kt` - Conversión Entity ↔ Domain

### 2. Domain Module (`android/domain/`)

**Responsabilidades**:
- Modelos de dominio (sin dependencias de Android)
- Interfaces de repositorios
- Lógica de negocio (AchievementEngine, StreakService)

**Modelos principales**:
- `Pack`, `PackMeta`, `Question`, `Option`, `TextContent`
- `UserProfile`, `ExamAttempt`, `ExamAnswer`
- `Achievement`, `Cosmetic`

**Repositorios**:
- `PackRepository` - Gestión de packs
- `ProfileRepository` - Perfiles de usuario
- `ExamRepository` - Intentos de examen
- `RankingRepository` - Rankings
- `StoreRepository` - Tienda

### 3. Feature Modules

#### feature-auth
- Autenticación con Firebase Auth
- `AuthViewModel` - Estado de autenticación

#### feature-exam
- Pantalla de examen
- `ExamViewModel` - Lógica del examen
- `ExamModels.kt` - Estados y modelos de UI

#### feature-profile
- Perfil de usuario
- Visualización de logros y estadísticas

#### feature-ranking
- Leaderboard
- Rankings por UGEL, global, etc.

#### feature-store
- Tienda de cosméticos
- Compra con coins

---

## 🔧 Tecnologías Utilizadas

### Backend/Datos
- **Room** - Base de datos local SQLite
- **Firestore** - Base de datos en la nube
- **Firebase Auth** - Autenticación
- **WorkManager** - Tareas en segundo plano

### Arquitectura
- **Hilt** - Inyección de dependencias
- **Coroutines** - Programación asíncrona
- **Flow** - Streams reactivos

### UI
- **Jetpack Compose** - UI moderna
- **Material Design 3** - Diseño

### Testing
- **JUnit** - Tests unitarios
- **Room Testing** - Tests de base de datos

---

## 📝 Scripts Disponibles

Ubicación: `scripts/`

1. **`init-firestore.js`**
   - Crea datos de prueba iniciales
   - 1 pack, 3 textos, 6 preguntas

2. **`update-firestore-subjects.js`**
   - Normaliza valores de `subject` en textos

3. **`verify-firestore.js`**
   - Verifica estructura y datos en Firestore

4. **`init-users-firestore.js`**
   - Crea usuarios de prueba

5. **`init-users-data-firestore.js`**
   - Crea datos de prueba para usuarios

---

## 🔐 Configuración Requerida

### Firebase
- `google-services.json` en `android/app/`
- `serviceAccountKey.json` en la raíz del proyecto (para scripts)

### Dependencias
- Node.js y npm (para scripts)
- Android SDK
- Kotlin

---

## 📊 Estadísticas del Código

- **Módulos**: 7 (app, core, data, domain, 4 features)
- **Entidades Room**: 11
- **DAOs**: 7
- **Repositorios**: 5+
- **Versión BD**: 6
- **Migraciones**: 5

---

## 🚀 Flujo Típico de Uso

1. **Usuario inicia sesión** → Firebase Auth
2. **App busca packs disponibles** → Firestore
3. **Usuario descarga pack** → Firestore → Room
4. **Usuario inicia examen** → Room (offline)
5. **Usuario completa examen** → Guarda en Room
6. **App sincroniza resultados** → Room → Firestore
7. **App actualiza ranking** → Firestore → UI

---

## 📚 Documentación Adicional

Ver carpeta `docs/` para:
- Guías de configuración
- Soluciones a problemas comunes
- Diagramas de base de datos
- Flujos de sincronización

---

**Última actualización**: 2025-01-27
