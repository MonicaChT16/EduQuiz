# 🚨 Solución: Proceso Termina y Reinicia (Crash)

## ❌ Problema Detectado

En Logcat aparecen estos mensajes:
```
PROCESS ENDED (25165) for package com.eduquiz.app
PROCESS STARTED (27233) for package com.eduquiz.app
```

**Esto significa**: La app se está **crasheando** y Android la reinicia automáticamente.

---

## 🔍 Causas Comunes

### 1. **Excepción No Manejada en el Hilo Principal**

**Síntoma**: La app crashea inmediatamente al iniciar o al navegar a una pantalla

**Causa**: Una excepción no capturada en el hilo principal (UI thread)

**Solución**: Busca en Logcat:
- `FATAL EXCEPTION: main`
- `android.runtime.JavaBinder`
- Stack traces completos

---

### 2. **Error en la Inicialización de Hilt**

**Síntoma**: La app crashea al iniciar, antes de mostrar cualquier pantalla

**Causa**: Problema con la inyección de dependencias (Hilt)

**Solución**: Busca en Logcat:
- `HiltAndroidApp`
- `Dagger`
- `Cannot provide`
- `Missing binding`

---

### 3. **Error al Acceder a la Base de Datos**

**Síntoma**: La app crashea al intentar acceder a Room

**Causa**: Problema con la base de datos o migraciones

**Solución**: Busca en Logcat:
- `Room`
- `SQLiteException`
- `IllegalStateException`
- `Migration`

---

### 4. **Error en Firebase/Firestore**

**Síntoma**: La app crashea al intentar usar Firebase

**Causa**: Problema con la configuración de Firebase

**Solución**: Busca en Logcat:
- `FirebaseApp`
- `Firestore`
- `google-services.json`
- `Missing google-services.json`

---

### 5. **Error de Memoria (OutOfMemoryError)**

**Síntoma**: La app crashea después de usar mucha memoria

**Causa**: Uso excesivo de memoria

**Solución**: Busca en Logcat:
- `OutOfMemoryError`
- `java.lang.OutOfMemoryError`

---

## 🔧 Pasos para Diagnosticar

### Paso 1: Ver el Stack Trace Completo

1. **Abre Logcat** en Android Studio
2. **Quita todos los filtros** (o filtra por `FATAL`)
3. **Busca líneas rojas** que indiquen errores
4. **Busca el stack trace completo** que muestre dónde ocurrió el crash

**Busca específicamente**:
- `FATAL EXCEPTION`
- `Process: com.eduquiz.app`
- `java.lang.RuntimeException`
- `android.util.AndroidRuntimeException`

---

### Paso 2: Verificar Errores Comunes

#### Error 1: Hilt no puede inyectar dependencias

**Busca en Logcat**:
```
java.lang.IllegalStateException: Cannot provide X without an @Inject constructor
```

**Solución**: Verifica que todas las dependencias estén correctamente anotadas con `@Inject`

---

#### Error 2: Base de datos corrupta o migración fallida

**Busca en Logcat**:
```
android.database.sqlite.SQLiteException
IllegalStateException: Room cannot verify the data integrity
```

**Solución**: 
1. Desinstala la app completamente
2. Reinstala desde cero
3. Esto recreará la base de datos limpia

---

#### Error 3: Firebase no inicializado

**Busca en Logcat**:
```
java.lang.IllegalStateException: Default FirebaseApp is not initialized
```

**Solución**: Verifica que `google-services.json` esté en `app/src/main/`

---

#### Error 4: Acceso a base de datos en hilo principal

**Busca en Logcat**:
```
android.database.sqlite.SQLiteException: cannot perform this operation because the connection pool has been closed
```

**Solución**: Asegúrate de que todas las operaciones de base de datos sean `suspend` o se ejecuten en un coroutine

---

### Paso 3: Verificar Logs Antes del Crash

**Antes de que aparezca `PROCESS ENDED`**, deberías ver:
- Logs de inicialización
- Logs de `EduQuizApp.onCreate()`
- Logs de `ExamViewModel.initialize()`
- Algún error o excepción

**Busca estos logs**:
```
EduQuizApp: Firebase initialized
EduQuizApp: Workers scheduled
ExamViewModel: initialize called
```

Si **NO ves estos logs**, el crash ocurre **antes** de la inicialización.

---

## 🛠️ Soluciones Rápidas

### Solución 1: Limpiar y Reconstruir

1. **Build → Clean Project**
2. **Build → Rebuild Project**
3. **Ejecuta la app de nuevo**

---

### Solución 2: Desinstalar y Reinstalar

1. **Desinstala la app** completamente del dispositivo
2. **Limpia el proyecto**: `Build → Clean Project`
3. **Reinstala la app** desde Android Studio

Esto eliminará:
- La base de datos corrupta (si existe)
- Cache de la app
- Datos corruptos

---

### Solución 3: Verificar google-services.json

1. Verifica que `google-services.json` esté en:
   - `android/app/src/main/google-services.json`
2. Verifica que el archivo no esté corrupto
3. Verifica que tenga el formato JSON correcto

---

### Solución 4: Verificar Dependencias

1. **Sync Project with Gradle Files**
2. Verifica que no haya errores de compilación
3. Verifica que todas las dependencias estén actualizadas

---

## 📊 Qué Buscar en Logcat

### Filtros Útiles

1. **Filtrar por nivel de error**:
   ```
   level:error
   ```

2. **Filtrar por proceso**:
   ```
   package:com.eduquiz.app
   ```

3. **Filtrar por excepciones**:
   ```
   FATAL OR Exception OR Error
   ```

4. **Ver todo** (sin filtros):
   - Quita todos los filtros temporalmente
   - Busca líneas rojas

---

### Logs Esperados (Sin Crash)

Si la app funciona correctamente, deberías ver:

```
EduQuizApp: Firebase initialized: [DEFAULT]
EduQuizApp: Firestore instance created successfully
EduQuizApp: Workers scheduled: periodic sync, pack update, and sync all users
ExamFeature: LaunchedEffect triggered with uid: user-123
ExamViewModel: initialize called with uid: user-123
ExamViewModel: Starting loadInitialState
```

---

## 🎯 Próximos Pasos

1. **Abre Logcat** sin filtros (o filtra por `FATAL` o `error`)
2. **Ejecuta la app**
3. **Busca el stack trace completo** del crash
4. **Comparte el stack trace completo** para identificar el problema exacto

El stack trace te dirá:
- **Dónde** ocurrió el crash (archivo y línea)
- **Qué** causó el crash (excepción)
- **Por qué** ocurrió (mensaje de error)

---

## ⚠️ Importante

**NO ignores los crashes**. Si la app se reinicia constantemente:
- El ViewModel se recrea cada vez
- Se pierde todo el estado
- No puede completar la inicialización
- Por eso no puedes iniciar el examen

**El crash debe resolverse primero** antes de poder usar la app correctamente.

---

## 📝 Checklist

- [ ] Ver stack trace completo en Logcat
- [ ] Identificar el tipo de excepción
- [ ] Verificar si es problema de Hilt
- [ ] Verificar si es problema de base de datos
- [ ] Verificar si es problema de Firebase
- [ ] Limpiar y reconstruir el proyecto
- [ ] Desinstalar y reinstalar la app
- [ ] Compartir el stack trace completo






