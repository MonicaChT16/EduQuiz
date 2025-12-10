# 📊 Actualizar Base de Datos para Sistema de Ranking

## 🎯 Objetivo

Actualizar los documentos de usuarios en Firestore para incluir los nuevos campos necesarios para el sistema de ranking:
- `totalScore` (Int): Puntaje total (XP) - mismo valor que `xp`
- `accuracy` (Float): Precisión promedio en porcentaje (0.0 a 100.0)
- `examsCompleted` (Int): Número de exámenes completados

## ✅ Solución Automática

El código ya está actualizado para calcular y sincronizar estos campos automáticamente:

1. **`FirestoreSyncService.syncUserProfile()`** ahora calcula y sincroniza:
   - `accuracy`: Calculado desde los intentos completados y respuestas correctas
   - `examsCompleted`: Contador de exámenes con status `COMPLETED` o `AUTO_SUBMIT`
   - `totalScore`: Mismo valor que `xp` (para compatibilidad)

2. **Cuando un usuario completa un examen**, el sistema:
   - Calcula automáticamente las estadísticas
   - Las sincroniza con Firestore en la próxima sincronización

## 🔄 Actualizar Usuarios Existentes

### Opción 1: Automático (Recomendado)

Los usuarios existentes se actualizarán automáticamente cuando:
1. **Completen un nuevo examen** - El sistema calculará y sincronizará los nuevos valores
2. **Se ejecute la sincronización** - El `SyncWorker` sincronizará los perfiles pendientes

### Opción 2: Manual desde la App

Si quieres forzar la actualización de todos los usuarios:

1. **Abre la app** en cada dispositivo
2. **Inicia sesión** con cada cuenta de usuario
3. El sistema calculará y sincronizará automáticamente los valores

### Opción 3: Script de Firebase Functions (Opcional)

Si tienes acceso a Firebase Functions, puedes crear un script para actualizar todos los usuarios de una vez:

```javascript
// functions/src/updateRankingStats.js
const admin = require('firebase-admin');
const functions = require('firebase-functions');

exports.updateAllUserRankingStats = functions.https.onCall(async (data, context) => {
  // Verificar autenticación de administrador
  if (!context.auth || !context.auth.token.admin) {
    throw new functions.https.HttpsError('permission-denied', 'Admin only');
  }

  const usersRef = admin.firestore().collection('users');
  const usersSnapshot = await usersRef.get();
  
  const batch = admin.firestore().batch();
  let count = 0;

  for (const userDoc of usersSnapshot.docs) {
    const uid = userDoc.id;
    const userData = userDoc.data();
    
    // Obtener intentos de examen
    const attemptsRef = usersRef.doc(uid).collection('examAttempts');
    const attemptsSnapshot = await attemptsRef
      .where('status', 'in', ['COMPLETED', 'AUTO_SUBMIT'])
      .get();
    
    let totalCorrect = 0;
    let totalAnswered = 0;
    let examsCompleted = attemptsSnapshot.size;
    
    // Calcular accuracy desde las respuestas
    for (const attemptDoc of attemptsSnapshot.docs) {
      const answersRef = attemptDoc.ref.collection('answers');
      const answersSnapshot = await answersRef.get();
      
      answersSnapshot.forEach(answerDoc => {
        const answerData = answerDoc.data();
        totalAnswered++;
        if (answerData.isCorrect === true) {
          totalCorrect++;
        }
      });
    }
    
    const accuracy = totalAnswered > 0 
      ? (totalCorrect / totalAnswered) * 100 
      : 0;
    
    const totalScore = userData.xp ? parseInt(userData.xp) : 0;
    
    // Actualizar documento del usuario
    batch.update(userDoc.ref, {
      totalScore: totalScore,
      accuracy: accuracy,
      examsCompleted: examsCompleted,
      lastRankingUpdate: admin.firestore.FieldValue.serverTimestamp()
    });
    
    count++;
    
    // Firestore batch limit es 500
    if (count % 500 === 0) {
      await batch.commit();
      batch = admin.firestore().batch();
    }
  }
  
  // Commit final
  if (count % 500 !== 0) {
    await batch.commit();
  }
  
  return { success: true, usersUpdated: count };
});
```

## 📋 Estructura de Datos en Firestore

Cada documento en `users/{uid}` debe tener:

```json
{
  "uid": "user123",
  "displayName": "Juan Pérez",
  "photoUrl": "https://...",
  "schoolId": "school1",
  "classroomId": "class1",
  "coins": 500,
  "xp": 1250,
  "totalScore": 1250,        // ← NUEVO: Mismo que xp
  "accuracy": 85.5,          // ← NUEVO: Precisión en %
  "examsCompleted": 12,      // ← NUEVO: Exámenes completados
  "selectedCosmeticId": null,
  "updatedAtLocal": 1234567890,
  "syncState": "SYNCED",
  "lastSyncedAt": 1234567890
}
```

## 🔍 Verificar en Firebase Console

1. **Abre Firebase Console**
2. **Ve a Firestore Database**
3. **Navega a `users/{uid}`**
4. **Verifica que existan los campos**:
   - ✅ `totalScore` (número)
   - ✅ `accuracy` (número decimal)
   - ✅ `examsCompleted` (número)

## ⚠️ Notas Importantes

1. **Los valores se calculan desde la base de datos local (Room)**, no desde Firestore
2. **Si un usuario no tiene exámenes completados**, los valores serán:
   - `accuracy`: 0.0
   - `examsCompleted`: 0
   - `totalScore`: valor de `xp`
3. **La sincronización es automática** cuando el usuario completa un examen
4. **Los valores se actualizan cada vez que se sincroniza el perfil**

## 🚀 Próximos Pasos

1. **Ejecuta la app** y completa un examen con un usuario de prueba
2. **Verifica en Firebase Console** que los campos se hayan actualizado
3. **Revisa el ranking** en la app para confirmar que los datos se muestran correctamente

## 📝 Campos Adicionales para Ranking por Colegio

Si quieres filtrar por colegio, asegúrate de que cada usuario tenga:
- `school_code` (String): Código del colegio/UGEL

Este campo se puede agregar manualmente en Firebase Console o desde la app cuando el usuario se registra.










