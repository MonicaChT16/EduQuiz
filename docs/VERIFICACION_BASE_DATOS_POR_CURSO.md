# ✅ Verificación: Base de Datos para Examen por Curso/Materia

## 📋 Resumen

Se ha verificado que la base de datos está **correctamente configurada** para filtrar preguntas por curso/materia. El sistema utiliza un JOIN entre `question_entity` y `text_entity` para obtener preguntas filtradas por materia.

---

## 🔍 Verificación de la Consulta SQL

### Consulta Actual

La consulta que filtra preguntas por materia está en `ContentDao.getQuestionsByPackAndSubject()`:

```sql
SELECT q.* FROM question_entity q
INNER JOIN text_entity t ON q.textId = t.textId
WHERE q.packId = :packId AND t.subject = :subject
```

**✅ Esta consulta es correcta** porque:
- Hace un JOIN correcto entre `question_entity` y `text_entity` usando `textId`
- Filtra por `packId` (índice disponible)
- Filtra por `subject` del texto asociado

---

## 📊 Estructura de Datos

### Relación entre Entidades

```
PackEntity (pack_entity)
    ↓ (1:N)
TextEntity (text_entity)
    - textId (PK)
    - packId (FK → PackEntity)
    - subject (MATEMATICA, COMPRENSION_LECTORA, CIENCIAS)
    ↓ (1:N)
QuestionEntity (question_entity)
    - questionId (PK)
    - packId (FK → PackEntity)
    - textId (FK → TextEntity)
```

**✅ La estructura es correcta**: Las preguntas se relacionan con textos mediante `textId`, y los textos tienen el campo `subject`.

---

## 🔑 Índices Actuales

### Índices Configurados

1. **text_entity**:
   - `packId` (índice simple)

2. **question_entity**:
   - `packId` (índice simple)
   - `textId` (índice simple)

### ⚠️ Posible Mejora de Rendimiento

La consulta actual funciona correctamente, pero podría beneficiarse de un **índice compuesto** en `text_entity` para mejorar el rendimiento cuando hay muchos textos:

```kotlin
indices = [
    Index("packId"),
    Index("packId", "subject") // Índice compuesto para consultas por pack y materia
]
```

**Nota**: Esta mejora es opcional y solo necesaria si hay problemas de rendimiento con muchos datos.

---

## ✅ Flujo de Funcionamiento

### 1. Descarga de Pack

Cuando se descarga un pack desde Firestore:
- Los textos se guardan en `text_entity` con su `subject` normalizado
- Las preguntas se guardan en `question_entity` con referencia a `textId`
- La normalización de materias se hace en `PackRemoteDataSource.toTextRemote()`

### 2. Selección de Materia

Cuando el usuario selecciona una materia (ej: MATEMATICA):
1. Se llama a `ExamViewModel.startExam(subject = "MATEMATICA")`
2. Se ejecuta `prepareQuestions(packId, subject)`
3. Se llama a `packRepository.getQuestionsForPackBySubject(packId, subject)`
4. Se ejecuta la consulta SQL con JOIN
5. Se obtienen solo las preguntas asociadas a textos de esa materia
6. Se limitan a 10 preguntas (formato PISA)

### 3. Guardado del Intent

Cuando se inicia el examen:
- Se guarda el `subject` en `exam_attempt_entity`
- Esto permite filtrar correctamente las preguntas al revisar el examen

---

## 🧪 Verificación de Casos de Uso

### Caso 1: Examen de Matemáticas
```kotlin
// Usuario selecciona "Matemáticas"
viewModel.startExam(Subject.MATEMATICA)

// Consulta SQL ejecutada:
// SELECT q.* FROM question_entity q
// INNER JOIN text_entity t ON q.textId = t.textId
// WHERE q.packId = 'pack-123' AND t.subject = 'MATEMATICA'
```
**✅ Resultado esperado**: Solo preguntas de textos con `subject = 'MATEMATICA'`

### Caso 2: Examen de Comprensión Lectora
```kotlin
viewModel.startExam(Subject.COMPRENSION_LECTORA)
```
**✅ Resultado esperado**: Solo preguntas de textos con `subject = 'COMPRENSION_LECTORA'`

### Caso 3: Examen de Ciencias
```kotlin
viewModel.startExam(Subject.CIENCIAS)
```
**✅ Resultado esperado**: Solo preguntas de textos con `subject = 'CIENCIAS'`

---

