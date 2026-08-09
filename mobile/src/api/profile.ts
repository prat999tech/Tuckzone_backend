import { apiClient } from './client';
import type { UserSummary } from './types';

export interface ProfileUpdateRequest {
  fullName: string;
  studentClass?: string;
  section?: string;
  rollNumber?: string;
  seatNumber?: string;
  department?: string;
}

export const profileApi = {
  getMe: () => apiClient.get<UserSummary>('/me').then((r) => r.data),

  updateMe: (data: ProfileUpdateRequest) => apiClient.put<UserSummary>('/me', data).then((r) => r.data),
};
