# 📍 Ubicación de los Datos del Usuario

## 🗂️ Dónde se Guarda la Información

### 1. **Base de Datos Local (Room)**
**Ubicación**: `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`

**Tabla**: `user_profile_entity`

**Campos guardados**:
- `uid`: ID único del usuario (de Firebase Auth)
- `displayName`: Nombre del usuario
- `photoUrl`: URL de la foto de perfil
- `coins`: Monedas del usuario
- `xp`: Puntos de experiencia acumulados (local, se sincroniza como `totalXp` en Firestore)
- `selectedCosmeticId`: Cosmético equipado
- `updatedAtLocal`: Timestamp de última actualización
- `syncState`: Estado de sincronización (PENDING, SYNCED, FAILED)

**Nota**: El código UGEL se almacena en Firestore como `schoolCode`, no en Room localmente.

**Archivo de base de datos**: `eduquiz.db` (en el dispositivo)

---

### 2. **Firestore (Nube)**
**Ubicación**: Firebase Console → Firestore Database → Colección `users`

**Ruta del documento**: `users/{uid}`

**Estructura**:
```json
{
  "uid": "user123",
  "displayName": "Monica Chilon",
  "email": "monica@example.com",
  "photoUrl": "https://...",
  "schoolCode": "1234567",  // ← Código UGEL ingresado por el usuario
  "coins": 255,
  "selectedCosmeticId": null,
  "totalXp": 255,
  "averageAccuracy": 85.0,
  "totalAttempts": 3,
  "totalCorrectAnswers": 25,
  "totalQuestions": 30,
  "createdAt": "2025-01-15T10:00:00Z",
  "updatedAt": "2025-01-20T15:30:00Z"
}
```

**Campos eliminados (no se usan)**:
- `schoolId` - campo legacy eliminado
- `classroomId` - campo legacy eliminado
- `ugelCode` - duplicado de `schoolCode`, eliminado
- `xp` - duplicado de `totalXp`, eliminado
- `totalScore` - no se usa, eliminado
- `updatedAtLocal` - timestamp local, no debe estar en Firestore
- `lastSyncedAt` - no se usa, eliminado

**Sincronización**: Se sincroniza automáticamente cuando hay internet

---

## 🔄 Flujo de Sincronización

### Al Iniciar Sesión:
1. **Si el perfil NO existe localmente**:
   - Intenta obtenerlo desde Firestore
   - Si existe en Firestore → Se descarga y guarda en Room
   - Si NO existe en Firestore → Se crea un perfil nuevo

2. **Si el perfil YA existe localmente**:
   - Se mantiene el perfil local
   - Se sincroniza con Firestore en segundo plano

### Al Actualizar el Código UGEL:
1. Se guarda en Room (local)
2. Se marca como `PENDING` para sincronizar
3. Se sincroniza automáticamente con Firestore cuando hay internet
4. **El código UGEL se preserva** - no se borra al desinstalar/reinstalar

---

## 📦 Información de Packs

### Base de Datos Local (Room):
**Tabla**: `pack_entity`
- `packId`: ID del pack
- `weekLabel`: Etiqueta de la semana
- `status`: Estado (ACTIVE, DOWNLOADED, ARCHIVED)
- `publishedAt`: Fecha de publicación
- `downloadedAt`: Fecha de descarga

**Tablas relacionadas**:
- `text_entity`: Textos de lectura
- `question_entity`: Preguntas
- `option_entity`: Opciones de respuesta

### Firestore:
**Ruta**: `packs/{packId}`

**Sincronización**: 
- Los packs se descargan desde Firestore
- Se guardan en Room para uso offline
- **Al reinstalar la app**: Se descarga automáticamente el pack actual si no existe

---

## 🔐 Persistencia de Datos

### ✅ Lo que SE MANTIENE al desinstalar/reinstalar:
- **Perfil del usuario** (desde Firestore)
- **Código UGEL** (desde Firestore)
- **XP y Coins** (desde Firestore)
- **Historial de exámenes** (desde Firestore)
- **Logros** (desde Firestore)

### ❌ Lo que se BORRA al desinstalar:
- **Packs descargados** (se descargan automáticamente al iniciar sesión)
- **Datos temporales en Room**

### 🔄 Recuperación Automática:
Al iniciar sesión después de reinstalar:
1. Se recupera el perfil desde Firestore (incluyendo código UGEL)
2. Se descarga automáticamente el pack actual
3. Se sincronizan los datos pendientes

---

## 📊 Dónde Ver los Datos

### En Firebase Console:
1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto: `eduquiz-e2829`
3. Ve a **Firestore Database**
4. Colección `users` → Documento `{uid}`

### En la App:
- **Perfil**: Se muestra en la pantalla de perfil
- **Código UGEL**: Se muestra en la pantalla de ranking
- **Packs**: Se muestran en la pantalla de examen

---

## 🔍 Verificación de Datos

### Verificar que el código UGEL está guardado:

**En Firestore**:
```javascript
// En Firebase Console → Firestore
users/{uid}
  - ugelCode: "1234567"
  - schoolCode: "1234567"
```

**En Room (local)**:
```kotlin
// En Android Studio → Database Inspector
user_profile_entity
  - uid: "user123"
  - ugelCode: "1234567"
```

---

## ⚠️ Importante

1. **El código UGEL solo cambia cuando el usuario lo actualiza manualmente**
2. **No se borra al desinstalar la app** - se recupera desde Firestore
3. **Se sincroniza automáticamente** cuando hay internet
4. **El código debe tener exactamente 7 dígitos numéricos**

---

## 🛠️ Troubleshooting

### Si el código UGEL no aparece después de reinstalar:
1. Verifica que el usuario esté autenticado
2. Verifica en Firestore que el campo `ugelCode` o `schoolCode` exista
3. Revisa los logs de Android Studio para ver errores de sincronización

### Si el pack no se descarga automáticamente:
1. Verifica que haya un pack publicado en Firestore
2. Verifica la conexión a internet
3. Revisa los logs para ver errores de descarga







