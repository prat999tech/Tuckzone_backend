import client from './client';

export const getNotifications = async (page, size) => {
  const response = await client.get('/notifications', { params: { page, size } });
  return response.data;
};
