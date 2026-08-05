import React, { useEffect, useState } from 'react';
import { Plus, Edit2, Trash2, Power, X, AlertCircle } from 'lucide-react';
import { createMenuItem, deleteMenuItem, getMenuItems, updateMenuItem } from '../../api/admin';
import toast from 'react-hot-toast';
import './FixedMenuPage.css';

const EMPTY_FORM = { name: '', description: '', price: '', available: true, imageUrl: '', allergens: '' };

export default function FixedMenuPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [errors, setErrors] = useState({});
  const [formData, setFormData] = useState(EMPTY_FORM);

  useEffect(() => {
    fetchItems();
  }, []);

  const fetchItems = async () => {
    try {
      const data = await getMenuItems(true, 'FIXED');
      setItems(data);
    } catch (err) {
      toast.error('Failed to load fixed menu items');
    } finally {
      setLoading(false);
    }
  };

  const validate = () => {
    const errs = {};
    if (!formData.name.trim()) {
      errs.name = 'Item name is required';
    }

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
        menuType: 'FIXED',
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
      fetchItems();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Retire this menu item from future ordering?')) return;
    try {
      await deleteMenuItem(id);
      toast.success('Item retired');
      fetchItems();
    } catch (err) {
      toast.error('Delete failed');
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
      fetchItems();
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

  return (
    <div className="admin-container">
      <div className="page-header flex-between">
        <div>
          <h1>Fixed Menu Management</h1>
          <p>Permanent items like drinks, chips and snacks — always available until you change them.</p>
        </div>
        <button className="btn-primary" onClick={() => openModal()}>
          <Plus size={18} /> Add New Item
        </button>
      </div>

      {loading ? (
        <div className="loading-state">Loading items...</div>
      ) : items.length === 0 ? (
        <div className="empty-state">No fixed menu items yet. Add one to get started.</div>
      ) : (
        <div className="items-grid">
          {items.map((item) => (
            <div className={`catalog-card ${!item.active ? 'inactive' : ''}`} key={item.id}>
              <div className="card-image">
                {item.imageUrl ? (
                  <img src={item.imageUrl} alt={item.name} />
                ) : (
                  <div className="img-fallback">{item.name.charAt(0)}</div>
                )}
                {item.active && (
                  <span className={`stock-badge ${item.available ? 'available' : 'out'}`}>
                    {item.available ? 'Available' : 'Out of Stock'}
                  </span>
                )}
              </div>
              <div className="card-content">
                <div className="card-header">
                  <h3>{item.name}</h3>
                </div>
                <p className="desc">{item.description}</p>
                <div className="card-footer">
                  <span className="price">₹{item.price}</span>
                  <div className="actions">
                    {item.active && (
                      <button
                        className="btn-icon"
                        onClick={() => handleToggleAvailable(item)}
                        title={item.available ? 'Mark Out of Stock' : 'Mark Available'}
                      >
                        <Power size={16} />
                      </button>
                    )}
                    <button className="btn-icon" onClick={() => openModal(item)} title="Edit">
                      <Edit2 size={16} />
                    </button>
                    {item.active && (
                      <button className="btn-icon-danger" onClick={() => handleDelete(item.id)} title="Delete">
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingId ? 'Edit Fixed Menu Item' : 'Add New Fixed Menu Item'}</h2>
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
                {errors.name && (
                  <span className="field-error-text">
                    <AlertCircle size={14} /> {errors.name}
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
                      if (errors.price) setErrors({ ...errors, price: null });
                    }}
                    className={errors.price ? 'input-error' : ''}
                  />
                  {errors.price && (
                    <span className="field-error-text">
                      <AlertCircle size={14} /> {errors.price}
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
    </div>
  );
}
