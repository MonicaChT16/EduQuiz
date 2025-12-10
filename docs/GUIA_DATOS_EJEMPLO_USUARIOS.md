# 📊 Guía: Crear Datos de Ejemplo para Usuarios y Resultados

## 🎯 Objetivo

Crear datos de ejemplo realistas en Firestore para probar el leaderboard y las funcionalidades de ranking en la app.

---

## 🚀 Script de Inicialización

### Script Principal: `init-users-data-firestore.js`

Este script crea:
- **10 usuarios** con diferentes niveles de rendimiento
- **Múltiples intentos de examen** para cada usuario
- **Métricas calculadas** (XP total, promedio de aciertos, etc.)
- **Diferentes códigos de colegio** (UGEL-001, UGEL-002, UGEL-003)

### Ejecutar el Script

```bash
node scripts/init-users-data-firestore.js
```

---

## 📋 Datos que se Crean

### Usuarios Creados

El script crea 10 usuarios con diferentes perfiles:

| Usuario | Colegio | XP Total | Promedio | Intentos |
|---------|---------|----------|----------|----------|
| María González | UGEL-001 | 1500 | 92.5% | 15 |
| Carlos Rodríguez | UGEL-001 | 1200 | 85.0% | 12 |
| Ana Martínez | UGEL-001 | 950 | 78.3% | 10 |
| Luis Fernández | UGEL-002 | 1800 | 95.0% | 18 |
| Sofía López | UGEL-002 | 1100 | 82.5% | 11 |
| Diego Sánchez | UGEL-002 | 800 | 70.0% | 8 |
| Valentina Torres | UGEL-003 | 1300 | 88.3% | 13 |
| Andrés Ramírez | UGEL-003 | 700 | 65.0% | 7 |
| Camila Herrera | UGEL-003 | 1000 | 80.0% | 10 |
| Sebastián Jiménez | UGEL-001 | 600 | 60.0% | 6 |

### Intentos de Examen

Cada usuario tiene múltiples intentos de examen con:
- Resultados variados (basados en su promedio objetivo)
- Diferentes fechas (simulando actividad a lo largo del tiempo)
- XP ganado por intento
- Porcentaje de aciertos calculado

---

## 🔍 Verificar en Firebase Console

### 1. Verificar Colección `users`

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Firestore Database → Colección `users`
3. Debes ver 10 documentos con los usuarios creados

**Verifica que cada usuario tenga**:
- ✅ `uid`: ID único
- ✅ `displayName`: Nombre del usuario
- ✅ `email`: Email del usuario
- ✅ `schoolCode`: Código de colegio (UGEL-001, UGEL-002, UGEL-003)
- ✅ `totalXp`: XP total acumulado
- ✅ `averageAccuracy`: Promedio de aciertos (0-100)
- ✅ `totalAttempts`: Número de intentos
- ✅ `totalCorrectAnswers`: Total de respuestas correctas
- ✅ `totalQuestions`: Total de preguntas respondidas

### 2. Verificar Colección `exam_attempts`

1. Firestore Database → Colección `exam_attempts`
2. Debes ver múltiples documentos (aproximadamente 110 intentos en total)

**Verifica que cada intento tenga**:
- ✅ `attemptId`: ID único del intento
- ✅ `uid`: ID del usuario
- ✅ `packId`: ID del pack (pack_2025_w01)
- ✅ `status`: "COMPLETED"
- ✅ `correctAnswers`: Número de respuestas correctas
- ✅ `totalQuestions`: Total de preguntas (10)
- ✅ `accuracy`: Porcentaje de aciertos
- ✅ `xpEarned`: XP ganado en este intento

---

## 📊 Probar el Leaderboard

### Leaderboard Global

Después de ejecutar el script, puedes probar:

1. **Top por XP**:
   - Luis Fernández: 1800 XP
   - María González: 1500 XP
   - Valentina Torres: 1300 XP
   - Carlos Rodríguez: 1200 XP
   - Sofía López: 1100 XP

