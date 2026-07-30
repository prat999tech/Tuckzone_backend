import { apiClient } from './client';
import type { ChildResponse } from './types';

export const parentApi = {
  linkChild: (admissionNumber: string, parentMobile: string) =>
    apiClient.post<ChildResponse>('/parent/children', { admissionNumber, parentMobile }).then((r) => r.data),

  getChildren: () => apiClient.get<ChildResponse[]>('/parent/children').then((r) => r.data),

  unlinkChild: (linkId: string) =>
    apiClient.delete<void>(`/parent/children/${linkId}`).then((r) => r.data),
};
