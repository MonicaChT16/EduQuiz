# 📤 Guía Completa: Cómo Subir y Mejorar la Base de Datos en Firestore

## 📋 Resumen del Sistema Actual

### 🗄️ Base de Datos Local (Room)
- **Versión**: 6
- **Nombre**: `eduquiz.db`
- **Ubicación**: Base de datos SQLite en el dispositivo
- **Entidades**: 11 tablas
  - `pack_entity` - Packs semanales
  - `text_entity` - Textos de lectura
  - `question_entity` - Preguntas
  - `option_entity` - Opciones de respuesta
  - `user_profile_entity` - Perfiles de usuario
  - `inventory_entity` - Inventario de cosméticos
  - `achievement_entity` - Logros desbloqueados
  - `daily_streak_entity` - Racha diaria
  - `exam_attempt_entity` - Intentos de examen
  - `exam_answer_entity` - Respuestas de exámenes
  - `onboarding_preferences_entity` - Preferencias de onboarding

### ☁️ Base de Datos Remota (Firestore)
- **Proyecto**: `eduquiz-e2829`
- **Colecciones principales**:
  - `packs` - Packs publicados
  - `texts` - Textos de lectura
  - `questions` - Preguntas con opciones
  - `users/{uid}` - Perfiles de usuario
  - `users/{uid}/examAttempts` - Intentos de examen

### 🔄 Sincronización
- **Automática**: La app sincroniza intentos y perfiles automáticamente
- **Manual**: Los packs se descargan desde Firestore cuando están disponibles

---

## 🚀 Pasos para Subir/Mejorar la Base de Datos

### **Paso 1: Preparar los Datos**

Antes de subir, decide qué datos quieres incluir:

#### Opción A: Datos de Prueba (Ya existe)
- Script: `scripts/init-firestore.js`
- Contiene: 1 pack, 3 textos, 6 preguntas
- Uso: Para desarrollo y pruebas

#### Opción B: Datos Reales/Mejorados
- Necesitas crear un nuevo script o modificar el existente
- Incluye: Múltiples packs, más textos, más preguntas

---

### **Paso 2: Verificar Configuración de Firebase**

#### 2.1. Verificar Service Account Key

El archivo `serviceAccountKey.json` debe estar en la raíz del proyecto:

```bash
# Verificar que existe
ls serviceAccountKey.json
```

**Si no existe:**
1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto: `eduquiz-e2829`
3. Ve a **Configuración del proyecto** → **Cuentas de servicio**
4. Haz clic en **Generar nueva clave privada**
5. Descarga el JSON y guárdalo como `serviceAccountKey.json` en la raíz del proyecto

#### 2.2. Instalar Dependencias

```bash
# Desde la raíz del proyecto
npm install
```

Esto instalará `firebase-admin` necesario para los scripts.

---

### **Paso 3: Elegir el Script Apropiado**

Tienes varios scripts disponibles:

#### 📝 Script 1: `init-firestore.js`
**Propósito**: Crear datos de prueba iniciales

**Qué hace**:
- Crea 1 pack (`pack_2025_w01`)
- Crea 3 textos (LECTURA, MATEMATICA, CIENCIAS)
- Crea 6 preguntas con opciones
- Actualiza el pack con referencias

**Cuándo usarlo**:
- Primera vez que subes datos
- Quieres resetear y empezar de nuevo
- Desarrollo y pruebas

**Cómo ejecutarlo**:
```bash
node scripts/init-firestore.js
```

#### 📝 Script 2: `update-firestore-subjects.js`
**Propósito**: Normalizar los valores de `subject` en textos

**Qué hace**:
- Actualiza `subject` en todos los textos
- Normaliza valores: `LECTURA` → `COMPRENSION_LECTORA`
- Verifica que los packs tengan referencias correctas

**Cuándo usarlo**:
- Después de subir datos con subjects inconsistentes
- Quieres estandarizar los nombres de materias

**Cómo ejecutarlo**:
```bash
node scripts/update-firestore-subjects.js
```

#### 📝 Script 3: `verify-firestore.js`
**Propósito**: Verificar que los datos en Firestore estén correctos

**Qué hace**:
- Verifica que existan las colecciones
- Cuenta documentos en cada colección
- Verifica estructura de datos

**Cuándo usarlo**:
- Después de subir datos
- Para diagnosticar problemas
- Antes de probar en la app

**Cómo ejecutarlo**:
```bash
node scripts/verify-firestore.js
```

---

### **Paso 4: Crear un Script Personalizado (Para Datos Mejorados)**

Si quieres subir datos más completos o diferentes, crea un nuevo script:

#### Ejemplo: `scripts/upload-custom-data.js`

