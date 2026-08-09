import { apiClient } from './client';
import type { WardRequest, WardResponse } from './types';

export const wardsApi = {
  list: () => apiClient.get<WardResponse[]>('/parent/wards').then((r) => r.data),

  create: (data: WardRequest) => apiClient.post<WardResponse>('/parent/wards', data).then((r) => r.data),

  update: (wardId: string, data: WardRequest) =>
    apiClient.put<WardResponse>(`/parent/wards/${wardId}`, data).then((r) => r.data),

  remove: (wardId: string) => apiClient.delete<void>(`/parent/wards/${wardId}`).then((r) => r.data),
};
