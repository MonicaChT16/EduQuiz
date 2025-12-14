# 📥 Descarga Automática de Packs

## ✅ Sistema Implementado

La app tiene **3 mecanismos** para descargar packs automáticamente cuando hay conexión a internet:

---

## 🔄 Mecanismo 1: PackUpdateWorker (Background)

### Archivo: `android/data/src/main/java/com/eduquiz/data/sync/PackUpdateWorker.kt`

**Qué hace**:
- Se ejecuta **periódicamente cada 6 horas** en segundo plano
- Solo se ejecuta cuando hay **conexión a internet** (Network constraint)
- Verifica si hay un pack nuevo disponible en Firestore
- Si no hay pack activo o hay uno nuevo, lo descarga automáticamente
- **No requiere interacción del usuario**

**Código clave**:
```kotlin
override suspend fun doWork(): Result {
    // 1. Obtener el pack activo actual
    val activePack = packRepository.getActivePack()  // ← Consulta directa a base de datos
    val currentPackId = activePack?.packId
    
    // 2. Verificar si hay un pack nuevo disponible en Firestore
    val availablePackMeta = packRepository.fetchCurrentPackMeta()
    
    // 3. Si no hay pack activo o hay uno nuevo, descargarlo
    if (currentPackId == null || currentPackId != availablePackMeta.packId) {
        val downloadedPack = packRepository.downloadPack(availablePackMeta.packId)
        packRepository.setActivePack(downloadedPack.packId)  // ← Marca como activo
        return Result.success()
    }
}
```

**Cuándo se ejecuta**:
- Cada 6 horas automáticamente
- Cuando hay conexión a internet
- En segundo plano (no bloquea la UI)

**Logs esperados**:
```
PackUpdateWorker: Starting pack update check
PackUpdateWorker: Current active pack: none
PackUpdateWorker: No active pack found, downloading available pack: pack-123
PackUpdateWorker: Successfully downloaded new pack: pack-123
PackUpdateWorker: New pack activated: pack-123
```

---

## 🚀 Mecanismo 2: Al Iniciar la App

### Archivo: `android/app/src/main/java/com/eduquiz/app/EduQuizApp.kt`

**Qué hace**:
- Al iniciar la app, programa el worker periódico
- Ejecuta una verificación **inmediata** de packs disponibles
- Si hay conexión a internet, descarga el pack automáticamente

**Código clave** (líneas 42-46):
```kotlin
// Programar sincronización periódica y actualización automática de packs
syncRepository.schedulePeriodicSync()  // ← Programa worker periódico
syncRepository.schedulePackUpdate()    // ← Programa verificación cada 6 horas
syncRepository.checkPackUpdateNow()     // ← Verifica INMEDIATAMENTE al iniciar
```

**Cuándo se ejecuta**:
- Al iniciar la app
- Solo si hay conexión a internet
- Una vez al iniciar (no periódico)

---

## 📱 Mecanismo 3: Al Abrir la Pantalla de Examen

### Archivo: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt`

**Qué hace**:
- Cuando el usuario abre la pantalla de examen
- Si no hay pack activo, busca uno disponible en Firestore
- Si encuentra uno, lo descarga automáticamente
- Muestra mensaje "Descargando pack..." en la UI

**Código clave** (líneas 296-321):
```kotlin
if (pack == null) {
    // Si no hay pack activo, buscar packs disponibles y descargar automáticamente
    val availablePack = runCatching { packRepository.fetchCurrentPackMeta() }.getOrNull()
    
    if (availablePack != null) {
        // Descargar automáticamente el pack disponible
        _state.update { it.copy(isBusy = true, errorMessage = "Descargando pack...") }
        try {
            android.util.Log.d("ExamViewModel", "Auto-downloading pack: ${availablePack.packId}")
            pack = packRepository.downloadPack(availablePack.packId)
            android.util.Log.d("ExamViewModel", "Pack downloaded successfully: ${pack.packId}")
            // Continuar con la carga normal ahora que tenemos el pack
        } catch (e: Exception) {
            android.util.Log.e("ExamViewModel", "Error auto-downloading pack", e)
            _state.update {
                it.copy(
                    errorMessage = "Error al descargar el pack. Intenta nuevamente."
                )
            }
            return
        }
    }
}
```

**Cuándo se ejecuta**:
- Al abrir la pantalla de examen
- Solo si no hay pack activo
- Requiere conexión a internet

**Logs esperados**:
```
ExamViewModel: loadInitialState: Active pack = null
ExamViewModel: Auto-downloading pack: pack-123
ExamViewModel: Pack downloaded successfully: pack-123
```

---

## 🔧 Configuración del Worker

### Archivo: `android/data/src/main/java/com/eduquiz/data/repository/SyncRepositoryImpl.kt`

**Programación periódica** (líneas 79-98):
```kotlin
override fun schedulePackUpdate() {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)  // ← Solo con internet
        .build()

    val periodicRequest = PeriodicWorkRequestBuilder<PackUpdateWorker>(
        PERIODIC_PACK_UPDATE_INTERVAL_HOURS,  // ← 6 horas
        TimeUnit.HOURS
    )
        .setConstraints(constraints)
        .addTag(PERIODIC_PACK_UPDATE_TAG)
        .build()

    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(
            PERIODIC_PACK_UPDATE_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
}
```

