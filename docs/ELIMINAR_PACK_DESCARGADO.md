# 🗑️ Cómo Eliminar el Pack Descargado

## ⚠️ Problema

El pack se descargó cuando las colecciones `questions` y `texts` no existían o estaban vacías. Por eso no tiene preguntas almacenadas.

## ✅ Solución: Eliminar y Descargar de Nuevo

### Opción 1: Desinstalar y Reinstalar la App (Más Fácil)

1. **Desinstala la app** desde tu dispositivo/emulador
2. **Reinstala la app** desde Android Studio
3. **Abre la app**
4. **Ve a Simulacro PISA**
5. **Haz clic en "Refrescar"**
6. **Haz clic en "Descargar Pack"**

### Opción 2: Eliminar Datos de la App

1. **Ve a Configuración** del dispositivo/emulador
2. **Apps** o **Aplicaciones**
3. **Busca tu app** (EduQuiz o el nombre que tenga)
4. **Almacenamiento** o **Storage**
5. **Borrar datos** o **Clear data**
6. **Confirma**
7. **Abre la app de nuevo**
8. **Descarga el pack de nuevo**

### Opción 3: Usar Database Inspector (Android Studio)

1. **Abre Android Studio**
2. **View → Tool Windows → App Inspection**
3. **Database Inspector**
4. **Selecciona tu app**
5. **Busca la tabla `pack_entity`**
6. **Elimina el registro con `packId = "pack_2025_w01"`**
7. **También elimina de `text_entity`, `question_entity`, `option_entity`**

---

## 🔍 Verificación

Después de eliminar y descargar de nuevo:

1. **El pack debe aparecer** con "Preguntas: 6" (no "No disponibles")
2. **El botón "Iniciar intento"** debe estar habilitado
3. **No debe aparecer el error** "El pack no tiene preguntas almacenadas"

---

## 📝 Nota

El código actualmente **no re-descarga** un pack si ya existe en Room. Por eso necesitas eliminarlo primero.

Si el pack ya existe, el código solo lo marca como activo sin volver a descargar los datos desde Firestore.

---

**Recomendación**: Usa la **Opción 1** (desinstalar/reinstalar) porque es la más rápida y segura.











