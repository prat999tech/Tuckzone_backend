import { apiClient } from './client';
import type { AuthResponse, OtpIssuedResponse, OtpPurpose, UserSummary } from './types';

export interface StudentRegisterRequest {
  fullName: string;
  email: string;
  mobile: string;
  password: string;
  admissionNumber: string;
  studentClass: string;
  section: string;
  rollNumber: string;
  seatNumber?: string;
  /** Optional — a student can register without it and be linked to a parent later. */
  parentMobile?: string;
  studentMobile?: string;
}

export interface TeacherRegisterRequest {
  fullName: string;
  email: string;
  mobile: string;
  password: string;
  employeeId: string;
  department: string;
}

export interface ParentRegisterRequest {
  fullName: string;
  email: string;
  mobile: string;
  password: string;
}

/**
 * Canteen-admin sign-up. `signupCode` is the invite code held by the canteen owner — the
 * endpoint is public, so without it anyone could grant themselves admin.
 */
export interface AdminRegisterRequest {
  fullName: string;
  email: string;
  mobile: string;
  password: string;
  signupCode: string;
}

export const authApi = {
  registerStudent: (data: StudentRegisterRequest) =>
    apiClient.post<UserSummary>('/auth/register/student', data).then((r) => r.data),

  registerTeacher: (data: TeacherRegisterRequest) =>
    apiClient.post<UserSummary>('/auth/register/teacher', data).then((r) => r.data),

  registerParent: (data: ParentRegisterRequest) =>
    apiClient.post<UserSummary>('/auth/register/parent', data).then((r) => r.data),
  registerAdmin: (data: AdminRegisterRequest) =>
    apiClient.post<UserSummary>('/auth/register/admin', data).then((r) => r.data),

  login: (email: string, password: string) =>
    apiClient.post<AuthResponse>('/auth/login', { email, password }).then((r) => r.data),

  /** Codes are emailed — SMS was dropped, so the identity here is the email address. */
  requestOtp: (email: string, purpose: OtpPurpose) =>
    apiClient.post<OtpIssuedResponse>('/auth/otp/request', { email, purpose }).then((r) => r.data),

  loginWithOtp: (email: string, code: string) =>
    apiClient.post<AuthResponse>('/auth/otp/login', { email, code }).then((r) => r.data),

  resetPassword: (email: string, code: string, newPassword: string) =>
    apiClient.post<void>('/auth/password/reset', { email, code, newPassword }).then((r) => r.data),

  /** Confirms the address supplied at registration. */
  verifyEmail: (email: string, code: string) =>
    apiClient.post<void>('/auth/verify-email', { email, code }).then((r) => r.data),

  logout: (refreshToken: string) =>
    apiClient.post<void>('/auth/logout', { refreshToken }).then((r) => r.data),
};
