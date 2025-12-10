# 🔍 Diagnóstico de Sincronización con Firestore

## Problema
Los usuarios registrados en la app no aparecen en la colección `users` de Firestore.

## Pasos de Diagnóstico

### 1. Verificar Logs de Android Studio

1. Abre Android Studio
2. Conecta tu dispositivo o inicia el emulador
3. Abre **Logcat** (View → Tool Windows → Logcat)
4. Filtra por estos tags:
   - `SyncWorker`
   - `FirestoreSyncService`
   - `AuthViewModel`

5. Busca estos mensajes clave:
   - `"Starting sync work"` - El worker se está ejecutando
   - `"Found X pending profiles"` - Hay perfiles pendientes de sincronizar
   - `"Successfully synced user profile"` - Sincronización exitosa
   - `"Error syncing user profile"` - Error en la sincronización

### 2. Verificar que el Usuario Esté Autenticado

**Problema común**: El `SyncWorker` puede ejecutarse cuando el usuario no está autenticado.

**Solución**: Verifica en los logs si aparece:
```
Current user email: NOT AVAILABLE (user may not be authenticated)
```

Si aparece este mensaje, el problema es que el usuario no está autenticado cuando se ejecuta el worker.

### 3. Verificar Reglas de Firestore

**⚠️ CRÍTICO**: Las reglas de Firestore deben permitir que los usuarios escriban en su propio documento.

1. Ve a **Firebase Console** → **Firestore Database** → **Reglas**
2. Verifica que tengas estas reglas:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Usuarios: lectura pública para ranking, escritura solo del propio usuario
    match /users/{uid} {
      allow read: if true; // Todos pueden leer para ver ranking
      allow write: if request.auth != null && request.auth.uid == uid;
    }
    
    // Intentos de examen: lectura solo del propio usuario, escritura solo del propio usuario
    match /exam_attempts/{attemptId} {
      allow read: if request.auth != null && 
                     request.resource.data.uid == request.auth.uid;
      allow write: if request.auth != null && 
                      request.resource.data.uid == request.auth.uid;
    }
  }
}
```

3. Haz clic en **Publicar** para aplicar las reglas

### 4. Verificar que el Perfil Tenga `syncState = PENDING`

El `SyncWorker` solo sincroniza perfiles con `syncState = PENDING`.

**Verificar en la app**:
1. Abre la app
2. Haz login con Google
3. Revisa los logs para ver si el perfil se crea con `syncState = PENDING`

**Si el perfil ya tiene `syncState = SYNCED`**, no se sincronizará de nuevo a menos que se actualice.

### 5. Forzar Sincronización Manual

Si necesitas forzar una sincronización inmediata:

1. **Opción 1: Cerrar y reabrir la app**
   - Cierra completamente la app
   - Vuelve a abrirla
   - Esto debería disparar `enqueueSyncNow()` si el usuario está autenticado

2. **Opción 2: Hacer logout y login de nuevo**
   - Esto creará un nuevo perfil con `syncState = PENDING`
   - Y disparará la sincronización inmediata

3. **Opción 3: Completar un examen**
   - Completar un examen dispara `enqueueSyncNow()`

### 6. Verificar Conexión a Internet

El `SyncWorker` tiene la constraint `NetworkType.CONNECTED`, por lo que solo se ejecuta cuando hay internet.

**Verificar**:
- Asegúrate de que el dispositivo tenga conexión a internet
- Verifica en los logs si aparece algún error de red

### 7. Verificar Errores Específicos en Logs

Busca estos errores comunes:

#### Error: "PERMISSION_DENIED"
**Causa**: Las reglas de Firestore no permiten escribir.
**Solución**: Verifica y actualiza las reglas de Firestore (paso 3).

#### Error: "UNAUTHENTICATED"
**Causa**: El usuario no está autenticado cuando se ejecuta el worker.
**Solución**: Asegúrate de que el usuario esté autenticado antes de que se ejecute el worker.

#### Error: "NOT_FOUND" o "Collection not found"
**Causa**: La colección no existe (esto es normal, se crea automáticamente).
**Solución**: No es un error, la colección se creará al escribir el primer documento.

### 8. Verificar que el SyncWorker se Esté Ejecutando

**En los logs, busca**:
```
SyncWorker: Starting sync work
SyncWorker: Found X pending profiles
```

Si no ves estos mensajes, el worker no se está ejecutando.

**Posibles causas**:
- El usuario no ha hecho login (el worker se programa al hacer login)
- WorkManager no está configurado correctamente
- Hay un error en la inyección de dependencias

### 9. Verificar Datos en Room Database

Para verificar que los perfiles existen en Room:

1. Usa **Database Inspector** en Android Studio
2. Conecta a la app en ejecución
3. Navega a la tabla `user_profile_entity`
4. Verifica que:
   - Existan perfiles con `syncState = 'PENDING'`
   - Los perfiles tengan `uid`, `displayName`, etc.

## Solución Rápida

Si después de revisar todo lo anterior aún no funciona, prueba esto:

1. **Actualizar reglas de Firestore** (paso 3)
2. **Desinstalar y reinstalar la app** (esto borra Room y fuerza recrear perfiles)
3. **Hacer login de nuevo**
4. **Revisar logs inmediatamente después del login**

## Comandos Útiles para Logs

En Logcat, usa estos filtros:

```
tag:SyncWorker OR tag:FirestoreSyncService OR tag:AuthViewModel
```

O busca por texto:
```
"sync" OR "SyncWorker" OR "FirestoreSyncService"
```

## Contacto

Si después de seguir estos pasos el problema persiste, comparte:
1. Los logs completos de `SyncWorker` y `FirestoreSyncService`
2. Una captura de las reglas de Firestore
3. Una captura de la tabla `user_profile_entity` en Database Inspector










