# 🔍 Diagnóstico: Sincronización de Usuarios a Firestore

## ❌ Problema
Los usuarios no aparecen en la colección `users` de Firestore después de sincronizar.

---

## ✅ Checklist de Verificación

### 1. Verificar Reglas de Firestore (CRÍTICO)

**⚠️ ESTO ES LO MÁS IMPORTANTE**

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Proyecto: `eduquiz-e2829`
3. **Firestore Database** → **Reglas**
4. Verifica que tengas estas reglas:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Usuarios: lectura pública para ranking, escritura solo del propio usuario
    match /users/{uid} {
      allow read: if true; // Todos pueden leer para ver ranking
      allow write: if request.auth != null && request.auth.uid == uid;
    }
    
    // Intentos de examen
    match /exam_attempts/{attemptId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
    
    // Packs, textos, preguntas (lectura pública)
    match /packs/{packId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    match /texts/{textId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    match /questions/{questionId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

5. **HAZ CLIC EN "PUBLICAR"** (esto es CRÍTICO - las reglas no se aplican hasta que las publiques)

---

### 2. Verificar Logs de Android Studio

1. Abre **Android Studio**
2. Conecta tu dispositivo o inicia el emulador
3. Abre **Logcat** (View → Tool Windows → Logcat)
4. Filtra por estos tags:
   - `SyncAllUsersWorker`
   - `SyncRepository`
   - `FirestoreSyncService`
   - `SyncWorker`

5. Busca estos mensajes clave:

**Si la sincronización se ejecuta:**
```
SyncAllUsersWorker: Starting sync of all users to Firestore
SyncRepository: Found X users to sync
SyncRepository: ✅ Synced user: uid123 (Nombre Usuario)
FirestoreSyncService: ✅ Successfully synced user profile uid123 to Firestore
```

**Si hay errores:**
```
FirestoreSyncService: ❌ Error syncing user profile uid123
FirestoreSyncService: Error message: PERMISSION_DENIED
```

---

### 3. Verificar que el Usuario Esté Autenticado

**Problema común**: La sincronización puede fallar si el usuario no está autenticado.

**Verificar en los logs:**
```
FirestoreSyncService: User email: NOT AVAILABLE
```

**Solución:**
1. Asegúrate de estar logueado en la app
2. La sincronización masiva se ejecuta al iniciar la app, pero requiere que el usuario esté autenticado

---

### 4. Verificar Estado de Sincronización de los Perfiles

El método `syncAllUsers()` marca todos los perfiles como `PENDING` antes de sincronizar.

**Verificar en la base de datos local:**
- Abre **Database Inspector** en Android Studio
- Tabla: `user_profile_entity`
- Verifica que los perfiles tengan `syncState = 'PENDING'` o `'SYNCED'`

---

### 5. Ejecutar Sincronización Manual

#### Opción A: Desde la App (si tienes acceso al código)

Agrega un botón temporal para ejecutar la sincronización:

```kotlin
// En cualquier ViewModel o Activity
viewModelScope.launch {
    val result = syncRepository.syncAllUsers()
    Log.d("Sync", "Resultado: ${result.syncedUsers} de ${result.totalUsers}")
}
```

#### Opción B: Reiniciar la App

1. Cierra completamente la app
2. Vuelve a abrirla
3. La sincronización masiva se ejecuta automáticamente al iniciar

#### Opción C: Forzar Sincronización Individual

Si solo quieres sincronizar tu perfil:

1. Haz logout
2. Haz login de nuevo
3. Esto creará un perfil nuevo con `syncState = PENDING`
4. El `SyncWorker` lo sincronizará automáticamente

---

### 6. Verificar Errores Específicos

#### Error: PERMISSION_DENIED

**Causa**: Las reglas de Firestore no permiten escritura.

**Solución**:
1. Verifica las reglas (paso 1)
2. Asegúrate de que el usuario esté autenticado
3. Verifica que `request.auth.uid == uid` en las reglas

#### Error: Missing or insufficient permissions

**Causa**: Las reglas no están publicadas o son incorrectas.

**Solución**:
1. Ve a Firestore → Reglas
2. Copia las reglas del paso 1
3. Haz clic en **"Publicar"**

#### Error: Network error

**Causa**: No hay conexión a internet.

**Solución**:
1. Verifica la conexión a internet
2. El worker se ejecutará cuando haya conexión

---

### 7. Verificar en Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Proyecto: `eduquiz-e2829`
3. **Firestore Database** → **Datos**
4. Busca la colección `users`
5. Deberías ver documentos con el formato: `users/{uid}`

**Si no ves la colección `users`:**
- La sincronización no se ha ejecutado o ha fallado
- Revisa los logs (paso 2)

**Si ves la colección pero está vacía:**
- Los usuarios no se están sincronizando
- Revisa los logs para ver errores

---

### 8. Verificar Estructura del Documento en Firestore

Cuando un usuario se sincroniza correctamente, el documento en `users/{uid}` debe tener:

```json
{
  "uid": "user123",
  "displayName": "Nombre Usuario",
  "email": "usuario@example.com",
  "photoUrl": "https://...",
  "schoolCode": "1234567",
  "ugelCode": "1234567",
  "coins": 0,
  "xp": 0,
  "totalXp": 0,
  "totalScore": 0,
  "averageAccuracy": 0.0,
  "totalAttempts": 0,
  "totalCorrectAnswers": 0,
  "totalQuestions": 0,
  "updatedAtLocal": 1737382200000,
  "lastSyncedAt": 1737382200000
}
```

---

## 🔧 Solución Rápida

Si después de verificar todo lo anterior aún no ves los usuarios:

1. **Verifica las reglas de Firestore** (paso 1) - esto es lo más común
2. **Revisa los logs** (paso 2) para ver errores específicos
3. **Asegúrate de estar autenticado** en la app
4. **Reinicia la app** para ejecutar la sincronización automática
5. **Verifica en Firebase Console** que la colección `users` exista

---

## 📝 Comandos Útiles para Debugging

### Ver logs de sincronización:
```bash
# En Android Studio Logcat, filtra por:
SyncAllUsersWorker
SyncRepository
FirestoreSyncService
```

### Verificar usuarios en Firestore:
1. Firebase Console → Firestore Database → Datos
2. Busca colección `users`
3. Deberías ver documentos con el formato `users/{uid}`

---

## ⚠️ Problemas Comunes

1. **"Las reglas no están publicadas"**
   - Después de cambiar las reglas, DEBES hacer clic en "Publicar"
   - Si no las publicas, no se aplican

2. **"El usuario no está autenticado"**
   - La sincronización requiere que el usuario esté logueado
   - Verifica que Firebase Auth esté funcionando

3. **"Los perfiles ya están SYNCED"**
   - El `SyncWorker` solo sincroniza perfiles con `syncState = PENDING`
   - El método `syncAllUsers()` marca todos como PENDING antes de sincronizar

4. **"No hay conexión a internet"**
   - El worker requiere conexión a internet
   - Se ejecutará automáticamente cuando haya conexión

---

## 🎯 Próximos Pasos

Si después de seguir todos estos pasos aún no funciona:

1. Comparte los logs de Android Studio (filtrados por `SyncAllUsersWorker` y `FirestoreSyncService`)
2. Verifica que las reglas de Firestore estén publicadas
3. Verifica que el usuario esté autenticado en la app
4. Intenta ejecutar la sincronización manualmente desde el código







