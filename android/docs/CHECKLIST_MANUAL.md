# ✅ CHECKLIST MANUAL - Base de Datos Room

## 📋 PASOS QUE DEBES VERIFICAR/HACER MANUALMENTE

### 🔍 FASE 1: VERIFICACIÓN DE DEPENDENCIAS

#### ✅ Paso 1.1: Verificar `android/gradle/libs.versions.toml`
**Archivo**: `android/gradle/libs.versions.toml`

**Verifica que existan estas líneas**:
```toml
[versions]
room = "2.7.0-alpha10"  ← DEBE ESTAR

[libraries]
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
```

**✅ ACCIÓN MANUAL**: 
- [ ] Abre el archivo `android/gradle/libs.versions.toml`
- [ ] Verifica que la versión de Room esté definida
- [ ] Verifica que las 3 librerías de Room estén en la sección `[libraries]`

---

#### ✅ Paso 1.2: Verificar `android/data/build.gradle.kts`
**Archivo**: `android/data/build.gradle.kts`

**Verifica que existan estas líneas**:
```kotlin
plugins {
    alias(libs.plugins.kotlin.kapt)  ← DEBE ESTAR
    alias(libs.plugins.hilt)        ← DEBE ESTAR
}

kapt {
    correctErrorTypes = true
    useBuildCache = true
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")  ← DEBE ESTAR
    }
}

dependencies {
    implementation(libs.androidx.room.runtime)   ← DEBE ESTAR
    implementation(libs.androidx.room.ktx)      ← DEBE ESTAR
    kapt(libs.androidx.room.compiler)           ← DEBE ESTAR
}
```

**✅ ACCIÓN MANUAL**:
- [ ] Abre `android/data/build.gradle.kts`
- [ ] Verifica que el plugin `kotlin-kapt` esté en la sección `plugins`
- [ ] Verifica que la sección `kapt` tenga el argumento `room.schemaLocation`
- [ ] Verifica que las 3 dependencias de Room estén en `dependencies`
- [ ] Si falta algo, agrégalo manualmente

---

### 🗄️ FASE 2: VERIFICACIÓN DE LA BASE DE DATOS

#### ✅ Paso 2.1: Verificar que `AppDatabase.kt` existe
**Archivo**: `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`

**✅ ACCIÓN MANUAL**:
- [ ] Verifica que el archivo existe en la ruta correcta
- [ ] Abre el archivo y verifica que contenga:
  - [ ] 10 entidades (PackEntity, TextEntity, QuestionEntity, OptionEntity, UserProfileEntity, InventoryEntity, AchievementEntity, DailyStreakEntity, ExamAttemptEntity, ExamAnswerEntity)
  - [ ] 6 DAOs (PackDao, ContentDao, ProfileDao, StoreDao, AchievementsDao, ExamDao)
  - [ ] La clase `AppDatabase` con `@Database(version = 2)`

**Código que DEBE estar**:
```kotlin
@Database(
    entities = [
        PackEntity::class,
        TextEntity::class,
        QuestionEntity::class,
        OptionEntity::class,
        UserProfileEntity::class,
        InventoryEntity::class,
        AchievementEntity::class,
        DailyStreakEntity::class,
        ExamAttemptEntity::class,
        ExamAnswerEntity::class,
    ],
    version = 2,  ← VERIFICA QUE SEA VERSIÓN 2
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packDao(): PackDao
    abstract fun contentDao(): ContentDao
    abstract fun profileDao(): ProfileDao
    abstract fun storeDao(): StoreDao
    abstract fun achievementsDao(): AchievementsDao
    abstract fun examDao(): ExamDao
}
```

---

#### ✅ Paso 2.2: Verificar `DatabaseModule.kt`
**Archivo**: `android/data/src/main/java/com/eduquiz/data/di/DatabaseModule.kt`

**✅ ACCIÓN MANUAL**:
- [ ] Verifica que el archivo existe
- [ ] Verifica que contenga el método `provideDatabase()` con:
  - [ ] `@Singleton`
  - [ ] `Room.databaseBuilder()`
  - [ ] `.addMigrations(*AppDatabase.MIGRATIONS)`
  - [ ] `.fallbackToDestructiveMigration(dropAllTables = true)`
- [ ] Verifica que todos los DAOs estén siendo proporcionados (6 métodos `@Provides`)

**Código que DEBE estar**:
```kotlin
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
```

---

### 🔨 FASE 3: COMPILACIÓN Y VERIFICACIÓN

#### ✅ Paso 3.1: Sincronizar Gradle
**✅ ACCIÓN MANUAL**:
- [ ] Abre Android Studio
- [ ] Ve a: **File → Sync Project with Gradle Files**
- [ ] Espera a que termine la sincronización
- [ ] Verifica que NO haya errores en la sincronización

---

#### ✅ Paso 3.2: Compilar el módulo `data`
**✅ ACCIÓN MANUAL**:
- [ ] En Android Studio, haz clic derecho en el módulo `data`
- [ ] Selecciona: **Build Module 'data'**
- [ ] O ejecuta en terminal: `./gradlew :data:build`
- [ ] Verifica que compile sin errores

**Si hay errores**:
- [ ] Revisa los mensajes de error
- [ ] Verifica que todas las dependencias estén correctas
- [ ] Verifica que los imports estén correctos

---

#### ✅ Paso 3.3: Verificar que se generen los schemas
**✅ ACCIÓN MANUAL**:
- [ ] Después de compilar, verifica que exista la carpeta:
  ```
  android/data/schemas/com.eduquiz.data.db.AppDatabase/
  ```
