import { apiClient } from './client';
import type { DailyMenuItemResponse, DeliverySlotResponse, FoodType, MenuCategory, OrderingWindowResponse } from './types';

export const menuApi = {
  getMenu: (params: { date?: string; foodType?: FoodType; category?: MenuCategory; q?: string }) =>
    apiClient.get<DailyMenuItemResponse[]>('/menu', { params }).then((r) => r.data),

  getDeliverySlots: () =>
    apiClient.get<DeliverySlotResponse[]>('/delivery-slots').then((r) => r.data),

  getOrderingStatus: (date: string) =>
    apiClient.get<OrderingWindowResponse[]>('/ordering-status', { params: { date } }).then((r) => r.data),
};
