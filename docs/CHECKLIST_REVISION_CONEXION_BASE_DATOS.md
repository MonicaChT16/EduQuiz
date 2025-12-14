# ✅ Checklist: Revisión de Conexión a Base de Datos

## 📍 Archivos y Líneas Exactas a Revisar

### 1. 🔌 CONEXIÓN A BASE DE DATOS - Pack Activo

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`
- **Línea 293**: `var pack = packRepository.getActivePack()`

**Cadena de llamadas**:
```
ExamViewModel.getActivePack()
  ↓
PackRepositoryImpl.getActivePack()  (línea 127)
  ↓
PackDao.findByStatus(PackStatus.ACTIVE)  (línea 272 en AppDatabase.kt)
  ↓
SQL: SELECT * FROM pack_entity WHERE status = 'ACTIVE' LIMIT 1
```

**✅ Qué verificar**:
- [ ] El log muestra: `ExamViewModel: loadInitialState: Active pack = pack-XXX` (no null)
- [ ] En Database Inspector: `SELECT * FROM pack_entity WHERE status = 'ACTIVE'` retorna 1 fila
- [ ] En pantalla: Muestra el `weekLabel` del pack (no "Sin pack activo")

---

### 2. 🔌 CONEXIÓN A FIRESTORE - Pack Disponible

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`
- **Línea 298**: `val availablePack = runCatching { packRepository.fetchCurrentPackMeta() }.getOrNull()`

**Cadena de llamadas**:
```
ExamViewModel.fetchCurrentPackMeta()
  ↓
PackRepositoryImpl.fetchCurrentPackMeta()  (línea 30)
  ↓
PackRemoteDataSource.fetchCurrentPackMeta()  (línea 61)
  ↓
FirebaseFirestore.collection("packs").whereEqualTo("status", "PUBLISHED").get()
```

**✅ Qué verificar**:
- [ ] El log muestra: `PackRemoteDataSource: ✅ Consulta completada. Found X published packs`
- [ ] En Firebase Console: Hay documentos en `packs` con `status = "PUBLISHED"`
- [ ] En pantalla: Aparece card "Pack disponible" o mensaje de error

---

### 3. 🔌 CONEXIÓN A BASE DE DATOS - Textos

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`
- **Línea 415**: `val texts = packRepository.getTextsForPack(packId).associateBy { it.textId }`

**Cadena de llamadas**:
```
ExamViewModel.getTextsForPack()
  ↓
PackRepositoryImpl.getTextsForPack()  (línea 130)
  ↓
ContentDao.getTextsByPack(packId)  (línea 283 en AppDatabase.kt)
  ↓
SQL: SELECT * FROM text_entity WHERE packId = 'pack-XXX'
```

**✅ Qué verificar**:
- [ ] El log muestra: `ExamViewModel: Found X texts for pack pack-XXX` (X > 0)
- [ ] En Database Inspector: `SELECT * FROM text_entity WHERE packId = 'TU_PACK_ID'` retorna filas
- [ ] Los textos tienen `subject` correcto (MATEMATICA, COMPRENSION_LECTORA, CIENCIAS)

---

### 4. 🔌 CONEXIÓN A BASE DE DATOS - Preguntas por Materia

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`
- **Línea 420**: `val allQuestions = packRepository.getQuestionsForPackBySubject(packId, subject)`

**Cadena de llamadas**:
```
ExamViewModel.getQuestionsForPackBySubject()
  ↓
PackRepositoryImpl.getQuestionsForPackBySubject()  (línea 139)
  ↓
ContentDao.getQuestionsByPackAndSubject(packId, subject)  (línea 292 en AppDatabase.kt)
  ↓
SQL: SELECT q.* FROM question_entity q
     INNER JOIN text_entity t ON q.textId = t.textId
     WHERE q.packId = 'pack-XXX' AND t.subject = 'MATEMATICA'
```

**✅ Qué verificar**:
- [ ] El log muestra: `ExamViewModel: Found X questions for subject MATEMATICA` (X > 0)
- [ ] En Database Inspector:
  ```sql
  SELECT q.*, t.subject 
  FROM question_entity q
  INNER JOIN text_entity t ON q.textId = t.textId
  WHERE q.packId = 'TU_PACK_ID' AND t.subject = 'MATEMATICA';
  ```
  Retorna preguntas
- [ ] Las preguntas están asociadas a textos con `textId` correcto

---

### 5. 🔌 CONEXIÓN A BASE DE DATOS - Opciones

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`
- **Línea 432**: `val options = packRepository.getOptionsForQuestion(question.questionId)`

**Cadena de llamadas**:
```
ExamViewModel.getOptionsForQuestion()
  ↓
PackRepositoryImpl.getOptionsForQuestion()  (línea 140)
  ↓
