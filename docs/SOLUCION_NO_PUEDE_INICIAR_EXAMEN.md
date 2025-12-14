# 🔧 Solución: No Puede Iniciar el Examen

## ❌ Problema
Al seleccionar una materia (Matemáticas, Comprensión Lectora o Ciencias) y presionar "Iniciar intento", el examen no se inicia.

---

## ✅ Soluciones Implementadas

### 1. Mejoras en el Manejo de Errores

Se han mejorado los mensajes de error para que sean más informativos:

- **Antes**: Si faltaba `userId` o `pack`, la función retornaba silenciosamente sin mostrar error
- **Ahora**: Se muestran mensajes claros indicando qué falta

### 2. Logs de Debugging Agregados

Se agregaron logs detallados para diagnosticar problemas:
- Logs cuando se carga el examen
- Logs de cuántos textos y preguntas se encuentran
- Logs de errores específicos

---

## 🔍 Diagnóstico Paso a Paso

### Paso 1: Verificar Logs en Android Studio

1. Abre **Android Studio**
2. Conecta tu dispositivo o inicia el emulador
3. Abre **Logcat** (View → Tool Windows → Logcat)
4. Filtra por: `ExamViewModel`
5. Intenta iniciar un examen
6. Busca estos mensajes:

**Si todo está bien:**
```
ExamViewModel: startExam: packId=pack-123, subject=MATEMATICA
ExamViewModel: prepareQuestions: packId=pack-123, subject=MATEMATICA
ExamViewModel: Found X texts for pack pack-123
ExamViewModel: Found Y texts for subject MATEMATICA
ExamViewModel: Found Z questions for subject MATEMATICA
ExamViewModel: Prepared N exam contents
```

**Si hay problemas:**
```
ExamViewModel: startExam: userId is null
ExamViewModel: startExam: pack is null
ExamViewModel: Error preparing questions for subject MATEMATICA
ExamViewModel: No questions found for packId=pack-123, subject=MATEMATICA
```

---

### Paso 2: Verificar Mensajes de Error en la App

Ahora la app muestra mensajes de error más claros:

#### Error: "Usuario no identificado"
**Causa**: El usuario no está autenticado o la sesión expiró.

**Solución**:
1. Cierra sesión
2. Vuelve a iniciar sesión con Google
3. Intenta iniciar el examen nuevamente

#### Error: "No hay pack activo"
**Causa**: No hay un pack descargado en el dispositivo.

**Solución**:
1. Verifica que hayas descargado un pack
2. Si no hay pack, descárgalo desde la pantalla de inicio
3. Intenta iniciar el examen nuevamente

#### Error: "No hay preguntas disponibles para [Materia]"
**Causa**: El pack no tiene preguntas para esa materia específica.

**Posibles causas**:
- El pack en Firestore no tiene textos con esa materia
- Los textos no tienen el campo `subject` correctamente configurado
- Las preguntas no están asociadas a textos con esa materia

**Solución**:
1. Verifica en Firestore que los textos tengan el campo `subject` con valores:
   - `MATEMATICA`
   - `COMPRENSION_LECTORA`
   - `CIENCIAS`
2. Verifica que las preguntas estén asociadas a textos con `textId` correcto
3. Intenta con otra materia
4. Si el problema persiste, re-descarga el pack

---

### Paso 3: Verificar Base de Datos Local

Usa **Database Inspector** en Android Studio:

1. Abre Database Inspector
2. Ejecuta esta consulta para ver textos por materia:
   ```sql
   SELECT textId, packId, subject, title 
   FROM text_entity 
   WHERE packId = 'TU_PACK_ID'
   ```
3. Verifica que haya textos con `subject` correcto

4. Ejecuta esta consulta para ver preguntas por materia:
   ```sql
   SELECT q.questionId, q.textId, t.subject 
   FROM question_entity q
   INNER JOIN text_entity t ON q.textId = t.textId
   WHERE q.packId = 'TU_PACK_ID' AND t.subject = 'MATEMATICA'
   ```
5. Verifica que haya preguntas para la materia seleccionada

---

### Paso 4: Verificar Firestore

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Proyecto: `eduquiz-e2829`
3. **Firestore Database** → **Datos**
4. Verifica la colección `texts`:
   - Cada texto debe tener el campo `subject` con valores:
     - `MATEMATICA`
     - `COMPRENSION_LECTORA`
     - `CIENCIAS`
   - O valores antiguos que se normalizan automáticamente:
     - `LECTURA`, `LECTURA_COMPRENSION`, `COMPRENSION` → `COMPRENSION_LECTORA`
     - `MATEMATICAS`, `MATH` → `MATEMATICA`
     - `CIENCIA`, `SCIENCE` → `CIENCIAS`

