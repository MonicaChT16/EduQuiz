# 🔧 Solución Definitiva: Conexión a Firestore

## ⚠️ PROBLEMA IDENTIFICADO

La app no puede conectarse a Firestore para obtener los packs. Esto puede deberse a varios factores.

## ✅ SOLUCIÓN PASO A PASO

### Paso 1: Verificar que los Datos Estén en Firestore

**Ejecuta el script de verificación:**
```bash
node scripts/verify-firestore.js
```

**Si muestra errores, ejecuta el script de inicialización:**
```bash
node scripts/init-firestore.js
```

### Paso 2: Verificar Reglas de Firestore (CRÍTICO)

1. Ve a https://console.firebase.google.com/
2. Proyecto: `eduquiz-e2829`
3. **Firestore Database** → **Reglas**
4. **COPIA Y PEGA estas reglas:**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Packs: lectura pública para todos
    match /packs/{packId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // Textos: lectura pública
    match /texts/{textId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // Preguntas: lectura pública
    match /questions/{questionId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // Opciones: lectura pública
    match /options/{optionId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // Usuarios
    match /users/{uid} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

5. **HAZ CLIC EN "PUBLICAR"** (esto es CRÍTICO)

### Paso 3: Verificar en Firebase Console

1. Ve a **Firestore Database** → **Datos**
2. Verifica que exista:
   - Colección `packs` con documento `pack_2025_w01`
   - Campo `status` = `"PUBLISHED"` (exactamente en mayúsculas)
   - Campo `publishedAt` = un número (no un timestamp de Firestore)

### Paso 4: Recompilar la Aplicación

```bash
cd android
./gradlew clean assembleDebug
```

O en Android Studio:
1. **Build** → **Clean Project**
2. **Build** → **Rebuild Project**

### Paso 5: Revisar Logs en Android Studio

1. Abre **Logcat**
2. Filtra por: `PackRemoteDataSource` o `EduQuizApp`
3. Haz clic en "Refrescar" en la app
4. Busca estos mensajes:

**✅ Mensajes de éxito:**
```
EduQuizApp: Firebase initialized: [DEFAULT]
EduQuizApp: Firestore instance created successfully
PackRemoteDataSource: === INICIANDO CONSULTA A FIRESTORE ===
PackRemoteDataSource: ✅ Consulta completada. Found 1 published packs
PackRemoteDataSource: Successfully fetched pack meta: pack_2025_w01
```

**❌ Mensajes de error:**
- `Missing or insufficient permissions` → Las reglas de Firestore no permiten lectura
- `No published pack found` → No hay packs o el status no es "PUBLISHED"
- `Network error` → Problema de conexión a internet
- `FirebaseApp is not initialized` → Problema de configuración

### Paso 6: Verificar google-services.json

**Ubicación:** `android/app/google-services.json`

**Debe contener:**
- `project_id`: `"eduquiz-e2829"`
- `package_name`: `"com.eduquiz.app"`

**Si no existe o está incorrecto:**
1. Ve a Firebase Console
2. Configuración del proyecto → **Tus apps**
3. Descarga el `google-services.json` para Android
4. Reemplaza el archivo en `android/app/`
5. **Recompila la app**

## 🔍 DIAGNÓSTICO ADICIONAL

### Si los logs muestran "No published pack found":

1. Verifica en Firebase Console que el pack tenga `status: "PUBLISHED"` (exactamente así)
2. Verifica que `publishedAt` sea un número, no un timestamp de Firestore
3. Ejecuta el script de nuevo: `node scripts/init-firestore.js`

### Si los logs muestran "Missing or insufficient permissions":

1. Ve a Firestore → Reglas
2. Asegúrate de que las reglas permitan `allow read: if true;` para packs, texts y questions
3. **HAZ CLIC EN "PUBLICAR"** (esto es muy importante)

### Si los logs muestran "Network error":

1. Verifica que el dispositivo/emulador tenga conexión a internet
2. Prueba abrir un navegador en el dispositivo
3. Verifica que Firebase esté accesible

## 📝 CHECKLIST FINAL

- [ ] Script de verificación ejecutado sin errores
- [ ] Reglas de Firestore actualizadas y publicadas
- [ ] Pack existe en Firestore con `status: "PUBLISHED"`
- [ ] `google-services.json` existe y es correcto
- [ ] App recompilada después de los cambios
- [ ] Logs muestran mensajes de éxito

## 🚨 SI AÚN NO FUNCIONA

Comparte los logs completos de Logcat filtrados por `PackRemoteDataSource` o `EduQuizApp` para diagnosticar el problema específico.

