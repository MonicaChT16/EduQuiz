# 🔍 Flujo Completo: Conexión a Base de Datos del Examen

## 📋 Resumen del Flujo

Este documento muestra **exactamente** dónde se conecta a la base de datos, qué código se ejecuta, y cómo se muestra en pantalla.

---

## 🎯 Punto de Entrada: Navegación a la Pantalla de Examen

### Archivo: `android/app/src/main/java/com/eduquiz/app/EduQuizNavHost.kt`

**Líneas 151-158**:
```kotlin
composable(RootDestination.Exam.route) {
    ExamFeature(
        uid = authUser.uid,  // ← Se pasa el UID del usuario autenticado
        modifier = Modifier.fillMaxSize(),
        onExit = {
            navController.popBackStack(RootDestination.Home.route, inclusive = false)
        }
    )
}
```

**Qué verificar aquí**:
- ✅ `authUser` no debe ser null
- ✅ `authUser.uid` debe tener un valor válido
- ✅ La navegación debe llegar a esta pantalla

**Cómo verificar**:
- Agrega un log: `Log.d("NavHost", "Navigating to Exam with uid: ${authUser.uid}")`

---

## 🔌 Paso 1: Inicialización del ViewModel

### Archivo: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamFeature.kt`

**Líneas 56-64**:
```kotlin
@Composable
fun ExamFeature(
    uid: String,  // ← Recibe el UID
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {},
    viewModel: ExamViewModel = hiltViewModel()  // ← ViewModel inyectado con Hilt
) {
    val state by viewModel.state.collectAsStateWithLifecycle()  // ← Observa el estado
    LaunchedEffect(uid) { viewModel.initialize(uid) }  // ← Inicializa cuando cambia el UID
```

**Qué verificar aquí**:
- ✅ El `uid` se pasa correctamente
- ✅ El ViewModel se crea correctamente (Hilt)
- ✅ El `LaunchedEffect` se ejecuta

**Cómo verificar**:
- Los logs deberían mostrar: `ExamViewModel: initialize called with uid: ...`

---

## 🗄️ Paso 2: Conexión a la Base de Datos - Obtener Pack Activo

### Archivo: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`

**Líneas 61-73**:
```kotlin
fun initialize(uid: String) {
    android.util.Log.d("ExamViewModel", "initialize called with uid: $uid")
    if (userId != null) {
        android.util.Log.d("ExamViewModel", "Already initialized with userId: $userId")
        return
    }
    userId = uid
    android.util.Log.d("ExamViewModel", "Setting userId to: $uid")
    viewModelScope.launch {
        android.util.Log.d("ExamViewModel", "Starting loadInitialState")
        loadInitialState()  // ← Aquí se conecta a la base de datos
    }
}
```

**Líneas 288-294**:
```kotlin
private suspend fun loadInitialState() {
    android.util.Log.d("ExamViewModel", "loadInitialState: Starting")
    _state.update { it.copy(stage = ExamStage.Loading, isBusy = true, errorMessage = null) }
    
    android.util.Log.d("ExamViewModel", "loadInitialState: Getting active pack from database")
    var pack = packRepository.getActivePack()  // ← 🔌 CONEXIÓN A BASE DE DATOS
    android.util.Log.d("ExamViewModel", "loadInitialState: Active pack = ${pack?.packId ?: "null"}")
```

### 🔗 Cadena de Conexión a la Base de Datos

#### 2.1. PackRepository.getActivePack()

**Archivo**: `android/data/src/main/java/com/eduquiz/data/repository/PackRepositoryImpl.kt`

**Líneas 127-128**:
```kotlin
override suspend fun getActivePack(): Pack? =
    packDao.findByStatus(PackStatus.ACTIVE)?.toDomain()  // ← Llama al DAO
```

#### 2.2. PackDao.findByStatus()

**Archivo**: `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`

**Líneas 271-272**:
```kotlin
@Query("SELECT * FROM pack_entity WHERE status = :status LIMIT 1")
suspend fun findByStatus(status: String = PackStatus.ACTIVE): PackEntity?  // ← 🔌 CONSULTA SQL A ROOM
```

**Esta es la consulta SQL real que se ejecuta**:
```sql
SELECT * FROM pack_entity WHERE status = 'ACTIVE' LIMIT 1
```

#### 2.3. AppDatabase (Room)

