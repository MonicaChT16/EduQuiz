<!-- Instrucciones concisas para agentes de IA que trabajen en este repo -->
# Instrucciones rápidas para Copilot / agentes

Este repositorio es una app Android modular en Kotlin (Jetpack Compose, Hilt, Room, Firebase). Estas notas ayudan a un agente a ser productivo rápidamente.

**Arquitectura (alto nivel)**
- **Multi-módulo**: módulos principales: `:app`, `:core`, `:data`, `:domain`, `:feature-*` (ver `settings.gradle.kts`).
- **Capa de datos**: `data/src/main/java/.../db` contiene `AppDatabase.kt` (Room). DAO y entidades están en el mismo paquete.
- **Inyección**: Hilt se configura con módulos en `data/src/main/java/.../di` (ej. `DatabaseModule.kt`).
- **Características (UI + ViewModel)**: cada `feature-*` expone ViewModels con `@Inject constructor(...)` (p. ej. `feature-auth`, `feature-exam`).
- **App entry**: `app/src/main/java/com/eduquiz/app/EduQuizApp.kt` tiene `@HiltAndroidApp` y configura `WorkManager` y Firebase.

**Puntos de integración críticos**
- Firebase: `app/build.gradle.kts` incluye `firebase` en el BOM; inicialización en `EduQuizApp.kt`.
- WorkManager + Hilt: `EduQuizApp` usa `EntryPointAccessors` para configurar `HiltWorkerFactory`.
- Room migrations: ver `AppDatabase.MIGRATIONS` y `DatabaseModule` (se usa `fallbackToDestructiveMigration(dropAllTables = true)` en desarrollo).
- Version catalog: dependencias gestionadas por `gradle/libs.versions.toml` (ver `gradle/`).

**Comandos comunes (PowerShell en Windows)**
```powershell
.\gradlew clean assembleDebug        # compilar app (debug)
.\gradlew :app:installDebug         # instalar en dispositivo/emulador
.\gradlew test                      # ejecutar tests JVM
.\gradlew connectedAndroidTest     # pruebas instrumentadas (requiere dispositivo)
.\gradlew lint                      # análisis (si configurado)
```

Si necesitas compilar un solo módulo, usa `.:module:build`, p. ej. `.\gradlew :feature-exam:build`.

**Patrones y convenciones del proyecto**
- Namespaces/pkgs: `com.eduquiz.feature.<name>` para features; `com.eduquiz.data`, `com.eduquiz.domain`.
- ViewModels: inyectados con Hilt (`class XViewModel @Inject constructor(...)`). Buscar en `**/Feature*/**/*ViewModel.kt`.
- DAOs/Entities: Room definido en `data/src/.../db/AppDatabase.kt`; agregar DAOs y proveerlos en `DatabaseModule.kt`.
- Migraciones: anexar entradas a `AppDatabase.MIGRATIONS` y actualizar `version` en `@Database`.
- Recursos/strings/imagenes siguen estructura Android estándar en `app/src/main/res`.

**Dónde leer código de ejemplo (referencias concretas)**
- `app/src/main/java/com/eduquiz/app/EduQuizApp.kt` — inicialización Hilt/Firebase/WorkManager.
- `data/src/main/java/com/eduquiz/data/db/AppDatabase.kt` — entidades, DAOs, migraciones.
- `data/src/main/java/com/eduquiz/data/di/DatabaseModule.kt` — cómo se provee `AppDatabase` y DAOs.
- `app/build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` — configuración de compilación y dependencias.
- `feature-*/src/main/java/com/eduquiz/feature/*/*ViewModel.kt` — patrón de inyección de ViewModels.

**Consejos para cambios comunes**
- Añadir un nuevo módulo: agregar `include(":new-module")` en `settings.gradle.kts` y crear `build.gradle.kts` que use las mismas convenciones (plugins del catalogo).
- Añadir DAO/entidad: actualizar `AppDatabase` `entities` + agregar `Migration` si cambia el esquema.
- Cambios en dependencias: actualizar `gradle/libs.versions.toml` y sincronizar.

Si encuentras información adicional (scripts CI, reglas de lint o convenciones internas) en la futura carpeta `.github/` o `docs/`, intégrala aquí.

---
Si quieres, fusiono este borrador con contenido adicional existente o traduzco al inglés; dime qué prefieres y qué secciones quieres detallar más.
