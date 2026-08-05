import React, { useEffect, useState } from 'react';
import { Plus, Edit2, Power, X, AlertCircle, Calendar } from 'lucide-react';
import {
  addDailyMenu,
  createMenuItem,
  getDailyMenu,
  getMenuItems,
  updateDailyMenu,
  updateMenuItem,
} from '../../api/admin';
import toast from 'react-hot-toast';
import './MenuManagementPage.css';

const EMPTY_FORM = { name: '', description: '', price: '', imageUrl: '', allergens: '' };

export default function MenuManagementPage() {
  const [tab, setTab] = useState('DAILY');

  // Catalogs (fetched once, one list per menu type)
  const [dailyCatalog, setDailyCatalog] = useState([]);
  const [fixedCatalog, setFixedCatalog] = useState([]);
  const [loadingCatalog, setLoadingCatalog] = useState(true);

  // Add/edit modal (shared, scoped by menuType matching the active tab)
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [errors, setErrors] = useState({});
  const [formData, setFormData] = useState(EMPTY_FORM);

  // Daily scheduling
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [dailyItems, setDailyItems] = useState([]);
  const [loadingDaily, setLoadingDaily] = useState(false);
  const [selectedCatalogItem, setSelectedCatalogItem] = useState('');
  const [newQuantity, setNewQuantity] = useState(50);
  const [dailyErrors, setDailyErrors] = useState({});

  useEffect(() => {
    fetchCatalogs();
  }, []);

  useEffect(() => {
    if (tab === 'DAILY') {
      fetchDailyMenu();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, date]);

  const fetchCatalogs = async () => {
    setLoadingCatalog(true);
    try {
      const [daily, fixed] = await Promise.all([
        getMenuItems(true, 'DAILY'),
        getMenuItems(true, 'FIXED'),
      ]);
      setDailyCatalog(daily);
      setFixedCatalog(fixed);
    } catch (err) {
      toast.error('Failed to load menu items');
    } finally {
      setLoadingCatalog(false);
    }
  };

  const fetchDailyMenu = async () => {
    setLoadingDaily(true);
    try {
      const data = await getDailyMenu(date);
      setDailyItems(data);
    } catch (err) {
      toast.error('Failed to load daily availability');
    } finally {
      setLoadingDaily(false);
    }
  };

  // ─── Catalog: add/edit (no delete for Sub Admin) ───
  const validate = () => {
    const errs = {};
    if (!formData.name.trim()) errs.name = 'Item name is required';
    const priceNum = parseFloat(formData.price);
    if (!formData.price || isNaN(priceNum) || priceNum <= 0) {
      errs.price = 'Enter a valid price greater than ₹0';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) {
      toast.error('Please fix errors highlighted in red');
      return;
    }
    try {
      const payload = {
        ...formData,
        price: Number(formData.price).toFixed(2),
        menuType: tab,
        available: editingId ? formData.available : true,
      };
      if (editingId) {
        await updateMenuItem(editingId, payload);
        toast.success('Item updated');
      } else {
        await createMenuItem(payload);
        toast.success('Item created');
      }
      setIsModalOpen(false);
      setErrors({});
      fetchCatalogs();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  const handleToggleAvailable = async (item) => {
    try {
      await updateMenuItem(item.id, {
        name: item.name,
        description: item.description,
        price: item.price,
        menuType: 'FIXED',
        available: !item.available,
        imageUrl: item.imageUrl,
        allergens: item.allergens,
      });
      toast.success(item.available ? 'Marked out of stock' : 'Marked available');
      fetchCatalogs();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Update failed');
    }
  };

  const openModal = (item = null) => {
    setErrors({});
    if (item) {
      setEditingId(item.id);
      setFormData({
        name: item.name,
        description: item.description || '',
        price: item.price,
        available: item.available,
        imageUrl: item.imageUrl || '',
        allergens: item.allergens || '',
      });
    } else {
      setEditingId(null);
      setFormData(EMPTY_FORM);
    }
    setIsModalOpen(true);
  };

  // ─── Daily scheduling ───
  const validateDaily = () => {
    const errs = {};
    if (!selectedCatalogItem) errs.item = 'Select an item from the catalog';
    const qty = parseInt(newQuantity, 10);
    if (isNaN(qty) || qty <= 0) errs.quantity = 'Quantity must be at least 1';
    setDailyErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleAddDailyItem = async () => {
    if (!validateDaily()) {
      toast.error('Please fix the fields highlighted in red');
      return;
    }
    try {
      await addDailyMenu({ menuItemId: selectedCatalogItem, menuDate: date, totalQuantity: Number(newQuantity) });
      toast.success('Item added to today’s menu');
      setSelectedCatalogItem('');
      setNewQuantity(50);
      setDailyErrors({});
      fetchDailyMenu();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to add item');
    }
  };

  const updateDailyItem = async (dayItem, field, value) => {
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
    return dailyCatalog.filter((item) => item.active && !usedIds.includes(item.id));
  };

  return (
    <div className="admin-container">
      <div className="page-header">
        <h1>Menu Management</h1>
        <p>Add and edit items, update prices and manage availability.</p>
      </div>

      <div className="status-tabs">
        <button className={`tab-btn ${tab === 'DAILY' ? 'active' : ''}`} onClick={() => setTab('DAILY')}>
          Daily Menu
        </button>
        <button className={`tab-btn ${tab === 'FIXED' ? 'active' : ''}`} onClick={() => setTab('FIXED')}>
          Fixed Menu
        </button>
      </div>

      {tab === 'DAILY' && (
        <>
          <div className="page-header flex-between catalog-header">
            <p className="text-muted">{dailyCatalog.length} items in the daily catalog</p>
            <button className="btn-primary" onClick={() => openModal()}>
              <Plus size={18} /> Add Daily Item
            </button>
          </div>

          {loadingCatalog ? (
            <div className="loading-state">Loading items...</div>
          ) : (
            <div className="catalog-chip-list">
              {dailyCatalog.map((item) => (
                <div key={item.id} className={`catalog-chip ${!item.active ? 'inactive' : ''}`}>
                  <span>{item.name}</span>
                  <span className="price">₹{item.price}</span>
                  <button className="btn-icon" onClick={() => openModal(item)} title="Edit">
                    <Edit2 size={14} />
                  </button>
                </div>
              ))}
            </div>
          )}

          <div className="page-header">
            <h2 className="section-title">Today&apos;s Schedule</h2>
          </div>

          <div className="daily-menu-controls">
            <div className="date-picker-wrapper">
              <Calendar size={20} className="text-amber" />
              <input type="date" value={date} onChange={(e) => setDate(e.target.value)} className="date-input" />
            </div>

            <div className="add-item-form-container">
              <div className="add-item-form">
                <div className="form-group-inline">
                  <select
                    value={selectedCatalogItem}
                    onChange={(e) => {
                      setSelectedCatalogItem(e.target.value);
                      if (dailyErrors.item) setDailyErrors({ ...dailyErrors, item: null });
                    }}
                    className={dailyErrors.item ? 'input-error' : ''}
                  >
                    <option value="">Select item from catalog...</option>
                    {getUnusedCatalog().map((item) => (
                      <option key={item.id} value={item.id}>{item.name}</option>
                    ))}
                  </select>
                  {dailyErrors.item && (
                    <span className="field-error-text"><AlertCircle size={14} /> {dailyErrors.item}</span>
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
                      if (dailyErrors.quantity) setDailyErrors({ ...dailyErrors, quantity: null });
                    }}
                    className={dailyErrors.quantity ? 'input-error qty-input' : 'qty-input'}
                  />
                  {dailyErrors.quantity && (
                    <span className="field-error-text"><AlertCircle size={14} /> {dailyErrors.quantity}</span>
                  )}
                </div>

                <button className="btn-primary" onClick={handleAddDailyItem}>
                  <Plus size={18} /> Add to Today
                </button>
              </div>
            </div>
          </div>

          {loadingDaily ? (
            <div className="loading-state">Loading availability...</div>
          ) : dailyItems.length === 0 ? (
            <div className="empty-state">No items scheduled for {date} yet.</div>
          ) : (
            <div className="daily-items-list">
              {dailyItems.map((item) => (
                <div className="daily-item-row" key={item.id}>
                  <div className="item-info">
                    <h3>{item.menuItem.name}</h3>
                    <span className="price">₹{item.menuItem.price}</span>
                  </div>

                  <div className="stock-info">
                    <div className="progress-bar">
                      <div className="progress-fill" style={{ width: `${(item.remainingQuantity / item.totalQuantity) * 100}%` }}></div>
                    </div>
                    <span>{item.remainingQuantity} / {item.totalQuantity} remaining</span>
                  </div>

                  <div className="row-actions">
                    <input
                      type="number"
                      defaultValue={item.totalQuantity}
                      onBlur={(e) => updateDailyItem(item, 'totalQuantity', e.target.value)}
                      className="qty-update-input"
                    />
                    <button
                      className={`btn-toggle ${item.available ? 'active' : ''}`}
                      onClick={() => updateDailyItem(item, 'available', !item.available)}
                    >
                      {item.available ? 'Available' : 'Out of Stock'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === 'FIXED' && (
        <>
          <div className="page-header flex-between catalog-header">
            <p className="text-muted">{fixedCatalog.length} items in the fixed catalog</p>
            <button className="btn-primary" onClick={() => openModal()}>
              <Plus size={18} /> Add Fixed Item
            </button>
          </div>

          {loadingCatalog ? (
            <div className="loading-state">Loading items...</div>
          ) : (
            <div className="items-grid">
              {fixedCatalog.map((item) => (
                <div className={`catalog-card ${!item.active ? 'inactive' : ''}`} key={item.id}>
                  <div className="card-image">
                    {item.imageUrl ? (
                      <img src={item.imageUrl} alt={item.name} />
                    ) : (
                      <div className="img-fallback">{item.name.charAt(0)}</div>
                    )}
                    <span className={`stock-badge ${item.available ? 'available' : 'out'}`}>
                      {item.available ? 'Available' : 'Out of Stock'}
                    </span>
                  </div>
                  <div className="card-content">
                    <div className="card-header">
                      <h3>{item.name}</h3>
                    </div>
                    <p className="desc">{item.description}</p>
                    <div className="card-footer">
                      <span className="price">₹{item.price}</span>
                      <div className="actions">
                        <button
                          className="btn-icon"
                          onClick={() => handleToggleAvailable(item)}
                          title={item.available ? 'Mark Out of Stock' : 'Mark Available'}
                        >
                          <Power size={16} />
                        </button>
                        <button className="btn-icon" onClick={() => openModal(item)} title="Edit">
                          <Edit2 size={16} />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingId ? 'Edit Menu Item' : `Add New ${tab === 'DAILY' ? 'Daily' : 'Fixed'} Item`}</h2>
              <button className="close-btn" onClick={() => setIsModalOpen(false)}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSubmit} noValidate>
              <div className="form-group">
                <label>Item Name</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => {
                    setFormData({ ...formData, name: e.target.value });
                    if (errors.name) setErrors({ ...errors, name: null });
                  }}
                  className={errors.name ? 'input-error' : ''}
                />
                {errors.name && <span className="field-error-text"><AlertCircle size={14} /> {errors.name}</span>}
              </div>

              <div className="form-group">
                <label>Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  rows={3}
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Price (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={formData.price}
                    onChange={(e) => {
                      setFormData({ ...formData, price: e.target.value });
                      if (errors.price) setErrors({ ...errors, price: null });
                    }}
                    className={errors.price ? 'input-error' : ''}
                  />
                  {errors.price && <span className="field-error-text"><AlertCircle size={14} /> {errors.price}</span>}
                </div>

                <div className="form-group">
                  <label>Image URL</label>
                  <input
                    type="url"
                    placeholder="https://..."
                    value={formData.imageUrl}
                    onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                  />
                </div>
              </div>

              {tab === 'FIXED' && editingId && (
                <div className="form-group">
                  <label>Availability</label>
                  <button
                    type="button"
                    className={`btn-toggle ${formData.available ? 'active' : ''}`}
                    onClick={() => setFormData({ ...formData, available: !formData.available })}
                  >
                    {formData.available ? 'Available' : 'Out of Stock'}
                  </button>
                </div>
              )}

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setIsModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn-primary">{editingId ? 'Save Changes' : 'Create Item'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
