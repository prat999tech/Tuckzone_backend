import client from './client';

export const getWallet = async () => {
  const response = await client.get('/wallet');
  return response.data;
};

export const getTransactions = async () => {
  const response = await client.get('/wallet/transactions');
  return response.data;
};

export const initiateTopup = async (data) => {
  const response = await client.post('/wallet/topup', data);
  return response.data;
};

export const verifyTopup = async (data) => {
  const response = await client.post('/wallet/topup/verify', data);
  return response.data;
};

export const mockCompleteTopup = async (data) => {
  const response = await client.post('/wallet/topup/mock-complete', data);
  return response.data;
};
