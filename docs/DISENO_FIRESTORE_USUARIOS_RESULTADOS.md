# 🎯 Diseño de Firestore: Usuarios y Resultados

## 📋 Objetivo

Diseñar las colecciones de Firestore para soportar:
1. **Tabla de Clasificación (Leaderboard)** con filtrado por Colegio/UGEL
2. **Métricas acumuladas** pre-calculadas para ordenamiento rápido
3. **Historial de intentos de examen**

---

## 🗂️ Estructura de Colecciones

### 1. Colección `users`

**Ruta**: `users/{uid}`

**Propósito**: Almacenar perfil del usuario y métricas acumuladas para ranking.

**⚠️ IMPORTANTE**: El usuario se crea automáticamente cuando hace login con Google (Firebase Auth). La app obtiene automáticamente:
- `uid`: ID único del usuario de Firebase Auth
- `displayName`: Nombre del usuario de Google
- `email`: Email del usuario de Google
- `photoUrl`: URL de la foto de perfil de Google

**Estructura del Documento**:

```json
{
  "uid": "user123",                    // ← De Firebase Auth (automático)
  "displayName": "Juan Pérez",        // ← De Firebase Auth (automático)
  "email": "juan.perez@example.com",  // ← De Firebase Auth (automático)
  "photoUrl": "https://...",          // ← De Firebase Auth (automático)
  
  // Código de Colegio/UGEL (ingresado manualmente por el usuario en la app)
  "schoolCode": "UGEL-001",
  
  // Métricas acumuladas para ranking (pre-calculadas automáticamente)
  "totalXp": 1250,              // Puntaje Total XP (acumulativo)
  "averageAccuracy": 85.5,      // Promedio de Aciertos % (0-100)
  "totalAttempts": 12,          // Total de intentos completados
  "totalCorrectAnswers": 102,   // Total de respuestas correctas
  "totalQuestions": 120,         // Total de preguntas respondidas
  
  // Campos adicionales (se sincronizan desde Room)
  "schoolId": "school1",
  "classroomId": "class1",
  "coins": 500,
  "selectedCosmeticId": null,
  
  // Metadatos
  "createdAt": "2025-01-15T10:00:00Z",
  "updatedAt": "2025-01-20T15:30:00Z",
  "lastSyncedAt": 1737382200000
}
```

**Flujo de Creación**:
1. Usuario hace login con Google → Firebase Auth crea el usuario
2. `AuthViewModel` detecta el login y crea el perfil en Room automáticamente
3. El perfil se sincroniza a Firestore con los datos de Firebase Auth
4. El usuario ingresa manualmente el `schoolCode` desde la app
5. Las métricas se actualizan automáticamente cuando completa exámenes

**Campos Clave para Ranking**:
- `schoolCode` (String): Código de Colegio/UGEL ingresado por el usuario
- `totalXp` (Number): XP total acumulado **calculado desde los intentos reales** (para ordenar por puntaje)
- `averageAccuracy` (Number): Promedio de aciertos en % (0-100) **calculado desde los intentos reales**
- `totalAttempts` (Number): Total de intentos completados
- `totalCorrectAnswers` (Number): Total de respuestas correctas **sumado desde los intentos reales**
- `totalQuestions` (Number): Total de preguntas respondidas **sumado desde los intentos reales**

**⚠️ IMPORTANTE**: Las métricas NO son inventadas. Se calculan REALMENTE desde los intentos de examen:
1. Se suman todos los `xpEarned` de los intentos completados → `totalXp`
2. Se suman todos los `correctAnswers` de los intentos → `totalCorrectAnswers`
3. Se suman todos los `totalQuestions` de los intentos → `totalQuestions`
4. Se calcula: `averageAccuracy = (totalCorrectAnswers / totalQuestions) * 100`

**Cálculo de `averageAccuracy`**:
```
averageAccuracy = (totalCorrectAnswers / totalQuestions) * 100
```

**Foto de Perfil**:
- `photoUrl` viene automáticamente de Firebase Auth (Gmail del usuario)
- En usuarios de ejemplo puede ser `null` porque no están autenticados

---

### 2. Colección `exam_attempts`

**Ruta**: `exam_attempts/{attemptId}`

**Propósito**: Almacenar cada intento de examen completado.

**Estructura del Documento**:

```json
{
  "attemptId": "attempt_2025_001",
  "uid": "user123",
  "packId": "pack_2025_w01",
  
  // Timestamps
  "startedAt": "2025-01-20T10:00:00Z",
  "finishedAt": "2025-01-20T10:20:00Z",
  "durationMs": 1200000,
  
  // Resultados
  "status": "COMPLETED",
  "scoreRaw": 8,                // Preguntas correctas
  "scoreValidated": 8,          // Puntaje validado (puede ser null)
  "totalQuestions": 10,         // Total de preguntas del examen
  "correctAnswers": 8,          // Respuestas correctas
  "accuracy": 80.0,             // Porcentaje de aciertos (0-100)
  "xpEarned": 80,               // XP ganado en este intento
  
  // Metadatos
  "origin": "OFFLINE",
  "createdAt": "2025-01-20T10:00:00Z",
  "updatedAt": "2025-01-20T10:20:00Z"
}
```

