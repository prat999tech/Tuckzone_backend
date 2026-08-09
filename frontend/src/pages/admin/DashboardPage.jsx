import React, { useEffect, useState } from 'react';
import {
  ShoppingBag,
  Clock,
  CheckCircle2,
  Users,
  AlertTriangle,
} from 'lucide-react';
import { getDashboard } from '../../api/admin';
import { getConfig } from '../../api/config';
import toast from 'react-hot-toast';
import './DashboardPage.css';

function StatCard({ icon: Icon, label, value, tone }) {
  return (
    <div className={`stat-card stat-${tone}`}>
      <div className="stat-icon">
        <Icon size={20} />
      </div>
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}

function TopItemsCard({ items, emptyMessage }) {
  if (items.length === 0) {
    return <div className="card top-items-card"><p className="empty-hint">{emptyMessage}</p></div>;
  }
  return (
    <div className="card top-items-card">
      {items.map((item, index) => (
        <div className="top-item-row" key={item.itemName}>
          <span className="rank-badge">{index + 1}</span>
          <span className="top-item-name">{item.itemName}</span>
          <span className="top-item-qty">{item.quantitySold} sold</span>
        </div>
      ))}
    </div>
  );
}

export default function DashboardPage() {
  const [data, setData] = useState(null);
  const [currency, setCurrency] = useState('INR');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboard();
    getConfig().then((c) => setCurrency(c.currency)).catch(() => undefined);
  }, []);

  const fetchDashboard = async () => {
    setLoading(true);
    try {
      const res = await getDashboard();
      setData(res);
    } catch (err) {
      toast.error('Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  };

  const money = (value) => `${currency} ${Number(value || 0).toFixed(2)}`;

  if (loading || !data) {
    return <div className="loading-state">Loading dashboard...</div>;
  }

  const netProfitPositive = Number(data.netProfit) >= 0;

  return (
    <div className="dashboard-container">
      <div className="page-header">
        <h1>Dashboard</h1>
        <p>{data.date}</p>
      </div>

      <div className="stat-grid">
        <StatCard icon={ShoppingBag} label="Today's Orders" value={data.totalOrders} tone="info" />
        <StatCard icon={Clock} label="Pending" value={data.pendingOrders} tone="warning" />
        <StatCard icon={CheckCircle2} label="Completed" value={data.completedOrders} tone="success" />
        <StatCard icon={Users} label="Total Users" value={data.totalCustomers} tone="primary" />
      </div>

      <h2 className="section-title">Revenue &amp; Profit</h2>
      <div className="card finance-card">
        <div className="finance-row">
          <span>Revenue</span>
          <span>{money(data.revenue)}</span>
        </div>
        <div className="finance-row muted">
          <span>Cost of Goods</span>
          <span>- {money(data.costOfGoods)}</span>
        </div>
        <div className="finance-row">
          <span>Gross Profit</span>
          <span>{money(data.grossProfit)}</span>
        </div>
        <div className="finance-row muted">
          <span>Expenses</span>
          <span>- {money(data.expenses)}</span>
        </div>
        <div className="finance-divider" />
        <div className={`finance-row finance-total ${netProfitPositive ? 'text-success' : 'text-error'}`}>
          <span>Net Profit</span>
          <span>{money(data.netProfit)}</span>
        </div>
      </div>

      {data.lowStock.length > 0 && (
        <>
          <h2 className="section-title">Low Stock</h2>
          <div className="card low-stock-card">
            {data.lowStock.map((item) => (
              <div className="low-stock-row" key={item.itemName}>
                <AlertTriangle size={16} />
                <span className="low-stock-name">{item.itemName}</span>
                <span className="low-stock-count">
                  {item.remainingQuantity}/{item.totalQuantity} left
                </span>
              </div>
            ))}
          </div>
        </>
      )}

      <h2 className="section-title">Best-Selling Meal of the Day Items</h2>
      <TopItemsCard items={data.topDailyItems} emptyMessage="No Meal of the Day sales recorded yet today." />

      <h2 className="section-title">Best-Selling Daily Delights Items</h2>
      <TopItemsCard items={data.topFixedItems} emptyMessage="No Daily Delights sales recorded yet today." />
    </div>
  );
}
