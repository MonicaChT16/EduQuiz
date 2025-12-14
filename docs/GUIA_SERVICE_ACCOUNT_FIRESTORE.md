# 🔐 Guía Paso a Paso: Service Account Key y Inicialización de Firestore

## 📋 Requisitos Previos

- Tener un proyecto Firebase creado
- Tener acceso a Firebase Console
- Node.js instalado (versión 20 LTS recomendada)
- Firebase CLI instalado (`npm install -g firebase-tools`)

---

## 🔑 PASO 1: Crear Service Account Key en Firebase Console

### 1.1. Acceder a Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto (o créalo si no existe)

### 1.2. Ir a Configuración del Proyecto

1. Haz clic en el **ícono de engranaje** ⚙️ (arriba a la izquierda)
2. Selecciona **"Configuración del proyecto"** (Project settings)

### 1.3. Ir a la Pestaña "Cuentas de servicio"

1. En la parte superior, busca la pestaña **"Cuentas de servicio"** (Service accounts)
2. Haz clic en ella

### 1.4. Generar Nueva Clave Privada

1. En la sección **"Firebase Admin SDK"**, verás un botón que dice:
   - **"Generar nueva clave privada"** (Generate new private key)
   - O en inglés: **"Generate New Private Key"**

2. **⚠️ ADVERTENCIA**: Firebase te mostrará un mensaje de advertencia que dice:
   > "Mantén tu clave privada segura. No la compartas públicamente."

3. Haz clic en **"Generar clave"** (Generate key)

4. Se descargará automáticamente un archivo JSON con un nombre como:
   ```
   tu-proyecto-firebase-adminsdk-xxxxx-xxxxxxxxxx.json
   ```

---

## 📁 PASO 2: Guardar el Archivo en el Proyecto

### 2.1. Renombrar el Archivo

1. **Renombra** el archivo descargado a:
   ```
   serviceAccountKey.json
   ```

### 2.2. Mover el Archivo a la Raíz del Proyecto

1. **Copia** el archivo `serviceAccountKey.json`
2. **Pégalo** en la **raíz del proyecto** (la misma carpeta donde está `README.md`)

**Estructura esperada**:
```
Grupo_5/
├── android/
├── functions/
├── web-admin/
├── scripts/
├── README.md
├── serviceAccountKey.json  ← AQUÍ
└── .gitignore
```

---

## 🚫 PASO 3: Agregar al .gitignore (MUY IMPORTANTE)

### 3.1. Abrir el Archivo .gitignore

1. Abre el archivo `.gitignore` que está en la **raíz del proyecto**

### 3.2. Agregar serviceAccountKey.json

1. **Agrega** esta línea al final del archivo `.gitignore`:
   ```
   serviceAccountKey.json
   ```

**⚠️ CRÍTICO**: Este archivo contiene credenciales secretas. **NUNCA** lo subas a Git/GitHub.

### 3.3. Verificar que Está en .gitignore

El archivo `.gitignore` debería verse así (al final):
```gitignore
# ... otras líneas ...

# Firebase Service Account Key (NUNCA subir a Git)
serviceAccountKey.json
```

---

## 📝 PASO 4: Crear el Script de Inicialización

### 4.1. Crear la Carpeta scripts (si no existe)

1. En la **raíz del proyecto**, verifica que existe la carpeta `scripts/`
2. Si no existe, **crédala**

### 4.2. Crear el Archivo init-firestore.js

1. Crea un nuevo archivo llamado `init-firestore.js` dentro de la carpeta `scripts/`

2. **Copia y pega** este código:

```javascript
const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// Ruta al archivo serviceAccountKey.json
const serviceAccountPath = path.join(__dirname, '..', 'serviceAccountKey.json');

// Verificar que el archivo existe
if (!fs.existsSync(serviceAccountPath)) {
    console.error('❌ ERROR: No se encontró serviceAccountKey.json');
    console.error('   Asegúrate de que el archivo esté en la raíz del proyecto');
    process.exit(1);
}

// Cargar las credenciales
const serviceAccount = require(serviceAccountPath);

// Inicializar Firebase Admin
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Función para inicializar Firestore con datos base
async function initFirestore() {
    console.log('🚀 Iniciando inicialización de Firestore...\n');

    try {
        // Aquí puedes agregar tus datos iniciales
        // Por ejemplo: crear colecciones, documentos, índices, etc.

        console.log('✅ Firestore inicializado correctamente');
        console.log('📊 Proyecto:', serviceAccount.project_id);
        
        // Ejemplo: Crear un documento de prueba
        // const testDoc = await db.collection('test').doc('init').set({
        //     initialized: true,
        //     timestamp: admin.firestore.FieldValue.serverTimestamp()
        // });
        // console.log('✅ Documento de prueba creado');

    } catch (error) {
        console.error('❌ Error al inicializar Firestore:', error);
        process.exit(1);
    }
}

// Ejecutar la inicialización
initFirestore()
    .then(() => {
        console.log('\n✨ Inicialización completada');
        process.exit(0);
    })
    .catch((error) => {
        console.error('\n❌ Error fatal:', error);
        process.exit(1);
    });
```