**Archivo**: `android/data/src/main/java/com/eduquiz/data/di/DatabaseModule.kt`

**Líneas 25-34**:
```kotlin
@Provides
@Singleton
fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.NAME  // ← "eduquiz.db"
    )
        .addMigrations(*AppDatabase.MIGRATIONS)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
```

**Qué verificar aquí**:
- ✅ La base de datos se crea correctamente
- ✅ El archivo `eduquiz.db` existe en el dispositivo
- ✅ La tabla `pack_entity` existe
- ✅ Hay un pack con `status = 'ACTIVE'`

**Cómo verificar**:
1. **Database Inspector** en Android Studio:
   ```sql
   SELECT * FROM pack_entity WHERE status = 'ACTIVE'
   ```
   Debe retornar exactamente 1 fila

2. **Logs**:
   ```
   ExamViewModel: loadInitialState: Active pack = pack-123
   ```
   Si muestra `null`, no hay pack activo en la base de datos

---

## 📺 Paso 3: Mostrar en Pantalla - Pack Activo

### Archivo: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamFeature.kt`

**Líneas 116-139**:
```kotlin
// Card del Pack Activo
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = state.pack?.weekLabel ?: "Sin pack activo",  // ← Muestra el pack o "Sin pack activo"
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "ID: ${state.pack?.packId ?: "--"}",  // ← Muestra el packId o "--"
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Preguntas: ${state.totalQuestions.takeIf { it > 0 } ?: "No disponibles"}",  // ← Muestra cantidad de preguntas
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
```

**Qué verificar en la pantalla**:
- ✅ Si muestra "Sin pack activo" → No hay pack en la base de datos
- ✅ Si muestra el `weekLabel` → El pack se cargó correctamente
- ✅ Si muestra "No disponibles" → El pack no tiene preguntas cargadas

---

## 🔍 Paso 4: Si No Hay Pack - Buscar en Firestore

### Archivo: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`

**Líneas 296-335**:
```kotlin
if (pack == null) {
    // Si no hay pack activo, buscar packs disponibles y descargar automáticamente
    val availablePack = runCatching { packRepository.fetchCurrentPackMeta() }.getOrNull()  // ← 🔌 CONEXIÓN A FIRESTORE
    
    if (availablePack != null) {
        // Descargar automáticamente el pack disponible
        _state.update { it.copy(isBusy = true, errorMessage = "Descargando pack...") }
        try {
            android.util.Log.d("ExamViewModel", "Auto-downloading pack: ${availablePack.packId}")
            pack = packRepository.downloadPack(availablePack.packId)  // ← Descarga y guarda en base de datos
            android.util.Log.d("ExamViewModel", "Pack downloaded successfully: ${pack.packId}")
            // Continuar con la carga normal ahora que tenemos el pack
        } catch (e: Exception) {
            android.util.Log.e("ExamViewModel", "Error auto-downloading pack", e)
            _state.update {
                it.copy(
                    stage = ExamStage.Start,
                    pack = null,
                    availablePack = availablePack,
                    questions = emptyList(),
                    isBusy = false,
                    errorMessage = "Error al descargar el pack. Intenta nuevamente."
                )
            }
            return
        }
    } else {
        // No hay pack disponible
        _state.update {
            it.copy(
                stage = ExamStage.Start,
                pack = null,
                availablePack = null,
                questions = emptyList(),
                isBusy = false,
                errorMessage = "No hay packs disponibles. Intenta refrescar."
            )
        }
        return
    }
}
```

### 🔗 Conexión a Firestore

**Archivo**: `android/data/src/main/java/com/eduquiz/data/remote/PackRemoteDataSource.kt`

