# ⚡ Verificación Rápida: Pack No Disponible

## 🔍 Verificación Inmediata

### 1. ¿Ejecutaste el script DESPUÉS de los últimos cambios?

El script fue actualizado para usar las colecciones correctas. **Ejecútalo de nuevo**:

```bash
node scripts/init-firestore.js
```

### 2. Verifica en Firebase Console

1. Ve a: https://console.firebase.google.com/
2. Proyecto: `eduquiz-e2829`
3. **Firestore Database**
4. Busca la colección **`packs`**
5. Debe existir: **`pack_2025_w01`**

**Haz clic en el documento y verifica**:
- ✅ `status` = `"PUBLISHED"` (exactamente así, en mayúsculas)
- ✅ `publishedAt` = un número (ej: `1735689600000`)
- ✅ `textIds` = array con 3 elementos
- ✅ `questionIds` = array con 6 elementos

### 3. Verifica las Colecciones

Deben existir:
- ✅ `packs` (no `content_packs`)
- ✅ `texts` (no `content_texts`)
- ✅ `questions` (no `content_questions`)

### 4. En la App

1. **Haz clic en "Refrescar"**
2. **Revisa los logs de Android Studio** (Logcat)
   - Filtra por: "Firestore", "Pack", "Error"
   - Busca mensajes de error específicos

---

## 🚨 Errores Comunes

### Error: "Missing or insufficient permissions"
**Solución**: Las reglas de Firestore no permiten lectura. Ve a Firestore → Reglas y permite lectura pública temporalmente para desarrollo.

### Error: "The query requires an index"
**Solución**: Ya modifiqué el código para que no lo requiera. Si aún aparece, el código no se compiló con los cambios. Recompila la app.

### No aparece ningún error pero no encuentra packs
**Solución**: 
1. Verifica que el pack tenga `status: "PUBLISHED"` exactamente
2. Verifica que `publishedAt` sea un número, no un timestamp de Firestore
3. Ejecuta el script de nuevo

---

## ✅ Pasos de Solución Rápida

1. **Ejecuta el script**:
   ```bash
   node scripts/init-firestore.js
   ```

2. **Verifica en Firebase Console** que el pack existe

3. **Recompila la app** (por si acaso):
   ```bash
   ./gradlew :app:assembleDebug
   ```

4. **Ejecuta la app** y haz clic en "Refrescar"

5. **Revisa los logs** si aún no funciona

---

**¿Qué error específico ves en los logs de Android Studio?** Eso me ayudará a darte una solución más precisa.











