import client from './client';

/** Public, unauthenticated — launch-screen config (currency, mock-payments flag, etc). */
export const getConfig = async () => {
  const response = await client.get('/config');
  return response.data;
};