### 4.3. Instalar Dependencias Necesarias

1. Abre una **terminal** en la raíz del proyecto

2. Instala `firebase-admin`:
   ```bash
   npm install firebase-admin
   ```

   O si prefieres instalarlo globalmente:
   ```bash
   npm install -g firebase-admin
   ```

---

## ▶️ PASO 5: Ejecutar el Script

### 5.1. Verificar que Todo Está Listo

Antes de ejecutar, verifica:
- [ ] `serviceAccountKey.json` está en la raíz del proyecto
- [ ] `serviceAccountKey.json` está en `.gitignore`
- [ ] El archivo `scripts/init-firestore.js` existe
- [ ] `firebase-admin` está instalado

### 5.2. Ejecutar el Script

1. Abre una **terminal** en la **raíz del proyecto**

2. Ejecuta:
   ```bash
   node scripts/init-firestore.js
   ```

### 5.3. Verificar la Ejecución

Si todo está correcto, deberías ver:
```
🚀 Iniciando inicialización de Firestore...

✅ Firestore inicializado correctamente
📊 Proyecto: tu-proyecto-id

✨ Inicialización completada
```

---

## 🔍 PASO 6: Verificar en Firebase Console

### 6.1. Verificar en Firestore

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto
3. Ve a **Firestore Database** en el menú lateral
4. Verifica que se hayan creado las colecciones/documentos que definiste en el script

---

## ⚠️ IMPORTANTE: Seguridad

### ✅ HACER:
- ✅ Mantener `serviceAccountKey.json` en `.gitignore`
- ✅ No compartir el archivo públicamente
- ✅ Usar variables de entorno en producción
- ✅ Rotar las claves periódicamente

### ❌ NO HACER:
- ❌ Subir `serviceAccountKey.json` a Git/GitHub
- ❌ Compartir el archivo en chats públicos
- ❌ Incluir el archivo en commits
- ❌ Dejarlo en carpetas públicas

---

## 🐛 Solución de Problemas

### Error: "No se encontró serviceAccountKey.json"
**Solución**:
1. Verifica que el archivo esté en la raíz del proyecto
2. Verifica que el nombre sea exactamente `serviceAccountKey.json` (sin espacios)

### Error: "Cannot find module 'firebase-admin'"
**Solución**:
```bash
npm install firebase-admin
```

### Error: "Permission denied"
**Solución**:
1. Verifica que la Service Account tenga los permisos correctos en Firebase Console
2. Ve a IAM & Admin en Google Cloud Console y verifica los roles

### Error: "Invalid credentials"
**Solución**:
1. Regenera la clave privada desde Firebase Console
2. Reemplaza el archivo `serviceAccountKey.json` con el nuevo

---

## 📚 Recursos Adicionales

- [Documentación de Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)
- [Firebase Console](https://console.firebase.google.com/)
- [Guía de Service Accounts](https://cloud.google.com/iam/docs/service-accounts)

---

## ✅ Checklist Final

- [ ] Service Account Key descargado desde Firebase Console
- [ ] Archivo renombrado a `serviceAccountKey.json`
- [ ] Archivo colocado en la raíz del proyecto
- [ ] `serviceAccountKey.json` agregado a `.gitignore`
- [ ] Script `scripts/init-firestore.js` creado
- [ ] `firebase-admin` instalado (`npm install firebase-admin`)
- [ ] Script ejecutado exitosamente (`node scripts/init-firestore.js`)
- [ ] Verificado en Firebase Console

---

**¡Listo! Ya tienes configurado el Service Account Key y el script de inicialización.** 🎉