- [ ] Debe contener archivos JSON (al menos `1.json` y `2.json`)
- [ ] Si no existen, ejecuta: `./gradlew :data:build` de nuevo

**Ubicación esperada**:
```
android/data/schemas/
└── com.eduquiz.data.db.AppDatabase/
    ├── 1.json
    └── 2.json
```

---

### 🧪 FASE 4: PRUEBAS

#### ✅ Paso 4.1: Ejecutar tests de la base de datos
**✅ ACCIÓN MANUAL**:
- [ ] Ejecuta los tests: `./gradlew :data:test`
- [ ] O en Android Studio: haz clic derecho en `android/data/src/test` → **Run Tests**
- [ ] Verifica que todos los tests pasen

**Tests que DEBEN existir**:
- `AppDatabaseTest.kt` - Tests básicos de la base de datos
- `ExamRepositoryTest.kt` - Tests del repositorio de exámenes

---

#### ✅ Paso 4.2: Probar la aplicación
**✅ ACCIÓN MANUAL**:
- [ ] Compila y ejecuta la aplicación: `./gradlew :app:assembleDebug`
- [ ] Instala en un dispositivo/emulador
- [ ] Verifica que la aplicación inicie sin crashes
- [ ] Prueba crear un examen y verifica que se guarde en la base de datos

---

### 🔧 FASE 5: CONFIGURACIÓN ADICIONAL (OPCIONAL)

#### ✅ Paso 5.1: Cambiar fallbackToDestructiveMigration (PRODUCCIÓN)
**⚠️ IMPORTANTE**: Solo si vas a producción

**Archivo**: `android/data/src/main/java/com/eduquiz/data/di/DatabaseModule.kt`

**Código actual**:
```kotlin
.fallbackToDestructiveMigration(dropAllTables = true)  ← ELIMINA ESTO EN PRODUCCIÓN
```

**✅ ACCIÓN MANUAL (Solo para producción)**:
- [ ] Si vas a publicar la app, ELIMINA o COMENTA esta línea:
  ```kotlin
  // .fallbackToDestructiveMigration(dropAllTables = true)
  ```
- [ ] Esto evita que se borren los datos de los usuarios al actualizar

---

#### ✅ Paso 5.2: Verificar que Hilt esté configurado en la App
**Archivo**: `android/app/src/main/java/com/eduquiz/app/EduQuizApp.kt`

**✅ ACCIÓN MANUAL**:
- [ ] Verifica que la clase Application tenga `@HiltAndroidApp`:
  ```kotlin
  @HiltAndroidApp
  class EduQuizApp : Application()
  ```
- [ ] Verifica que en `AndroidManifest.xml` esté declarada:
  ```xml
  <application
      android:name=".EduQuizApp"
      ...>
  ```

---

### 📝 FASE 6: VERIFICACIÓN FINAL

#### ✅ Checklist Final

**Código**:
- [ ] `AppDatabase.kt` existe y tiene 10 entidades + 6 DAOs
- [ ] `DatabaseModule.kt` existe y está configurado
- [ ] `build.gradle.kts` tiene las dependencias de Room
- [ ] `libs.versions.toml` tiene la versión de Room

**Compilación**:
- [ ] El proyecto compila sin errores
- [ ] Los schemas se generaron correctamente
- [ ] Los tests pasan

**Funcionalidad**:
- [ ] La aplicación inicia sin crashes
- [ ] Se pueden crear exámenes
- [ ] Los datos se guardan en la base de datos

---

## 🚨 PROBLEMAS COMUNES Y SOLUCIONES

### Error: "Cannot find symbol: AppDatabase"
**Solución**:
1. Verifica que `AppDatabase.kt` esté en el paquete correcto
2. Ejecuta: `./gradlew clean build`
3. Sincroniza Gradle de nuevo

### Error: "Room cannot find the migration path"
**Solución**:
1. Verifica que `version = 2` en `@Database`
2. Verifica que `MIGRATIONS` tenga la migración 1→2
3. Si es desarrollo, puedes eliminar la app y reinstalar

### Error: "Schema export directory is not provided"
**Solución**:
1. Verifica que en `build.gradle.kts` esté:
   ```kotlin
   kapt {
       arguments {
           arg("room.schemaLocation", "$projectDir/schemas")
       }
   }
   ```

### La base de datos no se crea
**Solución**:
1. Verifica que `DatabaseModule` esté en `SingletonComponent`
2. Verifica que Hilt esté configurado en la App
3. Verifica que estés inyectando correctamente en los ViewModels

---

## 📞 COMANDOS ÚTILES

### Compilar solo el módulo data
```bash
./gradlew :data:build
```

### Limpiar y reconstruir
```bash
./gradlew clean build
```

### Ejecutar tests
```bash
./gradlew :data:test
```

### Ver schemas generados
```bash
# En Windows PowerShell
Get-ChildItem -Path android\data\schemas -Recurse

# En Linux/Mac
find android/data/schemas -name "*.json"
```

---

## ✅ RESUMEN DE ARCHIVOS A VERIFICAR

1. ✅ `android/gradle/libs.versions.toml` - Versiones y librerías
2. ✅ `android/data/build.gradle.kts` - Dependencias y KAPT
3. ✅ `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt` - Base de datos
4. ✅ `android/data/src/main/java/com/eduquiz/data/di/DatabaseModule.kt` - Inyección
5. ✅ `android/app/src/main/java/com/eduquiz/app/EduQuizApp.kt` - Hilt App
6. ✅ `android/data/schemas/` - Schemas generados (después de compilar)

---

**✅ Una vez completado este checklist, tu base de datos estará lista para usar.**












