# 🔍 Cómo Verificar la Base de Datos Room

## ❌ No se puede ejecutar SQL en PowerShell

**Problema**: Intentaste ejecutar SQL directamente en PowerShell:
```powershell
SELECT * FROM PACK_ENTITY WHERE STATUS = 'ACTIVE';
```

**Por qué no funciona**:
- PowerShell interpreta `SELECT` como el cmdlet `Select-Object`
- La base de datos Room está en el **dispositivo Android**, no en tu computadora
- Necesitas usar **Database Inspector** de Android Studio

---

## ✅ Método Correcto: Database Inspector

### Paso 1: Conectar el Dispositivo

1. Conecta tu dispositivo Android por USB
2. Habilita **Depuración USB** en el dispositivo
3. En Android Studio, verifica que el dispositivo aparezca en la lista de dispositivos

### Paso 2: Abrir Database Inspector

1. En Android Studio, ve a: **View → Tool Windows → Database Inspector**
   - O presiona `Alt + 6` (Windows/Linux) o `Cmd + 6` (Mac)

2. Deberías ver una lista de bases de datos disponibles
3. Busca `eduquiz.db` (o el nombre de tu base de datos)

### Paso 3: Ejecutar Consultas SQL

1. Haz clic en la base de datos `eduquiz.db`
2. Se abrirá una pestaña con pestañas para cada tabla
3. Haz clic en la pestaña **"Query"** o **"SQL"**
4. Escribe tu consulta SQL:

```sql
SELECT * FROM pack_entity WHERE status = 'ACTIVE';
```

5. Presiona **Enter** o haz clic en el botón de ejecutar

### Paso 4: Ver Resultados

Los resultados aparecerán en una tabla debajo de la consulta, mostrando:
- `packId`: El ID del pack
- `weekLabel`: El nombre del pack (ej: "Semana 1")
- `status`: El estado (debe ser `'ACTIVE'`)
- `publishedAt`: Fecha de publicación
- `downloadedAt`: Fecha de descarga

---

## 📊 Consultas Útiles para Verificar

### 1. Verificar Pack Activo

```sql
SELECT * FROM pack_entity WHERE status = 'ACTIVE';
```

**Resultado esperado**: 1 fila con un pack activo

### 2. Ver Todos los Packs

```sql
SELECT * FROM pack_entity ORDER BY downloadedAt DESC;
```

**Resultado esperado**: Lista de todos los packs descargados

### 3. Verificar Textos del Pack

```sql
SELECT * FROM text_entity WHERE packId = 'TU_PACK_ID';
```

**Reemplaza `TU_PACK_ID`** con el ID del pack activo

### 4. Verificar Preguntas por Materia

```sql
SELECT q.*, t.subject 
FROM question_entity q
INNER JOIN text_entity t ON q.textId = t.textId
WHERE q.packId = 'TU_PACK_ID' AND t.subject = 'MATEMATICA';
```

**Reemplaza `TU_PACK_ID`** con el ID del pack activo

### 5. Verificar Opciones

```sql
SELECT * FROM option_entity 
WHERE questionId IN (
    SELECT questionId FROM question_entity WHERE packId = 'TU_PACK_ID'
);
```

---

## 🔍 Verificar Logs en Logcat

### Problema: No aparecen logs

Si el Logcat muestra "All logs entries are hidden by the filter", puede ser porque:

1. **La app no se está ejecutando**
   - Asegúrate de que la app esté corriendo en el dispositivo
   - Ejecuta la app desde Android Studio

2. **El filtro es muy específico**
   - Prueba con filtros más amplios:
     - `ExamViewModel` (sin comillas)
     - `PackUpdateWorker` (sin comillas)
     - `EduQuizApp` (sin comillas)
   - O quita el filtro temporalmente para ver todos los logs

3. **Los logs no se están generando**
   - Verifica que el código se haya compilado correctamente
   - Asegúrate de que los logs estén en el código

### Cómo Verificar Logs Correctamente

1. **Abre Logcat** en Android Studio
2. **Selecciona tu dispositivo** en el dropdown superior
3. **Selecciona tu app** en el dropdown de procesos (debe mostrar el nombre del paquete)
4. **Aplica filtros**:
   - `ExamViewModel` - Para ver logs del examen
   - `PackUpdateWorker` - Para ver logs del worker
   - `EduQuizApp` - Para ver logs de inicialización
   - `PackRepositoryImpl` - Para ver logs del repositorio

5. **Ejecuta la app** o realiza la acción que quieres verificar

### Logs Esperados

#### Al Iniciar la App:
```
EduQuizApp: Firebase initialized: [DEFAULT]
EduQuizApp: Firestore instance created successfully
EduQuizApp: Workers scheduled: periodic sync, pack update, and sync all users
```

#### PackUpdateWorker:
```
PackUpdateWorker: Starting pack update check
PackUpdateWorker: Current active pack: none
PackUpdateWorker: No active pack found, downloading available pack: pack-123
PackUpdateWorker: Successfully downloaded new pack: pack-123
PackUpdateWorker: New pack activated: pack-123
```

#### ExamViewModel:
```
ExamViewModel: initialize called with uid: user-123
ExamViewModel: Setting userId to: user-123
ExamViewModel: Starting loadInitialState
ExamViewModel: loadInitialState: Getting active pack from database
ExamViewModel: loadInitialState: Active pack = pack-123
```

---

## 🐛 Solución de Problemas

### Problema 1: Database Inspector no muestra la base de datos

**Solución**:
1. Asegúrate de que la app esté corriendo en el dispositivo
2. La base de datos se crea cuando la app se ejecuta por primera vez
3. Si no aparece, ejecuta la app y luego refresca Database Inspector

### Problema 2: No puedo ejecutar consultas SQL

**Solución**:
1. Asegúrate de estar en la pestaña "Query" o "SQL"
2. Verifica que la base de datos esté seleccionada
3. La sintaxis SQL debe ser correcta (sin punto y coma al final en algunos casos)

### Problema 3: No hay datos en las tablas

**Solución**:
1. Verifica que la app haya descargado un pack
2. Revisa los logs para ver si hubo errores al descargar
3. Verifica que el pack se haya guardado correctamente

### Problema 4: Los logs no aparecen

**Solución**:
1. Verifica que el dispositivo esté conectado
2. Verifica que la app esté corriendo
3. Quita los filtros temporalmente
4. Verifica que el nivel de log sea correcto (Info, Debug, etc.)

---

## 📝 Resumen

✅ **Usa Database Inspector** para consultar la base de datos Room
❌ **No uses PowerShell** para ejecutar SQL (no funciona)
✅ **Usa Logcat** para ver los logs de la app
✅ **Verifica que la app esté corriendo** antes de consultar la base de datos

---

## 🎯 Pasos Rápidos

1. **Conecta el dispositivo** por USB
2. **Ejecuta la app** desde Android Studio
3. **Abre Database Inspector**: View → Tool Windows → Database Inspector
4. **Selecciona `eduquiz.db`**
5. **Ejecuta la consulta SQL**:
   ```sql
   SELECT * FROM pack_entity WHERE status = 'ACTIVE';
   ```
6. **Revisa los resultados**






