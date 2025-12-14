# 🔍 Diagnóstico: ¿Por qué solo se muestran 6 preguntas?

## 📊 Situación Actual

### En Firestore (Base de datos remota) ✅
- **Total de preguntas**: 32
  - **LECTURA**: 12 preguntas
  - **CIENCIAS**: 10 preguntas
  - **MATEMATICA**: 10 preguntas

### En la App (Base de datos local Room) ⚠️
- **Muestra**: Solo 6 preguntas disponibles
- **Warning**: "Comprensión Lectora solo tiene 2 preguntas, pero se requieren 10"

---

## 🔎 Causa del Problema

El problema es que **la app muestra las preguntas que están descargadas en Room** (la base de datos local del dispositivo), no las que están en Firestore.

Posibles causas:
1. **El pack no se descargó completamente** - Solo se descargaron 6 preguntas a Room
2. **Problema al descargar** - Hubo un error durante la descarga que interrumpió el proceso
3. **Datos parciales** - Se descargó una versión anterior con menos preguntas

---

## 🔧 Solución

### Opción 1: Volver a descargar el pack (Recomendado)

1. En la app, ve a la pantalla de "Simulacro PISA"
2. Elimina el pack actual si existe
3. Descarga el pack nuevamente desde Firestore
4. Verifica que ahora muestre 32 preguntas

### Opción 2: Verificar datos en Room

Si quieres verificar qué hay realmente en Room, puedes:
1. Revisar los logs de la app cuando descarga el pack
2. Buscar mensajes como "Descargando preguntas..." o "Preguntas descargadas: X"

### Opción 3: Limpiar y reinstalar

Si el problema persiste:
1. Desinstala la app
2. Reinstala
3. Inicia sesión nuevamente
4. Descarga el pack

---

## 📝 Nota sobre el Mapeo de Subjects

El código tiene un mapeo que convierte automáticamente:
- `"LECTURA"` → `"COMPRENSION_LECTORA"`

Este mapeo ocurre en:
- `PackRemoteDataSource.kt` (línea 236) - Al descargar desde Firestore
- `PackRepositoryImpl.kt` (línea 170) - Al buscar preguntas por subject

Entonces, aunque en Firestore el subject es "LECTURA", la app debería encontrar esas 12 preguntas cuando busca "COMPRENSION_LECTORA".

---

## ✅ Verificación

Para verificar que todo esté correcto después de descargar:

1. **En Firestore** (usando el script):
   ```bash
   node scripts/check-pack-questions.js
   ```
   Debe mostrar: 32 preguntas (12 LECTURA + 10 CIENCIAS + 10 MATEMATICA)

2. **En la app**:
   - Debe mostrar "Preguntas disponibles: 32" (o al menos más de 10 por materia)
   - No debe mostrar el warning sobre Comprensión Lectora con solo 2 preguntas

---

## 🚨 Si el Problema Persiste

Si después de volver a descargar el pack sigue mostrando solo 6 preguntas:

1. **Revisa los logs** de la app durante la descarga:
   - Busca mensajes de error
   - Verifica que se descarguen todas las preguntas

2. **Verifica la estructura de datos**:
   - Asegúrate que en Firestore, el campo `subject` de los textos sea correcto
   - Verifica que las preguntas tengan `options` como array, no como subcolección

3. **Verifica el código de descarga**:
   - Revisa `PackRemoteDataSource.kt` para ver si hay algún límite o filtro
   - Verifica que el proceso de descarga sea atómico (todo o nada)

---

## 📊 Datos Esperados vs Actuales

| Subject | Firestore | App (Esperado) | App (Actual) | Estado |
|---------|-----------|----------------|--------------|--------|
| LECTURA → COMPRENSION_LECTORA | 12 | 12 | 2 | ❌ |
| CIENCIAS | 10 | 10 | ? | ❓ |
| MATEMATICA | 10 | 10 | ? | ❓ |
| **TOTAL** | **32** | **32** | **6** | ❌ |

---

**Fecha del análisis**: 2025-01-27