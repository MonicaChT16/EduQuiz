# 📊 Consultas de Leaderboard para Firestore

## 🎯 Objetivo

Ejemplos de consultas para implementar la tabla de clasificación (leaderboard) en la app.

---

## 📋 Consultas Básicas

### 1. Leaderboard Global - Top 100 por XP

```kotlin
// Android (Kotlin)
val leaderboardQuery = firestore.collection("users")
    .orderBy("totalXp", Query.Direction.DESCENDING)
    .limit(100)

leaderboardQuery.get()
    .addOnSuccessListener { snapshot ->
        val users = snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserProfile::class.java)
        }
        // Mostrar en UI
    }
```

```javascript
// JavaScript/Web
const leaderboardRef = db.collection('users')
  .orderBy('totalXp', 'desc')
  .limit(100);

const snapshot = await leaderboardRef.get();
const users = snapshot.docs.map(doc => doc.data());
```

### 2. Leaderboard Global - Top 100 por Promedio de Aciertos

```kotlin
val leaderboardQuery = firestore.collection("users")
    .orderBy("averageAccuracy", Query.Direction.DESCENDING)
    .limit(100)
```

```javascript
const leaderboardRef = db.collection('users')
  .orderBy('averageAccuracy', 'desc')
  .limit(100);
```

---

## 🏫 Leaderboard por Colegio/UGEL

### 3. Top 50 por XP - Filtrado por Colegio

```kotlin
val schoolCode = "UGEL-001" // Código ingresado por el usuario

val leaderboardQuery = firestore.collection("users")
    .whereEqualTo("schoolCode", schoolCode)
    .orderBy("totalXp", Query.Direction.DESCENDING)
    .limit(50)
```

```javascript
const schoolCode = "UGEL-001";
const leaderboardRef = db.collection('users')
  .where('schoolCode', '==', schoolCode)
  .orderBy('totalXp', 'desc')
  .limit(50);
```

**⚠️ Requiere Índice Compuesto**: `schoolCode` (Ascendente) + `totalXp` (Descendente)

### 4. Top 50 por Promedio de Aciertos - Filtrado por Colegio

```kotlin
val leaderboardQuery = firestore.collection("users")
    .whereEqualTo("schoolCode", schoolCode)
    .orderBy("averageAccuracy", Query.Direction.DESCENDING)
    .limit(50)
```

```javascript
const leaderboardRef = db.collection('users')
  .where('schoolCode', '==', schoolCode)
  .orderBy('averageAccuracy', 'desc')
  .limit(50);
```

**⚠️ Requiere Índice Compuesto**: `schoolCode` (Ascendente) + `averageAccuracy` (Descendente)

---

## 🔍 Consultas Avanzadas

### 5. Posición del Usuario Actual en el Ranking

```kotlin
val currentUserUid = "user123"
val currentUserRef = firestore.collection("users").document(currentUserUid)

currentUserRef.get().addOnSuccessListener { userDoc ->
    val user = userDoc.toObject(UserProfile::class.java)
    val userXp = user?.totalXp ?: 0
    
    // Contar cuántos usuarios tienen más XP
    firestore.collection("users")
        .whereGreaterThan("totalXp", userXp)
        .get()
        .addOnSuccessListener { snapshot ->
            val position = snapshot.size() + 1
            // Mostrar posición
        }
}
```

```javascript
const currentUserUid = "user123";
const userDoc = await db.collection('users').doc(currentUserUid).get();
const userXp = userDoc.data()?.totalXp || 0;

const betterUsers = await db.collection('users')
  .where('totalXp', '>', userXp)
  .get();

const position = betterUsers.size + 1;
```

### 6. Top 10 con Paginación

```kotlin
// Primera página
var lastXp: Long? = null
var lastDoc: DocumentSnapshot? = null

val firstPageQuery = firestore.collection("users")
    .orderBy("totalXp", Query.Direction.DESCENDING)
    .limit(10)

firstPageQuery.get().addOnSuccessListener { snapshot ->
    val users = snapshot.documents.mapNotNull { doc ->
        doc.toObject(UserProfile::class.java)
    }
    lastDoc = snapshot.documents.lastOrNull()
    // Mostrar usuarios
}

// Siguiente página
fun loadNextPage() {
    val nextPageQuery = firestore.collection("users")
        .orderBy("totalXp", Query.Direction.DESCENDING)
        .startAfter(lastDoc)
        .limit(10)
    
    nextPageQuery.get().addOnSuccessListener { snapshot ->
        val users = snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserProfile::class.java)
        }
        lastDoc = snapshot.documents.lastOrNull()
        // Agregar a lista
    }
}
```

```javascript
// Primera página
let lastDoc = null;
const firstPage = await db.collection('users')
  .orderBy('totalXp', 'desc')
  .limit(10)
  .get();

const users = firstPage.docs.map(doc => doc.data());
lastDoc = firstPage.docs[firstPage.docs.length - 1];

// Siguiente página
const nextPage = await db.collection('users')
  .orderBy('totalXp', 'desc')
  .startAfter(lastDoc)
  .limit(10)
  .get();
```

### 7. Ranking con Mínimo de Intentos