**Líneas 61-98**:
```kotlin
override suspend fun fetchCurrentPackMeta(): PackMetaRemote? {
    return try {
        android.util.Log.d("PackRemoteDataSource", "=== INICIANDO CONSULTA A FIRESTORE ===")
        android.util.Log.d("PackRemoteDataSource", "Firestore app: ${firestore.app.name}")
        android.util.Log.d("PackRemoteDataSource", "Collection: $PACKS_COLLECTION")
        android.util.Log.d("PackRemoteDataSource", "Status filter: $STATUS_PUBLISHED")
        
        // 🔌 CONEXIÓN A FIRESTORE
        val snapshots = firestore.collection(PACKS_COLLECTION)  // ← "packs"
            .whereEqualTo("status", STATUS_PUBLISHED)  // ← "PUBLISHED"
            .get()
            .await()
            .documents

        android.util.Log.d("PackRemoteDataSource", "✅ Consulta completada. Found ${snapshots.size} published packs")

        // Ordenar por publishedAt descendente y tomar el más reciente
        val snapshot = snapshots
            .sortedByDescending { it.getLong("publishedAt") ?: 0L }
            .firstOrNull()

        if (snapshot == null) {
            android.util.Log.w("PackRemoteDataSource", "No published pack found")
            return null
        }

        val meta = snapshot.toPackMeta()
        android.util.Log.d("PackRemoteDataSource", "Successfully fetched pack meta: ${meta?.packId}")
        meta
    } catch (e: Exception) {
        android.util.Log.e("PackRemoteDataSource", "Error fetching current pack meta", e)
        android.util.Log.e("PackRemoteDataSource", "Error message: ${e.message}")
        android.util.Log.e("PackRemoteDataSource", "Error cause: ${e.cause?.message}")
        throw e
    }
}
```

**Qué verificar aquí**:
- ✅ Firestore está inicializado
- ✅ Hay packs con `status = "PUBLISHED"` en Firestore
- ✅ Las reglas de Firestore permiten lectura
- ✅ Hay conexión a internet

**Cómo verificar**:
1. **Firebase Console**:
   - Ve a Firestore Database → Datos
   - Busca la colección `packs`
   - Debe haber al menos un documento con `status = "PUBLISHED"`

2. **Logs**:
   ```
   PackRemoteDataSource: ✅ Consulta completada. Found X published packs
   ```
   Si muestra `Found 0`, no hay packs publicados en Firestore

---

## 📺 Paso 5: Mostrar en Pantalla - Pack Disponible

### Archivo: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamFeature.kt`

**Líneas 141-162**:
```kotlin
// Card del Pack Disponible (si no hay pack activo)
if (state.pack == null && state.availablePack != null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Pack disponible: ${state.availablePack.weekLabel}",  // ← Muestra pack disponible
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "ID: ${state.availablePack.packId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
```

**Qué verificar en la pantalla**:
- ✅ Si aparece esta card → Hay un pack disponible en Firestore pero no está descargado
- ✅ Debe mostrar el `weekLabel` y `packId` del pack disponible

---

## 📺 Paso 6: Mostrar Mensajes de Error

### Archivo: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamFeature.kt`

**Líneas 164-171**:
```kotlin
// Mensaje de error
if (state.errorMessage != null) {
    Text(
        text = state.errorMessage,  // ← Muestra el mensaje de error en ROJO
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}
```

**Mensajes de error posibles**:
- `"No hay pack activo. Por favor, descarga un pack primero."` → No hay pack en la base de datos
- `"No hay packs disponibles. Intenta refrescar."` → No hay packs en Firestore
- `"Error al descargar el pack. Intenta nuevamente."` → Error al descargar desde Firestore
- `"No hay preguntas disponibles para [Materia]..."` → No hay preguntas para esa materia

---

## 🔌 Paso 7: Cargar Preguntas desde Base de Datos

### Archivo: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`

**Líneas 338-351**:
```kotlin
android.util.Log.d("ExamViewModel", "loadInitialState: Preparing questions for pack ${pack.packId}")
val questions = runCatching { prepareQuestions(pack.packId) }
    .getOrElse { throwable ->
        android.util.Log.e("ExamViewModel", "loadInitialState: Error preparing questions", throwable)
        _state.update {
            it.copy(
                stage = ExamStage.Start,
                pack = pack,
                questions = emptyList(),
                isBusy = false,
                errorMessage = throwable.localizedMessage
                    ?: "No se pudieron cargar las preguntas."
            )
        }
        return
    }
```

