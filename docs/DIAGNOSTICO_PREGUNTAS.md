# 🔍 Diagnóstico: "El pack no tiene preguntas almacenadas"

## ✅ Estado Actual

- ✅ El pack se descargó correctamente (`pack_2025_w01` aparece)
- ❌ Las preguntas no se están cargando desde Room
- ❌ Error: "El pack no tiene preguntas almacenadas"

---

## 🔍 Verificación Paso a Paso

### 1. Verificar en Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Proyecto: `eduquiz-e2829`
3. **Firestore Database**

#### Verificar el Pack

1. Colección: **`packs`**
2. Documento: **`pack_2025_w01`**
3. Verifica que tenga:
   ```json
   {
     "questionIds": [
       "q_2025_w01_0001",
       "q_2025_w01_0002",
       "q_2025_w01_0003",
       "q_2025_w01_0004",
       "q_2025_w01_0005",
       "q_2025_w01_0006"
     ]
   }
   ```

#### Verificar las Preguntas

1. Colección: **`questions`**
2. Debe existir cada pregunta:
   - `q_2025_w01_0001`
   - `q_2025_w01_0002`
   - `q_2025_w01_0003`
   - `q_2025_w01_0004`
   - `q_2025_w01_0005`
   - `q_2025_w01_0006`

3. **Haz clic en una pregunta** (ej: `q_2025_w01_0001`)
4. Verifica que tenga estos campos:
   ```json
   {
     "questionId": "q_2025_w01_0001",
     "textId": "txt_2025_w01_001",
     "packId": "pack_2025_w01",
     "prompt": "¿Cuál es la idea principal...?",
     "correctOptionId": "B",
     "difficulty": 2,
     "explanationText": "...",
     "explanationStatus": "APPROVED",
     "options": [
       { "optionId": "A", "text": "..." },
       { "optionId": "B", "text": "..." },
       { "optionId": "C", "text": "..." },
       { "optionId": "D", "text": "..." }
     ]
   }
   ```

**⚠️ IMPORTANTE**: El campo `options` debe ser un **array de objetos**, no una subcolección.

---

### 2. Re-ejecutar el Script

Si las preguntas no existen o están mal formateadas:

```bash
node scripts/init-firestore.js
```

Esto recreará todos los datos con el formato correcto.

---

### 3. Verificar Logs de Android Studio

1. Abre **Android Studio**
2. Ve a **Logcat**
3. Filtra por: `PackRepository`, `Firestore`, `Error`
4. **Descarga el pack de nuevo** (haz clic en "Descargar Pack")
5. Busca mensajes de error específicos

**Errores comunes a buscar**:
- "No se encontraron preguntas"
- "Error al parsear opciones"
- "El pack no tiene preguntas asociadas"

---

### 4. Verificar que el Pack se Descargó Correctamente

Después de descargar, verifica en Room:

1. Usa **Database Inspector** en Android Studio
2. O agrega logs temporales en el código

---

## 🚨 Problemas Comunes

### Problema 1: Las Preguntas No Existen en Firestore

**Síntoma**: El pack se descarga pero no hay preguntas

**Solución**:
1. Verifica en Firebase Console que existan las 6 preguntas
2. Si no existen, ejecuta: `node scripts/init-firestore.js`

### Problema 2: El Campo `options` Está Mal Formateado

**Síntoma**: Las preguntas se descargan pero sin opciones

**Solución**:
1. Verifica que `options` sea un array de objetos:
   ```json
   "options": [
     { "optionId": "A", "text": "..." }
   ]
   ```
2. NO debe ser una subcolección

### Problema 3: Los IDs No Coinciden

**Síntoma**: El pack tiene `questionIds` pero las preguntas no se encuentran

**Solución**:
1. Verifica que los IDs en `pack.questionIds` coincidan exactamente con los IDs de los documentos en `questions`
2. Verifica que no haya espacios o caracteres especiales

---

## ✅ Solución Rápida

1. **Ejecuta el script de nuevo**:
   ```bash
   node scripts/init-firestore.js
   ```

2. **Verifica en Firebase Console**:
   - Pack tiene `questionIds` con 6 elementos
   - Colección `questions` tiene 6 documentos
   - Cada pregunta tiene campo `options` como array

3. **Elimina el pack descargado** (si es necesario):
   - Desinstala y reinstala la app
   - O elimina los datos de la app desde Configuración

4. **Descarga el pack de nuevo** desde la app

5. **Verifica los logs** si aún no funciona

---

## 🔧 Si Nada Funciona

Revisa los logs de Android Studio (Logcat) y busca:
- Errores de Firestore
- Errores de Room
- Mensajes sobre preguntas vacías

Comparte el error específico que aparezca en los logs.

---

**El problema más probable es que las preguntas no se están descargando correctamente desde Firestore. Verifica en Firebase Console que existan y tengan el formato correcto.**











