# Implementación de Pantalla de Onboarding - EduQuiz

## Descripción General
Se ha implementado una pantalla de onboarding con 3 pasos que se muestra solo una vez al instalar la aplicación. Cada pantalla contiene un título, descripción, espacio para imagen del robot, y indicadores de progreso.

## Componentes Creados

### 1. Base de Datos (Data Layer)

#### Archivo: `android/data/src/main/java/com/eduquiz/data/db/AppDatabase.kt`
- **Nueva Entity**: `OnboardingPreferencesEntity`
  - Almacena si el usuario ha completado el onboarding
  - Usa un singleton con `id = 1` para garantizar una sola entrada
  - Campo: `hasCompletedOnboarding: Boolean`

- **Nuevo DAO**: `OnboardingDao`
  - `upsertOnboardingPreferences()`: Insertar o actualizar preferencias
  - `observeOnboardingPreferences()`: Observar cambios en Flow<>
  - `getOnboardingPreferences()`: Obtener estado sincrónico

- **Migración**: De versión 2 a 3
  - Crea la tabla `onboarding_preferences_entity` con estructura compatible

### 2. Repository Pattern

#### Archivo: `android/data/src/main/java/com/eduquiz/data/repository/OnboardingRepository.kt`
```kotlin
class OnboardingRepository @Inject constructor(
    private val database: AppDatabase
)
```

Métodos:
- `hasCompletedOnboarding: Flow<Boolean>` - Observa cambios de estado
- `markOnboardingAsCompleted()` - Marca onboarding como completado
- `getOnboardingStatus(): Boolean` - Obtiene estado sincrónico

### 3. ViewModel

#### Archivo: `android/feature-auth/src/main/java/com/eduquiz/feature/auth/presentation/OnboardingViewModel.kt`
```kotlin
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository
)
```

Propiedades:
- `currentStep: StateFlow<Int>` - Paso actual (0-2)
- `TOTAL_STEPS = 3` - Total de pasos

Métodos:
- `nextStep()` - Avanza al siguiente paso
- `previousStep()` - Retrocede al paso anterior
- `completeOnboarding()` - Marca onboarding completado

### 4. Interfaz de Usuario

#### Archivo: `android/feature-auth/src/main/java/com/eduquiz/feature/auth/ui/OnboardingScreen.kt`

**Composables:**

1. **OnboardingRoute** 
   - Wrapper que inyecta el ViewModel
   - Conecta acciones con el ViewModel
   - Navega a Login al completar

2. **OnboardingScreen**
   - Pantalla principal del onboarding
   - Acepta parámetros: currentStep, totalSteps, callbacks

**Características de la UI:**

- **Gradiente de Fondo**: Azul marino a azul claro (mismo que LoginScreen)
- **Título**: "EduQuiz" en grande, blanco
- **Espacio de Imagen**: Box de 200x200dp con emoji 🤖 como placeholder
  - Color: blanco semi-transparente
  - BorderRadius: 20dp
- **Descripción**: Texto blanco, 16sp, centrado
- **Indicadores de Progreso**:
  - Puntos animados que cambian de tamaño
  - Punto activo: blanco, 12dp
  - Puntos inactivos: blanco 30% transparente, 8dp
  - Animación suave con `animateColorAsState`

- **Barra de Progreso**: 
  - Altura: 4dp
  - Color de fondo: blanco 20% transparente
  - Barra de relleno: blanco, ancho proporcional al paso

- **Botones**:
  - **Botón Principal**: 
    - Fondo blanco, texto azul oscuro
    - Alto: 48dp, ancho: match_parent
    - Esquinas redondeadas: 24dp
    - Texto: "SIGUIENTE" o "EMPEZAR" según el paso
  
  - **Botón Anterior** (solo visible en paso 1 y 2):
    - Fondo blanco 20% transparente, texto blanco
    - Mismas dimensiones que botón principal

- **Copyright**: Pequeño, semi-transparente, en el fondo

**Contenido de los 3 Pasos:**