**Líneas 409-460**:
```kotlin
private suspend fun prepareQuestions(packId: String, subject: String? = null): List<ExamContent> {
    android.util.Log.d("ExamViewModel", "prepareQuestions: packId=$packId, subject=$subject")
    
    // 🔌 CONEXIÓN A BASE DE DATOS - Obtener textos
    val texts = packRepository.getTextsForPack(packId).associateBy { it.textId }
    android.util.Log.d("ExamViewModel", "Found ${texts.size} texts for pack $packId")
    
    // 🔌 CONEXIÓN A BASE DE DATOS - Obtener preguntas
    val questions = if (subject != null) {
        val allQuestions = packRepository.getQuestionsForPackBySubject(packId, subject)  // ← Consulta por materia
        android.util.Log.d("ExamViewModel", "Found ${allQuestions.size} questions for subject $subject")
        allQuestions
            .sortedBy { it.questionId }
            .take(10)
    } else {
        val allQuestions = packRepository.getQuestionsForPack(packId)  // ← Consulta todas las preguntas
        android.util.Log.d("ExamViewModel", "Found ${allQuestions.size} questions for pack (no subject filter)")
        allQuestions.sortedBy { it.questionId }
    }
    
    // 🔌 CONEXIÓN A BASE DE DATOS - Obtener opciones para cada pregunta
    val result = questions.mapNotNull { question ->
        val text = texts[question.textId]
        if (text == null) {
            android.util.Log.e("ExamViewModel", "Missing text ${question.textId} for question ${question.questionId}")
            null
        } else {
            val options = packRepository.getOptionsForQuestion(question.questionId)  // ← Consulta opciones
            if (options.isEmpty()) {
                android.util.Log.w("ExamViewModel", "Question ${question.questionId} has no options")
            }
            ExamContent(question, text, options)
        }
    }
    
    android.util.Log.d("ExamViewModel", "Prepared ${result.size} exam contents (from ${questions.size} questions)")
    return result
}
```

### 🔗 Consultas SQL a la Base de Datos

#### 7.1. getTextsForPack()

**Archivo**: `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`

**Líneas 283-284**:
```kotlin
@Query("SELECT * FROM text_entity WHERE packId = :packId")
suspend fun getTextsByPack(packId: String): List<TextEntity>
```

**SQL ejecutado**:
```sql
SELECT * FROM text_entity WHERE packId = 'pack-123'
```

#### 7.2. getQuestionsForPackBySubject()

**Archivo**: `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`

**Líneas 292-297**:
```kotlin
@Query("""
    SELECT q.* FROM question_entity q
    INNER JOIN text_entity t ON q.textId = t.textId
    WHERE q.packId = :packId AND t.subject = :subject
""")
suspend fun getQuestionsByPackAndSubject(packId: String, subject: String): List<QuestionEntity>
```

**SQL ejecutado**:
```sql
SELECT q.* FROM question_entity q
INNER JOIN text_entity t ON q.textId = t.textId
WHERE q.packId = 'pack-123' AND t.subject = 'MATEMATICA'
```

#### 7.3. getOptionsForQuestion()

**Archivo**: `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`

**Líneas 299-300**:
```kotlin
@Query("SELECT * FROM option_entity WHERE questionId = :questionId")
suspend fun getOptionsByQuestion(questionId: String): List<OptionEntity>
```

**SQL ejecutado**:
```sql
SELECT * FROM option_entity WHERE questionId = 'question-456'
```

**Qué verificar aquí**:
- ✅ Hay textos en `text_entity` para el pack
- ✅ Hay preguntas en `question_entity` para el pack
- ✅ Las preguntas están asociadas a textos con `textId` correcto
- ✅ Los textos tienen `subject` correcto
- ✅ Hay opciones en `option_entity` para cada pregunta

**Cómo verificar**:
1. **Database Inspector**:
   ```sql
   -- Verificar textos
   SELECT * FROM text_entity WHERE packId = 'TU_PACK_ID';
   
   -- Verificar preguntas por materia
   SELECT q.*, t.subject 
   FROM question_entity q
   INNER JOIN text_entity t ON q.textId = t.textId
   WHERE q.packId = 'TU_PACK_ID' AND t.subject = 'MATEMATICA';
   
   -- Verificar opciones
   SELECT * FROM option_entity WHERE questionId = 'TU_QUESTION_ID';
   ```

2. **Logs**:
   ```
   ExamViewModel: Found X texts for pack pack-123
   ExamViewModel: Found Y questions for subject MATEMATICA
   ExamViewModel: Prepared Z exam contents
   ```

---

## 🎯 Paso 8: Iniciar Examen - Validaciones

### Archivo: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`

