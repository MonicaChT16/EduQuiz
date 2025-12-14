# 🔄 Sincronización Masiva de Usuarios a Firestore

## 📋 Descripción

Este sistema permite sincronizar **todos los usuarios** de la app a Firestore, no solo los que tienen estado `PENDING`. Es útil para:

- ✅ Migrar usuarios existentes a Firestore
- ✅ Forzar una actualización masiva de todos los perfiles
- ✅ Sincronizar usuarios que no se han sincronizado antes
- ✅ Actualizar métricas de ranking para todos los usuarios

---

## 🚀 Cómo Usar

### Opción 1: Sincronización Automática en Segundo Plano

La sincronización masiva se puede ejecutar automáticamente usando WorkManager:

```kotlin
// En cualquier parte de la app donde tengas acceso a SyncRepository
syncRepository.enqueueSyncAllUsers()
```

Esto encola un trabajo en segundo plano que se ejecutará cuando haya conexión a internet.

### Opción 2: Sincronización Síncrona (Directa)

Si necesitas ejecutar la sincronización de forma síncrona y obtener el resultado:

```kotlin
viewModelScope.launch {
    val result = syncRepository.syncAllUsers()
    
    Log.d("Sync", "Total usuarios: ${result.totalUsers}")
    Log.d("Sync", "Sincronizados: ${result.syncedUsers}")
    Log.d("Sync", "Fallidos: ${result.failedUsers}")
    Log.d("Sync", "Omitidos: ${result.skippedUsers}")
}
```

### Opción 3: Al Iniciar la App (Opcional)

Si quieres que se sincronice automáticamente al iniciar la app, puedes agregar esto en `EduQuizApp.kt`:

```kotlin
override fun onCreate() {
    super.onCreate()
    
    // ... código existente ...
    
    // Sincronizar todos los usuarios al iniciar (opcional)
    // Descomentar si quieres que se ejecute automáticamente
    // syncRepository.enqueueSyncAllUsers()
}
```

---

## 📊 Resultado de la Sincronización

El método `syncAllUsers()` retorna un objeto `SyncAllUsersResult` con:

```kotlin
data class SyncAllUsersResult(
    val totalUsers: Int,      // Total de usuarios en la base de datos local
    val syncedUsers: Int,    // Usuarios sincronizados exitosamente
    val failedUsers: Int,    // Usuarios que fallaron al sincronizar
    val skippedUsers: Int    // Usuarios omitidos (actualmente siempre 0)
)
```

---

## 🔍 Qué se Sincroniza

Para cada usuario, se sincroniza:

1. **Datos básicos**:
   - `uid`, `displayName`, `email`, `photoUrl`

2. **Datos de colegio/UGEL**:
   - `schoolCode` (del `ugelCode`)
   - `ugelCode` (código UGEL original)
   - `schoolId`, `classroomId`

3. **Monedas y XP**:
   - `coins`, `xp`, `totalXp`, `totalScore`

4. **Métricas de ranking** (calculadas automáticamente):
   - `averageAccuracy`: Promedio de aciertos (%)
   - `totalAttempts`: Total de exámenes completados
   - `totalCorrectAnswers`: Total de respuestas correctas
   - `totalQuestions`: Total de preguntas respondidas

5. **Otros campos**:
   - `selectedCosmeticId`
   - `updatedAtLocal`, `lastSyncedAt`

---

## ⚙️ Cómo Funciona

1. **Obtiene todos los perfiles** de la base de datos local (Room)
2. **Marca cada perfil como PENDING** para forzar sincronización
3. **Sincroniza cada perfil** con Firestore usando `FirestoreSyncService`
4. **Calcula métricas de ranking** desde los intentos de examen locales
5. **Actualiza el estado** a `SYNCED` si es exitoso, `FAILED` si falla
6. **Retorna el resultado** con estadísticas de la sincronización

---

## 📝 Logs

La sincronización genera logs detallados:

```
SyncRepository: Starting sync of all users to Firestore
SyncRepository: Found X users to sync
SyncRepository: ✅ Synced user: uid123 (Nombre Usuario)
SyncRepository: ❌ Failed to sync user: uid456
SyncRepository: Sync completed: X synced, Y failed out of Z total users
```

Puedes ver estos logs en **Android Studio → Logcat** filtrando por:
- `SyncRepository`
- `SyncAllUsersWorker`
- `FirestoreSyncService`

---

## ⚠️ Consideraciones

1. **Conexión a Internet**: La sincronización requiere conexión a internet
2. **Tiempo de Ejecución**: Puede tardar varios minutos si hay muchos usuarios
3. **Límites de Firestore**: Firestore tiene límites de escritura, pero el código incluye pausas para evitar sobrecarga
4. **Reglas de Firestore**: Asegúrate de que las reglas de Firestore permitan escribir en `users/{uid}`

---

## 🔧 Troubleshooting

### Los usuarios no se sincronizan

1. **Verifica las reglas de Firestore**:
   ```javascript
   match /users/{uid} {
     allow write: if request.auth != null && request.auth.uid == uid;
   }
   ```

2. **Verifica los logs** en Logcat para ver errores específicos

3. **Verifica la conexión a internet** antes de ejecutar

### Algunos usuarios fallan

- Revisa los logs para ver qué usuarios fallan y por qué
- Los usuarios que fallan se marcan como `FAILED` y se pueden reintentar después

---

## 📍 Ubicación del Código

- **Interfaz**: `android/domain/src/main/java/com/eduquiz/domain/sync/SyncRepository.kt`
- **Implementación**: `android/data/src/main/java/com/eduquiz/data/repository/SyncRepositoryImpl.kt`
- **Worker**: `android/data/src/main/java/com/eduquiz/data/sync/SyncAllUsersWorker.kt`
- **Servicio de sincronización**: `android/data/src/main/java/com/eduquiz/data/remote/FirestoreSyncService.kt`

---

## 🎯 Ejemplo de Uso Completo

```kotlin
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val syncRepository: SyncRepository
) : ViewModel() {
    
    private val _syncResult = MutableStateFlow<SyncAllUsersResult?>(null)
    val syncResult: StateFlow<SyncAllUsersResult?> = _syncResult.asStateFlow()
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    fun syncAllUsers() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = syncRepository.syncAllUsers()
                _syncResult.value = result
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error syncing all users", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    fun syncAllUsersInBackground() {
        syncRepository.enqueueSyncAllUsers()
    }
}
```

---

## ✅ Checklist de Sincronización

- [ ] Verificar que las reglas de Firestore permitan escritura
- [ ] Verificar conexión a internet
- [ ] Ejecutar sincronización (automática o manual)
- [ ] Revisar logs para verificar éxito
- [ ] Verificar en Firebase Console que los usuarios aparezcan en `users/{uid}`
- [ ] Verificar que las métricas de ranking se calculen correctamente







