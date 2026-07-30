import * as SecureStore from 'expo-secure-store';

/**
 * Wraps expo-secure-store (Keychain on iOS, Keystore-backed EncryptedSharedPreferences on
 * Android) for the auth tokens. Deliberately not AsyncStorage: a JWT sitting in
 * AsyncStorage is plain, unencrypted text on disk, readable by anything with filesystem
 * access on a rooted device.
 */
const ACCESS_TOKEN_KEY = 'schoolbite.accessToken';
const REFRESH_TOKEN_KEY = 'schoolbite.refreshToken';
const USER_KEY = 'schoolbite.user';

export const tokenStorage = {
  async getAccessToken() {
    return SecureStore.getItemAsync(ACCESS_TOKEN_KEY);
  },
  async getRefreshToken() {
    return SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
  },
  async setTokens(accessToken: string, refreshToken: string) {
    await Promise.all([
      SecureStore.setItemAsync(ACCESS_TOKEN_KEY, accessToken),
      SecureStore.setItemAsync(REFRESH_TOKEN_KEY, refreshToken),
    ]);
  },
  async setAccessToken(accessToken: string) {
    await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, accessToken);
  },
  async clear() {
    await Promise.all([
      SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY),
      SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY),
      SecureStore.deleteItemAsync(USER_KEY),
    ]);
  },
};