**Líneas 75-152**:
```kotlin
fun startExam(subject: String? = null) {
    if (_state.value.stage == ExamStage.InProgress) return
    
    viewModelScope.launch {
        _state.update { it.copy(isBusy = true, errorMessage = null) }
        
        // Validar que tenemos userId
        val uid = userId
        if (uid == null) {
            android.util.Log.e("ExamViewModel", "startExam: userId is null")
            _state.update {
                it.copy(
                    isBusy = false,
                    errorMessage = "Error: Usuario no identificado. Por favor, cierra sesión y vuelve a iniciar sesión."
                )
            }
            return@launch
        }
        
        // Validar que tenemos pack
        val pack = _state.value.pack  // ← Obtiene el pack del estado (ya cargado desde la base de datos)
        if (pack == null) {
            android.util.Log.e("ExamViewModel", "startExam: pack is null")
            _state.update {
                it.copy(
                    isBusy = false,
                    errorMessage = "No hay pack activo. Por favor, descarga un pack primero."
                )
            }
            return@launch
        }
        
        // Guardar la materia actual
        currentSubject = subject
        
        android.util.Log.d("ExamViewModel", "startExam: packId=${pack.packId}, subject=$subject")
        
        // Si se especifica una materia, cargar solo preguntas de esa materia (máximo 10)
        val questions = if (subject != null) {
            runCatching { 
                android.util.Log.d("ExamViewModel", "Loading questions for subject: $subject")
                prepareQuestions(pack.packId, subject)  // ← 🔌 CONEXIÓN A BASE DE DATOS (de nuevo)
            }.getOrElse { throwable ->
                android.util.Log.e("ExamViewModel", "Error preparing questions for subject $subject", throwable)
                _state.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = throwable.localizedMessage ?: "No hay preguntas disponibles para ${com.eduquiz.domain.pack.Subject.getDisplayName(subject)}. Verifica que el pack tenga contenido para esta materia."
                    )
                }
                return@launch
            }
        } else {
            _state.value.questions
        }
        
        android.util.Log.d("ExamViewModel", "Loaded ${questions.size} questions")
        
        if (questions.isEmpty()) {
            android.util.Log.w("ExamViewModel", "No questions found for packId=${pack.packId}, subject=$subject")
            _state.update { 
                it.copy(
                    isBusy = false,
                    errorMessage = if (subject != null) {
                        "No hay preguntas disponibles para ${com.eduquiz.domain.pack.Subject.getDisplayName(subject)} en este pack. Intenta con otra materia."
                    } else {
                        "No hay preguntas disponibles para este pack."
                    }
                ) 
            }
            return@launch
        }
        
        _state.update { it.copy(questions = questions) }
        
        startExamInternal()  // ← Inicia el examen
    }
}
```

**Qué verificar aquí**:
- ✅ `userId` no es null
- ✅ `pack` no es null (debe estar en `_state.value.pack`)
- ✅ `questions` no está vacío
- ✅ Los logs muestran el proceso completo

---

## 📺 Paso 9: Mostrar Botones de Materias

### Archivo: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamFeature.kt`

**Líneas 207-239**:
```kotlin
// Si hay pack activo, mostrar botones de materias
Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    Text(
        text = "Selecciona una materia:",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    
    SubjectButton(
        subject = com.eduquiz.domain.pack.Subject.MATEMATICA,
        onClick = { viewModel.startExam(com.eduquiz.domain.pack.Subject.MATEMATICA) },  // ← Al presionar, llama a startExam()
        enabled = !state.isBusy,
        isLoading = state.isBusy
    )
    
    SubjectButton(
        subject = com.eduquiz.domain.pack.Subject.COMPRENSION_LECTORA,
        onClick = { viewModel.startExam(com.eduquiz.domain.pack.Subject.COMPRENSION_LECTORA) },
        enabled = !state.isBusy,
        isLoading = state.isBusy
    )
    
    SubjectButton(
        subject = com.eduquiz.domain.pack.Subject.CIENCIAS,
        onClick = { viewModel.startExam(com.eduquiz.domain.pack.Subject.CIENCIAS) },
        enabled = !state.isBusy,
        isLoading = state.isBusy
    )
}
```

**Qué verificar en la pantalla**:
- ✅ Si aparecen los botones → El pack se cargó correctamente
- ✅ Si los botones están deshabilitados (`enabled = false`) → `state.isBusy = true`
- ✅ Si muestran un spinner → `isLoading = true`

---

## 🔍 Checklist de Verificación Completo

### 1. Verificar que el UID se pasa correctamente

**Código**: `EduQuizNavHost.kt` línea 153
```kotlin
uid = authUser.uid
```

