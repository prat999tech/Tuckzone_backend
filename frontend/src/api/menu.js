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

/** The date the ordering screen should default to right now — see
 *  OrderingWindowService#resolveDefaultOrderingDate. Always tomorrow unless tomorrow's
 *  cutoff has already passed or the canteen closed it, in which case it's the next open date. */
export const getDefaultOrderingDate = async () => {
  const response = await client.get('/ordering-status/default');
  return response.data;
};

export const getOrderingStatus = async (date) => {
  const response = await client.get('/ordering-status', { params: { date } });
  return response.data;
};
