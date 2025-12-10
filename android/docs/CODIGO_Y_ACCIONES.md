# 📝 CÓDIGO ACTUAL Y ACCIONES MANUALES

## 🎯 RESUMEN

**La base de datos YA ESTÁ IMPLEMENTADA en el código.** Solo necesitas **VERIFICAR** que todo esté correcto.

---

## 📂 ARCHIVO 1: `android/gradle/libs.versions.toml`

### ✅ CÓDIGO QUE DEBE ESTAR:

```toml
[versions]
room = "2.7.0-alpha10"  ← VERIFICA ESTA LÍNEA

[libraries]
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
```

### 🔧 ACCIÓN MANUAL:
- [ ] Abre el archivo `android/gradle/libs.versions.toml`
- [ ] Verifica que la línea `room = "2.7.0-alpha10"` esté en `[versions]`
- [ ] Verifica que las 3 librerías de Room estén en `[libraries]`
- [ ] Si falta algo, **AGREGA** las líneas faltantes

---

## 📂 ARCHIVO 2: `android/data/build.gradle.kts`

### ✅ CÓDIGO QUE DEBE ESTAR:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)  ← VERIFICA ESTA LÍNEA
    alias(libs.plugins.hilt)
}

kapt {
    correctErrorTypes = true
    useBuildCache = true
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")  ← VERIFICA ESTA LÍNEA
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))

    // Room - VERIFICA ESTAS 3 LÍNEAS
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    
    // ... otras dependencias
}
```

### 🔧 ACCIÓN MANUAL:
- [ ] Abre `android/data/build.gradle.kts`
- [ ] Verifica que `kotlin-kapt` esté en `plugins`
- [ ] Verifica que `kapt` tenga el argumento `room.schemaLocation`
- [ ] Verifica que las 3 dependencias de Room estén en `dependencies`
- [ ] Si falta algo, **AGREGA** las líneas faltantes

---

## 📂 ARCHIVO 3: `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`

### ✅ CÓDIGO COMPLETO (YA ESTÁ IMPLEMENTADO):

Este archivo contiene:
- ✅ 10 Entidades (PackEntity, TextEntity, QuestionEntity, OptionEntity, UserProfileEntity, InventoryEntity, AchievementEntity, DailyStreakEntity, ExamAttemptEntity, ExamAnswerEntity)
- ✅ 6 DAOs (PackDao, ContentDao, ProfileDao, StoreDao, AchievementsDao, ExamDao)
- ✅ Clase AppDatabase con versión 2

### 🔧 ACCIÓN MANUAL:
- [ ] Abre `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`
- [ ] Verifica que el archivo existe y tiene contenido
- [ ] Verifica que `@Database(version = 2)` esté presente
- [ ] Verifica que todas las 10 entidades estén en la lista `entities = [...]`
- [ ] Verifica que los 6 DAOs estén definidos como métodos abstractos

**NO necesitas modificar nada aquí, solo VERIFICAR que existe.**

---

## 📂 ARCHIVO 4: `android/data/src/main/java/com/eduquiz/data/di/DatabaseModule.kt`

### ✅ CÓDIGO QUE DEBE ESTAR:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.NAME
        )
            .addMigrations(*AppDatabase.MIGRATIONS)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun providePackDao(db: AppDatabase): PackDao = db.packDao()

    @Provides
    fun provideContentDao(db: AppDatabase): ContentDao = db.contentDao()

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideStoreDao(db: AppDatabase): StoreDao = db.storeDao()

    @Provides
    fun provideAchievementsDao(db: AppDatabase): AchievementsDao = db.achievementsDao()

    @Provides
    fun provideExamDao(db: AppDatabase): ExamDao = db.examDao()
}
```

### 🔧 ACCIÓN MANUAL:
- [ ] Abre `android/data/src/main/java/com/eduquiz/data/di/DatabaseModule.kt`
- [ ] Verifica que el archivo existe
- [ ] Verifica que `provideDatabase()` tenga `@Singleton`
- [ ] Verifica que todos los 6 DAOs estén siendo proporcionados
- [ ] **NO modifiques nada**, solo verifica

---

## 📂 ARCHIVO 5: `android/app/src/main/java/com/eduquiz/app/EduQuizApp.kt`

