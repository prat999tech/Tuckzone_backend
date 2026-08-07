import client from './client';

export const registerStudent = async (data) => {
  const response = await client.post('/auth/register/student', data);
  return response.data;
};

export const registerTeacher = async (data) => {
  const response = await client.post('/auth/register/teacher', data);
  return response.data;
};

export const registerParent = async (data) => {
  const response = await client.post('/auth/register/parent', data);
  return response.data;
};

export const login = async (data) => {
  const response = await client.post('/auth/login', data);
  return response.data;
};

/** Codes are emailed — SMS was dropped, so the identity here is the email address. */
export const requestOtp = async (email, purpose) => {
  const response = await client.post('/auth/otp/request', { email, purpose });
  return response.data;
};

export const loginWithOtp = async (email, code) => {
  const response = await client.post('/auth/otp/login', { email, code });
  return response.data;
};

export const refresh = async (data) => {
  const response = await client.post('/auth/refresh', data);
  return response.data;
};

export const getMe = async () => {
  const response = await client.get('/me');
  return response.data;
};
