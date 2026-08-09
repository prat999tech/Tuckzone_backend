import {
  getAuth,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  sendEmailVerification,
  sendPasswordResetEmail,
  AuthError,
} from '@react-native-firebase/auth';

export async function signInWithFirebaseEmail(email: string, password: string): Promise<string> {
  const credential = await signInWithEmailAndPassword(getAuth(), email, password);
  return credential.user.getIdToken();
}

/** Creates the Firebase identity and fires off Firebase's own verification email — our
 *  backend never sends this one, Firebase does, since it owns the credential now. */
export async function createFirebaseEmailAccount(email: string, password: string): Promise<string> {
  const credential = await createUserWithEmailAndPassword(getAuth(), email, password);
  await sendEmailVerification(credential.user);
  return credential.user.getIdToken();
}

export async function sendFirebasePasswordReset(email: string): Promise<void> {
  await sendPasswordResetEmail(getAuth(), email);
}

/** Firebase's own error codes, translated to something worth showing a user. */
export function firebaseAuthErrorMessage(error: unknown): string {
  const code = (error as AuthError)?.code;
  switch (code) {
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
      return 'Something went wrong. Please try again.';
  }
}