```javascript
const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// Cargar service account
const serviceAccountPath = path.join(__dirname, '..', 'serviceAccountKey.json');
if (!fs.existsSync(serviceAccountPath)) {
    console.error('❌ ERROR: No se encontró serviceAccountKey.json');
    process.exit(1);
}

const serviceAccount = require(serviceAccountPath);

// Inicializar Firebase Admin
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function uploadCustomData() {
    console.log('🚀 Subiendo datos personalizados...\n');

    try {
        // ============================================
        // 1. CREAR PACKS
        // ============================================
        const packs = [
            {
                packId: 'pack_2025_w02',
                weekLabel: '2025-W02',
                status: 'PUBLISHED',
                publishedAt: Date.now()
            },
            {
                packId: 'pack_2025_w03',
                weekLabel: '2025-W03',
                status: 'PUBLISHED',
                publishedAt: Date.now()
            }
        ];

        for (const pack of packs) {
            const packRef = db.collection('packs').doc(pack.packId);
            await packRef.set({
                ...pack,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            console.log(`✅ Pack creado: ${pack.packId}`);
        }

        // ============================================
        // 2. CREAR TEXTOS
        // ============================================
        const texts = [
            {
                textId: 'txt_2025_w02_001',
                packId: 'pack_2025_w02',
                title: 'Tu Título Aquí',
                body: 'Tu contenido aquí...',
                subject: 'COMPRENSION_LECTORA' // o 'MATEMATICA', 'CIENCIAS'
            }
            // Agrega más textos aquí
        ];

        for (const text of texts) {
            const textRef = db.collection('texts').doc(text.textId);
            await textRef.set({
                ...text,
                gradeBand: 'PISA',
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                status: 'PUBLISHED'
            });
            console.log(`✅ Texto creado: ${text.textId}`);
        }

        // ============================================
        // 3. CREAR PREGUNTAS
        // ============================================
        const questions = [
            {
                questionId: 'q_2025_w02_0001',
                textId: 'txt_2025_w02_001',
                packId: 'pack_2025_w02',
                prompt: '¿Cuál es la pregunta?',
                correctOptionId: 'B',
                difficulty: 2, // 1=fácil, 2=medio, 3=difícil
                explanationText: 'Explicación de por qué es correcta',
                explanationStatus: 'APPROVED',
                options: [
                    { optionId: 'A', text: 'Opción A' },
                    { optionId: 'B', text: 'Opción B (correcta)' },
                    { optionId: 'C', text: 'Opción C' },
                    { optionId: 'D', text: 'Opción D' }
                ]
            }
            // Agrega más preguntas aquí
        ];

        for (const question of questions) {
            const questionRef = db.collection('questions').doc(question.questionId);
            await questionRef.set({
                questionId: question.questionId,
                textId: question.textId,
                packId: question.packId,
                prompt: question.prompt,
                correctOptionId: question.correctOptionId,
                difficulty: question.difficulty,
                explanationText: question.explanationText,
                explanationStatus: question.explanationStatus,
                options: question.options, // ⚠️ IMPORTANTE: Array de objetos
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                status: 'PUBLISHED'
            });
            console.log(`✅ Pregunta creada: ${question.questionId}`);
        }

        // ============================================
        // 4. ACTUALIZAR PACKS CON REFERENCIAS
        // ============================================
        for (const pack of packs) {
            const packTexts = texts.filter(t => t.packId === pack.packId);
            const packQuestions = questions.filter(q => q.packId === pack.packId);
            
            const packRef = db.collection('packs').doc(pack.packId);
            await packRef.update({
                textIds: packTexts.map(t => t.textId),
                questionIds: packQuestions.map(q => q.questionId),
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            console.log(`✅ Pack ${pack.packId} actualizado con referencias`);
        }

        console.log('\n✅ Datos subidos correctamente');

    } catch (error) {
        console.error('❌ Error:', error);
        process.exit(1);
    }
}

uploadCustomData()
    .then(() => {
        console.log('\n✨ Proceso completado');
        process.exit(0);
    })
    .catch((error) => {
        console.error('\n❌ Error fatal:', error);
        process.exit(1);
    });
```

**Cómo ejecutarlo**:
```bash
node scripts/upload-custom-data.js
```

---

### **Paso 5: Verificar los Datos Subidos**

#### 5.1. Usar el Script de Verificación

```bash
node scripts/verify-firestore.js
```

Este script mostrará:
- Cuántos packs hay
- Cuántos textos hay
- Cuántas preguntas hay
- Si las referencias están correctas

#### 5.2. Verificar en Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto: `eduquiz-e2829`
3. Ve a **Firestore Database**
4. Verifica las colecciones:
   - `packs` - Debe tener tus packs
   - `texts` - Debe tener tus textos
   - `questions` - Debe tener tus preguntas

