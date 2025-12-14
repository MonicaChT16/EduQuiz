# 🔍 Verificar Colecciones en Firestore

## ⚠️ Problema Detectado

En Firebase Console veo que existen:
- `content_questions` (colección antigua)
- `content_texts` (colección antigua)

Pero el código de la app busca en:
- `questions` (sin prefijo)
- `texts` (sin prefijo)

## ✅ Solución

### Opción 1: Verificar si Existen las Colecciones Correctas

1. En Firebase Console, busca en el panel izquierdo:
   - ¿Existe la colección `questions`? ✅ o ❌
   - ¿Existe la colección `texts`? ✅ o ❌

2. Si NO existen:
   - Ejecuta el script de nuevo:
     ```bash
     node scripts/init-firestore.js
     ```
   - Esto creará las colecciones `questions` y `texts` con los datos correctos

3. Si SÍ existen:
   - Verifica que tengan las preguntas y textos correctos
   - El problema puede ser otro (formato de datos, etc.)

### Opción 2: Si Solo Existen las Colecciones Antiguas

Si solo existen `content_questions` y `content_texts`, entonces necesitamos:

1. **Actualizar el código** para que busque en las colecciones correctas, O
2. **Ejecutar el script** para crear las colecciones nuevas

**Recomendación**: Ejecuta el script de nuevo para crear las colecciones correctas (`questions` y `texts`).

---

## 🔍 Verificación Paso a Paso

1. **Abre Firebase Console**
2. **Ve a Firestore Database**
3. **Revisa el panel izquierdo** (colecciones):
   - ¿Ves `questions`? ✅ o ❌
   - ¿Ves `texts`? ✅ o ❌
   - ¿Ves `content_questions`? ✅ o ❌
   - ¿Ves `content_texts`? ✅ o ❌

4. **Si NO ves `questions` y `texts`**:
   - Ejecuta: `node scripts/init-firestore.js`
   - Espera a que termine
   - Verifica de nuevo en Firebase Console

5. **Si SÍ ves `questions` y `texts`**:
   - Haz clic en `questions`
   - Verifica que existan las 6 preguntas:
     - `q_2025_w01_0001`
     - `q_2025_w01_0002`
     - `q_2025_w01_0003`
     - `q_2025_w01_0004`
   - `q_2025_w01_0005`
   - `q_2025_w01_0006`

---

## 📝 Nota

Las colecciones `content_questions` y `content_texts` son de una versión anterior del script. El script actualizado crea `questions` y `texts` (sin el prefijo `content_`).

Si ejecutas el script de nuevo, se crearán las colecciones correctas y el problema debería resolverse.











