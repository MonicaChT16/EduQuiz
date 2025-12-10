# 🔌 Cómo Conectar la App con Firestore

## ✅ Pasos para Conectar la App Android con Firestore

### 1. Verificar que los Datos Estén en Firestore

Primero, ejecuta el script actualizado para crear los datos con las colecciones correctas:

```bash
node scripts/init-firestore.js
```

**Importante**: El script ahora usa las colecciones correctas:
- ✅ `packs` (correcto)
- ✅ `texts` (antes era `content_texts`)
- ✅ `questions` (antes era `content_questions`)

### 2. Verificar en Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto: `eduquiz-e2829`
3. Ve a **Firestore Database**
4. Verifica que existan estas colecciones:
   - `packs` - Debe tener `pack_2025_w01`
   - `texts` - Debe tener 3 textos
   - `questions` - Debe tener 6 preguntas

### 3. Verificar Configuración de Firebase en la App

#### 3.1. Verificar google-services.json

Asegúrate de que el archivo `google-services.json` esté en:
```
android/app/google-services.json
```

Este archivo debe ser el descargado desde Firebase Console para tu proyecto.

#### 3.2. Verificar que Firebase esté Configurado

El código ya está configurado para:
- ✅ Buscar packs con `status = "PUBLISHED"`
- ✅ Descargar desde las colecciones correctas
- ✅ Guardar en Room automáticamente

### 4. Usar la App

#### Opción A: Desde la Pantalla de Examen

1. **Abre la app** en tu dispositivo/emulador
2. Ve a la pantalla de **Simulacro PISA**
3. La app automáticamente:
   - Busca packs publicados en Firestore
   - Muestra el pack disponible
   - Permite descargarlo

#### Opción B: Desde la Pantalla de Packs (si existe)

1. Ve a la pantalla de **Packs** o **Pack semanal**
2. Haz clic en **"Refrescar"** para buscar packs disponibles
3. Deberías ver: **"Pack disponible"** con `pack_2025_w01`
4. Haz clic en **"Descargar Pack de la Semana"**
5. Espera a que termine la descarga
6. El pack se guardará en Room y estará disponible offline

### 5. Verificar que Funcionó

Después de descargar, deberías ver:

- ✅ **Pack activo offline**: `pack_2025_w01`
- ✅ **Preguntas disponibles**: 6 preguntas
- ✅ El botón **"Iniciar intento"** debería estar habilitado

---

## 🔍 Solución de Problemas

### Problema: "No hay pack disponible"

**Causas posibles**:
1. El pack no tiene `status = "PUBLISHED"` en Firestore
2. El pack no tiene `publishedAt` definido
3. No hay conexión a internet

**Solución**:
1. Verifica en Firebase Console que el pack tenga:
   ```json
   {
     "status": "PUBLISHED",
     "publishedAt": 1234567890
   }
   ```
2. Ejecuta el script de nuevo: `node scripts/init-firestore.js`
3. Verifica tu conexión a internet

### Problema: "No se pudo descargar el pack"

**Causas posibles**:
1. Las colecciones tienen nombres incorrectos
2. Faltan campos requeridos en los documentos
3. Problemas de permisos en Firestore

**Solución**:
1. Verifica que las colecciones se llamen:
   - `packs` (no `content_packs`)
   - `texts` (no `content_texts`)
   - `questions` (no `content_questions`)
2. Verifica que cada pregunta tenga el campo `options` como array
3. Verifica las reglas de seguridad de Firestore

### Problema: "El pack se descarga pero no aparecen preguntas"

**Causas posibles**:
1. Las preguntas no tienen el campo `options` correctamente
2. El formato de `options` no es el esperado

**Solución**:
1. Verifica en Firestore que cada pregunta tenga:
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
2. Ejecuta el script de nuevo para recrear los datos

---

## 📋 Checklist de Verificación

Antes de probar, verifica:

- [ ] Script ejecutado: `node scripts/init-firestore.js`
- [ ] Colecciones en Firestore: `packs`, `texts`, `questions`
- [ ] Pack tiene `status: "PUBLISHED"`
- [ ] Pack tiene `textIds` y `questionIds` definidos
- [ ] Cada pregunta tiene `options` como array
- [ ] `google-services.json` está en `android/app/`
- [ ] App tiene conexión a internet
- [ ] Firebase está inicializado en la app

---

## 🎯 Flujo Completo

1. **Ejecutar script** → Crea datos en Firestore
2. **Abrir app** → Busca packs disponibles
3. **Refrescar** → Obtiene `pack_2025_w01` desde Firestore
4. **Descargar** → Descarga pack, textos, preguntas y opciones
5. **Guardar en Room** → Datos disponibles offline
6. **Iniciar examen** → Usa los datos de Room

---

## ✅ Resultado Esperado

Después de seguir estos pasos:

- ✅ La app detecta el pack disponible
- ✅ Puedes descargarlo con un clic
- ✅ Los datos se guardan en Room
- ✅ Puedes iniciar el simulacro offline
- ✅ Aparecen las 6 preguntas de prueba

---

**¡Listo! Tu app ahora está conectada con Firestore.** 🎉