## 🔧 Normalización de Materias

El sistema normaliza automáticamente los valores de materia desde Firestore:

```kotlin
// En PackRemoteDataSource.toTextRemote()
val subject = when (rawSubject.uppercase()) {
    "LECTURA", "LECTURA_COMPRENSION", "COMPRENSION" -> Subject.COMPRENSION_LECTORA
    "MATEMATICA", "MATEMATICAS", "MATH" -> Subject.MATEMATICA
    "CIENCIAS", "CIENCIA", "SCIENCE" -> Subject.CIENCIAS
    else -> rawSubject.uppercase()
}
```

**✅ Esto asegura** que los valores en la base de datos sean consistentes.

---

## ⚠️ Posibles Problemas y Soluciones

### Problema 1: No se encuentran preguntas para una materia

**Causa posible**: 
- El pack no tiene textos con esa materia
- Los textos no tienen el campo `subject` correctamente configurado en Firestore

**Solución**:
1. Verificar en Firestore que los textos tengan el campo `subject` con valores válidos
2. Verificar que las preguntas estén asociadas a textos con `textId` correcto
3. Revisar los logs de descarga del pack

### Problema 2: Consulta lenta con muchos datos

**Causa posible**: 
- Falta de índice compuesto en `text_entity`

**Solución**:
- Agregar índice compuesto `(packId, subject)` en `text_entity` (ver sección de mejoras)

### Problema 3: Preguntas de materia incorrecta

**Causa posible**: 
- El `textId` en `question_entity` no coincide con el `textId` en `text_entity`
- El `subject` en Firestore no está normalizado correctamente

**Solución**:
1. Verificar la integridad referencial en la base de datos
2. Verificar que los datos en Firestore estén correctos
3. Re-descargar el pack si es necesario

---

## 📝 Recomendaciones

### ✅ Implementado Correctamente

1. **Consulta SQL con JOIN**: ✅ Correcta
2. **Índices básicos**: ✅ Configurados
3. **Normalización de materias**: ✅ Implementada
4. **Guardado de materia en intento**: ✅ Implementado
5. **Filtrado en revisión**: ✅ Implementado

### 🔄 Mejoras Opcionales (Solo si hay problemas de rendimiento)

1. **Índice compuesto en text_entity**:
   ```kotlin
   @Entity(
       tableName = "text_entity",
       indices = [
           Index("packId"),
           Index(value = ["packId", "subject"]) // Índice compuesto
       ]
   )
   ```

2. **Verificación de integridad**:
   - Agregar validación al descargar packs para asegurar que todas las preguntas tengan textos asociados

---

## ✅ Conclusión

La base de datos está **correctamente configurada** para filtrar preguntas por curso/materia. El sistema:

- ✅ Utiliza JOIN correcto entre `question_entity` y `text_entity`
- ✅ Filtra correctamente por `packId` y `subject`
- ✅ Guarda la materia en el intento de examen
- ✅ Filtra correctamente al revisar exámenes
- ✅ Normaliza los valores de materia desde Firestore

**No se requieren cambios** a menos que haya problemas específicos de rendimiento o datos.

---

## 🧪 Cómo Verificar Manualmente

### 1. Verificar en la Base de Datos Local

Usa Database Inspector en Android Studio:
1. Abre Database Inspector
2. Ejecuta esta consulta:
   ```sql
   SELECT q.questionId, q.textId, t.subject 
   FROM question_entity q
   INNER JOIN text_entity t ON q.textId = t.textId
   WHERE q.packId = 'TU_PACK_ID'
   ```
3. Verifica que las preguntas estén asociadas a textos con `subject` correcto

### 2. Verificar en Logs

Busca en Logcat:
- `ExamViewModel`: Mensajes sobre carga de preguntas
- `PackRepositoryImpl`: Mensajes sobre consultas a la base de datos

### 3. Probar en la App

1. Descarga un pack
2. Selecciona cada materia (Matemáticas, Comprensión Lectora, Ciencias)
3. Verifica que solo aparezcan preguntas de esa materia
4. Completa un examen y verifica la revisión

---

## 📚 Referencias

- Consulta SQL: `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt` (línea 292-297)
- Implementación: `android/data/src/main/java/com/eduquiz/data/repository/PackRepositoryImpl.kt` (línea 136-137)
- Uso en ViewModel: `android/feature-exam/src/main/java/com/eduquiz/feature/exam/ExamViewModel.kt` (línea 372-391)