**Subcolección `answers`** (opcional, si necesitas detalle):
- Ruta: `exam_attempts/{attemptId}/answers/{questionId}`
- Estructura:
```json
{
  "questionId": "q_2025_w01_0001",
  "selectedOptionId": "B",
  "isCorrect": true,
  "timeSpentMs": 45000
}
```

---

## 🔄 Actualización de Métricas

### Cuándo Actualizar

Las métricas en `users/{uid}` se actualizan cuando:
1. Un usuario completa un examen (`status = "COMPLETED"`)
2. Un intento es validado (`scoreValidated` cambia)

### Cómo Actualizar

**Opción 1: Cloud Function (Recomendado)**

Usa una Cloud Function que se ejecute cuando se crea/actualiza un documento en `exam_attempts`:

```javascript
exports.onExamAttemptCompleted = functions.firestore
  .document('exam_attempts/{attemptId}')
  .onWrite(async (change, context) => {
    const attempt = change.after.data();
    
    // Solo procesar si el intento está completado
    if (attempt.status !== 'COMPLETED') return;
    
    const uid = attempt.uid;
    const userRef = admin.firestore().doc(`users/${uid}`);
    
    // Obtener intentos completados del usuario
    const attemptsSnapshot = await admin.firestore()
      .collection('exam_attempts')
      .where('uid', '==', uid)
      .where('status', '==', 'COMPLETED')
      .get();
    
    // Calcular métricas acumuladas
    let totalXp = 0;
    let totalCorrectAnswers = 0;
    let totalQuestions = 0;
    let totalAttempts = 0;
    
    attemptsSnapshot.forEach(doc => {
      const data = doc.data();
      totalXp += data.xpEarned || 0;
      totalCorrectAnswers += data.correctAnswers || 0;
      totalQuestions += data.totalQuestions || 0;
      totalAttempts += 1;
    });
    
    const averageAccuracy = totalQuestions > 0 
      ? (totalCorrectAnswers / totalQuestions) * 100 
      : 0;
    
    // Actualizar documento del usuario
    await userRef.update({
      totalXp: totalXp,
      averageAccuracy: averageAccuracy,
      totalAttempts: totalAttempts,
      totalCorrectAnswers: totalCorrectAnswers,
      totalQuestions: totalQuestions,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });
```

**Opción 2: Desde la App (Android)**

Actualiza las métricas desde la app cuando se completa un examen:

```kotlin
// En PackRepositoryImpl o similar
suspend fun updateUserMetrics(uid: String, attempt: ExamAttempt) {
    val userRef = firestore.collection("users").document(uid)
    
    // Obtener métricas actuales
    val userDoc = userRef.get().await()
    val currentTotalXp = userDoc.getLong("totalXp") ?: 0L
    val currentTotalCorrect = userDoc.getLong("totalCorrectAnswers") ?: 0L
    val currentTotalQuestions = userDoc.getLong("totalQuestions") ?: 0L
    val currentTotalAttempts = userDoc.getLong("totalAttempts") ?: 0L
    
    // Calcular nuevas métricas
    val newTotalXp = currentTotalXp + attempt.xpEarned
    val newTotalCorrect = currentTotalCorrect + attempt.correctAnswers
    val newTotalQuestions = currentTotalQuestions + attempt.totalQuestions
    val newTotalAttempts = currentTotalAttempts + 1
    val newAverageAccuracy = if (newTotalQuestions > 0) {
        (newTotalCorrect.toDouble() / newTotalQuestions.toDouble()) * 100.0
    } else {
        0.0
    }
    
    // Actualizar
    userRef.update(
        mapOf(
            "totalXp" to newTotalXp,
            "averageAccuracy" to newAverageAccuracy,
            "totalAttempts" to newTotalAttempts,
            "totalCorrectAnswers" to newTotalCorrect,
            "totalQuestions" to newTotalQuestions,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    ).await()
}
```

---

## 📊 Consultas para Leaderboard

### 1. Leaderboard Global (Todos los usuarios)

```javascript
// Ordenar por XP total (descendente)
db.collection('users')
  .orderBy('totalXp', 'desc')
  .limit(100)
  .get()

// Ordenar por promedio de aciertos (descendente)
db.collection('users')
  .orderBy('averageAccuracy', 'desc')
  .limit(100)
  .get()
```

### 2. Leaderboard por Colegio/UGEL

