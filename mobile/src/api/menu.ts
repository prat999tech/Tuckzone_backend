import { apiClient } from './client';
import type {
  DailyMenuItemResponse,
  DefaultOrderingDateResponse,
  DeliverySlotResponse,
  MenuItemResponse,
  OrderingWindowResponse,
} from './types';

export const menuApi = {
  getDailyMenu: (params: { date?: string; q?: string }) =>
    apiClient.get<DailyMenuItemResponse[]>('/menu/daily', { params }).then((r) => r.data),

  getFixedMenu: (params: { q?: string } = {}) =>
    apiClient.get<MenuItemResponse[]>('/menu/fixed', { params }).then((r) => r.data),

  getDeliverySlots: () =>
    apiClient.get<DeliverySlotResponse[]>('/delivery-slots').then((r) => r.data),

  getOrderingStatus: (date: string) =>
    apiClient.get<OrderingWindowResponse[]>('/ordering-status', { params: { date } }).then((r) => r.data),

  /** The date the ordering screen should default to right now — always tomorrow unless
   *  its cutoff already passed or the canteen closed it, in which case the next open date. */
  getDefaultOrderingDate: () =>
    apiClient.get<DefaultOrderingDateResponse>('/ordering-status/default').then((r) => r.data),
};
