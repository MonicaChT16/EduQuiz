# ☁️ Cloud Function: Actualización Automática de Métricas

## 📋 Objetivo

Cloud Function que actualiza automáticamente las métricas de ranking en `users/{uid}` cuando se completa un examen.

---

## 🔧 Implementación

### Archivo: `functions/src/index.ts`

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

admin.initializeApp();

/**
 * Actualiza las métricas de ranking cuando se completa un examen
 */
export const onExamAttemptCompleted = functions.firestore
  .document('exam_attempts/{attemptId}')
  .onWrite(async (change, context) => {
    const after = change.after;
    const before = change.before;
    
    // Obtener datos del intento
    const attempt = after.data();
    const previousAttempt = before.exists ? before.data() : null;
    
    // Solo procesar si el intento está completado
    if (attempt?.status !== 'COMPLETED') {
      console.log(`Intento ${context.params.attemptId} no está completado, ignorando...`);
      return null;
    }
    
    const uid = attempt.uid;
    if (!uid) {
      console.error('Intento sin uid, ignorando...');
      return null;
    }
    
    const userRef = admin.firestore().doc(`users/${uid}`);
    
    try {
      // Obtener todos los intentos completados del usuario
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
      
      // Calcular promedio de aciertos
      const averageAccuracy = totalQuestions > 0 
        ? Math.round((totalCorrectAnswers / totalQuestions) * 100 * 100) / 100 // Redondear a 2 decimales
        : 0.0;
      
      // Actualizar documento del usuario
      await userRef.update({
        totalXp: totalXp,
        averageAccuracy: averageAccuracy,
        totalAttempts: totalAttempts,
        totalCorrectAnswers: totalCorrectAnswers,
        totalQuestions: totalQuestions,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      
      console.log(`Métricas actualizadas para usuario ${uid}:`, {
        totalXp,
        averageAccuracy,
        totalAttempts
      });
      
      return null;
    } catch (error) {
      console.error(`Error actualizando métricas para usuario ${uid}:`, error);
      throw error;
    }
  });

/**
 * Función HTTP para actualizar métricas de un usuario específico
 * Útil para migraciones o correcciones manuales
 */
export const updateUserMetrics = functions.https.onCall(async (data, context) => {
  // Verificar autenticación
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'Debes estar autenticado para actualizar métricas'
    );
  }
  
  const uid = data.uid || context.auth.uid;
  
  // Verificar que el usuario solo pueda actualizar sus propias métricas
  if (uid !== context.auth.uid && !context.auth.token.admin) {
    throw new functions.https.HttpsError(
      'permission-denied',
      'No tienes permiso para actualizar estas métricas'
    );
  }
  
  try {
    const userRef = admin.firestore().doc(`users/${uid}`);
    
    // Obtener todos los intentos completados
    const attemptsSnapshot = await admin.firestore()
      .collection('exam_attempts')
      .where('uid', '==', uid)
      .where('status', '==', 'COMPLETED')
      .get();
    
    // Calcular métricas
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
      ? Math.round((totalCorrectAnswers / totalQuestions) * 100 * 100) / 100
      : 0.0;
    
    // Actualizar
    await userRef.update({
      totalXp: totalXp,
      averageAccuracy: averageAccuracy,
      totalAttempts: totalAttempts,
      totalCorrectAnswers: totalCorrectAnswers,
      totalQuestions: totalQuestions,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    
    return {
      success: true,
      metrics: {
        totalXp,
        averageAccuracy,
        totalAttempts,
        totalCorrectAnswers,
        totalQuestions
      }
    };
  } catch (error) {
    console.error(`Error actualizando métricas para ${uid}:`, error);
    throw new functions.https.HttpsError(
      'internal',
      'Error al actualizar métricas',
      error
    );
  }
});
```

---

## 📦 Instalación

### 1. Instalar Dependencias

```bash
cd functions
npm install firebase-functions firebase-admin
```

### 2. Desplegar

```bash
firebase deploy --only functions
```

---

## 🧪 Pruebas

### Prueba Manual

1. **Crear un intento de examen**:
```javascript
// En Firebase Console o desde la app
await db.collection('exam_attempts').add({
  uid: 'user123',
  packId: 'pack_2025_w01',
  status: 'COMPLETED',
  scoreRaw: 8,
  totalQuestions: 10,
  correctAnswers: 8,
  accuracy: 80.0,
  xpEarned: 80,
  startedAt: new Date(),
  finishedAt: new Date()
});
```

2. **Verificar que se actualizaron las métricas**:
```javascript
const user = await db.collection('users').doc('user123').get();
console.log(user.data());
// Debe mostrar: totalXp: 80, averageAccuracy: 80.0, totalAttempts: 1
```

### Prueba con Función HTTP

```javascript
// Desde la app o Postman
const updateMetrics = firebase.functions().httpsCallable('updateUserMetrics');
const result = await updateMetrics({ uid: 'user123' });
console.log(result.data);
```

---

## 📝 Notas

1. **Rendimiento**: La función se ejecuta para cada intento completado. Si tienes muchos usuarios, considera usar batch updates.

2. **Idempotencia**: La función recalcula todas las métricas desde cero cada vez, lo que asegura consistencia.

3. **Errores**: Si la función falla, Firebase la reintentará automáticamente.

4. **Logs**: Revisa los logs en Firebase Console → Functions → Logs para debugging.

---

**Última actualización**: Diciembre 2025











