import { apiClient } from './client';
import type { TopupInitResponse, WalletResponse, WalletTransactionResponse } from './types';

export const walletApi = {
  getWallet: () => apiClient.get<WalletResponse>('/wallet').then((r) => r.data),

  getTransactions: (page = 0, size = 50) =>
    apiClient
      .get<WalletTransactionResponse[]>('/wallet/transactions', { params: { page, size } })
      .then((r) => r.data),

  initiateTopup: (amount: number) =>
    apiClient
      .post<TopupInitResponse>('/wallet/topup', { amount: amount.toFixed(2) })
      .then((r) => r.data),

  // Dev-only path while the real payment gateway is not yet integrated (see backend
  // app.payment.allow-mock-topup). Swap this call for the real gateway SDK flow once it
  // is wired up — everything else (wallet balance, transactions) stays the same.
  mockCompleteTopup: (gatewayOrderId: string) =>
    apiClient
      .post<WalletResponse>('/wallet/topup/mock-complete', { gatewayOrderId })
      .then((r) => r.data),

  verifyTopup: (gatewayOrderId: string, gatewayPaymentId: string, signature: string) =>
    apiClient
      .post<WalletResponse>('/wallet/topup/verify', { gatewayOrderId, gatewayPaymentId, signature })
      .then((r) => r.data),
};
