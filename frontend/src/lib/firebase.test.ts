import { beforeEach, describe, expect, it, vi } from 'vitest';

const getApps = vi.fn(() => []);
const initializeApp = vi.fn(() => ({ name: 'app' }));
const getAuth = vi.fn(() => ({ currentUser: null }));
const signInWithEmailAndPassword = vi.fn();
const createUserWithEmailAndPassword = vi.fn();
const signOut = vi.fn();

vi.mock('firebase/app', () => ({ getApps, initializeApp }));
vi.mock('firebase/auth', () => ({
  getAuth,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
}));

describe('firebase client', () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
    process.env.NEXT_PUBLIC_FIREBASE_API_KEY = 'key';
    process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN = 'project.firebaseapp.com';
    process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID = 'project';
    process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET = 'project.firebasestorage.app';
    process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID = '123';
    process.env.NEXT_PUBLIC_FIREBASE_APP_ID = 'app-id';
  });

  it('initializes the browser app once and exposes auth actions', async () => {
    const firebase = await import('./firebase');
    void firebase.auth.currentUser;
    expect(initializeApp).toHaveBeenCalledWith({
      apiKey: 'key',
      authDomain: 'project.firebaseapp.com',
      projectId: 'project',
      storageBucket: 'project.firebasestorage.app',
      messagingSenderId: '123',
      appId: 'app-id',
    });
    expect(getAuth).toHaveBeenCalled();
    await firebase.signIn('user@example.com', 'password');
    expect(signInWithEmailAndPassword).toHaveBeenCalledWith(expect.anything(), 'user@example.com', 'password');
    await firebase.registerWithEmail('user@example.com', 'password');
    expect(createUserWithEmailAndPassword).toHaveBeenCalledWith(expect.anything(), 'user@example.com', 'password');
    await firebase.signOutUser();
    expect(signOut).toHaveBeenCalledWith(expect.anything());
  });
});
