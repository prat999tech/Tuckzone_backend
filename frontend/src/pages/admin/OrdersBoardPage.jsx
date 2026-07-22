import React, { useEffect, useState } from 'react';
import { Calendar, RefreshCw } from 'lucide-react';
import { getAdminOrders, updateOrderStatus } from '../../api/admin';
import toast from 'react-hot-toast';
import './OrdersBoardPage.css';

export default function OrdersBoardPage() {
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [deliveryPerson, setDeliveryPerson] = useState({});

  useEffect(() => {
    fetchOrders();
  }, [date]);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const data = await getAdminOrders(date);
      setOrders(data);
    } catch (err) {
      toast.error('Failed to load orders');
    } finally {
      setLoading(false);
    }
  };

  const updateStatusAction = async (orderId, status) => {
    try {
      const payload = { status };
      if (status === 'OUT_FOR_DELIVERY') {
        if (!deliveryPerson[orderId]) {
          return toast.error('Enter delivery person name');
        }
        payload.deliveryPersonName = deliveryPerson[orderId];
      }
      await updateOrderStatus(orderId, payload);
      toast.success('Order status updated');
      fetchOrders();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Status update failed');
    }
  };

  const filteredOrders = orders.filter((order) => statusFilter === 'ALL' || order.status === statusFilter);

  const renderActionButtons = (order) => {
    switch (order.status) {
      case 'PLACED':
        return (
          <div className="action-buttons">
            <button className="btn-success" onClick={() => updateStatusAction(order.id, 'ACCEPTED')}>Accept</button>
            <button className="btn-danger" onClick={() => updateStatusAction(order.id, 'REJECTED')}>Reject</button>
          </div>
        );
      case 'ACCEPTED':
        return <button className="btn-primary w-100" onClick={() => updateStatusAction(order.id, 'PREPARING')}>Start Preparing</button>;
      case 'PREPARING':
        return <button className="btn-primary w-100" onClick={() => updateStatusAction(order.id, 'PACKED')}>Mark Packed</button>;
      case 'PACKED':
        return (
          <div className="dispatch-form">
            <input
              type="text"
              placeholder="Delivery person name"
              value={deliveryPerson[order.id] || ''}
              onChange={(e) => setDeliveryPerson({ ...deliveryPerson, [order.id]: e.target.value })}
            />
            <button className="btn-amber" onClick={() => updateStatusAction(order.id, 'OUT_FOR_DELIVERY')}>
              Dispatch
            </button>
          </div>
        );
      case 'OUT_FOR_DELIVERY':
        return <button className="btn-success w-100" onClick={() => updateStatusAction(order.id, 'DELIVERED')}>Mark Delivered</button>;
      default:
        return <div className="status-locked text-muted">No actions available</div>;
    }
  };

  return (
    <div className="admin-container">
      <div className="page-header flex-between">
        <div>
          <h1>Kitchen Orders Board</h1>
          <p>Drive the canteen flow from order acceptance to delivery confirmation.</p>
        </div>
        <button className="btn-icon" onClick={fetchOrders} title="Refresh orders">
          <RefreshCw size={24} className={loading ? 'spinner' : ''} />
        </button>
      </div>

      <div className="board-controls">
        <div className="date-picker-wrapper inline">
          <Calendar size={20} className="text-amber" />
          <input
            type="date"
            value={date}
            min="2026-07-21"
            onChange={(e) => setDate(e.target.value)}
            className="date-input"
          />
        </div>

        <div className="status-filters-pills">
          {['ALL', 'PLACED', 'ACCEPTED', 'PREPARING', 'PACKED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'REJECTED', 'CANCELLED'].map((status) => {
            const count = status === 'ALL' ? orders.length : orders.filter((order) => order.status === status).length;
            return (
              <button
                key={status}
                className={`pill-btn ${statusFilter === status ? 'active' : ''}`}
                onClick={() => setStatusFilter(status)}
              >
                {status.replace(/_/g, ' ')} <span className="count">{count}</span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="orders-board-grid">
        {filteredOrders.length === 0 ? (
          <div className="empty-state">No orders found for this status.</div>
        ) : (
          filteredOrders.map((order) => (
            <div className={`kitchen-card status-${order.status.toLowerCase()}`} key={order.id}>
              <div className="kc-header">
                <h3>{order.orderNumber}</h3>
                <span className="slot-badge">{order.slotName} ({order.deliveryTime})</span>
              </div>

              <div className="kc-body">
                <div className="recipient-info">
                  <strong>To:</strong> {order.recipientName}
                  <br />
                  <strong>Loc:</strong> {order.deliveryLocation}
                </div>

                <ul className="kc-items">
                  {order.items.map((item) => (
                    <li key={item.menuItemId}>
                      <span className="qty">{item.quantity}x</span> {item.itemName}
                    </li>
                  ))}
                </ul>
              </div>

              <div className="kc-footer">
                {renderActionButtons(order)}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
