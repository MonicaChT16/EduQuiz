# 🔍 Diagnóstico Completo: Conexión a Firestore

## ✅ Checklist de Verificación

### 1. Verificar que los Datos Estén en Firestore

**Ejecuta el script de inicialización:**
```bash
node scripts/init-firestore.js
```

**Verifica en Firebase Console:**
1. Ve a https://console.firebase.google.com/
2. Proyecto: `eduquiz-e2829`
3. **Firestore Database** → **Datos**
4. Debe existir:
   - Colección `packs` con documento `pack_2025_w01`
   - Campo `status` = `"PUBLISHED"` (exactamente en mayúsculas)
   - Campo `publishedAt` = un número (timestamp)
   - Arrays `textIds` y `questionIds` con elementos

### 2. Verificar Reglas de Firestore

**En Firebase Console → Firestore Database → Reglas:**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Permitir lectura de packs publicados
    match /packs/{packId} {
      allow read: if true;  // Temporal para desarrollo
      allow write: if request.auth != null;
    }
    
    // Permitir lectura de textos y preguntas
    match /texts/{textId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    match /questions/{questionId} {
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

**⚠️ IMPORTANTE:** Haz clic en **"Publicar"** después de cambiar las reglas.

### 3. Verificar google-services.json

**Ubicación:** `android/app/google-services.json`

**Debe contener:**
- `project_id`: `"eduquiz-e2829"`
- `package_name`: `"com.eduquiz.app"`

**Si no existe o está incorrecto:**
1. Ve a Firebase Console
2. Configuración del proyecto → **Tus apps**
3. Descarga el `google-services.json` para Android
4. Reemplaza el archivo en `android/app/`

### 4. Verificar Permisos en AndroidManifest.xml

**Debe tener:**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 5. Verificar Logs en Android Studio

**Pasos:**
1. Abre Android Studio
2. Conecta dispositivo/emulador
3. Abre **Logcat** (View → Tool Windows → Logcat)
4. Filtra por: `PackRemoteDataSource` o `Firestore`
5. Haz clic en "Refrescar" en la app
6. Busca estos mensajes:

**✅ Mensajes de éxito:**
```
PackRemoteDataSource: Fetching current pack meta from Firestore...
PackRemoteDataSource: Found X published packs
PackRemoteDataSource: Successfully fetched pack meta: pack_2025_w01
```

**❌ Mensajes de error comunes:**
- `Missing or insufficient permissions` → Problema con reglas de Firestore
- `Network error` o `Failed to get document` → Problema de conexión
- `FirebaseApp is not initialized` → Problema de configuración
- `No published pack found` → No hay packs en Firestore o status incorrecto

### 6. Verificar Inicialización de Firebase

**El código ya no inicializa Firebase manualmente** (se hace automáticamente con google-services.json).

**Verifica en `EduQuizApp.kt`:**
```kotlin
override fun onCreate() {
    super.onCreate()
    // Firebase se inicializa automáticamente con google-services.json
    // No es necesario llamar FirebaseApp.initializeApp() manualmente
}
```

### 7. Verificar Conexión a Internet

- Asegúrate de que el dispositivo/emulador tenga conexión a internet
- Prueba abrir un navegador en el dispositivo
- Verifica que Firebase esté accesible

### 8. Recompilar la Aplicación

**Después de cualquier cambio:**
```bash
cd android
./gradlew clean assembleDebug
```

O en Android Studio: **Build → Clean Project** y luego **Build → Rebuild Project**

---

## 🚨 Errores Comunes y Soluciones

### Error: "No hay packs disponibles en este momento"

**Posibles causas:**
1. **No hay packs en Firestore**
   - Solución: Ejecuta `node scripts/init-firestore.js`

2. **El pack no tiene `status = "PUBLISHED"`**
   - Solución: Verifica en Firebase Console que el campo sea exactamente `"PUBLISHED"` (mayúsculas)

3. **Reglas de Firestore bloquean la lectura**
   - Solución: Actualiza las reglas (ver punto 2)

4. **Error de conexión**
   - Solución: Verifica internet y revisa logs para el error específico

5. **google-services.json incorrecto o faltante**
   - Solución: Descarga el archivo correcto desde Firebase Console

### Error: "Missing or insufficient permissions"

**Solución:** Las reglas de Firestore no permiten lectura. Actualiza las reglas (ver punto 2).

### Error: "Network error"

**Solución:**
- Verifica conexión a internet
- Verifica que Firebase esté accesible
- Revisa si hay firewall o proxy bloqueando

---

## 📝 Pasos de Solución Rápida

1. **Ejecuta el script:**
   ```bash
   node scripts/init-firestore.js
   ```

2. **Verifica en Firebase Console** que el pack existe con `status: "PUBLISHED"`

3. **Actualiza las reglas de Firestore** (ver punto 2)

4. **Recompila la app:**
   ```bash
   cd android
   ./gradlew clean assembleDebug
   ```

5. **Instala y prueba** la app

6. **Revisa los logs** en Logcat para ver el error específico

---

## 🔍 Verificación Final

Después de seguir todos los pasos, deberías ver en Logcat:

```
PackRemoteDataSource: Fetching current pack meta from Firestore...
PackRemoteDataSource: Found 1 published packs
PackRemoteDataSource: Successfully fetched pack meta: pack_2025_w01
```

Y en la app deberías ver el pack disponible para descargar.


