import { FirebaseApp, initializeApp, getApp, getApps } from 'firebase/app';
import { getAuth } from 'firebase/auth';

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY ?? '',
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN ?? '',
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID ?? '',
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET ?? '',
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID ?? '',
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID ?? '',
};

const canInitialize = Object.values(firebaseConfig).every((value) => value.length > 0);

let appInstance: FirebaseApp | undefined;

if (canInitialize) {
  appInstance = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);
}

export const getFirebaseApp = () => {
  if (!appInstance) {
    throw new Error('Firebase environment variables are missing.');
  }

  return appInstance;
};

export const getFirebaseAuth = () => getAuth(getFirebaseApp());
