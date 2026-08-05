import client from './client';

export const getTodayMenu = async (params) => {
  const response = await client.get('/menu/daily', { params });
  return response.data;
};

export const getFixedMenu = async (q) => {
  const response = await client.get('/menu/fixed', { params: { q } });
  return response.data;
};

export const getDeliverySlots = async () => {
  const response = await client.get('/delivery-slots');
  return response.data;
};