1. **Paso 1**:
   - "Domina las pruebas PISA con simulacros interactivos. Mejora tu comprensión lectora, matemática y científica desde tu celular"
   - Botón: "SIGUIENTE"

2. **Paso 2**:
   - "Recibe feedback inteligente al instante. Nuestra IA te explica cada respuesta para que aprendas de tus errores y mejores día a día"
   - Botón: "SIGUIENTE"

3. **Paso 3**:
   - "Gana EduCoins y destaca en tu aula. Supera retos semanales, personaliza tu perfil y demuestra que estás listo para el futuro"
   - Botón: "EMPEZAR"

### 5. Integración con Navegación

#### Archivo: `android/app/src/main/java/com/eduquiz/app/EduQuizNavHost.kt`
- Actualizado para inyectar `OnboardingRepository`
- Lógica en `EduQuizNavHost()`:
  1. Si `AuthState.Loading` → Mostrar LoadingScreen
  2. Si `AuthState.Authenticated` → Mostrar MainNavHost (app principal)
  3. Si no autenticado:
     - Si `hasCompletedOnboarding = true` → Mostrar LoginRoute
     - Si `hasCompletedOnboarding = false` → Mostrar OnboardingRoute

#### Archivo: `android/app/src/main/java/com/eduquiz/app/MainActivity.kt`
- Inyecta `OnboardingRepository` con `@Inject`
- Pasa repository a `EduQuizNavHost()`

### 6. Inyección de Dependencias

#### Archivo: `android/data/src/main/java/com/eduquiz/data/di/RepositoryModule.kt`
```kotlin
companion object {
    @Provides
    @Singleton
    fun provideOnboardingRepository(database: AppDatabase): OnboardingRepository {
        return OnboardingRepository(database)
    }
}
```
- `OnboardingViewModel` se inyecta automáticamente a través de `@HiltViewModel`
- `OnboardingRepository` se proporciona a través del módulo DI

## Flujo de Ejecución

1. **Primera Instalación/Uso**:
   - App inicia → MainActivity crea activity
   - EduQuizNavHost consulta `OnboardingRepository.hasCompletedOnboarding`
   - Valor: `false` → Muestra OnboardingRoute
   - Usuario ve 3 pantallas de onboarding con navegación

2. **Durante el Onboarding**:
   - Usuario presiona "SIGUIENTE" → ViewModel incrementa step (0 → 1 → 2)
   - UI actualiza con nueva descripción y botones
   - Indicadores y barra se animan

3. **Al Completar**:
   - Usuario presiona "EMPEZAR" en paso 3
   - `OnboardingViewModel.completeOnboarding()` llama a repository
   - Repository guarda en DB: `hasCompletedOnboarding = true`
   - Callback navega a LoginRoute
   - AuthState aún es Unauthenticated, muestra LoginRoute

4. **Siguientes Aperturas**:
   - App inicia → Consulta `hasCompletedOnboarding`
   - Valor: `true` → Salta directamente a LoginRoute
   - Onboarding nunca se vuelve a mostrar

## Características Implementadas

✅ Pantalla con 3 pasos que cambian al presionar siguiente  
✅ Indicadores de progreso (puntos animados)  
✅ Barra de progreso que se llena  
✅ Espacio reservado para imagen del robot  
✅ Botón "SIGUIENTE" en pasos 1 y 2  
✅ Botón "EMPEZAR" en paso 3  
✅ Botón "ANTERIOR" en pasos 1 y 2  
✅ Almacenamiento en BD para no repetir  
✅ Se muestra solo una vez al instalar  
✅ Navega a Login al completar  
✅ Gradiente de colores azul (coincide con Login)  
✅ Diseño responsive  
✅ Animaciones suaves  

## Próximas Mejoras (Opcional)

- Reemplazar emoji 🤖 con imagen real del robot
- Agregar transiciones/animaciones entre pasos
- Agregar skip button opcional
- Añadir sonidos o vibraciones
- Localización/i18n para otros idiomas
