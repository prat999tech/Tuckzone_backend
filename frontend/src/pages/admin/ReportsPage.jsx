import React, { useEffect, useState } from 'react';
import { getSalesReport } from '../../api/admin';
import { getConfig } from '../../api/config';
import toast from 'react-hot-toast';
import '../admin/DashboardPage.css';
import './ReportsPage.css';

function isoDaysAgo(days) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return date.toISOString().slice(0, 10);
}

function TopItemsCard({ items, emptyMessage }) {
  const top10 = items.slice(0, 10);
  if (top10.length === 0) {
    return <div className="card top-items-card"><p className="empty-hint">{emptyMessage}</p></div>;
  }
  return (
    <div className="card top-items-card">
      {top10.map((item, index) => (
        <div className="top-item-row" key={item.itemName}>
          <span className="rank-badge">{index + 1}</span>
          <span className="top-item-name">{item.itemName}</span>
          <span className="top-item-qty">{item.quantitySold} sold</span>
        </div>
      ))}
    </div>
  );
}

export default function ReportsPage() {
  const [from, setFrom] = useState(isoDaysAgo(6));
  const [to, setTo] = useState(isoDaysAgo(0));
  const [currency, setCurrency] = useState('INR');
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchReport();
  }, [from, to]);

  useEffect(() => {
    getConfig().then((c) => setCurrency(c.currency)).catch(() => undefined);
  }, []);

  const fetchReport = async () => {
    setLoading(true);
    try {
      const data = await getSalesReport(from, to);
      setReport(data);
    } catch (err) {
      toast.error('Failed to load report');
    } finally {
      setLoading(false);
    }
  };

  const money = (value) => `${currency} ${Number(value || 0).toFixed(2)}`;

  return (
    <div className="reports-container">
      <div className="page-header">
        <h1>Reports</h1>
        <p>Sales performance for a custom date range</p>
      </div>

      <div className="date-range-row">
        <div className="form-group">
          <label>From</label>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div className="form-group">
          <label>To</label>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
      </div>

      {loading || !report ? (
        <div className="loading-state">Loading report...</div>
      ) : (
        <>
          <div className="card finance-card">
            <div className="finance-row">
              <span>Revenue</span>
              <span>{money(report.revenue)}</span>
            </div>
            <div className="finance-row muted">
              <span>Cost of Goods</span>
              <span>- {money(report.costOfGoods)}</span>
            </div>
            <div className="finance-row">
              <span>Gross Profit</span>
              <span>{money(report.grossProfit)}</span>
            </div>
            <div className="finance-row muted">
              <span>Expenses</span>
              <span>- {money(report.expenses)}</span>
            </div>
            <div className="finance-divider" />
            <div className={`finance-row finance-total ${Number(report.netProfit) >= 0 ? 'text-success' : 'text-error'}`}>
              <span>Net Profit</span>
              <span>{money(report.netProfit)}</span>
            </div>
            <p className="order-count">{report.orderCount} orders in this period</p>
          </div>

          <h2 className="section-title">Daily Breakdown</h2>
          <div className="card daily-breakdown-card">
            {report.daily.length === 0 ? (
              <p className="empty-hint">No sales in this period.</p>
            ) : (
              report.daily.map((day) => (
                <div className="daily-row" key={day.date}>
                  <span className="daily-date">{day.date}</span>
                  <span className="daily-orders">{day.orders} orders</span>
                  <span className="daily-revenue">{money(day.revenue)}</span>
                </div>
              ))
            )}
          </div>

          <h2 className="section-title">Top Meal of the Day Items</h2>
          <TopItemsCard items={report.topDailyItems} emptyMessage="No sales in this period." />

          <h2 className="section-title">Top Daily Delights Items</h2>
          <TopItemsCard items={report.topFixedItems} emptyMessage="No sales in this period." />

          <h2 className="section-title">Peak Order Hours</h2>
          <div className="card peak-hours-card">
            {report.peakHours.length === 0 ? (
              <p className="empty-hint">Not enough data yet.</p>
            ) : (
              <div className="peak-grid">
                {report.peakHours.map((slot) => (
                  <div className="peak-item" key={slot.hour}>
                    <span className="peak-hour">{slot.hour}:00</span>
                    <span className="peak-orders">{slot.orders}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
