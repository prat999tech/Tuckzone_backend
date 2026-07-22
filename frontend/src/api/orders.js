import client from './client';

export const placeOrder = async (data) => {
  const response = await client.post('/orders', data);
  return response.data;
};

export const getMyOrders = async () => {
  const response = await client.get('/orders');
  return response.data;
};

export const getOrder = async (id) => {
  const response = await client.get(`/orders/${id}`);
  return response.data;
};

export const cancelOrder = async (id) => {
  const response = await client.delete(`/orders/${id}`);
  return response.data;
};
