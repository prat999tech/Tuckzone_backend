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

export const getMenuItems = async (includeInactive, menuType) => {
  const response = await client.get('/admin/menu-items', { params: { includeInactive, menuType } });
  return response.data;
};

export const updateMenuItem = async (id, data) => {
  const response = await client.put(`/admin/menu-items/${id}`, data);
  return response.data;
};

// Soft delete (Retire): hides the item from students, keeps it under the Retired filter.
export const deleteMenuItem = async (id) => {
  const response = await client.delete(`/admin/menu-items/${id}`);
  return response.data;
};

// Brings a retired item back to Active.
export const activateMenuItem = async (id) => {
  const response = await client.post(`/admin/menu-items/${id}/activate`);
  return response.data;
};

// Hard delete. Past orders are unaffected — each keeps its own snapshot of the item's
// name/price/etc., independent of this catalog row.
export const permanentlyDeleteMenuItem = async (id) => {
  const response = await client.delete(`/admin/menu-items/${id}/permanent`);
  return response.data;
};

// Changes the slot's standing daily cutoff time — the default every date falls back to
// unless that date has its own open() override on the mobile Advance Ordering screen.
// Takes effect immediately.
export const updateCutoffTime = async (slotId, orderCutoffTime) => {
  const response = await client.put(`/admin/delivery-slots/${slotId}/cutoff-time`, { orderCutoffTime });
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

export const getAdminOrders = async (date, status, search, sort) => {
  const response = await client.get('/admin/orders', { params: { date, status, search, sort } });
  return response.data;
};

export const updateOrderStatus = async (id, data) => {
  const response = await client.put(`/admin/orders/${id}/status`, data);
  return response.data;
};

export const exportOrdersExcel = async (date, status, search) => {
  const response = await client.get('/admin/orders/export/excel', {
    params: { date, status, search },
    responseType: 'blob',
  });
  return response.data;
};

export const exportOrdersPdf = async (date, status, search) => {
  const response = await client.get('/admin/orders/export/pdf', {
    params: { date, status, search },
    responseType: 'blob',
  });
  return response.data;
};

// ─── Sub Admin account management (Canteen Admin only) ───
export const listSubAdmins = async () => {
  const response = await client.get('/admin/sub-admins');
  return response.data;
};

export const createSubAdmin = async (data) => {
  const response = await client.post('/admin/sub-admins', data);
  return response.data;
};

export const updateSubAdmin = async (id, data) => {
  const response = await client.put(`/admin/sub-admins/${id}`, data);
  return response.data;
};

export const activateSubAdmin = async (id) => {
  const response = await client.post(`/admin/sub-admins/${id}/activate`);
  return response.data;
};

export const deactivateSubAdmin = async (id) => {
  const response = await client.post(`/admin/sub-admins/${id}/deactivate`);
  return response.data;
};

export const deleteSubAdmin = async (id) => {
  const response = await client.delete(`/admin/sub-admins/${id}`);
  return response.data;
};
