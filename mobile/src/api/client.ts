import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { tokenStorage } from '../utils/storage';
import type { ApiErrorBody, AuthResponse } from './types';

// See .env.example — Android emulator vs. physical device vs. production all need a
// different host here, which is why it is an env var rather than a hardcoded string.
const BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://10.0.2.2:8080/api';

/**
 * Set once by AuthContext on mount. Lets the interceptor drop the user back to the login
 * screen when a session can no longer be refreshed, without this file needing to import
 * navigation or context (which would create a circular dependency).
 */
let onSessionExpired: (() => void) | null = null;
export function registerSessionExpiredHandler(handler: () => void) {
  onSessionExpired = handler;
}

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
});

apiClient.interceptors.request.use(async (config) => {
  const token = await tokenStorage.getAccessToken();
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

/** Endpoints that legitimately return 401/400 as part of their own flow — a failed
 *  refresh here must not trigger ANOTHER refresh attempt or bounce the user around. */
const AUTH_ENDPOINTS = ['/auth/login', '/auth/otp/login', '/auth/refresh', '/auth/register'];

function isAuthEndpoint(url?: string): boolean {
  return !!url && AUTH_ENDPOINTS.some((path) => url.includes(path));
}

/**
 * Single-flight refresh: if five requests 401 at once (e.g. a screen fires several
 * parallel GETs on focus), they must all wait on the SAME refresh call, not each start
 * their own. Racing refreshes would have the loser overwrite the winner's new token with
 * a stale one, or worse, both succeed and leave the client holding a token that was
 * already superseded.
 */
let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = await tokenStorage.getRefreshToken();
      if (!refreshToken) {
        throw new Error('No refresh token available');
      }
      const response = await axios.post<AuthResponse>(
        `${BASE_URL}/auth/refresh`,
        { refreshToken },
        { headers: { 'Content-Type': 'application/json' } },
      );
      await tokenStorage.setTokens(response.data.accessToken, response.data.refreshToken);
      return response.data.accessToken;
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorBody>) => {
    const originalRequest = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !isAuthEndpoint(originalRequest.url)
    ) {
      originalRequest._retry = true;
      try {
        const newAccessToken = await refreshAccessToken();
        originalRequest.headers.set('Authorization', `Bearer ${newAccessToken}`);
        return apiClient(originalRequest);
      } catch {
        await tokenStorage.clear();
        onSessionExpired?.();
        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  },
);

/** Pulls the backend's message out of a failed request, falling back to something generic
 *  rather than ever showing the user "[object Object]" or a raw stack trace. */
export function apiErrorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as ApiErrorBody | undefined;
    if (body?.details?.length) return body.details.join('\n');
    if (body?.message) return body.message;
  }
  return fallback;
}

/**
 * Reads the stable error marker the backend sets for conditions the app must react to
 * rather than merely display — currently only EMAIL_NOT_VERIFIED. Kept separate from
 * `apiErrorMessage` because that one renders `details` to the user, so a code placed
 * there would leak into the UI as literal text.
 */
export function apiErrorCode(error: unknown): string | null {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as ApiErrorBody | undefined;
    return body?.code ?? null;
  }
  return null;
}

/**
 * Server-side validation failures, keyed by field name.
 *
 * The backend validates far more than any form can sensibly duplicate — length caps,
 * uniqueness, password rules — so a rejection can name a field the client never checked.
 * Without this, those came back as one combined toast and the user had to guess which box
 * was wrong. Merge the result into the form's error state to mark the actual input.
 */
export function apiFieldErrors(error: unknown): Record<string, string> {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as ApiErrorBody | undefined;
    if (body?.fieldErrors) return body.fieldErrors;
  }
  return {};
}