```kotlin
// Solo mostrar usuarios con al menos 5 intentos completados
val leaderboardQuery = firestore.collection("users")
    .whereGreaterThanOrEqualTo("totalAttempts", 5)
    .orderBy("totalAttempts", Query.Direction.DESCENDING)
    .orderBy("totalXp", Query.Direction.DESCENDING)
    .limit(100)
```

```javascript
const leaderboardRef = db.collection('users')
  .where('totalAttempts', '>=', 5)
  .orderBy('totalAttempts', 'desc')
  .orderBy('totalXp', 'desc')
  .limit(100);
```

**⚠️ Requiere Índice Compuesto**: `totalAttempts` (Descendente) + `totalXp` (Descendente)

---

## 📱 Implementación en Android (Kotlin)

### ViewModel para Leaderboard

```kotlin
class RankingViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {
    
    private val _leaderboardState = MutableStateFlow<List<UserProfile>>(emptyList())
    val leaderboardState: StateFlow<List<UserProfile>> = _leaderboardState
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    fun loadLeaderboard(schoolCode: String? = null, limit: Int = 100) {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val query = if (schoolCode != null) {
                    firestore.collection("users")
                        .whereEqualTo("schoolCode", schoolCode)
                        .orderBy("totalXp", Query.Direction.DESCENDING)
                        .limit(limit)
                } else {
                    firestore.collection("users")
                        .orderBy("totalXp", Query.Direction.DESCENDING)
                        .limit(limit)
                }
                
                val snapshot = query.get().await()
                val users = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)
                }
                
                _leaderboardState.value = users
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

### Composable para Mostrar Leaderboard

```kotlin
@Composable
fun LeaderboardScreen(
    viewModel: RankingViewModel = hiltViewModel(),
    schoolCode: String? = null
) {
    val leaderboard by viewModel.leaderboardState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(schoolCode) {
        viewModel.loadLeaderboard(schoolCode)
    }
    
    if (isLoading) {
        CircularProgressIndicator()
    } else {
        LazyColumn {
            itemsIndexed(leaderboard) { index, user ->
                LeaderboardItem(
                    position = index + 1,
                    user = user
                )
            }
        }
    }
}

@Composable
fun LeaderboardItem(position: Int, user: UserProfile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("#$position")
        Text(user.displayName)
        Text("${user.totalXp} XP")
        Text("${user.averageAccuracy}%")
    }
}
```

---

## 🔧 Crear Índices en Firestore

### Índice 1: Leaderboard Global por XP

1. Ve a Firebase Console → Firestore Database → Índices
2. Haz clic en "Crear índice"
3. Configura:
   - **Colección**: `users`
   - **Campos**:
     - `totalXp` (Descendente)
   - **Estado de consulta**: Habilitado

### Índice 2: Leaderboard por Colegio por XP

1. **Colección**: `users`
2. **Campos**:
   - `schoolCode` (Ascendente)
   - `totalXp` (Descendente)

### Índice 3: Leaderboard por Colegio por Promedio

1. **Colección**: `users`
2. **Campos**:
   - `schoolCode` (Ascendente)
   - `averageAccuracy` (Descendente)

### Índice 4: Ranking con Mínimo de Intentos

1. **Colección**: `users`
2. **Campos**:
   - `totalAttempts` (Descendente)
   - `totalXp` (Descendente)

---

## ⚠️ Notas Importantes

1. **Límites de Firestore**:
   - Máximo 100 documentos por consulta (sin paginación)
   - Máximo 10 condiciones `where` por consulta
   - Máximo 1 `orderBy` por consulta (a menos que uses índices compuestos)

2. **Rendimiento**:
   - Las consultas con `orderBy` son rápidas si hay índices
   - Usa `limit()` para limitar resultados
   - Considera cachear resultados en el cliente

3. **Índices Compuestos**:
   - Firestore te mostrará un enlace para crear el índice automáticamente cuando ejecutes una consulta que lo requiera
   - Los índices pueden tardar unos minutos en crearse

---

## 📝 Ejemplo Completo: Leaderboard con Filtros

```kotlin
data class LeaderboardFilters(
    val schoolCode: String? = null,
    val minAttempts: Int = 0,
    val sortBy: SortBy = SortBy.XP
)

enum class SortBy {
    XP, ACCURACY
}

fun loadLeaderboard(filters: LeaderboardFilters) {
    var query: Query = firestore.collection("users")
    
    // Aplicar filtros
    if (filters.schoolCode != null) {
        query = query.whereEqualTo("schoolCode", filters.schoolCode)
    }
    
    if (filters.minAttempts > 0) {
        query = query.whereGreaterThanOrEqualTo("totalAttempts", filters.minAttempts)
    }
    
    // Ordenar
    when (filters.sortBy) {
        SortBy.XP -> query = query.orderBy("totalXp", Query.Direction.DESCENDING)
        SortBy.ACCURACY -> query = query.orderBy("averageAccuracy", Query.Direction.DESCENDING)
    }
    
    query = query.limit(100)
    
    query.get().addOnSuccessListener { snapshot ->
        // Procesar resultados
    }
}
```

---

**Última actualización**: Diciembre 2025










