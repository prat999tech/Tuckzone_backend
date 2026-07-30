import { apiClient } from './client';
import type {
  DailyMenuItemResponse,
  DashboardResponse,
  DemandRow,
  ExpenseCategory,
  ExpenseResponse,
  FoodType,
  MenuCategory,
  MenuItemResponse,
  OrderResponse,
  OrderStatus,
  OrderingStatus,
  OrderingWindowResponse,
  Role,
  SalesReportResponse,
  UserSummary,
} from './types';

export interface MenuItemRequest {
  name: string;
  description?: string;
  price: number;
  costPrice?: number;
  foodType: FoodType;
  category: MenuCategory;
  imageUrl?: string;
  allergens?: string;
}

export interface OrderStatusUpdateRequest {
  status: OrderStatus;
  deliveryPersonName?: string;
}

export interface OrderingWindowRequest {
  menuDate: string;
  slotId: string;
  overrideCutoffTime?: string;
  reason?: string;
}

export interface ExpenseRequest {
  expenseDate: string;
  category: ExpenseCategory;
  description?: string;
  amount: number;
}

export const adminApi = {
  // Menu catalog
  listMenuItems: (includeInactive = false) =>
    apiClient
      .get<MenuItemResponse[]>('/admin/menu-items', { params: { includeInactive } })
      .then((r) => r.data),
  createMenuItem: (data: MenuItemRequest) =>
    apiClient.post<MenuItemResponse>('/admin/menu-items', data).then((r) => r.data),
  updateMenuItem: (id: string, data: MenuItemRequest) =>
    apiClient.put<MenuItemResponse>(`/admin/menu-items/${id}`, data).then((r) => r.data),
  deactivateMenuItem: (id: string) => apiClient.delete<void>(`/admin/menu-items/${id}`).then((r) => r.data),

  // Daily menu / stock
  listDailyMenu: (date: string) =>
    apiClient.get<DailyMenuItemResponse[]>('/admin/daily-menu', { params: { date } }).then((r) => r.data),
  addDailyMenuItem: (menuDate: string, menuItemId: string, totalQuantity: number) =>
    apiClient
      .post<DailyMenuItemResponse>('/admin/daily-menu', { menuDate, menuItemId, totalQuantity })
      .then((r) => r.data),
  updateDailyMenuItem: (id: string, totalQuantity: number, available: boolean) =>
    apiClient
      .put<DailyMenuItemResponse>(`/admin/daily-menu/${id}`, { totalQuantity, available })
      .then((r) => r.data),
  removeDailyMenuItem: (id: string) => apiClient.delete<void>(`/admin/daily-menu/${id}`).then((r) => r.data),

  // Orders board
  listOrders: (date: string, status?: OrderStatus) =>
    apiClient.get<OrderResponse[]>('/admin/orders', { params: { date, status } }).then((r) => r.data),
  updateOrderStatus: (id: string, data: OrderStatusUpdateRequest) =>
    apiClient.put<OrderResponse>(`/admin/orders/${id}/status`, data).then((r) => r.data),
  collectTakeaway: (date: string, pickupCode: string) =>
    apiClient
      .post<OrderResponse>('/admin/orders/collect', null, { params: { date, pickupCode } })
      .then((r) => r.data),

  // Advance ordering control
  getOrderingStatus: (date: string) =>
    apiClient.get<OrderingWindowResponse[]>('/ordering-status', { params: { date } }).then((r) => r.data),
  closeOrdering: (data: OrderingWindowRequest) =>
    apiClient.post<OrderingWindowResponse>('/admin/ordering/close', data).then((r) => r.data),
  openOrdering: (data: OrderingWindowRequest) =>
    apiClient.post<OrderingWindowResponse>('/admin/ordering/open', data).then((r) => r.data),
  getDemand: (date: string) => apiClient.get<DemandRow[]>('/admin/demand', { params: { date } }).then((r) => r.data),

  // Reporting
  getDashboard: (date?: string) =>
    apiClient.get<DashboardResponse>('/admin/dashboard', { params: { date } }).then((r) => r.data),
  getSalesReport: (from: string, to: string) =>
    apiClient.get<SalesReportResponse>('/admin/reports/sales', { params: { from, to } }).then((r) => r.data),

  // Expenses
  listExpenses: (from: string, to: string) =>
    apiClient.get<ExpenseResponse[]>('/admin/expenses', { params: { from, to } }).then((r) => r.data),
  addExpense: (data: ExpenseRequest) =>
    apiClient.post<ExpenseResponse>('/admin/expenses', data).then((r) => r.data),
  deleteExpense: (id: string) => apiClient.delete<void>(`/admin/expenses/${id}`).then((r) => r.data),

  // Accounts
  listUsers: (role?: Role, page = 0, size = 100) =>
    apiClient.get<UserSummary[]>('/admin/users', { params: { role, page, size } }).then((r) => r.data),
  disableUser: (id: string) => apiClient.post<UserSummary>(`/admin/users/${id}/disable`).then((r) => r.data),
  enableUser: (id: string) => apiClient.post<UserSummary>(`/admin/users/${id}/enable`).then((r) => r.data),
};

export type { OrderingStatus };
