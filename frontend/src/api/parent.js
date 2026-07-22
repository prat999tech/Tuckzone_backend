import client from './client';

export const linkChild = async (data) => {
  const response = await client.post('/parent/children', data);
  return response.data;
};

export const getChildren = async () => {
  const response = await client.get('/parent/children');
  return response.data;
};

export const unlinkChild = async (linkId) => {
  const response = await client.delete(`/parent/children/${linkId}`);
  return response.data;
};
