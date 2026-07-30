/**
 * TypeScript mirrors of the backend DTOs (see docs/openapi.json). Kept in one file so a
 * field rename on the backend is a single, obvious place to fix on the client.
 */

export type Role = 'STUDENT' | 'TEACHER' | 'PARENT' | 'CANTEEN_ADMIN';
export type UserStatus = 'ACTIVE' | 'DISABLED';
export type OtpPurpose = 'LOGIN' | 'PASSWORD_RESET' | 'EMAIL_VERIFICATION';
export type FoodType = 'VEG' | 'NON_VEG';
export type MenuCategory = 'SNACKS' | 'MEALS' | 'DRINKS' | 'COMBOS';
export type OrderStatus =
  | 'PLACED'
  | 'ACCEPTED'
  | 'PREPARING'
  | 'PACKED'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REJECTED';
export type OrderType = 'DELIVERY' | 'TAKEAWAY';
export type PaymentStatus = 'PENDING' | 'PAID' | 'REFUNDED';
export type TransactionType = 'CREDIT' | 'DEBIT';
export type OrderingStatus = 'OPEN' | 'CLOSED';
export type ExpenseCategory =
  | 'INGREDIENTS'
  | 'STAFF_WAGES'
  | 'GAS_FUEL'
  | 'RENT'
  | 'UTILITIES'
  | 'PACKAGING'
  | 'EQUIPMENT'
  | 'OTHER';

export interface UserSummary {
  id: string;
  fullName: string;
  email: string;
  mobile: string;
  role: Role;
  status: UserStatus;
  emailVerified?: boolean;
  createdAt: string;
  admissionNumber?: string | null;
  studentClass?: string | null;
  section?: string | null;
  rollNumber?: string | null;
  seatNumber?: string | null;
  studentMobile?: string | null;
  parentMobile?: string | null;
  employeeId?: string | null;
  department?: string | null;
}

export interface AuthResponse {
  tokenType: string;
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: UserSummary;
}

export interface AppConfigResponse {
  currency: string;
  timezone: string;
  otpLength: number;
  otpTtlMinutes: number;
  otpDevCodeReturned: boolean;
  mockPaymentsEnabled: boolean;
  maxTopupAmount: number;
  passwordMinLength: number;
  quickTopupAmounts: number[];
  minimumAppVersion: string;
}

export interface OtpIssuedResponse {
  message: string;
  expiresInMinutes: number;
  devCode: string | null;
}

export interface MenuItemResponse {
  id: string;
  name: string;
  description?: string | null;
  price: number;
  costPrice?: number | null;
  foodType: FoodType;
  category: MenuCategory;
  imageUrl?: string | null;
  allergens?: string | null;
  active: boolean;
}

export interface DailyMenuItemResponse {
  id: string;
  menuDate: string;
  menuItem: MenuItemResponse;
  totalQuantity: number;
  remainingQuantity: number;
  available: boolean;
}

export interface DeliverySlotResponse {
  id: string;
  name: string;
  orderCutoffTime: string;
  deliveryTime: string;
}

export interface OrderItemResponse {
  menuItemId: string;
  itemName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface OrderResponse {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  orderType: OrderType;
  pickupCode?: string | null;
  menuDate: string;
  slotName: string;
  deliveryTime: string;
  recipientName: string;
  deliveryLocation: string;
  deliveryPersonName?: string | null;
  paymentMethod: string;
  paymentStatus: PaymentStatus;
  totalAmount: number;
  items: OrderItemResponse[];
  createdAt: string;
}

export interface WalletResponse {
  userId: string;
  balance: number;
  currency: string;
}

export interface WalletTransactionResponse {
  id: string;
  type: TransactionType;
  amount: number;
  balanceAfter: number;
  referenceType?: string | null;
  referenceId?: string | null;
  description?: string | null;
  createdAt: string;
}

export interface TopupInitResponse {
  topupId: string;
  gatewayOrderId: string;
  amount: number;
  currency: string;
  gatewayKeyId: string;
}

export interface ChildResponse {
  linkId: string;
  studentProfileId: string;
  studentUserId: string;
  fullName: string;
  admissionNumber: string;
  studentClass: string;
  section: string;
  rollNumber: string;
}

export interface NotificationResponse {
  id: string;
  event: string;
  title: string;
  body: string;
  payload?: string | null;
  createdAt: string;
}

export interface OrderingWindowResponse {
  menuDate: string;
  slotId: string;
  slotName: string;
  status: OrderingStatus;
  effectiveCutoffTime: string;
  acceptingOrders: boolean;
  reason?: string | null;
}

export interface DemandRow {
  menuItemId: string;
  itemName: string;
  orderedQuantity: number;
  totalQuantity: number;
  remainingQuantity: number;
  shortfall: number;
}

export interface DashboardResponse {
  date: string;
  totalOrders: number;
  pendingOrders: number;
  completedOrders: number;
  rejectedOrders: number;
  cancelledOrders: number;
  revenue: number;
  costOfGoods: number;
  grossProfit: number;
  expenses: number;
  netProfit: number;
  totalCustomers: number;
  topItems: { itemName: string; quantitySold: number; revenue: number }[];
  lowStock: { itemName: string; remainingQuantity: number; totalQuantity: number }[];
}

export interface SalesReportResponse {
  from: string;
  to: string;
  revenue: number;
  costOfGoods: number;
  grossProfit: number;
  expenses: number;
  netProfit: number;
  orderCount: number;
  daily: { date: string; orders: number; revenue: number }[];
  topItems: { itemName: string; quantitySold: number; revenue: number }[];
  peakHours: { hour: number; orders: number }[];
}

export interface ExpenseResponse {
  id: string;
  expenseDate: string;
  category: ExpenseCategory;
  description?: string | null;
  amount: number;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details: string[];
  /** Stable marker for errors the app must branch on, e.g. 'EMAIL_NOT_VERIFIED'. */
  code?: string | null;
}
