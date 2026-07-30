import { apiClient } from './client';
import type { AppConfigResponse } from './types';

export const configApi = {
  get: () => apiClient.get<AppConfigResponse>('/config').then((r) => r.data),
};
