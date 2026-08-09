import React, { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react';
import { authApi, StudentRegisterRequest, TeacherRegisterRequest, ParentRegisterRequest } from '../api/auth';
import { profileApi } from '../api/profile';
import { registerSessionExpiredHandler } from '../api/client';
import { tokenStorage } from '../utils/storage';
import type { AuthResponse, UserSummary } from '../api/types';

interface AuthContextValue {
  user: UserSummary | null;
  loading: boolean;
  isAdmin: boolean;
  isSubAdmin: boolean;
  login: (email: string, password: string) => Promise<UserSummary>;
  loginWithOtp: (mobile: string, code: string) => Promise<UserSummary>;
  loginWithFirebase: (idToken: string) => Promise<UserSummary>;
  registerStudentWithFirebase: (
    data: { idToken: string } & Omit<StudentRegisterRequest, 'password'>,
  ) => Promise<UserSummary>;
  registerTeacherWithFirebase: (
    data: { idToken: string } & Omit<TeacherRegisterRequest, 'password'>,
  ) => Promise<UserSummary>;
  registerParentWithFirebase: (
    data: { idToken: string } & Omit<ParentRegisterRequest, 'password'>,
  ) => Promise<UserSummary>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Wired once so a refresh failure anywhere in the app (any screen, any request) drops
    // the user back to signed-out state, instead of every screen needing its own 401
    // handling.
    registerSessionExpiredHandler(() => setUser(null));

    (async () => {
      const token = await tokenStorage.getAccessToken();
      if (!token) {
        setLoading(false);
        return;
      }
      try {
        const me = await profileApi.getMe();
        setUser(me);
      } catch {
        await tokenStorage.clear();
        setUser(null);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await authApi.login(email, password);
    await tokenStorage.setTokens(response.accessToken, response.refreshToken);
    setUser(response.user);
    return response.user;
  }, []);

  const loginWithOtp = useCallback(async (mobile: string, code: string) => {
    const response = await authApi.loginWithOtp(mobile, code);
    await tokenStorage.setTokens(response.accessToken, response.refreshToken);
    setUser(response.user);
    return response.user;
  }, []);

  /** Applies the session a Firebase-backed call returned — identical shape to a normal
   *  login response, so the rest of the app (token refresh, /me, logout) never needs to
   *  know which provider verified this sign-in. */
  const applyFirebaseSession = useCallback(async (response: AuthResponse) => {
    await tokenStorage.setTokens(response.accessToken, response.refreshToken);
    setUser(response.user);
    return response.user;
  }, []);

  const loginWithFirebase = useCallback(
    async (idToken: string) => applyFirebaseSession(await authApi.firebaseExchange(idToken)),
    [applyFirebaseSession],
  );

  const registerStudentWithFirebase = useCallback(
    async (data: { idToken: string } & Omit<StudentRegisterRequest, 'password'>) =>
      applyFirebaseSession(await authApi.firebaseRegisterStudent(data)),
    [applyFirebaseSession],
  );

  const registerTeacherWithFirebase = useCallback(
    async (data: { idToken: string } & Omit<TeacherRegisterRequest, 'password'>) =>
      applyFirebaseSession(await authApi.firebaseRegisterTeacher(data)),
    [applyFirebaseSession],
  );

  const registerParentWithFirebase = useCallback(
    async (data: { idToken: string } & Omit<ParentRegisterRequest, 'password'>) =>
      applyFirebaseSession(await authApi.firebaseRegisterParent(data)),
    [applyFirebaseSession],
  );

  const logout = useCallback(async () => {
    const refreshToken = await tokenStorage.getRefreshToken();
    if (refreshToken) {
      // Best-effort: the session must end locally even if the network call fails (plane
      // mode, server hiccup) — we never want "logout" to be blocked by connectivity.
      try {
        await authApi.logout(refreshToken);
      } catch {
        // ignored on purpose
      }
    }
    await tokenStorage.clear();
    setUser(null);
  }, []);

  const refreshProfile = useCallback(async () => {
    const me = await profileApi.getMe();
    setUser(me);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      isAdmin: user?.role === 'CANTEEN_ADMIN',
      isSubAdmin: user?.role === 'SUB_ADMIN',
      login,
      loginWithOtp,
      loginWithFirebase,
      registerStudentWithFirebase,
      registerTeacherWithFirebase,
      registerParentWithFirebase,
      logout,
      refreshProfile,
    }),
    [
      user,
      loading,
      login,
      loginWithOtp,
      loginWithFirebase,
      registerStudentWithFirebase,
      registerTeacherWithFirebase,
      registerParentWithFirebase,
      logout,
      refreshProfile,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
}
