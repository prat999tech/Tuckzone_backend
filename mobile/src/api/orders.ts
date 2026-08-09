import { apiClient } from './client';
import type { OrderResponse, OrderType, PaymentMode } from './types';

export interface OrderLineRequest {
  menuItemId: string;
  quantity: number;
}

export interface PlaceOrderRequest {
  orderType?: OrderType;
  menuDate: string;
  beneficiaryWardId?: string;
  deliveryLocation?: string;
  items: OrderLineRequest[];
  idempotencyKey: string;
  /** Omitted (or WALLET_ONLY) = the original, unchanged behaviour: pay the whole total
   *  from wallet balance immediately. GATEWAY_ONLY / WALLET_PLUS_GATEWAY route through
   *  PaymentService instead — see OrderResponse.payment. */
  paymentMode?: PaymentMode;
}

export const ordersApi = {
  place: (data: PlaceOrderRequest) => apiClient.post<OrderResponse>('/orders', data).then((r) => r.data),

  myOrders: (page = 0, size = 50) =>
    apiClient.get<OrderResponse[]>('/orders', { params: { page, size } }).then((r) => r.data),

  getById: (id: string) => apiClient.get<OrderResponse>(`/orders/${id}`).then((r) => r.data),

  // No cancel here: once placed, an order can no longer be cancelled by the buyer — the
  // backend has removed the capability entirely (DELETE /orders/{id} no longer exists).
};
