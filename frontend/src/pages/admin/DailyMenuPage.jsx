import React, { useEffect, useState } from 'react';
import { Calendar, Plus, AlertCircle } from 'lucide-react';
import { addDailyMenu, getDailyMenu, getMenuItems, updateDailyMenu } from '../../api/admin';
import toast from 'react-hot-toast';
import './DailyMenuPage.css';

export default function DailyMenuPage() {
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [dailyItems, setDailyItems] = useState([]);
  const [catalog, setCatalog] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedCatalogItem, setSelectedCatalogItem] = useState('');
  const [newQuantity, setNewQuantity] = useState(50);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    fetchDailyMenu();
    fetchCatalog();
  }, [date]);

  const fetchDailyMenu = async () => {
    setLoading(true);
    try {
      const data = await getDailyMenu(date);
      setDailyItems(data);
    } catch (err) {
      toast.error('Failed to load daily menu');
    } finally {
      setLoading(false);
    }
  };

  const fetchCatalog = async () => {
    try {
      const data = await getMenuItems(false);
      setCatalog(data.filter((item) => item.active));
    } catch (err) {
      toast.error('Failed to load menu catalog');
    }
  };

  const validate = () => {
    const errs = {};
    if (!selectedCatalogItem) {
      errs.item = 'Select an item from the catalog';
    }

    const qty = parseInt(newQuantity, 10);
    if (isNaN(qty) || qty <= 0) {
      errs.quantity = 'Quantity must be at least 1';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleAddItem = async () => {
    if (!validate()) {
      toast.error('Please fix the fields highlighted in red');
      return;
    }

    try {
      await addDailyMenu({
        menuItemId: selectedCatalogItem,
        menuDate: date,
        totalQuantity: Number(newQuantity),
      });
      toast.success('Item added to daily menu');
      setSelectedCatalogItem('');
      setNewQuantity(50);
      setErrors({});
      fetchDailyMenu();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to add item');
    }
  };

  const updateItem = async (dayItem, field, value) => {
    const payload = {
      totalQuantity: field === 'totalQuantity' ? Number(value) : dayItem.totalQuantity,
      available: field === 'available' ? value : dayItem.available,
    };
    try {
      await updateDailyMenu(dayItem.id, payload);
      toast.success('Updated');
      fetchDailyMenu();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Update failed');
    }
  };

  const getUnusedCatalog = () => {
    const usedIds = dailyItems.map((item) => item.menuItem.id);
    return catalog.filter((item) => !usedIds.includes(item.id));
  };

  return (
    <div className="admin-container">
      <div className="page-header">
        <h1>Daily Menu Setup</h1>
        <p>Publish each school day&apos;s menu and control stock before orders open.</p>
      </div>

      <div className="daily-menu-controls">
        <div className="date-picker-wrapper">
          <Calendar size={20} className="text-amber" />
          <input
            type="date"
            value={date}
            min="2026-07-21"
            onChange={(e) => setDate(e.target.value)}
            className="date-input"
          />
        </div>

        <div className="add-item-form-container">
          <div className="add-item-form">
            <div className="form-group-inline">
              <select
                value={selectedCatalogItem}
                onChange={(e) => {
                  setSelectedCatalogItem(e.target.value);
                  if (errors.item) setErrors({ ...errors, item: null });
                }}
                className={errors.item ? 'input-error' : ''}
              >
                <option value="">Select item from catalog...</option>
                {getUnusedCatalog().map((item) => (
                  <option key={item.id} value={item.id}>{item.name}</option>
                ))}
              </select>
              {errors.item && (
                <span className="field-error-text">
                  <AlertCircle size={14} /> {errors.item}
                </span>
              )}
            </div>

            <div className="form-group-inline">
              <input
                type="number"
                min="1"
                placeholder="Qty"
                value={newQuantity}
                onChange={(e) => {
                  setNewQuantity(e.target.value);
                  if (errors.quantity) setErrors({ ...errors, quantity: null });
                }}
                className={errors.quantity ? 'input-error qty-input' : 'qty-input'}
              />
              {errors.quantity && (
                <span className="field-error-text">
                  <AlertCircle size={14} /> {errors.quantity}
                </span>
              )}
            </div>

            <button className="btn-primary" onClick={handleAddItem}>
              <Plus size={18} /> Schedule Item
            </button>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="loading-state">Loading daily menu...</div>
      ) : dailyItems.length === 0 ? (
        <div className="empty-state">No items scheduled for {date}. Use the dropdown above to add items.</div>
      ) : (
        <div className="daily-items-list">
          {dailyItems.map((item) => (
            <div className="daily-item-row" key={item.id}>
              <div className="item-info">
                <h3>{item.menuItem.name}</h3>
                <span className="badge badge-amber">{item.menuItem.category}</span>
                <span className="price">₹{item.menuItem.price}</span>
              </div>

              <div className="stock-info">
                <div className="progress-bar">
                  <div
                    className="progress-fill"
                    style={{ width: `${(item.remainingQuantity / item.totalQuantity) * 100}%` }}
                  ></div>
                </div>
                <span>{item.remainingQuantity} / {item.totalQuantity} remaining</span>
              </div>

              <div className="row-actions">
                <input
                  type="number"
                  defaultValue={item.totalQuantity}
                  onBlur={(e) => updateItem(item, 'totalQuantity', e.target.value)}
                  className="qty-update-input"
                />
                <button
                  className={`btn-toggle ${item.available ? 'active' : ''}`}
                  onClick={() => updateItem(item, 'available', !item.available)}
                >
                  {item.available ? 'Available' : 'Disabled'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