5. Verifica la colección `questions`:
   - Cada pregunta debe tener el campo `textId` que coincida con un `textId` en la colección `texts`

---

## 🔧 Soluciones Rápidas

### Solución 1: Re-descargar el Pack

Si el pack está corrupto o incompleto:

1. Ve a la pantalla de examen
2. Si hay un pack activo, elimínalo (si es posible) o descarga uno nuevo
3. Descarga el pack más reciente
4. Intenta iniciar el examen nuevamente

### Solución 2: Verificar Conexión a Internet

Algunos datos pueden necesitar descargarse:

1. Verifica que tengas conexión a internet
2. Intenta refrescar el pack disponible
3. Descarga el pack nuevamente si es necesario

### Solución 3: Limpiar Datos de la App

Si hay datos corruptos en la base de datos local:

1. Ve a **Configuración** → **Apps** → **EduQuiz**
2. **Almacenamiento** → **Borrar datos**
3. Abre la app nuevamente
4. Inicia sesión
5. Descarga un pack
6. Intenta iniciar el examen

**⚠️ Nota**: Esto eliminará todos los datos locales, incluyendo progreso no sincronizado.

---

## 📊 Verificación de Datos en Firestore

### Estructura Correcta de un Texto

```json
{
  "textId": "text-123",
  "packId": "pack-456",
  "title": "Título del Texto",
  "body": "Contenido del texto...",
  "subject": "MATEMATICA"  // ← Debe estar presente y ser correcto
}
```

### Estructura Correcta de una Pregunta

```json
{
  "questionId": "question-789",
  "packId": "pack-456",
  "textId": "text-123",  // ← Debe coincidir con un textId existente
  "prompt": "¿Cuál es la respuesta?",
  "correctOptionId": "option-A",
  "difficulty": 1,
  "explanationStatus": "NONE"
}
```

---

## 🧪 Pruebas Manuales

### Test 1: Verificar que se Puede Iniciar un Examen

1. Abre la app
2. Ve a la pantalla de examen
3. Verifica que haya un pack activo
4. Selecciona "Matemáticas"
5. Presiona "Iniciar intento"
6. **Resultado esperado**: El examen debe iniciar y mostrar la primera pregunta

### Test 2: Verificar Mensajes de Error

1. Si no hay pack activo, intenta iniciar un examen
2. **Resultado esperado**: Debe mostrar "No hay pack activo. Por favor, descarga un pack primero."

3. Si hay pack pero no tiene preguntas para una materia:
   - Selecciona esa materia
   - Presiona "Iniciar intento"
   - **Resultado esperado**: Debe mostrar un mensaje indicando que no hay preguntas para esa materia

---

## 📝 Logs Útiles para Debugging

Busca estos tags en Logcat:

- `ExamViewModel`: Logs principales del ViewModel
- `PackRepositoryImpl`: Logs de carga de packs
- `ContentDao`: Logs de consultas a la base de datos (si están habilitados)

---

## ⚠️ Problemas Comunes

### 1. "No hay preguntas disponibles para Matemáticas"

**Causa**: El pack no tiene textos con `subject = "MATEMATICA"` o las preguntas no están asociadas correctamente.

**Solución**:
- Verifica en Firestore que los textos tengan `subject` correcto
- Verifica que las preguntas tengan `textId` que coincida con textos existentes
- Re-descarga el pack

### 2. El botón "Iniciar intento" no hace nada

**Causa**: Puede ser que:
- El `userId` sea null (no autenticado)
- El `pack` sea null (no hay pack activo)
- Hay un error silencioso

**Solución**:
- Revisa los logs en Logcat
- Verifica que estés autenticado
- Verifica que haya un pack activo
- Revisa los mensajes de error en la pantalla

### 3. El examen se inicia pero no hay preguntas

**Causa**: Las preguntas se cargaron pero están vacías o no tienen opciones.

**Solución**:
- Verifica en la base de datos que las preguntas tengan opciones asociadas
- Re-descarga el pack
- Verifica los logs para ver cuántas preguntas se cargaron

---

## 🎯 Próximos Pasos

Si después de seguir estos pasos aún no funciona:

1. **Comparte los logs** de Logcat (filtrados por `ExamViewModel`)
2. **Comparte el mensaje de error** que aparece en la pantalla
3. **Verifica en Firestore** que los datos estén correctos
4. **Verifica en Database Inspector** que los datos locales estén correctos

---

## ✅ Cambios Realizados

1. ✅ Mejorado manejo de errores en `startExam()`
2. ✅ Agregados logs detallados para debugging
3. ✅ Mensajes de error más informativos
4. ✅ Validación de `userId` y `pack` antes de iniciar
5. ✅ Logs de cuántos textos y preguntas se encuentran por materia