ContentDao.getOptionsByQuestion(questionId)  (línea 299 en AppDatabase.kt)
  ↓
SQL: SELECT * FROM option_entity WHERE questionId = 'question-XXX'
```

**✅ Qué verificar**:
- [ ] Cada pregunta tiene al menos 2 opciones
- [ ] En Database Inspector: `SELECT * FROM option_entity WHERE questionId = 'TU_QUESTION_ID'` retorna opciones
- [ ] Las opciones tienen `optionId` y `text` correctos

---

### 6. 📺 MOSTRAR EN PANTALLA - Pack Activo

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamFeature.kt`
- **Líneas 116-139**: Card que muestra información del pack

**Código clave**:
```kotlin
Text(text = state.pack?.weekLabel ?: "Sin pack activo")  // ← Línea 126
Text(text = "ID: ${state.pack?.packId ?: "--"}")  // ← Línea 130
Text(text = "Preguntas: ${state.totalQuestions.takeIf { it > 0 } ?: "No disponibles"}")  // ← Línea 135
```

**✅ Qué verificar en pantalla**:
- [ ] Muestra el `weekLabel` del pack (ej: "Semana 1")
- [ ] Muestra el `packId` (ej: "pack-123")
- [ ] Muestra "Preguntas: X" donde X > 0
- [ ] NO muestra "Sin pack activo"
- [ ] NO muestra "Preguntas: No disponibles"

---

### 7. 📺 MOSTRAR EN PANTALLA - Botones de Materias

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamFeature.kt`
- **Líneas 207-239**: Botones para seleccionar materia

**Código clave**:
```kotlin
if (state.pack == null) {
    // Mostrar botones de descarga
} else {
    // Mostrar botones de materias  ← Línea 207
    SubjectButton(
        subject = Subject.MATEMATICA,
        onClick = { viewModel.startExam(Subject.MATEMATICA) },  // ← Línea 221
        enabled = !state.isBusy,
        isLoading = state.isBusy
    )
}
```

**✅ Qué verificar en pantalla**:
- [ ] Aparecen los 3 botones de materias (Matemáticas, Comprensión lectora, Ciencias)
- [ ] Los botones están habilitados (`enabled = true`)
- [ ] NO aparecen los botones de descarga
- [ ] Al presionar un botón, se inicia el examen

---

### 8. 📺 MOSTRAR EN PANTALLA - Mensajes de Error

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamFeature.kt`
- **Líneas 164-171**: Muestra mensajes de error

**Código clave**:
```kotlin
if (state.errorMessage != null) {
    Text(
        text = state.errorMessage,  // ← Línea 167
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}
```

**✅ Qué verificar en pantalla**:
- [ ] Si hay error, se muestra en ROJO
- [ ] El mensaje es claro y específico
- [ ] Los mensajes posibles:
  - `"No hay pack activo. Por favor, descarga un pack primero."`
  - `"No hay packs disponibles. Intenta refrescar."`
  - `"No hay preguntas disponibles para [Materia]..."`

---

## 🔍 Verificación Paso a Paso

### Paso 1: Verificar que el UID se pasa

**Archivo**: `android/app/src/main/java/com/eduquiz/app/EduQuizNavHost.kt`
- **Línea 153**: `uid = authUser.uid`

**✅ Verificación**:
```kotlin
// Agrega este log temporalmente
Log.d("NavHost", "Exam uid: ${authUser.uid}")
```

**Resultado esperado**: Debe mostrar un UID válido (no null, no vacío)

---

### Paso 2: Verificar inicialización del ViewModel

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamFeature.kt`
- **Línea 64**: `LaunchedEffect(uid) { viewModel.initialize(uid) }`

**✅ Verificación en Logs**:
```
ExamViewModel: initialize called with uid: user-123
ExamViewModel: Setting userId to: user-123
ExamViewModel: Starting loadInitialState
```

---

### Paso 3: Verificar conexión a base de datos - Pack

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`
- **Línea 293**: `var pack = packRepository.getActivePack()`

**✅ Verificación en Logs**:
```
ExamViewModel: loadInitialState: Getting active pack from database
ExamViewModel: loadInitialState: Active pack = pack-123
```

**✅ Verificación en Database Inspector**:
```sql
SELECT * FROM pack_entity WHERE status = 'ACTIVE';
```
Debe retornar 1 fila con:
- `packId`: Un ID válido
- `status`: `'ACTIVE'`
- `weekLabel`: Un nombre (ej: "Semana 1")

**✅ Verificación en Pantalla**:
- Muestra el `weekLabel` del pack
- Muestra el `packId`
- NO muestra "Sin pack activo"

---

