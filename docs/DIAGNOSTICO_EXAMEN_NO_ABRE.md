# 🔍 Diagnóstico: Examen No Se Abre

## ❌ Problema
Al presionar "Iniciar intento" después de seleccionar una materia, el examen no se inicia.

---

## 🔍 Flujo del Examen

### 1. Navegación a la Pantalla de Examen

```
HomeScreen → PackFeature → ExamFeature
```

**Código**: `EduQuizNavHost.kt` línea 151-158
```kotlin
composable(RootDestination.Exam.route) {
    ExamFeature(
        uid = authUser.uid,
        modifier = Modifier.fillMaxSize(),
        onExit = { navController.popBackStack(RootDestination.Home.route, inclusive = false) }
    )
}
```

### 2. Inicialización del ViewModel

**Código**: `ExamFeature.kt` línea 64
```kotlin
LaunchedEffect(uid) { viewModel.initialize(uid) }
```

**Código**: `ExamViewModel.kt` línea 61-67
```kotlin
fun initialize(uid: String) {
    if (userId != null) return
    userId = uid
    viewModelScope.launch {
        loadInitialState()
    }
}
```

### 3. Carga del Estado Inicial

**Código**: `ExamViewModel.kt` línea 282-366
- Observa el pack activo
- Si no hay pack, intenta descargar uno automáticamente
- Carga las preguntas del pack
- Verifica si hay un intento en progreso

### 4. Inicio del Examen

**Código**: `ExamFeature.kt` línea 219-238
```kotlin
SubjectButton(
    subject = Subject.MATEMATICA,
    onClick = { viewModel.startExam(Subject.MATEMATICA) },
    enabled = !state.isBusy,
    isLoading = state.isBusy
)
```

**Código**: `ExamViewModel.kt` línea 69-146
- Valida `userId` y `pack`
- Carga preguntas por materia
- Llama a `startExamInternal()`

---

## 🔍 Puntos de Falla Potenciales

### 1. **UID no se pasa correctamente**

**Síntoma**: `userId is null` en los logs

**Verificación**:
- Revisa Logcat: `ExamViewModel: initialize called with uid: ...`
- Verifica que `authUser.uid` no sea null en `EduQuizNavHost`

**Solución**:
- Asegúrate de estar autenticado
- Verifica que `authUser` no sea null

### 2. **Pack no se carga**

**Síntoma**: `pack is null` en los logs

**Verificación**:
- Revisa Logcat: `ExamViewModel: Active pack = null`
- Verifica en Database Inspector que haya un pack con `status = 'ACTIVE'`

**Solución**:
- Descarga un pack desde la pantalla de packs
- Verifica que el pack se haya guardado correctamente en la base de datos

### 3. **No hay preguntas para la materia**

**Síntoma**: `No questions found for packId=..., subject=...`

**Verificación**:
- Revisa Logcat: `ExamViewModel: Found X texts for subject ...`
- Revisa Logcat: `ExamViewModel: Found Y questions for subject ...`
- Verifica en Database Inspector que haya textos con `subject` correcto

**Solución**:
- Verifica en Firestore que los textos tengan `subject` correcto
- Re-descarga el pack si es necesario

### 4. **Error al preparar preguntas**

**Síntoma**: `Error preparing questions for subject ...`

**Verificación**:
- Revisa Logcat para el stack trace completo
- Verifica que las preguntas tengan textos asociados
- Verifica que las preguntas tengan opciones

**Solución**:
- Re-descarga el pack
- Verifica la integridad de los datos en Firestore

### 5. **Error al crear el intento**

**Síntoma**: Error en `examRepository.startAttempt()`

**Verificación**:
- Revisa Logcat para errores de base de datos
- Verifica que Room esté funcionando correctamente

**Solución**:
- Limpia los datos de la app y vuelve a intentar
- Verifica que la base de datos no esté corrupta

---

## 📊 Logs de Debugging Agregados

Se agregaron logs detallados en:

1. **`initialize()`**: Log cuando se inicializa el ViewModel
2. **`loadInitialState()`**: Logs de cada paso de carga
3. **`startExam()`**: Logs de validación y carga de preguntas
4. **`prepareQuestions()`**: Logs de textos y preguntas encontradas

---

## 🧪 Pasos para Diagnosticar

### Paso 1: Verificar Logs

1. Abre **Android Studio**
2. Conecta tu dispositivo
3. Abre **Logcat**
4. Filtra por: `ExamViewModel`
5. Intenta iniciar un examen
6. Busca estos mensajes en orden:

