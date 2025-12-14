# 🔍 Diagnóstico: Error al Mostrar en Pantalla

## ✅ Base de Datos Verificada

La base de datos está correcta:
- ✅ Pack activo: `pack_2025_w01` con `status = 'ACTIVE'`
- ✅ Textos disponibles: 3 textos con diferentes materias
- ✅ Preguntas disponibles: Deben estar asociadas a los textos

## ❌ Problema: No se muestra correctamente en pantalla

El problema está en cómo se muestra la información en la UI, no en la base de datos.

---

## 🔍 Puntos a Revisar

### 1. Observación del Estado

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamFeature.kt`

**Línea 63**:
```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
```

**Qué verificar**:
- ✅ El estado se observa correctamente
- ✅ `collectAsStateWithLifecycle()` está funcionando
- ✅ La recomposición se ejecuta cuando cambia el estado

**Logs esperados**:
```
ExamFeature: State changed: stage=Start, pack=pack_2025_w01, questions=X, error=null
```

---

### 2. Inicialización del ViewModel

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamFeature.kt`

**Línea 64**:
```kotlin
LaunchedEffect(uid) { viewModel.initialize(uid) }
```

**Qué verificar**:
- ✅ `LaunchedEffect` se ejecuta solo una vez
- ✅ `initialize()` se llama correctamente
- ✅ `loadInitialState()` se ejecuta

**Logs esperados**:
```
ExamFeature: LaunchedEffect triggered with uid: user-123
ExamViewModel: initialize called with uid: user-123
ExamViewModel: Starting loadInitialState
```

---

### 3. Carga del Estado Inicial

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`

**Líneas 288-387**:
```kotlin
private suspend fun loadInitialState() {
    // 1. Obtener pack activo
    var pack = packRepository.getActivePack()
    
    // 2. Si no hay pack, descargar automáticamente
    if (pack == null) { ... }
    
    // 3. Cargar preguntas
    val questions = prepareQuestions(pack.packId)
    
    // 4. Actualizar estado
    _state.update {
        it.copy(
            stage = ExamStage.Start,
            pack = pack,
            questions = questions,
            isBusy = false
        )
    }
}
```

**Qué verificar**:
- ✅ El pack se obtiene correctamente
- ✅ Las preguntas se cargan correctamente
- ✅ El estado se actualiza con `_state.update()`

**Logs esperados**:
```
ExamViewModel: loadInitialState: Active pack = pack_2025_w01
ExamViewModel: loadInitialState: Prepared X questions
ExamViewModel: loadInitialState: State updated, stage=Start, pack=pack_2025_w01, questions=X
```

---

### 4. Renderizado en Pantalla

**Archivo**: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamFeature.kt`

**Líneas 125-137**:
```kotlin
Text(
    text = state.pack?.weekLabel ?: "Sin pack activo",
    style = MaterialTheme.typography.titleLarge
)
Text(
    text = "ID: ${state.pack?.packId ?: "--"}",
    style = MaterialTheme.typography.bodySmall
)
Text(
    text = "Preguntas: ${state.totalQuestions.takeIf { it > 0 } ?: "No disponibles"}",
    style = MaterialTheme.typography.bodyMedium
)
```

**Qué verificar**:
- ✅ `state.pack` no es null
- ✅ `state.pack.weekLabel` tiene un valor
- ✅ `state.totalQuestions` es mayor que 0
- ✅ La recomposición se ejecuta cuando cambia el estado

**Logs esperados**:
```
ExamStartScreen: Rendering: pack=pack_2025_w01, weekLabel=2025-W01, questions=X, error=null
```

---

## 🐛 Problemas Comunes

### Problema 1: El estado no se actualiza

**Síntoma**: La pantalla muestra "Sin pack activo" aunque hay pack en la base de datos

**Causas posibles**:
1. `loadInitialState()` no se ejecuta
2. `_state.update()` no se llama
3. El estado se actualiza pero la UI no se recompone

**Solución**:
1. Verifica los logs: `ExamViewModel: loadInitialState: State updated`
2. Verifica que `state.pack` no sea null en la UI
3. Agrega logs en `ExamStartScreen` para ver qué valores recibe

---

### Problema 2: El estado se resetea después de actualizarse

**Síntoma**: El pack se carga pero luego desaparece

