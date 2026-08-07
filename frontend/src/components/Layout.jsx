import React, { useState } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  UtensilsCrossed,
  ShoppingBag,
  Wallet,
  Users,
  UserCheck,
  ChefHat,
  Calendar,
  ClipboardList,
  FileDown,
  ShieldPlus,
  CreditCard,
  LogOut,
  Menu,
  X
} from 'lucide-react';
import './Layout.css';

const Layout = () => {
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const closeMenu = () => setMobileMenuOpen(false);

  const getNavLinks = () => {
    const commonLinks = [
      { to: '/menu',   icon: UtensilsCrossed, label: 'Menu' },
      { to: '/orders', icon: ShoppingBag,     label: 'My Orders' },
      { to: '/wallet', icon: Wallet,           label: 'Wallet' },
    ];

    switch (role) {
      case 'STUDENT':
      case 'TEACHER':
        return commonLinks;
      case 'PARENT':
        return [...commonLinks, { to: '/children', icon: Users, label: 'My Children' }];
      case 'SCHOOL_ADMIN':
        return [{ to: '/admin/users', icon: UserCheck, label: 'User Approvals' }];
      case 'CANTEEN_ADMIN':
        return [
          { to: '/admin/daily-menu', icon: Calendar,        label: 'Meal of the Day' },
          { to: '/admin/fixed-menu', icon: ChefHat,         label: 'Daily Delights' },
          { to: '/admin/orders',     icon: ClipboardList,   label: 'Orders Board' },
          { to: '/admin/sub-admins', icon: ShieldPlus,      label: 'Sub Admins' },
          { to: '/admin/payment-settings', icon: CreditCard, label: 'Payment Settings' },
        ];
      case 'SUB_ADMIN':
        return [
          { to: '/subadmin/orders', icon: ClipboardList, label: 'Incoming Orders' },
          { to: '/subadmin/menu',   icon: ChefHat,        label: 'Menu Management' },
          { to: '/subadmin/export', icon: FileDown,       label: 'Export Orders' },
        ];
      default:
        return [];
    }
  };

  const initials = user?.fullName?.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase() || '?';

  const SidebarContent = () => (
    <>
      <div className="sidebar-header desktop-only">
        <UtensilsCrossed className="logo-icon" size={26} />
        <h2>School<span>Bite</span></h2>
      </div>

      <nav className="sidebar-nav">
        {getNavLinks().map((link) => (
          <NavLink 
            key={link.to} 
            to={link.to} 
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            onClick={closeMenu}
          >
            <link.icon className="nav-icon" size={18} />
            <span>{link.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="sidebar-user">
          <div className="sidebar-avatar">{initials}</div>
          <div className="sidebar-user-info">
            <span className="sidebar-user-name">{user?.fullName}</span>
            <span className="sidebar-user-role">{role?.replace('_', ' ')}</span>
          </div>
        </div>
        <button className="btn-logout" onClick={handleLogout}>
          <LogOut size={16} />
          <span>Sign Out</span>
        </button>
      </div>
    </>
  );

  return (
    <div className="layout-container">
      {/* Mobile Header */}
      <div className="mobile-header">
        <div className="logo">
          <UtensilsCrossed size={22} />
          <span>TuckZone</span>
        </div>
        <button className="mobile-toggle" onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
          {mobileMenuOpen ? <X size={22} /> : <Menu size={22} />}
        </button>
      </div>

      {/* Sidebar */}
      <aside className={`sidebar ${mobileMenuOpen ? 'open' : ''}`}>
        <SidebarContent />
      </aside>

      {/* Main Content */}
      <main className="main-content">
        <header className="top-header">
          <div className="header-title">TuckZone Canteen</div>
          <div className="user-profile">
            <div className="user-info">
              <span className="user-name">{user?.fullName}</span>
              <span className="role-badge">{role?.replace('_', ' ')}</span>
            </div>
            <div className="avatar">{initials}</div>
          </div>
        </header>

        <div className="content-area">
          <Outlet />
        </div>
      </main>

      {/* Mobile Overlay */}
      {mobileMenuOpen && (
        <div className="mobile-overlay" onClick={closeMenu}></div>
      )}
    </div>
  );
};

export default Layout;
