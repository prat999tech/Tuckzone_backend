import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';

import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import MenuPage from './pages/MenuPage';
import OrdersPage from './pages/OrdersPage';
import WalletPage from './pages/WalletPage';
import ChildrenPage from './pages/ChildrenPage';
import UserApprovalPage from './pages/admin/UserApprovalPage';
import MenuItemsPage from './pages/admin/MenuItemsPage';
import DailyMenuPage from './pages/admin/DailyMenuPage';
import OrdersBoardPage from './pages/admin/OrdersBoardPage';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <CartProvider>
          <Toaster
            position="top-right"
            toastOptions={{
              style: {
                background: '#ffffff',
                color: '#1f2937',
                border: '1px solid #e5e7eb',
                borderRadius: '10px',
                boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
                fontSize: '0.875rem',
                fontFamily: 'Inter, system-ui, sans-serif',
              },
              success: {
                iconTheme: { primary: '#16a34a', secondary: '#ffffff' },
              },
              error: {
                iconTheme: { primary: '#dc2626', secondary: '#ffffff' },
              },
            }}
          />
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            {/* Protected Routes */}
            <Route element={<ProtectedRoute />}>
              <Route element={<Layout />}>
                <Route path="/" element={<Navigate to="/menu" replace />} />
                <Route path="/menu" element={<MenuPage />} />
                <Route path="/orders" element={<OrdersPage />} />
                <Route path="/wallet" element={<WalletPage />} />

                {/* Parent Only */}
                <Route element={<ProtectedRoute allowedRoles={['PARENT']} />}>
                  <Route path="/children" element={<ChildrenPage />} />
                </Route>

                {/* School Admin Only */}
                <Route element={<ProtectedRoute allowedRoles={['SCHOOL_ADMIN']} />}>
                  <Route path="/admin/users" element={<UserApprovalPage />} />
                </Route>

                {/* Canteen Admin Only */}
                <Route element={<ProtectedRoute allowedRoles={['CANTEEN_ADMIN']} />}>
                  <Route path="/admin/menu-items" element={<MenuItemsPage />} />
                  <Route path="/admin/daily-menu" element={<DailyMenuPage />} />
                  <Route path="/admin/orders" element={<OrdersBoardPage />} />
                </Route>
              </Route>
            </Route>

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </CartProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
