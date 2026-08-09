import { apiClient } from './client';
import type {
  AdminOrderResponse,
  DailyMenuItemResponse,
  DashboardResponse,
  DemandRow,
  DeliverySlotResponse,
  ExpenseCategory,
  ExpenseResponse,
  MenuType,
  MenuItemResponse,
  OrderStatus,
  OrderingStatus,
  OrderingWindowResponse,
  PaymentUseCase,
  PlatformFeeSettingsResponse,
  PlatformFeeType,
  Role,
  SalesReportResponse,
  UserSummary,
} from './types';

export interface MenuItemRequest {
  name: string;
  description?: string;
  price: number;
  costPrice?: number;
  menuType: MenuType;
  available: boolean;
  // Deliberately no imageUrl — an image is set only via uploadMenuItemImage (multipart),
  // never by pasting a URL. See MenuItemRequest's javadoc on the backend.
  allergens?: string;
}

/** What ImagePicker's launchImageLibraryAsync hands back for the one asset we care about. */
export interface PickedImage {
  uri: string;
  fileName?: string | null;
  mimeType?: string | null;
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

export interface SubAdminCreateRequest {
  fullName: string;
  email: string;
  mobile: string;
  password: string;
}

export interface SubAdminUpdateRequest {
  fullName: string;
  email: string;
  mobile: string;
  newPassword?: string;
}

export interface PlatformFeeSettingsUpdateRequest {
  enabled: boolean;
  feeType: PlatformFeeType;
  feeValue: number;
  minFee?: number | null;
  maxFee?: number | null;
}

export const adminApi = {
  // Menu catalog
  listMenuItems: (includeInactive = false, menuType?: MenuType) =>
    apiClient
      .get<MenuItemResponse[]>('/admin/menu-items', { params: { includeInactive, menuType } })
      .then((r) => r.data),
  createMenuItem: (data: MenuItemRequest) =>
    apiClient.post<MenuItemResponse>('/admin/menu-items', data).then((r) => r.data),
  updateMenuItem: (id: string, data: MenuItemRequest) =>
    apiClient.put<MenuItemResponse>(`/admin/menu-items/${id}`, data).then((r) => r.data),
  /** Soft delete (Retire): hides the item from students, keeps it under the Retired filter. */
  deactivateMenuItem: (id: string) => apiClient.delete<void>(`/admin/menu-items/${id}`).then((r) => r.data),
  /** Brings a retired item back to Active. */
  activateMenuItem: (id: string) =>
    apiClient.post<MenuItemResponse>(`/admin/menu-items/${id}/activate`).then((r) => r.data),
  /** Hard delete — rejected server-side if the item has any order history. */
  permanentlyDeleteMenuItem: (id: string) =>
    apiClient.delete<void>(`/admin/menu-items/${id}/permanent`).then((r) => r.data),
  /** Uploads (or replaces) a menu item's photo. The backend validates type/size for real —
   *  this only ever runs after the same checks already passed in MenuManagementScreen. */
  uploadMenuItemImage: (id: string, image: PickedImage) => {
    const form = new FormData();
    // React Native's FormData accepts this {uri, name, type} shape for a file part — it is
    // not a real Blob/File, which is why the cast is needed against the DOM FormData types
    // TypeScript otherwise assumes.
    form.append('file', {
      uri: image.uri,
      name: image.fileName ?? 'food-image.jpg',
      type: image.mimeType ?? 'image/jpeg',
    } as unknown as Blob);
    return apiClient
      .post<MenuItemResponse>(`/admin/menu-items/${id}/image`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data);
  },
  /** Reverts to the ordering screen's default placeholder. */
  removeMenuItemImage: (id: string) =>
    apiClient.delete<MenuItemResponse>(`/admin/menu-items/${id}/image`).then((r) => r.data),

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
    apiClient.get<AdminOrderResponse[]>('/admin/orders', { params: { date, status } }).then((r) => r.data),
  updateOrderStatus: (id: string, data: OrderStatusUpdateRequest) =>
    apiClient.put<AdminOrderResponse>(`/admin/orders/${id}/status`, data).then((r) => r.data),

  // Advance ordering control
  getOrderingStatus: (date: string) =>
    apiClient.get<OrderingWindowResponse[]>('/ordering-status', { params: { date } }).then((r) => r.data),
  closeOrdering: (data: OrderingWindowRequest) =>
    apiClient.post<OrderingWindowResponse>('/admin/ordering/close', data).then((r) => r.data),
  openOrdering: (data: OrderingWindowRequest) =>
    apiClient.post<OrderingWindowResponse>('/admin/ordering/open', data).then((r) => r.data),
  getDemand: (date: string) => apiClient.get<DemandRow[]>('/admin/demand', { params: { date } }).then((r) => r.data),
  /** Changes the slot's standing daily cutoff time — the default every date falls back to
   *  unless that date has its own open() override. Takes effect immediately. */
  updateCutoffTime: (slotId: string, orderCutoffTime: string) =>
    apiClient
      .put<DeliverySlotResponse>(`/admin/delivery-slots/${slotId}/cutoff-time`, { orderCutoffTime })
      .then((r) => r.data),

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

  // Payment settings (platform fee)
  listPaymentSettings: () =>
    apiClient.get<PlatformFeeSettingsResponse[]>('/admin/payment-settings').then((r) => r.data),
  updatePaymentSettings: (useCase: PaymentUseCase, data: PlatformFeeSettingsUpdateRequest) =>
    apiClient.put<PlatformFeeSettingsResponse>(`/admin/payment-settings/${useCase}`, data).then((r) => r.data),

  // Accounts
  listUsers: (role?: Role, page = 0, size = 100) =>
    apiClient.get<UserSummary[]>('/admin/users', { params: { role, page, size } }).then((r) => r.data),
  disableUser: (id: string) => apiClient.post<UserSummary>(`/admin/users/${id}/disable`).then((r) => r.data),
  enableUser: (id: string) => apiClient.post<UserSummary>(`/admin/users/${id}/enable`).then((r) => r.data),

  // Order export (Canteen Admin + Sub Admin)
  exportOrdersExcel: (date: string, status?: OrderStatus, search?: string) =>
    apiClient
      .get<ArrayBuffer>('/admin/orders/export/excel', { params: { date, status, search }, responseType: 'arraybuffer' })
      .then((r) => r.data),
  exportOrdersPdf: (date: string, status?: OrderStatus, search?: string) =>
    apiClient
      .get<ArrayBuffer>('/admin/orders/export/pdf', { params: { date, status, search }, responseType: 'arraybuffer' })
      .then((r) => r.data),

  // Sub Admin account management (Canteen Admin only)
  listSubAdmins: () => apiClient.get<UserSummary[]>('/admin/sub-admins').then((r) => r.data),
  createSubAdmin: (data: SubAdminCreateRequest) =>
    apiClient.post<UserSummary>('/admin/sub-admins', data).then((r) => r.data),
  updateSubAdmin: (id: string, data: SubAdminUpdateRequest) =>
    apiClient.put<UserSummary>(`/admin/sub-admins/${id}`, data).then((r) => r.data),
  activateSubAdmin: (id: string) => apiClient.post<UserSummary>(`/admin/sub-admins/${id}/activate`).then((r) => r.data),
  deactivateSubAdmin: (id: string) =>
    apiClient.post<UserSummary>(`/admin/sub-admins/${id}/deactivate`).then((r) => r.data),
  deleteSubAdmin: (id: string) => apiClient.delete<void>(`/admin/sub-admins/${id}`).then((r) => r.data),
};

export type { OrderingStatus };
