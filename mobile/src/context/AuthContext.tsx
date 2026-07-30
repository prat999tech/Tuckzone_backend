import React, { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react';
import { authApi } from '../api/auth';
import { profileApi } from '../api/profile';
import { registerSessionExpiredHandler } from '../api/client';
import { tokenStorage } from '../utils/storage';
import type { UserSummary } from '../api/types';

interface AuthContextValue {
  user: UserSummary | null;
  loading: boolean;
  isAdmin: boolean;
  login: (email: string, password: string) => Promise<UserSummary>;
  loginWithOtp: (mobile: string, code: string) => Promise<UserSummary>;
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
      login,
      loginWithOtp,
      logout,
      refreshProfile,
    }),
    [user, loading, login, loginWithOtp, logout, refreshProfile],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
}