**Verificación**:
- Agrega log: `Log.d("NavHost", "Exam uid: ${authUser.uid}")`
- Debe mostrar un UID válido (no null, no vacío)

---

### 2. Verificar que el ViewModel se inicializa

**Código**: `ExamFeature.kt` línea 64
```kotlin
LaunchedEffect(uid) { viewModel.initialize(uid) }
```

**Verificación en Logs**:
```
ExamViewModel: initialize called with uid: user-123
ExamViewModel: Setting userId to: user-123
ExamViewModel: Starting loadInitialState
```

---

### 3. Verificar conexión a base de datos - Pack Activo

**Código**: `ExamViewModel.kt` línea 293
```kotlin
var pack = packRepository.getActivePack()
```

**Cadena de conexión**:
```
ExamViewModel.getActivePack()
  → PackRepositoryImpl.getActivePack()
    → PackDao.findByStatus(PackStatus.ACTIVE)
      → Room ejecuta: SELECT * FROM pack_entity WHERE status = 'ACTIVE' LIMIT 1
```

**Verificación**:
1. **Database Inspector**:
   ```sql
   SELECT * FROM pack_entity WHERE status = 'ACTIVE';
   ```
   Debe retornar 1 fila

2. **Logs**:
   ```
   ExamViewModel: loadInitialState: Active pack = pack-123
   ```
   Si muestra `null`, no hay pack activo

3. **Pantalla**:
   - Si muestra "Sin pack activo" → No hay pack en la base de datos
   - Si muestra el `weekLabel` → Pack encontrado correctamente

---

### 4. Verificar conexión a Firestore (si no hay pack)

**Código**: `ExamViewModel.kt` línea 298
```kotlin
val availablePack = runCatching { packRepository.fetchCurrentPackMeta() }.getOrNull()
```

**Cadena de conexión**:
```
ExamViewModel.fetchCurrentPackMeta()
  → PackRepositoryImpl.fetchCurrentPackMeta()
    → PackRemoteDataSource.fetchCurrentPackMeta()
      → FirebaseFirestore.collection("packs").whereEqualTo("status", "PUBLISHED").get()
```

**Verificación**:
1. **Firebase Console**:
   - Firestore Database → Datos → `packs`
   - Debe haber documentos con `status = "PUBLISHED"`

2. **Logs**:
   ```
   PackRemoteDataSource: ✅ Consulta completada. Found X published packs
   ```
   Si muestra `Found 0`, no hay packs en Firestore

3. **Pantalla**:
   - Si aparece card "Pack disponible" → Hay pack en Firestore pero no descargado
   - Si muestra "No hay packs disponibles" → No hay packs en Firestore

---

### 5. Verificar carga de preguntas desde base de datos

**Código**: `ExamViewModel.kt` línea 116
```kotlin
prepareQuestions(pack.packId, subject)
```

**Cadena de conexión**:
```
ExamViewModel.prepareQuestions()
  → PackRepository.getTextsForPack() → ContentDao.getTextsByPack()
  → PackRepository.getQuestionsForPackBySubject() → ContentDao.getQuestionsByPackAndSubject()
  → PackRepository.getOptionsForQuestion() → ContentDao.getOptionsByQuestion()
```

**Verificación**:
1. **Database Inspector**:
   ```sql
   -- Textos
   SELECT * FROM text_entity WHERE packId = 'pack-123';
   
   -- Preguntas por materia
   SELECT q.*, t.subject 
   FROM question_entity q
   INNER JOIN text_entity t ON q.textId = t.textId
   WHERE q.packId = 'pack-123' AND t.subject = 'MATEMATICA';
   
   -- Opciones
   SELECT * FROM option_entity WHERE questionId IN (
       SELECT questionId FROM question_entity WHERE packId = 'pack-123'
   );
   ```

2. **Logs**:
   ```
   ExamViewModel: Found X texts for pack pack-123
   ExamViewModel: Found Y texts for subject MATEMATICA
   ExamViewModel: Found Z questions for subject MATEMATICA
   ExamViewModel: Prepared N exam contents
   ```

3. **Pantalla**:
   - Si muestra "Preguntas: X" (donde X > 0) → Preguntas cargadas
   - Si muestra "Preguntas: No disponibles" → No hay preguntas

---

## 🐛 Problemas Comunes y Soluciones

### Problema 1: "Sin pack activo" en pantalla

