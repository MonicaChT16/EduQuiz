# EduQuiz Monorepo

Monorepo para la plataforma de simulacros tipo PISA. Incluye Android (Compose multi-módulo), Firebase Functions y panel Web Admin.

## Estructura

```
android/     # Proyecto Android Studio multi-módulo
functions/   # Firebase Cloud Functions en TypeScript
web-admin/   # Panel administrativo con Next.js
docs/        # Especificaciones y guías
```

## Android
1. Ve a la carpeta `android`.
2. Asegúrate de tener `JAVA_HOME` apuntando a JDK 17 y `local.properties` configurado.
3. Copia tu `google-services.json` dentro de `android/app/`.
4. Ejecuta `./gradlew :app:assembleDebug` para compilar, o abre el proyecto en Android Studio.

### Stack incluido
- Jetpack Compose Material 3 y Navigation
- Hilt para DI
- Room, WorkManager y Firebase (Auth + Firestore + Google Sign-In)
- Módulos: `app`, `core`, `data`, `domain`, `feature-*`

## Firebase Functions
1. Entra en `functions` y ejecuta `npm install` (ya inicializado pero necesario tras clonar).
2. Ajusta `.firebaserc` con el ID real.
3. Ejecuta `npm run build` para compilar TypeScript a `lib/`.
4. Usa `npm run serve` para emuladores y `npm run deploy` para subir funciones.

Función disponible: `helloWorld` (HTTP) que responde con un mensaje JSON.

## Web Admin
1. Ve a `web-admin` y crea `.env.local` con las credenciales de Firebase (ver `docs/DEV_SETUP.md`).
2. Instala dependencias (`npm install`).
3. Ejecuta `npm run dev` para desarrollo o `npm run build` seguido de `npm start` para producción.

Incluye rutas `/login` y `/dashboard` con un layout base y cliente Firebase listo para integrarse.

## Documentación
- `docs/PROJECT_SPEC.md`: índice y enlace al documento maestro.
- `docs/DEV_SETUP.md`: pasos de configuración local detallados.

## Scripts rápidos
- `android`: `./gradlew :app:assembleDebug`
- `functions`: `npm run build`
- `web-admin`: `npm run build`
