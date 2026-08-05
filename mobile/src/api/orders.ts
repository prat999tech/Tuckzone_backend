import { apiClient } from './client';
import type { OrderResponse, OrderType } from './types';

export interface OrderLineRequest {
  menuItemId: string;
  quantity: number;
}

export interface PlaceOrderRequest {
  orderType?: OrderType;
  menuDate: string;
  beneficiaryStudentProfileId?: string;
  deliveryLocation?: string;
  items: OrderLineRequest[];
  idempotencyKey: string;
}

export const ordersApi = {
  place: (data: PlaceOrderRequest) => apiClient.post<OrderResponse>('/orders', data).then((r) => r.data),

  myOrders: (page = 0, size = 50) =>
    apiClient.get<OrderResponse[]>('/orders', { params: { page, size } }).then((r) => r.data),

  getById: (id: string) => apiClient.get<OrderResponse>(`/orders/${id}`).then((r) => r.data),

  cancel: (id: string) => apiClient.delete<OrderResponse>(`/orders/${id}`).then((r) => r.data),
};