**Causa**: No hay pack con `status = 'ACTIVE'` en la base de datos

**Solución**:
1. Ve a la pantalla de packs
2. Descarga un pack
3. Verifica en Database Inspector:
   ```sql
   SELECT * FROM pack_entity WHERE status = 'ACTIVE';
   ```

---

### Problema 2: "No hay packs disponibles"

**Causa**: No hay packs con `status = 'PUBLISHED'` en Firestore

**Solución**:
1. Ve a Firebase Console
2. Firestore Database → Datos → `packs`
3. Verifica que haya documentos con `status = "PUBLISHED"`
4. Si no hay, crea uno o publica uno existente

---

### Problema 3: "No hay preguntas disponibles para [Materia]"

**Causa**: El pack no tiene preguntas para esa materia

**Solución**:
1. Verifica en Database Inspector:
   ```sql
   SELECT q.*, t.subject 
   FROM question_entity q
   INNER JOIN text_entity t ON q.textId = t.textId
   WHERE q.packId = 'TU_PACK_ID' AND t.subject = 'MATEMATICA';
   ```
2. Si no hay resultados, el pack no tiene contenido para esa materia
3. Re-descarga el pack o verifica en Firestore que tenga contenido

---

### Problema 4: Los botones de materias no aparecen

**Causa**: `state.pack` es null

**Verificación**:
- Revisa los logs: `ExamViewModel: loadInitialState: Active pack = null`
- Verifica en Database Inspector que haya un pack activo
- Verifica que `loadInitialState()` se ejecute correctamente

---

## 📊 Diagrama de Flujo

```
Usuario presiona "Iniciar intento"
    ↓
ExamFeature (UI)
    ↓
ExamViewModel.initialize(uid)
    ↓
loadInitialState()
    ↓
packRepository.getActivePack()  ← 🔌 CONEXIÓN A BASE DE DATOS (Room)
    ↓
PackDao.findByStatus('ACTIVE')
    ↓
SELECT * FROM pack_entity WHERE status = 'ACTIVE' LIMIT 1
    ↓
¿Pack encontrado?
    ├─ SÍ → Cargar preguntas
    │         ↓
    │     prepareQuestions()
    │         ↓
    │     getTextsForPack()  ← 🔌 CONEXIÓN A BASE DE DATOS
    │     getQuestionsForPackBySubject()  ← 🔌 CONEXIÓN A BASE DE DATOS
    │     getOptionsForQuestion()  ← 🔌 CONEXIÓN A BASE DE DATOS
    │         ↓
    │     Mostrar botones de materias en pantalla
    │
    └─ NO → fetchCurrentPackMeta()  ← 🔌 CONEXIÓN A FIRESTORE
              ↓
          ¿Pack disponible en Firestore?
              ├─ SÍ → downloadPack() → Guardar en base de datos
              └─ NO → Mostrar "No hay packs disponibles"
```

---

## ✅ Resumen de Puntos de Conexión

1. **Base de Datos Room** (local):
   - `PackDao.findByStatus()` - Obtener pack activo
   - `ContentDao.getTextsByPack()` - Obtener textos
   - `ContentDao.getQuestionsByPackAndSubject()` - Obtener preguntas por materia
   - `ContentDao.getOptionsByQuestion()` - Obtener opciones

2. **Firestore** (remoto):
   - `PackRemoteDataSource.fetchCurrentPackMeta()` - Buscar packs disponibles
   - `PackRemoteDataSource.fetchPack()` - Descargar pack completo

3. **Pantalla**:
   - `state.pack` - Muestra información del pack
   - `state.errorMessage` - Muestra errores
   - `state.isBusy` - Controla estado de carga
   - Botones de materias - Solo aparecen si `state.pack != null`

---

## 🎯 Próximos Pasos para Debugging

1. **Revisa los logs** en Logcat filtrados por:
   - `ExamViewModel`
   - `PackRepositoryImpl`
   - `PackRemoteDataSource`

2. **Verifica en Database Inspector**:
   - Que haya un pack con `status = 'ACTIVE'`
   - Que haya textos y preguntas para ese pack
   - Que las preguntas estén asociadas correctamente

3. **Verifica en la pantalla**:
   - Qué mensaje aparece
   - Si aparecen los botones de materias
   - Si hay mensajes de error

4. **Comparte los logs** para identificar el problema exacto