**Causas posibles**:
1. `LaunchedEffect` se ejecuta múltiples veces
2. El ViewModel se recrea
3. Hay otro código que resetea el estado

**Solución**:
1. Verifica que `initialize()` tenga la guarda: `if (userId != null) return`
2. Verifica que `LaunchedEffect` solo se ejecute cuando cambia `uid`
3. Revisa si hay otros lugares que llamen `_state.update()`

---

### Problema 3: La UI no se recompone

**Síntoma**: El estado se actualiza pero la pantalla no cambia

**Causas posibles**:
1. `collectAsStateWithLifecycle()` no está funcionando
2. El estado no cambia realmente (mismo valor)
3. Hay un problema con la recomposición de Compose

**Solución**:
1. Verifica que `state` se observe con `by` (no `=`)
2. Agrega logs en `ExamStartScreen` para ver si se recompone
3. Verifica que el estado realmente cambie (usa `distinctUntilChanged()` si es necesario)

---

### Problema 4: Las preguntas no se cargan

**Síntoma**: El pack se muestra pero "Preguntas: No disponibles"

**Causas posibles**:
1. `prepareQuestions()` retorna lista vacía
2. No hay preguntas en la base de datos
3. Error al cargar preguntas

**Solución**:
1. Verifica los logs: `ExamViewModel: Prepared X questions`
2. Verifica en Database Inspector:
   ```sql
   SELECT COUNT(*) FROM question_entity WHERE packId = 'pack_2025_w01';
   ```
3. Verifica que las preguntas estén asociadas a textos

---

## 🔧 Solución Implementada

Se agregaron logs detallados en:

1. **`ExamFeature`**: Log cuando cambia el estado
2. **`ExamStartScreen`**: Log cuando se renderiza con los valores actuales
3. **`ExamViewModel`**: Logs ya existentes en `loadInitialState()`

---

## 📊 Logs Esperados (Flujo Completo)

```
ExamFeature: LaunchedEffect triggered with uid: user-123
ExamViewModel: initialize called with uid: user-123
ExamViewModel: Starting loadInitialState
ExamViewModel: loadInitialState: Getting active pack from database
ExamViewModel: loadInitialState: Active pack = pack_2025_w01
ExamViewModel: loadInitialState: Preparing questions for pack pack_2025_w01
ExamViewModel: prepareQuestions: packId=pack_2025_w01, subject=null
ExamViewModel: Found 3 texts for pack pack_2025_w01
ExamViewModel: Found X questions for pack (no subject filter)
ExamViewModel: Prepared X exam contents
ExamViewModel: loadInitialState: Prepared X questions
ExamViewModel: loadInitialState: Setting stage to Start with X questions
ExamViewModel: loadInitialState: State updated, stage=Start, pack=pack_2025_w01, questions=X
ExamFeature: State changed: stage=Start, pack=pack_2025_w01, questions=X, error=null
ExamStartScreen: Rendering: pack=pack_2025_w01, weekLabel=2025-W01, questions=X, error=null
```

---

## ✅ Checklist de Verificación

1. **Revisa los logs en Logcat**:
   - [ ] `ExamFeature: LaunchedEffect triggered`
   - [ ] `ExamViewModel: initialize called`
   - [ ] `ExamViewModel: loadInitialState: Active pack = pack_2025_w01`
   - [ ] `ExamViewModel: loadInitialState: State updated`
   - [ ] `ExamFeature: State changed`
   - [ ] `ExamStartScreen: Rendering`

2. **Verifica en Database Inspector**:
   - [ ] Hay un pack con `status = 'ACTIVE'`
   - [ ] Hay textos para ese pack
   - [ ] Hay preguntas para ese pack

3. **Verifica en la pantalla**:
   - [ ] Muestra el `weekLabel` del pack
   - [ ] Muestra el `packId`
   - [ ] Muestra "Preguntas: X" donde X > 0
   - [ ] Aparecen los botones de materias

---

## 🎯 Próximos Pasos

1. **Ejecuta la app** y revisa los logs en Logcat
2. **Comparte los logs** completos para identificar dónde está el problema
3. **Verifica qué muestra la pantalla** exactamente
4. **Compara los logs** con los esperados para encontrar diferencias

Los logs agregados te ayudarán a identificar exactamente dónde está fallando el flujo.






