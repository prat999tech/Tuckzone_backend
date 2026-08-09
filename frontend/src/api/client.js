import axios from 'axios';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('canteen_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const refreshToken = localStorage.getItem('canteen_refresh_token');

    if (
      error.response?.status === 401 &&
      refreshToken &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/login') &&
      !originalRequest.url?.includes('/auth/refresh')
    ) {
      originalRequest._retry = true;
      try {
        const refreshResponse = await axios.post(
          `${client.defaults.baseURL}/auth/refresh`,
          { refreshToken },
          { headers: { 'Content-Type': 'application/json' } }
        );
        const data = refreshResponse.data;
        localStorage.setItem('canteen_token', data.accessToken);
        localStorage.setItem('canteen_refresh_token', data.refreshToken);
        localStorage.setItem('canteen_user', JSON.stringify(data.user));
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return client(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem('canteen_token');
        localStorage.removeItem('canteen_refresh_token');
        localStorage.removeItem('canteen_user');
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        return Promise.reject(refreshError);
      }
    }

    if (error.response?.status === 401) {
      localStorage.removeItem('canteen_token');
      localStorage.removeItem('canteen_refresh_token');
      localStorage.removeItem('canteen_user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

/** Pulls the backend's message out of a failed request, falling back to something generic
 *  rather than ever showing the user "[object Object]" or a raw stack trace. Prefers
 *  `details` (per-field validation messages) over the generic top-level `message`, mirroring
 *  mobile's `apiErrorMessage` so the two platforms show the same text for the same error. */
export const apiErrorMessage = (error, fallback = 'Something went wrong') => {
  const body = error?.response?.data;
  if (body?.details?.length) return body.details.join('\n');
  if (body?.message) return body.message;
  return error?.message || fallback;
};

/** Reads the stable error marker the backend sets for conditions the app must react to
 *  rather than merely display (e.g. ORDERING_CLOSED, EMAIL_NOT_VERIFIED). Kept separate from
 *  `apiErrorMessage` since that one is rendered straight to the user. */
export const apiErrorCode = (error) => error?.response?.data?.code ?? null;

export default client;
