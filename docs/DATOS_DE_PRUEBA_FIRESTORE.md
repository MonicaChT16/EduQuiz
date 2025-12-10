# 📊 Datos de Prueba en Firestore

## ✅ Datos Creados por el Script

El script `init-firestore.js` ahora crea datos de prueba completos para EduQuiz:

### 📦 Pack de Prueba
- **ID**: `pack_2025_w01`
- **Etiqueta**: `2025-W01`
- **Estado**: `PUBLISHED`
- **Ubicación**: `packs/pack_2025_w01`

### 📚 Textos de Lectura (3 textos)

1. **La Energía Solar en las Ciudades** (LECTURA)
   - ID: `txt_2025_w01_001`
   - Materia: LECTURA
   - Ubicación: `texts/txt_2025_w01_001`

2. **Problema de Geometría: Área de un Triángulo** (MATEMATICA)
   - ID: `txt_2025_w01_002`
   - Materia: MATEMATICA
   - Ubicación: `texts/txt_2025_w01_002`

3. **El Ciclo del Agua** (CIENCIAS)
   - ID: `txt_2025_w01_003`
   - Materia: CIENCIAS
   - Ubicación: `texts/txt_2025_w01_003`

### ❓ Preguntas (6 preguntas)

#### Preguntas de LECTURA (2 preguntas)
1. **q_2025_w01_0001**: "¿Cuál es la idea principal del texto sobre la energía solar?"
   - Respuesta correcta: **B**
   - Dificultad: 2

2. **q_2025_w01_0002**: "Según el texto, ¿qué beneficio adicional obtienen las ciudades que invierten en energía solar?"
   - Respuesta correcta: **C**
   - Dificultad: 1

#### Preguntas de MATEMATICA (2 preguntas)
3. **q_2025_w01_0003**: "Si un triángulo tiene base de 12 cm y altura de 8 cm, ¿cuál es su área?"
   - Respuesta correcta: **D** (48 cm²)
   - Dificultad: 1

4. **q_2025_w01_0004**: "Si duplicamos tanto la base como la altura de un triángulo, ¿qué sucede con su área?"
   - Respuesta correcta: **B** (Se cuadruplica)
   - Dificultad: 2

#### Preguntas de CIENCIAS (2 preguntas)
5. **q_2025_w01_0005**: "¿Qué proceso ocurre cuando el vapor de agua se enfría en la atmósfera?"
   - Respuesta correcta: **A** (Condensación)
   - Dificultad: 1

6. **q_2025_w01_0006**: "¿Qué fuerza principal impulsa el ciclo del agua?"
   - Respuesta correcta: **C** (La energía del sol)
   - Dificultad: 2

### 📝 Opciones
- Cada pregunta tiene 4 opciones (A, B, C, D)
- Total: **24 opciones** (6 preguntas × 4 opciones)

---

## 🔍 Cómo Verificar los Datos

### 1. En Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto: `eduquiz-e2829`
3. Ve a **Firestore Database**

### 2. Verificar Colecciones

Deberías ver estas colecciones:

- ✅ `_system` - Documento de inicialización
- ✅ `packs` - Pack de prueba
- ✅ `texts` - 3 textos
- ✅ `questions` - 6 preguntas

### 3. Verificar Estructura de un Pack

Navega a: `packs/pack_2025_w01`

Deberías ver:
```json
{
  "packId": "pack_2025_w01",
  "weekLabel": "2025-W01",
  "status": "PUBLISHED",
  "publishedAt": 1234567890,
  "textIds": ["txt_2025_w01_001", "txt_2025_w01_002", "txt_2025_w01_003"],
  "questionIds": ["q_2025_w01_0001", "q_2025_w01_0002", ...]
}
```

### 4. Verificar una Pregunta

Navega a: `questions/q_2025_w01_0001`

Deberías ver:
```json
{
  "questionId": "q_2025_w01_0001",
  "textId": "txt_2025_w01_001",
  "packId": "pack_2025_w01",
  "prompt": "¿Cuál es la idea principal...?",
  "correctOptionId": "B",
  "difficulty": 2,
  "options": [
    { "optionId": "A", "text": "..." },
    { "optionId": "B", "text": "..." },
    { "optionId": "C", "text": "..." },
    { "optionId": "D", "text": "..." }
  ],
  "explanation": {
    "status": "APPROVED",
    "text": "..."
  }
}
```

---

## 🚀 Ejecutar el Script

Para crear estos datos de prueba, ejecuta:

```bash
node scripts/init-firestore.js
```

**Nota**: Si ejecutas el script varias veces, los datos se sobrescribirán (usando `set()` con los mismos IDs).

---

## 📝 Personalizar los Datos

Si quieres agregar más datos o modificar los existentes:

1. Abre `scripts/init-firestore.js`
2. Modifica los arrays `texts` y `questions`
3. Ejecuta el script de nuevo

### Ejemplo: Agregar más preguntas

```javascript
const questions = [
    // ... preguntas existentes ...
    {
        questionId: 'q_2025_w01_0007',
        textId: 'txt_2025_w01_001',
        packId: packId,
        prompt: 'Tu nueva pregunta aquí',
        correctOptionId: 'A',
        difficulty: 1,
        explanationText: 'Explicación aquí',
        explanationStatus: 'APPROVED',
        options: [
            { optionId: 'A', text: 'Opción A' },
            { optionId: 'B', text: 'Opción B' },
            { optionId: 'C', text: 'Opción C' },
            { optionId: 'D', text: 'Opción D' }
        ]
    }
];
```

---

## ✅ Checklist de Verificación

Después de ejecutar el script, verifica:

- [ ] Colección `packs` tiene 1 documento
- [ ] Colección `texts` tiene 3 documentos
- [ ] Colección `questions` tiene 6 documentos
- [ ] Cada pregunta tiene 4 opciones
- [ ] Cada pregunta tiene `correctOptionId` definido
- [ ] Cada pregunta tiene `explanation` con status y text
- [ ] El pack tiene `textIds` y `questionIds` actualizados

---

## 🎯 Uso en la Aplicación

Estos datos de prueba permiten:

1. **Probar la descarga de packs** desde Firestore a Room
2. **Probar la creación de exámenes** con preguntas reales
3. **Probar la sincronización** de respuestas
4. **Desarrollar sin necesidad de datos reales**

---

**¡Los datos de prueba están listos para usar!** 🎉

