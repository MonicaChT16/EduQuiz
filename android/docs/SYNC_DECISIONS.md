# Decisiones de Sincronización

## Reglas de Sincronización por Entidad

### ExamAttempts (Intentos de Examen)

**Regla: Merge/Unión (nunca borrar)**

- Los intentos de examen se sincronizan usando `SetOptions.merge()` en Firestore.
- Esto significa que:
  - Si un intento ya existe en Firestore, solo se actualizan los campos proporcionados.
  - Los campos que no están en el payload local se mantienen en Firestore.
  - **Nunca se borran datos** de intentos existentes.
- Ruta en Firestore: `users/{uid}/examAttempts/{attemptId}`
- Las respuestas se guardan como subcolección: `users/{uid}/examAttempts/{attemptId}/answers/{questionId}`

**Justificación:**
- Los intentos de examen son datos históricos valiosos que no deben perderse.
- Un intento puede ser actualizado desde múltiples dispositivos (ej: se completa en un dispositivo, se valida en otro).
- El merge asegura que no se pierdan datos importantes como `scoreValidated` que puede ser establecido por el backend.

### UserProfile (Perfil de Usuario)

**Regla: Última escritura gana (Last-Write-Wins)**

- El perfil se sincroniza comparando `updatedAtLocal` antes de escribir.
- Si el perfil local tiene `updatedAtLocal >= remoteUpdatedAt`, se escribe.
- Si el perfil remoto es más reciente, se omite la escritura (pero se marca como SYNCED porque el remoto ya tiene la versión correcta).
- Ruta en Firestore: `users/{uid}`

**Justificación:**
- El perfil puede ser actualizado desde múltiples dispositivos.
- La última escritura gana asegura que los cambios más recientes no se sobrescriban.
- Ejemplo: Usuario actualiza su avatar en el dispositivo A, luego actualiza monedas en el dispositivo B. Con last-write-wins, el avatar se mantiene si el dispositivo A escribió después.

## Estados de Sincronización

- **PENDING**: El dato está pendiente de sincronización.
- **SYNCED**: El dato ha sido sincronizado exitosamente con Firestore.
- **FAILED**: La sincronización falló (se reintentará en la próxima ejecución del Worker).

## Estrategia de Reintentos

- El `SyncWorker` marca intentos/perfiles como `FAILED` si la sincronización falla.
- WorkManager maneja los reintentos automáticamente según su política de backoff.
- Los datos marcados como `FAILED` se reintentarán en la próxima ejecución del Worker (periódica o inmediata).

## Triggers de Sincronización

1. **Al finalizar un examen**: Se encola una sincronización inmediata (`enqueueSyncNow()`).
2. **Al iniciar sesión**: Se programa sincronización periódica (`schedulePeriodicSync()`) y se encola una sincronización inmediata.
3. **Sincronización periódica**: Cada 4 horas cuando hay conexión a internet.

## Constraints del Worker

- **NetworkType.CONNECTED**: El Worker solo se ejecuta cuando hay conexión a internet.
- Esto asegura que no se intente sincronizar sin conexión, evitando fallos innecesarios.















