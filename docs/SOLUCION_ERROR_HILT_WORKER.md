# 🔧 Solución: Error HiltWorker NoSuchMethodException

## ❌ Error
```
Could not instantiate com.eduquiz.data.sync.SyncAllUsersWorker
java.lang.NoSuchMethodException: com.eduquiz.data.sync.SyncAllUsersWorker.<init>
```

## 🔍 Causa
Hilt no está generando el código necesario para crear los Workers con inyección de dependencias.

## ✅ Solución Paso a Paso

### Paso 1: Verificar Dependencias

**En `android/app/build.gradle.kts`:**
```kotlin
dependencies {
    // ... otras dependencias ...
    implementation(libs.androidx.hilt.work)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    kapt(libs.androidx.hilt.compiler)  // ← ESTO ES CRÍTICO
}
```

**En `android/data/build.gradle.kts`:**
```kotlin
dependencies {
    // ... otras dependencias ...
    implementation(libs.androidx.hilt.work)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    kapt(libs.androidx.hilt.compiler)  // ← ESTO ES CRÍTICO
}
```

### Paso 2: Clean Build Completo

**Desde Android Studio:**
1. **Build** → **Clean Project**
2. **Build** → **Rebuild Project**

**Desde Terminal:**
```bash
cd android
./gradlew clean
./gradlew build
```

### Paso 3: Invalidar Cachés (Si el problema persiste)

1. **File** → **Invalidate Caches...**
2. Marca todas las opciones
3. Haz clic en **Invalidate and Restart**

### Paso 4: Verificar que el Código se Genere

Después del build, verifica que se generen estos archivos:
- `android/app/build/generated/source/kapt/debug/com/eduquiz/app/EduQuizApp_HiltComponents.java`
- `android/data/build/generated/source/kapt/debug/com/eduquiz/data/sync/SyncAllUsersWorker_AssistedFactory.java`

### Paso 5: Verificar AndroidManifest

**En `android/app/src/main/AndroidManifest.xml`:**
```xml
<application
    android:name=".EduQuizApp"
    ...>
```

Asegúrate de que `EduQuizApp` esté configurado como la clase Application.

### Paso 6: Verificar EduQuizApp

**En `android/app/src/main/java/com/eduquiz/app/EduQuizApp.kt`:**
```kotlin
@HiltAndroidApp
class EduQuizApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

## 🔄 Solución Alternativa (Si el problema persiste)

Si después de seguir todos los pasos el error persiste, puedes ejecutar la sincronización directamente sin usar Workers:

**Modificar `EduQuizApp.kt`:**
```kotlin
override fun onCreate() {
    super.onCreate()
    
    // ... código existente ...
    
    // Ejecutar sincronización directamente (sin Worker)
    viewModelScope.launch {
        try {
            val result = syncRepository.syncAllUsers()
            Log.d("EduQuizApp", "Sync completed: ${result.syncedUsers} users")
        } catch (e: Exception) {
            Log.e("EduQuizApp", "Error syncing users", e)
        }
    }
}
```

**Nota:** Esto requiere que `EduQuizApp` tenga acceso a `viewModelScope`, lo cual no es posible directamente. En su lugar, puedes crear un método en `SyncRepository` que se ejecute de forma síncrona.

## ⚠️ Verificaciones Adicionales

1. **Versión de Hilt**: Asegúrate de usar versiones compatibles:
   - `hilt-android`: 2.48 o superior
   - `androidx.hilt:hilt-work`: 1.2.0
   - `androidx.hilt:hilt-compiler`: 1.2.0

2. **Orden de Plugins**: En `build.gradle.kts`:
   ```kotlin
   plugins {
       alias(libs.plugins.hilt)  // Debe estar antes de kapt
       alias(libs.plugins.kotlin.kapt)
   }
   ```

3. **Kapt Configuration**: Asegúrate de tener:
   ```kotlin
   kapt {
       correctErrorTypes = true
   }
   ```

## 📝 Logs de Verificación

Después del build, busca estos logs al iniciar la app:
```
EduQuizApp: WorkManager configuration set with HiltWorkerFactory
SyncAllUsersWorker: Starting sync of all users to Firestore
```

Si ves estos logs, el problema está resuelto.

## 🆘 Si Nada Funciona

1. Elimina las carpetas `build/` de todos los módulos
2. Elimina `.gradle/` en la raíz del proyecto
3. Haz un **Clean Project** completo
4. **Rebuild Project**
5. Si persiste, considera usar KSP en lugar de Kapt (requiere migración)







