# 📝 PASOS MANUALES: Service Account Key y Firestore

## ✅ RESUMEN RÁPIDO

Estos son los pasos que **TÚ debes hacer manualmente**:

---

## 🔑 PASO 1: Generar Service Account Key en Firebase Console

### 1.1. Ir a Firebase Console
1. Abre tu navegador
2. Ve a: https://console.firebase.google.com/
3. **Inicia sesión** con tu cuenta de Google
4. **Selecciona tu proyecto** (o créalo si no existe)

### 1.2. Ir a Configuración del Proyecto
1. Haz clic en el **ícono de engranaje** ⚙️ (arriba a la izquierda, al lado del nombre del proyecto)
2. Selecciona **"Configuración del proyecto"** (Project settings)

### 1.3. Ir a la Pestaña "Cuentas de servicio"
1. En la parte superior de la página, verás varias pestañas
2. Haz clic en la pestaña **"Cuentas de servicio"** (Service accounts)

### 1.4. Generar Nueva Clave Privada
1. En la sección **"Firebase Admin SDK"**, verás un botón que dice:
   - **"Generar nueva clave privada"** (en español)
   - O **"Generate New Private Key"** (en inglés)

2. **⚠️ ADVERTENCIA IMPORTANTE**: 
   - Firebase te mostrará un mensaje de advertencia
   - Dice algo como: "Mantén tu clave privada segura. No la compartas públicamente."
   - Esto es normal, haz clic en **"Generar clave"** o **"Generate key"**

3. Se descargará automáticamente un archivo JSON
   - El nombre será algo como: `tu-proyecto-firebase-adminsdk-xxxxx-xxxxxxxxxx.json`
   - Este archivo contiene tus credenciales secretas

---

## 📁 PASO 2: Guardar el Archivo en el Proyecto

### 2.1. Renombrar el Archivo
1. **Busca** el archivo descargado (normalmente en tu carpeta de Descargas)
2. **Renómbralo** a: `serviceAccountKey.json`
   - Quita todos los espacios y caracteres especiales
   - El nombre debe ser exactamente: `serviceAccountKey.json`

### 2.2. Mover a la Raíz del Proyecto
1. **Copia** el archivo `serviceAccountKey.json`
2. **Pégalo** en la **raíz del proyecto** `Grupo_5`
   - La misma carpeta donde está `README.md`
   - La misma carpeta donde está `.gitignore`

**Ubicación correcta**:
```
C:\Users\Monicaca\AndroidStudioProjects\Grupo_5\
├── android/
├── functions/
├── web-admin/
├── scripts/
├── README.md
├── .gitignore
└── serviceAccountKey.json  ← AQUÍ (en la raíz)
```

---

## 🚫 PASO 3: Agregar al .gitignore

### 3.1. Abrir .gitignore
1. Abre el archivo `.gitignore` que está en la **raíz del proyecto**
   - Puedes abrirlo con cualquier editor de texto (Notepad++, VS Code, etc.)

### 3.2. Agregar serviceAccountKey.json
1. Ve al **final del archivo**
2. **Agrega** estas líneas (si no están ya):
   ```
   
   # Firebase Service Account Key (NUNCA subir a Git - contiene credenciales secretas)
   serviceAccountKey.json
   ```

3. **Guarda** el archivo

**⚠️ MUY IMPORTANTE**: Este paso es **CRÍTICO**. Si no agregas el archivo a `.gitignore`, podrías subir accidentalmente tus credenciales secretas a GitHub, lo cual es un **riesgo de seguridad grave**.

---

## 📝 PASO 4: Instalar Dependencias

### 4.1. Abrir Terminal
1. Abre una **terminal** (PowerShell, CMD, o Git Bash)
2. **Navega** a la raíz del proyecto:
   ```bash
   cd C:\Users\Monicaca\AndroidStudioProjects\Grupo_5
   ```

### 4.2. Instalar firebase-admin
1. Ejecuta este comando:
   ```bash
   npm install firebase-admin
   ```

2. Espera a que termine la instalación
3. Deberías ver algo como: `added 1 package`

---

## ▶️ PASO 5: Ejecutar el Script

### 5.1. Verificar que Todo Está Listo
Antes de ejecutar, verifica:
- [ ] `serviceAccountKey.json` está en la raíz del proyecto
- [ ] `serviceAccountKey.json` está en `.gitignore`
- [ ] El archivo `scripts/init-firestore.js` existe (ya lo creé por ti)
- [ ] `firebase-admin` está instalado (paso anterior)

### 5.2. Ejecutar el Script
1. En la **terminal** (en la raíz del proyecto), ejecuta:
   ```bash
   node scripts/init-firestore.js
   ```

### 5.3. Verificar la Salida
Si todo está correcto, deberías ver:
```
✅ Firebase Admin inicializado correctamente
🚀 Iniciando inicialización de Firestore...

📊 Proyecto: tu-proyecto-id
📧 Cliente Email: firebase-adminsdk-xxxxx@tu-proyecto.iam.gserviceaccount.com

✅ Documento de prueba creado en _system/init

✅ Firestore inicializado correctamente

✨ Inicialización completada exitosamente
💡 Puedes verificar los datos en Firebase Console
```

---

## 🔍 PASO 6: Verificar en Firebase Console (Opcional)

### 6.1. Ir a Firestore Database
1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto
3. En el menú lateral izquierdo, haz clic en **"Firestore Database"**

### 6.2. Verificar el Documento de Prueba
1. Deberías ver una colección llamada `_system`
2. Dentro debería haber un documento `init`
3. Este documento confirma que el script funcionó correctamente

---

## ✅ CHECKLIST FINAL

Marca cada paso cuando lo completes:

- [ ] **Paso 1**: Service Account Key descargado desde Firebase Console
- [ ] **Paso 2**: Archivo renombrado a `serviceAccountKey.json` y colocado en la raíz
- [ ] **Paso 3**: `serviceAccountKey.json` agregado a `.gitignore`
- [ ] **Paso 4**: `firebase-admin` instalado (`npm install firebase-admin`)
- [ ] **Paso 5**: Script ejecutado exitosamente (`node scripts/init-firestore.js`)
- [ ] **Paso 6**: (Opcional) Verificado en Firebase Console

---

## 🐛 Si Algo Sale Mal

### Error: "No se encontró serviceAccountKey.json"
**Solución**:
1. Verifica que el archivo esté en la raíz del proyecto (no en una subcarpeta)
2. Verifica que el nombre sea exactamente `serviceAccountKey.json` (sin espacios, sin mayúsculas excepto la S y A)

### Error: "Cannot find module 'firebase-admin'"
**Solución**:
```bash
npm install firebase-admin
```

### Error: "Permission denied" o "Invalid credentials"
**Solución**:
1. Regenera la clave desde Firebase Console (Paso 1.4)
2. Reemplaza el archivo `serviceAccountKey.json` con el nuevo
3. Vuelve a ejecutar el script

---

## 📚 Documentación Completa

Para más detalles, consulta:
- `docs/GUIA_SERVICE_ACCOUNT_FIRESTORE.md` - Guía completa y detallada

---

**¡Listo! Sigue estos pasos en orden y todo funcionará correctamente.** 🎉