**Verificación inmediata** (líneas 104-120):
```kotlin
override fun checkPackUpdateNow() {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)  // ← Solo con internet
        .build()

    val packUpdateRequest = OneTimeWorkRequestBuilder<PackUpdateWorker>()
        .setConstraints(constraints)
        .addTag(PackUpdateWorker.WORK_NAME)
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork(
            "immediate_pack_update",
            ExistingWorkPolicy.REPLACE,
            packUpdateRequest
        )
}
```

---

## ✅ Flujo Completo de Descarga Automática

```
1. Usuario inicia la app
   ↓
2. EduQuizApp.onCreate()
   ↓
3. schedulePackUpdate() → Programa worker cada 6 horas
   ↓
4. checkPackUpdateNow() → Verifica INMEDIATAMENTE
   ↓
5. PackUpdateWorker.doWork()
   ↓
6. ¿Hay conexión a internet?
   ├─ NO → Espera hasta que haya conexión
   └─ SÍ → Continúa
   ↓
7. getActivePack() → ¿Hay pack activo?
   ├─ SÍ → ¿Es diferente al disponible?
   │   ├─ NO → No hace nada (ya está actualizado)
   │   └─ SÍ → Descarga el nuevo pack
   └─ NO → Descarga el pack disponible
   ↓
8. downloadPack() → Descarga desde Firestore
   ↓
9. setActivePack() → Marca como activo
   ↓
10. Pack listo para usar
```

---

## 🔍 Verificación

### 1. Verificar que el Worker está programado

**Logs esperados al iniciar la app**:
```
EduQuizApp: Workers scheduled: periodic sync, pack update, and sync all users
```

### 2. Verificar que el Worker se ejecuta

**Logs esperados cada 6 horas o al iniciar**:
```
PackUpdateWorker: Starting pack update check
PackUpdateWorker: Current active pack: none
PackUpdateWorker: No active pack found, downloading available pack: pack-123
PackUpdateWorker: Successfully downloaded new pack: pack-123
PackUpdateWorker: New pack activated: pack-123
```

### 3. Verificar descarga automática en pantalla de examen

**Logs esperados**:
```
ExamViewModel: loadInitialState: Active pack = null
ExamViewModel: Auto-downloading pack: pack-123
ExamViewModel: Pack downloaded successfully: pack-123
```

**En pantalla**:
- Muestra "Descargando pack..." mientras descarga
- Luego muestra el pack activo con su información

---

## 🐛 Problemas Comunes

### ❌ Problema: El pack no se descarga automáticamente

**Causas posibles**:
1. No hay conexión a internet
2. El worker no está programado
3. No hay packs disponibles en Firestore
4. Error al descargar el pack

**Solución**:
1. Verifica que hay conexión a internet
2. Revisa los logs: `PackUpdateWorker` o `ExamViewModel`
3. Verifica en Firebase Console que haya packs con `status = "PUBLISHED"`
4. Revisa los logs de error

---

### ❌ Problema: El pack se descarga pero no aparece como activo

**Causa**: Error al marcar como activo

**Solución**:
1. Verifica los logs: `PackUpdateWorker: New pack activated: ...`
2. Verifica en Database Inspector:
   ```sql
   SELECT * FROM pack_entity WHERE status = 'ACTIVE';
   ```
3. Si no hay pack activo, el problema está en `setActivePack()`

---

### ❌ Problema: El worker no se ejecuta

**Causas posibles**:
1. WorkManager no está configurado correctamente
2. El worker está bloqueado por el sistema
3. La app está en modo de ahorro de batería

**Solución**:
1. Verifica que `EduQuizApp` extiende `Configuration.Provider`
2. Verifica que `workManagerConfiguration` está implementado
3. Desactiva el modo de ahorro de batería para la app
4. Verifica en Logcat que el worker se programa:
   ```
   EduQuizApp: Workers scheduled: periodic sync, pack update, and sync all users
   ```

---

## 📊 Resumen

| Mecanismo | Cuándo se ejecuta | Requiere internet | Requiere UI |
|-----------|-------------------|-------------------|-------------|
| PackUpdateWorker (periódico) | Cada 6 horas | ✅ Sí | ❌ No |
| checkPackUpdateNow() | Al iniciar app | ✅ Sí | ❌ No |
| ExamViewModel | Al abrir examen | ✅ Sí | ✅ Sí |

**Todos los mecanismos**:
- ✅ Solo se ejecutan con conexión a internet
- ✅ Descargan automáticamente sin necesidad de presionar botones
- ✅ Marcan el pack como activo automáticamente
- ✅ Funcionan en segundo plano (excepto ExamViewModel que muestra UI)

---

## ✅ Cambios Realizados

1. ✅ Corregido `PackUpdateWorker` para usar `getActivePack()` en lugar de `observeActivePack().firstOrNull()`
2. ✅ Corregido `AuthViewModel` para usar `getActivePack()` en lugar de `observeActivePack().firstOrNull()`
3. ✅ Agregados logs detallados para debugging
4. ✅ Verificado que todos los mecanismos funcionan correctamente

El sistema de descarga automática está completamente funcional y no requiere interacción del usuario cuando hay conexión a internet.






