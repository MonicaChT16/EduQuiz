# 📚 Actualizar Materias en Firestore

## 🎯 Objetivo

Asegurar que todos los textos en Firestore tengan los valores correctos de `subject`:
- `COMPRENSION_LECTORA` (antes podía ser "LECTURA")
- `MATEMATICA`
- `CIENCIAS`

## ✅ Solución Automática

### Opción 1: Script de Actualización (Recomendado)

Ejecuta el script que actualiza automáticamente todos los textos existentes:

```bash
node scripts/update-firestore-subjects.js
```

Este script:
- ✅ Busca todos los textos en Firestore
- ✅ Convierte valores antiguos a los nuevos:
  - `LECTURA` → `COMPRENSION_LECTORA`
  - `LECTURA_COMPRENSION` → `COMPRENSION_LECTORA`
  - `COMPRENSION` → `COMPRENSION_LECTORA`
  - `MATEMATICA` / `MATEMATICAS` → `MATEMATICA`
  - `CIENCIAS` / `CIENCIA` → `CIENCIAS`
- ✅ Muestra un resumen de los cambios

### Opción 2: Re-ejecutar Script de Inicialización

Si prefieres recrear todos los datos desde cero:

```bash
node scripts/init-firestore.js
```

Este script ya está actualizado para usar los valores correctos:
- `COMPRENSION_LECTORA`
- `MATEMATICA`
- `CIENCIAS`

## 🔍 Verificación Manual

### En Firebase Console:

1. Ve a **Firestore Database** → **Datos**
2. Abre la colección `texts`
3. Verifica que cada texto tenga `subject` con uno de estos valores:
   - `COMPRENSION_LECTORA`
   - `MATEMATICA`
   - `CIENCIAS`

### Ejemplo de texto correcto:

```json
{
  "textId": "txt_2025_w01_001",
  "packId": "pack_2025_w01",
  "title": "La Energía Solar en las Ciudades",
  "body": "...",
  "subject": "COMPRENSION_LECTORA"  ← Debe ser exactamente así
}
```

## 🔄 Compatibilidad

El código de la app ahora normaliza automáticamente los valores antiguos:
- Si encuentra `LECTURA`, lo convierte a `COMPRENSION_LECTORA`
- Si encuentra `MATEMATICAS`, lo convierte a `MATEMATICA`
- Etc.

Pero es mejor actualizar Firestore para mantener consistencia.

## 📝 Pasos Recomendados

1. **Ejecuta el script de actualización:**
   ```bash
   node scripts/update-firestore-subjects.js
   ```

2. **Verifica en Firebase Console** que los subjects estén correctos

3. **Recompila la app:**
   ```bash
   cd android
   ./gradlew clean assembleDebug
   ```

4. **Prueba la app** - deberías ver los 3 botones de materias funcionando correctamente

---

**✅ Después de ejecutar el script, todos los textos en Firestore tendrán los valores correctos de subject.**










