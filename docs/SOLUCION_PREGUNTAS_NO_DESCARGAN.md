# 🔧 Solución: "El pack no tiene preguntas almacenadas"

## 🔍 Diagnóstico Rápido

El pack se descargó pero las preguntas no. Esto significa:
- ✅ El pack existe en Firestore
- ✅ El pack se descargó a Room
- ❌ Las preguntas no se descargaron o no se guardaron

---

## ✅ Verificación Inmediata en Firebase Console

### 1. Verificar el Pack

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Firestore Database → Colección `packs`
3. Documento: `pack_2025_w01` (o el ID que aparezca en tu app)
4. **Verifica el campo `questionIds`**:
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

### 2. Verificar que las Preguntas Existan

1. Firestore Database → Colección `questions`
2. **Debe existir cada pregunta** con el ID exacto:
   - `q_2025_w01_0001` ✅
   - `q_2025_w01_0002` ✅
   - `q_2025_w01_0003` ✅
   - `q_2025_w01_0004` ✅
   - `q_2025_w01_0005` ✅
   - `q_2025_w01_0006` ✅

3. **Haz clic en una pregunta** (ej: `q_2025_w01_0001`)
4. **Verifica que tenga el campo `options`**:
   ```json
   {
     "options": [
       { "optionId": "A", "text": "..." },
       { "optionId": "B", "text": "..." },
       { "optionId": "C", "text": "..." },
       { "optionId": "D", "text": "..." }
     ]
   }
   ```

**⚠️ IMPORTANTE**: 
- El campo `options` debe ser un **array de objetos**
- NO debe ser una subcolección
- Cada objeto debe tener `optionId` y `text`

---

## 🚨 Problemas Comunes

### Problema 1: Los IDs No Coinciden

**Síntoma**: El pack tiene `questionIds` pero las preguntas no se encuentran

**Solución**:
1. Verifica que los IDs en `pack.questionIds` coincidan **exactamente** con los IDs de los documentos en `questions`
2. Verifica que no haya espacios, mayúsculas/minúsculas diferentes, o caracteres especiales
3. Ejemplo correcto:
   - Pack: `questionIds: ["q_2025_w01_0001"]`
   - Firestore: Documento `q_2025_w01_0001` existe ✅

### Problema 2: Las Preguntas No Tienen Campo `options`

**Síntoma**: Las preguntas se descargan pero sin opciones

**Solución**:
1. Verifica que cada pregunta tenga el campo `options` como array
2. Re-ejecuta el script:
   ```bash
   node scripts/init-firestore.js
   ```

### Problema 3: El Campo `options` Está Vacío o Mal Formateado

**Síntoma**: Las preguntas se descargan pero el parseo de opciones falla

**Solución**:
1. Verifica el formato en Firebase Console
2. Debe ser exactamente:
   ```json
   "options": [
     { "optionId": "A", "text": "Texto de la opción A" },
     { "optionId": "B", "text": "Texto de la opción B" },
     { "optionId": "C", "text": "Texto de la opción C" },
     { "optionId": "D", "text": "Texto de la opción D" }
   ]
   ```

---

## ✅ Solución Paso a Paso

### Paso 1: Re-ejecutar el Script

```bash
node scripts/init-firestore.js
```

Esto recreará todos los datos con el formato correcto.

### Paso 2: Verificar en Firebase Console

1. Verifica que el pack tenga `questionIds` con 6 elementos
2. Verifica que existan las 6 preguntas en la colección `questions`
3. Verifica que cada pregunta tenga el campo `options` como array

### Paso 3: Eliminar el Pack Descargado

**Opción A: Desinstalar y Reinstalar la App**
- Esto elimina todos los datos de Room

**Opción B: Eliminar Datos de la App**
- Configuración → Apps → Tu App → Almacenamiento → Borrar datos

### Paso 4: Recompilar la App

```bash
./gradlew :app:assembleDebug
```

O en Android Studio: **Build → Rebuild Project**

### Paso 5: Descargar el Pack de Nuevo

1. Abre la app
2. Ve a Simulacro PISA
3. Haz clic en **"Refrescar"**
4. Haz clic en **"Descargar Pack"**
5. Espera a que termine la descarga

### Paso 6: Verificar los Logs

Si aún no funciona, revisa los logs de Android Studio (Logcat):
1. Filtra por: `PackRepository`, `Firestore`, `Error`
2. Busca el mensaje de error específico
3. Los nuevos mensajes de error te dirán:
   - Cuántas preguntas se esperaban
   - Cuántas se encontraron
   - Qué IDs se esperaban

---

## 🔍 Verificación Detallada

### Checklist de Verificación

**En Firebase Console**:
- [ ] Pack existe con ID correcto
- [ ] Pack tiene campo `questionIds` con 6 elementos
- [ ] Colección `questions` tiene 6 documentos
- [ ] Cada pregunta tiene el ID exacto que aparece en `questionIds`
- [ ] Cada pregunta tiene campo `options` como array
- [ ] Cada opción tiene `optionId` y `text`

**En la App**:
- [ ] El pack se descargó (aparece en la UI)
- [ ] Los logs no muestran errores de Firestore
- [ ] Los logs muestran cuántas preguntas se descargaron

---

## 🐛 Si Nada Funciona

1. **Revisa los logs de Android Studio (Logcat)**
   - Filtra por: `PackRepository`, `Firestore`
   - Busca el mensaje de error específico
   - Los nuevos mensajes te dirán exactamente qué está fallando

2. **Verifica que el formato en Firestore sea exacto**:
   - El campo `options` debe ser un array
   - Cada elemento del array debe ser un objeto con `optionId` y `text`

3. **Prueba descargar el pack de nuevo** después de verificar todo

---

**El problema más probable es que los IDs de las preguntas en el pack no coinciden con los IDs de los documentos en Firestore, o que las preguntas no tienen el campo `options` correctamente formateado.**