```javascript
// Filtrar por código de colegio y ordenar por XP
db.collection('users')
  .where('schoolCode', '==', 'UGEL-001')
  .orderBy('totalXp', 'desc')
  .limit(100)
  .get()

// Filtrar por código de colegio y ordenar por promedio de aciertos
db.collection('users')
  .where('schoolCode', '==', 'UGEL-001')
  .orderBy('averageAccuracy', 'desc')
  .limit(100)
  .get()
```

### 3. Índices Requeridos

Para las consultas anteriores, necesitas crear índices compuestos en Firestore:

**Índice 1**: `schoolCode` + `totalXp` (descendente)
- Colección: `users`
- Campos: `schoolCode` (Ascendente), `totalXp` (Descendente)

**Índice 2**: `schoolCode` + `averageAccuracy` (descendente)
- Colección: `users`
- Campos: `schoolCode` (Ascendente), `averageAccuracy` (Descendente)

**Cómo crear índices**:
1. Ve a Firebase Console → Firestore Database → Índices
2. Haz clic en "Crear índice"
3. Configura los campos según arriba
4. O usa el enlace que aparece en el error cuando ejecutas la consulta

---

## 🔐 Reglas de Seguridad

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

---

## 📝 Ejemplo de Flujo Completo

### 1. Usuario se Registra (Automático)

**Cuando el usuario hace login con Google:**

1. Firebase Auth crea automáticamente el usuario
2. `AuthViewModel` detecta el login y crea el perfil en Room:
   ```kotlin
   // En AuthViewModel.kt
   val existingProfile = profileRepository.observeProfile(user.uid).firstOrNull()
   if (existingProfile == null) {
       profileRepository.upsertProfile(
           UserProfile(
               uid = user.uid,
               displayName = user.displayName ?: "Usuario",  // ← De Firebase Auth
               photoUrl = user.photoUrl,                      // ← De Firebase Auth
               schoolId = "",
               classroomId = "",
               coins = 0,
               selectedCosmeticId = null,
               updatedAtLocal = System.currentTimeMillis(),
               syncState = SyncState.PENDING
           )
       )
   }
   ```
3. El perfil se sincroniza automáticamente a Firestore con los datos de Firebase Auth
4. El documento en Firestore se crea con:
   ```javascript
   {
     uid: "user123",                    // ← De Firebase Auth
     displayName: "Juan Pérez",        // ← De Firebase Auth
     email: "juan@example.com",        // ← De Firebase Auth
     photoUrl: "https://...",          // ← De Firebase Auth
     schoolCode: "",                   // ← Vacío inicialmente
     totalXp: 0,
     averageAccuracy: 0.0,
     totalAttempts: 0,
     totalCorrectAnswers: 0,
     totalQuestions: 0
   }
   ```

### 2. Usuario Ingresa Código de Colegio

```javascript
// Actualizar schoolCode
await db.collection('users').doc(uid).update({
  schoolCode: "UGEL-001",
  updatedAt: admin.firestore.FieldValue.serverTimestamp()
});
```

### 3. Usuario Completa un Examen

```javascript
// Crear intento
const attemptRef = await db.collection('exam_attempts').add({
  uid: uid,
  packId: "pack_2025_w01",
  startedAt: startTime,
  finishedAt: finishTime,
  status: "COMPLETED",
  scoreRaw: 8,
  totalQuestions: 10,
  correctAnswers: 8,
  accuracy: 80.0,
  xpEarned: 80,
  createdAt: admin.firestore.FieldValue.serverTimestamp()
});

// La Cloud Function actualizará automáticamente las métricas en users/{uid}
```

### 4. Consultar Leaderboard

```javascript
// Leaderboard por colegio
const leaderboard = await db.collection('users')
  .where('schoolCode', '==', 'UGEL-001')
  .orderBy('totalXp', 'desc')
  .limit(50)
  .get();

leaderboard.forEach(doc => {
  const user = doc.data();
  console.log(`${user.displayName}: ${user.totalXp} XP, ${user.averageAccuracy}%`);
});
```

---

## ✅ Checklist de Implementación

- [ ] Crear colección `users` con estructura definida
- [ ] Crear colección `exam_attempts` con estructura definida
- [ ] Agregar campo `schoolCode` a documentos de usuarios existentes
- [ ] Crear Cloud Function para actualizar métricas automáticamente
- [ ] Crear índices compuestos para consultas de ranking
- [ ] Configurar reglas de seguridad
- [ ] Implementar actualización de métricas desde la app (opcional)
- [ ] Probar consultas de leaderboard

---

## 🎯 Ventajas de este Diseño

1. **Rendimiento**: Métricas pre-calculadas permiten ordenamiento rápido sin cálculos en el cliente
2. **Escalabilidad**: Funciona bien con miles de usuarios
3. **Flexibilidad**: Permite filtrar por colegio/UGEL fácilmente
4. **Mantenibilidad**: Lógica de cálculo centralizada en Cloud Functions
5. **Consistencia**: Métricas siempre actualizadas automáticamente

---

**Última actualización**: Diciembre 2025

