/** Shared param-list types so `navigation.navigate(...)` calls are type-checked. */

export type AuthStackParamList = {
  Login: undefined;
  Register: undefined;
  ForgotPassword: undefined;
  /** `devCode` is only ever populated when the backend runs with OTP dev codes enabled. */
  VerifyEmail: { email: string; devCode?: string | null };
};

export type CustomerTabParamList = {
  Menu: undefined;
  Orders: undefined;
  Wallet: undefined;
  Children: undefined;
  Profile: undefined;
};

export type CustomerStackParamList = {
  CustomerTabs: undefined;
  Checkout: undefined;
  /** Reached by replace() from Checkout, so back cannot return to a submitted cart. */
  OrderConfirmation: { orderId: string };
  OrderDetail: { orderId: string };
  Notifications: undefined;
};

export type AdminTabParamList = {
  Dashboard: undefined;
  OrdersBoard: undefined;
  MenuManagement: undefined;
  More: undefined;
};

export type AdminStackParamList = {
  AdminTabs: undefined;
  Reports: undefined;
  Expenses: undefined;
  Users: undefined;
  OrderingWindows: undefined;
  TakeawayCollect: undefined;
  Notifications: undefined;
  SubAdmins: undefined;
};

export type SubAdminTabParamList = {
  OrdersBoard: undefined;
  MenuManagement: undefined;
  ExportOrders: undefined;
  Account: undefined;
};

export type SubAdminStackParamList = {
  SubAdminTabs: undefined;
};
