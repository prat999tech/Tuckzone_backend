import { apiClient } from './client';
import type { DailyMenuItemResponse, DeliverySlotResponse, MenuItemResponse, OrderingWindowResponse } from './types';

export const menuApi = {
  getDailyMenu: (params: { date?: string; q?: string }) =>
    apiClient.get<DailyMenuItemResponse[]>('/menu/daily', { params }).then((r) => r.data),

  getFixedMenu: (params: { q?: string } = {}) =>
    apiClient.get<MenuItemResponse[]>('/menu/fixed', { params }).then((r) => r.data),

  getDeliverySlots: () =>
    apiClient.get<DeliverySlotResponse[]>('/delivery-slots').then((r) => r.data),

  getOrderingStatus: (date: string) =>
    apiClient.get<OrderingWindowResponse[]>('/ordering-status', { params: { date } }).then((r) => r.data),
};
