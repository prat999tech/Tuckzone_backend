import React, { useEffect, useState } from 'react';
import { Calendar, Clock, Plus, Edit2, Trash2, RotateCcw, ChevronDown, ChevronRight, AlertCircle, X, UtensilsCrossed } from 'lucide-react';
import {
  activateMenuItem,
  addDailyMenu,
  createMenuItem,
  deleteMenuItem,
  getDailyMenu,
  getMenuItems,
  permanentlyDeleteMenuItem,
  updateCutoffTime,
  updateDailyMenu,
  updateMenuItem,
} from '../../api/admin';
import { getDeliverySlots } from '../../api/menu';
import toast from 'react-hot-toast';
import EmptyState from '../../components/EmptyState';
import ConfirmDialog from '../../components/ConfirmDialog';
import MenuActionDialog from '../../components/MenuActionDialog';
import './DailyMenuPage.css';

const EMPTY_FORM = { name: '', description: '', price: '', imageUrl: '', allergens: '' };

export default function DailyMenuPage() {
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [dailyItems, setDailyItems] = useState([]);
  const [catalog, setCatalog] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedCatalogItem, setSelectedCatalogItem] = useState('');
  const [newQuantity, setNewQuantity] = useState(50);
  const [errors, setErrors] = useState({});

  // Catalog add/edit modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [formErrors, setFormErrors] = useState({});

  // Catalog delete flow: choice dialog first, then a second confirm for permanent delete
  const [actionTarget, setActionTarget] = useState(null);
  const [permanentDeleteTarget, setPermanentDeleteTarget] = useState(null);
  const [showRetired, setShowRetired] = useState(false);

  // Order cut-off time (standing default for the delivery slot, takes effect immediately)
  const [slots, setSlots] = useState([]);
  const [cutoffDraft, setCutoffDraft] = useState('');
  const [savingCutoff, setSavingCutoff] = useState(false);

  useEffect(() => {
    fetchDailyMenu();
    fetchCatalog();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [date]);

  useEffect(() => {
    fetchSlots();
  }, []);

  const fetchSlots = async () => {
    try {
      const data = await getDeliverySlots();
      setSlots(data);
      // Only seed the draft on first load so an in-progress edit isn't clobbered on refresh.
      setCutoffDraft((prev) => (prev === '' && data.length > 0 ? data[0].orderCutoffTime.slice(0, 5) : prev));
    } catch (err) {
      toast.error('Failed to load ordering slot');
    }
  };

  const handleSaveCutoff = async () => {
    if (slots.length === 0) return;
    setSavingCutoff(true);
    try {
      const updated = await updateCutoffTime(slots[0].id, cutoffDraft);
      setSlots([updated]);
      setCutoffDraft(updated.orderCutoffTime.slice(0, 5));
      toast.success('Cut-off time updated — takes effect immediately');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not update cut-off time');
    } finally {
      setSavingCutoff(false);
    }
  };

  const fetchDailyMenu = async () => {
    setLoading(true);
    try {
      const data = await getDailyMenu(date);
      setDailyItems(data);
    } catch (err) {
      toast.error('Failed to load Meal of the Day');
    } finally {
      setLoading(false);
    }
  };

  const fetchCatalog = async () => {
    try {
      const data = await getMenuItems(true, 'DAILY');
      setCatalog(data);
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
      toast.success('Item added to Meal of the Day');
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
    return catalog.filter((item) => item.active && !usedIds.includes(item.id));
  };

  const activeCatalog = catalog.filter((item) => item.active);
  const retiredCatalog = catalog.filter((item) => !item.active);

  // ─── Catalog add/edit ───
  const validateForm = () => {
    const errs = {};
    if (!formData.name.trim()) errs.name = 'Item name is required';
    const priceNum = parseFloat(formData.price);
    if (!formData.price || isNaN(priceNum) || priceNum <= 0) {
      errs.price = 'Enter a valid price greater than ₹0';
    }
    setFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) {
      toast.error('Please fix errors highlighted in red');
      return;
    }
    try {
      const payload = {
        ...formData,
        price: Number(formData.price).toFixed(2),
        menuType: 'DAILY',
        available: true,
      };
      if (editingId) {
        await updateMenuItem(editingId, payload);
        toast.success('Item updated');
      } else {
        await createMenuItem(payload);
        toast.success('Item added to the daily catalog');
      }
      setIsModalOpen(false);
      setFormErrors({});
      fetchCatalog();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  const handleRetire = async () => {
    if (!actionTarget) return;
    try {
      await deleteMenuItem(actionTarget.id);
      toast.success('Item retired');
      setActionTarget(null);
      fetchCatalog();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not retire item');
    }
  };

  const handleRestore = async (item) => {
    try {
      await activateMenuItem(item.id);
      toast.success('Item restored to Active');
      fetchCatalog();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not restore item');
    }
  };

  const handlePermanentDelete = async () => {
    if (!permanentDeleteTarget) return;
    try {
      await permanentlyDeleteMenuItem(permanentDeleteTarget.id);
      toast.success('Item permanently deleted');
      setPermanentDeleteTarget(null);
      fetchCatalog();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not permanently delete item');
    }
  };

  const openModal = (item = null) => {
    setFormErrors({});
    if (item) {
      setEditingId(item.id);
      setFormData({
        name: item.name,
        description: item.description || '',
        price: item.price,
        imageUrl: item.imageUrl || '',
        allergens: item.allergens || '',
      });
    } else {
      setEditingId(null);
      setFormData(EMPTY_FORM);
    }
    setIsModalOpen(true);
  };

  return (
    <div className="admin-container">
      <div className="page-header">
        <h1>Meal of the Day Management</h1>
        <p>Manage the rotating catalog and publish each school day&apos;s menu with stock.</p>
      </div>

      <div className="page-header flex-between catalog-header">
        <h2 className="section-title">Daily Item Catalog</h2>
        <button className="btn-primary" onClick={() => openModal()}>
          <Plus size={18} /> Add Daily Item
        </button>
      </div>

      {activeCatalog.length === 0 ? (
        <EmptyState
          icon={UtensilsCrossed}
          title="No Menu Available"
          message="No menu items have been added yet."
          action={
            <button className="btn-primary" onClick={() => openModal()}>
              <Plus size={18} /> Add Menu
            </button>
          }
        />
      ) : (
        <div className="catalog-chip-list">
          {activeCatalog.map((item) => (
            <div key={item.id} className="catalog-chip">
              <span>{item.name}</span>
              <span className="price">₹{item.price}</span>
              <button className="btn-icon" onClick={() => openModal(item)} title="Edit">
                <Edit2 size={14} />
              </button>
              <button className="btn-icon-danger" onClick={() => setActionTarget(item)} title="Delete">
                <Trash2 size={14} />
              </button>
            </div>
          ))}
        </div>
      )}

      {retiredCatalog.length > 0 && (
        <div className="retired-section">
          <button className="retired-header" onClick={() => setShowRetired((prev) => !prev)}>
            {showRetired ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
            Retired Items ({retiredCatalog.length})
          </button>
          {showRetired && (
            <div className="catalog-chip-list">
              {retiredCatalog.map((item) => (
                <div key={item.id} className="catalog-chip inactive">
                  <span>{item.name}</span>
                  <span className="price">₹{item.price}</span>
                  <button className="btn-icon" onClick={() => openModal(item)} title="Edit">
                    <Edit2 size={14} />
                  </button>
                  <button className="btn-icon" onClick={() => handleRestore(item)} title="Restore">
                    <RotateCcw size={14} />
                  </button>
                  <button className="btn-icon-danger" onClick={() => setPermanentDeleteTarget(item)} title="Delete Permanently">
                    <Trash2 size={14} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <div className="cutoff-card">
        <div className="cutoff-header">
          <Clock size={18} className="text-amber" />
          <h2 className="section-title">Order Cut-off Time</h2>
        </div>
        <p className="cutoff-hint">
          Students can place orders only before this time each day. Changes take effect immediately.
        </p>
        <div className="cutoff-row">
          <input
            type="time"
            value={cutoffDraft}
            onChange={(e) => setCutoffDraft(e.target.value)}
            className="cutoff-input"
          />
          <button
            className="btn-primary"
            onClick={handleSaveCutoff}
            disabled={slots.length === 0 || savingCutoff || cutoffDraft === slots[0]?.orderCutoffTime.slice(0, 5)}
          >
            {savingCutoff ? 'Saving...' : 'Save'}
          </button>
        </div>
      </div>

      <div className="page-header">
        <h2 className="section-title">Today&apos;s Schedule</h2>
      </div>

      <div className="daily-menu-controls">
        <div className="date-picker-wrapper">
          <Calendar size={20} className="text-amber" />
          <input
            type="date"
            value={date}
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
        <div className="loading-state">Loading Meal of the Day...</div>
      ) : dailyItems.length === 0 ? (
        <EmptyState
          icon={UtensilsCrossed}
          title="Nothing Scheduled Yet"
          message={`No items scheduled for ${date}. Use the dropdown above to add items.`}
        />
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

      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingId ? 'Edit Daily Item' : 'Add New Daily Item'}</h2>
              <button className="close-btn" onClick={() => setIsModalOpen(false)}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleFormSubmit} noValidate>
              <div className="form-group">
                <label>Item Name</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => {
                    setFormData({ ...formData, name: e.target.value });
                    if (formErrors.name) setFormErrors({ ...formErrors, name: null });
                  }}
                  className={formErrors.name ? 'input-error' : ''}
                />
                {formErrors.name && (
                  <span className="field-error-text">
                    <AlertCircle size={14} /> {formErrors.name}
                  </span>
                )}
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
                      if (formErrors.price) setFormErrors({ ...formErrors, price: null });
                    }}
                    className={formErrors.price ? 'input-error' : ''}
                  />
                  {formErrors.price && (
                    <span className="field-error-text">
                      <AlertCircle size={14} /> {formErrors.price}
                    </span>
                  )}
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

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setIsModalOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {editingId ? 'Save Changes' : 'Create Item'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <MenuActionDialog
        open={Boolean(actionTarget)}
        itemName={actionTarget?.name}
        onRetire={handleRetire}
        onPermanentDelete={() => {
          setPermanentDeleteTarget(actionTarget);
          setActionTarget(null);
        }}
        onCancel={() => setActionTarget(null)}
      />

      <ConfirmDialog
        open={Boolean(permanentDeleteTarget)}
        title="Permanently delete this item?"
        message="Are you sure you want to permanently delete this menu item? This action cannot be undone."
        confirmLabel="Delete Permanently"
        onCancel={() => setPermanentDeleteTarget(null)}
        onConfirm={handlePermanentDelete}
      />
    </div>
  );
}
