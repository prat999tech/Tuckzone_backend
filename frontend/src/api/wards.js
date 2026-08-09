import client from './client';

export const listWards = async () => {
  const response = await client.get('/parent/wards');
  return response.data;
};

export const createWard = async (data) => {
  const response = await client.post('/parent/wards', data);
  return response.data;
};

export const updateWard = async (wardId, data) => {
  const response = await client.put(`/parent/wards/${wardId}`, data);
  return response.data;
};

export const deleteWard = async (wardId) => {
  const response = await client.delete(`/parent/wards/${wardId}`);
  return response.data;
};