2. **Top por Promedio de Aciertos**:
   - Luis Fernández: 95.0%
   - María González: 92.5%
   - Valentina Torres: 88.3%
   - Carlos Rodríguez: 85.0%
   - Sofía López: 82.5%

### Leaderboard por Colegio

**UGEL-001**:
- María González: 1500 XP (92.5%)
- Carlos Rodríguez: 1200 XP (85.0%)
- Ana Martínez: 950 XP (78.3%)
- Sebastián Jiménez: 600 XP (60.0%)

**UGEL-002**:
- Luis Fernández: 1800 XP (95.0%)
- Sofía López: 1100 XP (82.5%)
- Diego Sánchez: 800 XP (70.0%)

**UGEL-003**:
- Valentina Torres: 1300 XP (88.3%)
- Camila Herrera: 1000 XP (80.0%)
- Andrés Ramírez: 700 XP (65.0%)

---

## 🔧 Personalizar los Datos

Si quieres modificar los datos de ejemplo, edita el array `exampleUsers` en `scripts/init-users-data-firestore.js`:

```javascript
const exampleUsers = [
  {
    uid: 'user_demo_001',
    displayName: 'María González',
    email: 'maria.gonzalez@example.com',
    schoolCode: 'UGEL-001',
    photoUrl: null,
    targetXp: 1500,        // ← Cambia el XP objetivo
    targetAccuracy: 92.5,  // ← Cambia el promedio objetivo
    attempts: 15           // ← Cambia el número de intentos
  },
  // ... más usuarios
];
```

---

## ⚠️ Notas Importantes

1. **Los usuarios son de ejemplo**: Estos usuarios NO están autenticados en Firebase Auth. Son solo documentos en Firestore para pruebas.

2. **Para usuarios reales**: Cuando un usuario real hace login con Google:
   - Firebase Auth crea el usuario automáticamente
   - El perfil se crea en Room con datos de Firebase Auth (`uid`, `displayName`, `email`, `photoUrl` de Gmail)
   - El perfil se sincroniza a Firestore automáticamente

3. **Métricas REALES**: 
   - Las métricas NO son inventadas
   - Se calculan REALMENTE desde los intentos de examen creados
   - Cada intento tiene resultados reales (`correctAnswers`, `xpEarned`)
   - Las métricas se suman desde todos los intentos: `totalXp = suma(xpEarned)`, `averageAccuracy = (suma(correctAnswers) / suma(totalQuestions)) * 100`

4. **Foto de Perfil**:
   - En usuarios reales: viene de Gmail (Firebase Auth) automáticamente
   - En usuarios de ejemplo: es `null` porque no están autenticados

5. **Eliminar datos de ejemplo**: Si quieres eliminar los datos de ejemplo:
   ```javascript
   // En Firebase Console, elimina manualmente:
   // - Colección users: documentos user_demo_001 a user_demo_010
   // - Colección exam_attempts: documentos attempt_user_demo_*_*
   ```

---

## ✅ Checklist

- [ ] Script ejecutado: `node scripts/init-users-data-firestore.js`
- [ ] 10 usuarios creados en colección `users`
- [ ] Múltiples intentos creados en colección `exam_attempts`
- [ ] Métricas calculadas correctamente (totalXp, averageAccuracy)
- [ ] Leaderboard funciona en la app
- [ ] Filtrado por colegio funciona correctamente

---

## 🎯 Próximos Pasos

1. **Probar el leaderboard en la app**:
   - Abre la pantalla de ranking
   - Verifica que se muestren los usuarios
   - Prueba ordenar por XP o por promedio
   - Prueba filtrar por colegio

2. **Verificar consultas**:
   - Revisa que las consultas de Firestore funcionen correctamente
   - Verifica que los índices compuestos estén creados

3. **Probar con usuarios reales**:
   - Cuando un usuario real hace login, su perfil se crea automáticamente
   - Las métricas se actualizan cuando completa exámenes

---

**Última actualización**: Diciembre 2025

