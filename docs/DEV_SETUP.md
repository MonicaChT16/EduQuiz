# DEV SETUP

## Requerimientos generales
- Java 17
- Android Studio Koala o superior
- Node.js 20 LTS (para Functions y Web Admin)
- Firebase CLI (`npm install -g firebase-tools`)

## Android (`/android`)
1. Copia tu `google-services.json` en `android/app/google-services.json`.
2. Añade el archivo `firebase-config.json` si usas Config remoto.
3. Crea un archivo `local.properties` en `/android` con la ruta de tu SDK:
   ```
   sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
   ```
4. Ejecuta `./gradlew :app:assembleDebug` desde la carpeta `android` para validar.

## Firebase Functions (`/functions`)
1. Ejecuta `npm install` (ya ejecutado en este repo pero requerido tras clonaciones nuevas).
2. Inicia sesión en Firebase CLI: `firebase login`.
3. Actualiza `.firebaserc` con el ID real del proyecto.
4. Usa emuladores locales con `npm run serve`.
5. Despliega con `npm run deploy`.

### Variables y configuraciones
- Define `FIREBASE_CONFIG` en el entorno si necesitas valores personalizados para los emuladores.

## Web Admin (`/web-admin`)
1. Crea un archivo `.env.local` con los siguientes valores:
   ```bash
   NEXT_PUBLIC_FIREBASE_API_KEY=...
   NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=...
   NEXT_PUBLIC_FIREBASE_PROJECT_ID=...
   NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=...
   NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=...
   NEXT_PUBLIC_FIREBASE_APP_ID=...
   ```
2. Instala dependencias: `npm install`.
3. Ejecuta `npm run dev` para desarrollo o `npm run build` seguido de `npm start` para producción.

## Servicios externos
- Google Sign-In requiere configurar el OAuth consent screen y añadir el SHA1/SHA256 del proyecto Android.
- Para notificaciones push, registra claves VAPID y agrégalas cuando se implementen.

## Buenas prácticas
- Ejecuta `npm run lint` en Functions y Web Admin antes de hacer commit.
- Usa `./gradlew lint` y `./gradlew ktlint` (cuando se agregue) para Android.
