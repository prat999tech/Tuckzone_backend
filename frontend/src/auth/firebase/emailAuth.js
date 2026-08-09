import {
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  sendEmailVerification,
  sendPasswordResetEmail,
} from 'firebase/auth';
import { getFirebaseAuth } from './config';

export async function signInWithFirebaseEmail(email, password) {
  const credential = await signInWithEmailAndPassword(getFirebaseAuth(), email, password);
  return credential.user.getIdToken();
}

/** Creates the Firebase identity and fires off Firebase's own verification email — our
 *  backend never sends this one, Firebase does, since it owns the credential now. */
export async function createFirebaseEmailAccount(email, password) {
  const credential = await createUserWithEmailAndPassword(getFirebaseAuth(), email, password);
  await sendEmailVerification(credential.user);
  return credential.user.getIdToken();
}

export async function sendFirebasePasswordReset(email) {
  await sendPasswordResetEmail(getFirebaseAuth(), email);
}

/** Firebase's own error codes, translated to something worth showing a user. */
export function firebaseAuthErrorMessage(error) {
  switch (error.code) {
    case 'auth/email-already-in-use':
      return 'An account with this email already exists — try Sign In instead.';
    case 'auth/invalid-credential':
    case 'auth/wrong-password':
      return 'Incorrect email or password.';
    case 'auth/user-not-found':
      return 'No account found with this email — try Create Account instead.';
    case 'auth/weak-password':
      return 'Password must be at least 6 characters.';
    case 'auth/invalid-email':
      return 'Enter a valid email address.';
    case 'auth/too-many-requests':
      return 'Too many attempts. Please wait a moment and try again.';
    default:
      return error.message?.includes('Firebase is not configured')
        ? 'Email sign-in is not set up yet.'
        : 'Something went wrong. Please try again.';
  }
}