```
ExamViewModel: initialize called with uid: ...
ExamViewModel: Starting loadInitialState
ExamViewModel: loadInitialState: Starting
ExamViewModel: loadInitialState: Observing active pack
ExamViewModel: loadInitialState: Active pack = ...
ExamViewModel: loadInitialState: Preparing questions for pack ...
ExamViewModel: prepareQuestions: packId=..., subject=null
ExamViewModel: Found X texts for pack ...
ExamViewModel: Found Y questions for pack ...
ExamViewModel: loadInitialState: Prepared X questions
ExamViewModel: loadInitialState: Setting stage to Start
```

Luego cuando presionas "Iniciar intento":

```
ExamViewModel: startExam: packId=..., subject=...
ExamViewModel: Loading questions for subject: ...
ExamViewModel: Found X texts for subject ...
ExamViewModel: Found Y questions for subject ...
ExamViewModel: Loaded Y questions
ExamViewModel: Prepared X exam contents
```

### Paso 2: Verificar Estado en la UI

En la pantalla de inicio del examen, verifica:

1. **¿Hay un pack activo?**
   - Debería mostrar el `weekLabel` del pack
   - Debería mostrar el `packId`

2. **¿Hay preguntas disponibles?**
   - Debería mostrar "Preguntas: X" (donde X > 0)

3. **¿Hay mensajes de error?**
   - Revisa si aparece algún mensaje en rojo

### Paso 3: Verificar Base de Datos

Usa **Database Inspector**:

1. Abre Database Inspector
2. Verifica `pack_entity`:
   ```sql
   SELECT * FROM pack_entity WHERE status = 'ACTIVE'
   ```
   Debería haber exactamente 1 pack

3. Verifica `text_entity`:
   ```sql
   SELECT textId, packId, subject FROM text_entity WHERE packId = 'TU_PACK_ID'
   ```
   Debería haber textos con `subject` correcto

4. Verifica `question_entity`:
   ```sql
   SELECT q.questionId, q.textId, t.subject 
   FROM question_entity q
   INNER JOIN text_entity t ON q.textId = t.textId
   WHERE q.packId = 'TU_PACK_ID' AND t.subject = 'MATEMATICA'
   ```
   Debería haber preguntas para cada materia

---

## 🔧 Soluciones Comunes

### Solución 1: No hay pack activo

**Problema**: El pack no se descargó o no está marcado como ACTIVE

**Solución**:
1. Ve a la pantalla de packs
2. Descarga un pack
3. Verifica que se haya descargado correctamente
4. Vuelve a la pantalla de examen

### Solución 2: No hay preguntas para la materia

**Problema**: El pack no tiene contenido para esa materia

**Solución**:
1. Verifica en Firestore que los textos tengan `subject` correcto
2. Intenta con otra materia
3. Re-descarga el pack

### Solución 3: Error de base de datos

**Problema**: La base de datos está corrupta o incompleta

**Solución**:
1. Limpia los datos de la app
2. Vuelve a iniciar sesión
3. Descarga el pack nuevamente

---

## 📝 Checklist de Verificación

Antes de reportar el problema, verifica:

- [ ] Estás autenticado (hay un `uid` válido)
- [ ] Hay un pack activo en la base de datos
- [ ] El pack tiene textos con `subject` correcto
- [ ] El pack tiene preguntas asociadas a esos textos
- [ ] Las preguntas tienen opciones
- [ ] No hay errores en Logcat
- [ ] El estado en la UI muestra información correcta

---

## 🎯 Próximos Pasos

Si después de seguir estos pasos aún no funciona:

1. **Comparte los logs completos** de Logcat (filtrados por `ExamViewModel`)
2. **Comparte una captura de pantalla** de la pantalla de inicio del examen
3. **Comparte el resultado** de las consultas SQL en Database Inspector
4. **Describe exactamente qué pasa** cuando presionas "Iniciar intento"

---

## ✅ Cambios Realizados

1. ✅ Agregados logs detallados en `initialize()`
2. ✅ Agregados logs detallados en `loadInitialState()`
3. ✅ Agregados logs detallados en `startExam()`
4. ✅ Agregados logs detallados en `prepareQuestions()`
5. ✅ Mejorado manejo de errores con mensajes claros

Estos logs te ayudarán a identificar exactamente dónde está fallando el flujo.