### Paso 4: Verificar carga de preguntas

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`
- **Línea 338**: `val questions = runCatching { prepareQuestions(pack.packId) }`

**✅ Verificación en Logs**:
```
ExamViewModel: loadInitialState: Preparing questions for pack pack-123
ExamViewModel: prepareQuestions: packId=pack-123, subject=null
ExamViewModel: Found X texts for pack pack-123
ExamViewModel: Found Y questions for pack (no subject filter)
ExamViewModel: Prepared Z exam contents
```

**✅ Verificación en Database Inspector**:
```sql
-- Textos
SELECT * FROM text_entity WHERE packId = 'pack-123';

-- Preguntas
SELECT q.*, t.subject 
FROM question_entity q
INNER JOIN text_entity t ON q.textId = t.textId
WHERE q.packId = 'pack-123';

-- Opciones
SELECT * FROM option_entity 
WHERE questionId IN (
    SELECT questionId FROM question_entity WHERE packId = 'pack-123'
);
```

**✅ Verificación en Pantalla**:
- Muestra "Preguntas: X" donde X > 0
- NO muestra "Preguntas: No disponibles"

---

### Paso 5: Verificar botones de materias

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamFeature.kt`
- **Líneas 207-239**: Botones de materias

**✅ Verificación en Pantalla**:
- Aparecen los 3 botones de materias
- Los botones están habilitados
- Al presionar, se inicia el examen

---

### Paso 6: Verificar inicio de examen

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`
- **Línea 75**: `fun startExam(subject: String? = null)`

**✅ Verificación en Logs**:
```
ExamViewModel: startExam: packId=pack-123, subject=MATEMATICA
ExamViewModel: Loading questions for subject: MATEMATICA
ExamViewModel: Found X texts for subject MATEMATICA
ExamViewModel: Found Y questions for subject MATEMATICA
ExamViewModel: Loaded Y questions
ExamViewModel: Prepared Z exam contents
```

**✅ Verificación en Database Inspector**:
```sql
SELECT q.*, t.subject 
FROM question_entity q
INNER JOIN text_entity t ON q.textId = t.textId
WHERE q.packId = 'pack-123' AND t.subject = 'MATEMATICA';
```
Debe retornar preguntas

**✅ Verificación en Pantalla**:
- El examen se inicia (cambia a pantalla de preguntas)
- NO muestra mensajes de error

---

## 🐛 Problemas Comunes

### ❌ Problema: "Sin pack activo" en pantalla

**Causa**: No hay pack con `status = 'ACTIVE'` en la base de datos

**Solución**:
1. Ve a la pantalla de packs
2. Descarga un pack
3. Verifica en Database Inspector:
   ```sql
   SELECT * FROM pack_entity WHERE status = 'ACTIVE';
   ```

---

### ❌ Problema: "No hay preguntas disponibles"

**Causa**: El pack no tiene preguntas cargadas

**Solución**:
1. Verifica en Database Inspector:
   ```sql
   SELECT COUNT(*) FROM question_entity WHERE packId = 'TU_PACK_ID';
   ```
2. Si es 0, re-descarga el pack
3. Verifica que el pack tenga contenido en Firestore

---

### ❌ Problema: Los botones de materias no aparecen

**Causa**: `state.pack` es null

**Verificación**:
1. Revisa los logs: `ExamViewModel: loadInitialState: Active pack = null`
2. Verifica en Database Inspector que haya un pack activo
3. Verifica que `loadInitialState()` se ejecute correctamente

---

### ❌ Problema: "No hay preguntas disponibles para [Materia]"

**Causa**: El pack no tiene preguntas para esa materia

**Verificación**:
```sql
SELECT q.*, t.subject 
FROM question_entity q
INNER JOIN text_entity t ON q.textId = t.textId
WHERE q.packId = 'TU_PACK_ID' AND t.subject = 'MATEMATICA';
```

Si no hay resultados, el pack no tiene contenido para esa materia.

---

## 📊 Resumen de Archivos a Revisar

1. **`ExamViewModel.kt`** (líneas 61-73, 288-294, 338-351, 409-460, 75-152)
   - Inicialización
   - Conexión a base de datos para pack
   - Carga de preguntas
   - Inicio de examen

2. **`ExamFeature.kt`** (líneas 56-64, 116-139, 164-171, 207-239)
   - UI que muestra el pack
   - Botones de materias
   - Mensajes de error

3. **`PackRepositoryImpl.kt`** (líneas 127-128, 130, 139, 140)
   - Implementación de métodos de base de datos

4. **`AppDatabase.kt`** (líneas 272, 283, 292-297, 299-300)
   - Consultas SQL a Room

5. **`PackRemoteDataSource.kt`** (líneas 61-98)
   - Conexión a Firestore

---

## 🎯 Próximos Pasos

1. **Revisa los logs** en Logcat filtrados por `ExamViewModel`
2. **Verifica en Database Inspector** que haya datos
3. **Verifica en la pantalla** qué se muestra
4. **Comparte los logs** si aún no funciona






