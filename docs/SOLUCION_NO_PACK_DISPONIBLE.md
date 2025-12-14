# 🔧 Solución: "No hay packs disponibles"

## 🔍 Diagnóstico del Problema

Si ves el mensaje **"No hay packs disponibles en este momento"**, puede ser por varias razones:

### Posibles Causas

1. **El pack no existe en Firestore**
2. **El pack no tiene `status = "PUBLISHED"`**
3. **Falta el índice compuesto en Firestore** (requerido para la consulta)
4. **Problemas de permisos en Firestore**
5. **No hay conexión a internet**
6. **El script no se ejecutó correctamente**

---

## ✅ Solución 1: Verificar que el Pack Existe en Firestore

### Paso 1: Ir a Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto: `eduquiz-e2829`
3. Ve a **Firestore Database**

### Paso 2: Verificar la Colección `packs`

1. Busca la colección **`packs`**
2. Debe existir el documento **`pack_2025_w01`**
3. Haz clic en el documento para ver su contenido

### Paso 3: Verificar los Campos del Pack

El documento debe tener estos campos:

```json
{
  "packId": "pack_2025_w01",
  "weekLabel": "2025-W01",
  "status": "PUBLISHED",  ← DEBE SER EXACTAMENTE "PUBLISHED"
  "publishedAt": 1234567890,  ← DEBE SER UN NÚMERO (timestamp)
  "textIds": ["txt_2025_w01_001", "txt_2025_w01_002", "txt_2025_w01_003"],
  "questionIds": ["q_2025_w01_0001", "q_2025_w01_0002", ...]
}
```

**⚠️ IMPORTANTE**:
- `status` debe ser exactamente `"PUBLISHED"` (en mayúsculas)
- `publishedAt` debe ser un número (no un timestamp de Firestore)

---

## ✅ Solución 2: Re-ejecutar el Script

Si el pack no existe o está mal configurado:

1. **Ejecuta el script de nuevo**:
   ```bash
   node scripts/init-firestore.js
   ```

2. **Verifica la salida**:
   Deberías ver:
   ```
   ✅ Pack creado: pack_2025_w01
   ✅ Texto creado: txt_2025_w01_001 (LECTURA)
   ✅ Texto creado: txt_2025_w01_002 (MATEMATICA)
   ✅ Texto creado: txt_2025_w01_003 (CIENCIAS)
   ✅ Pregunta creada: q_2025_w01_0001 (B es correcta)
   ...
   ✅ Pack actualizado con referencias a textos y preguntas
   ```

3. **Verifica en Firebase Console** que el pack se creó correctamente

---

## ✅ Solución 3: Crear Índice Compuesto en Firestore

La consulta requiere un índice compuesto. Firestore te mostrará un error con un enlace para crearlo automáticamente.

### Opción A: Crear desde el Error (Recomendado)

1. **Ejecuta la app** y haz clic en "Refrescar"
2. **Revisa los logs** de Android Studio (Logcat)
3. Busca un error que diga algo como:
   ```
   The query requires an index. You can create it here: https://console.firebase.google.com/...
   ```
4. **Haz clic en el enlace** o cópialo y ábrelo en el navegador
5. **Crea el índice** automáticamente desde Firebase Console

### Opción B: Crear Manualmente

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto
3. Ve a **Firestore Database** → **Índices**
4. Haz clic en **"Crear índice"**
5. Configura:
   - **Colección**: `packs`
   - **Campos del índice**:
     - `status` (Ascendente)
     - `publishedAt` (Descendente)
   - **Estado de consulta**: Habilitado
6. Haz clic en **"Crear"**
7. Espera a que el índice se cree (puede tardar unos minutos)

---

## ✅ Solución 4: Verificar Reglas de Seguridad de Firestore

### Verificar Reglas Actuales

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto
3. Ve a **Firestore Database** → **Reglas**

### Reglas Mínimas Necesarias

Para que la app pueda leer los packs, las reglas deben permitir lectura:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Permitir lectura de packs publicados
    match /packs/{packId} {
      allow read: if request.resource.data.status == "PUBLISHED" 
                  || resource.data.status == "PUBLISHED";
    }
    
    // Permitir lectura de textos y preguntas
    match /texts/{textId} {
      allow read: if true;
    }
    
    match /questions/{questionId} {
      allow read: if true;
    }
  }
}
```

**⚠️ IMPORTANTE**: Estas son reglas básicas para desarrollo. En producción, deberías tener reglas más restrictivas.

---

## ✅ Solución 5: Verificar Conexión a Internet

1. **Verifica que tu dispositivo/emulador tenga internet**
2. **Prueba abrir un navegador** en el dispositivo
3. **Verifica que Firebase esté accesible**

---

## ✅ Solución 6: Verificar google-services.json

1. **Verifica que el archivo existe**:
   ```
   android/app/google-services.json
   ```

2. **Verifica que sea del proyecto correcto**:
   - Debe tener el `project_id` correcto: `eduquiz-e2829`
   - Debe estar descargado desde Firebase Console

3. **Si no existe o está incorrecto**:
   - Ve a Firebase Console
   - Configuración del proyecto → **Tus apps**
   - Descarga el `google-services.json` para Android
   - Reemplaza el archivo en `android/app/`

---

## 🔍 Verificación Paso a Paso

### Checklist de Diagnóstico

1. **En Firebase Console**:
   - [ ] Colección `packs` existe
   - [ ] Documento `pack_2025_w01` existe
   - [ ] Campo `status = "PUBLISHED"` (exacto)
   - [ ] Campo `publishedAt` es un número
   - [ ] Campo `textIds` tiene 3 elementos
   - [ ] Campo `questionIds` tiene 6 elementos

2. **En la App**:
   - [ ] `google-services.json` existe y es correcto
   - [ ] Hay conexión a internet
   - [ ] Los logs no muestran errores de Firestore

3. **Índice Compuesto**:
   - [ ] Índice creado en Firestore (o consulta modificada)

---

## 🚀 Solución Rápida (Recomendada)

Si quieres una solución rápida, sigue estos pasos en orden:

1. **Ejecuta el script de nuevo**:
   ```bash
   node scripts/init-firestore.js
   ```

2. **Verifica en Firebase Console** que el pack existe con `status: "PUBLISHED"`

3. **En la app, haz clic en "Refrescar"**

4. **Si aparece un error sobre índice**, haz clic en el enlace que te da Firestore para crearlo automáticamente

5. **Espera a que se cree el índice** (puede tardar 1-2 minutos)

6. **Vuelve a hacer clic en "Refrescar"** en la app

---

## 📝 Nota sobre el Índice Compuesto

He modificado el código para que **no requiera el índice compuesto** (ordenando en memoria en lugar de en Firestore). Esto debería funcionar sin necesidad de crear el índice manualmente.

**Si aún no funciona**, verifica:
1. Que el script se ejecutó correctamente
2. Que el pack tiene `status: "PUBLISHED"`
3. Que hay conexión a internet
4. Que las reglas de Firestore permiten lectura

---

## 🐛 Si Nada Funciona

1. **Revisa los logs de Android Studio (Logcat)**:
   - Filtra por "Firestore" o "Pack"
   - Busca errores específicos

2. **Verifica que Firebase esté inicializado**:
   - Revisa que `google-services.json` esté correcto
   - Verifica que Firebase esté configurado en la app

3. **Prueba con un pack manual en Firestore Console**:
   - Crea un pack manualmente desde Firebase Console
   - Asegúrate de que tenga todos los campos correctos

---

**¡Sigue estos pasos y deberías poder ver el pack disponible!** 🎯











