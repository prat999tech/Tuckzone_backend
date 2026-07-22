import client from './client';

export const listUsers = async (status) => {
  const response = await client.get('/admin/users', { params: { status } });
  return response.data;
};

export const approveUser = async (id) => {
  const response = await client.post(`/admin/users/${id}/approve`);
  return response.data;
};

export const rejectUser = async (id) => {
  const response = await client.post(`/admin/users/${id}/reject`);
  return response.data;
};

export const createMenuItem = async (data) => {
  const response = await client.post('/admin/menu-items', data);
  return response.data;
};

export const getMenuItems = async (includeInactive) => {
  const response = await client.get('/admin/menu-items', { params: { includeInactive } });
  return response.data;
};

export const updateMenuItem = async (id, data) => {
  const response = await client.put(`/admin/menu-items/${id}`, data);
  return response.data;
};

export const deleteMenuItem = async (id) => {
  const response = await client.delete(`/admin/menu-items/${id}`);
  return response.data;
};

export const addDailyMenu = async (data) => {
  const response = await client.post('/admin/daily-menu', data);
  return response.data;
};

export const getDailyMenu = async (date) => {
  const response = await client.get('/admin/daily-menu', { params: { date } });
  return response.data;
};

export const updateDailyMenu = async (id, data) => {
  const response = await client.put(`/admin/daily-menu/${id}`, data);
  return response.data;
};

export const removeDailyMenu = async (id) => {
  const response = await client.delete(`/admin/daily-menu/${id}`);
  return response.data;
};

export const getAdminOrders = async (date, status) => {
  const response = await client.get('/admin/orders', { params: { date, status } });
  return response.data;
};

export const updateOrderStatus = async (id, data) => {
  const response = await client.put(`/admin/orders/${id}/status`, data);
  return response.data;
};