### ✅ CÓDIGO QUE DEBE ESTAR:

```kotlin
@HiltAndroidApp
class EduQuizApp : Application()
```

### 🔧 ACCIÓN MANUAL:
- [ ] Abre `android/app/src/main/java/com/eduquiz/app/EduQuizApp.kt`
- [ ] Verifica que tenga la anotación `@HiltAndroidApp`
- [ ] Verifica que en `AndroidManifest.xml` esté declarada:
  ```xml
  <application
      android:name=".EduQuizApp"
      ...>
  ```

---

## 🔨 ACCIONES MANUALES DE COMPILACIÓN

### ✅ Paso 1: Sincronizar Gradle
**ACCIÓN**:
1. Abre Android Studio
2. Ve a: **File → Sync Project with Gradle Files**
3. Espera a que termine
4. Verifica que NO haya errores

**O en terminal**:
```bash
./gradlew --refresh-dependencies
```

---

### ✅ Paso 2: Compilar el módulo data
**ACCIÓN**:
1. En Android Studio: clic derecho en módulo `data` → **Build Module 'data'**
2. O en terminal:
   ```bash
   ./gradlew :data:build
   ```
3. Verifica que compile sin errores

---

### ✅ Paso 3: Verificar schemas generados
**ACCIÓN**:
1. Después de compilar, verifica que exista:
   ```
   android/data/schemas/com.eduquiz.data.db.AppDatabase/
   ```
2. Debe contener archivos `1.json` y `2.json`
3. Si no existen, ejecuta `./gradlew :data:build` de nuevo

---

### ✅ Paso 4: Ejecutar tests
**ACCIÓN**:
1. En terminal:
   ```bash
   ./gradlew :data:test
   ```
2. O en Android Studio: clic derecho en `android/data/src/test` → **Run Tests**
3. Verifica que los tests pasen

---

### ✅ Paso 5: Probar la aplicación
**ACCIÓN**:
1. Compila la app: `./gradlew :app:assembleDebug`
2. Instala en dispositivo/emulador
3. Verifica que la app inicie sin crashes
4. Prueba crear un examen

---

## 📋 CHECKLIST RÁPIDO

### Verificación de Archivos:
- [ ] `libs.versions.toml` tiene versión de Room
- [ ] `data/build.gradle.kts` tiene dependencias de Room y KAPT configurado
- [ ] `AppDatabase.kt` existe y tiene 10 entidades + 6 DAOs
- [ ] `DatabaseModule.kt` existe y está configurado
- [ ] `EduQuizApp.kt` tiene `@HiltAndroidApp`

### Compilación:
- [ ] Gradle sincronizado sin errores
- [ ] Módulo `data` compila correctamente
- [ ] Schemas generados en `android/data/schemas/`
- [ ] Tests pasan

### Funcionalidad:
- [ ] App inicia sin crashes
- [ ] Se pueden crear exámenes
- [ ] Datos se guardan en BD

---

## 🚨 SI HAY ERRORES

### Error: "Cannot find symbol: AppDatabase"
**Solución**:
```bash
./gradlew clean
./gradlew :data:build
```
Luego sincroniza Gradle de nuevo.

### Error: "Room cannot find the migration path"
**Solución**:
1. Verifica que `version = 2` en `AppDatabase.kt`
2. Si es desarrollo, desinstala la app y reinstala

### Error: "Schema export directory is not provided"
**Solución**:
Verifica que en `data/build.gradle.kts` esté:
```kotlin
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}
```

---

## ✅ RESUMEN FINAL

**Lo que YA está hecho**:
- ✅ Base de datos implementada (AppDatabase.kt)
- ✅ Entidades creadas (10 entidades)
- ✅ DAOs implementados (6 DAOs)
- ✅ Módulo de inyección configurado (DatabaseModule.kt)
- ✅ Migraciones configuradas (versión 2)

**Lo que TÚ debes hacer**:
1. ✅ Verificar que las dependencias estén en `build.gradle.kts`
2. ✅ Verificar que `libs.versions.toml` tenga Room
3. ✅ Sincronizar Gradle
4. ✅ Compilar el proyecto
5. ✅ Verificar que compile sin errores
6. ✅ Probar la aplicación

---

**Una vez completado este checklist, tu base de datos estará lista para usar.** 🎉









