import { getApps, initializeApp } from 'firebase/app';
import {
  createUserWithEmailAndPassword,
  getAuth,
  onAuthStateChanged,
  sendEmailVerification,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signOut,
} from 'firebase/auth';
import type { Auth } from 'firebase/auth';

const config = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
};

let authInstance: Auth | undefined;

function getFirebaseAuth(): Auth {
  if (!authInstance) {
    if (Object.values(config).some((value) => !value)) {
      throw new Error('Firebase browser configuration is incomplete');
    }
    const app = getApps()[0] ?? initializeApp(config);
    authInstance = getAuth(app);
  }
  return authInstance;
}

export const auth = {
  get currentUser() {
    return getFirebaseAuth().currentUser;
  },
};

export function signIn(email: string, password: string) {
  return signInWithEmailAndPassword(getFirebaseAuth(), email, password);
}

export function registerWithEmail(email: string, password: string) {
  return createUserWithEmailAndPassword(getFirebaseAuth(), email, password);
}

export function signOutUser() {
  return signOut(getFirebaseAuth());
}

export function observeAuth(callback: Parameters<typeof onAuthStateChanged>[1]) {
  return onAuthStateChanged(getFirebaseAuth(), callback);
}

export function resendVerification(user: Parameters<typeof sendEmailVerification>[0]) {
  return sendEmailVerification(user);
}

export function resetPassword(email: string) {
  return sendPasswordResetEmail(getFirebaseAuth(), email);
}
