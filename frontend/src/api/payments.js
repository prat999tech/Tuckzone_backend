import client from './client';

/** Preview only — the real charge always re-derives its amount server-side. */
export const calculatePricing = async (data) => {
  const response = await client.post('/payments/calculate-pricing', data);
  return response.data;
};

export const createPayment = async (data) => {
  const response = await client.post('/payments', data);
  return response.data;
};

export const verifyPayment = async (paymentId, data) => {
  const response = await client.post(`/payments/${paymentId}/verify`, data);
  return response.data;
};

/** Dev-only: simulates a successful gateway callback when mock payments are allowed. */
export const mockCompletePayment = async (paymentId) => {
  const response = await client.post(`/payments/${paymentId}/mock-complete`);
  return response.data;
};

export const getPaymentStatus = async (paymentId) => {
  const response = await client.get(`/payments/${paymentId}`);
  return response.data;
};

/** Admin only. */
export const refundPayment = async (paymentId, data) => {
  const response = await client.post(`/payments/${paymentId}/refund`, data);
  return response.data;
};

export const getPlatformFeeSettings = async () => {
  const response = await client.get('/admin/payment-settings');
  return response.data;
};

export const updatePlatformFeeSettings = async (useCase, data) => {
  const response = await client.put(`/admin/payment-settings/${useCase}`, data);
  return response.data;
};

export const getPlatformRevenue = async () => {
  const response = await client.get('/admin/reports/payments');
  return response.data;
};