#### 5.3. Verificar Estructura de una Pregunta

Abre una pregunta en Firestore y verifica que tenga:

```json
{
  "questionId": "q_2025_w01_0001",
  "textId": "txt_2025_w01_001",
  "packId": "pack_2025_w01",
  "prompt": "¿Cuál es la pregunta?",
  "correctOptionId": "B",
  "difficulty": 2,
  "explanationText": "Explicación...",
  "explanationStatus": "APPROVED",
  "options": [
    { "optionId": "A", "text": "Opción A" },
    { "optionId": "B", "text": "Opción B" },
    { "optionId": "C", "text": "Opción C" },
    { "optionId": "D", "text": "Opción D" }
  ],
  "status": "PUBLISHED"
}
```

**⚠️ IMPORTANTE**: El campo `options` debe ser un **array de objetos**, no una subcolección.

---

### **Paso 6: Probar en la App**

1. **Abre la app** en tu dispositivo/emulador
2. **Ve a la pantalla de Simulacro PISA**
3. **Refresca** para buscar packs disponibles
4. **Deberías ver** tus nuevos packs con estado `PUBLISHED`
5. **Descarga** el pack
6. **Verifica** que las preguntas se carguen correctamente

---

## 📋 Checklist Completo

Antes de subir datos, verifica:

- [ ] `serviceAccountKey.json` existe en la raíz del proyecto
- [ ] `npm install` ejecutado (dependencias instaladas)
- [ ] Datos preparados (packs, textos, preguntas)
- [ ] Script creado o modificado según tus necesidades
- [ ] Script ejecutado sin errores
- [ ] Datos verificados en Firebase Console
- [ ] Estructura de preguntas correcta (options como array)
- [ ] Packs tienen `status: "PUBLISHED"`
- [ ] Packs tienen `textIds` y `questionIds` definidos
- [ ] App puede descargar los packs
- [ ] Preguntas se cargan correctamente en la app

---

## 🔧 Mejoras Recomendadas

### 1. **Agregar Más Packs**
- Crea múltiples packs semanales
- Cada pack con diferentes temas
- Variedad en dificultad

### 2. **Mejorar Contenido**
- Textos más largos y realistas
- Preguntas más desafiantes
- Explicaciones detalladas

### 3. **Organizar por Materias**
- Usa `subject` correctamente: `COMPRENSION_LECTORA`, `MATEMATICA`, `CIENCIAS`
- Agrupa preguntas por materia en cada pack

### 4. **Agregar Metadatos**
- Tags para categorización
- Dificultad promedio del pack
- Tiempo estimado de completación

### 5. **Validación de Datos**
- Verificar que todas las preguntas tengan 4 opciones
- Verificar que `correctOptionId` exista en las opciones
- Verificar que `textId` exista en la colección `texts`

---

## 🚨 Solución de Problemas

### Error: "No se encontró serviceAccountKey.json"
**Solución**: Descarga el archivo desde Firebase Console y guárdalo en la raíz del proyecto.

### Error: "Permission denied"
**Solución**: Verifica que el service account tenga permisos de escritura en Firestore.

### Error: "Collection not found"
**Solución**: Las colecciones se crean automáticamente al escribir el primer documento. No necesitas crearlas manualmente.

### Problema: "Las preguntas no se descargan en la app"
**Causas posibles**:
1. El campo `options` no es un array
2. Faltan campos requeridos
3. El pack no tiene `status: "PUBLISHED"`

**Solución**:
1. Verifica la estructura en Firestore Console
2. Ejecuta el script de verificación
3. Re-ejecuta el script de inicialización

### Problema: "Pack disponible pero sin preguntas"
**Causa**: El pack no tiene `questionIds` o las preguntas no existen.

**Solución**:
1. Verifica que el pack tenga `questionIds: [...]`
2. Verifica que cada `questionId` exista en la colección `questions`
3. Re-ejecuta el script actualizando el pack con las referencias

---

## 📚 Recursos Adicionales

- **Documentación de Firestore**: https://firebase.google.com/docs/firestore
- **Firebase Admin SDK**: https://firebase.google.com/docs/admin/setup
- **Guía de estructura de datos**: Ver `docs/DISENO_FIRESTORE_USUARIOS_RESULTADOS.md`

---

## ✅ Resumen de Comandos

```bash
# 1. Instalar dependencias
npm install

# 2. Subir datos de prueba
node scripts/init-firestore.js

# 3. Normalizar subjects
node scripts/update-firestore-subjects.js

# 4. Verificar datos
node scripts/verify-firestore.js

# 5. Subir datos personalizados (si creaste un script)
node scripts/upload-custom-data.js
```

---

**¡Listo! Ahora sabes cómo subir y mejorar tu base de datos en Firestore.** 🎉
