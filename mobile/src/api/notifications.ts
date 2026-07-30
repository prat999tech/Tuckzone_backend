import { apiClient } from './client';
import type { NotificationResponse } from './types';

export const notificationsApi = {
  getFeed: (page = 0, size = 50) =>
    apiClient
      .get<NotificationResponse[]>('/notifications', { params: { page, size } })
      .then((r) => r.data),

  registerDevice: (token: string, platform: 'android' | 'ios') =>
    apiClient.post<void>('/devices', { token, platform }).then((r) => r.data),

  unregisterDevice: (token: string) =>
    apiClient.delete<void>('/devices', { params: { token } }).then((r) => r.data),
};
